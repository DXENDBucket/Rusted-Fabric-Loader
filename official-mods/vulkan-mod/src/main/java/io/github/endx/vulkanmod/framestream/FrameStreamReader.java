package io.github.endx.vulkanmod.framestream;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;

/** Structurally validates a FrameStream before any record decoder or native allocation runs. */
public final class FrameStreamReader {
    private final ByteBuffer bytes;
    private final int minorVersion;
    private final int totalBytes;
    private final long frameId;
    private final long requiredResourceSequence;
    private final int flags;
    private final int width;
    private final int height;
    private final int passCount;
    private final int batchCount;
    private final Map<Integer, Section> sections;

    private FrameStreamReader(ByteBuffer bytes, int minorVersion, int totalBytes,
                              long frameId, long requiredResourceSequence, int flags,
                              int width, int height, int passCount, int batchCount,
                              Map<Integer, Section> sections) {
        this.bytes = bytes;
        this.minorVersion = minorVersion;
        this.totalBytes = totalBytes;
        this.frameId = frameId;
        this.requiredResourceSequence = requiredResourceSequence;
        this.flags = flags;
        this.width = width;
        this.height = height;
        this.passCount = passCount;
        this.batchCount = batchCount;
        this.sections = Collections.unmodifiableMap(sections);
    }

