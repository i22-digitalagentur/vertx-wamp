package io.vertx.wamp.messages;

import java.util.List;
import java.util.Map;

public class WelcomeMessage extends AbstractWAMPMessage {

  private final long sessionId;
  private final Map<String, ?> details;

  public WelcomeMessage(long sessionId, Map<String, ?> details) {
    super(Type.WELCOME);
    this.sessionId = sessionId;
    this.details = details;
  }

  public long getSessionId() {
    return sessionId;
  }

  @Override
  public List<Object> getPayload() {
    return List.of(sessionId, details);
  }
}
