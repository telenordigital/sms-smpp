package com.telenordigital.sms.smpp.charset;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class GsmCharsetTest {
  private static GsmCharset gsm;

  @BeforeAll
  public static void beforeAll() {
    gsm = new GsmCharset();
  }

  @Test
  public void testEncodeDecode() {
    final var original = "hei";
    final byte[] encoded = original.getBytes(gsm);

    final String back = new String(encoded, gsm);

    assertThat(original).isEqualTo(back);
  }

  @Test
  public void testCanRepresent() {
    assertThat(gsm.canRepresent("hei")).isTrue();
    assertThat(gsm.canRepresent("ø")).isTrue();
    assertThat(gsm.canRepresent("ภาษาไทย")).isFalse();
  }
}
