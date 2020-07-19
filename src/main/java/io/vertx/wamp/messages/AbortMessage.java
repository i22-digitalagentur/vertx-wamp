package io.vertx.wamp.messages;

import io.vertx.core.json.JsonArray;
import io.vertx.wamp.Uri;
import io.vertx.wamp.WAMPMessage;

import java.util.List;
import java.util.Map;

public class AbortMessage implements WAMPMessage {

    private final Uri reason;
    private final Map<String, ?> details;

    public AbortMessage(Map<String, ?> details, Uri reason) {
        this.details = details;
        this.reason = reason;
    }

    public AbortMessage(JsonArray args) {
        this.details = args.getJsonObject(0).getMap();
        this.reason = new Uri(args.getString(1));
    }

    @Override
    public Type getType() {
        return Type.ABORT;
    }

    @Override
    public List<Object> getPayload() {
        return List.of(details, reason);
    }

    public Uri getReason() {
        return reason;
    }
}
