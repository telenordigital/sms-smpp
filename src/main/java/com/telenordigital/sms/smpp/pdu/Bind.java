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

public record Bind(
    Command command,
    int commandStatus,
    int sequenceNumber,
    String systemId,
    String password,
    String systemType)
    implements RequestPdu<BindResp> {
  private static final byte interfaceVersion = PduConstants.VERSION_3_4;
  private static final Address address = Address.empty();

  public Bind(Command command, String systemId, String password, String systemType) {
    this(command, 0, Sequencer.next(), systemId, password, systemType);
  }

  @Override
  public void serialize(final ByteBuf buf) {
    // header
    PduUtil.writeHeader(buf, this);

    // body
    PduUtil.writeCString(buf, systemId);
    PduUtil.writeCString(buf, password);
    PduUtil.writeCString(buf, systemType);
    buf.writeByte(interfaceVersion);
    address.writeToBuffer(buf);
  }

  @Override
  public BindResp createResponse(int commandStatus) {
    return new BindResp(command, commandStatus, sequenceNumber, systemId, interfaceVersion);
  }
}
