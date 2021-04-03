package io.vertx.wamp.messages;

import java.util.List;

public class SubscribedMessage extends AbstractWAMPMessage {

  private final long id;
  private final long subscription;

  public SubscribedMessage(long requestId, long subscription) {
    super(Type.SUBSCRIBED);
    this.id = requestId;
    this.subscription = subscription;
  }

  @Override
  public List<Object> getPayload() {
    return List.of(id, subscription);
  }

  public long getId() {
    return id;
  }

  public long getSubscription() {
    return subscription;
  }
}
