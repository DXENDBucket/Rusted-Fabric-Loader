package io.github.endx.vulkanmod.framestream;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.zip.CRC32;

/** Deterministic writer for the validated FrameStream version-1 envelope. */
public final class FrameStreamWriter {
    private final long frameId;
    private final long requiredResourceSequence;
    private final int width;
    private final int height;
    private final int flags;
    private final List<PendingSection> sections = new ArrayList<PendingSection>();

    public FrameStreamWriter(long frameId, long requiredResourceSequence,
                             int width, int height, int flags) {
        if (frameId < 0L) throw new IllegalArgumentException("frameId must not be negative");
        if (requiredResourceSequence < 0L) {
            throw new IllegalArgumentException("resource sequence must not be negative");
        }
        requireDimension("width", width);
        requireDimension("height", height);
        if ((flags & ~FrameStreamFormat.KNOWN_FLAGS) != 0) {
            throw new IllegalArgumentException("unknown frame flags: 0x"
                    + Integer.toHexString(flags));
        }
        this.frameId = frameId;
        this.requiredResourceSequence = requiredResourceSequence;
        this.width = width;
        this.height = height;
        this.flags = flags;
    }

    public FrameStreamWriter section(int type, int elementCount, byte[] payload) {
        if (payload == null) throw new NullPointerException("payload");
        return section(type, elementCount, ByteBuffer.wrap(payload));
    }

    public FrameStreamWriter section(int type, int elementCount, ByteBuffer payload) {
        if (type == 0) throw new IllegalArgumentException("section type zero is invalid");
        if (elementCount < 0 || elementCount > FrameStreamFormat.MAX_SECTION_ELEMENTS) {
            throw new IllegalArgumentException("section element count is out of range");
        }
        if (payload == null) throw new NullPointerException("payload");
        for (PendingSection section : sections) {
            if (section.type == type) {
                throw new IllegalArgumentException("duplicate section type "
                        + Integer.toUnsignedString(type));
            }
        }
        ByteBuffer source = payload.slice();
        byte[] copy = new byte[source.remaining()];
        source.get(copy);
        sections.add(new PendingSection(type, elementCount, copy));
        return this;
    }

    public int encodedSize() {
        Layout layout = layout();
        return layout.totalBytes;
    }

    public ByteBuffer toDirectBuffer() {
        ByteBuffer result = ByteBuffer.allocateDirect(encodedSize()).order(ByteOrder.LITTLE_ENDIAN);
        return writeTo(result);
    }

