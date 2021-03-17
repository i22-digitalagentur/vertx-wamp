package io.vertx.wamp.util;

import java.security.SecureRandom;
import java.util.Iterator;

/**
 * Just generates random numbers, it does not matter whether they repeat
 */
public class RandomIdGenerator implements IdGenerator {
  private final Iterator<Long> random = new SecureRandom().longs(1, MAX_ID).iterator();

  public synchronized long next() {
    return random.next();
  }
}
