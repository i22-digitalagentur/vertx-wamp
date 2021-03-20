package io.vertx.wamp.messages;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InvocationMessage extends AbstractWAMPMessage {

  private final long id;
  private final long registrationId;
  private final Map<String, Object> details;
  private final List<Object> arguments;
  private final Map<String, Object> argumentsKw;

  public InvocationMessage(long id, long registrationId,
      Map<String, Object> details,
      List<Object> arguments,
      Map<String, Object> argumentsKw) {
    super(Type.INVOCATION);
    this.id = id;
    this.registrationId = registrationId;
    this.details = details;
    this.arguments = arguments;
    this.argumentsKw = argumentsKw;
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
