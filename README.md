# Vert.x WAMP

This component provides a Vert.x-based WAMP broker using the basic profile.

It's very basic but does the job 😉

Authorization can be handled by implementing the `io.vertx.wamp.SecurityPolicy`
interface and setting it on your server.

The `SecurityPolicy` controls whether a client gets permission to connect to the
broker, join a specific realm or subscribe or publish to a specific topic.

In addition to serving as a broker, there is a (non-standard)
`io.vertx.wamp.Realm#publishMessage` method to directly publish messages without
needing to separately connect with a client.

To start the server, instantiate it in your verticle like so

```kotlin
val realm = Realm(Uri("com.example.wamp"))
val wampServer = WAMPWebsocketServer.create(vertx)
wampServer.addRealm(realm)
          .withSecurityPolicy(SecurityPolicy)
          .listen(8080, "127.0.0.1")
```

## Features
 - JSON and MsgPack subprotocol support
 - broker functionality (connect/publish/subscribe/unsubscribe)
 - security mechanism to let the broker control who can connect, publish or subscribe to which topic

## Roadmap / Desirable features

1. RPC support
2. Advanced Profile / Authentication

## Development

### Running tests

```shell
$ mvn verify
```

### Contributing

Contributions of all kinds are welcome! If you spot an error, please report
it via a Github issue.
