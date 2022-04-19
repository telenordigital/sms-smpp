package com.telenordigital.sms.smpp.config;

import com.telenordigital.sms.smpp.charset.GsmCharset;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public enum DefaultEncoding {
  GSM(GsmCharset.GSM),
  LATIN1(StandardCharsets.ISO_8859_1);

  public final Charset charset;

  DefaultEncoding(final Charset charset) {
    this.charset = charset;
  }
}
