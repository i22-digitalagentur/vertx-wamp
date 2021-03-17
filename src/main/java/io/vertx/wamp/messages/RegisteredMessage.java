package io.vertx.wamp.messages;

import io.vertx.wamp.WAMPMessage;

import java.util.List;

public class RegisteredMessage implements WAMPMessage {

  private final long id;
  private final long registration;

  public RegisteredMessage(long requestId, long registration) {
    this.id = requestId;
    this.registration = registration;
  }

  @Override
  public Type getType() {
    return Type.REGISTERED;
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
