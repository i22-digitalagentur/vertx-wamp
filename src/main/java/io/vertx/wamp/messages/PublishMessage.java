package io.vertx.wamp.messages;

import io.vertx.core.json.JsonArray;
import io.vertx.wamp.Uri;
import io.vertx.wamp.WAMPMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.vertx.wamp.messages.Util.addArgsAndArgsKw;

public class PublishMessage implements WAMPMessage {
    private final long id;
    private final Map<String, Object> options;
    private final Uri topic;
    private final List<Object> arguments;
    private final Map<String, Object> argumentsKw;

    public PublishMessage(JsonArray args) {
        this.id = args.getLong(0);
        this.options = args.getJsonObject(2).getMap();
        this.topic = new Uri(args.getString(3));
        this.arguments = args.size() > 3 ? args.getJsonArray(4).getList() : null;
        this.argumentsKw = args.size() > 4 ? args.getJsonObject(5).getMap() : null;
    }

    @Override
    public Type getType() {
        return Type.PUBLISH;
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
        return Collections.unmodifiableList(arguments);
    }

    public Map<String, Object> getArgumentsKw() {
        return Collections.unmodifiableMap(argumentsKw);
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
