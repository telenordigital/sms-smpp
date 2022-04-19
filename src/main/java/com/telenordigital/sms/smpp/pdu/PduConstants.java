package com.telenordigital.sms.smpp.pdu;

final class PduConstants {
  static final byte VERSION_3_4 = 0x34;

  static final byte REGISTERED_DELIVERY_SMSC_RECEIPT_REQUESTED = 1;

  static final byte DATA_CODING_DEFAULT = 0x00;
  static final byte DATA_CODING_LATIN1 = 0x03;
  static final byte DATA_CODING_UNSPECIFIED = 0x04;
  static final byte DATA_CODING_UCS2 = 0x08;

  static final byte TON_INTERNATIONAL = 0x01;
  static final byte TON_NATIONAL = 0x02;
  static final byte TON_SUBSCRIBER = 0x04;
  static final byte TON_ALPHANUMERIC = 0x05;

  static final byte NPI_E164 = 0x01;
  static final byte NPI_PRIVATE = 0x09;

  private PduConstants() {}
}
