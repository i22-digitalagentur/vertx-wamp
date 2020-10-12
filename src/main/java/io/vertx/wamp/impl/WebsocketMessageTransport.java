package io.vertx.wamp.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.wamp.MessageFactory;
import io.vertx.wamp.MessageTransport;
import io.vertx.wamp.Uri;
import io.vertx.wamp.WAMPMessage;

import java.io.IOException;
import java.util.function.Consumer;

public class WebsocketMessageTransport implements MessageTransport {
  private final ServerWebSocket websocket;
  private final JsonMessageDecoder jsonDecoder = new JsonMessageDecoder();
  private final MsgPackMessageDecoder msgPackDecoder = new MsgPackMessageDecoder();
  private final JsonMessageEncoder jsonEncoder = new JsonMessageEncoder();
  private final MsgPackMessageEncoder msgPackEncoder = new MsgPackMessageEncoder();

  private Consumer<WAMPMessage> messageConsumer = null;
  private Consumer<Uri> errorConsumer = null;

  public WebsocketMessageTransport(ServerWebSocket websocket) {
    this.websocket = websocket;
    websocket.textMessageHandler(this::onTextMessageReceived);
    websocket.binaryMessageHandler(this::onBinaryMessageReceived);
  }

  private void onTextMessageReceived(String s) {
    WAMPMessage message = null;
    try {
      message = MessageFactory.parseMessage(s, jsonDecoder);
      dispatchMessage(message);
    } catch (IOException e) {
      errorConsumer.accept(Uri.PROTOCOL_VIOLATION);
    }
  }

  private void onBinaryMessageReceived(Buffer data) {
    WAMPMessage message = null;
    try {
      message = MessageFactory.parseMessage(data, msgPackDecoder);
      dispatchMessage(message);
    } catch (IOException e) {
      errorConsumer.accept(Uri.PROTOCOL_VIOLATION);
    }
  }

  private void dispatchMessage(WAMPMessage message) {
    if (message == null) {
      if (errorConsumer != null) {
        errorConsumer.accept(Uri.PROTOCOL_VIOLATION);
      }
    } else if (messageConsumer != null) {
      messageConsumer.accept(message);
    }
  }

  @Override
  public void sendMessage(WAMPMessage message, Handler<AsyncResult<Void>> completionHandler) throws IOException {
    // if the socket is already closed, don't try to send any message anymore
    if (websocket.isClosed()) {
      throw new IOException("Transport is closed");
    }
    if (websocket.subProtocol().equalsIgnoreCase("wamp.2.msgpack")) {
      final Buffer buffer = msgPackEncoder.encode(message);
      if (completionHandler != null) {
        websocket.writeBinaryMessage(buffer, completionHandler);
      } else {
        websocket.writeBinaryMessage(buffer);
      }
    } else {
      final String json = jsonEncoder.encode(message);
      if (completionHandler != null) {
        websocket.writeTextMessage(json, completionHandler);
      } else {
        websocket.writeTextMessage(json);
      }
    }
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
    if (websocket.isClosed()) {
      if (promise != null) {
        promise.complete();
      }
    } else {
      websocket.close(voidResult -> {
        if (promise != null) {
          promise.complete();
        }
      });
    }
  }
}
