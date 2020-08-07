package io.vertx.wamp.messages;

import io.vertx.wamp.MessageDecoder;
import io.vertx.wamp.Uri;
import io.vertx.wamp.WAMPMessage;

import java.util.List;
import java.util.Map;

public class SubscribeMessage implements WAMPMessage {
  private final long id;
  private final Map<String, Object> options;
  private final Uri topic;

  public SubscribeMessage(Long id, Map<String, Object> options, Uri topic) {
    this.id = id;
    this.options = options;
    this.topic = topic;
  }

  public <T> SubscribeMessage(T data, MessageDecoder<?, T> decoder) {
    this.id = decoder.getLong(data, 0);
    this.options = decoder.getMap(data, 1);
    this.topic = new Uri(decoder.getString(data, 2));
  }

  @Override
  public Type getType() {
    return Type.SUBSCRIBE;
  }

  @Override
  public List<Object> getPayload() {
    return List.of(id, options, topic);
  }

  public Map<String, Object> getOptions() {
    return options;
  }

  public Uri getTopic() {
    return topic;
  }

  public long getId() {
    return id;
  }
}
