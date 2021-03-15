package io.vertx.wamp.util;

import java.util.concurrent.atomic.AtomicLong;

import static io.vertx.wamp.util.SessionIdGenerator.MAX_ID;


// generates sequential ids, thread-safe
public class SequentialIdGenerator {

  private final AtomicLong currentValue;

  public SequentialIdGenerator() {
    this.currentValue = new AtomicLong(1);
  }

  public long next() {
    return currentValue.getAndUpdate(last -> last == MAX_ID ? 1 : (last + 1));
  }
}
