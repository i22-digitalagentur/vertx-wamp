package io.vertx.wamp.messages;

import io.vertx.wamp.Uri;
import io.vertx.wamp.WAMPMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ErrorMessage extends AbstractWAMPMessage {
  private final WAMPMessage.Type requestType;
  private final long id;
  private final Map<String, Object> details;
  private final Uri error;
  private final List<Object> arguments;
  private final Map<String, Object> argumentsKw;

  public ErrorMessage(WAMPMessage.Type requestType,
                      long requestId,
                      Map<String, Object> details,
                      Uri error) {
    this(requestType, requestId, details, error, null, null);
  }

  public ErrorMessage(WAMPMessage.Type requestType,
                      long requestId,
                      Map<String, Object> details,
                      Uri error,
                      List<Object> arguments,
                      Map<String, Object> argumentsKw) {
    super(Type.ERROR);
    this.requestType = requestType;
    this.id = requestId;
    this.details = details;
    this.error = error;
    this.arguments = arguments;
    this.argumentsKw = argumentsKw;
  }

  public Type getRequestType() {
    return requestType;
  }

  @Override
  public List<Object> getPayload() {
    ArrayList<Object> result = new ArrayList<>();
    result.add(requestType.getMessageCode());
    result.add(id);
    result.add(details);
    result.add(error);
    addArgsAndArgsKw(result, arguments, argumentsKw);
    return result;
  }

  public Uri getError() {
    return error;
  }
}
