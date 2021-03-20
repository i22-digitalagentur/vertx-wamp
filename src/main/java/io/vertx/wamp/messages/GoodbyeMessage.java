package io.vertx.wamp.messages;

import io.vertx.wamp.MessageDecoder;
import io.vertx.wamp.Uri;

import java.util.List;
import java.util.Map;

public class GoodbyeMessage extends AbstractWAMPMessage {

  private final Uri reason;
  private final Map<String, Object> details;

  public GoodbyeMessage(Map<String, Object> details, Uri reason) {
    super(Type.GOODBYE);
    this.details = details;
    this.reason = reason;
  }

  public <T> GoodbyeMessage(T data, MessageDecoder<?, T> decoder) {
    super(Type.GOODBYE);
    this.details = decoder.getMap(data, 0);
    this.reason = new Uri(decoder.getString(data, 1));
  }

  @Override
  public List<Object> getPayload() {
    return List.of(details, reason);
  }

  public Uri getReason() {
    return reason;
  }
}
