package com.telenordigital.sms.smpp.pdu;

import io.netty.buffer.ByteBuf;

public interface Pdu {
  Command command();

  int commandStatus();

  int sequenceNumber();

  default void serialize(final ByteBuf buf) {
    throw new UnsupportedOperationException();
  }
}
