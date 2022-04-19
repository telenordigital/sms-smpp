package com.telenordigital.sms.smpp;

public record SmppResponse(
    SmppResultCode result, String reference, String message, String details) {

  public static SmppResponse success(final String reference) {
    return new SmppResponse(SmppResultCode.SUCCESS, reference, null, null);
  }

  public static SmppResponse retriableError(final String message, final String details) {
    return new SmppResponse(SmppResultCode.RETRIABLE_ERROR, null, message, details);
  }

  public static SmppResponse routeDown(final String message, final String details) {
    return new SmppResponse(SmppResultCode.RETRIABLE_ERROR_ROUTE_DOWN, null, message, details);
  }

  public static SmppResponse failure(final String message, final String details) {
    return new SmppResponse(SmppResultCode.GENERAL_FAILURE, null, message, details);
  }
}
