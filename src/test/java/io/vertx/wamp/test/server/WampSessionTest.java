package io.vertx.wamp.test.server;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.junit5.VertxExtension;
import io.vertx.wamp.*;
import io.vertx.wamp.messages.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class WampSessionTest {
  @Spy
  TestMessageTransport transport = new TestMessageTransport();
  Realm testRealm = new Realm(new Uri("test.realm"));

  private RealmProvider createFakeRealmProvider(List<Realm> realms) {
    return new RealmProvider() {
      @Override
      public List<Realm> getRealms() {
        return realms;
      }
    };
  }

  @Nested
  @DisplayName("Session handshake")
  class SessionHandshakeTests {
    @Test
    @DisplayName("It sends WELCOME after receiving HELLO via its transport")
    public void testWelcomeAfterHello() {
      WampSession session = WampSession.establish(transport, null, createFakeRealmProvider(List.of(testRealm)));
      assertNotNull(session);
      transport.receiveHandler.accept(new HelloMessage(new JsonArray(List.of("test.realm", Collections.EMPTY_MAP))));
      ArgumentCaptor<WelcomeMessage> captor = ArgumentCaptor.forClass(WelcomeMessage.class);
      Mockito.verify(transport).sendMessage(captor.capture(), any());
      assertTrue(captor.getValue().getSessionId() > 0);
    }

    @Test
    @DisplayName("It rejects HELLO messages with unknown realm")
    public void testUnknownRealmHello() {
      WampSession session = WampSession.establish(transport, null, createFakeRealmProvider(List.of(testRealm)));
      assertNotNull(session);
      transport.receiveHandler.accept(new HelloMessage(new JsonArray(List.of("unknown.realm", Collections.EMPTY_MAP))));
      ArgumentCaptor<AbortMessage> captor = ArgumentCaptor.forClass(AbortMessage.class);
      Mockito.verify(transport).sendMessage(captor.capture(), any());
      assertEquals(Uri.NO_SUCH_REALM, captor.getValue().getReason());
    }
  }

  @Nested
  @DisplayName("With established session")
  class EstablishedSessionTests {
    WampSession session;

    @BeforeEach
    public void setupEstablishedSession() {
      this.session = WampSession.establish(transport, null, createFakeRealmProvider(List.of(testRealm)));
      ReflectionTestUtils.setField(session, "state", WampSession.State.ESTABLISHED);
      ReflectionTestUtils.setField(session, "realm", testRealm);
    }

    @Test
    @DisplayName("It does not accept HELLO messages anymore")
    public void testRejectHello() {
      transport.receiveHandler.accept(new HelloMessage(new JsonArray(List.of("test.realm", Collections.EMPTY_MAP))));
      ArgumentCaptor<AbortMessage> captor = ArgumentCaptor.forClass(AbortMessage.class);
      Mockito.verify(transport).sendMessage(captor.capture(), any());
      assertEquals(Uri.PROTOCOL_VIOLATION, captor.getValue().getReason());
      Mockito.verify(transport).close();
    }

    @Test
    @DisplayName("It terminates connections on GOODBYE")
    public void testHandleGoodbye() {
      transport.receiveHandler.accept(new GoodbyeMessage(new JsonArray(List.of(Collections.EMPTY_MAP, Uri.SYSTEM_SHUTDOWN))));
      ArgumentCaptor<GoodbyeMessage> captor = ArgumentCaptor.forClass(GoodbyeMessage.class);
      Mockito.verify(transport).sendMessage(captor.capture(), any());
      Mockito.verify(transport).close();
      assertEquals(Uri.GOODBYE_AND_OUT, captor.getValue().getReason());
    }

    @Test
    @DisplayName("It handles SUBSCRIBE requests")
    public void testSubscribe() {
      transport.receiveHandler.accept(new SubscribeMessage(new JsonArray(List.of(5432, Collections.EMPTY_MAP, new Uri("my.topic")))));
      ArgumentCaptor<SubscribedMessage> captor = ArgumentCaptor.forClass(SubscribedMessage.class);
      Mockito.verify(transport).sendMessage(captor.capture(), any());
      assertEquals(5432, captor.getValue().getId());
      assertTrue(captor.getValue().getId() > 0);
    }
  }

  private class TestMessageTransport implements MessageTransport {
    Consumer<WAMPMessage> receiveHandler;
    Consumer<Uri> errorHandler;

    @Override
    public void sendMessage(WAMPMessage message, Handler<AsyncResult<Void>> completeHandler) {
      // nothing to send out... it's just for tests
      if (completeHandler != null) {
        completeHandler.handle(Future.succeededFuture());
      }
    }

    @Override
    public void setReceiveHandler(Consumer<WAMPMessage> consumer) {
      this.receiveHandler = consumer;
    }

    @Override
    public void setErrorHandler(Consumer<Uri> consumer) {
      this.errorHandler = consumer;
    }

    @Override
    public void close(Promise<Void> promise) {
      if (promise != null) {
        promise.complete();
      }
    }
  }
}
