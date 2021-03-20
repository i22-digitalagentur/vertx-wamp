package io.vertx.wamp.messages;

import java.util.List;

public class RegisteredMessage extends AbstractWAMPMessage {

  private final long id;
  private final long registration;

  public RegisteredMessage(long requestId, long registration) {
    super(Type.REGISTERED);
    this.id = requestId;
    this.registration = registration;
  }

  @Override
  public List<Object> getPayload() {
    return List.of(id, registration);
  }

  public long getId() {
    return id;
  }

  public long getRegistration() {
    return registration;
  }
}
