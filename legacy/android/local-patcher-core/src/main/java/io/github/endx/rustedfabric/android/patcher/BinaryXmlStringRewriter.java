package io.github.endx.rustedfabric.android.patcher;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class BinaryXmlStringRewriter {
    private static final int RES_XML_TYPE = 0x0003;
    private static final int RES_STRING_POOL_TYPE = 0x0001;
    private static final int RES_XML_START_ELEMENT_TYPE = 0x0102;
    private static final int RES_XML_END_ELEMENT_TYPE = 0x0103;
    private static final int NO_INDEX = 0xffffffff;
    private static final int TYPE_STRING = 0x03;
    private static final int UTF8_FLAG = 0x00000100;
    private static final int SORTED_FLAG = 0x00000001;

    private BinaryXmlStringRewriter() {
    }

    static byte[] replace(byte[] xml, Map<String, String> replacements)
            throws PatchException {
        return rewritePool(xml, replacements, Collections.emptyList());
    }

    static byte[] addProviderQueries(byte[] xml, List<String> authorities)
            throws PatchException {
        require(authorities != null && !authorities.isEmpty(),
                "At least one Loader provider authority is required");
        List<String> additions = new ArrayList<>();
        additions.add("queries");
        additions.addAll(authorities);
        byte[] expanded = rewritePool(xml, Collections.emptyMap(), additions);
        Map<String, Integer> strings = stringIndexes(expanded);
        int queries = requiredIndex(strings, "queries");
        int provider = requiredIndex(strings, "provider");
        int application = requiredIndex(strings, "application");
        int androidNamespace = requiredIndex(strings,
                "http://schemas.android.com/apk/res/android");
        int authoritiesName = requiredIndex(strings, "authorities");

        ByteArrayOutputStream nodes = new ByteArrayOutputStream();
        writeStartElement(nodes, queries, null, 0, 0, 0);
        for (String authority : authorities) {
            int value = requiredIndex(strings, authority);
            writeStartElement(nodes, provider, androidNamespace, authoritiesName, value, value);
            writeEndElement(nodes, provider);
        }
        writeEndElement(nodes, queries);
        byte[] inserted = nodes.toByteArray();
        int applicationOffset = findStartElement(expanded, application);
        ByteArrayOutputStream output = new ByteArrayOutputStream(expanded.length + inserted.length);
        output.write(expanded, 0, applicationOffset);
        output.write(inserted, 0, inserted.length);
        output.write(expanded, applicationOffset, expanded.length - applicationOffset);
        byte[] result = output.toByteArray();
        putI32(result, 4, result.length);
        return result;
    }

    private static byte[] rewritePool(byte[] xml, Map<String, String> replacements,
                                      List<String> additions) throws PatchException {
        try {
            require(xml.length >= 8 && u16(xml, 0) == RES_XML_TYPE,
                    "AndroidManifest.xml is not binary XML");
            int declaredSize = i32(xml, 4);
            require(declaredSize == xml.length, "Binary XML size is inconsistent");

            int poolOffset = findStringPool(xml);
            int headerSize = u16(xml, poolOffset + 2);
            int chunkSize = i32(xml, poolOffset + 4);
            int stringCount = i32(xml, poolOffset + 8);
            int styleCount = i32(xml, poolOffset + 12);
            int flags = i32(xml, poolOffset + 16);
            int stringsStart = i32(xml, poolOffset + 20);
            int stylesStart = i32(xml, poolOffset + 24);
            require(headerSize == 28 && stringCount >= 0 && styleCount >= 0,
                    "Binary XML string pool header is invalid");
            require(poolOffset + chunkSize <= xml.length, "Binary XML string pool is truncated");
            require(headerSize + (long) stringCount * 4 + (long) styleCount * 4 <= chunkSize,
                    "Binary XML string indexes are truncated");

            boolean utf8 = (flags & UTF8_FLAG) != 0;
            List<String> strings = new ArrayList<>(stringCount);
            Map<String, Integer> found = new HashMap<>();
            for (int index = 0; index < stringCount; index++) {
                int relative = i32(xml, poolOffset + headerSize + index * 4);
                int absolute = poolOffset + stringsStart + relative;
                String original = utf8 ? readUtf8(xml, absolute) : readUtf16(xml, absolute);
                String replacement = replacements.get(original);
                if (replacement != null) {
                    strings.add(replacement);
                    found.put(original, found.getOrDefault(original, 0) + 1);
                } else {
                    strings.add(original);
                }
            }
            for (String required : replacements.keySet()) {
                require(found.containsKey(required),
                        "Required manifest string is missing: " + required);
            }
            for (String addition : additions) {
                if (!strings.contains(addition)) strings.add(addition);
            }

            byte[] styleOffsets = copy(xml, poolOffset + headerSize + stringCount * 4,
                    styleCount * 4);
            byte[] styleData = stylesStart == 0 ? new byte[0]
                    : copy(xml, poolOffset + stylesStart, chunkSize - stylesStart);
            byte[] rebuiltPool = buildPool(headerSize, flags & ~SORTED_FLAG, utf8, strings,
                    styleOffsets, styleCount, styleData);

            ByteArrayOutputStream output = new ByteArrayOutputStream(
                    xml.length - chunkSize + rebuiltPool.length);
            output.write(xml, 0, poolOffset);
            output.write(rebuiltPool, 0, rebuiltPool.length);
            output.write(xml, poolOffset + chunkSize,
                    xml.length - poolOffset - chunkSize);
            byte[] result = output.toByteArray();
            putI32(result, 4, result.length);
            return result;
        } catch (PatchException expected) {
            throw expected;
        } catch (RuntimeException malformed) {
            throw new PatchException(PatchException.Reason.MANIFEST_REWRITE_FAILED,
                    "Binary Android manifest is malformed", malformed);
        }
    }

    private static Map<String, Integer> stringIndexes(byte[] xml) throws PatchException {
        int pool = findStringPool(xml);
        int count = i32(xml, pool + 8);
        int flags = i32(xml, pool + 16);
        int start = i32(xml, pool + 20);
        int header = u16(xml, pool + 2);
        boolean utf8 = (flags & UTF8_FLAG) != 0;
        Map<String, Integer> result = new HashMap<>();
        for (int index = 0; index < count; index++) {
            int offset = pool + start + i32(xml, pool + header + index * 4);
            String value = utf8 ? readUtf8(xml, offset) : readUtf16(xml, offset);
            result.putIfAbsent(value, index);
        }
        return result;
    }

    private static int requiredIndex(Map<String, Integer> strings, String value)
            throws PatchException {
        Integer index = strings.get(value);
        require(index != null, "Required manifest structure string is missing: " + value);
        return index;
    }

    private static int findStartElement(byte[] xml, int nameIndex) throws PatchException {
        int offset = u16(xml, 2);
        while (offset + 8 <= xml.length) {
            int type = u16(xml, offset);
            int size = i32(xml, offset + 4);
            require(size >= 8 && offset + size <= xml.length,
                    "Binary XML node chunk is malformed");
            if (type == RES_XML_START_ELEMENT_TYPE) {
                require(size >= 36, "Binary XML start element is truncated");
                if (i32(xml, offset + 20) == nameIndex) return offset;
            }
            offset += size;
        }
        throw new PatchException(PatchException.Reason.MANIFEST_REWRITE_FAILED,
                "Binary manifest application element is missing");
    }

    private static void writeStartElement(ByteArrayOutputStream output, int elementName,
                                          Integer attributeNamespace, int attributeName,
                                          int rawValue, int typedValue) {
        int attributeCount = attributeNamespace == null ? 0 : 1;
        byte[] node = new byte[36 + attributeCount * 20];
        putU16(node, 0, RES_XML_START_ELEMENT_TYPE);
        putU16(node, 2, 16);
        putI32(node, 4, node.length);
        putI32(node, 12, NO_INDEX);
        putI32(node, 16, NO_INDEX);
        putI32(node, 20, elementName);
        putU16(node, 24, 20);
        putU16(node, 26, 20);
        putU16(node, 28, attributeCount);
        if (attributeCount == 1) {
            int attribute = 36;
            putI32(node, attribute, attributeNamespace);
            putI32(node, attribute + 4, attributeName);
            putI32(node, attribute + 8, rawValue);
            putU16(node, attribute + 12, 8);
            node[attribute + 15] = TYPE_STRING;
            putI32(node, attribute + 16, typedValue);
        }
        output.write(node, 0, node.length);
    }

    private static void writeEndElement(ByteArrayOutputStream output, int elementName) {
        byte[] node = new byte[24];
        putU16(node, 0, RES_XML_END_ELEMENT_TYPE);
        putU16(node, 2, 16);
        putI32(node, 4, node.length);
        putI32(node, 12, NO_INDEX);
        putI32(node, 16, NO_INDEX);
        putI32(node, 20, elementName);
        output.write(node, 0, node.length);
    }

    private static int findStringPool(byte[] xml) throws PatchException {
        int offset = u16(xml, 2);
        while (offset + 8 <= xml.length) {
            int type = u16(xml, offset);
            int size = i32(xml, offset + 4);
            require(size >= 8 && offset + size <= xml.length,
                    "Binary XML chunk is malformed");
            if (type == RES_STRING_POOL_TYPE) {
                return offset;
            }
            offset += size;
        }
        throw new PatchException(PatchException.Reason.MANIFEST_REWRITE_FAILED,
                "Binary XML string pool is missing");
    }

    private static byte[] buildPool(int headerSize, int flags, boolean utf8,
                                    List<String> strings, byte[] styleOffsets,
                                    int styleCount, byte[] styleData) {
        ByteArrayOutputStream stringData = new ByteArrayOutputStream();
        int[] offsets = new int[strings.size()];
        for (int index = 0; index < strings.size(); index++) {
            offsets[index] = stringData.size();
            byte[] encoded = utf8 ? encodeUtf8(strings.get(index)) : encodeUtf16(strings.get(index));
            stringData.write(encoded, 0, encoded.length);
        }
        pad4(stringData);

        int indexesSize = strings.size() * 4 + styleCount * 4;
        int stringsStart = headerSize + indexesSize;
        int stylesStart = styleCount == 0 ? 0 : stringsStart + stringData.size();
        int chunkSize = stringsStart + stringData.size() + styleData.length;
        byte[] pool = new byte[chunkSize];
        putU16(pool, 0, RES_STRING_POOL_TYPE);
        putU16(pool, 2, headerSize);
        putI32(pool, 4, chunkSize);
        putI32(pool, 8, strings.size());
        putI32(pool, 12, styleCount);
        putI32(pool, 16, flags);
        putI32(pool, 20, stringsStart);
        putI32(pool, 24, stylesStart);
        for (int index = 0; index < offsets.length; index++) {
            putI32(pool, headerSize + index * 4, offsets[index]);
        }
        System.arraycopy(styleOffsets, 0, pool, headerSize + offsets.length * 4,
                styleOffsets.length);
        byte[] stringsBytes = stringData.toByteArray();
        System.arraycopy(stringsBytes, 0, pool, stringsStart, stringsBytes.length);
        if (styleData.length > 0) {
            System.arraycopy(styleData, 0, pool, stylesStart, styleData.length);
        }
        return pool;
    }

    private static String readUtf8(byte[] data, int offset) throws PatchException {
        Length first = readLength8(data, offset);
        Length second = readLength8(data, first.next);
        require(second.next + second.value < data.length
                        && data[second.next + second.value] == 0,
                "UTF-8 manifest string is truncated");
        return new String(data, second.next, second.value, StandardCharsets.UTF_8);
    }

    private static String readUtf16(byte[] data, int offset) throws PatchException {
        Length length = readLength16(data, offset);
        long byteLength = (long) length.value * 2;
        require(byteLength <= Integer.MAX_VALUE
                        && length.next + byteLength + 1 < data.length
                        && data[(int) (length.next + byteLength)] == 0
                        && data[(int) (length.next + byteLength + 1)] == 0,
                "UTF-16 manifest string is truncated");
        return new String(data, length.next, (int) byteLength, StandardCharsets.UTF_16LE);
    }

    private static byte[] encodeUtf8(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream output = new ByteArrayOutputStream(bytes.length + 5);
        writeLength8(output, value.length());
        writeLength8(output, bytes.length);
        output.write(bytes, 0, bytes.length);
        output.write(0);
        return output.toByteArray();
    }

    private static byte[] encodeUtf16(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_16LE);
        ByteArrayOutputStream output = new ByteArrayOutputStream(bytes.length + 6);
        writeLength16(output, value.length());
        output.write(bytes, 0, bytes.length);
        output.write(0);
        output.write(0);
        return output.toByteArray();
    }

    private static Length readLength8(byte[] data, int offset) throws PatchException {
        require(offset < data.length, "Manifest string length is truncated");
        int first = data[offset] & 0xff;
        if ((first & 0x80) == 0) return new Length(first, offset + 1);
        require(offset + 1 < data.length, "Manifest string length is truncated");
        return new Length(((first & 0x7f) << 8) | (data[offset + 1] & 0xff), offset + 2);
    }

    private static Length readLength16(byte[] data, int offset) throws PatchException {
        require(offset + 1 < data.length, "Manifest string length is truncated");
        int first = u16(data, offset);
        if ((first & 0x8000) == 0) return new Length(first, offset + 2);
        require(offset + 3 < data.length, "Manifest string length is truncated");
        return new Length(((first & 0x7fff) << 16) | u16(data, offset + 2), offset + 4);
    }

    private static void writeLength8(ByteArrayOutputStream output, int value) {
        if (value > 0x7f) output.write(((value >>> 8) & 0x7f) | 0x80);
        output.write(value & 0xff);
    }

    private static void writeLength16(ByteArrayOutputStream output, int value) {
        if (value > 0x7fff) {
            writeU16(output, ((value >>> 16) & 0x7fff) | 0x8000);
        }
        writeU16(output, value & 0xffff);
    }

    private static void pad4(ByteArrayOutputStream output) {
        while ((output.size() & 3) != 0) output.write(0);
    }

    private static byte[] copy(byte[] source, int offset, int length) {
        byte[] result = new byte[length];
        System.arraycopy(source, offset, result, 0, length);
        return result;
    }

    private static int u16(byte[] data, int offset) {
        return (data[offset] & 0xff) | ((data[offset + 1] & 0xff) << 8);
    }

    private static int i32(byte[] data, int offset) {
        return u16(data, offset) | (u16(data, offset + 2) << 16);
    }

    private static void putU16(byte[] data, int offset, int value) {
        data[offset] = (byte) value;
        data[offset + 1] = (byte) (value >>> 8);
    }

    private static void putI32(byte[] data, int offset, int value) {
        putU16(data, offset, value);
        putU16(data, offset + 2, value >>> 16);
    }

    private static void writeU16(ByteArrayOutputStream output, int value) {
        output.write(value & 0xff);
        output.write((value >>> 8) & 0xff);
    }

    private static void require(boolean condition, String message) throws PatchException {
        if (!condition) {
            throw new PatchException(PatchException.Reason.MANIFEST_REWRITE_FAILED, message);
        }
    }

    private static final class Length {
        private final int value;
        private final int next;

        private Length(int value, int next) {
            this.value = value;
            this.next = next;
        }
    }
}
