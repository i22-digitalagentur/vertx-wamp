package io.vertx.wamp.messages;

import io.vertx.wamp.MessageDecoder;
import io.vertx.wamp.Uri;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PublishMessage extends AbstractWAMPMessage {

  private final long id;
  private final Map<String, Object> options;
  private final Uri topic;
  private final List<Object> arguments;
  private final Map<String, Object> argumentsKw;

  public PublishMessage(long id, Map<String, Object> options, Uri topic, List<Object> arguments,
                        Map<String,
                            Object> argumentsKw) {
    super(Type.PUBLISH);
    this.id = id;
    this.options = options;
    this.topic = topic;
    this.arguments = arguments;
    this.argumentsKw = argumentsKw;
  }

  public <T> PublishMessage(T data, MessageDecoder<?, T> decoder) {
    super(Type.PUBLISH);
    this.id = decoder.getLong(data, 0);
    this.options = decoder.getMap(data, 1);
    this.topic = new Uri(decoder.getString(data, 2));
    this.arguments = decoder.getList(data, 3);
    this.argumentsKw = decoder.getMap(data, 4);
  }

  @Override
  public List<Object> getPayload() {
    ArrayList<Object> result = new ArrayList<>();
    result.add(id);
    result.add(options);
    result.add(topic);
    addArgsAndArgsKw(result, arguments, argumentsKw);
    return result;
  }

  public List<Object> getArguments() {
    return arguments;
  }

  public Map<String, Object> getArgumentsKw() {
    return argumentsKw;
  }

  public Uri getTopic() {
    return topic;
  }

  public Map<String, Object> getOptions() {
    return options;
  }

  public long getId() {
    return id;
  }
}
