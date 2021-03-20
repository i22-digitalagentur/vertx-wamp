package io.vertx.wamp.messages;

import io.vertx.wamp.MessageDecoder;
import io.vertx.wamp.WAMPMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.vertx.wamp.messages.Util.addArgsAndArgsKw;

public class YieldMessage implements WAMPMessage {

  private final long requestId;
  private final Map<String, Object> options;
  private final List<Object> arguments;
  private final Map<String, Object> argumentsKw;

  public YieldMessage(long requestId,
                      Map<String, Object> options,
                      List<Object> arguments,
                      Map<String, Object> argumentsKw) {
    this.requestId = requestId;
    this.options = options;
    this.arguments = arguments;
    this.argumentsKw = argumentsKw;
  }

  public <T> YieldMessage(T data, MessageDecoder<?, T> decoder) {
    this.requestId = decoder.getLong(data, 0);
    this.options = decoder.getMap(data, 1);
    this.arguments = decoder.elementCount(data) > 2 ? decoder.getList(data, 2) : null;
    this.argumentsKw = decoder.elementCount(data) > 3 ? decoder.getMap(data, 3) : null;
  }

  @Override
  public Type getType() {
    return Type.YIELD;
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
