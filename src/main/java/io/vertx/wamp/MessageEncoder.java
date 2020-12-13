package io.vertx.wamp;

import java.io.IOException;

public interface MessageEncoder<O> {

  O encode(WAMPMessage message) throws IOException;
}
