package com.telenordigital.sms.smpp.pdu;

enum TlvType {
  BYTE,
  BYTE_ARRAY,
  OCTET_STRING, // without null terminator
  C_STRING, // with null terminator
}
