package com.telenordigital.sms.smpp.config;

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

import java.util.Objects;

public record TlsConfig(
    SmppConnectionConfig.SslProvider sslProvider,
    byte[] trustedCerts,
    byte[] clientCert,
    byte[] clientKey,
    String expectedServerHostname,
    boolean verifyHostname) {
  public TlsConfig {
    if (verifyHostname) {
      Objects.requireNonNull(expectedServerHostname);
    }
  }

  public TlsConfig(final String expectedServerHostname) {
    this(null, null, null, null, expectedServerHostname, true);
  }
}
