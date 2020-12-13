package io.vertx.wamp.test.server;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.wamp.Realm;
import io.vertx.wamp.SecurityPolicy;
import io.vertx.wamp.Uri;
import io.vertx.wamp.WampSession;
import io.vertx.wamp.messages.EventMessage;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
class RealmTest {

    Realm classUnderTest;
    WampSession fakeSession;

    @BeforeEach
    void initRealm() {
        classUnderTest = new Realm(new Uri("test.realm"));
        fakeSession = Mockito.mock(WampSession.class);
    }

    @Test
    @DisplayName("It skips event publication for subscriptions which the security policy prevents")
    void testPublishMessageSecurityPolicyReject() {
        SecurityPolicy.ClientInfo clientInfo = TestUtils.buildMockClientInfo();
        Mockito.when(fakeSession.getClientInfo()).thenReturn(clientInfo);
        Mockito.when(clientInfo.getPolicy().authorizeEvent(eq(clientInfo), any(), any()))
            .thenReturn(false);
        classUnderTest.addSubscription(fakeSession, new Uri("test.topic"));
        classUnderTest.publishMessage(
            new Uri("test.topic"),
            Collections.emptyMap(),
            Collections.emptyList(),
            Collections.emptyMap());
        Mockito.verify(fakeSession, Mockito.never()).sendMessage(any());
    }

    @Test
    @DisplayName("It permits event publication for subscriptions which the security policy permits")
    void testPublishMessageSecurityPolicyPermit() {
        SecurityPolicy.ClientInfo clientInfo = TestUtils.buildMockClientInfo();
        Mockito.when(fakeSession.getClientInfo()).thenReturn(clientInfo);
        Mockito.when(fakeSession.sendMessage(any())).thenReturn(Future.succeededFuture());
        Mockito.when(clientInfo.getPolicy().authorizeEvent(eq(clientInfo), any(), any()))
            .thenReturn(true);
        classUnderTest.addSubscription(fakeSession, new Uri("test.topic"));
        classUnderTest.publishMessage(new Uri("test.topic"),
            Collections.emptyMap(),
            Collections.emptyList(),
            Collections.emptyMap());
        Mockito.verify(fakeSession).sendMessage(any(EventMessage.class));
    }

    @Test
    @DisplayName("It permits event publication for subscriptions which have no security policy")
    void testPublishMessageNoSecurityPolicy() {
        Mockito.when(fakeSession.sendMessage(any())).thenReturn(Future.succeededFuture());
        classUnderTest.addSubscription(fakeSession, new Uri("test.topic"));
        classUnderTest.publishMessage(new Uri("test.topic"),
            Collections.emptyMap(),
            Collections.emptyList(),
            Collections.emptyMap());
        Mockito.verify(fakeSession).sendMessage(any(EventMessage.class));
    }
}
