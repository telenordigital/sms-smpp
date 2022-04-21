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

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;
import static org.mockito.Mockito.mock;

import com.cloudhopper.smpp.SmppServerConfiguration;
import com.cloudhopper.smpp.SmppServerHandler;
import com.cloudhopper.smpp.SmppServerSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.impl.DefaultSmppServer;
import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;
import com.cloudhopper.smpp.pdu.BaseBind;
import com.cloudhopper.smpp.pdu.BaseBindResp;
import com.cloudhopper.smpp.pdu.Pdu;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.ssl.SslConfiguration;
import com.cloudhopper.smpp.type.SmppProcessingException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.jboss.netty.channel.Channel;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;

@SuppressWarnings({"rawtypes", "unchecked", "ConstantConditions"})
public class SmppServerExtension
    implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback, CloseableResource {
  public record Session(
      long id,
      BaseBind bind,
      Consumer<PduRequest<?>> mock,
      SmppServerSession smppSession,
      List<PduResponse> responses) {}

  public static class MockException extends RuntimeException {
    private final int commandStatus;

    public MockException(int commandStatus) {
      this.commandStatus = commandStatus;
    }
  }

  public static class InvalidSequenceException extends RuntimeException {}

  private DefaultSmppServer server;
  private final Map<Long, Session> sessions = new ConcurrentHashMap<>();
  private final boolean useSsl;
  private int port;

  public SmppServerExtension() {
    this(false);
  }

  public SmppServerExtension(final boolean useSsl) {
    this.useSsl = useSsl;
  }

  @Override
  public void beforeAll(final ExtensionContext context) throws Exception {
    startServer();
    context.getRoot().getStore(GLOBAL).put(getClass().getName(), this);
  }

  public void startServer() throws Exception {
    final var config = new SmppServerConfiguration();
    config.setPort(port);
    config.setDefaultWindowSize(100);
    config.setBindTimeout(5000);
    if (useSsl) {
      config.setUseSsl(true);
      final var ssl = new SslConfiguration();
      ssl.setKeyStorePath(
          getClass().getResource("/com/telenordigital/sms/smpp/cloudhopper.jks").getPath());
      ssl.setKeyStorePassword("changeme");
      config.setSslConfiguration(ssl);
    }
    server =
        new DefaultSmppServer(
            config,
            new SmppServerHandler() {
              @Override
              public void sessionBindRequested(
                  final Long id, final SmppSessionConfiguration config, final BaseBind bind)
                  throws SmppProcessingException {
                if ("don't accept me for the first time".equals(bind.getPassword())
                    && bind.getSequenceNumber() == 1) {
                  throw new SmppProcessingException(13, "Invalid bind requested");
                }

                sessions.computeIfAbsent(
                    id,
                    key -> new Session(id, bind, mock(Consumer.class), null, new ArrayList<>()));
              }

              @Override
              public void sessionCreated(
                  final Long id, final SmppServerSession smppSession, final BaseBindResp bindResp) {
                final var session =
                    sessions.computeIfPresent(
                        id,
                        (k, s) ->
                            new Session(
                                s.id(), s.bind(), s.mock(), smppSession, new ArrayList<>()));

                smppSession.serverReady(
                    new DefaultSmppSessionHandler() {
                      @Override
                      public boolean firePduReceived(final Pdu pdu) {
                        if (pdu instanceof PduResponse pduResponse) {
                          session.responses.add(pduResponse);
                        }
                        return super.firePduReceived(pdu);
                      }

                      @Override
                      public PduResponse firePduRequestReceived(final PduRequest pduRequest) {
                        try {
                          session.mock().accept(pduRequest);
                        } catch (final MockException e) {
                          final var response = pduRequest.createResponse();
                          response.setCommandStatus(e.commandStatus);
                          return response;
                        } catch (final InvalidSequenceException e) {
                          final var response = pduRequest.createResponse();
                          response.setSequenceNumber(Integer.MAX_VALUE);
                          return response;
                        }

                        return pduRequest.createResponse();
                      }
                    });
              }

              @Override
              public void sessionDestroyed(final Long id, final SmppServerSession session) {
                sessions.remove(id);
              }
            });
    server.start();
    final Field field = DefaultSmppServer.class.getDeclaredField("serverChannel");
    field.setAccessible(true);
    final Channel serverChannel = (Channel) field.get(server);
    this.port = ((InetSocketAddress) serverChannel.getLocalAddress()).getPort();
  }

  @Override
  public void beforeEach(final ExtensionContext context) {
    sessions.values().forEach(s -> s.smppSession().close());
    sessions.clear();
  }

  @Override
  public void afterEach(final ExtensionContext context) {
    disconnectAll();
  }

  public void disconnectAll() {
    server.getChannels().forEach(ch -> ch.close().awaitUninterruptibly());
  }

  public Session getFirstSession() {
    return sessions.entrySet().iterator().next().getValue();
  }

  @Override
  public void close() {
    server.stop();
  }

  public int getPort() {
    return port;
  }
}
