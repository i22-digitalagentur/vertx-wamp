package io.vertx.wamp.util;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.PrimitiveIterator;
import java.util.Set;
import java.util.stream.LongStream;

/**
 * Generates random IDs.
 *
 * Session generator has the specific requirement that sessions are potentially
 * long-lived and as such, ID generation must take care to never re-use IDs of
 * still-running sessions.
 */
public class SessionIdGenerator {
  public final static long maxId = 2^53;

  private final Set<Long> currentIds = new HashSet<>();
  private final PrimitiveIterator.OfLong random = new SecureRandom().longs(1, maxId).iterator();

  public long next() {
    Long result;
    synchronized (currentIds) {
      do {
        result = random.next();
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
