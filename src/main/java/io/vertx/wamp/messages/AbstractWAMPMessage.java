package io.vertx.wamp.messages;

import io.vertx.wamp.WAMPMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class AbstractWAMPMessage implements WAMPMessage {
  private Type type;

  protected AbstractWAMPMessage(Type type) {
    this.type = type;
  }

  public Type getType() {
    return type;
  }

  void addArgsAndArgsKw(ArrayList<Object> list, List<?> arguments,
                        Map<String, ?> argumentsKw) {
    if (arguments != null) {
      list.add(arguments);
      if (argumentsKw != null) {
        list.add(argumentsKw);
      }
    } else if (argumentsKw != null) {
      list.add(null);
      list.add(argumentsKw);
    }
  }
}
