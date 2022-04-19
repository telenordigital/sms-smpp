package com.telenordigital.sms.smpp.pdu;

import io.netty.buffer.ByteBuf;

public record EnquireLinkResp(int commandStatus, int sequenceNumber) implements ResponsePdu {
  public Command command() {
    return Command.ENQUIRE_LINK_RESP;
  }

  @Override
  public void serialize(ByteBuf buf) {
    PduUtil.writeHeader(buf, this);
  }

  public static EnquireLinkResp deserialize(final ByteBuf buf) {
    return new EnquireLinkResp(buf.readInt(), buf.readInt());
  }
}
