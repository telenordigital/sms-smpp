package com.telenordigital.sms.smpp.pdu;

import io.netty.buffer.ByteBuf;

public record SubmitSmResp(int commandStatus, int sequenceNumber, String messageId)
    implements ResponsePdu {
  @Override
  public Command command() {
    return Command.SUBMIT_SM_RESP;
  }

  public static SubmitSmResp deserialize(final ByteBuf buf) {
    final int status = buf.readInt();
    final int sequence = buf.readInt();
    final String messageId = PduUtil.readCString(buf);
    return new SubmitSmResp(status, sequence, messageId);
  }
}
