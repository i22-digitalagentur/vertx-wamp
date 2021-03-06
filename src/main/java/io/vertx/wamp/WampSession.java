package io.vertx.wamp;

import static io.vertx.wamp.Uri.NO_SUCH_REGISTRATION;
import static io.vertx.wamp.Uri.NO_SUCH_SUBSCRIPTION;
import static io.vertx.wamp.Uri.PROCEDURE_ALREADY_EXISTS;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.wamp.messages.AbortMessage;
import io.vertx.wamp.messages.CallMessage;
import io.vertx.wamp.messages.GoodbyeMessage;
import io.vertx.wamp.messages.HelloMessage;
import io.vertx.wamp.messages.InvocationMessage;
import io.vertx.wamp.messages.PublishMessage;
import io.vertx.wamp.messages.RegisterMessage;
import io.vertx.wamp.messages.SubscribeMessage;
import io.vertx.wamp.messages.UnregisterMessage;
import io.vertx.wamp.messages.UnsubscribeMessage;
import io.vertx.wamp.messages.YieldMessage;
import io.vertx.wamp.util.NonDuplicateRandomIdGenerator;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a session to the server. The underlying transport lifecycle is tied to the session and
 * will be closed once the session terminates.
 */
public class WampSession {

  private final static NonDuplicateRandomIdGenerator sessionIdGenerator = new NonDuplicateRandomIdGenerator();

  private final Logger logger = Logger.getLogger(WampSession.class.getCanonicalName());
  private final MessageTransport messageTransport;
  private final SecurityPolicy.ClientInfo clientInfo;
  private final Long sessionId;
  private final RealmProvider realmProvider;
  private Realm realm;
  private State state;
  private Handler<AsyncResult<Void>> shutdownHandler;
  private final ConcurrentHashMap<Long, Promise<AbstractMap.SimpleImmutableEntry<List<Object>, Map<String, Object>>>>
      pendingInvocations = new ConcurrentHashMap<>();

  private final Map<WAMPMessage.Type, Consumer<WAMPMessage>> messageHandlers = Map.of(
      WAMPMessage.Type.HELLO, (WAMPMessage msg) -> handleHello((HelloMessage) msg),
      WAMPMessage.Type.SUBSCRIBE, (WAMPMessage msg) -> handleSubscribe((SubscribeMessage) msg),
      WAMPMessage.Type.UNSUBSCRIBE,
      (WAMPMessage msg) -> handleUnsubscribe((UnsubscribeMessage) msg),
      WAMPMessage.Type.PUBLISH, (WAMPMessage msg) -> handlePublish((PublishMessage) msg),
      WAMPMessage.Type.ABORT, (WAMPMessage msg) -> handleAbort((AbortMessage) msg),
      WAMPMessage.Type.GOODBYE, (WAMPMessage msg) -> handleGoodbye((GoodbyeMessage) msg),
      WAMPMessage.Type.REGISTER, (WAMPMessage msg) -> handleRegister((RegisterMessage) msg),
      WAMPMessage.Type.UNREGISTER, (WAMPMessage msg) -> handleUnregister((UnregisterMessage) msg),
      WAMPMessage.Type.CALL, (WAMPMessage msg) -> handleCall((CallMessage) msg),
      WAMPMessage.Type.YIELD, (WAMPMessage msg) -> handleYield((YieldMessage) msg)
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
    this.sessionId = sessionIdGenerator.next();
  }

  // constructor function
  public static WampSession establish(MessageTransport messageTransport,
      SecurityPolicy.ClientInfo clientInfo,
      RealmProvider realmProvider) {
    return new WampSession(messageTransport, clientInfo, realmProvider);
  }

