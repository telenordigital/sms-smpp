package com.telenordigital.sms.smpp.pdu;

import io.netty.buffer.ByteBuf;

public record DeliverSmResp(int commandStatus, int sequenceNumber) implements ResponsePdu {

  @Override
  public Command command() {
    return Command.DELIVER_SM_RESP;
  }

  @Override
  public void serialize(final ByteBuf buf) {
    PduUtil.writeHeader(buf, this);

    buf.writeByte(0); // message_id, unused
  }
}
