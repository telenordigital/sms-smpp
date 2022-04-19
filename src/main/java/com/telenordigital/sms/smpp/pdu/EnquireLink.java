package com.telenordigital.sms.smpp.pdu;

import io.netty.buffer.ByteBuf;

public record EnquireLink(int commandStatus, int sequenceNumber)
    implements RequestPdu<EnquireLinkResp> {
  @Override
  public Command command() {
    return Command.ENQUIRE_LINK;
  }

  public EnquireLink() {
    this(0, Sequencer.next());
  }

  @Override
  public void serialize(final ByteBuf buf) {
    PduUtil.writeHeader(buf, this);
  }

  public static EnquireLink deserialize(final ByteBuf buf) {
    return new EnquireLink(buf.readInt(), buf.readInt());
  }

  @Override
  public EnquireLinkResp createResponse(int commandStatus) {
    return new EnquireLinkResp(commandStatus, sequenceNumber);
  }
}
