package io.vertx.wamp.messages;

import io.vertx.wamp.MessageDecoder;

import java.util.List;

public class UnsubscribeMessage extends AbstractWAMPMessage {

  private final long id;
  private final long subscription;

  public <T> UnsubscribeMessage(T data, MessageDecoder<?, T> decoder) {
    super(Type.UNSUBSCRIBE);
    this.id = decoder.getLong(data, 0);
    this.subscription = decoder.getLong(data, 1);
  }

  public UnsubscribeMessage(long id, long subscription) {
    super(Type.UNSUBSCRIBE);
    this.id = id;
    this.subscription = subscription;
  }

  public long getId() {
    return id;
  }

  public long getSubscription() {
    return subscription;
  }

  @Override
  public List<Object> getPayload() {
    return List.of(id, subscription);
  }
}
