package io.vertx.wamp.messages;

import io.vertx.core.json.JsonArray;
import io.vertx.wamp.Uri;
import io.vertx.wamp.WAMPMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.vertx.wamp.messages.Util.addArgsAndArgsKw;

public class ErrorMessage implements WAMPMessage {
  private final WAMPMessage.Type requestType;
  private final long id;
  private final Map<String, Object> details;
  private final Uri error;
  private final List<Object> arguments;
  private final Map<String, Object> argumentsKw;

  public ErrorMessage(JsonArray args) {
    this.requestType = WAMPMessage.Type.findByCode(args.getInteger(0));
    this.id = args.getLong(1);
    this.details = args.getJsonObject(2).getMap();
    this.error = new Uri(args.getString(3));
    this.arguments = args.size() > 4 ? args.getJsonArray(4).getList() : null;
    this.argumentsKw = args.size() > 5 ? args.getJsonObject(5).getMap() : null;
  }

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
    this.requestType = requestType;
    this.id = requestId;
    this.details = details;
    this.error = error;
    this.arguments = arguments;
    this.argumentsKw = argumentsKw;
  }

  @Override
  public Type getType() {
    return Type.ERROR;
  }

  @Override
  public List<?> getPayload() {
    ArrayList<Object> result = new ArrayList<>();
    result.add(requestType.getMessageCode());
    result.add(id);
    result.add(details);
    result.add(error);
    addArgsAndArgsKw(result, arguments, argumentsKw);
    return result;
  }
}