package io.vertx.wamp.impl;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.wamp.MessageDecoder;
import io.vertx.wamp.WAMPMessage;
import io.vertx.wamp.WAMPProtocolException;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;

public class JsonMessageDecoder implements MessageDecoder<String, JsonArray> {
    public Map.Entry<WAMPMessage.Type, JsonArray> parseMessage(String message) {
        Object decoded = Json.decodeValue(message);
        if (!(decoded instanceof JsonArray) || ((JsonArray) decoded).size() == 0) {
            throw new WAMPProtocolException("Invalid message");
        }
        JsonArray jsonArray = (JsonArray) decoded;
        WAMPMessage.Type messageType = WAMPMessage.Type.findByCode(getInteger(jsonArray, 0));
        jsonArray.remove(0);
        return new AbstractMap.SimpleImmutableEntry(messageType, jsonArray);
    }

    @Override
    public String getString(JsonArray data, int idx) {
        return data.getString(idx);
    }

    @Override
    public Map<String, Object> getMap(JsonArray data, int idx) {
        if (data.size() <= idx) {
            return null;
        }
        return data.getJsonObject(idx).getMap();
    }

    @Override
    public Integer getInteger(JsonArray data, int idx) {
        return data.getInteger(idx);
    }

    @Override
    public Long getLong(JsonArray data, int idx) {
        return data.getLong(idx);
    }

    @Override
    public List<Object> getList(JsonArray data, int idx) {
        if (data.size() <= idx) {
            return null;
        }
        return (List<Object>) data.getJsonArray(idx).getList();
    }
}
