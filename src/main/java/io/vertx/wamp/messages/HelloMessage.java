package io.vertx.wamp.messages;

import io.vertx.wamp.MessageDecoder;
import io.vertx.wamp.Uri;
import io.vertx.wamp.WAMPMessage;

import java.util.List;
import java.util.Map;

public class HelloMessage implements WAMPMessage {

  private final Uri realm;
  private final Map<String, Object> details;

  public HelloMessage(Uri realm, Map<String, Object> details) {
    this.realm = realm;
    this.details = details;
  }

  public <T> HelloMessage(T data, MessageDecoder<?, T> decoder) {
    this.realm = new Uri(decoder.getString(data, 0));
    this.details = decoder.getMap(data, 1);
  }

  @Override
  public Type getType() {
    return Type.HELLO;
  }

  public Uri getRealm() {
    return realm;
  }

  @Override
  public List<Object> getPayload() {
    return List.of(realm, details);
  }
}
