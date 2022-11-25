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

import java.util.Optional;

/**
 * For source_subaddress or dest_subaddress in deliver_sm
 *
 * @param mcc Mobile country code
 * @param mnc Mobile network code
 */
public record SubAddress(int mcc, int mnc) {
  public static Optional<SubAddress> create(final String input) {
    if (input.length() != 7) {
      return Optional.empty();
    }
    final var result =
        new SubAddress(
            Integer.parseInt(input.substring(1, 4)), Integer.parseInt(input.substring(4)));
    return Optional.of(result);
  }
}
