package io.vertx.wamp.impl;

import io.vertx.core.buffer.Buffer;
import io.vertx.wamp.MessageEncoder;
import io.vertx.wamp.WAMPMessage;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

public class MsgPackMessageEncoder implements MessageEncoder<Buffer> {
    @Override
    public Buffer encode(WAMPMessage message) throws IOException {
        try (MessageBufferPacker packer = MessagePack.newDefaultBufferPacker()) {
            List<Object> payload = message.getPayload();
            packer.packArrayHeader(payload.size() + 1);
            packer.packInt(message.getType().getMessageCode());
            for (Object entry : message.getPayload()) {
                packValue(packer, entry);
            }
            return Buffer.buffer(packer.toByteArray());
        }
    }

    private void packValue(MessageBufferPacker packer, Object value) throws IOException {
        if (value instanceof Integer) {
            packer.packInt((Integer) value);
        } else if (value instanceof Long) {
            packer.packLong((Long) value);
        } else if (value instanceof List) {
            packList(packer, (List<Object>) value);
        } else if (value instanceof Map) {
            packMap(packer, (Map<String, Object>) value);
        } else if (value instanceof ByteBuffer) {
            ByteBuffer buffer = (ByteBuffer) value;
            packer.packBinaryHeader(buffer.limit());
            packer.writePayload(buffer.array());
        } else if (value instanceof Buffer) {
            Buffer buffer = (Buffer) value;
            packer.packBinaryHeader(buffer.length());
            packer.writePayload(buffer.getBytes());
        } else if (value instanceof String) {
            packer.packString(value.toString());
        } else if (value instanceof Double) {
            packer.packDouble((Double) value);
        } else if (value instanceof Float) {
            packer.packFloat((Float) value);
        } else if (value instanceof Byte) {
            packer.packByte((Byte) value);
        } else if (value instanceof Boolean) {
            packer.packBoolean((Boolean) value);
        } else if (value instanceof Short) {
            packer.packShort((Short) value);
        } else if (value == null) {
            packer.packNil();
        }
    }

    private void packMap(MessageBufferPacker packer, Map<String, Object> value) throws IOException {
        Map<String, Object> map = value;
        packer.packMapHeader(map.size());
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            packer.packString(entry.getKey());
            packValue(packer, entry.getValue());
        }
    }

    private void packList(MessageBufferPacker packer, List<Object> value) throws IOException {
        List<Object> list = value;
        packer.packArrayHeader(list.size());
        for (Object entry : list) {
            packValue(packer, entry);
        }
    }
}
