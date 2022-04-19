package com.telenordigital.sms.smpp.pdu;

import io.netty.buffer.ByteBuf;

public record UnbindResp(int commandStatus, int sequenceNumber) implements ResponsePdu {
  @Override
  public Command command() {
    return Command.UNBIND_RESP;
  }

  @Override
  public void serialize(final ByteBuf buf) {
    PduUtil.writeHeader(buf, this);
  }

  public static UnbindResp deserialize(final ByteBuf buf) {
    return new UnbindResp(buf.readInt(), buf.readInt());
  }
}
