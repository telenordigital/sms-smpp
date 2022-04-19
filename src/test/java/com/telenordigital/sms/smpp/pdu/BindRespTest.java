package com.telenordigital.sms.smpp.pdu;

import static org.assertj.core.api.Assertions.assertThat;

import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

public class BindRespTest {
  // Test strings are from cloudhopper
  @Test
  public void testDeserialize() {
    final var encoded = "0000001F800000090000000000039951536D73632053696D756C61746F7200";
    final byte[] bytes = ByteBufUtil.decodeHexDump(encoded);

    final var buf = Unpooled.copiedBuffer(bytes);
    // Simulate netty reading the command length and command id
    buf.readBytes(8);

    final BindResp resp = BindResp.deserialize(Command.BIND_TRANCEIVER_RESP, buf);

    assertThat(resp.command()).isEqualTo(Command.BIND_TRANCEIVER_RESP);
    assertThat(resp.systemId()).isEqualTo("Smsc Simulator");
    assertThat(resp.sequenceNumber()).isEqualTo(235857);
    assertThat(resp.commandStatus()).isEqualTo(0);
  }

  @Test
  public void testDeserializeWithOptional() {
    final var encoded = "0000001D80000001000000000003996274776974746572000210000134";
    final byte[] bytes = ByteBufUtil.decodeHexDump(encoded);

    final var buf = Unpooled.copiedBuffer(bytes);
    // Simulate netty reading the command length and command id
    buf.readBytes(8);

    final BindResp resp = BindResp.deserialize(Command.BIND_RECEIVER_RESP, buf);

    assertThat(resp.command()).isEqualTo(Command.BIND_RECEIVER_RESP);
    assertThat(resp.systemId()).isEqualTo("twitter");
    assertThat(resp.sequenceNumber()).isEqualTo(235874);
    assertThat(resp.commandStatus()).isEqualTo(0);
    assertThat(resp.interfaceVersion()).isEqualTo((byte) 0x34);
  }

  @Test
  public void bindFailed() {
    final var encoded = "00000010800000090000000d00000001";
    final byte[] bytes = ByteBufUtil.decodeHexDump(encoded);
    final var buf = Unpooled.copiedBuffer(bytes);
    buf.readBytes(8);
    BindResp.deserialize(Command.BIND_RECEIVER_RESP, buf);
  }

  @Test
  public void bindFailedCloudhopper() {
    final var encoded = "00000021800000090000000d00000001636c6f7564686f70706572000210000134";
    final byte[] bytes = ByteBufUtil.decodeHexDump(encoded);
    final var buf = Unpooled.copiedBuffer(bytes);
    buf.readBytes(8);
    BindResp.deserialize(Command.BIND_RECEIVER_RESP, buf);
  }
}
