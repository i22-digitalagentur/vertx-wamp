package io.vertx.wamp.messages;

import java.util.List;

public class UnsubscribedMessage extends AbstractWAMPMessage {

  private final long requestId;

  public UnsubscribedMessage(long requestId) {
    super(Type.UNSUBSCRIBED);
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
