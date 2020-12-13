package io.vertx.wamp;

import io.vertx.wamp.messages.*;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MessageFactory {
    private MessageFactory() {}

    // only supports messages part of the receiving / broker end
    public static <I, O> WAMPMessage parseMessage(I message, MessageDecoder<I, O> messageDecoder) throws IOException {
        Map.Entry<WAMPMessage.Type, O> decoded = messageDecoder.parseMessage(message);
        switch (decoded.getKey()) {
            case HELLO:
                return new HelloMessage(decoded.getValue(), messageDecoder);
            case ABORT:
                return new AbortMessage(decoded.getValue(), messageDecoder);
            case GOODBYE:
                return new GoodbyeMessage(decoded.getValue(), messageDecoder);
            case SUBSCRIBE:
                return new SubscribeMessage(decoded.getValue(), messageDecoder);
            case UNSUBSCRIBE:
                return new UnsubscribeMessage(decoded.getValue(), messageDecoder);
            case PUBLISH:
                return new PublishMessage(decoded.getValue(), messageDecoder);
            default:
                throw new IllegalArgumentException(String.format("Construction of %s not supported",
                        decoded.getKey().name()));
        }
    }

    static WAMPMessage createWelcomeMessage(long sessionId) {
        final Map<String, Object> roles = Map.of("broker", Collections.emptyMap());
        final Map<String, Object> details = Map.of("roles", roles, "agent", WAMPWebsocketServer.USER_AGENT);
        return new WelcomeMessage(sessionId, details);
    }

    static WAMPMessage createGoodbyeMessage(Uri reason) {
        return new GoodbyeMessage(Map.of(), reason);
    }

    static WAMPMessage createAbortMessage(Uri reason) {
        return new AbortMessage(Map.of(), reason);
    }

    static WAMPMessage createUnsubscribedMessage(long requestId) {
        return new UnsubscribedMessage(requestId);
    }

    static WAMPMessage createPublishedMessage(long requestId, long publicationId) {
        return new PublishedMessage(requestId, publicationId);
    }

    static WAMPMessage createErrorMessage(WAMPMessage.Type messageType,
                                          long requestId,
                                          Map<String, Object> details,
                                          Uri error) {
        return new ErrorMessage(messageType, requestId, details, error);
    }

    public static WAMPMessage createSubscribedMessage(long id, long subscriptionId) {
        return new SubscribedMessage(id, subscriptionId);
    }

    public static EventMessage createEvent(long subscriptionId,
                                           long publicationId,
                                           Map<String, Object> details,
                                           List<Object> arguments,
                                           Map<String, Object> argumentsKw) {
        return new EventMessage(subscriptionId,
                publicationId,
                details,
                arguments,
                argumentsKw);
    }
}
