package com.telenordigital.sms.smpp.pdu;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class EnquireLinkTest extends PduTest {
  @Test
  public void testSerialize() {
    Sequencer.sequence.set(171192039);
    final var enquireLink = new EnquireLink();

    final var hex = serialize(enquireLink);

    assertThat(hex).isEqualTo("00000015000000000a342ee7");
  }
}
