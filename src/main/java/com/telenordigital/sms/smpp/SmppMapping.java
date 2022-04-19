package com.telenordigital.sms.smpp;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

final class SmppMapping {
  private SmppMapping() {}

  record Status(String name, String message, SmppResultCode result) {
    boolean isSuccessful() {
      return result == SmppResultCode.SUCCESS;
    }
  }

  private static final Map<Integer, Status> statusMap = new HashMap<>();

  static {
    statusMap.put(0x00, new Status("ESME_ROK", null, SmppResultCode.SUCCESS));
    statusMap.put(
        0x01,
        new Status("ESME_RINVMSGLEN", "Message Length is invalid", SmppResultCode.INVALID_REQUEST));
    statusMap.put(
        0x0A,
        new Status("ESME_RINVSRCADR", "Invalid Source Address", SmppResultCode.INVALID_SENDER));
    statusMap.put(
        0x0B,
        new Status("ESME_RINVDSTADR", "Invalid Destination Address", SmppResultCode.INVALID_USER));
    statusMap.put(
        0x14, new Status("ESME_RMSGQFUL", "Message Queue Full", SmppResultCode.RETRIABLE_ERROR));
    statusMap.put(
        0x45, new Status("ESME_RSUBMITFAIL", "Generic failure", SmppResultCode.GENERAL_FAILURE));
    statusMap.put(
        0x58, new Status("ESME_RTHROTTLED", "Throttling error", SmppResultCode.RETRIABLE_ERROR));
    statusMap.put(
        0x61,
        new Status(
            "ESME_RINVSCHED", "Invalid Scheduled Delivery Time", SmppResultCode.INVALID_REQUEST));
    statusMap.put(
        0x62,
        new Status(
            "ESME_RINVEXPIRY",
            "Invalid message validity period (Expiry time)",
            SmppResultCode.INVALID_REQUEST));
    // TODO: add BARRED_USER and ABSENT_SUBSCRIBER mappings
  }

  static Optional<Status> map(final int status) {
    return Optional.ofNullable(statusMap.get(status));
  }

  static Optional<DeliveryState> mapDeliverSm(final byte state) {
    final var result =
        switch (state) {
          case 0 -> DeliveryState.NO_VALUE;
          case 1 -> DeliveryState.ENROUTE;
          case 2 -> DeliveryState.DELIVERED;
          case 3 -> DeliveryState.EXPIRED;
          case 4 -> DeliveryState.DELETED;
          case 5 -> DeliveryState.UNDELIVERABLE;
          case 6 -> DeliveryState.ACCEPTED;
          case 7 -> DeliveryState.UNKNOWN;
          case 8 -> DeliveryState.REJECTED;
          default -> null;
        };

    return Optional.ofNullable(result);
  }
}
