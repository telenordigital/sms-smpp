package com.telenordigital.sms.smpp.pdu;

/*-
 * #%L
 * sms-smpp
 * %%
 * Copyright (C) 2022 Telenor Digital
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

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
  public void testSubAddress() {
    final var encoded = "0000002480000004000000000000000336453834464330340002030007a0323432303130";
    final byte[] bytes = ByteBufUtil.decodeHexDump(encoded);

    final var buf = Unpooled.copiedBuffer(bytes);
    // Simulate netty reading the command length and command id
    buf.readBytes(8);

    final SubmitSmResp resp = SubmitSmResp.deserialize(buf);
    assertThat(resp.commandStatus()).isEqualTo(0);
    assertThat(resp.messageId()).isEqualTo("6E84FC04");
    assertThat(resp.destSubAddress()).isEqualTo("242010");
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
