package io.vertx.wamp.messages;

import io.vertx.wamp.MessageDecoder;
import io.vertx.wamp.Uri;
import io.vertx.wamp.WAMPMessage;

import java.util.List;
import java.util.Map;

public class GoodbyeMessage implements WAMPMessage {
  private final Uri reason;
  private final Map<String, Object> details;

  public GoodbyeMessage(Map<String, Object> details, Uri reason) {
    this.details = details;
    this.reason = reason;
  }

  public <T> GoodbyeMessage(T data, MessageDecoder<?, T> decoder) {
    this.details = decoder.getMap(data, 0);
    this.reason = new Uri(decoder.getString(data, 1));
  }

  @Override
  public Type getType() {
    return Type.GOODBYE;
  }

  @Override
  public List<Object> getPayload() {
    return List.of(details, reason);
  }

  public Uri getReason() {
    return reason;
  }
}
