package io.vertx.wamp.messages;

import java.util.List;

public class UnregisteredMessage extends AbstractWAMPMessage {

  private final long requestId;

  public UnregisteredMessage(long requestId) {
    super(Type.UNREGISTERED);
    this.requestId = requestId;
  }

  public long getRequestId() {
    return requestId;
  }

  @Override
  public List<Object> getPayload() {
    return List.of(requestId);
  }
}
