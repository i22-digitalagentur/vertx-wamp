package io.vertx.wamp.impl;

import io.vertx.core.json.JsonArray;
import io.vertx.wamp.MessageEncoder;
import io.vertx.wamp.WAMPMessage;

public class JsonMessageEncoder implements MessageEncoder<String> {
    @Override
    public String encode(WAMPMessage message) {
        JsonArray encoded = new JsonArray();
        encoded.add(message.getType().getMessageCode());
        for (Object entry : message.getPayload()) {
            encoded.add(entry);
        }
        return encoded.encode();
    }
}
