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

import java.net.URI;

public record SmppConnectionConfig(
    String host,
    int port,
    int numberOfBinds,
    BindType bindType,
    // Specify which encoding should be used when we get a PDU with "default (0x00)" encoding
    DefaultEncoding defaultEncoding,
    int reconnectTimeSeconds,
    // sends enquire link if there has not been any reads or writes
    int idleTimeSeconds,
    // closes the connection if there has not been any reads
    int requestTimeoutSeconds,
    // graceful shutdown
    int shutdownTimeoutSeconds,
    // timeout of handing inbound requests
    int handlerTimeoutSeconds,
    String systemId,
    String password,
    String systemType,
    boolean useTls,
    SslProvider sslProvider,
    boolean splitWithUdh,
    byte[] trustedCerts,
    byte[] clientCert,
    byte[] clientKey,
    int windowSize,
    boolean useNetworkSpecificTonForShortCode) {

  public SmppConnectionConfig(String host, int port, int reconnectTimeSeconds, int numberOfBinds) {
    this(
        host,
        port,
        numberOfBinds,
        BindType.TRANSCEIVER,
        DefaultEncoding.LATIN1,
        reconnectTimeSeconds,
        10,
        60,
        60,
        60,
        null,
        null,
        null,
        false,
        SslProvider.JDK,
        false,
        null,
        null,
        null,
        100,
        false);
  }

  public enum SslProvider {
    JDK,
    OPENSSL
  }

  public SmppConnectionConfig(String host, int port, int reconnectTimeSeconds) {
    this(host, port, reconnectTimeSeconds, 1);
  }

  public URI connectionUrl() {
    return URI.create(
        String.format("%s://%s@%s:%d", useTls ? "smpps" : "smpp", systemId, host, port));
  }
}
