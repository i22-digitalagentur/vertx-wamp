package io.vertx.wamp;

import java.util.List;

public interface WAMPMessage {
  static long MAX_ID = 9007199254740991L; // 2^53-1

  Type getType();

  // the payload describes the content of the message regardless of encoding
  // the contents of the list can be a combination of numbers, strings,
  // maps etc according to the WAMP specification
  // in Java, this would be Integer, Float, String, Map<String, Object>,
  // List<Object> (where Object is one of the other types
  public List<?> getPayload();

  // only contains Broker/PubSub functionality for now
  // Dealer / RPC still TBD
  enum Type {
    HELLO(1),
    WELCOME(2),
    ABORT(3),
    GOODBYE(6),
    ERROR(8),
    PUBLISH(16),
    PUBLISHED(17),
    SUBSCRIBE(32),
    SUBSCRIBED(33),
    UNSUBSCRIBE(34),
    UNSUBSCRIBED(35),
    EVENT(36);

    private final int messageCode;

    private Type(int messageCode) {
      this.messageCode = messageCode;
    }

    static public Type findByCode(int code) {
      for (Type v : values()) {
        if (v.messageCode == code) {
          return v;
        }
      }
      return null;
    }

    public int getMessageCode() {
      return this.messageCode;
    }
  }
}
