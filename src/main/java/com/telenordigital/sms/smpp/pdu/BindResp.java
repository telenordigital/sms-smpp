package com.telenordigital.sms.smpp.pdu;

import io.netty.buffer.ByteBuf;

public record BindResp(
    Command command, int commandStatus, int sequenceNumber, String systemId, short interfaceVersion)
    implements ResponsePdu {

  public static BindResp deserialize(final Command command, final ByteBuf buf) {
    final int status = buf.readInt();
    final int sequence = buf.readInt();
    final String systemId = PduUtil.readCString(buf);
    final short version =
        PduUtil.readOptionalParams(buf).getByte(TlvTag.SC_INTERFACE_VERSION).orElse((byte) 0);

    return new BindResp(command, status, sequence, systemId, version);
  }
}
