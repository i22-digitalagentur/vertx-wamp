package io.vertx.wamp.impl;

import io.vertx.core.buffer.Buffer;
import io.vertx.wamp.MessageDecoder;
import io.vertx.wamp.WAMPMessage;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.value.ArrayValue;
import org.msgpack.value.IntegerValue;
import org.msgpack.value.MapValue;
import org.msgpack.value.Value;

import java.io.IOException;
import java.util.*;

public class MsgPackMessageDecoder implements MessageDecoder<Buffer, List<Object>> {

  @Override
  public Map.Entry<WAMPMessage.Type, List<Object>> parseMessage(Buffer data) throws IOException {
    List<Value> parsed = new ArrayList<>();
    try (MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(data.getBytes())) {
      while (unpacker.hasNext()) {
        parsed.add(unpacker.unpackValue());
      }
    }
    if (parsed.size() != 1) {
      throw new IOException("Invalid data");
    }
    List<Object> messageData = decodeArray(parsed.get(0).asArrayValue());
    WAMPMessage.Type messageType = WAMPMessage.Type.findByCode(getInteger(messageData, 0));
    messageData.remove(0);
    return new AbstractMap.SimpleImmutableEntry<>(messageType, messageData);
  }

  @Override
  public Integer elementCount(List<Object> data) {
    return data.size();
  }

  @Override
  public String getString(List<Object> data, int idx) {
    return data.get(idx).toString();
  }

  @Override
  public Map<String, Object> getMap(List<Object> data, int idx) {
    Object item = data.get(idx);
    if (item instanceof Map) {
      return (Map<String, Object>) item;
    }
    return Collections.emptyMap();
  }

  @Override
  public Integer getInteger(List<Object> data, int idx) {
    Object item = data.get(idx);
    if (item instanceof Integer) {
      return (Integer) item;
    } else if (item instanceof Long) {
      return ((Long) item).intValue();
    }
    return null;
  }

  @Override
  public Long getLong(List<Object> data, int idx) {
    Object item = data.get(idx);
    if (item instanceof Integer) {
      return ((Integer) item).longValue();
    } else if (item instanceof Long) {
      return (Long) item;
    }
    return null;
  }

  @Override
  public List<Object> getList(List<Object> data, int idx) {
    Object item = data.get(idx);
    if (item instanceof List) {
      return (List<Object>) item;
    }
    return Collections.emptyList();
  }

  private List<Object> decodeArray(ArrayValue value) {
    List<Object> res = new ArrayList<>();
    value.iterator().forEachRemaining(e -> res.add(decodeAny(e)));
    return res;
  }

  private Map<String, Object> decodeMap(MapValue value) {
    Map<String, Object> res = new HashMap<>();
    value.entrySet().forEach(entry -> res.put(entry.getKey().asStringValue().asString(),
        decodeAny(entry.getValue())));
    return res;
  }

  private Object decodeAny(Value value) {
    switch (value.getValueType()) {
      case BOOLEAN:
        return value.asBooleanValue().getBoolean();
      case INTEGER:
        IntegerValue intVal = value.asIntegerValue();
        if (intVal.isInIntRange()) {
          return intVal.asInt();
        } else if (intVal.isInLongRange()) {
          return intVal.asLong();
        } else {
          return intVal.asBigInteger();
        }
      case FLOAT:
        return value.asFloatValue().toDouble();
      case STRING:
        return value.asStringValue().asString();
      case BINARY:
        return value.asBinaryValue().asByteBuffer();
      case ARRAY:
        return decodeArray(value.asArrayValue());
      case MAP:
        return decodeMap(value.asMapValue());
      default:
        return null;
    }
  }
}
