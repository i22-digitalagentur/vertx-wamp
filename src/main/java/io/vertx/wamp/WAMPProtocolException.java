package io.vertx.wamp;

public class WAMPProtocolException extends RuntimeException {
    WAMPProtocolException(String reason) {
        super(reason);
    }
}
