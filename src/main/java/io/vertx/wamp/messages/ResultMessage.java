package io.vertx.wamp.messages;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ResultMessage extends AbstractWAMPMessage {

  private final long requestId;
  private final Map<String, Object> details;
  private final List<Object> arguments;
  private final Map<String, Object> argumentsKw;

  public ResultMessage(long requestId,
                       Map<String, Object> details,
                       List<Object> arguments,
                       Map<String, Object> argumentsKw) {
    super(Type.RESULT);
    this.requestId = requestId;
    this.details = details;
    this.arguments = arguments;
    this.argumentsKw = argumentsKw;
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
