package io.vertx.wamp;

import io.vertx.core.http.ServerWebSocket;

/*
 Authorizing connections at the protocol level is not part of the specification.
 As such the websocket connection will be authenticated instead.
 The security policy then returns information about the connected client which
 it can later use to authorize subsequent subscribe/publish requests.
 I assume that not many brokers will actually have more than one realm, but
 it is nonetheless possible to delegate subscription and publication to
 realm-specific functionality within an implementation.
*/
public interface SecurityPolicy<T extends SecurityPolicy.ClientInfo> {
  /*
   * @return null if authentication failed, a ClientInfo to be used for subsequent
   *         authorization otherwise
   */
  T authenticateConnection(ServerWebSocket webSocket);

  ;

  /*
   * Called to verify that a connected client is permitted to join a realm
   * Later on, advanced profile wampcra or ticket (JWT) could be added
   */
  boolean authorizeHello(T client, Uri realm);

  /*
   * Authorize a subscription request to a given pattern
   */
  boolean authorizeSubscribe(T client, Uri realm, Uri subscribePattern);

  /*
   * Authorize a publication to a given topic
   */
  boolean authorizePublish(T client, Uri realm, Uri topic);

  interface ClientInfo {
    SecurityPolicy getPolicy();
  }
}
