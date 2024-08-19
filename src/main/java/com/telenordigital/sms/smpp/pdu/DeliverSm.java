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
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public record DeliverSm(
    int commandStatus,
    int sequenceNumber,
    Address sender,
    Address destination,
    byte esmClass,
    byte registeredDelivery,
    byte dataCoding,
    ByteArray encodedShortMessage,
    String receiptedMsgId,
    byte state,
    String networkCode,
    Charset defaultCharset,
    String srcSubAddress,
    String concatenatedRef,
    int part,
    int numberOfParts)
    implements RequestPdu<DeliverSmResp> {

  private static final Pattern HEX_MESSAGE_ID_PATTERN = Pattern.compile("id:([A-Fa-f0-9]+) ");
  private static final Pattern MESSAGE_ID_PATTERN = Pattern.compile("id:([0-9]+) ");
  private static final Pattern STATE_PATTERN = Pattern.compile("stat:([A-Z]+)");
  private static final byte DELIVERY_RECEIPT_ESM_CLASS = 0x04;
  private static final byte MULTIPART_MO_ESM_CLASS = 0x40;

  @Override
  public Command command() {
    return Command.DELIVER_SM;
  }

  public boolean isDeliveryReceipt() {
    return esmClass == DELIVERY_RECEIPT_ESM_CLASS;
  }

  public String message() {
    return decodeMessage(encodedShortMessage, dataCoding, defaultCharset)
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "Unsupported charset: " + dataCoding + ", message: " + encodedShortMessage));
  }

  private static Optional<String> decodeMessage(
      final ByteArray encodedShortMessage, final byte dataCoding, final Charset defaultCharset) {
    final Charset charset =
        switch (dataCoding) {
          case PduConstants.DATA_CODING_DEFAULT -> defaultCharset;
          case PduConstants.DATA_CODING_UNSPECIFIED -> GsmCharset.GSM;
          case PduConstants.DATA_CODING_LATIN1 -> StandardCharsets.ISO_8859_1;
          case PduConstants.DATA_CODING_UCS2 -> StandardCharsets.UTF_16;
          default -> null;
        };

    return Optional.ofNullable(charset).map(cs -> new String(encodedShortMessage.array(), cs));
  }

  public static DeliverSm deserialize(final ByteBuf buf, final Charset defaultCharset) {
    final var status = buf.readInt();
    final var sequence = buf.readInt();

    PduUtil.readCString(buf); // serviceType
    final var source = Address.readFromBuffer(buf);
    final var destination = Address.readFromBuffer(buf);
    final byte esmClass = buf.readByte(); // esm_class
    buf.readByte(); // protocol_id
    buf.readByte(); // priority
    buf.readByte(); // schedule_delivery_time, unused
    buf.readByte(); // validity_period, unused
    final var registeredDelivery = buf.readByte();
    buf.readByte(); // replace_if_present_flag, unused
    final var dataCoding = buf.readByte();
    buf.readByte(); // sm_default_msg_id, unused

    final var smLength = buf.readUnsignedByte();
    final var messageBytes = new byte[smLength];
    buf.readBytes(messageBytes);

    final ByteArray messageArray;
    final String concatenatedRef;
    final int numberOfParts;
    final int part;

    if (esmClass == MULTIPART_MO_ESM_CLASS) {
      // The first bytes of the message has UDH data, which needs to be parsed.
      // The SMPP spec does not contain info on how to parse this, but it can
      // be found on Wikipedia:
      // https://en.wikipedia.org/wiki/Concatenated_SMS
      final byte udhLength = messageBytes[0];
      final byte identifier = messageBytes[1];
      if (identifier == 0x0) {
        // single byte identifier
        concatenatedRef = "%X".formatted(messageBytes[3]);
      } else if (identifier == 0x08) {
        // two byte identifier
        concatenatedRef = "%X%X".formatted(messageBytes[3], messageBytes[4]);
      } else {
        concatenatedRef = null;
      }
      numberOfParts = messageBytes[udhLength - 1];
      part = messageBytes[udhLength];

      messageArray =
          new ByteArray(Arrays.copyOfRange(messageBytes, udhLength + 1, messageBytes.length));
    } else {
      concatenatedRef = null;
      numberOfParts = 1;
      part = 1;
      messageArray = new ByteArray(messageBytes);
    }

    final var opts = PduUtil.readOptionalParams(buf);
    final var networkCode =
        opts.getByteArray(TlvTag.NETWORK_ERROR_CODE)
            .map(nc -> "0x%06x".formatted(new BigInteger(1, nc)));

    final var srcSubAddress =
        opts.getString(TlvTag.SRC_SUBADDRESS)
            .filter(s -> s.length() == 6 || s.length() == 7)
            .map(s -> s.substring(1))
            .orElse(null);

    final var idAndState = getMessageIdAndState(opts, messageArray, dataCoding, defaultCharset);

    return new DeliverSm(
        status,
        sequence,
        source,
        destination,
        esmClass,
        registeredDelivery,
        dataCoding,
        messageArray,
        idAndState.messageId(),
        idAndState.state(),
        networkCode.orElse(null),
        defaultCharset,
        srcSubAddress,
        concatenatedRef,
        part,
        numberOfParts);
  }

  record MessageIdAndState(String messageId, byte state) {}

  // Get the ID of the original MT, and the state of the delivery.
  // Will use standard optional parameters, or decode message
  private static MessageIdAndState getMessageIdAndState(
      final TlvMap opts,
      final ByteArray messageArray,
      final byte dataCoding,
      final Charset defaultCharset) {
    final var ref = opts.getString(TlvTag.RECEIPTED_MESSAGE_ID);
    final var stateTlv = opts.getByte(TlvTag.MESSAGE_STATE);

    // Prefer standard optional parameters
    if (ref.isPresent() && stateTlv.isPresent()) {
      return new MessageIdAndState(ref.get(), stateTlv.get());
    }

    final var message = decodeMessage(messageArray, dataCoding, defaultCharset);
    if (message.isEmpty()) {
      return new MessageIdAndState(ref.orElse(null), stateTlv.orElse((byte) 0));
    }

    final var messageId = ref.orElseGet(() -> parseIdFromMessage(message.get()));
    final var state = stateTlv.orElseGet(() -> parseStateFromMessage(message.get()));
    return new MessageIdAndState(messageId, state);
  }

  private static String parseIdFromMessage(final String message) {
    final var matcher = MESSAGE_ID_PATTERN.matcher(message);
    if (matcher.find()) {
      final var decimal = Long.parseLong(matcher.group(1), 10);
      return Long.toString(decimal, 16).toUpperCase(Locale.ROOT);
    }

    final var hexMatcher = HEX_MESSAGE_ID_PATTERN.matcher(message);
    if (hexMatcher.find()) {
      return hexMatcher.group(1);
    }
    return null;
  }

  private static byte parseStateFromMessage(final String message) {
    final var matcher = STATE_PATTERN.matcher(message);
    if (matcher.find()) {
      final var stateName = matcher.group(1);
      return (byte)
          switch (stateName) {
            case "ENROUTE" -> 1;
            case "DELIVRD" -> 2;
            case "EXPIRED" -> 3;
            case "DELETED" -> 4;
            case "UNDELIV" -> 5;
            case "ACCEPTD" -> 6;
            case "UNKNOWN" -> 7;
            case "REJECTD" -> 8;
            default -> 0;
          };
    } else {
      return 0;
    }
  }

  @Override
  public DeliverSmResp createResponse(int commandStatus) {
    return new DeliverSmResp(commandStatus, sequenceNumber);
  }
}
