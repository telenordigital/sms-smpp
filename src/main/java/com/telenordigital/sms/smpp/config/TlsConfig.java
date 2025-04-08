package com.telenordigital.sms.smpp.config;

import java.util.Objects;

public record TlsConfig(
    Boolean enabled,
    SmppConnectionConfig.SslProvider sslProvider,
    byte[] trustedCerts,
    byte[] clientCert,
    byte[] clientKey,
    String expectedServerHostname,
    boolean verifyHostname) {
  public TlsConfig {
    enabled = Objects.requireNonNullElse(enabled, true);

    if (enabled && verifyHostname) {
      Objects.requireNonNull(expectedServerHostname);
    }
  }

  public TlsConfig(final String expectedServerHostname) {
    this(true, null, null, null, null, expectedServerHostname, true);
  }
}
