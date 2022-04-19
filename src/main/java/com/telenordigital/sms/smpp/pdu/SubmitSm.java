package com.telenordigital.sms.smpp.pdu;

import io.netty.buffer.ByteBuf;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public record SubmitSm(
    int commandStatus,
    int sequenceNumber,
    Address sender,
    Address destination,
    String validityPeriod,
    byte dataCoding,
    ByteArray encodedShortMessage,
    int maxBytes)
    implements RequestPdu<SubmitSmResp> {

  private static final DateTimeFormatter validityFormatter =
      DateTimeFormatter.ofPattern("uuMMddHHmmssS00+").withZone(ZoneId.of("UTC"));

  @Override
  public Command command() {
    return Command.SUBMIT_SM;
  }

  private static final byte serviceType = 0;
  private static final byte esmClass = 0;
  private static final byte protocolId = 0;
  private static final byte priorityFlag = 0;
  private static final byte registeredDelivery =
      PduConstants.REGISTERED_DELIVERY_SMSC_RECEIPT_REQUESTED;
  private static final byte replaceIfPresent = 0;
  private static final byte smDefaultMessageId = 0;
  private static final byte scheduleDeliveryTime = 0;

  public static SubmitSm create(
      final Clock clock,
      final String sender,
      final String msisdn,
      final String message,
      final Duration validity) {
    final var charset = getCharset(message);
    final var canUseLatin1 = charset == StandardCharsets.ISO_8859_1;
    final var encodedShortMessage = message.getBytes(charset);
    return new SubmitSm(
        0,
        Sequencer.next(),
        getSender(sender),
        getDestination(msisdn),
        validityPeriod(clock, validity),
        canUseLatin1 ? PduConstants.DATA_CODING_LATIN1 : PduConstants.DATA_CODING_UCS2,
        new ByteArray(encodedShortMessage),
        canUseLatin1 ? 160 : 140);
  }

  static String validityPeriod(final Clock clock, final Duration duration) {
    if (duration == null) {
      return "";
    }
    final Instant expiration = clock.instant().plus(duration);
    return validityFormatter.format(expiration);
  }

  private static Address getSender(final String sender) {
    final byte senderTon =
        sender.matches(".*[a-zA-Z].*")
            ? PduConstants.TON_ALPHANUMERIC
            : PduConstants.TON_INTERNATIONAL;

    return new Address(senderTon, PduConstants.NPI_E164, sender);
  }

  private static Address getDestination(final String msisdn) {
    return new Address(PduConstants.TON_INTERNATIONAL, PduConstants.NPI_E164, msisdn);
  }

  private static Charset getCharset(final String message) {
    if (StandardCharsets.ISO_8859_1.newEncoder().canEncode(message)) {
      return StandardCharsets.ISO_8859_1;
    }

    for (int i = 0; i < message.length(); i++) {
      if (Character.isHighSurrogate(message.charAt(i))) {
        throw new IllegalArgumentException("Message contents is outside the BMP");
      }
    }
    return StandardCharsets.UTF_16;
  }

  @Override
  public void serialize(final ByteBuf buf) {
    PduUtil.writeHeader(buf, this);

    buf.writeByte(serviceType);
    sender.writeToBuffer(buf);
    destination.writeToBuffer(buf);
    buf.writeByte(esmClass);
    buf.writeByte(protocolId);
    buf.writeByte(priorityFlag);
    buf.writeByte(scheduleDeliveryTime);
    PduUtil.writeCString(buf, validityPeriod);
    buf.writeByte(registeredDelivery);
    buf.writeByte(replaceIfPresent);
    buf.writeByte(dataCoding);
    buf.writeByte(smDefaultMessageId);
    if (encodedShortMessage.length() <= maxBytes) {
      // The message fits in one SMS
      buf.writeByte(encodedShortMessage.length());
      buf.writeBytes(encodedShortMessage.array());
    } else {
      // The message must be split. This is handled automatically
      // by the SMPP server, but we have to put the message in a
      // TLV field
      buf.writeByte(0); // length of SM
      PduUtil.writeTlv(buf, TlvTag.MESSAGE_PAYLOAD.tagId, encodedShortMessage.array());
    }
  }

  @Override
  public SubmitSmResp createResponse(int commandStatus) {
    return new SubmitSmResp(commandStatus, sequenceNumber, "TODO");
  }
}
