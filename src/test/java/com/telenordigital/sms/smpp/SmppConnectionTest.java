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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.pdu.DataSmResp;
import com.cloudhopper.smpp.pdu.EnquireLink;
import com.cloudhopper.smpp.pdu.SubmitSmResp;
import com.cloudhopper.smpp.tlv.Tlv;
import com.telenordigital.sms.smpp.SmppServerExtension.InvalidSequenceException;
import com.telenordigital.sms.smpp.config.BindType;
import com.telenordigital.sms.smpp.config.DefaultEncoding;
import com.telenordigital.sms.smpp.config.SmppConnectionConfig;
import com.telenordigital.sms.smpp.pdu.SubmitSm;
import java.io.IOException;
import java.net.ServerSocket;
import java.time.Clock;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.IntStream;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;

@Timeout(10)
public class SmppConnectionTest {
  @RegisterExtension public static SmppServerExtension plainServer = new SmppServerExtension(false);
  @RegisterExtension public static SmppServerExtension secureServer = new SmppServerExtension(true);

  private SmppConnectionConfig plainConfig;
  private SmppConnectionConfig secureConfig;

  private Function<SmppSmsMo, CompletableFuture<Void>> moHandler;
  private Function<SmppDeliveryReceipt, CompletableFuture<Void>> drHandler;

  private CompletableFuture<SmppSmsMo> receivedMo;
  private CompletableFuture<SmppDeliveryReceipt> receivedDr;
  private final AtomicInteger counter = new AtomicInteger(0);

  @BeforeEach
  public void setup() throws IOException {
    plainConfig =
        new SmppConnectionConfig(
            "localhost",
            plainServer.getPort(),
            1,
            BindType.TRANSCEIVER,
            DefaultEncoding.LATIN1,
            1,
            10,
            10,
            5,
            10,
            "test",
            "test",
            "",
            false,
            null,
            100);
    final var trustedCerts =
        Objects.requireNonNull(getClass().getResourceAsStream("cloudhopper-cert.pem"))
            .readAllBytes();

    secureConfig =
        new SmppConnectionConfig(
            "localhost",
            secureServer.getPort(),
            1,
            BindType.TRANSCEIVER,
            DefaultEncoding.LATIN1,
            1,
            10,
            10,
            10,
            1,
            "test",
            "test",
            "",
            true,
            trustedCerts,
            100);

    receivedMo = new CompletableFuture<>();
    moHandler =
        smsMo -> {
          receivedMo.complete(smsMo);
          return CompletableFuture.completedFuture(null);
        };

    receivedDr = new CompletableFuture<>();
    drHandler =
        dr -> {
          receivedDr.complete(dr);
          return CompletableFuture.completedFuture(null);
        };
  }

  @Test
  public void connectionTestPlain() throws Exception {
    connectionTest(plainServer, plainConfig);
  }

  @Test
  public void connectionTestSecure() throws Exception {
    connectionTest(secureServer, secureConfig);
  }

  public void connectionTest(
      final SmppServerExtension smppServer, final SmppConnectionConfig config) throws Exception {
    try (var connection = new SmppConnection(config, moHandler, drHandler)) {
      waitUntilActive(connection, 4);
      assertThat(connection.isActive()).isTrue();
      assertThat(connection.getRemoteSystemId()).isEqualTo("cloudhopper");

      final var enquireLinkResp1 = connection.enquireLink().get();
      assertThat(enquireLinkResp1.commandStatus()).isEqualTo(0);
      assertThat(enquireLinkResp1.toString())
          .contains("EnquireLinkResp[commandStatus=0, sequenceNumber=");

      final var enquireLinkResp2 = connection.enquireLink().get();
      assertThat(enquireLinkResp1.sequenceNumber()).isNotEqualTo(enquireLinkResp2.sequenceNumber());

      // enquire link from server to client
      assertThat(
              smppServer
                  .getFirstSession()
                  .smppSession()
                  .enquireLink(new EnquireLink(), 1000)
                  .getCommandStatus())
          .isEqualTo(0);

      smppServer.disconnectAll();
      await().atMost(2, TimeUnit.SECONDS).until(() -> !connection.isActive());

      // waiting for reconnect
      waitUntilActive(connection, 4);
    }
  }

