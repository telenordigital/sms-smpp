package com.telenordigital.sms.smpp.pdu;

import static org.assertj.core.api.Assertions.assertThat;

import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

public class SubmitSmRespTest extends PduTest {
  @Test
  public void testDeserialize() {
    final var encoded = "0000001C80000004000000000A342EE1393432353834333135393400";
    final byte[] bytes = ByteBufUtil.decodeHexDump(encoded);

    final var buf = Unpooled.copiedBuffer(bytes);
    // Simulate netty reading the command length and command id
    buf.readBytes(8);

    final SubmitSmResp resp = SubmitSmResp.deserialize(buf);
    assertThat(resp.commandStatus()).isEqualTo(0);
    assertThat(resp.sequenceNumber()).isEqualTo(171192033);
    assertThat(resp.messageId()).isEqualTo("94258431594");
  }

  @Test
  public void testDeserializeFailed() {
    final var encoded = "00000010800000040000000b00019dba";
    final byte[] bytes = ByteBufUtil.decodeHexDump(encoded);

    final var buf = Unpooled.copiedBuffer(bytes);
    // Simulate netty reading the command length and command id
    buf.readBytes(8);

    final SubmitSmResp resp = SubmitSmResp.deserialize(buf);
    assertThat(resp.commandStatus()).isEqualTo(0xb);
    assertThat(resp.sequenceNumber()).isEqualTo(105914);
    assertThat(resp.messageId()).isNull();
  }
}
