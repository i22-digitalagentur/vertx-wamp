package io.vertx.wamp.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.wamp.MessageFactory;
import io.vertx.wamp.MessageTransport;
import io.vertx.wamp.Uri;
import io.vertx.wamp.WAMPMessage;

import java.util.function.Consumer;

public class WebsocketMessageTransport implements MessageTransport {
    private final ServerWebSocket websocket;
    private Consumer<WAMPMessage> messageConsumer = null;
    private Consumer<Uri> errorConsumer = null;

    public WebsocketMessageTransport(ServerWebSocket websocket) {
        this.websocket = websocket;
        websocket.textMessageHandler(this::onTextMessageReceived);
    }

    private void onTextMessageReceived(String s) {
        Object decoded = Json.decodeValue(s);
        if (!(decoded instanceof JsonArray)) {
            errorConsumer.accept(Uri.PROTOCOL_VIOLATION);
            return;
        }
        JsonArray arrayData = (JsonArray) decoded;
        WAMPMessage message = MessageFactory.parseMessage(arrayData);
        if (messageConsumer != null) {
            messageConsumer.accept(message);
        }
    }

    @Override
    public void sendMessage(WAMPMessage message, Handler<AsyncResult<Void>> completionHandler) {
        final String json = encodeJson(message);
        if (completionHandler != null) {
            websocket.writeTextMessage(json, completionHandler);
        } else {
            websocket.writeTextMessage(json);
        }
    }

    private String encodeJson(WAMPMessage message) {
        JsonArray encoded = new JsonArray();
        encoded.add(message.getType().getMessageCode());
        for (Object entry : message.getPayload()) {
            encoded.add(entry);
        }
        return encoded.encode();
    }

    @Override
    public void setReceiveHandler(Consumer<WAMPMessage> consumer) {
        if (consumer == null) {
            throw new IllegalArgumentException("Invalid receive handler: null");
        }
        this.messageConsumer = consumer;
    }

    @Override
    public void setErrorHandler(Consumer<Uri> consumer) {
        if (consumer == null) {
            throw new IllegalArgumentException("Invalid error handler: null");
        }
        this.errorConsumer = consumer;
    }

    @Override
    public void close(Promise<Void> promise) {
        websocket.close(voidResult -> {
            if (promise != null) {
                promise.complete();
            }
        });
    }
}
