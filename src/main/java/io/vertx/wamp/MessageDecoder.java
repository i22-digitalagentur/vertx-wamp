package io.vertx.wamp;

import java.util.List;
import java.util.Map;

public interface MessageDecoder<I, O> {
  /**
   * Parses a message from the input data. Depending on the implementation this might be strings, byte buffers etc.
   *
   * @param data the raw data
   * @return the parsed message, separated into message type and data
   */
  Map.Entry<WAMPMessage.Type, O> parseMessage(I data);

  String getString(O data, int idx);

  Map<String, Object> getMap(O data, int idx);

  Integer getInteger(O data, int idx);

  Long getLong(O data, int idx);

  List<Object> getList(O data, int idx);
}
