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

import com.telenordigital.sms.smpp.pdu.BindResp;
import com.telenordigital.sms.smpp.pdu.Command;
import com.telenordigital.sms.smpp.pdu.DeliverSm;
import com.telenordigital.sms.smpp.pdu.EnquireLink;
import com.telenordigital.sms.smpp.pdu.EnquireLinkResp;
import com.telenordigital.sms.smpp.pdu.Pdu;
import com.telenordigital.sms.smpp.pdu.SubmitSmResp;
import com.telenordigital.sms.smpp.pdu.Unbind;
import com.telenordigital.sms.smpp.pdu.UnbindResp;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PduDecoder extends ByteToMessageDecoder {
  private final Charset defaultCharset;

  PduDecoder(final Charset defaultCharset) {
    this.defaultCharset = defaultCharset;
  }

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Override
  protected void decode(final ChannelHandlerContext ctx, final ByteBuf in, final List<Object> out) {
    final var id = in.readInt();
    Command.valueOf(id)
        .ifPresentOrElse(
            command -> {
              final var pdu = mapPdu(command, in);
              out.add(pdu);
              if (in.readableBytes() > 0) {
                LOG.warn(
                    "Finished reading PDU {}, but there are still {} readable bytes left",
                    Integer.toHexString(id),
                    in.readableBytes());
                skipReadableBytes(in);
              }
            },
            () -> {
              // TODO: reply with unknown command response
              LOG.warn(
                  "Unknown command: {}, skipping remaining {} bytes",
                  Integer.toHexString(id),
                  in.readableBytes());
              // skip the rest
              skipReadableBytes(in);
            });
  }

  private Pdu mapPdu(final Command command, final ByteBuf in) {
    switch (command) {
      case BIND_RECEIVER_RESP:
      case BIND_TRANCEIVER_RESP:
      case BIND_TRANSMITTER_RESP:
        return BindResp.deserialize(command, in);
      case DELIVER_SM:
        return DeliverSm.deserialize(in, defaultCharset);
      case UNBIND_RESP:
        return UnbindResp.deserialize(in);
      case ENQUIRE_LINK_RESP:
        return EnquireLinkResp.deserialize(in);
      case ENQUIRE_LINK:
        return EnquireLink.deserialize(in);
      case UNBIND:
        return Unbind.deserialize(in);
      case SUBMIT_SM_RESP:
        return SubmitSmResp.deserialize(in);
      default:
        skipReadableBytes(in);
        throw new IllegalArgumentException("Unsupported command: " + command);
    }
  }

  private static void skipReadableBytes(final ByteBuf in) {
    in.skipBytes(in.readableBytes());
  }
}