    /** Writes at the target's current position and advances it by the encoded frame length. */
    public ByteBuffer writeTo(ByteBuffer target) {
        if (target == null) throw new NullPointerException("target");
        Layout layout = layout();
        if (target.remaining() < layout.totalBytes) {
            throw new IllegalArgumentException("target has " + target.remaining()
                    + " bytes remaining, needs " + layout.totalBytes);
        }
        int targetStart = target.position();
        ByteBuffer writable = target.duplicate().order(ByteOrder.LITTLE_ENDIAN);
        writable.position(targetStart);
        writable.limit(targetStart + layout.totalBytes);
        ByteBuffer frame = writable.slice().order(ByteOrder.LITTLE_ENDIAN);

        frame.put(FrameStreamFormat.OFFSET_MAGIC, FrameStreamFormat.MAGIC_R);
        frame.put(FrameStreamFormat.OFFSET_MAGIC + 1, FrameStreamFormat.MAGIC_V);
        frame.put(FrameStreamFormat.OFFSET_MAGIC + 2, FrameStreamFormat.MAGIC_K);
        frame.put(FrameStreamFormat.OFFSET_MAGIC + 3, FrameStreamFormat.MAGIC_F);
        frame.putShort(FrameStreamFormat.OFFSET_MAJOR_VERSION,
                (short) FrameStreamFormat.MAJOR_VERSION);
        frame.putShort(FrameStreamFormat.OFFSET_MINOR_VERSION,
                (short) FrameStreamFormat.MINOR_VERSION);
        frame.putInt(FrameStreamFormat.OFFSET_HEADER_BYTES, layout.headerBytes);
        frame.putInt(FrameStreamFormat.OFFSET_TOTAL_BYTES, layout.totalBytes);
        frame.putLong(FrameStreamFormat.OFFSET_FRAME_ID, frameId);
        frame.putLong(FrameStreamFormat.OFFSET_REQUIRED_RESOURCE_SEQUENCE,
                requiredResourceSequence);
        frame.putInt(FrameStreamFormat.OFFSET_FLAGS, flags);
        frame.putInt(FrameStreamFormat.OFFSET_WIDTH, width);
        frame.putInt(FrameStreamFormat.OFFSET_HEIGHT, height);
        frame.putInt(FrameStreamFormat.OFFSET_SECTION_COUNT, layout.sections.size());
        frame.putInt(FrameStreamFormat.OFFSET_PASS_COUNT,
                layout.section(FrameStreamFormat.SECTION_PASSES).elementCount);
        frame.putInt(FrameStreamFormat.OFFSET_BATCH_COUNT,
                layout.section(FrameStreamFormat.SECTION_BATCHES).elementCount);
        frame.putInt(FrameStreamFormat.OFFSET_PAYLOAD_CRC32, 0);
        frame.putInt(FrameStreamFormat.OFFSET_RESERVED, 0);

        int directoryOffset = FrameStreamFormat.FIXED_HEADER_BYTES;
        for (PlacedSection section : layout.sections) {
            frame.putInt(directoryOffset, section.type);
            frame.putInt(directoryOffset + 4, section.offset);
            frame.putInt(directoryOffset + 8, section.payload.length);
            frame.putInt(directoryOffset + 12, section.elementCount);
            directoryOffset += FrameStreamFormat.SECTION_ENTRY_BYTES;
            for (int padding = section.previousEnd; padding < section.offset; padding++) {
                frame.put(padding, (byte) 0);
            }
            ByteBuffer destination = frame.duplicate();
            destination.position(section.offset);
            destination.put(section.payload);
        }
        if ((flags & FrameStreamFormat.FLAG_HAS_PAYLOAD_CRC32) != 0) {
            frame.putInt(FrameStreamFormat.OFFSET_PAYLOAD_CRC32,
                    crc32(frame, layout.headerBytes, layout.totalBytes));
        }

        target.position(targetStart + layout.totalBytes);
        ByteBuffer result = frame.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN);
        result.position(0);
        result.limit(layout.totalBytes);
        return result;
    }

    private Layout layout() {
        if (sections.size() > FrameStreamFormat.MAX_SECTIONS) {
            throw new IllegalStateException("too many FrameStream sections");
        }
        for (int required : new int[] {
                FrameStreamFormat.SECTION_PASSES,
                FrameStreamFormat.SECTION_BATCHES,
                FrameStreamFormat.SECTION_VERTICES,
                FrameStreamFormat.SECTION_MATERIALS }) {
            if (find(required) == null) {
                throw new IllegalStateException("missing required section " + required);
            }
        }
        PendingSection passes = find(FrameStreamFormat.SECTION_PASSES);
        PendingSection batches = find(FrameStreamFormat.SECTION_BATCHES);
        boolean debugSection = find(FrameStreamFormat.SECTION_DEBUG_LABELS) != null;
        if (debugSection != ((flags & FrameStreamFormat.FLAG_HAS_DEBUG_LABELS) != 0)) {
            throw new IllegalStateException(
                    "debug-label section and HAS_DEBUG_LABELS flag must agree");
        }
        if (passes.elementCount <= 0 || passes.elementCount > FrameStreamFormat.MAX_PASSES) {
            throw new IllegalStateException("FrameStream must contain at least one valid pass");
        }
        if (batches.elementCount > FrameStreamFormat.MAX_BATCHES) {
            throw new IllegalStateException("FrameStream contains too many batches");
        }
        ArrayList<PendingSection> sorted = new ArrayList<PendingSection>(sections);
        sorted.sort(new Comparator<PendingSection>() {
            @Override public int compare(PendingSection first, PendingSection second) {
                return Integer.compareUnsigned(first.type, second.type);
            }
        });
        int headerBytes = Math.addExact(FrameStreamFormat.FIXED_HEADER_BYTES,
                Math.multiplyExact(sorted.size(), FrameStreamFormat.SECTION_ENTRY_BYTES));
        int cursor = FrameStreamFormat.align(headerBytes);
        ArrayList<PlacedSection> placed = new ArrayList<PlacedSection>(sorted.size());
        for (PendingSection section : sorted) {
            int offset = FrameStreamFormat.align(cursor);
            int end = Math.addExact(offset, section.payload.length);
            if (end > FrameStreamFormat.MAX_STREAM_BYTES) {
                throw new FrameStreamFormatException("FrameStream exceeds maximum size");
            }
            placed.add(new PlacedSection(section, offset, cursor));
            cursor = end;
        }
        return new Layout(headerBytes, cursor, placed);
    }

    private PendingSection find(int type) {
        for (PendingSection section : sections) if (section.type == type) return section;
        return null;
    }

    private static void requireDimension(String name, int value) {
        if (value <= 0 || value > FrameStreamFormat.MAX_DIMENSION) {
            throw new IllegalArgumentException(name + " is outside FrameStream limits: " + value);
        }
    }

    private static int crc32(ByteBuffer bytes, int start, int end) {
        CRC32 crc = new CRC32();
        for (int offset = start; offset < end; offset++) crc.update(bytes.get(offset) & 0xff);
        return (int) crc.getValue();
    }

    private static final class PendingSection {
        private final int type;
        private final int elementCount;
        private final byte[] payload;

        private PendingSection(int type, int elementCount, byte[] payload) {
            this.type = type;
            this.elementCount = elementCount;
            this.payload = payload;
        }
    }

    private static final class PlacedSection {
        private final int type;
        private final int elementCount;
        private final byte[] payload;
        private final int offset;
        private final int previousEnd;

        private PlacedSection(PendingSection section, int offset, int previousEnd) {
            type = section.type;
            elementCount = section.elementCount;
            payload = section.payload;
            this.offset = offset;
            this.previousEnd = previousEnd;
        }
    }

    private static final class Layout {
        private final int headerBytes;
        private final int totalBytes;
        private final List<PlacedSection> sections;

        private Layout(int headerBytes, int totalBytes, List<PlacedSection> sections) {
            this.headerBytes = headerBytes;
            this.totalBytes = totalBytes;
            this.sections = sections;
        }

        private PlacedSection section(int type) {
            for (PlacedSection section : sections) if (section.type == type) return section;
            throw new IllegalStateException("missing section " + type);
        }
    }
}
