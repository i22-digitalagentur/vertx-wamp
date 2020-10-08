# Vert.x WAMP

This component provides a Vert.x-based WAMP broker using the basic profile.

Authorization can be handled by implementing the `io.vertx.wamp.SecurityPolicy`
interface and setting it on your server.

The `SecurityPolicy` controls whether a client gets permission to connect to the
broker, join a specific realm or subscribe or publish to a specific topic.

In addition to serving as a broker, there is a (non-standard)
`io.vertx.wamp.Realm#publishMessage` method to directly publish messages without
needing to separately connect with a client.

To start the server, instantiate it in your verticle like so

```kotlin
val realm: Realm = Realm(Uri("com.example.wamp.realm"))
val wampServer = WAMPWebsocketServer.create(vertx)
wampServer.addRealm(realm)
          .withSecurityPolicy(SecurityPolicy)
          .listen(8080, "127.0.0.1")
```

## Running tests

Run all tests.

```
> mvn verify
```
