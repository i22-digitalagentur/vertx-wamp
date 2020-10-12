package io.vertx.wamp.messages;

import io.vertx.wamp.WAMPMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.vertx.wamp.messages.Util.addArgsAndArgsKw;

public class EventMessage implements WAMPMessage {
    private final long subscriptionId;
    private final long publicationId;
    private final Map<String, Object> details;
    private final List<Object> arguments;
    private final Map<String, Object> argumentsKw;

    public EventMessage(long subscriptionId,
                        long publicationId,
                        Map<String, Object> details,
                        List<Object> arguments,
                        Map<String, Object> argumentsKw) {
        this.subscriptionId = subscriptionId;
        this.publicationId = publicationId;
        this.details = details;
        this.arguments = arguments;
        this.argumentsKw = argumentsKw;
    }

    @Override
    public Type getType() {
        return Type.EVENT;
    }

    @Override
    public List<Object> getPayload() {
        ArrayList<Object> res = new ArrayList<>();
        res.add(subscriptionId);
        res.add(publicationId);
        res.add(details);
        addArgsAndArgsKw(res, arguments, argumentsKw);
        return res;
    }

    public long getSubscriptionId() {
        return subscriptionId;
    }

    public long getPublicationId() {
        return publicationId;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public List<Object> getArguments() {
        return arguments;
    }

    public Map<String, Object> getArgumentsKw() {
        return argumentsKw;
    }
}
