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
import java.nio.charset.StandardCharsets;

final class PduUtil {
  private PduUtil() {}

  static void writeCString(final ByteBuf buf, final String string) {
    if (string != null) {
      buf.writeBytes(string.getBytes(StandardCharsets.US_ASCII));
    }
    buf.writeByte(0);
  }

  static String readOctetString(final ByteBuf buf, final int length) {
    final byte[] bytes = new byte[length];
    buf.readBytes(bytes);
    return new String(bytes, StandardCharsets.US_ASCII);
  }

  static String readCString(final ByteBuf buf) {
    final int length = buf.bytesBefore((byte) 0);
    if (length == -1) {
      return null;
    }
    final var result = readOctetString(buf, length);

    // Read and ignore the terminator
    buf.readByte();
    return result;
  }

  static void writeHeader(final ByteBuf buf, final Pdu pdu) {
    // write header
    buf.writeInt(pdu.command().id());
    buf.writeInt(pdu.commandStatus());
    buf.writeInt(pdu.sequenceNumber());
  }

  static void writeTlv(final ByteBuf buf, final short tlvId, final byte[] value) {
    buf.writeShort(tlvId);
    buf.writeShort(value.length);
    buf.writeBytes(value);
  }

  static TlvMap readOptionalParams(final ByteBuf buf) {
    final var params = new TlvMap();
    while (buf.readableBytes() > 0) {
      final short tag = buf.readShort();
      final short length = buf.readShort(); // length
      final var tlv = TlvTag.valueOf(tag);

      if (tlv == null) {
        // An unsupported optional parameter
        // We are ignoring it, skipping ahead
        buf.skipBytes(length);
        continue;
      }

      switch (tlv.type) {
        case BYTE -> params.putByte(tlv, buf.readByte());
        case OCTET_STRING -> params.putString(tlv, PduUtil.readOctetString(buf, length));
        case C_STRING -> params.putString(tlv, PduUtil.readCString(buf));
        case BYTE_ARRAY -> {
          final var bytes = new byte[length];
          buf.readBytes(bytes);
          params.putByteArray(tlv, bytes);
        }
        default -> throw new IllegalArgumentException("Unsupported TLV type " + tlv.type);
      }
    }
    return params;
  }
}
