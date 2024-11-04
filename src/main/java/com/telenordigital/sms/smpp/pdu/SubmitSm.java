package com.telenordigital.sms.smpp.pdu;

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

import com.telenordigital.sms.smpp.charset.GsmCharset;
import io.netty.buffer.ByteBuf;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public record SubmitSm(
    int commandStatus,
    int sequenceNumber,
    Address sender,
    Address destination,
    String validityPeriod,
    byte dataCoding,
    ByteArray encodedShortMessage,
    int maxBytes,
    boolean udhUsed)
    implements RequestPdu<SubmitSmResp> {

  private static final Random random = new Random();

  private static final DateTimeFormatter validityFormatter =
      DateTimeFormatter.ofPattern("uuMMddHHmmssS00+").withZone(ZoneId.of("UTC"));

  @Override
  public Command command() {
    return Command.SUBMIT_SM;
  }

  private static final byte serviceType = 0;
  private static final byte standardEsmClass = 0;
  private static final byte multipartUdhEsmClass = 0x40;
  private static final byte protocolId = 0;
  private static final byte priorityFlag = 0;
  private static final byte registeredDelivery =
      PduConstants.REGISTERED_DELIVERY_SMSC_RECEIPT_REQUESTED;
  private static final byte replaceIfPresent = 0;
  private static final byte smDefaultMessageId = 0;
  private static final byte scheduleDeliveryTime = 0;

  public static List<SubmitSm> create(
      final Clock clock,
      final String sender,
      final String msisdn,
      final String message,
      final Duration validity,
      final boolean splitWithUdh,
      final boolean useNetworkSpecificTonForShortCode) {
    return create(
        clock,
        sender,
        msisdn,
        message,
        validity,
        splitWithUdh,
        () -> (byte) random.nextInt(0xff),
        useNetworkSpecificTonForShortCode);
  }

  static List<SubmitSm> create(
      final Clock clock,
      final String sender,
      final String msisdn,
      final String message,
      final Duration validity,
      final boolean splitWithUdh,
      final Supplier<Byte> referenceGenerator,
      final boolean useNetworkSpecificTonForShortCode) {
    final var charset = getCharset(message);
    final var canUseLatin1 = charset == StandardCharsets.ISO_8859_1;
    final var encodedShortMessage = message.getBytes(charset);
    final var maxBytes = canUseLatin1 ? 159 : 140;
    final int msgCount = 1 + (encodedShortMessage.length / maxBytes);
    final Address senderAddress = getSender(sender, useNetworkSpecificTonForShortCode);
    final Address destination = getDestination(msisdn);
    final String validityPeriod = validityPeriod(clock, validity);
    final byte dataCoding =
        canUseLatin1 ? PduConstants.DATA_CODING_LATIN1 : PduConstants.DATA_CODING_UCS2;
    final ByteArray msgArray = new ByteArray(encodedShortMessage);

    if (msgCount == 1 || !splitWithUdh) {
      return List.of(
          new SubmitSm(
              0,
              Sequencer.next(),
              senderAddress,
              destination,
              validityPeriod,
              dataCoding,
              msgArray,
              maxBytes,
              false));
    } else {
      // Split message using UDH, the SMSC does not support the message_payload TLV
      final byte reference = referenceGenerator.get();
      final var messages = splitMessage(encodedShortMessage, maxBytes);

      return messages.stream()
          .map(
              m ->
                  new SubmitSm(
                      0,
                      Sequencer.next(),
                      senderAddress,
                      destination,
                      validityPeriod,
                      dataCoding,
                      messageWithUdh(reference, messages.size(), m.index + 1, m.message),
                      maxBytes,
                      true))
          .toList();
    }
  }

  record MessagePart(int index, byte[] message) {}

  static List<MessagePart> splitMessage(final byte[] encodedShortMessage, final int maxBytes) {
    final int maxBytesExcludingUdh = maxBytes - 6;
    final int msgCount = 1 + (encodedShortMessage.length / maxBytesExcludingUdh);

    return IntStream.range(0, msgCount)
        .mapToObj(
            i ->
                new MessagePart(
                    i,
                    Arrays.copyOfRange(
                        encodedShortMessage,
                        i * maxBytesExcludingUdh,
                        Math.min((i + 1) * maxBytesExcludingUdh, encodedShortMessage.length))))
        .toList();
  }

  private static ByteArray messageWithUdh(
      final byte reference, final int pduCount, final int partNumber, final byte[] messagePart) {
    final ByteBuffer buf = ByteBuffer.allocate(6 + messagePart.length);
    // Length of the UDH
    buf.put((byte) 0x5);
    // IEI, 0x0 indicates a single byte reference number
    // Ideally we would use 0x8, indicating a two-byte reference number,
    // but not all SMSCs support this.
    buf.put((byte) 0x0);
    // Length of the remaining fields
    buf.put((byte) 0x3);
    // Single byte reference number, must be equal for all parts
    buf.put(reference);
    // Number of parts
    buf.put((byte) pduCount);
    // This part's number in the sequence
    buf.put((byte) partNumber);
    // The actual message
    buf.put(messagePart);
    return new ByteArray(buf.array());
  }

  static String validityPeriod(final Clock clock, final Duration duration) {
    if (duration == null) {
      return "";
    }
    final Instant expiration = clock.instant().plus(duration);
    return validityFormatter.format(expiration);
  }

  static Address getSender(final String sender, boolean useNetworkSpecificTonForShortCode) {
    final byte senderTon =
        sender.matches(".*[a-zA-Z].*")
            ? PduConstants.TON_ALPHANUMERIC
            // Short Code (3 digits to 8 digits in length)
            : useNetworkSpecificTonForShortCode && sender.matches("\\d{3,8}")
                ? PduConstants.TON_NETWORK_SPECIFIC
                : PduConstants.TON_INTERNATIONAL;

    return new Address(
        senderTon,
        senderTon == PduConstants.TON_NETWORK_SPECIFIC
            ? PduConstants.NPI_UNKNOWN
            : PduConstants.NPI_E164,
        sender);
  }

  private static Address getDestination(final String msisdn) {
    return new Address(PduConstants.TON_INTERNATIONAL, PduConstants.NPI_E164, msisdn);
  }

  static Charset getCharset(final String message) {
    // Some SMSCs will convert a Latin1 encoded message to GSM encoding, even if the
    // message is not representable using GSM. This results in any characters that
    // are in Latin1, but not in GSM, to be dropped from the message.
    if (StandardCharsets.ISO_8859_1.newEncoder().canEncode(message)
        && GsmCharset.GSM.canRepresent(message)) {
      return StandardCharsets.ISO_8859_1;
    }

    return StandardCharsets.UTF_16BE;
  }

  @Override
  public void serialize(final ByteBuf buf) {
    PduUtil.writeHeader(buf, this);

    buf.writeByte(serviceType);
    sender.writeToBuffer(buf);
    destination.writeToBuffer(buf);
    buf.writeByte(udhUsed ? multipartUdhEsmClass : standardEsmClass);
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
  public SubmitSmResp createResponse(final int commandStatus) {
    return new SubmitSmResp(commandStatus, sequenceNumber, "TODO", null);
  }
}
