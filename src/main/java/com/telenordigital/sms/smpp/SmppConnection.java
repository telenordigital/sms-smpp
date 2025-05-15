package com.telenordigital.sms.smpp;

/*-
 * #%L
 * sms-smpp
 * %%
 * Copyright (C) 2022 Telenor Digital
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.telenordigital.sms.smpp.config.SmppConnectionConfig;
import com.telenordigital.sms.smpp.config.TlsConfig;
import com.telenordigital.sms.smpp.pdu.Bind;
import com.telenordigital.sms.smpp.pdu.BindResp;
import com.telenordigital.sms.smpp.pdu.Command;
import com.telenordigital.sms.smpp.pdu.EnquireLink;
import com.telenordigital.sms.smpp.pdu.EnquireLinkResp;
import com.telenordigital.sms.smpp.pdu.RequestPdu;
import com.telenordigital.sms.smpp.pdu.ResponsePdu;
import com.telenordigital.sms.smpp.pdu.SubmitSm;
import com.telenordigital.sms.smpp.pdu.SubmitSmResp;
import com.telenordigital.sms.smpp.pdu.Unbind;
import com.telenordigital.sms.smpp.pdu.UnbindResp;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SmppConnection implements Closeable {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final int MAX_SMPP_FRAME_LENGTH = 64 * 1024; // 64K based on the spec

  private OutboundPduHandler outboundPduHandler;
  final SmppConnectionConfig config;
  private final MultiThreadIoEventLoopGroup group;
  private final Bootstrap bootstrap;
  final Function<SmppSmsMo, CompletableFuture<Void>> moHandler;
  final Function<SmppDeliveryReceipt, CompletableFuture<Void>> drHandler;

  private SmppState state = SmppState.INACTIVE;
  private Channel channel;
  private String remoteSystemId;

  SmppConnection(
      final SmppConnectionConfig config,
      final Function<SmppSmsMo, CompletableFuture<Void>> moHandler,
      final Function<SmppDeliveryReceipt, CompletableFuture<Void>> drHandler) {
    this.config = config;
    group =
        new MultiThreadIoEventLoopGroup(
            new DefaultThreadFactory(config.connectionUrl().toString()), NioIoHandler.newFactory());
    this.moHandler = moHandler;
    this.drHandler = drHandler;

    bootstrap =
        new Bootstrap()
            .group(group)
            .remoteAddress(config.host(), config.port())
            .channel(NioSocketChannel.class)
            .handler(
                new ChannelInitializer<SocketChannel>() {
                  @Override
                  protected void initChannel(final SocketChannel channel) {
                    setupPipeline(channel, config.tls(), config.defaultEncoding().charset);
                  }
                });

    connectInternal();
  }

  int getOpenWindowSlots() {
    return outboundPduHandler == null ? 0 : outboundPduHandler.getOpenWindowSlots();
  }

  class ConnectionHandler extends ChannelInboundHandlerAdapter {

    private Bind createBind() {
      final var command =
          switch (config.bindType()) {
            case TRANSMITTER -> Command.BIND_TRANSMITTER;
            case RECEIVER -> Command.BIND_RECEIVER;
            case TRANSCEIVER -> Command.BIND_TRANCEIVER;
          };

      return new Bind(command, config.systemId(), config.password(), config.systemType());
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) {
      ctx.writeAndFlush(new RequestResponse<>(createBind()))
          .addListener(
              future -> {
                if (!future.isSuccess()) {
                  LOG.warn("Error sending bind", future.cause());
                  ctx.close();
                }
              });
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
      if (msg instanceof BindResp bindResp) {
        if (bindResp.commandStatus() == 0) {
          remoteSystemId = bindResp.systemId();
          stateChange(SmppState.ACTIVE);
          LOG.info("Successfully bound to: {}", config.systemId());
        } else {
          LOG.warn("Invalid bind response status: {}", bindResp.status());
          ctx.channel().close();
        }
      }
    }

    @Override
    public void channelUnregistered(final ChannelHandlerContext ctx) {
      if (state == SmppState.ACTIVE) {
        remoteSystemId = null;
        stateChange(SmppState.INACTIVE);
      }

      // don't reconnect if connection is being closed
      if (state != SmppState.CLOSING && state != SmppState.CLOSED) {
        scheduleReconnect();
      } else {
        LOG.debug("Skip reconnecting, connection is closing");
      }
    }

    @Override
    public void userEventTriggered(final ChannelHandlerContext ctx, final Object evt)
        throws Exception {
      if (evt instanceof IdleStateEvent e) {
        LOG.debug("Received idle event: {}", e);
        if (e.state() == IdleState.READER_IDLE) {
          LOG.warn(
              "No data received for {}s. Closing connection...", config.requestTimeoutSeconds());
          ctx.channel().close();
        } else if (e.state() == IdleState.ALL_IDLE) {
          enquireLink().thenRun(() -> LOG.info("Enquired link for idle connection"));
        }
      }

      super.userEventTriggered(ctx, evt);
    }
  }

  private void addSslHandler(final SocketChannel channel, final TlsConfig tls) {
    try {
      final var sslContext =
          SslContextBuilder.forClient()
              .sslProvider(
                  tls.sslProvider() == SmppConnectionConfig.SslProvider.OPENSSL
                      ? SslProvider.OPENSSL
                      : SslProvider.JDK);

      if (tls.verifyHostname()) {
        sslContext
            .endpointIdentificationAlgorithm("HTTPS")
            .serverName(new SNIHostName(tls.expectedServerHostname()));
      } else {
        sslContext.endpointIdentificationAlgorithm(null);
      }

      if (tls.trustedCerts() != null) {
        sslContext.trustManager(new ByteArrayInputStream(tls.trustedCerts()));
      }
      if (tls.clientCert() != null) {
        sslContext.keyManager(
            new ByteArrayInputStream(tls.clientCert()), new ByteArrayInputStream(tls.clientKey()));
      }
      final SSLEngine engine = sslContext.build().newEngine(channel.alloc());

      channel.pipeline().addFirst("ssl", new SslHandler(engine));
    } catch (final SSLException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void setupPipeline(
      final SocketChannel channel, final TlsConfig tls, final Charset defaultCharset) {
    if (tls != null) {
      addSslHandler(channel, tls);
    }

    outboundPduHandler =
        new OutboundPduHandler(config.windowSize(), config.requestTimeoutSeconds());

    channel
        .pipeline()
        .addLast("logging", new LoggingHandler(SmppConnection.class))
        .addLast(
            "idleState",
            new IdleStateHandler(config.requestTimeoutSeconds(), 0, config.idleTimeSeconds()))
        .addLast(
            "frameDecoder", new LengthFieldBasedFrameDecoder(MAX_SMPP_FRAME_LENGTH, 0, 4, -4, 4))
        .addLast("frameEncoder", new LengthFieldPrepender(4, 0, true))
        .addLast("decoder", new PduDecoder(defaultCharset))
        .addLast("encoder", new PduEncoder())
        .addLast("inboundHandler", new InboundPduHandler(this))
        .addLast("outboundHandler", outboundPduHandler)
        .addLast("connectionHandler", new ConnectionHandler());
  }

  private void connectInternal() {
    bootstrap.connect().addListener((ChannelFutureListener) this::connectOperationComplete);
  }

  private void connectOperationComplete(final ChannelFuture future) {
    if (!future.isSuccess()) {
      LOG.info("Unable to connect: {}", future.cause().getMessage(), future.cause());
      future.channel().close();
    } else {
      channel = future.channel();
    }
  }

  void scheduleReconnect() {
    LOG.info("Reconnecting in {}s", config.reconnectTimeSeconds());
    group.schedule(this::connectInternal, config.reconnectTimeSeconds(), TimeUnit.SECONDS);
  }

  public boolean isActive() {
    return state == SmppState.ACTIVE;
  }

  CompletableFuture<EnquireLinkResp> enquireLink() {
    return submitInternal(new EnquireLink(), true);
  }

  public CompletableFuture<SubmitSmResp> submit(final SubmitSm submitSm) {
    return submitInternal(submitSm, true);
  }

  CompletableFuture<UnbindResp> unbind() {
    // stop submit_sm and start draining outbound
    return stateChange(SmppState.DRAINING_OUTBOUND)
        // send unbind
        .thenCompose(v -> submitInternal(new Unbind(), false))
        // draining inbound
        .thenCompose(resp -> stateChange(SmppState.DRAINING_INBOUND).thenApply(v -> resp));
  }

  private <R extends ResponsePdu, T extends RequestPdu<R>> CompletableFuture<R> submitInternal(
      final T pdu, final boolean requireActive) {
    final var requestResponse = new RequestResponse<>(pdu);
    if (requireActive && !isActive()) {
      LOG.debug("Ignore sending PDUs for non-active connections. State:: {}. ", state);
      requestResponse.completeExceptionally(channel.eventLoop(), "Connection is inactive");
    } else {
      LOG.debug("Submitting PDU: {}", pdu);
      channel.writeAndFlush(requestResponse);
    }
    return requestResponse.responseFuture();
  }

  @Override
  public void close() {
    // try to unbind if the session is bound
    if (isActive()) {
      try {
        unbind().get(config.shutdownTimeoutSeconds(), TimeUnit.SECONDS);
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        LOG.warn("Unbind interrupt", e);
      } catch (final Exception e) {
        LOG.warn("Unbind error ", e);
      }
    }

    if (channel != null) {
      stateChange(SmppState.CLOSING);
      group.shutdownGracefully().syncUninterruptibly();
      channel = null;
      outboundPduHandler = null;
      stateChange(SmppState.CLOSED);
    }
  }

  String getRemoteSystemId() {
    return remoteSystemId;
  }

  CompletableFuture<Void> stateChange(final SmppState state) {
    LOG.debug("Connection state changed {} -> {}", this.state, state);
    this.state = state;
    if (channel != null) {
      final var event = new SmppDrainEvent(state);
      channel.pipeline().fireUserEventTriggered(event);

      return event.future();
    }

    return CompletableFuture.completedFuture(null);
  }

  String info() {
    return String.format("Connection: %s. State: %s", config.connectionUrl(), state);
  }
}
