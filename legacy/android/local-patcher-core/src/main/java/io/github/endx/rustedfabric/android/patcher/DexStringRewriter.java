package io.github.endx.rustedfabric.android.patcher;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.Adler32;

final class DexStringRewriter {
    private static final int HEADER_SIZE = 112;

    private DexStringRewriter() {
    }

    static byte[] replaceEqualWidth(byte[] source, Map<String, String> replacements)
            throws PatchException {
        try {
            require(source.length >= HEADER_SIZE, "DEX header is truncated");
            require(source[0] == 'd' && source[1] == 'e' && source[2] == 'x'
                            && source[3] == '\n' && source[7] == 0,
                    "DEX magic is invalid");
            require(u32(source, 32) == source.length && u32(source, 36) == HEADER_SIZE,
                    "DEX header size is inconsistent");
            int stringCount = checkedInt(u32(source, 56), "DEX string count");
            int stringIdsOffset = checkedInt(u32(source, 60), "DEX string table offset");
            require((long) stringIdsOffset + (long) stringCount * 4 <= source.length,
                    "DEX string table is truncated");
            for (Map.Entry<String, String> replacement : replacements.entrySet()) {
                require(replacement.getKey().getBytes(StandardCharsets.UTF_8).length
                                == replacement.getValue().getBytes(StandardCharsets.UTF_8).length
                                && replacement.getKey().length() == replacement.getValue().length(),
                        "DEX v1 replacement must preserve UTF-8 and UTF-16 width");
            }

            byte[] result = source.clone();
            Map<String, Integer> found = new HashMap<>();
            for (int index = 0; index < stringCount; index++) {
                int dataOffset = checkedInt(u32(source, stringIdsOffset + index * 4),
                        "DEX string data offset");
                Uleb length = readUleb(source, dataOffset);
                int end = length.next;
                while (end < source.length && source[end] != 0) end++;
                require(end < source.length, "DEX string is unterminated");
                String value = new String(source, length.next, end - length.next,
                        StandardCharsets.UTF_8);
                String replacement = replacements.get(value);
                if (replacement != null) {
                    byte[] bytes = replacement.getBytes(StandardCharsets.UTF_8);
                    require(bytes.length == end - length.next
                                    && length.value == replacement.length(),
                            "DEX replacement width changed");
                    System.arraycopy(bytes, 0, result, length.next, bytes.length);
                    found.put(value, found.getOrDefault(value, 0) + 1);
                }
            }
            for (String required : replacements.keySet()) {
                require(found.containsKey(required), "Required DEX string is missing: " + required);
            }
            updateSignatureAndChecksum(result);
            return result;
        } catch (PatchException expected) {
            throw expected;
        } catch (RuntimeException malformed) {
            throw new PatchException(PatchException.Reason.DEX_REWRITE_FAILED,
                    "DEX string table is malformed", malformed);
        }
    }

    static void validateDex(byte[] dex) throws PatchException {
        require(dex.length >= HEADER_SIZE && dex[0] == 'd' && dex[1] == 'e'
                        && dex[2] == 'x' && dex[3] == '\n' && dex[7] == 0,
                "Bootstrap DEX magic is invalid");
        require(u32(dex, 32) == dex.length && u32(dex, 36) == HEADER_SIZE,
                "Bootstrap DEX size is invalid");
    }

    static Set<String> definedClasses(byte[] dex) throws PatchException {
        validateDex(dex);
        int stringCount = checkedInt(u32(dex, 56), "DEX string count");
        int stringOffset = checkedInt(u32(dex, 60), "DEX string table offset");
        int typeCount = checkedInt(u32(dex, 64), "DEX type count");
        int typeOffset = checkedInt(u32(dex, 68), "DEX type table offset");
        int classCount = checkedInt(u32(dex, 96), "DEX class count");
        int classOffset = checkedInt(u32(dex, 100), "DEX class table offset");
        require((long) stringOffset + (long) stringCount * 4 <= dex.length,
                "DEX string table is truncated");
        require((long) typeOffset + (long) typeCount * 4 <= dex.length,
                "DEX type table is truncated");
        require((long) classOffset + (long) classCount * 32 <= dex.length,
                "DEX class table is truncated");
        Set<String> classes = new LinkedHashSet<>();
        for (int index = 0; index < classCount; index++) {
            int typeIndex = checkedInt(u32(dex, classOffset + index * 32), "DEX class type");
            require(typeIndex < typeCount, "DEX class type index is invalid");
            int stringIndex = checkedInt(u32(dex, typeOffset + typeIndex * 4),
                    "DEX descriptor string");
            require(stringIndex < stringCount, "DEX descriptor string index is invalid");
            int dataOffset = checkedInt(u32(dex, stringOffset + stringIndex * 4),
                    "DEX descriptor offset");
            Uleb length = readUleb(dex, dataOffset);
            int end = length.next;
            while (end < dex.length && dex[end] != 0) end++;
            require(end < dex.length, "DEX class descriptor is unterminated");
            String descriptor = new String(dex, length.next, end - length.next,
                    StandardCharsets.UTF_8);
            require(descriptor.startsWith("L") && descriptor.endsWith(";"),
                    "DEX class descriptor is malformed");
            require(classes.add(descriptor), "DEX defines a class more than once");
        }
        return classes;
    }

    private static void updateSignatureAndChecksum(byte[] dex) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            sha1.update(dex, 32, dex.length - 32);
            byte[] signature = sha1.digest();
            System.arraycopy(signature, 0, dex, 12, signature.length);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
        Adler32 adler = new Adler32();
        adler.update(dex, 12, dex.length - 12);
        long checksum = adler.getValue();
        dex[8] = (byte) checksum;
        dex[9] = (byte) (checksum >>> 8);
        dex[10] = (byte) (checksum >>> 16);
        dex[11] = (byte) (checksum >>> 24);
    }

    private static Uleb readUleb(byte[] data, int offset) throws PatchException {
        int value = 0;
        int cursor = offset;
        for (int shift = 0; shift < 35; shift += 7) {
            require(cursor < data.length, "DEX ULEB128 is truncated");
            int current = data[cursor++] & 0xff;
            if (shift == 28) {
                require((current & 0xf0) == 0, "DEX ULEB128 exceeds 32 bits");
            }
            value |= (current & 0x7f) << shift;
            if ((current & 0x80) == 0) return new Uleb(value, cursor);
        }
        throw new PatchException(PatchException.Reason.DEX_REWRITE_FAILED,
                "DEX ULEB128 is invalid");
    }

    private static int checkedInt(long value, String label) throws PatchException {
        require(value <= Integer.MAX_VALUE, label + " exceeds supported limits");
        return (int) value;
    }

    private static long u32(byte[] data, int offset) throws PatchException {
        require(offset >= 0 && offset + 4 <= data.length, "DEX field is out of bounds");
        return ((long) data[offset] & 0xff)
                | (((long) data[offset + 1] & 0xff) << 8)
                | (((long) data[offset + 2] & 0xff) << 16)
                | (((long) data[offset + 3] & 0xff) << 24);
    }

    private static void require(boolean condition, String message) throws PatchException {
        if (!condition) {
            throw new PatchException(PatchException.Reason.DEX_REWRITE_FAILED, message);
        }
    }

    private static final class Uleb {
        private final int value;
        private final int next;

        private Uleb(int value, int next) {
            this.value = value;
            this.next = next;
        }
    }
}