  @Test
  public void testDeliveryReceipt() throws Exception {
    try (var connection = new SmppConnection(plainConfig, moHandler, drHandler)) {
      await().atMost(2, TimeUnit.SECONDS).until(connection::isActive);

      sendDr(true, (byte) 4);

      final var dr = receivedDr.get(3, TimeUnit.SECONDS);
      assertThat(dr.messageId()).isEqualTo("ab");
    }
  }

  @Test
  public void testDeliveryReceiptInvalidStatus() throws Exception {
    try (var connection = new SmppConnection(plainConfig, moHandler, drHandler)) {
      await().atMost(2, TimeUnit.SECONDS).until(connection::isActive);

      sendDr(true, (byte) 10);

      assertThatThrownBy(() -> receivedDr.get(1, TimeUnit.SECONDS))
          .isInstanceOf(TimeoutException.class);
    }
  }

  private void sendDr(final boolean synchronous, byte state) {
    try {
      final byte[] messageId = new byte[] {0x61, 0x62, 0};
      final var delivery = new com.cloudhopper.smpp.pdu.DeliverSm();
      delivery.setCommandStatus(0);
      delivery.setEsmClass(SmppConstants.ESM_CLASS_MT_SMSC_DELIVERY_RECEIPT);
      delivery.setOptionalParameter(new Tlv(SmppConstants.TAG_RECEIPTED_MSG_ID, messageId));
      delivery.setOptionalParameter(new Tlv(SmppConstants.TAG_MSG_STATE, new byte[] {state}));

      plainServer.getFirstSession().smppSession().sendRequestPdu(delivery, 2000, synchronous);
    } catch (final Exception e) {
      throw new UnsupportedOperationException(e);
    }
  }

  @Test
  public void testMo() throws Exception {
    try (var connection = new SmppConnection(plainConfig, moHandler, drHandler)) {
      waitUntilActive(connection, 4);

      sendMo(true);

      final var mo = receivedMo.get(3, TimeUnit.SECONDS);
      assertThat(mo.message()).isEqualTo("ab");

      final var responses =
          Awaitility.waitAtMost(1, TimeUnit.SECONDS)
              .until(() -> plainServer.getFirstSession().responses(), list -> !list.isEmpty());
      assertThat(responses)
          .hasSize(1)
          .allSatisfy(pdu -> assertThat(pdu.getResultMessage()).isEqualTo("OK"));
    }
  }

  @Test
  public void unableToConnect() throws IOException {
    final var config = new SmppConnectionConfig("localhost", findFreePort(), 1);

    try (var connection = new SmppConnection(config, moHandler, drHandler)) {
      await().atMost(2, TimeUnit.SECONDS).until(() -> !connection.isActive());
    }
  }

  @Test
  public void submitSmPlain() throws Exception {
    submitSm(plainConfig);
  }

  @Test
  public void submitSmSecure() throws Exception {
    submitSm(secureConfig);
  }

  public void submitSm(final SmppConnectionConfig config) throws Exception {
    try (var connection = new SmppConnection(config, moHandler, drHandler)) {
      waitUntilActive(connection, 4);
      assertThat(connection.getOpenWindowSlots()).isEqualTo(config.windowSize());

      // mix sync and async requests
      final var futures =
          IntStream.range(0, 20)
              .mapToObj(
                  i -> {
                    final var submitSm =
                        SubmitSm.create(Clock.systemUTC(), "A", "Z", "O" + i, null);
                    // sync
                    assertThat(connection.submit(submitSm).join().commandStatus()).isEqualTo(0);
                    // async
                    return connection
                        .submit(submitSm)
                        .thenApply(cs -> assertThat(cs.commandStatus()).isEqualTo(0));
                  })
              .toArray(CompletableFuture[]::new);
      // for wait interrupt
      CompletableFuture.allOf(futures).get();
    }
  }

  @Test
  public void unbindPlain() throws Exception {
    unbind(plainConfig);
  }

  @Test
  public void unbindSecure() throws Exception {
    unbind(secureConfig);
  }

