package io.github.endx.vulkanmod.resourcestream;

/** Versioned constants for RustedVK's reliable ordered resource/control stream. */
public final class ResourceStreamFormat {
    public static final byte MAGIC_R = 'R';
    public static final byte MAGIC_V = 'V';
    public static final byte MAGIC_K = 'K';
    public static final byte MAGIC_RESOURCE = 'R';

    public static final int MAJOR_VERSION = 1;
    public static final int MINOR_VERSION = 0;
    public static final int HEADER_BYTES = 48;
    public static final int RECORD_HEADER_BYTES = 32;
    public static final int ALIGNMENT = 8;
    public static final int MAX_STREAM_BYTES = 256 * 1024 * 1024;
    public static final int MAX_RECORDS = 1_048_576;
    public static final int MAX_TEXTURE_DIMENSION = 32_768;

    public static final int FLAG_HAS_PAYLOAD_CRC32 = 1;
    public static final int FLAG_REQUIRES_COMPLETION = 1 << 1;
    public static final int KNOWN_FLAGS = FLAG_HAS_PAYLOAD_CRC32 | FLAG_REQUIRES_COMPLETION;

    public static final int RECORD_HAS_EXTERNAL_PAYLOAD = 1;
    public static final int RECORD_EXPECTS_RESULT = 1 << 1;
    public static final int KNOWN_RECORD_FLAGS = RECORD_HAS_EXTERNAL_PAYLOAD
            | RECORD_EXPECTS_RESULT;

    public static final int TEXTURE_CREATE = 1;
    public static final int TEXTURE_UPLOAD = 2;
    public static final int TEXTURE_REGION_UPDATE = 3;
    public static final int TEXTURE_DESTROY = 4;
    public static final int RENDER_TARGET_CREATE = 5;
    public static final int SHADER_PROGRAM_CREATE = 6;
    public static final int SHADER_PROGRAM_DESTROY = 7;
    public static final int TEXTURE_READBACK = 8;
    public static final int FLUSH = 9;
    public static final int LIFECYCLE_BARRIER = 10;
    public static final int REQUIRED_EXTENSION_BIT = 0x8000;

    public static final int FORMAT_RGBA8_UNORM = 1;

    public static final int TEXTURE_USAGE_SAMPLED = 1;
    public static final int TEXTURE_USAGE_COLOR_ATTACHMENT = 1 << 1;
    public static final int TEXTURE_USAGE_TRANSFER_SOURCE = 1 << 2;
    public static final int TEXTURE_USAGE_TRANSFER_DESTINATION = 1 << 3;
    public static final int KNOWN_TEXTURE_USAGE = TEXTURE_USAGE_SAMPLED
            | TEXTURE_USAGE_COLOR_ATTACHMENT | TEXTURE_USAGE_TRANSFER_SOURCE
            | TEXTURE_USAGE_TRANSFER_DESTINATION;

    public static final int SAMPLER_NEAREST = 1;
    public static final int SAMPLER_CLAMP_TO_EDGE = 1 << 1;
    public static final int KNOWN_SAMPLER_FLAGS = SAMPLER_NEAREST | SAMPLER_CLAMP_TO_EDGE;

    public static final int BARRIER_RESOURCE_TABLE = 1;
    public static final int BARRIER_RENDERER_LIFECYCLE = 2;

    public static final int SHADER_LANGUAGE_VULKAN_GLSL = 1;
    public static final int SHADER_HAS_VERTEX_SOURCE = 1;

    static final int OFFSET_MAGIC = 0;
    static final int OFFSET_MAJOR = 4;
    static final int OFFSET_MINOR = 6;
    static final int OFFSET_HEADER_BYTES = 8;
    static final int OFFSET_TOTAL_BYTES = 12;
    static final int OFFSET_FIRST_SEQUENCE = 16;
    static final int OFFSET_RECORD_COUNT = 24;
    static final int OFFSET_FLAGS = 28;
    static final int OFFSET_CRC32 = 32;
    static final int OFFSET_RESERVED = 36;
    static final int OFFSET_COMPLETION_ID = 40;

    private ResourceStreamFormat() { }

    public static boolean knownRecordType(int type) {
        return type >= TEXTURE_CREATE && type <= LIFECYCLE_BARRIER;
    }

    static int align(int value) {
        if (value < 0 || value > Integer.MAX_VALUE - (ALIGNMENT - 1)) {
            throw new ResourceStreamFormatException("aligned resource size overflows");
        }
        return (value + ALIGNMENT - 1) & -ALIGNMENT;
    }
}
