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
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

public class PduTest {
  protected static String serialize(final Pdu pdu) {
    final var buf = Unpooled.buffer();
    pdu.serialize(buf);
    return serialize(buf);
  }

  protected static String serialize(final ByteArray byteArray) {
    final var buf = Unpooled.buffer();
    buf.writeBytes(byteArray.array());
    return serialize(buf);
  }

  private static String serialize(final ByteBuf buf) {
    final var length = buf.readableBytes();
    final var bytes = new byte[length];
    buf.readBytes(bytes);
    return ByteBufUtil.hexDump(bytes);
  }
}
