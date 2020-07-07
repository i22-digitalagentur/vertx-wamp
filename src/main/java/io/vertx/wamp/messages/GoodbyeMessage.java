package io.vertx.wamp.messages;

import io.vertx.core.json.JsonArray;
import io.vertx.wamp.Uri;
import io.vertx.wamp.WAMPMessage;

import java.util.List;
import java.util.Map;

public class GoodbyeMessage implements WAMPMessage {
  private final Uri reason;
  private final Map<String, ?> details;

  public GoodbyeMessage(Map<String, ?> details, Uri reason) {
    this.details = details;
    this.reason = reason;
  }

  public GoodbyeMessage(JsonArray args) {
    this.details = args.getJsonObject(0).getMap();
    this.reason = new Uri(args.getString(1));
  }

  @Override
  public Type getType() {
    return Type.GOODBYE;
  }

  @Override
  public List<?> getPayload() {
    return List.of(details, reason);
  }

  public Uri getReason() {
    return reason;
  }
}
