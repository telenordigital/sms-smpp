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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import com.telenordigital.sms.smpp.config.SmppConnectionConfig;
import com.telenordigital.sms.smpp.pdu.SubmitSmResp;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

@Timeout(20)
public class SmppConnectionGroupTest {
  @RegisterExtension public static SmppServerExtension smppServer1 = new SmppServerExtension();
  @RegisterExtension public static SmppServerExtension smppServer2 = new SmppServerExtension();

  @Test
  public void success() throws ExecutionException, InterruptedException {
    final var c1 = new SmppConnectionConfig("localhost", smppServer1.getPort(), 10, 2);
    final var c2 = new SmppConnectionConfig("localhost", smppServer2.getPort(), 10);
    final var c3 = new SmppConnectionConfig("non-existing-host", 1000, 10);

    final var sms = new SmppSmsMt("a", "b", "c", null);
    try (var group =
        new SmppConnectionGroup(Clock.systemUTC(), "group", List.of(c1, c2, c3), null, null)) {

      Awaitility.await()
          .atMost(5, TimeUnit.SECONDS)
          .until(() -> group.getNumberOfActiveConnections() == 3);

      assertThat(group.connectionsWithOpenWindowSlots().get("group-0").get())
          .isEqualTo(c1.windowSize());

      final var futures =
          IntStream.range(0, 10).mapToObj(i -> group.submit(sms)).toArray(CompletableFuture[]::new);
      CompletableFuture.allOf(futures).get();

      final var response = group.submit(sms).get();
      assertThat(response.result()).isEqualTo(SmppResultCode.SUCCESS);
      assertThat(response.references()).containsExactly("");

      // one of the connection not active
      assertThat(group.isHealthy()).isFalse();
    }
  }

  @Test
  public void mergeStatus() {
    final var allGood =
        List.of(new SubmitSmResp(0, 10, "a", "123"), new SubmitSmResp(0, 11, "b", "123"));

    assertThat(SmppConnectionGroup.mergeStatuses(allGood, "d1"))
        .isEqualTo(
            new SmppResponse(
                SmppResultCode.SUCCESS, List.of("a", "b"), "ESME_ROK: null", null, "123"));

    final var oneFailed =
        List.of(new SubmitSmResp(0, 10, "a", "123"), new SubmitSmResp(0x45, 11, "b", "123"));

    assertThat(SmppConnectionGroup.mergeStatuses(oneFailed, "d1"))
        .isEqualTo(
            new SmppResponse(
                SmppResultCode.GENERAL_FAILURE,
                List.of("a", "b"),
                "ESME_ROK: null,ESME_RSUBMITFAIL: Generic failure",
                "d1",
                "123"));

    final var failed =
        List.of(new SubmitSmResp(0x45, 10, "a", null), new SubmitSmResp(0x45, 11, "b", null));
    assertThat(SmppConnectionGroup.mergeStatuses(failed, "d1"))
        .isEqualTo(
            new SmppResponse(
                SmppResultCode.GENERAL_FAILURE,
                List.of("a", "b"),
                "ESME_RSUBMITFAIL: Generic failure",
                "d1",
                null));
  }

  @SuppressWarnings("unchecked")
  @Timeout(10)
  @Test
  public void windowFix() throws InterruptedException {
    final var c1 = new SmppConnectionConfig("localhost", smppServer1.getPort(), 10);
    final var c2 = new SmppConnectionConfig("localhost", smppServer2.getPort(), 10);

    final var sms = new SmppSmsMt("a", "b", "c", null);
    try (var group =
        new SmppConnectionGroup(Clock.systemUTC(), "group", List.of(c1, c2), null, null)) {

      Awaitility.await()
          .atMost(1, TimeUnit.SECONDS)
          .until(() -> group.getNumberOfActiveConnections() == 2);

      final var countDownLatch = new CountDownLatch(1);
      // block any response
      doAnswer(
              invocationOnMock -> {
                countDownLatch.await();
                return null;
              })
          .when(smppServer1.getFirstSession().mock())
          .accept(any());

      // one of the first SMSs should be blocked, the number of window slots should be reduced
      final var future1 = group.submit(sms);
      final var future2 = group.submit(sms);
      // wait a little
      Thread.sleep(100);

      // this should always go to the connection with a max free slots
      group.submit(sms).join();
      countDownLatch.countDown();
      future1.join();
      future2.join();

      verify(smppServer1.getFirstSession().mock(), atLeast(1)).accept(any());
      verify(smppServer2.getFirstSession().mock(), atLeast(1)).accept(any());

      reset(smppServer1.getFirstSession().mock());
    }
  }

  @Test
  public void failure() throws ExecutionException, InterruptedException {
    final var c1 = new SmppConnectionConfig("localhost", smppServer1.getPort(), 10);
    final var sms = new SmppSmsMt("a", "b", "c", null);
    try (var group = new SmppConnectionGroup(Clock.systemUTC(), "test", List.of(c1), null, null)) {

      Awaitility.await()
          .atMost(1, TimeUnit.SECONDS)
          .until(() -> group.getNumberOfActiveConnections() == 1);

      assertThat(group.isHealthy()).isTrue();

      mockSubmitSmResponse(1);
      final var response = group.submit(sms).get();
      assertThat(response.result()).isEqualTo(SmppResultCode.INVALID_REQUEST);
      assertThat(response.message()).isEqualTo("ESME_RINVMSGLEN: Message Length is invalid");
      assertThat(response.details()).startsWith("Group: test. Connection: smpp://null@localhost:");

      mockSubmitSmResponse(88, 88); // retry also fails
      final var response1 = group.submit(sms).get();
      assertThat(response1.result()).isEqualTo(SmppResultCode.RATE_LIMITED);
      assertThat(response1.message()).isEqualTo("ESME_RTHROTTLED: Throttling error");

      mockSubmitSmResponse(100);
      final var response2 = group.submit(sms).get();
      assertThat(response2.result()).isEqualTo(SmppResultCode.GENERAL_FAILURE);
      assertThat(response2.message()).isEqualTo("Unknown command status");
    }
  }

  @Test
  public void retry() throws Exception {
    final var c1 = new SmppConnectionConfig("localhost", smppServer1.getPort(), 10);
    final var sms = new SmppSmsMt("a", "b", "c", null);
    try (var group = new SmppConnectionGroup(Clock.systemUTC(), "test", List.of(c1), null, null)) {

      Awaitility.await()
          .atMost(1, TimeUnit.SECONDS)
          .until(() -> group.getNumberOfActiveConnections() == 1);

      mockSubmitSmResponse(0x14); // returns success the second time
      final var response = group.submit(sms).get();
      assertThat(response.result()).isEqualTo(SmppResultCode.SUCCESS);
      assertThat(response.references()).containsExactly("");
    }
  }

  private void mockSubmitSmResponse(int... commandStatus) {
    doAnswer(
            new Answer<Void>() {
              private int count = 0;

              @Override
              public Void answer(InvocationOnMock invocation) {
                if (commandStatus.length > count) {
                  throw new SmppServerExtension.MockException(commandStatus[count++]);
                }
                return null;
              }
            })
        .when(smppServer1.getFirstSession().mock())
        .accept(any());
  }
}
