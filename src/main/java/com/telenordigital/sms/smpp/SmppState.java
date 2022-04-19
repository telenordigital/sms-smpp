package com.telenordigital.sms.smpp;

enum SmppState {
  ACTIVE,
  INACTIVE,
  DRAINING_OUTBOUND,
  DRAINING_INBOUND,
  CLOSING,
  CLOSED
}
