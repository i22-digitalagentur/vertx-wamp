package io.vertx.wamp.messages;

import io.vertx.core.json.JsonArray;
import io.vertx.wamp.Uri;
import io.vertx.wamp.WAMPMessage;

import java.util.List;
import java.util.Map;

public class HelloMessage implements WAMPMessage {

    private final Uri realm;
    private final Map<String, Object> details;

    public HelloMessage(JsonArray args) {
        this.realm = new Uri(args.getString(0));
        this.details = args.getJsonObject(1).getMap();
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