  public void unbind(final SmppConnectionConfig config) throws Exception {
    try (var connection = new SmppConnection(config, moHandler, drHandler)) {
      waitUntilActive(connection, 4);
      connection.unbind().get(2, TimeUnit.SECONDS);

      assertThatThrownBy(() -> connection.enquireLink().get(2, TimeUnit.SECONDS))
          .isInstanceOf(ExecutionException.class)
          .hasCauseInstanceOf(SmppException.class)
          .hasRootCauseMessage("Connection is inactive");
    }
  }

  interface Unbinder {
    void unbind(SmppConnection connection) throws Exception;
  }

  private void draining(final Unbinder unbinder) throws Exception {
    try (var connection = new SmppConnection(plainConfig, moHandler, drHandler)) {
      waitUntilActive(connection, 4);

      final var futures =
          IntStream.range(0, 10)
              .mapToObj(
                  i ->
                      connection
                          .submit(SubmitSm.create(Clock.systemUTC(), "X", "Y", "Z" + i, null))
                          .thenApply(cs -> assertThat(cs.commandStatus()).isEqualTo(0)))
              .toArray(CompletableFuture[]::new);
      unbinder.unbind(connection);
      CompletableFuture.allOf(futures).get();
    }
  }

  @Test
  public void draining() throws Exception {
    draining(connection -> connection.unbind().get(2, TimeUnit.SECONDS));
  }

  @Test
  public void drainingTriggeredByServer() throws Exception {
    draining(connection -> plainServer.getFirstSession().smppSession().unbind(2000));
  }

  @Test
  public void connectionLost() {
    try (var connection = new SmppConnection(plainConfig, moHandler, drHandler)) {
      waitUntilActive(connection, 4);
      // delay submit_sm on 1000 second on server
      doAnswer(
              invocationOnMock -> {
                if (invocationOnMock.getArgument(0) instanceof com.cloudhopper.smpp.pdu.SubmitSm) {
                  sleepUninterruptibly(1000);
                }

                return null;
              })
          .when(plainServer.getFirstSession().mock())
          .accept(any());

      final var future = connection.submit(SubmitSm.create(Clock.systemUTC(), "X", "Y", "L", null));
      plainServer.disconnectAll();
      assertThatThrownBy(future::get)
          .hasMessage("com.telenordigital.sms.smpp.SmppException: Connection lost");
    }
  }

  @Test
  public void idleWriteEvent() {
    final var idleConfig =
        new SmppConnectionConfig(
            "localhost",
            plainServer.getPort(),
            1,
            BindType.TRANSCEIVER,
            DefaultEncoding.LATIN1,
            1,
            1,
            60,
            60,
            1,
            "test",
            "test",
            "",
            false,
            null,
            100);
    try (var connection = new SmppConnection(idleConfig, moHandler, drHandler)) {
      waitUntilActive(connection, 4);

      // wait for 2 idle request
      verify(plainServer.getFirstSession().mock(), timeout(4_000).atLeast(2))
          .accept(isA(EnquireLink.class));
    }
  }

  @SuppressWarnings("unchecked")
  @Test
  public void idleReadEvent() {
    final var idleConfig =
        new SmppConnectionConfig(
            "localhost",
            plainServer.getPort(),
            1,
            BindType.TRANSCEIVER,
            DefaultEncoding.LATIN1,
            1,
            1,
            1,
            10,
            1,
            "test",
            "test",
            "",
            false,
            null,
            100);
    try (var connection = new SmppConnection(idleConfig, moHandler, drHandler)) {
      waitUntilActive(connection, 4);

      // block any response
      doAnswer(
              invocationOnMock -> {
                sleepUninterruptibly(60_000);
                return null;
              })
          .when(plainServer.getFirstSession().mock())
          .accept(any());

      final var future = connection.submit(SubmitSm.create(Clock.systemUTC(), "1", "2", "3", null));
      // simulate resubmission
      future.whenComplete(
          (x, e) -> connection.submit(SubmitSm.create(Clock.systemUTC(), "4", "5", "6", null)));
      assertThatThrownBy(future::get)
          .hasMessage("com.telenordigital.sms.smpp.SmppException: Connection lost");

      reset(plainServer.getFirstSession().mock());
      // wait for reconnect
      waitUntilActive(connection, 2);
    }
  }

