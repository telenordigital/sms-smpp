package com.telenordigital.sms.smpp.pdu;

import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

public class PduTest {
  protected static String serialize(final Pdu pdu) {
    final var buf = Unpooled.buffer();
    pdu.serialize(buf);
    final var length = buf.readableBytes();
    final var bytes = new byte[length];
    buf.readBytes(bytes);
    return ByteBufUtil.hexDump(bytes);
  }
}
