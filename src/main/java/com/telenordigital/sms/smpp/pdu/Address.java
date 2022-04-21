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

public record Address(byte ton, byte npi, String address) {

  static Address empty() {
    return new Address((byte) 0, (byte) 0, null);
  }

  void writeToBuffer(final ByteBuf buf) {
    buf.writeByte(ton);
    buf.writeByte(npi);
    PduUtil.writeCString(buf, address);
  }

  static Address readFromBuffer(final ByteBuf buf) {
    final byte ton = buf.readByte();
    final byte npi = buf.readByte();
    final String address = PduUtil.readCString(buf);
    return new Address(ton, npi, address);
  }
}
