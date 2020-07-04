package io.vertx.wamp.test.server;

import io.crossbar.autobahn.wamp.Client;
import io.crossbar.autobahn.wamp.Session;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.wamp.Uri;
import io.vertx.wamp.WAMPWebsocketServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class IntegrationTest {
  @Test
  @DisplayName("It starts a WAMP server that can be connected to with a client")
  public void testStartWAMPServer(Vertx vertx, VertxTestContext testContext) {
    WAMPWebsocketServer server = WAMPWebsocketServer.create(vertx);
    server.addRealm(new Uri("de.i22.wamptest")).listen(8080).onComplete(res -> {
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
      Client client = new Client(session, "ws://localhost:8080/", "de.i22.wamptest");
      client.connect().handle((exitInfo, throwable) -> {
        if (throwable != null) {
          testContext.failNow(throwable);
        } else {
          if (exitInfo.code == -1) {
            testContext.failNow(new RuntimeException("Failed to connect"));
          }
        }
        return true;
      });
    });
  }
}
