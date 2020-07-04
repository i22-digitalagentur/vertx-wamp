package io.vertx.wamp;

import io.vertx.core.*;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.wamp.impl.WebsocketMessageTransport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class WAMPWebsocketServer implements RealmProvider, Closeable {
  public final static String USER_AGENT = "vertx-wamp-1.0";

  private HttpServer httpServer;
  private SecurityPolicy securityPolicy;
  private List<Realm> realms = new ArrayList<>();

  private List<WampSession> connections = new ArrayList<>();

  protected WAMPWebsocketServer(Vertx vertx) {
    HttpServerOptions options = new HttpServerOptions();
    options.addWebSocketSubProtocol("wamp.2.json"); // according to spec
    options.addWebSocketSubProtocol("wamp"); // listed on iana.org
    httpServer = vertx.createHttpServer(options);
    httpServer.webSocketHandler(this::handleWebsocketConnection);
  }

  public static WAMPWebsocketServer create(Vertx vertx) {
    return new WAMPWebsocketServer(vertx);
  }

  @Override
  public List<Realm> getRealms() {
    return Collections.unmodifiableList(realms);
  }

  public WAMPWebsocketServer addRealm(Uri realmId) {
    if (this.realms.stream().anyMatch(realm -> realm.getUri() == realmId)) {
      throw new RealmExistsException();
    }
    this.realms.add(new Realm(realmId));
    return this;
  }

  public synchronized WAMPWebsocketServer withSecurityPolicy(SecurityPolicy securityPolicy) {
    if (this.connections.size() > 0) {
      throw new ConnectionsAlreadyEstablishedException();
    }
    this.securityPolicy = securityPolicy;
    return this;
  }

  public Future<WAMPWebsocketServer> listen(int port) {
    return this.httpServer.listen(port).map(this);
  }

  private void handleWebsocketConnection(ServerWebSocket webSocket) {
    SecurityPolicy.ClientInfo clientInfo = null;
    if (securityPolicy != null) {
      clientInfo = securityPolicy.authenticateConnection(webSocket);
      if (clientInfo == null) {
        webSocket.reject();
        return;
      }
    }
    MessageTransport messageTransport = new WebsocketMessageTransport(webSocket);
    WampSession session = WampSession.establish(messageTransport, clientInfo, this);
    connections.add(session);
    webSocket.closeHandler(_void -> {
      // Investigate whether this may clash with shutdown
      connections.remove(webSocket);
    });
    webSocket.accept();
  }

  public void close() {
    close(null);
  }

  @Override
  public void close(Promise<Void> promise) {
    List<Future> promises = connections.parallelStream().map(session -> {
      Promise sessionPromise = Promise.promise();
      session.shutdown(Uri.CLOSE_REALM, sessionPromise);
      return sessionPromise.future();
    }).collect(Collectors.toList());
    CompositeFuture.all(promises).onComplete(_result -> {
      httpServer.close(promise);
    });
  }

  public static class RealmExistsException extends RuntimeException {
  }

  class ConnectionsAlreadyEstablishedException extends RuntimeException {
    ConnectionsAlreadyEstablishedException() {
      super("Cannot set a security policy after connections have already been established");
    }
  }
}
