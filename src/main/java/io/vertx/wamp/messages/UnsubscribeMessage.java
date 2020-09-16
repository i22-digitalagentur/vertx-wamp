package io.vertx.wamp.messages;

import io.vertx.wamp.MessageDecoder;
import io.vertx.wamp.WAMPMessage;

import java.util.List;

public class UnsubscribeMessage implements WAMPMessage {
    private final long id;
    private final long subscription;

    public <T> UnsubscribeMessage(T data, MessageDecoder<?, T> decoder) {
        this.id = decoder.getLong(data, 0);
        this.subscription = decoder.getLong(data, 1);
    }

    public long getId() {
        return id;
    }

    public long getSubscription() {
        return subscription;
    }

    @Override
    public Type getType() {
        return Type.UNSUBSCRIBE;
    }

    @Override
    public List<Object> getPayload() {
        return List.of(id, subscription);
    }
}
