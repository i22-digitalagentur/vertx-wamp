package io.vertx.wamp.messages;

import io.vertx.wamp.MessageDecoder;
import io.vertx.wamp.WAMPMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.vertx.wamp.messages.Util.addArgsAndArgsKw;

public class InvocationMessage implements WAMPMessage {

  private final long id;
  private final long registrationId;
  private final Map<String, Object> details;
  private final List<Object> arguments;
  private final Map<String, Object> argumentsKw;

  public InvocationMessage(long id, long registrationId,
                           Map<String, Object> details,
                           List<Object> arguments,
                           Map<String, Object> argumentsKw) {
    this.id = id;
    this.registrationId = registrationId;
    this.details = details;
    this.arguments = arguments;
    this.argumentsKw = argumentsKw;
  }

  public <T> InvocationMessage(T data, MessageDecoder<?, T> decoder) {
    this.id = decoder.getLong(data, 0);
    this.registrationId = decoder.getLong(data, 1);
    this.details = decoder.getMap(data, 2);
    this.arguments = decoder.elementCount(data) > 3 ? decoder.getList(data, 3) : null;
    this.argumentsKw = decoder.elementCount(data) > 4 ? decoder.getMap(data, 4) : null;
  }

  @Override
  public Type getType() {
    return Type.INVOCATION;
  }

  @Override
  public List<Object> getPayload() {
    ArrayList<Object> result = new ArrayList<>();
    result.add(id);
    result.add(registrationId);
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

  public Long getRegistrationId() {
    return registrationId;
  }

  public Map<String, Object> getDetails() {
    return details;
  }

  public long getId() {
    return id;
  }
}
