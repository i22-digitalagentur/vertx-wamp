package io.vertx.wamp.test.server;

import io.crossbar.autobahn.wamp.Client;
import io.crossbar.autobahn.wamp.Session;
import io.crossbar.autobahn.wamp.types.Subscription;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.wamp.Realm;
import io.vertx.wamp.Uri;
import io.vertx.wamp.WAMPWebsocketServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
class IntegrationTest {
  static final int LISTEN_PORT = 8899;
  static final String LISTEN_HOST = "127.0.0.1";

  Realm testRealm;

  @BeforeEach
  private void initTestRealm() {
    this.testRealm = new Realm(new Uri("test.realm"));
  }

  @Test
  @DisplayName("It starts a WAMP server that can be connected to with a client")
  void testStartWAMPServer(Vertx vertx, VertxTestContext testContext) {
    WAMPWebsocketServer server = WAMPWebsocketServer.create(vertx);
    server.addRealm(testRealm);
    server.listen(LISTEN_PORT, LISTEN_HOST).onComplete(res -> {
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
      connectSessionOrFail(testContext, session);
    });
  }

  @Test
  @DisplayName("It allows clients to join an existing realm")
  void testRealmJoin(Vertx vertx, VertxTestContext testContext) {
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
      connectSessionOrFail(testContext, session);
    });
  }

  @Test
  @DisplayName("It prevents clients from joining an unknown realm")
  void testRealmNotExists(Vertx vertx, VertxTestContext testContext) {
    WAMPWebsocketServer server = WAMPWebsocketServer.create(vertx);
    server.listen(LISTEN_PORT, LISTEN_HOST).onComplete(testContext.succeeding(res -> {
      Session session = new Session();
      session.addOnJoinListener((_session, sessionDetails) -> {
        testContext.failNow(new RuntimeException("Managed to join a non-existing realm"));
      });
      session.addOnDisconnectListener((_sess, clean) -> {
        testContext.completeNow();
      });
      connectSessionOrFail(testContext, session);
    }));
  }

  @Test
  @DisplayName("It allows clients to subscribe to and receive messages")
  void testSubscribe(Vertx vertx, VertxTestContext testContext) {
    startWithTestRealm(vertx, testContext, server -> {
      Session session = new Session();
      session.addOnJoinListener((session1, sessionDetails) -> {
        session.subscribe("hello.world", (o, eventDetails) -> {
          testContext.verify(() -> {
            assertEquals(4321, eventDetails.publication);
          });
          testContext.completeNow();
        }).thenApply((Subscription subscription) -> {
          testContext.verify(() -> {
            assertTrue(subscription.isActive());
          });
          testRealm.publishMessage(4321,
              new Uri("hello.world"),
              Collections.emptyMap(),
              null,
              null);
          return true;
        });
      });
      connectSessionOrFail(testContext, session);
    });
  }

  @Test
  @DisplayName("It delivers messages after other subscription shuts down")
  void testSubscriptionDisconnect(Vertx vertx, VertxTestContext testContext) {
    startWithTestRealm(vertx, testContext, server -> {
      Session session = new Session();
      session.addOnJoinListener((session1, sessionDetails) -> {
        session.subscribe("hello.world", (o, eventDetails) -> {
          // do nothing in this, it will disconnect ASAP
        }).thenApply((Subscription subscription) -> {
          testContext.verify(() -> {
            assertTrue(subscription.isActive());
          });
          session.addOnDisconnectListener((_sess, clean) -> {
            // start a second session
            Session sess2 = new Session();
            sess2.addOnJoinListener((_sess2, sess2Details) -> {
              sess2.subscribe("hello.world", (o, eventDetails) -> {
                testContext.completeNow();
              }).thenApply((Subscription sub2) -> {
                testContext.verify(() -> {
                  assertTrue(sub2.isActive());
                });
                testRealm.publishMessage(4321,
                    new Uri("hello.world"),
                    Collections.emptyMap(),
                    null,
                    null);
                return true;
              });
            });
            connectSessionOrFail(testContext, sess2);
          });
          session.leave();
          return true;
        });
      });
      connectSessionOrFail(testContext, session);
    });
  }

  private void startWithTestRealm(Vertx vertx,
                                  VertxTestContext testContext,
                                  Handler<WAMPWebsocketServer> handler) {
    WAMPWebsocketServer server = WAMPWebsocketServer.create(vertx);
    server.addRealm(testRealm).listen(LISTEN_PORT, LISTEN_HOST).onComplete(res -> {
      if (res.failed()) {
        testContext.failNow(res.cause());
      }
      handler.handle(server);
    });
  }

  private Client createClient(Session session) {
    return new Client(session,
        String.format("ws://%s:%d/", LISTEN_HOST, LISTEN_PORT),
        "test.realm");
  }

  private void connectSessionOrFail(VertxTestContext testContext, Session session) {
    Client client = createClient(session);
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
