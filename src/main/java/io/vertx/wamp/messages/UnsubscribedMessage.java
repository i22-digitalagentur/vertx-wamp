package io.vertx.wamp.messages;

import io.vertx.wamp.WAMPMessage;

import java.util.List;

public class UnsubscribedMessage implements WAMPMessage {
    private final long requestId;

    public UnsubscribedMessage(long requestId) {
        this.requestId = requestId;
    }

    public long getRequestId() {
        return requestId;
    }

    @Override
    public WAMPMessage.Type getType() {
        return Type.UNSUBSCRIBED;
    }

    @Override
    public List<Object> getPayload() {
        return List.of(requestId);
    }
}
