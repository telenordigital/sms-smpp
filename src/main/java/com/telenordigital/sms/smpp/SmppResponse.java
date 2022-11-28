package com.telenordigital.sms.smpp;

/*-
 * #%L
 * sms-smpp
 * %%
 * Copyright (C) 2022 Telenor Digital
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

/**
 * @param result
 * @param reference
 * @param message
 * @param details
 * @param destSubAddress The PLMN, aka the MCC and MNC concatenated
 */
public record SmppResponse(
    SmppResultCode result,
    String reference,
    String message,
    String details,
    String destSubAddress) {

  public static SmppResponse success(final String reference) {
    return new SmppResponse(SmppResultCode.SUCCESS, reference, null, null, null);
  }

  public static SmppResponse retriableError(final String message, final String details) {
    return new SmppResponse(SmppResultCode.RETRIABLE_ERROR, null, message, details, null);
  }

  public static SmppResponse routeDown(final String message, final String details) {
    return new SmppResponse(
        SmppResultCode.RETRIABLE_ERROR_ROUTE_DOWN, null, message, details, null);
  }

  public static SmppResponse failure(final String message, final String details) {
    return new SmppResponse(SmppResultCode.GENERAL_FAILURE, null, message, details, null);
  }
}
