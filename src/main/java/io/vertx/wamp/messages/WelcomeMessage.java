package io.vertx.wamp.messages;

import io.vertx.wamp.WAMPMessage;

import java.util.List;
import java.util.Map;

public class WelcomeMessage implements WAMPMessage {

  private final long sessionId;
  private final Map<String, ?> details;

  public WelcomeMessage(long sessionId, Map<String, ?> details) {
    this.sessionId = sessionId;
    this.details = details;
  }

  @Override
  public Type getType() {
    return Type.WELCOME;
  }

  @Override
  public List<?> getPayload() {
    return List.of(sessionId, details);
  }
}