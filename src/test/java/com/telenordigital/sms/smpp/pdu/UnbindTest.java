package com.telenordigital.sms.smpp.pdu;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class UnbindTest extends PduTest {
  @Test
  public void testSerialization() {
    Sequencer.sequence.set(1);
    final var unbind = new Unbind();

    final var hex = serialize(unbind);

    assertThat(hex).isEqualTo("000000060000000000000001");
  }
}
