package io.vertx.wamp;

public class WAMPProtocolException extends RuntimeException {
  public WAMPProtocolException(String reason) {
    super(reason);
  }
}
