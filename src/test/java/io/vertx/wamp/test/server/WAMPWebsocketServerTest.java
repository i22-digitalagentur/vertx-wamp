package io.vertx.wamp.test.server;

import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.wamp.Realm;
import io.vertx.wamp.Uri;
import io.vertx.wamp.WAMPWebsocketServer;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class WAMPWebsocketServerTest {

  @Test
  @DisplayName("It prevents adding realms with duplicate URI")
  void testAddRealm(Vertx vertx) {
    WAMPWebsocketServer server = WAMPWebsocketServer.create(vertx);
    Realm realm1 = new Realm(new Uri("test.realm"));
    server.addRealm(realm1);
    Realm realm2 = new Realm(new Uri("test.realm"));
    assertThrows(WAMPWebsocketServer.RealmExistsException.class, () -> {
      server.addRealm(realm2);
    });
  }

  @Test
  @DisplayName("It returns a list of added realms")
  void testGetRealms(Vertx vertx) {
    WAMPWebsocketServer server = WAMPWebsocketServer.create(vertx);
    Realm realm1 = new Realm(new Uri("test.realm1"));
    server.addRealm(realm1);
    Realm realm2 = new Realm(new Uri("test.realm2"));
    server.addRealm(realm2);

    List<Realm> returned = server.getRealms();

    assertIterableEquals(List.of(realm1, realm2), returned);
  }
}
