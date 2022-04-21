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

import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum Command {
  BIND_RECEIVER(0x1),
  BIND_TRANSMITTER(0x2),
  BIND_TRANCEIVER(0x9),
  BIND_RECEIVER_RESP(0x80000001),
  BIND_TRANSMITTER_RESP(0x80000002),
  BIND_TRANCEIVER_RESP(0x80000009),
  UNBIND(0x6),
  UNBIND_RESP(0x80000006),
  ENQUIRE_LINK(0x15),
  ENQUIRE_LINK_RESP(0x80000015),
  SUBMIT_SM(0x4),
  SUBMIT_SM_RESP(0x80000004),
  DELIVER_SM(0x5),
  DELIVER_SM_RESP(0x80000005),
  ;

  private final int id;

  private static final Map<Integer, Command> commandIds =
      EnumSet.allOf(Command.class).stream()
          .collect(Collectors.toMap(Command::id, Function.identity()));

  public static Optional<Command> valueOf(final int id) {
    return Optional.ofNullable(commandIds.get(id));
  }

  Command(final int id) {
    this.id = id;
  }

  public int id() {
    return id;
  }
}
