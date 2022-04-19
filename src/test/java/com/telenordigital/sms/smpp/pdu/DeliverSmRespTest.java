package com.telenordigital.sms.smpp.pdu;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class DeliverSmRespTest extends PduTest {
  @Test
  public void testSerialize() {
    final var pdu = new DeliverSmResp(0, 1141447);

    final var hex = serialize(pdu);

    assertThat(hex).isEqualTo("800000050000000000116ac700");
  }
}
