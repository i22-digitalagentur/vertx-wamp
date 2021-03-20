package io.vertx.wamp.messages;

import io.vertx.wamp.MessageDecoder;
import io.vertx.wamp.WAMPMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.vertx.wamp.messages.Util.addArgsAndArgsKw;

public class ResultMessage implements WAMPMessage {

  private final long requestId;
  private final Map<String, Object> details;
  private final List<Object> arguments;
  private final Map<String, Object> argumentsKw;

  public ResultMessage(long requestId,
                       Map<String, Object> details,
                       List<Object> arguments,
                       Map<String, Object> argumentsKw) {
    this.requestId = requestId;
    this.details = details;
    this.arguments = arguments;
    this.argumentsKw = argumentsKw;
  }

  public <T> ResultMessage(T data, MessageDecoder<?, T> decoder) {
    this.requestId = decoder.getLong(data, 0);
    this.details = decoder.getMap(data, 1);
    this.arguments = decoder.elementCount(data) > 2 ? decoder.getList(data, 2) : null;
    this.argumentsKw = decoder.elementCount(data) > 3 ? decoder.getMap(data, 3) : null;
  }

  @Override
  public Type getType() {
    return Type.RESULT;
  }

  @Override
  public List<Object> getPayload() {
    ArrayList<Object> result = new ArrayList<>();
    result.add(requestId);
    result.add(details);
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

  public Map<String, Object> getDetails() {
    return details;
  }
}
