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

public record EnquireLink(int commandStatus, int sequenceNumber)
    implements RequestPdu<EnquireLinkResp> {
  @Override
  public Command command() {
    return Command.ENQUIRE_LINK;
  }

  public EnquireLink() {
    this(0, Sequencer.next());
  }

  @Override
  public void serialize(final ByteBuf buf) {
    PduUtil.writeHeader(buf, this);
  }

  public static EnquireLink deserialize(final ByteBuf buf) {
    return new EnquireLink(buf.readInt(), buf.readInt());
  }

  @Override
  public EnquireLinkResp createResponse(int commandStatus) {
    return new EnquireLinkResp(commandStatus, sequenceNumber);
  }
}
