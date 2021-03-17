package io.vertx.wamp.messages;

import io.vertx.wamp.WAMPMessage;

import java.util.List;

public class UnregisteredMessage implements WAMPMessage {

  private final long requestId;

  public UnregisteredMessage(long requestId) {
    this.requestId = requestId;
  }

  public long getRequestId() {
    return requestId;
  }

  @Override
  public Type getType() {
    return Type.UNREGISTERED;
  }

  @Override
  public List<Object> getPayload() {
    return List.of(requestId);
  }
}
