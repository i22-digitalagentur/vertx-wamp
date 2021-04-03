package io.vertx.wamp.messages;

import io.vertx.wamp.MessageDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class YieldMessage extends AbstractWAMPMessage {

  private final long requestId;
  private final Map<String, Object> options;
  private final List<Object> arguments;
  private final Map<String, Object> argumentsKw;

  public <T> YieldMessage(T data, MessageDecoder<?, T> decoder) {
    super(Type.YIELD);
    this.requestId = decoder.getLong(data, 0);
    this.options = decoder.getMap(data, 1);
    this.arguments = decoder.elementCount(data) > 2 ? decoder.getList(data, 2) : null;
    this.argumentsKw = decoder.elementCount(data) > 3 ? decoder.getMap(data, 3) : null;
  }

  @Override
  public List<Object> getPayload() {
    ArrayList<Object> result = new ArrayList<>();
    result.add(requestId);
    result.add(options);
    addArgsAndArgsKw(result, arguments, argumentsKw);
    return result;
  }

  public List<Object> getArguments() {
    return arguments;
  }

  public Map<String, Object> getArgumentsKw() {
    return argumentsKw;
  }

  public Long getRequestId() {
    return requestId;
  }

  public Map<String, Object> getOptions() {
    return options;
  }
}
