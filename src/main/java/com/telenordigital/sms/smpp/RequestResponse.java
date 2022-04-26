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
