package io.github.endx.vulkanmod.framestream;

/** Versioned constants for RustedVK's platform-neutral whole-frame binary envelope. */
public final class FrameStreamFormat {
    public static final byte MAGIC_R = 'R';
    public static final byte MAGIC_V = 'V';
    public static final byte MAGIC_K = 'K';
    public static final byte MAGIC_F = 'F';

    public static final int MAJOR_VERSION = 1;
    public static final int MINOR_VERSION = 0;
    public static final int FIXED_HEADER_BYTES = 64;
    public static final int SECTION_ENTRY_BYTES = 16;
    public static final int ALIGNMENT = 8;

    public static final int SECTION_PASSES = 1;
    public static final int SECTION_BATCHES = 2;
    public static final int SECTION_VERTICES = 3;
    public static final int SECTION_INDICES = 4;
    public static final int SECTION_MATERIALS = 5;
    public static final int SECTION_DEBUG_LABELS = 6;
    public static final int REQUIRED_EXTENSION_BIT = 0x80000000;

    public static final int FLAG_HAS_PAYLOAD_CRC32 = 1;
    public static final int FLAG_REQUIRES_COMPLETION = 1 << 1;
    public static final int FLAG_REPLACEABLE_PRESENT = 1 << 2;
    public static final int FLAG_HAS_DEBUG_LABELS = 1 << 3;
    public static final int KNOWN_FLAGS = FLAG_HAS_PAYLOAD_CRC32
            | FLAG_REQUIRES_COMPLETION
            | FLAG_REPLACEABLE_PRESENT
            | FLAG_HAS_DEBUG_LABELS;

    public static final int MAX_STREAM_BYTES = 256 * 1024 * 1024;
    public static final int MAX_SECTIONS = 64;
    public static final int MAX_DIMENSION = 32768;
    public static final int MAX_PASSES = 1_048_576;
    public static final int MAX_BATCHES = 4_194_304;
    public static final int MAX_SECTION_ELEMENTS = 16_777_216;

    static final int OFFSET_MAGIC = 0;
    static final int OFFSET_MAJOR_VERSION = 4;
    static final int OFFSET_MINOR_VERSION = 6;
    static final int OFFSET_HEADER_BYTES = 8;
    static final int OFFSET_TOTAL_BYTES = 12;
    static final int OFFSET_FRAME_ID = 16;
    static final int OFFSET_REQUIRED_RESOURCE_SEQUENCE = 24;
    static final int OFFSET_FLAGS = 32;
    static final int OFFSET_WIDTH = 36;
    static final int OFFSET_HEIGHT = 40;
    static final int OFFSET_SECTION_COUNT = 44;
    static final int OFFSET_PASS_COUNT = 48;
    static final int OFFSET_BATCH_COUNT = 52;
    static final int OFFSET_PAYLOAD_CRC32 = 56;
    static final int OFFSET_RESERVED = 60;

    private FrameStreamFormat() { }

    public static boolean knownSection(int type) {
        return type == SECTION_PASSES || type == SECTION_BATCHES
                || type == SECTION_VERTICES || type == SECTION_INDICES
                || type == SECTION_MATERIALS || type == SECTION_DEBUG_LABELS;
    }

    public static boolean requiredCoreSection(int type) {
        return type == SECTION_PASSES || type == SECTION_BATCHES
                || type == SECTION_VERTICES || type == SECTION_MATERIALS;
    }

    static int align(int value) {
        if (value < 0 || value > Integer.MAX_VALUE - (ALIGNMENT - 1)) {
            throw new FrameStreamFormatException("aligned size overflows: " + value);
        }
        return (value + ALIGNMENT - 1) & -ALIGNMENT;
    }
}