  public Future<Void> sendMessage(WAMPMessage message) {
    Promise<Void> promise = Promise.promise();
    trySendOrClose(message, promise);
    return promise.future();
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
    return (other instanceof WampSession && ((WampSession) other).sessionId.equals(sessionId));
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

  public Future<AbstractMap.SimpleImmutableEntry<List<Object>, Map<String, Object>>> invokeRegistration(
      InvocationMessage msg) {
    return Future.future((promise) -> {
      pendingInvocations.put(msg.getId(), promise);
      sendMessage(msg);
    });
  }

  private void handlePublish(PublishMessage msg) {
    logger.log(Level.FINEST, "Publishing message: {0}", msg);
    if (clientInfo != null && !clientInfo.getPolicy()
        .authorizePublish(clientInfo, realm.getUri(), msg.getTopic())) {
      sendMessage(MessageFactory.createErrorMessage(WAMPMessage.Type.PUBLISH,
          msg.getId(),
          Map.of(),
          Uri.NOT_AUTHORIZED));
    } else {
      realm.publishMessage(msg).onSuccess(publicationId ->
          sendMessage(MessageFactory.createPublishedMessage(msg.getId(), publicationId)));
    }
  }

  private void handleYield(YieldMessage msg) {
    this.pendingInvocations.computeIfPresent(msg.getRequestId(), (id, promise) -> {
      promise.complete(
          new AbstractMap.SimpleImmutableEntry<>(msg.getArguments(), msg.getArgumentsKw()));
      return null;
    });
  }

  private void handleCall(CallMessage msg) {
    logger.log(Level.FINEST, "Invoking procedure {0}", msg.getProcedure());
    if (clientInfo != null && !clientInfo.getPolicy()
        .authorizeCall(clientInfo, realm.getUri(), msg.getProcedure())) {
      sendMessage(MessageFactory.createErrorMessage(WAMPMessage.Type.CALL,
          msg.getId(),
          Map.of(),
          Uri.NOT_AUTHORIZED));
    } else {
      realm.callProcedure(msg.getProcedure(),
          msg.getArguments(),
          msg.getArgumentsKw())
          .onSuccess(arguments ->
              sendMessage(MessageFactory.createResultMessage(msg.getId(),
                  Collections.emptyMap(),
                  arguments.getKey(), arguments.getValue())))
          .onFailure(throwable ->
              sendMessage(MessageFactory.createErrorMessage(WAMPMessage.Type.CALL,
                  msg.getId(), Collections.emptyMap(),
                  new Uri("failed_to_invoke"))));
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
    if (clientInfo != null) {
      clientInfo.getPolicy().releaseConnection(clientInfo);
    }
    sessionIdGenerator.release(sessionId);
  }

  private void handleMessage(WAMPMessage message) {
    final WAMPMessage.Type messageType = message.getType();
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
    final Optional<Realm> targetRealm = realmProvider.getRealms().stream()
        .filter(r -> r.getUri().equals(message.getRealm()))
        .findFirst();
    if (targetRealm.isPresent()) {
      if (clientInfo != null && !clientInfo.getPolicy()
          .authorizeHello(clientInfo, message.getRealm())) {
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
    final long subscriptionId = realm.addSubscription(this, message.getTopic());
    sendMessage(MessageFactory.createSubscribedMessage(message.getId(), subscriptionId));
  }

  private void handleRegister(RegisterMessage message) {
    if (clientInfo != null && !clientInfo.getPolicy().authorizeRegister(clientInfo,
        this.realm.getUri(),
        message.getProcedure())) {
      logger.log(Level.WARNING, "Denied registration {0}: {1} - {2}",
          new Object[]{
              sessionId,
              realm,
              message.getProcedure()});
      sendMessage(MessageFactory.createErrorMessage(WAMPMessage.Type.REGISTER,
          message.getId(),
          Map.of(),
          Uri.NOT_AUTHORIZED));
      return;
    }

    // any event related to the registration will be delivered via the message transport
    logger.log(Level.FINE, "Registration added: {0} - {1} - {2}", new Object[]{sessionId, realm,
        message.getProcedure()});
    final long registrationId = realm.addRegistration(this, message.getProcedure());
    if (registrationId == -1) {
      sendMessage(MessageFactory.createErrorMessage(WAMPMessage.Type.REGISTER,
          message.getId(), Collections.emptyMap(), PROCEDURE_ALREADY_EXISTS));
    } else {
      sendMessage(MessageFactory.createRegisteredMessage(message.getId(), registrationId));
    }
  }

  private void handleUnsubscribe(UnsubscribeMessage message) {
    if (realm.removeSubscription(this, message.getSubscription())) {
      sendMessage(MessageFactory.createUnsubscribedMessage(message.getId()));
    } else {
      sendMessage(MessageFactory.createErrorMessage(WAMPMessage.Type.UNSUBSCRIBE,
          message.getId(), Collections.emptyMap(), NO_SUCH_SUBSCRIPTION));
    }
  }

  private void handleUnregister(UnregisterMessage message) {
    if (realm.removeRegistration(this, message.getRegistration())) {
      sendMessage(MessageFactory.createUnregisteredMessage(message.getId()));
    } else {
      sendMessage(MessageFactory.createErrorMessage(WAMPMessage.Type.UNREGISTER,
          message.getId(), Collections.emptyMap(), NO_SUCH_REGISTRATION));
    }
  }

  private void sendWelcome() {
    final WAMPMessage message = MessageFactory.createWelcomeMessage(sessionId);
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
        WAMPMessage.Type.REGISTER, WAMPMessage.Type.UNREGISTER,
        WAMPMessage.Type.CALL, WAMPMessage.Type.YIELD,
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
