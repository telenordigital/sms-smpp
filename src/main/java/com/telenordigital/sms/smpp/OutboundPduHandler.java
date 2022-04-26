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

import com.telenordigital.sms.smpp.pdu.ResponsePdu;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class OutboundPduHandler extends MessageToMessageCodec<ResponsePdu, RequestResponse<ResponsePdu>> {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final AtomicInteger openWindowSlots;
  private final int windowSize;
  private final int timeoutSeconds;

  private final Map<Integer, RequestResponse<ResponsePdu>> window = new LinkedHashMap<>();
  private CompletableFuture<Void> drainingFuture;

  OutboundPduHandler(final int windowSize, final int timeoutSeconds) {
    if (windowSize < 1) {
      throw new IllegalArgumentException("Window size must be > 0");
    }

    this.openWindowSlots = new AtomicInteger(windowSize);
    this.windowSize = windowSize;
    this.timeoutSeconds = timeoutSeconds;
  }

  private void removeExpired(final ChannelHandlerContext ctx) {
    window
        .entrySet()
        .removeIf(
            entry -> {
              final var rr = entry.getValue();
              final var age = System.currentTimeMillis() - rr.startedTimeMillis();

              if (age > timeoutSeconds * 1_000L) {
                LOG.warn("Entry expired: {}", entry);
                rr.completeExceptionally(
                    ctx, "Request expired. No response received in " + timeoutSeconds + "s");
                return true;
              }

              return false;
            });
  }

  int getOpenWindowSlots() {
    return openWindowSlots.get();
  }

  @Override
  protected void encode(
      final ChannelHandlerContext ctx,
      final RequestResponse<ResponsePdu> msg,
      final List<Object> out) {
    removeExpired(ctx);

    final var request = msg.request();
    if (window.size() >= windowSize) {
      LOG.warn("Window is full. Size: {}", window.size());
      msg.completeExceptionally(ctx, "Window is full");
    } else {
      window.put(request.sequenceNumber(), msg);
      openWindowSlots.decrementAndGet();
      LOG.debug("Sending request: {}. Window size: {}", msg, window.size());
      out.add(request);
    }
  }

  @Override
  protected void decode(
      final ChannelHandlerContext ctx, final ResponsePdu msg, final List<Object> out) {
    final var rr = window.remove(msg.sequenceNumber());
    if (rr == null) {
      LOG.warn("Unknown response PDU: {}", msg);
      return;
    }

    openWindowSlots.incrementAndGet();
    rr.complete(ctx, msg);
    out.add(msg);

    if (drainingFuture != null) {
      LOG.debug("Receiving response in DRAINING mode. Window size: {}", window.size());
      if (window.size() == 0) {
        drainingFuture.complete(null);
      }
    }

    removeExpired(ctx);
  }

  @Override
  public void userEventTriggered(final ChannelHandlerContext ctx, final Object evt)
      throws Exception {
    if (evt instanceof SmppDrainEvent event && event.type() == SmppState.DRAINING_OUTBOUND) {
      LOG.debug("Receiving draining state change. Window size: {}", window.size());
      if (window.size() == 0) {
        event.future().complete(null);
        drainingFuture = null;
      } else {
        drainingFuture = event.future();
      }
    }

    super.userEventTriggered(ctx, evt);
  }

  @Override
  public void channelUnregistered(final ChannelHandlerContext ctx) throws Exception {
    window.forEach((i, rr) -> rr.completeExceptionally(ctx, "Connection lost"));
    super.channelUnregistered(ctx);
  }
}
