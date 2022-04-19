package com.telenordigital.sms.smpp;

import java.time.Duration;

public record SmppSmsMt(String sender, String msisdn, String message, Duration validityPeriod) {}
