package com.telenordigital.sms.smpp.pdu;

import java.util.concurrent.atomic.AtomicInteger;

public final class Sequencer {
  private Sequencer() {}

  static final AtomicInteger sequence = new AtomicInteger(1);

  public static int next() {
    return sequence.getAndIncrement();
  }
}
