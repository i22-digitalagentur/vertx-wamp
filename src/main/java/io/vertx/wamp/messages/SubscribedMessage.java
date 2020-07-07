package io.vertx.wamp.messages;

import io.vertx.wamp.WAMPMessage;

import java.util.List;

public class SubscribedMessage implements WAMPMessage {
  private final long id;
  private final long subscription;

  public SubscribedMessage(long requestId, long subscription) {
    this.id = requestId;
    this.subscription = subscription;
  }

  @Override
  public Type getType() {
    return Type.SUBSCRIBED;
  }

  @Override
  public List<?> getPayload() {
    return List.of(id, subscription);
  }

  public long getId() {
    return id;
  }

  public long getSubscription() {
    return subscription;
  }
}
