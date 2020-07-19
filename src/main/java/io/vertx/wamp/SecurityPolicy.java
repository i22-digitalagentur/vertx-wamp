package io.vertx.wamp;

import io.vertx.core.http.ServerWebSocket;

/*
 As per section 13.3 of the specification:
 > Authentication of a Client to a Router at the WAMP level is not part of the
 > basic profile. When running over TLS, a Router MAY authenticate a Client at
 > the transport level by doing a client certificate based authentication.

 The security policy can authorize incoming connections based on the information
 from the websocket (peer certificates, headers etc.) and upon success returns
 information about the connected client which it can later use to authorize
 subsequent joins to a realm as well as subscribe or publish requests.
*/
public interface SecurityPolicy<T extends SecurityPolicy.ClientInfo> {
    /*
     * @return null if authentication failed, a ClientInfo to be used for subsequent
     *         authorization otherwise
     */
    T authenticateConnection(ServerWebSocket webSocket);

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
        @SuppressWarnings("java:S3740")
        SecurityPolicy getPolicy();
    }
}
