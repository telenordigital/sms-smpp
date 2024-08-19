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

import com.telenordigital.sms.smpp.SmppMapping.Status;
import com.telenordigital.sms.smpp.config.SmppConnectionConfig;
import com.telenordigital.sms.smpp.pdu.SubmitSm;
import com.telenordigital.sms.smpp.pdu.SubmitSmResp;
import java.io.Closeable;
import java.lang.invoke.MethodHandles;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmppConnectionGroup implements Closeable {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final List<SmppConnection> connections;
  private final Clock clock;
  private final String name;

  public SmppConnectionGroup(
      final Clock clock,
      final String name,
      final List<SmppConnectionConfig> config,
      final Function<SmppDeliveryReceipt, CompletableFuture<Void>> smsDrHandler,
      final Function<SmppSmsMo, CompletableFuture<Void>> smsMoHandler) {
    this.name = name;
    connections =
        config.stream()
            .flatMap(
                conn ->
                    IntStream.range(0, conn.numberOfBinds())
                        .mapToObj(i -> new SmppConnection(conn, smsMoHandler, smsDrHandler)))
            .toList();
    this.clock = clock;
  }

  public Map<String, Supplier<Integer>> connectionsWithOpenWindowSlots() {
    return IntStream.range(0, connections.size())
        .boxed()
        .collect(
            Collectors.toMap(
                i -> String.format("%s-%d", this.name, i),
                i -> () -> connections.get(i).getOpenWindowSlots()));
  }

  public CompletableFuture<SmppResponse> submit(final SmppSmsMt sms) {
    return trySubmit(sms)
        .thenCompose(
            r -> {
              if (r.result() == SmppResultCode.RETRIABLE_ERROR) {
                LOG.info("SubmitSm failed with a retriable error. Retrying once: {}", r);
                return trySubmit(sms);
              }
              return CompletableFuture.completedFuture(r);
            });
  }

  private CompletableFuture<SmppResponse> trySubmit(final SmppSmsMt sms) {
    final var activeConnections = connections.stream().filter(SmppConnection::isActive).toList();
    // connections with more free slots first
    return activeConnections.stream()
        .max(Comparator.comparing(SmppConnection::getOpenWindowSlots))
        .map(conn -> submitInternal(conn, sms))
        .orElseGet(
            () ->
                CompletableFuture.completedFuture(
                    SmppResponse.routeDown("No active connections", info())));
  }

  private CompletableFuture<SmppResponse> submitInternal(
      final SmppConnection conn, final SmppSmsMt sms) {
    final var details = info() + ". " + conn.info();
    final List<SubmitSm> pduList =
        SubmitSm.create(
            clock,
            sms.sender(),
            sms.msisdn(),
            sms.message(),
            sms.validityPeriod(),
            conn.config.splitWithUdh());

    final List<CompletableFuture<SubmitSmResp>> resps = pduList.stream().map(conn::submit).toList();

    return CompletableFuture.allOf(resps.toArray(new CompletableFuture<?>[0]))
        .thenApply(
            v -> {
              final var responses = resps.stream().map(CompletableFuture::join).toList();
              return mergeStatuses(responses, details);
            })
        .exceptionally(e -> SmppResponse.retriableError(e.getMessage(), details));
  }

  static SmppResponse mergeStatuses(final List<SubmitSmResp> resps, final String details) {
    final var optionalStatuses =
        resps.stream().map(resp -> SmppMapping.map(resp.commandStatus())).toList();

    if (optionalStatuses.stream().anyMatch(Optional::isEmpty)) {
      return SmppResponse.failure("Unknown command status", details);
    }

    final var statuses =
        optionalStatuses.stream()
            .filter(Optional::isPresent)
            .map(Optional::get)
            .distinct()
            .toList();

    final boolean success = statuses.stream().allMatch(Status::isSuccessful);

    final var result =
        success
            ? SmppResultCode.SUCCESS
            : statuses.stream()
                .filter(r -> !r.isSuccessful())
                .map(Status::result)
                .findFirst()
                .orElse(null);

    final String message =
        statuses.stream().map(s -> s.name() + ": " + s.message()).collect(Collectors.joining(","));

    final String destSubAddress =
        resps.stream().findAny().map(SubmitSmResp::destSubAddress).orElse(null);

    return new SmppResponse(
        result,
        resps.stream().map(SubmitSmResp::messageId).toList(),
        message,
        success ? null : details,
        destSubAddress);
  }

  public String info() {
    return String.format("Group: %s", name);
  }

  public long getNumberOfActiveConnections() {
    return connections.stream().filter(SmppConnection::isActive).count();
  }

  @Override
  public void close() {
    connections.forEach(SmppConnection::close);
  }

  public boolean isHealthy() {
    // are all connections up?
    return getNumberOfActiveConnections() == connections.size();
  }
}
