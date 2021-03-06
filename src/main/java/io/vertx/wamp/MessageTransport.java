package io.vertx.wamp;

import io.vertx.core.AsyncResult;
import io.vertx.core.Closeable;
import io.vertx.core.Handler;
import java.io.IOException;
import java.util.function.Consumer;

public interface MessageTransport extends Closeable {

  default void sendMessage(WAMPMessage message) throws IOException {
    sendMessage(message, null);
  }

  void sendMessage(WAMPMessage message, Handler<AsyncResult<Void>> completeHandler)
      throws IOException;

  void setReceiveHandler(Consumer<WAMPMessage> consumer);

  void setErrorHandler(Consumer<Uri> consumer);

  default void close() {
    close(null);
  }
}
