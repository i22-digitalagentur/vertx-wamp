package io.vertx.wamp.messages;

import io.vertx.wamp.MessageDecoder;
import io.vertx.wamp.Uri;
import io.vertx.wamp.WAMPMessage;

import java.util.List;
import java.util.Map;

public class RegisterMessage implements WAMPMessage {

  private final long requestId;
  private final Map<String, Object> options;
  private final Uri procedure;

  public RegisterMessage(Long requestId, Map<String, Object> options, Uri procedure) {
    this.requestId = requestId;
    this.options = options;
    this.procedure = procedure;
  }

  public <T> RegisterMessage(T data, MessageDecoder<?, T> decoder) {
    this.requestId = decoder.getLong(data, 0);
    this.options = decoder.getMap(data, 1);
    this.procedure = new Uri(decoder.getString(data, 2));
  }

  @Override
  public Type getType() {
    return Type.REGISTER;
  }

  @Override
  public List<Object> getPayload() {
    return List.of(requestId, options, procedure);
  }

  public Map<String, Object> getOptions() {
    return options;
  }

  public Uri getProcedure() {
    return procedure;
  }

  public long getId() {
    return requestId;
  }
}
