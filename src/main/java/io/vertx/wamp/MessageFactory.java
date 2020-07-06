package io.vertx.wamp;

import io.vertx.core.json.JsonArray;
import io.vertx.wamp.messages.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

// construct message objects. For decoding data currently fixed on JSON
// TODO: extract JSON stuff into separate codec functionality
public class MessageFactory {
  // only supports messages part of the receiving / broker end
  public static WAMPMessage parseMessage(JsonArray message) {
    final WAMPMessage.Type messageType = getMessageType(message);
    message.remove(0);
    switch (messageType) {
      case HELLO:
        return new HelloMessage(message);
      case ABORT:
        return new AbortMessage(message);
      case GOODBYE:
        return new GoodbyeMessage(message);
      case SUBSCRIBE:
        return new SubscribeMessage(message);
    }
    return null;
  }

  static WAMPMessage createWelcomeMessage(long sessionId) {
    final Map<String, Object> roles = Map.of("broker", Collections.emptyMap());
    final Map<String, Object> details = Map.of("roles", roles, "agent", WAMPWebsocketServer.USER_AGENT);
    return new WelcomeMessage(sessionId, details);
  }

  static WAMPMessage createGoodbyeMessage(Uri reason) {
    return new GoodbyeMessage(Map.of(), reason);
  }

  static WAMPMessage createAbortMessage(Uri reason) {
    return new AbortMessage(Map.of(), reason);
  }

  static WAMPMessage createErrorMessage(WAMPMessage.Type messageType,
                                        long requestId,
                                        Map<String, Object> details,
                                        Uri error) {
    return new ErrorMessage(messageType, requestId, details, error);
  }

  private static WAMPMessage.Type getMessageType(JsonArray message) {
    if (message.size() == 0) {
      throw new WAMPProtocolException("No data in message");
    }
    try {
      final Integer typeCode = message.getInteger(0);
      if (typeCode == null) {
        throw new WAMPProtocolException("No message type in message");
      }
      final WAMPMessage.Type messageType = WAMPMessage.Type.findByCode(typeCode);
      if (messageType == null) {
        throw new WAMPProtocolException("Unknown message type in message");
      }
      return messageType;
    } catch (ClassCastException e) {
      throw new WAMPProtocolException("Invalid message type in message");
    }
  }

  public static WAMPMessage createSubscribedMessage(long id, long subscriptionId) {
    return new SubscribedMessage(id, subscriptionId);
  }

  public static EventMessage createEvent(long subscriptionId,
                                         long publicationId,
                                         Map<String, Object> details,
                                         List<Object> arguments,
                                         Map<String, Object> argumentsKw) {
    return new EventMessage(subscriptionId,
        publicationId,
        details,
        arguments,
        argumentsKw);
  }
}
