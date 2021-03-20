package io.vertx.wamp.messages;

import io.vertx.wamp.MessageDecoder;
import io.vertx.wamp.Uri;

import java.util.List;
import java.util.Map;

public class RegisterMessage extends AbstractWAMPMessage {

  private final long requestId;
  private final Map<String, Object> options;
  private final Uri procedure;

  public RegisterMessage(Long requestId, Map<String, Object> options, Uri procedure) {
    super(Type.REGISTER);
    this.requestId = requestId;
    this.options = options;
    this.procedure = procedure;
  }

  public <T> RegisterMessage(T data, MessageDecoder<?, T> decoder) {
    super(Type.REGISTER);
    this.requestId = decoder.getLong(data, 0);
    this.options = decoder.getMap(data, 1);
    this.procedure = new Uri(decoder.getString(data, 2));
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
