package io.vertx.wamp.test.server;

import static io.vertx.wamp.Uri.NO_SUCH_REGISTRATION;
import static io.vertx.wamp.Uri.NO_SUCH_SUBSCRIPTION;
import static io.vertx.wamp.Uri.PROCEDURE_ALREADY_EXISTS;
import static io.vertx.wamp.test.server.TestUtils.buildMockClientInfo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.junit5.VertxExtension;
import io.vertx.wamp.MessageTransport;
import io.vertx.wamp.Realm;
import io.vertx.wamp.RealmProvider;
import io.vertx.wamp.SecurityPolicy;
import io.vertx.wamp.Uri;
import io.vertx.wamp.WAMPMessage;
import io.vertx.wamp.WampSession;
import io.vertx.wamp.messages.AbortMessage;
import io.vertx.wamp.messages.ErrorMessage;
import io.vertx.wamp.messages.GoodbyeMessage;
import io.vertx.wamp.messages.HelloMessage;
import io.vertx.wamp.messages.PublishMessage;
import io.vertx.wamp.messages.RegisterMessage;
import io.vertx.wamp.messages.RegisteredMessage;
import io.vertx.wamp.messages.SubscribeMessage;
import io.vertx.wamp.messages.SubscribedMessage;
import io.vertx.wamp.messages.UnregisterMessage;
import io.vertx.wamp.messages.UnregisteredMessage;
import io.vertx.wamp.messages.UnsubscribeMessage;
import io.vertx.wamp.messages.UnsubscribedMessage;
import io.vertx.wamp.messages.WelcomeMessage;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
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

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class WampSessionTest {

  @Spy
  TestMessageTransport transport = new TestMessageTransport();
  Realm testRealm = new Realm(new Uri("test.realm"));

  private RealmProvider createFakeRealmProvider(List<Realm> realms) {
    return () -> realms;
  }

  private static class TestMessageTransport implements MessageTransport {

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

  @Nested
  @DisplayName("Session handshake")
  class SessionHandshakeTests {

    @Test
    @DisplayName("It sends WELCOME after receiving HELLO via its transport")
    void testWelcomeAfterHello() {
      WampSession session = WampSession
          .establish(transport, null, createFakeRealmProvider(List.of(testRealm)));
      assertNotNull(session);
      transport.receiveHandler.accept(new HelloMessage(testRealm.getUri(), Collections.emptyMap()));
      ArgumentCaptor<WelcomeMessage> captor = ArgumentCaptor.forClass(WelcomeMessage.class);
      Mockito.verify(transport).sendMessage(captor.capture(), any());
      assertTrue(captor.getValue().getSessionId() > 0L);
    }

    @Test
    @DisplayName("It rejects HELLO messages with unknown realm")
    void testUnknownRealmHello() {
      WampSession session = WampSession
          .establish(transport, null, createFakeRealmProvider(List.of(testRealm)));
      assertNotNull(session);
      transport.receiveHandler
          .accept(new HelloMessage(new Uri("unknown.realm"), Collections.emptyMap()));
      ArgumentCaptor<AbortMessage> captor = ArgumentCaptor.forClass(AbortMessage.class);
      Mockito.verify(transport).sendMessage(captor.capture(), any());
      assertEquals(Uri.NO_SUCH_REALM, captor.getValue().getReason());
    }

    @Test
    @DisplayName("It denies join when security policy rejects hello")
    void testSecurityPolicyRejection() {
      SecurityPolicy.ClientInfo clientInfo = Mockito.mock(SecurityPolicy.ClientInfo.class);
      SecurityPolicy securityPolicy = Mockito.spy(Mockito.mock(SecurityPolicy.class));
      Mockito.when(securityPolicy.authorizeHello(clientInfo, testRealm.getUri())).thenReturn(false);
      Mockito.when(clientInfo.getPolicy()).thenReturn(securityPolicy);
      WampSession.establish(transport, clientInfo, createFakeRealmProvider(List.of(testRealm)));
      transport.receiveHandler.accept(new HelloMessage(testRealm.getUri(), Collections.emptyMap()));
      ArgumentCaptor<AbortMessage> captor = ArgumentCaptor.forClass(AbortMessage.class);
      Mockito.verify(transport).sendMessage(captor.capture(), any());
      assertEquals(Uri.NOT_AUTHORIZED, captor.getValue().getReason());
    }

    @Test
    @DisplayName("It accepts join when security policy permits hello")
    void testSecurityPolicyAcceptance() {
      SecurityPolicy.ClientInfo clientInfo = buildMockClientInfo();
      Mockito.when(clientInfo.getPolicy().authorizeHello(clientInfo, testRealm.getUri()))
          .thenReturn(true);
      WampSession.establish(transport, clientInfo, createFakeRealmProvider(List.of(testRealm)));
      transport.receiveHandler.accept(new HelloMessage(testRealm.getUri(), Collections.emptyMap()));
      ArgumentCaptor<WelcomeMessage> captor = ArgumentCaptor.forClass(WelcomeMessage.class);
      Mockito.verify(transport).sendMessage(captor.capture(), any());
      assertTrue(captor.getValue().getSessionId() > 0);
    }
  }

  @Nested
  @DisplayName("With established session")
  class EstablishedSessionTests {

    // Testing happens as a black box by monitoring the messages going in
    // and out of the transport. The session does not need to be referenced.
    WampSession session;

    @BeforeEach
    void setupEstablishedSession() {
      this.session = WampSession
          .establish(transport, null, createFakeRealmProvider(List.of(testRealm)));
      ReflectionTestUtils.setField(session, "state", WampSession.State.ESTABLISHED);
      ReflectionTestUtils.setField(session, "realm", testRealm);
    }

    @Test
    @DisplayName("It does not accept HELLO messages anymore")
    void testRejectHello() {
      transport.receiveHandler.accept(new HelloMessage(testRealm.getUri(), Collections.emptyMap()));
      ArgumentCaptor<AbortMessage> captor = ArgumentCaptor.forClass(AbortMessage.class);
      Mockito.verify(transport).sendMessage(captor.capture(), any());
      assertEquals(Uri.PROTOCOL_VIOLATION, captor.getValue().getReason());
      Mockito.verify(transport).close();
    }

    @Test
    @DisplayName("It terminates connections on GOODBYE")
    void testHandleGoodbye() {
      transport.receiveHandler
          .accept(new GoodbyeMessage(Collections.emptyMap(), Uri.SYSTEM_SHUTDOWN));
      ArgumentCaptor<GoodbyeMessage> captor = ArgumentCaptor.forClass(GoodbyeMessage.class);
      Mockito.verify(transport).sendMessage(captor.capture(), any());
      Mockito.verify(transport).close();
      assertEquals(Uri.GOODBYE_AND_OUT, captor.getValue().getReason());
    }

    @Test
    @DisplayName("It handles SUBSCRIBE requests")
    void testSubscribe() {
      transport.receiveHandler.accept(new SubscribeMessage(5432L, Collections.emptyMap(),
          new Uri("my.topic")));
      ArgumentCaptor<SubscribedMessage> captor = ArgumentCaptor.forClass(SubscribedMessage.class);
      Mockito.verify(transport).sendMessage(captor.capture(), any());
      assertEquals(5432, captor.getValue().getId());
      assertTrue(captor.getValue().getSubscription() > 0);
    }

    @Test
    @DisplayName("It handles known UNSUBSCRIBE requests")
    void testUnsubscribe() {
      final long subscriptionId = testRealm.addSubscription(session, new Uri("my.topic"));
      transport.receiveHandler.accept(new UnsubscribeMessage(5432L, subscriptionId));
      ArgumentCaptor<UnsubscribedMessage> captor = ArgumentCaptor
          .forClass(UnsubscribedMessage.class);
      Mockito.verify(transport).sendMessage(captor.capture(), any());
      assertEquals(5432, captor.getValue().getRequestId());
    }

    @Test
    @DisplayName("It handles unknown UNSUBSCRIBE requests")
    void testErroneousUnsubscribe() {
      transport.receiveHandler.accept(new UnsubscribeMessage(5432L, 98438432L));
      ArgumentCaptor<ErrorMessage> captor = ArgumentCaptor.forClass(ErrorMessage.class);
      Mockito.verify(transport).sendMessage(captor.capture(), any());
      assertEquals(NO_SUCH_SUBSCRIPTION, captor.getValue().getError());
    }

    @Test
    @DisplayName("It handles REGISTER requests")
    void testRegister() {
      transport.receiveHandler.accept(new RegisterMessage(5432L, Collections.emptyMap(),
          new Uri("my.procedure")));
      ArgumentCaptor<RegisteredMessage> captor = ArgumentCaptor.forClass(RegisteredMessage.class);
      Mockito.verify(transport).sendMessage(captor.capture(), any());
      assertEquals(5432, captor.getValue().getId());
      assertTrue(captor.getValue().getRegistration() > 0);
    }

    @Test
    @DisplayName("It handles duplicate REGISTER requests")
    void testDuplicateRegister() {
      transport.receiveHandler.accept(new RegisterMessage(5432L, Collections.emptyMap(),
          new Uri("my.procedure")));
      Mockito.clearInvocations(transport);

      transport.receiveHandler.accept(new RegisterMessage(5432L, Collections.emptyMap(),
          new Uri("my.procedure")));
      ArgumentCaptor<ErrorMessage> captor = ArgumentCaptor.forClass(ErrorMessage.class);
      Mockito.verify(transport).sendMessage(captor.capture(), any());
      assertEquals(WAMPMessage.Type.REGISTER, captor.getValue().getRequestType());
      assertEquals(PROCEDURE_ALREADY_EXISTS, captor.getValue().getError());
    }

    @Test
    @DisplayName("It handles UNREGISTER requests")
    void testUnregister() {
      long registrationId = testRealm.addRegistration(session, new Uri("my.procedure"));
      transport.receiveHandler.accept(new UnregisterMessage(5432L, registrationId));
      ArgumentCaptor<UnregisteredMessage> captor = ArgumentCaptor
          .forClass(UnregisteredMessage.class);
      Mockito.verify(transport).sendMessage(captor.capture(), any());
      assertEquals(5432, captor.getValue().getRequestId());
    }

    @Test
    @DisplayName("It handles unknown UNREGISTER requests")
    void testErroneousUnregister() {
      transport.receiveHandler.accept(new UnregisterMessage(5432L, 98438432L));
      ArgumentCaptor<ErrorMessage> captor = ArgumentCaptor.forClass(ErrorMessage.class);
      Mockito.verify(transport).sendMessage(captor.capture(), any());
      assertEquals(NO_SUCH_REGISTRATION, captor.getValue().getError());
    }

    @Nested
    @DisplayName("With security policy")
    class EstablishedSessionWithSecurityPolicyTests {

      SecurityPolicy.ClientInfo clientInfo = buildMockClientInfo();

      @BeforeEach
      void injectClientInfo() {
        ReflectionTestUtils.setField(session, "clientInfo", clientInfo);
      }

      @Test
      @DisplayName("It checks publish requests against security policy")
      void testPublishSecurityPolicyRejection() {
        Uri topic = new Uri("my.topic");
        testRealm.addSubscription(session, topic);

        Mockito.when(clientInfo.getPolicy().authorizePublish(clientInfo, testRealm.getUri(), topic))
            .thenReturn(false);
        transport.receiveHandler.accept(new PublishMessage(5432L, Collections.emptyMap(),
            topic,
            Collections.emptyList(), Collections.emptyMap()));
        ArgumentCaptor<ErrorMessage> captor = ArgumentCaptor.forClass(ErrorMessage.class);
        Mockito.verify(transport).sendMessage(captor.capture(), any());
        assertEquals(Uri.NOT_AUTHORIZED, captor.getValue().getError());
      }

      @Test
      @DisplayName("It checks subscribe requests against security policy")
      void testSubscribeSecurityPolicyRejection() {
        Uri topic = new Uri("my.topic");
        Mockito
            .when(clientInfo.getPolicy().authorizeSubscribe(clientInfo, testRealm.getUri(), topic))
            .thenReturn(false);
        transport.receiveHandler.accept(new SubscribeMessage(5432L, Collections.emptyMap(), topic));
        ArgumentCaptor<ErrorMessage> captor = ArgumentCaptor.forClass(ErrorMessage.class);
        Mockito.verify(transport).sendMessage(captor.capture(), any());
        assertEquals(Uri.NOT_AUTHORIZED, captor.getValue().getError());
      }

      @Test
      @DisplayName("It checks registration requests against security policy")
      void testRegisterSecurityPolicyRejection() {
        Uri procedure = new Uri("my.procedure");
        Mockito.when(
            clientInfo.getPolicy().authorizeRegister(clientInfo, testRealm.getUri(), procedure))
            .thenReturn(false);
        transport.receiveHandler
            .accept(new RegisterMessage(5432L, Collections.emptyMap(), procedure));
        ArgumentCaptor<ErrorMessage> captor = ArgumentCaptor.forClass(ErrorMessage.class);
        Mockito.verify(transport).sendMessage(captor.capture(), any());
        assertEquals(Uri.NOT_AUTHORIZED, captor.getValue().getError());
      }

      @Test
      @DisplayName("It forwards publish requests to the realm")
      void testPublishSecurityPolicyAcceptance() {
        Realm spiedRealm = Mockito.spy(testRealm);
        ReflectionTestUtils.setField(session, "realm", spiedRealm);
        Uri topic = new Uri("my.topic");
        spiedRealm.addSubscription(session, topic);
        Mockito.when(clientInfo.getPolicy().authorizePublish(clientInfo, testRealm.getUri(), topic))
            .thenReturn(true);
        PublishMessage message = new PublishMessage(5432L, Collections.emptyMap(),
            topic,
            Collections.emptyList(), Collections.emptyMap());
        transport.receiveHandler.accept(message);
        Mockito.verify(spiedRealm).publishMessage(message);
      }
    }
  }
}
