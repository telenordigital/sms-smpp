package com.telenordigital.sms.smpp;

public record SmppDeliveryReceipt(String messageId, DeliveryState result, String networkCode) {}
