package io.github.endx.rustedfabric.android.mod;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;

final class DexClassDefinitions {
    private static final int HEADER_SIZE = 112;
    private static final long ENDIAN_CONSTANT = 0x12345678L;

    private DexClassDefinitions() {
    }

    static Set<String> readBinaryNames(byte[] dex) throws ModVerificationException {
        try {
            require(dex.length >= HEADER_SIZE, "DEX header is truncated");
            require(dex[0] == 'd' && dex[1] == 'e' && dex[2] == 'x' && dex[3] == '\n'
                            && dex[7] == 0,
                    "DEX magic is invalid");
            require(u32(dex, 32) == dex.length, "DEX file size is inconsistent");
            require(u32(dex, 36) == HEADER_SIZE, "DEX header size is unsupported");
            require(u32(dex, 40) == ENDIAN_CONSTANT, "Reverse-endian DEX is unsupported");

            int stringCount = count(dex, 56, "string_ids_size");
            int stringOffset = offset(dex, 60, "string_ids_off");
            int typeCount = count(dex, 64, "type_ids_size");
            int typeOffset = offset(dex, 68, "type_ids_off");
            int classCount = count(dex, 96, "class_defs_size");
            int classOffset = offset(dex, 100, "class_defs_off");
            range(dex, stringOffset, stringCount, 4, "string ids");
            range(dex, typeOffset, typeCount, 4, "type ids");
            range(dex, classOffset, classCount, 32, "class definitions");

            Set<String> classes = new LinkedHashSet<>();
            for (int i = 0; i < classCount; i++) {
                int typeIndex = checkedIndex(u32(dex, classOffset + i * 32), typeCount, "class type");
                int stringIndex = checkedIndex(u32(dex, typeOffset + typeIndex * 4),
                        stringCount, "descriptor string");
                int dataOffset = offset(dex, stringOffset + stringIndex * 4, "string_data_off");
                String descriptor = readString(dex, dataOffset);
                require(descriptor.length() >= 3 && descriptor.charAt(0) == 'L'
                                && descriptor.charAt(descriptor.length() - 1) == ';',
                        "Class descriptor is malformed");
                String binaryName = descriptor.substring(1, descriptor.length() - 1)
                        .replace('/', '.');
                require(classes.add(binaryName), "Duplicate class definition");
            }
            return classes;
        } catch (ModVerificationException expected) {
            throw expected;
        } catch (RuntimeException malformed) {
            throw invalid("DEX structure is malformed", malformed);
        }
    }

    private static String readString(byte[] dex, int offset) throws ModVerificationException {
        int cursor = offset;
        int shift = 0;
        boolean lengthTerminated = false;
        for (int i = 0; i < 5; i++) {
            require(cursor < dex.length, "DEX string length is truncated");
            int value = dex[cursor++] & 0xff;
            if ((value & 0x80) == 0) {
                lengthTerminated = true;
                break;
            }
            shift += 7;
            require(shift < 32, "DEX string length is invalid");
        }
        require(lengthTerminated, "DEX string length is unterminated");
        int end = cursor;
        while (end < dex.length && dex[end] != 0) {
            end++;
        }
        require(end < dex.length, "DEX string data is unterminated");
        return new String(dex, cursor, end - cursor, StandardCharsets.UTF_8);
    }

    private static int count(byte[] data, int offset, String field) throws ModVerificationException {
        long value = u32(data, offset);
        require(value <= Integer.MAX_VALUE, field + " exceeds supported limits");
        return (int) value;
    }

    private static int offset(byte[] data, int fieldOffset, String field)
            throws ModVerificationException {
        long value = u32(data, fieldOffset);
        require(value <= data.length, field + " is out of bounds");
        return (int) value;
    }

    private static int checkedIndex(long value, int count, String label)
            throws ModVerificationException {
        require(value < count, label + " index is out of bounds");
        return (int) value;
    }

    private static void range(byte[] data, int offset, int count, int itemSize, String label)
            throws ModVerificationException {
        long end = (long) offset + (long) count * itemSize;
        require(offset >= 0 && end <= data.length, label + " are out of bounds");
    }

    private static long u32(byte[] data, int offset) {
        if (offset < 0 || offset + 4 > data.length) {
            throw new IndexOutOfBoundsException();
        }
        return ((long) data[offset] & 0xff)
                | (((long) data[offset + 1] & 0xff) << 8)
                | (((long) data[offset + 2] & 0xff) << 16)
                | (((long) data[offset + 3] & 0xff) << 24);
    }

    private static void require(boolean condition, String message) throws ModVerificationException {
        if (!condition) {
            throw invalid(message, null);
        }
    }

    private static ModVerificationException invalid(String message, Throwable cause) {
        return cause == null
                ? new ModVerificationException(ModVerificationException.Reason.INVALID_DEX, message)
                : new ModVerificationException(ModVerificationException.Reason.INVALID_DEX, message, cause);
    }
}
