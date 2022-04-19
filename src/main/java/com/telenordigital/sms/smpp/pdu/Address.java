package com.telenordigital.sms.smpp.pdu;

import io.netty.buffer.ByteBuf;

public record Address(byte ton, byte npi, String address) {

  static Address empty() {
    return new Address((byte) 0, (byte) 0, null);
  }

  void writeToBuffer(final ByteBuf buf) {
    buf.writeByte(ton);
    buf.writeByte(npi);
    PduUtil.writeCString(buf, address);
  }

  static Address readFromBuffer(final ByteBuf buf) {
    final byte ton = buf.readByte();
    final byte npi = buf.readByte();
    final String address = PduUtil.readCString(buf);
    return new Address(ton, npi, address);
  }
}
