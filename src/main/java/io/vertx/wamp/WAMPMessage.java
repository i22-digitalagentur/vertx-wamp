package io.vertx.wamp;

import java.util.List;

public interface WAMPMessage {

  Type getType();

  // the payload describes the content of the message regardless of encoding
  // the contents of the list can be a combination of numbers, strings,
  // maps etc according to the WAMP specification
  // in Java, this would be Integer, Float, String, Map<String, Object>,
  // List<Object> (where Object is one of the other types
  List<Object> getPayload();

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
    EVENT(36),
    CALL(48),
    RESULT(50),
    REGISTER(64),
    REGISTERED(65),
    UNREGISTER(66),
    UNREGISTERED(67),
    INVOCATION(68),
    YIELD(70);

    private final int messageCode;

    Type(int messageCode) {
      this.messageCode = messageCode;
    }

    static public Type findByCode(Integer code) {
      if (code == null) {
        throw new WAMPProtocolException("No message type in message");
      }
      for (Type v : values()) {
        if (v.messageCode == code) {
          return v;
        }
      }
      throw new WAMPProtocolException("Unknown message type " + code);
    }

    public int getMessageCode() {
      return this.messageCode;
    }
  }
}
