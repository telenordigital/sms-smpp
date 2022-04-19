package com.telenordigital.sms.smpp.pdu;

import io.netty.buffer.ByteBuf;

public record Bind(
    Command command,
    int commandStatus,
    int sequenceNumber,
    String systemId,
    String password,
    String systemType)
    implements RequestPdu<BindResp> {
  private static final byte interfaceVersion = PduConstants.VERSION_3_4;
  private static final Address address = Address.empty();

  public Bind(Command command, String systemId, String password, String systemType) {
    this(command, 0, Sequencer.next(), systemId, password, systemType);
  }

  @Override
  public void serialize(final ByteBuf buf) {
    // header
    PduUtil.writeHeader(buf, this);

    // body
    PduUtil.writeCString(buf, systemId);
    PduUtil.writeCString(buf, password);
    PduUtil.writeCString(buf, systemType);
    buf.writeByte(interfaceVersion);
    address.writeToBuffer(buf);
  }

  @Override
  public BindResp createResponse(int commandStatus) {
    return new BindResp(command, commandStatus, sequenceNumber, systemId, interfaceVersion);
  }
}
