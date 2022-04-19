package com.telenordigital.sms.smpp.pdu;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

public class SubmitSmTest extends PduTest {
  @Test
  public void testSerialize() {
    Sequencer.sequence.set(20456);
    final var pdu = SubmitSm.create(Clock.systemUTC(), "40404", "44951361920", "¡¤#!%&/:", null);

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
}
