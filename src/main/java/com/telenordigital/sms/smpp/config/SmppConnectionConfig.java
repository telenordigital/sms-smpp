package com.telenordigital.sms.smpp.config;

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
    byte[] trustedCerts,
    int windowSize) {

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
        null,
        100);
  }

  public SmppConnectionConfig(String host, int port, int reconnectTimeSeconds) {
    this(host, port, reconnectTimeSeconds, 1);
  }

  public URI connectionUrl() {
    return URI.create(
        String.format("%s://%s@%s:%d", useTls ? "smpps" : "smpp", systemId, host, port));
  }
}
