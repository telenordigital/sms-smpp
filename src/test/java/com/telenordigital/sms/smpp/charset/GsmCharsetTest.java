package com.telenordigital.sms.smpp.charset;

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
