package io.vertx.wamp.test.server;

import io.crossbar.autobahn.wamp.Client;
import io.crossbar.autobahn.wamp.Session;
import io.crossbar.autobahn.wamp.types.Subscription;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.wamp.Uri;
import io.vertx.wamp.WAMPWebsocketServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
public class IntegrationTest {
  @Test
  @DisplayName("It starts a WAMP server that can be connected to with a client")
  public void testStartWAMPServer(Vertx vertx, VertxTestContext testContext) {
    WAMPWebsocketServer server = WAMPWebsocketServer.create(vertx);
    server.listen(8080).onComplete(res -> {
      if (res.failed()) {
        testContext.failNow(res.cause());
      }
      Session session = new Session();
      session.addOnConnectListener(sess -> {
        if (sess.isConnected()) {
          sess.leave();
          testContext.completeNow();
        } else {
          testContext.failNow(new RuntimeException("WAMP not connected"));
        }
      });
      connectSessionOrFail(testContext, session, "realm");
    });
  }

  @Test
  @DisplayName("It allows clients to join an existing realm")
  public void testRealmJoin(Vertx vertx, VertxTestContext testContext) {
    startWithTestRealm(vertx, testContext, server -> {
      Session session = new Session();
      session.addOnJoinListener((session1, sessionDetails) -> {
        testContext.verify(() -> {
          assertNotEquals(0, sessionDetails.sessionID);
          // FIXME: the callback is only invoked on successful join
          // but details are mostly empty: bug in client library?
          // assertEquals("test.realm", sessionDetails.realm);
        });
        testContext.completeNow();
      });
      connectSessionOrFail(testContext, session, "test.realm");
    });
  }

  @Test
  @DisplayName("It prevents clients from joining an unknown realm")
  public void testRealmNotExists(Vertx vertx, VertxTestContext testContext) {
    WAMPWebsocketServer server = WAMPWebsocketServer.create(vertx);
    server.listen(8080).onComplete(res -> {
      if (res.failed()) {
        testContext.failNow(res.cause());
      }
      Session session = new Session();
      session.addOnJoinListener((_session, sessionDetails) -> {
        testContext.failNow(new RuntimeException("Managed to join a non-existing realm"));
      });
      session.addOnDisconnectListener((_sess, clean) -> {
        testContext.completeNow();
      });
      connectSessionOrFail(testContext, session, "test.realm");
    });
  }

  @Test
  @DisplayName("It allows clients to subscribe to and receive messages")
  public void testSubscribe(Vertx vertx, VertxTestContext testContext) {
    startWithTestRealm(vertx, testContext, server -> {
      Session session = new Session();
      session.addOnJoinListener((session1, sessionDetails) -> {
        session.subscribe("hello.world", (o, eventDetails) -> {
          testContext.verify(()-> {
            assertEquals(4321, eventDetails.publication);
          });
          testContext.completeNow();
        }).thenApply((Subscription subscription) -> {
          testContext.verify(() -> {
            assertTrue(subscription.isActive());
          });
          server.getRealms().get(0).publishMessage(4321,
              new Uri("hello.world"),
              Collections.emptyMap(),
              null,
              null);
          return true;
        });
      });
      connectSessionOrFail(testContext, session, "test.realm");
    });
  }

  private void startWithTestRealm(Vertx vertx,
                                  VertxTestContext testContext,
                                  Handler<WAMPWebsocketServer> handler) {
    WAMPWebsocketServer server = WAMPWebsocketServer.create(vertx);
    server.addRealm(new Uri("test.realm")).listen(8080).onComplete(res -> {
      if (res.failed()) {
        testContext.failNow(res.cause());
      }
      handler.handle(server);
    });
  }

  private void connectSessionOrFail(VertxTestContext testContext, Session session, String realm) {
    Client client = new Client(session, "ws://localhost:8080/", realm);
    client.connect().handle((exitInfo, throwable) -> {
      if (throwable != null) {
        testContext.failNow(throwable);
      } else {
        if (exitInfo.code != 0) {
          testContext.failNow(new RuntimeException("Unclean disconnect"));
        }
      }
      return true;
    });
  }
}