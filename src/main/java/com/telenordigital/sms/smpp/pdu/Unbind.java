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

import io.netty.buffer.ByteBuf;

public record Unbind(int commandStatus, int sequenceNumber) implements RequestPdu<UnbindResp> {
  @Override
  public Command command() {
    return Command.UNBIND;
  }

  public Unbind() {
    this(0, Sequencer.next());
  }

  @Override
  public void serialize(final ByteBuf buf) {
    PduUtil.writeHeader(buf, this);
  }

  public static Unbind deserialize(final ByteBuf buf) {
    return new Unbind(buf.readInt(), buf.readInt());
  }

  @Override
  public UnbindResp createResponse(int commandStatus) {
    return new UnbindResp(commandStatus, sequenceNumber);
  }
}
