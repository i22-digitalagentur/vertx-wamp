package io.vertx.wamp.util;

import java.util.HashSet;
import java.util.Set;

/**
 * For sessions: generates random IDs but avoids using any twice.
 *
 * Session IDs have the specific requirement that sessions are potentially
 * long-lived and as such, ID generation must take care to never re-use IDs of
 * still-running sessions.
 */
public class NonDuplicateRandomIdGenerator extends RandomIdGenerator {
  private final Set<Long> currentIds = new HashSet<>();

  public long next() {
    Long result;
    synchronized (currentIds) {
      do {
        result = super.next();
      } while (currentIds.contains(result));
      currentIds.add(result);
    }
    return result;
  }

  public void release(Long value) {
    synchronized (currentIds) {
      currentIds.remove(value);
    }
  }
}