package com.telenordigital.sms.smpp.pdu;

import static org.assertj.core.api.Assertions.assertThat;

import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

public class UnbindRespTest {
  @Test
  public void testDeserialize() {
    final var encoded = "00000010800000060000000000000001";
    final byte[] bytes = ByteBufUtil.decodeHexDump(encoded);

    final var buf = Unpooled.copiedBuffer(bytes);
    // Simulate netty reading the command length and command id
    buf.readBytes(8);

    final UnbindResp resp = UnbindResp.deserialize(buf);

    assertThat(resp.command()).isEqualTo(Command.UNBIND_RESP);
    assertThat(resp.sequenceNumber()).isEqualTo(1);
    assertThat(resp.commandStatus()).isEqualTo(0);
  }
}
