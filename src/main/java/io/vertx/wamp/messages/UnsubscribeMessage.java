package io.vertx.wamp.messages;

import io.vertx.core.json.JsonArray;
import io.vertx.wamp.WAMPMessage;

import java.util.List;

public class UnsubscribeMessage implements WAMPMessage {
  private final long id;
  private final long subscription;

  public UnsubscribeMessage(JsonArray args) {
    this.id = args.getLong(0);
    this.subscription = args.getLong(1);
  }

  @Override
  public Type getType() {
    return Type.SUBSCRIBE;
  }

  @Override
  public List<?> getPayload() {
    return List.of(id, subscription);
  }
}
