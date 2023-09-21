package com.alibaba.innodb.java.reader.column;

import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class JSONColumnParser {
    private byte[] stream;
    int index;
    StringBuffer out;

    public JSONColumnParser(byte[] stream) {
        this.stream = stream;
        this.out = new StringBuffer();
        this.index = 0;
    }

    public String parse() {
        if (stream.length == 0) {
            return null;
        }

        if (stream[index] == JSON_TYPES.ARRAY|| stream[index] == JSON_TYPES.LONG_ARRAY) {
            boolean isLarge = stream[index] == JSON_TYPES.LONG_ARRAY;
            index++;

            // small json array
            readArray(1,  isLarge);
        } else {
            log.error("The only supported types are JSON arrays");
        }
        return out.toString();
    }

    int shortLittle() {
        int i = ((stream[index+1]&0xff)<<8)+(stream[index]&0xff);
        index = index + 2;
        return i;
    }

    public int intLittle() {
        byte[] buffer = new byte[4];
        System.arraycopy(stream, index, buffer, 0, 4);
        index = index + 4;
        return ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN ).getInt();
    }

    public double doubleLittle() {
        byte[] buffer = new byte[8];
        System.arraycopy(stream, index, buffer, 0, 8);
        return ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN ).getDouble();
    }

    public long longLittle() {
        byte[] buffer = new byte[8];
        System.arraycopy(stream, index, buffer, 0, 8);
        return ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN ).getLong();
    }

    int shorBig() {
        int i = ((stream[index]&0xff)<<8)+(stream[index+1]&0xff);
        index = index + 2;
        return i;
    }

    void readObject (int type) {
        switch (type) {
            case JSON_TYPES.INTEGER:
                Integer integer = shortLittle();
                out.append(integer);
                return;
            case JSON_TYPES.FLOAT:
                Double doubleValue = doubleLittle();
                // THIS IS A CUSTOM PATCH!!!
                if (doubleValue > 10000000) {
                    // lat lon
                    Long lvalue = doubleValue.longValue();
                    out.append(lvalue);
                } else {
                    out.append(doubleValue);
                }
                return;
            case JSON_TYPES.LONG:
                Long lon = longLittle();
                out.append(lon);
                return;
            case JSON_TYPES.ARRAY:
                int offset = shorBig();
                readArray(0, false);
                return;
            default:
                log.error("Unsupported type: " + type);
        }
    }

    Type readType (boolean isLarge) {
        Type type = new Type();
        type.type = stream[index];
        index++;
        if (isLarge) {
            type.offset = intLittle();
        } else {
            type.offset = shortLittle();
        }
        return type;
    }

    void readArray(int parentOffset, boolean isLarge) {
        index = parentOffset;
        int length;
        int myOffset;
        if (isLarge) {
            length = intLittle();
            myOffset = intLittle();
        } else {
            length = shortLittle();
            myOffset = shortLittle();
        }
        out.append("[");
        List<Type> types = new ArrayList<>();
        for (int i=0 ; i<length ; i++) {
            types.add(readType(isLarge));
        }
        for (int i=0 ; i<length ; i++) {
            Type type = types.get(i);
            switch (type.type) {
                case JSON_TYPES.INTEGER:
                case 7:
                case 4:
                    out.append(type.offset);
                    break;
                case JSON_TYPES.ARRAY:
                    readArray(type.offset + 1, false);
                    break;
                default:
                    index = parentOffset + type.offset;
                    readObject(type.type);
                    break;
            }
            if (i < types.size() - 1) {
                out.append(", ");
            }
        }
        out.append("]");
    }

    static class Type {
        public int type;
        public int offset;
    }

    static class JSON_TYPES {
        static final int ARRAY = 2;
        static final int LONG_ARRAY = 3;
        static final int INTEGER = 5;
        static final int FLOAT = 11;
        static final int LONG = 10;
    }
}
