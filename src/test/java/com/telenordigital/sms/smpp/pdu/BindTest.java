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

import org.junit.jupiter.api.Test;

public class BindTest extends PduTest {
  // Test strings are from cloudhopper
  @Test
  public void testSerialization() {
    Sequencer.sequence.set(235871);
    final var bind = new Bind(Command.BIND_TRANSMITTER, "twitter", "twitter", "");
    assertThat(bind.sequenceNumber()).isEqualTo(235871);
    // Make sure it doesn't increase when calling get
    assertThat(bind.sequenceNumber()).isEqualTo(235871);
    final var hex = serialize(bind);

    assertThat(hex).isEqualTo("00000002000000000003995f747769747465720074776974746572000034000000");
  }

  private String nextSerializedSequence() {
    final var bind = new Bind(Command.BIND_RECEIVER, "", "", "");
    final String hex = serialize(bind);
    return hex.substring(16, 24);
  }

  @Test
  public void sequenceNumberWrap() {
    Sequencer.sequence.set(0);
    assertThat(nextSerializedSequence()).isEqualTo("00000000");
    assertThat(nextSerializedSequence()).isEqualTo("00000001");
    Sequencer.sequence.set(255);
    assertThat(nextSerializedSequence()).isEqualTo("000000ff");
    assertThat(nextSerializedSequence()).isEqualTo("00000100");

    Sequencer.sequence.set(Integer.MAX_VALUE - 1);
    assertThat(nextSerializedSequence()).isEqualTo("7ffffffe");
    assertThat(nextSerializedSequence()).isEqualTo("7fffffff");
    assertThat(nextSerializedSequence()).isEqualTo("80000000");
    Sequencer.sequence.set(-2);
    assertThat(nextSerializedSequence()).isEqualTo("fffffffe");
    assertThat(nextSerializedSequence()).isEqualTo("ffffffff");
    assertThat(nextSerializedSequence()).isEqualTo("00000000");
  }
}
