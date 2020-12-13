package io.vertx.wamp.util;

import java.util.concurrent.atomic.AtomicLong;

public class IDGenerator {
  public static long MAX_ID = 9007199254740991L; // 2^53-1

  private AtomicLong currentValue;

  public IDGenerator() {
    this.currentValue = new AtomicLong(1);
  }

  public long nextValue() {
    return currentValue.getAndUpdate(last -> last == MAX_ID ? 1 : (last + 1));
  }
}
