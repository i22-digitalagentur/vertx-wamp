package io.vertx.wamp.messages;

import io.vertx.wamp.MessageDecoder;
import java.util.List;

public class UnregisterMessage extends AbstractWAMPMessage {

  private final long id;
  private final long registration;

  public <T> UnregisterMessage(T data, MessageDecoder<?, T> decoder) {
    super(Type.UNREGISTER);
    this.id = decoder.getLong(data, 0);
    this.registration = decoder.getLong(data, 1);
  }

  public UnregisterMessage(long id, long registration) {
    super(Type.UNREGISTER);
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
  public List<Object> getPayload() {
    return List.of(id, registration);
  }
}
