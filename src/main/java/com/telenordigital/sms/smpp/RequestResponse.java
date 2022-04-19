package com.telenordigital.sms.smpp;

import com.telenordigital.sms.smpp.pdu.RequestPdu;
import com.telenordigital.sms.smpp.pdu.ResponsePdu;
import io.netty.channel.ChannelHandlerContext;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

record RequestResponse<T extends ResponsePdu>(
    RequestPdu<T> request, CompletableFuture<T> responseFuture, long startedTimeMillis) {
  public RequestResponse(RequestPdu<T> request) {
    this(request, new CompletableFuture<>(), System.currentTimeMillis());
  }

  CompletableFuture<T> completeExceptionally(
      final ChannelHandlerContext ctx, final String message) {
    return completeExceptionally(ctx.executor(), message);
  }

  CompletableFuture<T> completeExceptionally(final Executor executor, final String message) {
    return responseFuture.completeAsync(
        () -> {
          throw new SmppException(message);
        },
        executor);
  }

  CompletableFuture<T> complete(final ChannelHandlerContext ctx, final T value) {
    return responseFuture.completeAsync(() -> value, ctx.executor());
  }
}
