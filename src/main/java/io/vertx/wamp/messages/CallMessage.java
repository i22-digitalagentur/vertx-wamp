package io.vertx.wamp.messages;

import io.vertx.wamp.MessageDecoder;
import io.vertx.wamp.Uri;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CallMessage extends AbstractWAMPMessage {

  private final long id;
  private final Map<String, Object> options;
  private final Uri procedure;
  private final List<Object> arguments;
  private final Map<String, Object> argumentsKw;

  public <T> CallMessage(T data, MessageDecoder<?, T> decoder) {
    super(Type.CALL);
    this.id = decoder.getLong(data, 0);
    this.options = decoder.getMap(data, 1);
    this.procedure = new Uri(decoder.getString(data, 2));
    this.arguments = decoder.elementCount(data) > 3 ? decoder.getList(data, 3) : null;
    this.argumentsKw = decoder.elementCount(data) > 4 ? decoder.getMap(data, 4) : null;
  }

  @Override
  public List<Object> getPayload() {
    ArrayList<Object> result = new ArrayList<>();
    result.add(id);
    result.add(options);
    result.add(procedure);
    addArgsAndArgsKw(result, arguments, argumentsKw);
    return result;
  }

  public List<Object> getArguments() {
    return arguments;
  }

  public Map<String, Object> getArgumentsKw() {
    return argumentsKw;
  }

  public Uri getProcedure() {
    return procedure;
  }

  public Map<String, Object> getOptions() {
    return options;
  }

  public long getId() {
    return id;
  }
}
