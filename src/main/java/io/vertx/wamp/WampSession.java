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

/**
 * Represents a session to the server.
 * The underlying transport lifecycle is tied to the session and will be
 * closed once the session terminates.
 */
public class WampSession {
  static AtomicLong lastSessionId = new AtomicLong(1);
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

  public void shutdown(Uri reason, Handler<AsyncResult<Void>> shutdownHandler) {
    if (state == State.ESTABLISHING) {
      abortConnection(reason, shutdownHandler);
    } else {
      this.shutdownHandler = shutdownHandler;
      this.state = State.SHUTTING_DOWN;
      messageTransport.sendMessage(MessageFactory.createGoodbyeMessage(reason));
    }
  }

  private void handlePublish(PublishMessage msg) {
    if (clientInfo != null) {
      if (!clientInfo.getPolicy().authorizePublish(clientInfo,
          realm.getUri(),
          msg.getTopic())) {
        messageTransport.sendMessage(MessageFactory.createErrorMessage(WAMPMessage.Type.PUBLISH,
            msg.getId(),
            Map.of(),
            Uri.NOT_AUTHORIZED));
      } else {
        realm.publishMessage(msg);
      }
    }
  }

  private void handleGoodbye(GoodbyeMessage msg) {
    if (this.state == State.ESTABLISHED) {
      this.state = State.CLOSING;
      messageTransport.sendMessage(MessageFactory.createGoodbyeMessage(Uri.GOODBYE_AND_OUT),
          (_void) -> {
            closeSession();
          });
    } else {
      // we're already shutting down, this is a "Goodbye & Out" from the peer
      closeSession();
      if (shutdownHandler != null) {
        shutdownHandler.handle(Future.succeededFuture());
      }
    }
  }

  private void handleAbort(AbortMessage msg) {
    closeSession();
  }

  private void closeSession() {
    this.state = State.CLOSED;
    messageTransport.close();
  }

  private void handleMessage(WAMPMessage message) {
    WAMPMessage.Type messageType = message.getType();
    if (!state.allowedToReceive().contains(messageType)) {
      abortConnection(Uri.PROTOCOL_VIOLATION);
    }
    if (!messageHandlers.containsKey(messageType)) {
      // if there is no corresponding message handler, then this functionality
      // isn't implemented by us
      abortConnection(Uri.NO_SUCH_ROLE);
    }
    messageHandlers.get(messageType).accept(message);
  }

  private void handleHello(HelloMessage message) {
    Optional<Realm> targetRealm = realmProvider.getRealms().stream()
                                               .filter(r -> {
                                                 return r.getUri().equals(message.getRealm());
                                               })
                                               .findFirst();
    if (targetRealm.isPresent()) {
      this.realm = targetRealm.get();
      this.state = State.ESTABLISHED;
      sendWelcome();
    } else {
      abortConnection(Uri.NO_SUCH_REALM);
    }
  }

  private void handleSubscribe(SubscribeMessage message) {
    if (clientInfo != null && !clientInfo.getPolicy().authorizeSubscribe(clientInfo,
          this.realm.getUri(),
          message.getTopic())) {
      messageTransport.sendMessage(MessageFactory.createErrorMessage(WAMPMessage.Type.SUBSCRIBE,
          message.getId(),
          Map.of(),
          Uri.NOT_AUTHORIZED));
      return;
    }

    // any event related to the subscription will be delivered via the message transport
    long subscriptionId = realm.addSubscription(messageTransport::sendMessage, message.getTopic());
    messageTransport.sendMessage(MessageFactory.createSubscribedMessage(message.getId(), subscriptionId));
  }

  private void sendWelcome() {
    WAMPMessage message = MessageFactory.createWelcomeMessage(sessionId);
    messageTransport.sendMessage(message);
  }

  private void abortConnection(Uri reason) {
    abortConnection(reason, null);
  }

  private void abortConnection(Uri reason, Handler<AsyncResult<Void>> handler) {
    messageTransport.sendMessage(new AbortMessage(Map.of(), reason), _void -> {
      closeSession();
      if (handler != null) {
        handler.handle(Future.succeededFuture());
      }
    });
  }

  enum State {
    ESTABLISHING(WAMPMessage.Type.HELLO),
    // clients can only receive data in the first version
    ESTABLISHED(WAMPMessage.Type.SUBSCRIBE, WAMPMessage.Type.GOODBYE),
    CLOSING(), // received goodbye
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
