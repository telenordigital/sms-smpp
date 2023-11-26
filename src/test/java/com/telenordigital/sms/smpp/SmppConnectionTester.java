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

import static org.awaitility.Awaitility.await;

import com.telenordigital.sms.smpp.config.BindType;
import com.telenordigital.sms.smpp.config.DefaultEncoding;
import com.telenordigital.sms.smpp.config.SmppConnectionConfig;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class SmppConnectionTester {
  private static final CompletableFuture<SmppDeliveryReceipt> delivered = new CompletableFuture<>();

  public static CompletableFuture<Void> handleDelivery(final SmppDeliveryReceipt dr) {
    delivered.complete(dr);
    return CompletableFuture.completedFuture(null);
  }

  public static void main(final String[] args) throws Exception {
    final var host = args[0];
    final var port = Integer.parseInt(args[1]);
    final var systemId = args[2];
    final var password = args[3];
    final var systemType = args[4];
    final var ssl = args[5].equals("ssl");
    final var msisdn = args[6];
    final var sender = args[7];

    final var config =
        new SmppConnectionConfig(
            host,
            port,
            1,
            BindType.TRANSMITTER,
            DefaultEncoding.LATIN1,
            10,
            10,
            10,
            10,
            10,
            systemId,
            password,
            systemType,
            ssl,
            null,
            null,
            null,
            10);

    try (var group =
        new SmppConnectionGroup(
            Clock.systemUTC(),
            "test",
            List.of(config),
            SmppConnectionTester::handleDelivery,
            mo -> new CompletableFuture<>())) {
      await().atMost(60, TimeUnit.SECONDS).until(() -> group.getNumberOfActiveConnections() > 0);

      final var sms = new SmppSmsMt(sender, msisdn, "Test message. Delivery2", null);
      final var messageId = group.submit(sms).get(20, TimeUnit.SECONDS);

      System.out.printf("Message id: %s%n", messageId);

      System.out.println(delivered.get(20, TimeUnit.SECONDS));
    }
  }
}
