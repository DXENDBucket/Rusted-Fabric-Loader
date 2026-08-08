package io.github.endx.rustedfabric.android.inspector;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class BinaryAndroidManifestParser {
    private static final int XML = 0x0003;
    private static final int STRING_POOL = 0x0001;
    private static final int START_ELEMENT = 0x0102;
    private static final int END_ELEMENT = 0x0103;
    private static final long NO_INDEX = 0xffffffffL;

    AndroidManifestInfo parse(byte[] bytes) {
        if (bytes.length < 8 || u16(bytes, 0) != XML) {
            throw new IllegalArgumentException("AndroidManifest.xml is not binary Android XML");
        }
        int documentSize = checkedSize(u32(bytes, 4), bytes.length, "XML document");
        int offset = u16(bytes, 2);
        List<String> strings = null;
        AndroidManifestInfo info = new AndroidManifestInfo();
        String activeComponent = null;
        boolean inIntentFilter = false;
        boolean hasMainAction = false;
        boolean hasLauncherCategory = false;

        while (offset + 8 <= documentSize) {
            int type = u16(bytes, offset);
            int headerSize = u16(bytes, offset + 2);
            int chunkSize = checkedSize(u32(bytes, offset + 4), documentSize - offset, "XML chunk");
            if (headerSize < 8 || chunkSize < headerSize) {
                throw new IllegalArgumentException("Malformed binary XML chunk at " + offset);
            }

            if (type == STRING_POOL) {
                strings = readStringPool(bytes, offset, headerSize, chunkSize);
            } else if (type == START_ELEMENT) {
                requireStrings(strings);
                String element = string(strings, u32(bytes, offset + 20));
                List<Attribute> attributes = readAttributes(bytes, offset, chunkSize, strings);
                if ("manifest".equals(element)) {
                    info.packageName = text(attributes, "package");
                    info.versionName = text(attributes, "versionName");
                    info.versionCode = number(attributes, "versionCode");
                    info.compileSdk = number(attributes, "compileSdkVersion");
                } else if ("uses-sdk".equals(element)) {
                    info.minSdk = number(attributes, "minSdkVersion");
                    info.targetSdk = number(attributes, "targetSdkVersion");
                } else if ("uses-permission".equals(element)) {
                    String permission = text(attributes, "name");
                    if (permission != null) {
                        info.permissions.add(permission);
                    }
                } else if ("application".equals(element)) {
                    info.applicationClass = resolveClass(info.packageName, text(attributes, "name"));
                } else if ("activity".equals(element) || "activity-alias".equals(element)) {
                    activeComponent = resolveClass(info.packageName, text(attributes,
                            "activity-alias".equals(element) ? "targetActivity" : "name"));
                } else if ("intent-filter".equals(element) && activeComponent != null) {
                    inIntentFilter = true;
                    hasMainAction = false;
                    hasLauncherCategory = false;
                } else if ("action".equals(element) && inIntentFilter) {
                    hasMainAction |= "android.intent.action.MAIN".equals(text(attributes, "name"));
                } else if ("category".equals(element) && inIntentFilter) {
                    hasLauncherCategory |= "android.intent.category.LAUNCHER".equals(text(attributes, "name"));
                }
            } else if (type == END_ELEMENT) {
                requireStrings(strings);
                String element = string(strings, u32(bytes, offset + 20));
                if ("intent-filter".equals(element) && inIntentFilter) {
                    if (hasMainAction && hasLauncherCategory && info.launcherActivity == null) {
                        info.launcherActivity = activeComponent;
                    }
                    inIntentFilter = false;
                } else if ("activity".equals(element) || "activity-alias".equals(element)) {
                    activeComponent = null;
                    inIntentFilter = false;
                }
            }
            offset += chunkSize;
        }
        info.finish();
        return info;
    }

    private static List<String> readStringPool(byte[] bytes, int offset, int headerSize, int chunkSize) {
        if (headerSize < 28 || offset + headerSize > bytes.length) {
            throw new IllegalArgumentException("Malformed Android string pool header");
        }
        int count = checkedSize(u32(bytes, offset + 8), Integer.MAX_VALUE, "string count");
        long flags = u32(bytes, offset + 16);
        int stringsStart = checkedSize(u32(bytes, offset + 20), chunkSize, "strings start");
        if ((long) headerSize + (long) count * 4L > chunkSize) {
            throw new IllegalArgumentException("String offsets exceed Android string pool");
        }
        boolean utf8 = (flags & 0x100L) != 0;
        List<String> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int relative = checkedSize(u32(bytes, offset + headerSize + i * 4), chunkSize, "string offset");
            int cursor = offset + stringsStart + relative;
            if (cursor < offset || cursor >= offset + chunkSize) {
                throw new IllegalArgumentException("String lies outside Android string pool");
            }
            if (utf8) {
                cursor = skipLength8(bytes, cursor, offset + chunkSize);
                Length byteLength = length8(bytes, cursor, offset + chunkSize);
                cursor = byteLength.next;
                requireRange(cursor, byteLength.value, offset + chunkSize, "UTF-8 string");
                result.add(new String(bytes, cursor, byteLength.value, StandardCharsets.UTF_8));
            } else {
                Length length = length16(bytes, cursor, offset + chunkSize);
                cursor = length.next;
                int byteLength = Math.multiplyExact(length.value, 2);
                requireRange(cursor, byteLength, offset + chunkSize, "UTF-16 string");
                result.add(new String(bytes, cursor, byteLength, StandardCharsets.UTF_16LE));
            }
        }
        return result;
    }

    private static List<Attribute> readAttributes(byte[] bytes, int offset, int chunkSize,
                                                   List<String> strings) {
        if (chunkSize < 36) {
            throw new IllegalArgumentException("Malformed start-element chunk");
        }
        int attributeStart = u16(bytes, offset + 24);
        int attributeSize = u16(bytes, offset + 26);
        int attributeCount = u16(bytes, offset + 28);
        if (attributeSize < 20) {
            throw new IllegalArgumentException("Malformed Android XML attribute size");
        }
        int cursor = offset + 16 + attributeStart;
        long end = (long) cursor + (long) attributeSize * attributeCount;
        if (cursor < offset || end > (long) offset + chunkSize) {
            throw new IllegalArgumentException("Android XML attributes exceed their chunk");
        }
        List<Attribute> result = new ArrayList<>(attributeCount);
        for (int i = 0; i < attributeCount; i++) {
            long nameIndex = u32(bytes, cursor + 4);
            long rawIndex = u32(bytes, cursor + 8);
            int valueType = bytes[cursor + 15] & 0xff;
            long valueData = u32(bytes, cursor + 16);
            String name = string(strings, nameIndex);
            String value;
            if (rawIndex != NO_INDEX) {
                value = string(strings, rawIndex);
            } else if (valueType == 0x03) {
                value = string(strings, valueData);
            } else if (valueType == 0x10 || valueType == 0x11 || valueType == 0x12) {
                value = Long.toString(valueData);
            } else {
                value = null;
            }
            result.add(new Attribute(name, value, valueData,
                    valueType == 0x10 || valueType == 0x11 || valueType == 0x12));
            cursor += attributeSize;
        }
        return result;
    }

    private static String text(List<Attribute> attributes, String name) {
        for (Attribute attribute : attributes) {
            if (name.equals(attribute.name)) {
                return attribute.value;
            }
        }
        return null;
    }

    private static Long number(List<Attribute> attributes, String name) {
        for (Attribute attribute : attributes) {
            if (name.equals(attribute.name)) {
                if (attribute.numeric) {
                    return attribute.number;
                }
                try {
                    return attribute.value == null ? null : Long.parseLong(attribute.value);
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private static String resolveClass(String packageName, String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        if (value.startsWith(".")) {
            return packageName == null ? value : packageName + value;
        }
        if (value.indexOf('.') < 0 && packageName != null) {
            return packageName + "." + value;
        }
        return value;
    }

    private static void requireStrings(List<String> strings) {
        if (strings == null) {
            throw new IllegalArgumentException("Binary XML element appeared before its string pool");
        }
    }

    private static String string(List<String> strings, long index) {
        if (index == NO_INDEX) {
            return null;
        }
        if (index < 0 || index >= strings.size()) {
            throw new IllegalArgumentException("Android XML string index is out of range: " + index);
        }
        return strings.get((int) index);
    }

    private static int skipLength8(byte[] bytes, int cursor, int limit) {
        return length8(bytes, cursor, limit).next;
    }

    private static Length length8(byte[] bytes, int cursor, int limit) {
        requireRange(cursor, 1, limit, "UTF-8 length");
        int first = bytes[cursor] & 0xff;
        if ((first & 0x80) == 0) {
            return new Length(first, cursor + 1);
        }
        requireRange(cursor, 2, limit, "UTF-8 length");
        return new Length(((first & 0x7f) << 8) | (bytes[cursor + 1] & 0xff), cursor + 2);
    }

    private static Length length16(byte[] bytes, int cursor, int limit) {
        requireRange(cursor, 2, limit, "UTF-16 length");
        int first = u16(bytes, cursor);
        if ((first & 0x8000) == 0) {
            return new Length(first, cursor + 2);
        }
        requireRange(cursor, 4, limit, "UTF-16 length");
        return new Length(((first & 0x7fff) << 16) | u16(bytes, cursor + 2), cursor + 4);
    }

    private static void requireRange(int offset, int size, int limit, String label) {
        if (offset < 0 || size < 0 || (long) offset + size > limit) {
            throw new IllegalArgumentException(label + " exceeds its binary XML chunk");
        }
    }

    private static int checkedSize(long value, int maximum, String label) {
        if (value < 0 || value > maximum) {
            throw new IllegalArgumentException(label + " is too large: " + value);
        }
        return (int) value;
    }

    private static int u16(byte[] bytes, int offset) {
        if (offset < 0 || offset + 2 > bytes.length) {
            throw new IllegalArgumentException("Unexpected end of binary Android XML");
        }
        return ByteBuffer.wrap(bytes, offset, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xffff;
    }

    private static long u32(byte[] bytes, int offset) {
        if (offset < 0 || offset + 4 > bytes.length) {
            throw new IllegalArgumentException("Unexpected end of binary Android XML");
        }
        return ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.LITTLE_ENDIAN).getInt() & 0xffffffffL;
    }

    private static final class Length {
        final int value;
        final int next;

        Length(int value, int next) {
            this.value = value;
            this.next = next;
        }
    }

    private static final class Attribute {
        final String name;
        final String value;
        final long number;
        final boolean numeric;

        Attribute(String name, String value, long number, boolean numeric) {
            this.name = name;
            this.value = value;
            this.number = number;
            this.numeric = numeric;
        }
    }
}
