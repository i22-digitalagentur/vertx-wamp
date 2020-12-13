package io.vertx.wamp.messages;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Util {

  private Util() {
  }

  static void addArgsAndArgsKw(ArrayList<Object> list, List<?> arguments,
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
