package io.vertx.wamp.messages;

import io.vertx.wamp.MessageDecoder;
import io.vertx.wamp.Uri;
import java.util.List;
import java.util.Map;

public class AbortMessage extends AbstractWAMPMessage {

  private final Uri reason;
  private final Map<String, ?> details;

  public AbortMessage(Map<String, ?> details, Uri reason) {
    super(Type.ABORT);
    this.details = details;
    this.reason = reason;
  }

  public <T> AbortMessage(T data, MessageDecoder<?, T> decoder) {
    super(Type.ABORT);
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
