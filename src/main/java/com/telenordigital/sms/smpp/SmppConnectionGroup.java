package com.telenordigital.sms.smpp;

import com.telenordigital.sms.smpp.config.SmppConnectionConfig;
import com.telenordigital.sms.smpp.pdu.SubmitSm;
import com.telenordigital.sms.smpp.pdu.SubmitSmResp;
import java.io.Closeable;
import java.lang.invoke.MethodHandles;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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

    return conn.submit(
            SubmitSm.create(clock, sms.sender(), sms.msisdn(), sms.message(), sms.validityPeriod()))
        .thenApply(resp -> mapSubmitSmResp(resp, details))
        .exceptionally(e -> SmppResponse.retriableError(e.getMessage(), details));
  }

  private static SmppResponse mapSubmitSmResp(final SubmitSmResp resp, final String details) {
    return SmppMapping.map(resp.commandStatus())
        .map(
            status ->
                new SmppResponse(
                    status.result(),
                    resp.messageId(),
                    status.name() + ": " + status.message(),
                    status.isSuccessful() ? null : details))
        .orElse(SmppResponse.failure("Unknown command status", details));
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
