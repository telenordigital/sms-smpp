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

import java.util.EnumSet;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum TlvTag {
  MESSAGE_PAYLOAD((short) 0x0424, TlvType.OCTET_STRING),
  SC_INTERFACE_VERSION((short) 0x0210, TlvType.BYTE),
  RECEIPTED_MESSAGE_ID((short) 0x001e, TlvType.C_STRING),
  MESSAGE_STATE((short) 0x0427, TlvType.BYTE),
  NETWORK_ERROR_CODE((short) 0x0423, TlvType.BYTE_ARRAY),
  SRC_SUBADDRESS((short) 0x0202, TlvType.OCTET_STRING),
  DEST_SUBADDRESS((short) 0x0203, TlvType.OCTET_STRING),
  ;

  static Map<Short, TlvTag> tagMap =
      EnumSet.allOf(TlvTag.class).stream()
          .collect(Collectors.toMap(t -> t.tagId, Function.identity()));

  final short tagId;
  final TlvType type;

  TlvTag(final short tagId, TlvType type) {
    this.tagId = tagId;
    this.type = type;
  }

  static TlvTag valueOf(final short tag) {
    return tagMap.get(tag);
  }
}
