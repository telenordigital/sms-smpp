package com.telenordigital.sms.smpp;

import com.telenordigital.sms.smpp.pdu.Pdu;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

class PduEncoder extends MessageToByteEncoder<Pdu> {

  @Override
  protected void encode(final ChannelHandlerContext ctx, final Pdu msg, final ByteBuf out) {
    msg.serialize(out);
  }
}
