package com.telenordigital.sms.smpp.pdu;

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
