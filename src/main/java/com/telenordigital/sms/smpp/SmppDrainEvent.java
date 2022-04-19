package com.telenordigital.sms.smpp;

import java.util.concurrent.CompletableFuture;

record SmppDrainEvent(SmppState type, CompletableFuture<Void> future) {
  SmppDrainEvent(SmppState type) {
    this(type, new CompletableFuture<>());
  }
}
