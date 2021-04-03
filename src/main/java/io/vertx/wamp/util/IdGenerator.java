package io.vertx.wamp.util;

public interface IdGenerator {

  long MAX_ID = (1L << 53) - 1;

  long next();
}
