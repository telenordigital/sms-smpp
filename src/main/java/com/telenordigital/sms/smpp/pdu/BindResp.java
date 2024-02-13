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

import io.netty.buffer.ByteBuf;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public record BindResp(
    Command command, int commandStatus, int sequenceNumber, String systemId, short interfaceVersion)
    implements ResponsePdu {

  public static BindResp deserialize(final Command command, final ByteBuf buf) {
    final int status = buf.readInt();
    final int sequence = buf.readInt();
    final String systemId = PduUtil.readCString(buf);
    final short version =
        PduUtil.readOptionalParams(buf).getByte(TlvTag.SC_INTERFACE_VERSION).orElse((byte) 0);

    return new BindResp(command, status, sequence, systemId, version);
  }

  public String status() {
    return Optional.ofNullable(BindRespStatus.ALL.get(commandStatus))
        .map(BindRespStatus::toString)
        .orElse("UNKNOWN[0x%02X]".formatted(commandStatus));
  }

  private enum BindRespStatus {
    OK(0x00),
    ALREADY_BOUND(0x05),
    SYSTEM_ERROR(0x08),
    BIND_FAILED(0x0d),
    INVALID_PASSWORD(0x0e),
    INVALID_SYSTEM_ID(0x0f);

    private static final Map<Integer, BindRespStatus> ALL =
        EnumSet.allOf(BindRespStatus.class).stream().collect(Collectors.toMap(s -> s.code, s -> s));

    private final int code;

    BindRespStatus(final int code) {
      this.code = code;
    }

    @Override
    public String toString() {
      return "%s[0x%02X]".formatted(name(), code);
    }
  }
}
