package io.vertx.wamp.test.server.events;

import io.vertx.wamp.messages.EventMessage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventMessageTest {
    @Test
    void testGetters() {
        EventMessage objectUnderTest = buildEventMessage();
        assertEquals(123l, objectUnderTest.getSubscriptionId());
        assertEquals(345l, objectUnderTest.getPublicationId());
        assertEquals("bar", objectUnderTest.getDetails().get("foo"));
        assertEquals("baz", objectUnderTest.getArguments().get(0));
        assertEquals(2, objectUnderTest.getArgumentsKw().get("zap"));
    }

    @Test
    void testPayload() {
        EventMessage objectUnderTest = buildEventMessage();
        List<Object> payload = objectUnderTest.getPayload();
        assertEquals(objectUnderTest.getSubscriptionId(), payload.get(0));
        assertEquals(objectUnderTest.getPublicationId(), payload.get(1));
        assertEquals(objectUnderTest.getDetails(), payload.get(2));
        assertEquals(objectUnderTest.getArguments(), payload.get(3));
        assertEquals(objectUnderTest.getArgumentsKw(), payload.get(4));
    }

    private EventMessage buildEventMessage() {
        return new EventMessage(123l,
                345l,
                Map.of("foo", "bar"),
                List.of("baz"),
                Map.of("zap", 2));
    }


}
