package io.github.endx.rustedfabric.android.inspector;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

final class DexParser {
    void add(byte[] bytes, DexInventory inventory) {
        if (bytes.length < 112 || bytes[0] != 'd' || bytes[1] != 'e' || bytes[2] != 'x'
                || bytes[3] != '\n' || bytes[7] != 0) {
            throw new IllegalArgumentException("Invalid DEX header");
        }
        long declaredFileSize = u32(bytes, 32);
        long headerSize = u32(bytes, 36);
        if (declaredFileSize != bytes.length || headerSize < 112 || headerSize > bytes.length) {
            throw new IllegalArgumentException("Inconsistent DEX file or header size");
        }

        int stringCount = count(bytes, 56, "string_ids");
        int stringOffset = offset(bytes, 60, stringCount, 4, "string_ids");
        int typeCount = count(bytes, 64, "type_ids");
        int typeOffset = offset(bytes, 68, typeCount, 4, "type_ids");
        int protoCount = count(bytes, 72, "proto_ids");
        offset(bytes, 76, protoCount, 12, "proto_ids");
        int fieldCount = count(bytes, 80, "field_ids");
        offset(bytes, 84, fieldCount, 8, "field_ids");
        int methodCount = count(bytes, 88, "method_ids");
        offset(bytes, 92, methodCount, 8, "method_ids");
        int classCount = count(bytes, 96, "class_defs");
        int classOffset = offset(bytes, 100, classCount, 32, "class_defs");

        int[] stringDataOffsets = new int[stringCount];
        for (int i = 0; i < stringCount; i++) {
            stringDataOffsets[i] = absoluteOffset(bytes, stringOffset + i * 4, "string_data");
        }
        int[] descriptorStringIndexes = new int[typeCount];
        for (int i = 0; i < typeCount; i++) {
            long index = u32(bytes, typeOffset + i * 4);
            if (index >= stringCount) {
                throw new IllegalArgumentException("DEX type references an invalid string index");
            }
            descriptorStringIndexes[i] = (int) index;
        }
        for (int i = 0; i < classCount; i++) {
            long classIndex = u32(bytes, classOffset + i * 32);
            if (classIndex >= typeCount) {
                throw new IllegalArgumentException("DEX class definition references an invalid type index");
            }
            int stringIndex = descriptorStringIndexes[(int) classIndex];
            inventory.classDescriptors.add(readDexString(bytes, stringDataOffsets[stringIndex]));
        }

        inventory.dexFiles++;
        inventory.strings += stringCount;
        inventory.types += typeCount;
        inventory.prototypes += protoCount;
        inventory.fields += fieldCount;
        inventory.methods += methodCount;
        inventory.classDefinitions += classCount;
        inventory.dexSha256.add(Hashing.sha256(bytes));
    }

    private static String readDexString(byte[] bytes, int offset) {
        int cursor = skipUleb128(bytes, offset);
        int end = cursor;
        while (end < bytes.length && bytes[end] != 0) {
            end++;
        }
        if (end == bytes.length) {
            throw new IllegalArgumentException("Unterminated DEX string_data item");
        }
        // Class descriptors are ASCII. UTF-8 is sufficient here and avoids exposing other DEX strings.
        return new String(bytes, cursor, end - cursor, StandardCharsets.UTF_8);
    }

    private static int skipUleb128(byte[] bytes, int offset) {
        int cursor = offset;
        for (int i = 0; i < 5; i++) {
            if (cursor >= bytes.length) {
                throw new IllegalArgumentException("Truncated DEX ULEB128 value");
            }
            if ((bytes[cursor++] & 0x80) == 0) {
                return cursor;
            }
        }
        throw new IllegalArgumentException("Oversized DEX ULEB128 value");
    }

    private static int count(byte[] bytes, int headerOffset, String label) {
        long value = u32(bytes, headerOffset);
        if (value > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("DEX " + label + " count is too large");
        }
        return (int) value;
    }

    private static int offset(byte[] bytes, int headerOffset, int count, int itemSize, String label) {
        long value = u32(bytes, headerOffset);
        if (count == 0) {
            if (value != 0) {
                throw new IllegalArgumentException("Empty DEX " + label + " table has a non-zero offset");
            }
            return 0;
        }
        long end = value + (long) count * itemSize;
        if (value < 0 || value > Integer.MAX_VALUE || end > bytes.length) {
            throw new IllegalArgumentException("DEX " + label + " table exceeds file bounds");
        }
        return (int) value;
    }

    private static int absoluteOffset(byte[] bytes, int offset, String label) {
        long value = u32(bytes, offset);
        if (value >= bytes.length) {
            throw new IllegalArgumentException("DEX " + label + " offset exceeds file bounds");
        }
        return (int) value;
    }

    private static long u32(byte[] bytes, int offset) {
        if (offset < 0 || offset + 4 > bytes.length) {
            throw new IllegalArgumentException("Unexpected end of DEX file");
        }
        return ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.LITTLE_ENDIAN).getInt() & 0xffffffffL;
    }
}