  private void drainingInbound(final Unbinder unbinder) throws Exception {
    final var counter = new AtomicInteger();
    try (var connection =
        new SmppConnection(
            plainConfig,
            // slowing down processing of requests
            mo ->
                moHandler
                    .apply(mo)
                    .thenRunAsync(
                        () -> {
                          sleepUninterruptibly(2_000);
                          counter.incrementAndGet();
                        }),
            dr ->
                drHandler
                    .apply(dr)
                    .thenRunAsync(
                        () -> {
                          sleepUninterruptibly(2_000);
                          counter.incrementAndGet();
                        }))) {
      waitUntilActive(connection, 4);

      IntStream.range(0, 4).forEach(i -> sendMo(false));
      IntStream.range(0, 4).forEach(i -> sendDr(false, (byte) 2));

      // wait a bit to get all the requests sent
      Thread.sleep(500);

      final var sms =
          IntStream.range(0, 10)
              .mapToObj(
                  i -> connection.submit(SubmitSm.create(Clock.systemUTC(), "A", "Z", "O", null)))
              .toArray(CompletableFuture[]::new);

      // calling unbind which will wait until it is down
      unbinder.unbind(connection);

      // non errors for submit SMs
      CompletableFuture.allOf(sms).join();
    }

    // make sure everything has been processed
    assertThat(counter.get()).isEqualTo(8);
  }

  @Test
  public void drainingInbound() throws Exception {
    drainingInbound(connection -> connection.unbind().join());
  }

  @Test
  public void drainingInboundTriggeredByServer() throws Exception {
    drainingInbound(connection -> plainServer.getFirstSession().smppSession().unbind(8_000));
  }

  private static void sleepUninterruptibly(int millis) {
    try {
      Thread.sleep(millis);
    } catch (final InterruptedException e) {
      throw new IllegalStateException(e);
    }
  }

