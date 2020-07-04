package io.vertx.wamp.messages;

import io.vertx.wamp.WAMPMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.vertx.wamp.messages.Util.addArgsAndArgsKw;

public class EventMessage implements WAMPMessage {
  private final long subscriptionId;
  private final long publicationId;
  private final Map<String, ?> details;
  private final List<?> arguments;
  private final Map<String, ?> argumentsKw;

  public EventMessage(long subscriptionId,
                      long publicationId,
                      Map<String, ?> details,
                      List<?> arguments,
                      Map<String, ?> argumentsKw) {
    this.subscriptionId = subscriptionId;
    this.publicationId = publicationId;
    this.details = details;
    this.arguments = arguments;
    this.argumentsKw = argumentsKw;
  }

  @Override
  public Type getType() {
    return Type.EVENT;
  }

  @Override
  public List<?> getPayload() {
    ArrayList<Object> res = new ArrayList<>();
    res.add(subscriptionId);
    res.add(publicationId);
    res.add(details);
    addArgsAndArgsKw(res, arguments, argumentsKw);
    return res;
  }
}
