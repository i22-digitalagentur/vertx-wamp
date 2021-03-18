package io.vertx.wamp.messages;

import io.vertx.wamp.MessageDecoder;
import io.vertx.wamp.WAMPMessage;

import java.util.List;

public class UnregisterMessage implements WAMPMessage {

  private final long id;
  private final long registration;

  public <T> UnregisterMessage(T data, MessageDecoder<?, T> decoder) {
    this.id = decoder.getLong(data, 0);
    this.registration = decoder.getLong(data, 1);
  }

  public UnregisterMessage(long id, long registration) {
    this.id = id;
    this.registration = registration;
  }

  public long getId() {
    return id;
  }

  public long getRegistration() {
    return registration;
  }

  @Override
  public Type getType() {
    return Type.UNREGISTER;
  }

  @Override
  public List<Object> getPayload() {
    return List.of(id, registration);
  }
}
