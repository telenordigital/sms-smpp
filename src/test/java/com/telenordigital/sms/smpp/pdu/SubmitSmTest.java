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
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

public class SubmitSmTest extends PduTest {

  @Test
  public void testBom() {
    final var pdus =
        SubmitSm.create(
            Clock.systemUTC(), "40404", "44951361920", "седмичен абонамент", null, false, false);
    assertThat(pdus).hasSize(1);
    final var pdu = pdus.get(0);
    final var msg = pdu.encodedShortMessage();
    final var hex = serialize(msg);
    assertThat(hex).doesNotStartWith("feff");
  }

  @Test
  public void emoji() {
    assertThat(SubmitSm.getCharset("only english")).isEqualTo(StandardCharsets.ISO_8859_1);
    assertThat(SubmitSm.getCharset("scandinavian: æøå")).isEqualTo(StandardCharsets.ISO_8859_1);
    assertThat(SubmitSm.getCharset("latin1, but not gsm: ó")).isEqualTo(StandardCharsets.UTF_16BE);
    assertThat(SubmitSm.getCharset("With quote \"STOP\"")).isEqualTo(StandardCharsets.ISO_8859_1);

    final var charset = SubmitSm.getCharset("Hey 😬");
    assertThat(charset).isEqualTo(StandardCharsets.UTF_16BE);
    final var encoded = "Hey 😬".getBytes(charset);
    assertThat(ByteBufUtil.hexDump(encoded)).isEqualTo("0048006500790020d83dde2c");
  }

  @Test
  public void testSplitUnicode() {
    Sequencer.sequence.set(20456);
    final var pdus =
        SubmitSm.create(
            Clock.systemUTC(),
            "40404",
            "44951361920",
            "For thousands of years, mathematicians have attempted to extend their understanding of π, sometimes by computing its value to a high degree oπ accuracy. Ancient civilizations, including the Egyptians and Babylonians, required fairly accurate approximations of π for practical computations.",
            null,
            true,
            () -> (byte) 0x41,
            false);
    assertThat(pdus).hasSize(5);
    final var part1 = pdus.get(0);
    final var part2 = pdus.get(1);

    final var hex1 = serialize(part1);
    final var hex2 = serialize(part2);

    final var udh1 = hex1.substring(90, 102);
    final var udh2 = hex2.substring(90, 102);

    final var message1 = hex1.substring(102);
    final var message2 = hex2.substring(102);

    assertThat(part1.encodedShortMessage().length()).isEqualTo(140);
    assertThat(part2.encodedShortMessage().length()).isEqualTo(140);

    assertThat(udh1).isEqualTo("050003410501");
    assertThat(udh2).isEqualTo("050003410502");

    assertThat(message1).startsWith("00");
    assertThat(message1).doesNotEndWith("00");
    assertThat(message2).startsWith("00");
    assertThat(message2).doesNotEndWith("00");
  }

  @Test
  public void testSerializeLongUdh() {
    Sequencer.sequence.set(20456);
    final var pdus =
        SubmitSm.create(
            Clock.systemUTC(),
            "40404",
            "44951361920",
            "part1" + "a".repeat(149) + "part2" + "b".repeat(10),
            null,
            true,
            () -> (byte) 0x41,
            false);
    assertThat(pdus).hasSize(2);
    final var part1 = pdus.get(0);
    final var part2 = pdus.get(1);

    final var hex1 = serialize(part1);
    final var hex2 = serialize(part2);

    final var udh1 = hex1.substring(90, 102);
    final var udh2 = hex2.substring(90, 102);

    assertThat(part1.encodedShortMessage().length()).isEqualTo(160);
    assertThat(part2.encodedShortMessage().length()).isEqualTo(21);

    assertThat(udh1).isEqualTo("050003410201");
    assertThat(udh2).isEqualTo("050003410202");

    assertThat(hex1)
        .isEqualTo(
            "000000040000000000004fe80001013430343034000101343439353133363139323000400000000001000300a005000341020170617274316161616161616161616161616161616161616161616161616161616161616161616161616161616161616161616161616161616161616161616161616161616161616161616161616161616161616161616161616161616161616161616161616161616161616161616161616161616161616161616161616161616161616161616161616161616161616161616161616161616161");

    assertThat(hex2)
        .isEqualTo(
            "000000040000000000004fe9000101343034303400010134343935313336313932300040000000000100030015050003410202706172743262626262626262626262");
  }

  @Test
  public void splitMessage() {
    final var latin1 = StandardCharsets.ISO_8859_1;

    final var msg1 = "aa";
    final var split1 = SubmitSm.splitMessage(msg1.getBytes(latin1), 160);
    assertThat(split1).hasSize(1);
    assertThat(split1.get(0).message()).isEqualTo(new byte[] {0x61, 0x61});

    final var msg2 = "123456789 123456789";
    final var split2 = SubmitSm.splitMessage(msg2.getBytes(latin1), 9);
    assertThat(split2).hasSize(7);
    assertThat(split2.get(0).message()).isEqualTo(new byte[] {0x31, 0x32, 0x33});
    assertThat(split2.get(1).message()).isEqualTo(new byte[] {0x34, 0x35, 0x36});
    assertThat(split2.get(2).message()).isEqualTo(new byte[] {0x37, 0x38, 0x39});
    assertThat(split2.get(6).message()).isEqualTo(new byte[] {0x39});
  }

  @Test
  public void testSerialize() {
    Sequencer.sequence.set(20456);
    final var pdus =
        SubmitSm.create(Clock.systemUTC(), "40404", "44951361920", "¡¤#!%&/:", null, true, false);
    assertThat(pdus).hasSize(1);
    final var pdu = pdus.get(0);

    final var hex = serialize(pdu);

    assertThat(hex)
        .isEqualTo(
            "000000040000000000004fe80001013430343034000101343439353133363139323"
                + "00000000000000100030008a1a4232125262f3a");
  }

  @Test
  public void validityPeriod() {
    final var clock = Clock.fixed(Instant.parse("2021-04-22T13:53:40Z"), ZoneId.of("UTC"));

    assertThat(SubmitSm.validityPeriod(clock, Duration.ofMinutes(30)))
        .isEqualTo("210422142340000+")
        .hasSize(16);
  }

  @Test
  public void testNetworkSpecificTon() {
    Sequencer.sequence.set(20456);
    final var pdus =
        SubmitSm.create(Clock.systemUTC(), "40404", "44951361920", "¡¤#!%&/:", null, true, true);
    assertThat(pdus).hasSize(1);
    final var pdu = pdus.get(0);

    final var hex = serialize(pdu);

    assertThat(hex)
        .isEqualTo(
            "000000040000000000004fe8000300343034303400010134343935313336313932300000000000000100030008a1a4232125262f3a");
  }

  @Test
  void getSender() {
    final var sender0 = SubmitSm.getSender("47999990901", false);
    assertThat(sender0.ton()).isEqualTo(PduConstants.TON_INTERNATIONAL);
    assertThat(sender0.npi()).isEqualTo(PduConstants.NPI_E164);
    assertThat(SubmitSm.getSender("47999990901", true).ton())
        .isEqualTo(PduConstants.TON_INTERNATIONAL);
    assertThat(SubmitSm.getSender("300000", false).ton()).isEqualTo(PduConstants.TON_INTERNATIONAL);
    final var sender = SubmitSm.getSender("300000", true);
    assertThat(sender.ton()).isEqualTo(PduConstants.TON_NETWORK_SPECIFIC);
    assertThat(sender.npi()).isEqualTo(PduConstants.NPI_UNKNOWN);
  }
}
