package io.vertx.wamp;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.wamp.messages.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a session to the server.
 * The underlying transport lifecycle is tied to the session and will be
 * closed once the session terminates.
 */
public class WampSession {
  static AtomicLong lastSessionId = new AtomicLong(1);
  private final Logger logger = Logger.getLogger(WampSession.class.getCanonicalName());
  private final MessageTransport messageTransport;
  private final SecurityPolicy.ClientInfo clientInfo;
  private final long sessionId;
  private final RealmProvider realmProvider;
  private Realm realm;
  private State state;
  private Handler<AsyncResult<Void>> shutdownHandler;
  private final Map<WAMPMessage.Type, Consumer<WAMPMessage>> messageHandlers = Map.of(
      WAMPMessage.Type.HELLO, (WAMPMessage msg) -> handleHello((HelloMessage) msg),
      WAMPMessage.Type.SUBSCRIBE, (WAMPMessage msg) -> handleSubscribe((SubscribeMessage) msg),
      WAMPMessage.Type.UNSUBSCRIBE, (WAMPMessage msg) -> handleUnsubscribe((UnsubscribeMessage) msg),
      WAMPMessage.Type.PUBLISH, (WAMPMessage msg) -> handlePublish((PublishMessage) msg),
      WAMPMessage.Type.ABORT, (WAMPMessage msg) -> handleAbort((AbortMessage) msg),
      WAMPMessage.Type.GOODBYE, (WAMPMessage msg) -> handleGoodbye((GoodbyeMessage) msg)
  );

  private WampSession(MessageTransport messageTransport,
                      SecurityPolicy.ClientInfo clientInfo,
                      RealmProvider realmProvider) {
    this.messageTransport = messageTransport;
    messageTransport.setReceiveHandler(this::handleMessage);
    messageTransport.setErrorHandler(this::abortConnection);

    this.clientInfo = clientInfo;
    this.realmProvider = realmProvider;
    this.state = State.ESTABLISHING;

    // TODO: assumes for now that there'll never be 9007199254740991 sessions
    // before restarting the app. Could probably be a free list of ~2^16 or so
    // ids because we'll probably never have more connections than that
    this.sessionId = lastSessionId.addAndGet(1);
  }

  // constructor function
  public static WampSession establish(MessageTransport messageTransport,
                                      SecurityPolicy.ClientInfo clientInfo,
                                      RealmProvider realmProvider) {
    return new WampSession(messageTransport, clientInfo, realmProvider);
  }

  public void sendMessage(WAMPMessage message) {
    trySendOrClose(message, null);
  }

  private void trySendOrClose(WAMPMessage message, Handler<AsyncResult<Void>> completeHandler) {
    try {
      if (completeHandler != null) {
        messageTransport.sendMessage(message, completeHandler);
      } else {
        messageTransport.sendMessage(message);
      }
    } catch (Exception err) {
      close();
    }
  }

  public SecurityPolicy.ClientInfo getClientInfo() {
    return clientInfo;
  }

  @Override
  public String toString() {
    return String.format("Session %d", sessionId);
  }

  @Override
  public boolean equals(Object other) {
    return (other instanceof WampSession && ((WampSession) other).sessionId == sessionId);
  }

  public void shutdown(Uri reason, Handler<AsyncResult<Void>> shutdownHandler) {
    if (state == State.ESTABLISHING) {
      logger.log(Level.INFO, "Shutdown session during handshake: {0}", sessionId);
      abortConnection(reason, shutdownHandler);
    } else {
      logger.log(Level.INFO, "Shutting down session: {0}", sessionId);
      this.shutdownHandler = shutdownHandler;
      this.state = State.SHUTTING_DOWN;
      try {
        sendMessage(MessageFactory.createGoodbyeMessage(reason));
      } catch (Exception err) {
        shutdownHandler.handle(Future.failedFuture(err));
      }
    }
  }

  private void handlePublish(PublishMessage msg) {
    logger.log(Level.FINEST, "Publishing message: {0}", msg);
    if (clientInfo != null) {
      if (!clientInfo.getPolicy().authorizePublish(clientInfo,
          realm.getUri(),
          msg.getTopic())) {
        sendMessage(MessageFactory.createErrorMessage(WAMPMessage.Type.PUBLISH,
            msg.getId(),
            Map.of(),
            Uri.NOT_AUTHORIZED));
      } else {
        realm.publishMessage(msg);
      }
    }
  }

  private void handleGoodbye(GoodbyeMessage msg) {
    logger.log(Level.FINE, "Session received GOODBYE {0}: {1}", new Object[]{sessionId, msg});
    if (this.state == State.ESTABLISHED) {
      this.state = State.CLOSING;
      trySendOrClose(MessageFactory.createGoodbyeMessage(Uri.GOODBYE_AND_OUT),
          voidArg -> close());
    } else {
      // we're already shutting down, this is a "Goodbye & Out" from the peer
      close();
      if (shutdownHandler != null) {
        shutdownHandler.handle(Future.succeededFuture());
      }
    }
  }

