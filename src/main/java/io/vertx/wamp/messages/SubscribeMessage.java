package io.vertx.wamp.messages;

import io.vertx.core.json.JsonArray;
import io.vertx.wamp.Uri;
import io.vertx.wamp.WAMPMessage;

import java.util.List;
import java.util.Map;

public class SubscribeMessage implements WAMPMessage {
    private final long id;
    private final Map<String, Object> options;
    private final Uri topic;

    public SubscribeMessage(JsonArray args) {
        this.id = args.getLong(0);
        this.options = args.getJsonObject(1).getMap();
        this.topic = new Uri(args.getString(2));
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
