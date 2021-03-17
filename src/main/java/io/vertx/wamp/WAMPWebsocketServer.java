package io.vertx.wamp;

import io.vertx.core.Closeable;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.net.SocketAddress;
import io.vertx.wamp.impl.WebsocketMessageTransport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class WAMPWebsocketServer implements RealmProvider, Closeable {

  public final static String USER_AGENT = "vertx-wamp-1.0";

  private final HttpServer httpServer;
  private final List<Realm> realms = new ArrayList<>();
  private final List<WampSession> connections = new ArrayList<>();
  @SuppressWarnings("java:S3740")
  private SecurityPolicy securityPolicy;

  protected WAMPWebsocketServer(Vertx vertx) {
    final HttpServerOptions options = new HttpServerOptions();
    options.addWebSocketSubProtocol("wamp.2.json");
    options.addWebSocketSubProtocol("wamp.2.msgpack");
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

  public WAMPWebsocketServer addRealm(Realm realm) {
    if (this.realms.contains(realm)) {
      throw new RealmExistsException();
    }
    this.realms.add(realm);
    return this;
  }

  public synchronized WAMPWebsocketServer withSecurityPolicy(SecurityPolicy<?> securityPolicy) {
    if (!this.connections.isEmpty()) {
      throw new ConnectionsAlreadyEstablishedException();
    }
    this.securityPolicy = securityPolicy;
    return this;
  }


  public Future<WAMPWebsocketServer> listen(int port) {
    return this.httpServer.listen(port).map(this);
  }

  public Future<WAMPWebsocketServer> listen(int port, String host) {
    final SocketAddress sa = SocketAddress.inetSocketAddress(port, host);
    return this.httpServer.listen(sa).map(this);
  }

  private void handleWebsocketConnection(ServerWebSocket webSocket) {
    SecurityPolicy.ClientInfo clientInfo = null;
    if (securityPolicy != null) {
      clientInfo = securityPolicy.authenticateConnection(webSocket);
      if (clientInfo == null) {
        webSocket.reject(403);
        return;
      }
    }
    final MessageTransport messageTransport = new WebsocketMessageTransport(webSocket);
    final WampSession session = WampSession.establish(messageTransport, clientInfo, this);
    connections.add(session);
    webSocket.closeHandler(voidResult ->
        // Investigate whether this may clash with shutdown
        connections.remove(session)
    );
    webSocket.accept();
  }

  public void close() {
    close(null);
  }

  @Override
  public void close(Promise<Void> promise) {
    @SuppressWarnings("java:S3740")
    final List<Future> promises = connections.parallelStream().map(session -> {
      final Promise sessionPromise = Promise.promise();
      session.shutdown(Uri.CLOSE_REALM, sessionPromise);
      return sessionPromise.future();
    }).collect(Collectors.toList());
    CompositeFuture.join(promises).onComplete(result -> httpServer.close(promise));
  }

  public static class RealmExistsException extends RuntimeException {

  }

  static class ConnectionsAlreadyEstablishedException extends RuntimeException {

    ConnectionsAlreadyEstablishedException() {
      super("Cannot set a security policy after connections have already been established");
    }
  }
}
