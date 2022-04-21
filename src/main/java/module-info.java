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