  private void sendMo(final boolean synchronous) {
    try {
      final byte[] message = new byte[] {0x61, 0x62};
      final var moPdu = new com.cloudhopper.smpp.pdu.DeliverSm();
      moPdu.setCommandStatus(0);
      moPdu.setShortMessage(message);
      moPdu.setEsmClass(SmppConstants.ESM_CLASS_MM_DEFAULT);
      plainServer.getFirstSession().smppSession().sendRequestPdu(moPdu, 2000, synchronous);
    } catch (final Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private void waitUntilActive(final SmppConnection connection, final int timeoutSeconds) {
    await().atMost(timeoutSeconds, TimeUnit.SECONDS).until(connection::isActive);
  }

  private static int findFreePort() throws IOException {
    try (final var socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    }
  }

  @Test
  public void unexpectedResponse() throws Exception {
    try (var connection = new SmppConnection(plainConfig, moHandler, drHandler)) {
      waitUntilActive(connection, 4);

      // unexpected command should not break things
      plainServer.getFirstSession().smppSession().sendResponsePdu(new DataSmResp());

      // unknown sequence should generate warning
      final var submitSmResp = new SubmitSmResp();
      submitSmResp.setSequenceNumber(123);
      plainServer.getFirstSession().smppSession().sendResponsePdu(submitSmResp);
    }
  }

  @Test
  public void windowFlowControl() {
    final var smallWindow =
        new SmppConnectionConfig(
            "localhost",
            plainServer.getPort(),
            1,
            BindType.TRANSCEIVER,
            DefaultEncoding.LATIN1,
            1,
            1000,
            10,
            1,
            10,
            "test",
            "test",
            "",
            false,
            null,
            2);
    try (var connection = new SmppConnection(smallWindow, moHandler, drHandler)) {
      waitUntilActive(connection, 4);

      // make enquire link to be slow
      doAnswer(
              invocationOnMock -> {
                if (invocationOnMock.getArgument(0)
                    instanceof com.cloudhopper.smpp.pdu.EnquireLink link) {
                  if (counter.incrementAndGet() % 2 == 0) {
                    Thread.sleep(1_000);
                  }
                }

                return null;
              })
          .when(plainServer.getFirstSession().mock())
          .accept(any());

      // the window of size 2 will be filled in shortly
      final var futures =
          IntStream.range(0, 10)
              .mapToObj(i -> connection.enquireLink())
              .toArray(CompletableFuture[]::new);
      CompletableFuture.allOf(futures).exceptionally(e -> null).join();

      assertThat(Arrays.stream(futures).filter(CompletableFuture::isCompletedExceptionally))
          .hasSize(10 - counter.get())
          .allSatisfy(
              f ->
                  assertThatThrownBy(f::join)
                      .hasMessage("com.telenordigital.sms.smpp.SmppException: Window is full"));
    }
  }

  @Test
  public void expireOldRequests() {
    final var lowTimeout =
        new SmppConnectionConfig(
            "localhost",
            plainServer.getPort(),
            1,
            BindType.TRANSCEIVER,
            DefaultEncoding.LATIN1,
            1,
            10,
            2,
            1,
            10,
            "test",
            "test",
            "",
            false,
            null,
            100);
    try (var connection = new SmppConnection(lowTimeout, moHandler, drHandler)) {
      waitUntilActive(connection, 4);

      // send a fake sequence number every second time to avoid idle timeout
      doAnswer(
              invocationOnMock -> {
                if (invocationOnMock.getArgument(0)
                    instanceof com.cloudhopper.smpp.pdu.EnquireLink link) {
                  if (counter.incrementAndGet() % 2 == 0) {
                    throw new InvalidSequenceException();
                  }
                }

                return null;
              })
          .when(plainServer.getFirstSession().mock())
          .accept(any());

      final var futures =
          IntStream.range(0, 30)
              .mapToObj(
                  i -> {
                    try {
                      Thread.sleep(100);
                    } catch (final InterruptedException e) {
                      Thread.currentThread().interrupt();
                      throw new IllegalStateException(e);
                    }
                    return connection.enquireLink();
                  })
              .toArray(CompletableFuture[]::new);

      CompletableFuture.allOf(futures).exceptionally(e -> null).join();

      // at least on of the request should have been expired
      assertThat(Arrays.stream(futures).filter(CompletableFuture::isCompletedExceptionally))
          .anySatisfy(
              f ->
                  assertThatThrownBy(f::join)
                      .hasMessage(
                          "com.telenordigital.sms.smpp.SmppException: Request expired. No response received in 2s"));
    }
  }

  @Test
  public void blockingMo() throws Exception {
    final var shortTimeout =
        new SmppConnectionConfig(
            "localhost",
            plainServer.getPort(),
            1,
            BindType.TRANSCEIVER,
            DefaultEncoding.LATIN1,
            10,
            10,
            10,
            5,
            2,
            "test",
            "test",
            "",
            false,
            null,
            100);

    try (var connection =
        new SmppConnection(
            shortTimeout,
            // slowing down processing of requests
            mo ->
                moHandler
                    .apply(mo)
                    .thenRunAsync(
                        () -> {
                          sleepUninterruptibly(100_000);
                        }),
            drHandler)) {
      waitUntilActive(connection, 4);
      final var serverSession = plainServer.getFirstSession();
      // Force MO to be blocked
      IntStream.range(0, 4).forEach(i -> sendMo(false));
      // unbind should timeout
      assertThat(connection.unbind().join().commandStatus()).isEqualTo(0);
      Awaitility.waitAtMost(5, TimeUnit.SECONDS)
          .until(serverSession::responses, list -> list.size() == 4);
      // checking responses on the other side
      assertThat(serverSession.responses().stream())
          .hasSize(4)
          .allSatisfy(pdu -> assertThat(pdu.getResultMessage()).isEqualTo("System error"));
    }
  }

  @Test
  public void submitFailedFirstTime() throws Exception {
    final var badPasswordConfig =
        new SmppConnectionConfig(
            "localhost",
            plainServer.getPort(),
            1,
            BindType.TRANSCEIVER,
            DefaultEncoding.LATIN1,
            1,
            10,
            10,
            5,
            10,
            "test",
            "don't accept me for the first time",
            "",
            false,
            null,
            100);

    try (var connection = new SmppConnection(badPasswordConfig, moHandler, drHandler)) {
      waitUntilActive(connection, 4);
    }
  }

  @Test
  public void reconnect() throws Exception {
    try (var connection = new SmppConnection(plainConfig, moHandler, drHandler)) {
      waitUntilActive(connection, 4);
      // drop all the sessions
      plainServer.disconnectAll();
      // shutdown server
      plainServer.close();
      Thread.sleep(2000);
      plainServer.startServer();
      // waiting for reconnect
      waitUntilActive(connection, 4);
    }
  }
}
