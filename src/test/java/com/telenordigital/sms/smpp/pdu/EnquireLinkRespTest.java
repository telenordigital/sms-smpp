package com.telenordigital.sms.smpp.pdu;

import static org.assertj.core.api.Assertions.assertThat;

import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

public class EnquireLinkRespTest {
  @Test
  public void testDeserialize() {
    final var encoded = "0000001080000015000000000A342EED";
    final byte[] bytes = ByteBufUtil.decodeHexDump(encoded);

    final var buf = Unpooled.copiedBuffer(bytes);
    // Simulate netty reading the command length and command id
    buf.readBytes(8);

    final EnquireLinkResp resp = EnquireLinkResp.deserialize(buf);

    assertThat(resp.command()).isEqualTo(Command.ENQUIRE_LINK_RESP);
    assertThat(resp.sequenceNumber()).isEqualTo(171192045);
    assertThat(resp.commandStatus()).isEqualTo(0);
  }
}
