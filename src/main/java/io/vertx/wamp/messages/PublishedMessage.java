package io.vertx.wamp.messages;

import io.vertx.wamp.WAMPMessage;

import java.util.List;

public class PublishedMessage implements WAMPMessage {
    private final long id;
    private final long publication;

    public PublishedMessage(long requestId, long publication) {
        this.id = requestId;
        this.publication = publication;
    }

    @Override
    public Type getType() {
        return Type.PUBLISHED;
    }

    @Override
    public List<Object> getPayload() {
        return List.of(id, publication);
    }
}
