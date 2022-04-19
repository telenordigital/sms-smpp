package com.telenordigital.sms.smpp.pdu;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class TlvMap {
  private static final String TYPE_MISMATCH = "TLV type mismatch";
  public static final Set<TlvType> STRING_TYPES = Set.of(TlvType.OCTET_STRING, TlvType.C_STRING);

  private final Map<TlvTag, Byte> bytes = new EnumMap<>(TlvTag.class);
  private final Map<TlvTag, byte[]> byteArrays = new EnumMap<>(TlvTag.class);
  private final Map<TlvTag, String> strings = new EnumMap<>(TlvTag.class);

  public void putByte(final TlvTag tag, final byte value) {
    if (tag.type != TlvType.BYTE) {
      throw new IllegalArgumentException(TYPE_MISMATCH);
    }
    bytes.put(tag, value);
  }

  public void putByteArray(final TlvTag tag, final byte[] value) {
    if (tag.type != TlvType.BYTE_ARRAY) {
      throw new IllegalArgumentException(TYPE_MISMATCH);
    }
    byteArrays.put(tag, value);
  }

  public void putString(final TlvTag tag, final String value) {
    if (!STRING_TYPES.contains(tag.type)) {
      throw new IllegalArgumentException(TYPE_MISMATCH);
    }
    strings.put(tag, value);
  }

  public Optional<Byte> getByte(final TlvTag tag) {
    if (tag.type != TlvType.BYTE) {
      throw new IllegalArgumentException(TYPE_MISMATCH);
    }
    return Optional.ofNullable(bytes.get(tag));
  }

  public Optional<byte[]> getByteArray(final TlvTag tag) {
    if (tag.type != TlvType.BYTE_ARRAY) {
      throw new IllegalArgumentException(TYPE_MISMATCH);
    }
    return Optional.ofNullable(byteArrays.get(tag));
  }

  public Optional<String> getString(final TlvTag tag) {
    if (!STRING_TYPES.contains(tag.type)) {
      throw new IllegalArgumentException(TYPE_MISMATCH);
    }
    return Optional.ofNullable(strings.get(tag));
  }
}