  private void handleAbort(AbortMessage msg) {
    logger.log(Level.INFO, "Received abort {0}: {1}", new Object[]{sessionId, msg.getReason()});
    close();
  }

  void close() {
    if (realm != null) {
      realm.removeSession(this);
    }
    this.state = State.CLOSED;
    logger.log(Level.FINE, "Session closed: {0}", sessionId);
    messageTransport.close();
  }

  private void handleMessage(WAMPMessage message) {
    WAMPMessage.Type messageType = message.getType();
    logger.log(Level.FINEST, "Handling message {0}: {1}", new Object[]{sessionId, messageType});
    if (!state.allowedToReceive().contains(messageType)) {
      logger.log(Level.WARNING, "Protocol violation {0}: message {1} not expected in state {2}",
          new Object[]{sessionId,
              messageType,
              state});
      abortConnection(Uri.PROTOCOL_VIOLATION);
      return;
    }
    if (!messageHandlers.containsKey(messageType)) {
      // if there is no corresponding message handler, then this functionality
      // isn't implemented by us
      logger.log(Level.WARNING, "Unsupported message type {0}: {1}", new Object[]{sessionId,
          messageType});
      abortConnection(Uri.NO_SUCH_ROLE);
    }
    messageHandlers.get(messageType).accept(message);
  }

  private void handleHello(HelloMessage message) {
    Optional<Realm> targetRealm = realmProvider.getRealms().stream()
                                               .filter(r -> r.getUri().equals(message.getRealm()))
                                               .findFirst();
    if (targetRealm.isPresent()) {
      if (clientInfo != null && !clientInfo.getPolicy().authorizeHello(clientInfo, message.getRealm())) {
        abortConnection(Uri.NOT_AUTHORIZED);
      } else {
        this.realm = targetRealm.get();
        this.state = State.ESTABLISHED;
        logger.log(Level.FINE, "Received HELLO to Realm {0} - {1}", new Object[]{realm, sessionId});
        sendWelcome();
      }
    } else {
      abortConnection(Uri.NO_SUCH_REALM);
    }
  }

  private void handleSubscribe(SubscribeMessage message) {
    if (clientInfo != null && !clientInfo.getPolicy().authorizeSubscribe(clientInfo,
        this.realm.getUri(),
        message.getTopic())) {
      logger.log(Level.WARNING, "Denied subscription {0}: {1} - {2}",
          new Object[]{
              sessionId,
              realm,
              message.getTopic()});
      sendMessage(MessageFactory.createErrorMessage(WAMPMessage.Type.SUBSCRIBE,
          message.getId(),
          Map.of(),
          Uri.NOT_AUTHORIZED));
      return;
    }

    // any event related to the subscription will be delivered via the message transport
    logger.log(Level.FINE, "Subscription added: {0} - {1} - {2}", new Object[]{sessionId, realm,
        message.getTopic()});
    long subscriptionId = realm.addSubscription(this, message.getTopic());
    sendMessage(MessageFactory.createSubscribedMessage(message.getId(), subscriptionId));
  }

  private void handleUnsubscribe(UnsubscribeMessage message) {
    realm.removeSubscription(this, message.getSubscription());
    sendMessage(MessageFactory.createUnsubscribedMessage(message.getId()));
  }

  private void sendWelcome() {
    WAMPMessage message = MessageFactory.createWelcomeMessage(sessionId);
    sendMessage(message);
  }

  private void abortConnection(Uri reason) {
    abortConnection(reason, null);
  }

  private void abortConnection(Uri reason, Handler<AsyncResult<Void>> handler) {
    trySendOrClose(new AbortMessage(Map.of(), reason), voidArg -> {
      close();
      if (handler != null) {
        handler.handle(Future.succeededFuture());
      }
    });
  }

  public enum State {
    // when establishing the session, wait for a HELLO from the client
    ESTABLISHING(WAMPMessage.Type.HELLO),
    ESTABLISHED(WAMPMessage.Type.SUBSCRIBE, WAMPMessage.Type.PUBLISH, WAMPMessage.Type.UNSUBSCRIBE,
        WAMPMessage.Type.GOODBYE),
    CLOSING(), // received goodbye, not expecting any more messages
    SHUTTING_DOWN(WAMPMessage.Type.GOODBYE), // sent goodbye, waiting for ack
    CLOSED();

    // use a list for easy iteration
    private final List<WAMPMessage.Type> followUpMessages;

    State(WAMPMessage.Type... followUpMessages) {
      this.followUpMessages = Arrays.asList(followUpMessages);
    }

    public List<WAMPMessage.Type> allowedToReceive() {
      return this.followUpMessages;
    }
  }
}
