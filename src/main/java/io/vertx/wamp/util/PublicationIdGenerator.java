package io.vertx.wamp.util;

import java.security.SecureRandom;
import java.util.Iterator;

import static io.vertx.wamp.util.SessionIdGenerator.MAX_ID;

/**
 * Just generates random numbers, it does not matter whether they repeat
 */
public class PublicationIdGenerator {
  private final Iterator<Long> random = new SecureRandom().longs(1, MAX_ID).iterator();

  public synchronized long next() {
    return random.next();
  }
}
