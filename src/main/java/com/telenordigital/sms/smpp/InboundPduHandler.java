package com.telenordigital.sms.smpp;

import com.telenordigital.sms.smpp.pdu.DeliverSm;
import com.telenordigital.sms.smpp.pdu.EnquireLink;
import com.telenordigital.sms.smpp.pdu.RequestPdu;
import com.telenordigital.sms.smpp.pdu.Unbind;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class InboundPduHandler extends MessageToMessageDecoder<RequestPdu<?>> {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final SmppConnection parent;

  private CompletableFuture<Void> drainingFuture;
  private final Map<DeliverSm, CompletableFuture<ChannelFuture>> futures = new HashMap<>();

  InboundPduHandler(final SmppConnection parent) {
    this.parent = parent;
  }

  @Override
  protected void decode(
      final ChannelHandlerContext ctx, final RequestPdu<?> msg, final List<Object> out) {
    LOG.debug("Received PDU: {}", msg);

    if (msg instanceof Unbind) {
      parent
          // stop accepting submit_sms
          .stateChange(SmppState.DRAINING_OUTBOUND)
          // stop accepting deliver_sm, wait outstanding to complete
          .thenCompose(v -> parent.stateChange(SmppState.DRAINING_INBOUND))
          .thenApply(
              v ->
                  ctx.writeAndFlush(msg.createResponse()).addListener(ChannelFutureListener.CLOSE));
    } else if (msg instanceof EnquireLink) {
      ctx.writeAndFlush(msg.createResponse());

    } else if (msg instanceof DeliverSm deliverSm) {
      if (drainingFuture != null) {
        LOG.warn("Receiving deliver_sm in DRAINING mode: {}", deliverSm);
        // reply with error
        ctx.writeAndFlush(msg.createGenericFailureResponse());
      } else {
        try {
          handleDeliverSm(ctx, deliverSm);
        } catch (final RuntimeException e) {
          handleException(e, ctx, deliverSm);
        }
      }
    }
  }

  private void handleDeliverSm(final ChannelHandlerContext ctx, final DeliverSm deliverSm) {
    LOG.debug("Handling deliver_sm: {}", deliverSm);

    final CompletableFuture<ChannelFuture> future;
    if (deliverSm.isDeliveryReceipt()) {
      future =
          SmppMapping.mapDeliverSm(deliverSm.state())
              .map(
                  s ->
                      new SmppDeliveryReceipt(
                          deliverSm.receiptedMsgId(), s, deliverSm.networkCode()))
              .map(
                  dr ->
                      parent
                          .drHandler
                          .apply(dr)
                          .orTimeout(parent.config.handlerTimeoutSeconds(), TimeUnit.SECONDS)
                          .thenApply(i -> handleSuccess(ctx, deliverSm))
                          .exceptionally(e -> handleException(e, ctx, deliverSm)))
              .orElseGet(
                  () -> {
                    final var e = new SmppException("Unknown deliver_sm state");
                    handleException(e, ctx, deliverSm);
                    return CompletableFuture.failedFuture(e);
                  });
    } else {
      final var mo =
          new SmppSmsMo(
              deliverSm.sender().address(), deliverSm.destination().address(), deliverSm.message());
      future =
          parent
              .moHandler
              .apply(mo)
              .orTimeout(parent.config.handlerTimeoutSeconds(), TimeUnit.SECONDS)
              .thenApply(ignore -> handleSuccess(ctx, deliverSm))
              .exceptionally(e -> handleException(e, ctx, deliverSm));
    }

    if (!future.isDone()) {
      futures.put(deliverSm, future);
    } else {
      LOG.warn(
          "deliver_sm handled on the event thread. Should not happened in prod! {}", deliverSm);
    }
  }

  private ChannelFuture handleSuccess(final ChannelHandlerContext ctx, final DeliverSm deliverSm) {
    LOG.debug("Handle success: {}", deliverSm);
    return ctx.writeAndFlush(deliverSm.createResponse())
        .addListener(f -> operationComplete(deliverSm));
  }

  private ChannelFuture handleException(
      final Throwable e, final ChannelHandlerContext ctx, final DeliverSm msg) {
    LOG.warn("Error handling deliver sm: {}", msg, e);
    return ctx.writeAndFlush(msg.createGenericFailureResponse())
        .addListener(f -> operationComplete(msg));
  }

  private void operationComplete(final DeliverSm deliverSm) {
    LOG.debug("deliver_sm processed: {}", deliverSm);
    final var future = futures.remove(deliverSm);
    if (future == null) {
      LOG.warn("Unable to find future for: {}", deliverSm);
    }

    if (drainingFuture != null && futures.size() == 0) {
      LOG.debug("Draining complete");
      drainingFuture.complete(null);
    }
  }

  @Override
  public void userEventTriggered(final ChannelHandlerContext ctx, final Object evt)
      throws Exception {
    if (evt instanceof SmppDrainEvent event && event.type() == SmppState.DRAINING_INBOUND) {
      LOG.debug("Receiving draining state change. Outstanding requests: {}", futures.size());
      if (futures.size() == 0) {
        event.future().complete(null);
      } else {
        drainingFuture = event.future();
      }
    }

    super.userEventTriggered(ctx, evt);
  }
}
