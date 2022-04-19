package com.telenordigital.sms.smpp.pdu;

import io.netty.buffer.ByteBuf;

public record Unbind(int commandStatus, int sequenceNumber) implements RequestPdu<UnbindResp> {
  @Override
  public Command command() {
    return Command.UNBIND;
  }

  public Unbind() {
    this(0, Sequencer.next());
  }

  @Override
  public void serialize(final ByteBuf buf) {
    PduUtil.writeHeader(buf, this);
  }

  public static Unbind deserialize(final ByteBuf buf) {
    return new Unbind(buf.readInt(), buf.readInt());
  }

  @Override
  public UnbindResp createResponse(int commandStatus) {
    return new UnbindResp(commandStatus, sequenceNumber);
  }
}
