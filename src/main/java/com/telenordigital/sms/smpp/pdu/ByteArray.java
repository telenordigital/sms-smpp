package com.telenordigital.sms.smpp.pdu;

import io.netty.buffer.ByteBufUtil;

record ByteArray(byte[] array) {
  int length() {
    return array.length;
  }

  @Override
  public String toString() {
    return ByteBufUtil.hexDump(array);
  }
}
