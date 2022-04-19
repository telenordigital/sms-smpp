module com.telenordigital.sms.smpp {
  requires io.netty.codec;
  requires io.netty.transport;
  requires io.netty.buffer;
  requires io.netty.handler;
  requires io.netty.common;
  requires org.slf4j;

  // opens config package for deserialisation
  opens com.telenordigital.sms.smpp.config;

  exports com.telenordigital.sms.smpp;
  exports com.telenordigital.sms.smpp.config;
}