    public static FrameStreamReader read(ByteBuffer input) {
        if (input == null) throw new NullPointerException("input");
        ByteBuffer bytes = input.slice().order(ByteOrder.LITTLE_ENDIAN);
        require(bytes.remaining() >= FrameStreamFormat.FIXED_HEADER_BYTES,
                "truncated fixed header");
        require(bytes.get(0) == FrameStreamFormat.MAGIC_R
                        && bytes.get(1) == FrameStreamFormat.MAGIC_V
                        && bytes.get(2) == FrameStreamFormat.MAGIC_K
                        && bytes.get(3) == FrameStreamFormat.MAGIC_F,
                "invalid FrameStream magic");
        int major = Short.toUnsignedInt(bytes.getShort(
                FrameStreamFormat.OFFSET_MAJOR_VERSION));
        int minor = Short.toUnsignedInt(bytes.getShort(
                FrameStreamFormat.OFFSET_MINOR_VERSION));
        require(major == FrameStreamFormat.MAJOR_VERSION,
                "unsupported FrameStream major version " + major);
        require(minor <= FrameStreamFormat.MINOR_VERSION,
                "unsupported FrameStream minor version " + minor);

        int headerBytes = bytes.getInt(FrameStreamFormat.OFFSET_HEADER_BYTES);
        int totalBytes = bytes.getInt(FrameStreamFormat.OFFSET_TOTAL_BYTES);
        int sectionCount = bytes.getInt(FrameStreamFormat.OFFSET_SECTION_COUNT);
        require(sectionCount >= 0 && sectionCount <= FrameStreamFormat.MAX_SECTIONS,
                "section count is out of range: " + sectionCount);
        int expectedHeader;
        try {
            expectedHeader = Math.addExact(FrameStreamFormat.FIXED_HEADER_BYTES,
                    Math.multiplyExact(sectionCount, FrameStreamFormat.SECTION_ENTRY_BYTES));
        } catch (ArithmeticException overflow) {
            throw invalid("section directory size overflows");
        }
        require(headerBytes == expectedHeader && (headerBytes & 7) == 0,
                "invalid header/directory length: " + headerBytes);
        require(totalBytes >= headerBytes && totalBytes <= FrameStreamFormat.MAX_STREAM_BYTES,
                "total length is out of range: " + totalBytes);
        require(totalBytes == bytes.remaining(), "declared total length " + totalBytes
                + " does not match submitted length " + bytes.remaining());
        require(bytes.getInt(FrameStreamFormat.OFFSET_RESERVED) == 0,
                "reserved header field is not zero");

        long frameId = bytes.getLong(FrameStreamFormat.OFFSET_FRAME_ID);
        long sequence = bytes.getLong(FrameStreamFormat.OFFSET_REQUIRED_RESOURCE_SEQUENCE);
        require(frameId >= 0L, "frame ID uses the reserved negative range");
        require(sequence >= 0L, "resource sequence uses the reserved negative range");
        int flags = bytes.getInt(FrameStreamFormat.OFFSET_FLAGS);
        require((flags & ~FrameStreamFormat.KNOWN_FLAGS) == 0,
                "unknown frame flags: 0x" + Integer.toHexString(flags));
        int width = bytes.getInt(FrameStreamFormat.OFFSET_WIDTH);
        int height = bytes.getInt(FrameStreamFormat.OFFSET_HEIGHT);
        require(width > 0 && width <= FrameStreamFormat.MAX_DIMENSION,
                "frame width is out of range: " + width);
        require(height > 0 && height <= FrameStreamFormat.MAX_DIMENSION,
                "frame height is out of range: " + height);
        int passCount = bytes.getInt(FrameStreamFormat.OFFSET_PASS_COUNT);
        int batchCount = bytes.getInt(FrameStreamFormat.OFFSET_BATCH_COUNT);
        require(passCount > 0 && passCount <= FrameStreamFormat.MAX_PASSES,
                "pass count is out of range: " + passCount);
        require(batchCount >= 0 && batchCount <= FrameStreamFormat.MAX_BATCHES,
                "batch count is out of range: " + batchCount);

        LinkedHashMap<Integer, Section> byType = new LinkedHashMap<Integer, Section>();
        ArrayList<Section> byOffset = new ArrayList<Section>(sectionCount);
        int directory = FrameStreamFormat.FIXED_HEADER_BYTES;
        for (int index = 0; index < sectionCount; index++) {
            int type = bytes.getInt(directory);
            int offset = bytes.getInt(directory + 4);
            int length = bytes.getInt(directory + 8);
            int elements = bytes.getInt(directory + 12);
            directory += FrameStreamFormat.SECTION_ENTRY_BYTES;
            require(type != 0, "section type zero is invalid");
            require(!byType.containsKey(type), "duplicate section type "
                    + Integer.toUnsignedString(type));
            require(FrameStreamFormat.knownSection(type)
                            || (type & FrameStreamFormat.REQUIRED_EXTENSION_BIT) == 0,
                    "unknown required section " + Integer.toUnsignedString(type));
            require(offset >= headerBytes && (offset & 7) == 0,
                    "section " + Integer.toUnsignedString(type)
                            + " has an invalid offset " + offset);
            require(length >= 0, "section length is negative");
            require(elements >= 0 && elements <= FrameStreamFormat.MAX_SECTION_ELEMENTS,
                    "section element count is out of range");
            long end = (long) offset + length;
            require(end <= totalBytes, "section " + Integer.toUnsignedString(type)
                    + " extends beyond the stream");
            Section section = new Section(type, offset, length, elements);
            byType.put(type, section);
            if (length > 0) byOffset.add(section);
        }
        byOffset.sort(new Comparator<Section>() {
            @Override public int compare(Section first, Section second) {
                return Integer.compare(first.offset, second.offset);
            }
        });
        int previousEnd = headerBytes;
        for (Section section : byOffset) {
            require(section.offset >= previousEnd, "section ranges overlap at type "
                    + Integer.toUnsignedString(section.type));
            previousEnd = section.offset + section.byteLength;
        }
        for (int required : new int[] {
                FrameStreamFormat.SECTION_PASSES,
                FrameStreamFormat.SECTION_BATCHES,
                FrameStreamFormat.SECTION_VERTICES,
                FrameStreamFormat.SECTION_MATERIALS }) {
            require(byType.containsKey(required), "missing required section " + required);
        }
        require(byType.get(FrameStreamFormat.SECTION_PASSES).elementCount == passCount,
                "pass section count does not match header");
        require(byType.get(FrameStreamFormat.SECTION_BATCHES).elementCount == batchCount,
                "batch section count does not match header");
        require((flags & FrameStreamFormat.FLAG_HAS_DEBUG_LABELS) == 0
                        || byType.containsKey(FrameStreamFormat.SECTION_DEBUG_LABELS),
                "debug-label flag is set without a debug-label section");
        require((flags & FrameStreamFormat.FLAG_HAS_DEBUG_LABELS) != 0
                        || !byType.containsKey(FrameStreamFormat.SECTION_DEBUG_LABELS),
                "debug-label section is present without its frame flag");

        if ((flags & FrameStreamFormat.FLAG_HAS_PAYLOAD_CRC32) != 0) {
            int expected = bytes.getInt(FrameStreamFormat.OFFSET_PAYLOAD_CRC32);
            int actual = crc32(bytes, headerBytes, totalBytes);
            require(expected == actual, "payload CRC32 mismatch");
        } else {
            require(bytes.getInt(FrameStreamFormat.OFFSET_PAYLOAD_CRC32) == 0,
                    "payload CRC32 is nonzero without its frame flag");
        }

        ByteBuffer retained = bytes.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN);
        retained.position(0);
        retained.limit(totalBytes);
        return new FrameStreamReader(retained, minor, totalBytes, frameId, sequence,
                flags, width, height, passCount, batchCount, byType);
    }

    public int minorVersion() { return minorVersion; }
    public int totalBytes() { return totalBytes; }
    public long frameId() { return frameId; }
    public long requiredResourceSequence() { return requiredResourceSequence; }
    public int flags() { return flags; }
    public int width() { return width; }
    public int height() { return height; }
    public int passCount() { return passCount; }
    public int batchCount() { return batchCount; }
    public Map<Integer, Section> sections() { return sections; }

    public Section section(int type) {
        return sections.get(type);
    }

    public ByteBuffer sectionData(int type) {
        Section section = sections.get(type);
        if (section == null) return null;
        ByteBuffer view = bytes.duplicate().order(ByteOrder.LITTLE_ENDIAN);
        view.position(section.offset);
        view.limit(section.offset + section.byteLength);
        return view.slice().order(ByteOrder.LITTLE_ENDIAN).asReadOnlyBuffer()
                .order(ByteOrder.LITTLE_ENDIAN);
    }

    public static final class Section {
        private final int type;
        private final int offset;
        private final int byteLength;
        private final int elementCount;

        private Section(int type, int offset, int byteLength, int elementCount) {
            this.type = type;
            this.offset = offset;
            this.byteLength = byteLength;
            this.elementCount = elementCount;
        }

        public int type() { return type; }
        public int offset() { return offset; }
        public int byteLength() { return byteLength; }
        public int elementCount() { return elementCount; }
    }

    private static int crc32(ByteBuffer bytes, int start, int end) {
        CRC32 crc = new CRC32();
        for (int offset = start; offset < end; offset++) crc.update(bytes.get(offset) & 0xff);
        return (int) crc.getValue();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw invalid(message);
    }

    private static FrameStreamFormatException invalid(String message) {
        return new FrameStreamFormatException(message);
    }
}
