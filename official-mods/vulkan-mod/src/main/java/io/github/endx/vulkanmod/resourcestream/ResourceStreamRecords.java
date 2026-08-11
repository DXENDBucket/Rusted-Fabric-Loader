package io.github.endx.vulkanmod.resourcestream;

import io.github.endx.vulkanmod.spi.VulkanTextureData;
import io.github.endx.vulkanmod.spi.VulkanCustomFragmentShader;
import io.github.endx.vulkanmod.spi.VulkanCustomShaderProgram;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/** Version-1 typed payload codecs layered over the checked ResourceStream envelope. */
public final class ResourceStreamRecords {
    public static final int TEXTURE_DESCRIPTOR_BYTES = 32;
    public static final int TEXTURE_TRANSFER_HEADER_BYTES = 48;
    public static final int TEXTURE_READBACK_BYTES = 32;
    public static final int LIFECYCLE_BARRIER_BYTES = 16;
    public static final int SHADER_PROGRAM_HEADER_BYTES = 24;
    public static final int MAX_SHADER_NAME_BYTES = 4_096;
    public static final int MAX_SHADER_SOURCE_BYTES = 4 * 1024 * 1024;

    private ResourceStreamRecords() { }

    /** Validates every currently frozen version-1 payload before backend resource access. */
    public static void validateKnownRecords(ResourceStreamReader stream) {
        if (stream == null) throw new NullPointerException("stream");
        for (int index = 0; index < stream.recordCount(); index++) {
            int type = stream.record(index).type();
            switch (type) {
                case ResourceStreamFormat.TEXTURE_CREATE:
                case ResourceStreamFormat.RENDER_TARGET_CREATE:
                    decodeTextureDescriptor(stream, index);
                    break;
                case ResourceStreamFormat.TEXTURE_UPLOAD:
                case ResourceStreamFormat.TEXTURE_REGION_UPDATE:
                    decodeTextureTransfer(stream, index);
                    break;
                case ResourceStreamFormat.TEXTURE_DESTROY:
                case ResourceStreamFormat.SHADER_PROGRAM_DESTROY:
                case ResourceStreamFormat.FLUSH:
                    validateEmptyControl(stream, index);
                    break;
                case ResourceStreamFormat.TEXTURE_READBACK:
                    decodeTextureReadback(stream, index);
                    break;
                case ResourceStreamFormat.LIFECYCLE_BARRIER:
                    decodeLifecycleBarrier(stream, index);
                    break;
                case ResourceStreamFormat.SHADER_PROGRAM_CREATE:
                    decodeShaderProgram(stream, index);
                    break;
                default:
                    // Unknown optional records are deliberately skipped.
                    break;
            }
        }
    }

    public static ResourceStreamWriter textureCreate(ResourceStreamWriter writer, long handle,
            int width, int height, int mipLevels, int format, int usage, int samplerFlags) {
        requireWriter(writer);
        return writer.record(ResourceStreamFormat.TEXTURE_CREATE, 0, handle,
                textureDescriptor(width, height, mipLevels, format, usage, samplerFlags));
    }

    public static ResourceStreamWriter renderTargetCreate(ResourceStreamWriter writer, long handle,
            int width, int height, int format, int samplerFlags) {
        requireWriter(writer);
        int usage = ResourceStreamFormat.TEXTURE_USAGE_SAMPLED
                | ResourceStreamFormat.TEXTURE_USAGE_COLOR_ATTACHMENT
                | ResourceStreamFormat.TEXTURE_USAGE_TRANSFER_SOURCE
                | ResourceStreamFormat.TEXTURE_USAGE_TRANSFER_DESTINATION;
        return writer.record(ResourceStreamFormat.RENDER_TARGET_CREATE, 0, handle,
                textureDescriptor(width, height, 1, format, usage, samplerFlags));
    }

    public static ResourceStreamWriter textureUpload(ResourceStreamWriter writer, long handle,
                                                       VulkanTextureData texture) {
        if (texture == null) throw new NullPointerException("texture");
        return inlineTransfer(writer, ResourceStreamFormat.TEXTURE_UPLOAD, handle, 0, 0,
                texture.width(), texture.height(), texture.width() * 4,
                ResourceStreamFormat.FORMAT_RGBA8_UNORM, texture);
    }

    public static ResourceStreamWriter textureRegionUpdate(ResourceStreamWriter writer,
            long handle, int x, int y, VulkanTextureData texture) {
        if (texture == null) throw new NullPointerException("texture");
        return inlineTransfer(writer, ResourceStreamFormat.TEXTURE_REGION_UPDATE, handle, x, y,
                texture.width(), texture.height(), texture.width() * 4,
                ResourceStreamFormat.FORMAT_RGBA8_UNORM, texture);
    }

    public static ResourceStreamWriter externalTextureTransfer(ResourceStreamWriter writer,
            int recordType, long handle, int x, int y, int width, int height, int rowStride,
            int format, int dataBytes, long arenaId, long arenaOffset) {
        requireWriter(writer);
        requireTransferType(recordType);
        validateTransfer(x, y, width, height, rowStride, format, dataBytes);
        if (arenaId <= 0L) throw invalid("external upload arena ID must be positive");
        if (arenaOffset < 0L) throw invalid("external upload arena offset is negative");
        try {
            Math.addExact(arenaOffset, (long) dataBytes);
        } catch (ArithmeticException overflow) {
            throw invalid("external upload arena range overflows");
        }
        ByteBuffer payload = allocate(TEXTURE_TRANSFER_HEADER_BYTES);
        putTransferHeader(payload, x, y, width, height, rowStride, format, dataBytes,
                arenaId, arenaOffset);
        payload.position(0).limit(TEXTURE_TRANSFER_HEADER_BYTES);
        return writer.record(recordType, ResourceStreamFormat.RECORD_HAS_EXTERNAL_PAYLOAD,
                handle, payload);
    }

    public static ResourceStreamWriter textureDestroy(ResourceStreamWriter writer, long handle) {
        requireWriter(writer);
        return writer.record(ResourceStreamFormat.TEXTURE_DESTROY, 0, handle, new byte[0]);
    }

    public static ResourceStreamWriter shaderProgramCreate(ResourceStreamWriter writer,
            long handle, VulkanCustomShaderProgram program) {
        if (program == null) throw new NullPointerException("program");
        return shaderCreate(writer, handle, program.name(), program.vertexSource(),
                program.fragmentSource());
    }

    public static ResourceStreamWriter fragmentShaderCreate(ResourceStreamWriter writer,
            long handle, VulkanCustomFragmentShader shader) {
        if (shader == null) throw new NullPointerException("shader");
        return shaderCreate(writer, handle, shader.name(), null, shader.source());
    }

    public static ResourceStreamWriter shaderProgramDestroy(ResourceStreamWriter writer,
                                                              long handle) {
        requireWriter(writer);
        return writer.record(ResourceStreamFormat.SHADER_PROGRAM_DESTROY, 0,
                handle, new byte[0]);
    }

    public static ResourceStreamWriter textureReadback(ResourceStreamWriter writer, long handle,
            int x, int y, int width, int height, int format) {
        requireWriter(writer);
        validateRegion(x, y, width, height);
        requireFormat(format);
        int rowStride = checkedRgbaRowBytes(width);
        ByteBuffer payload = allocate(TEXTURE_READBACK_BYTES);
        payload.putInt(x).putInt(y).putInt(width).putInt(height);
        payload.putInt(format).putInt(rowStride).putLong(0L).flip();
        return writer.record(ResourceStreamFormat.TEXTURE_READBACK,
                ResourceStreamFormat.RECORD_EXPECTS_RESULT, handle, payload);
    }

    public static ResourceStreamWriter flush(ResourceStreamWriter writer, boolean awaitCompletion) {
        requireWriter(writer);
        return writer.record(ResourceStreamFormat.FLUSH,
                awaitCompletion ? ResourceStreamFormat.RECORD_EXPECTS_RESULT : 0,
                0L, new byte[0]);
    }

    public static ResourceStreamWriter lifecycleBarrier(ResourceStreamWriter writer, int scope,
            int barrierFlags, long waitThroughSequence, boolean awaitCompletion) {
        requireWriter(writer);
        if (scope != ResourceStreamFormat.BARRIER_RESOURCE_TABLE
                && scope != ResourceStreamFormat.BARRIER_RENDERER_LIFECYCLE) {
            throw invalid("unknown lifecycle barrier scope");
        }
        if (barrierFlags != 0) throw invalid("unknown lifecycle barrier flags");
        if (waitThroughSequence < 0L) throw invalid("negative lifecycle barrier sequence");
        ByteBuffer payload = allocate(LIFECYCLE_BARRIER_BYTES);
        payload.putInt(scope).putInt(barrierFlags).putLong(waitThroughSequence).flip();
        return writer.record(ResourceStreamFormat.LIFECYCLE_BARRIER,
                awaitCompletion ? ResourceStreamFormat.RECORD_EXPECTS_RESULT : 0,
                0L, payload);
    }

    public static TextureDescriptor decodeTextureDescriptor(ResourceStreamReader stream,
                                                              int recordIndex) {
        ResourceStreamReader.Record record = stream.record(recordIndex);
        require(record.type() == ResourceStreamFormat.TEXTURE_CREATE
                        || record.type() == ResourceStreamFormat.RENDER_TARGET_CREATE,
                "record is not a texture descriptor");
        require(record.flags() == 0, "texture descriptor has invalid flags");
        ByteBuffer payload = exactPayload(stream, recordIndex, TEXTURE_DESCRIPTOR_BYTES);
        int width = payload.getInt();
        int height = payload.getInt();
        int mipLevels = payload.getInt();
        int format = payload.getInt();
        int usage = payload.getInt();
        int samplerFlags = payload.getInt();
        require(payload.getLong() == 0L, "texture descriptor reserved field is not zero");
        validateDescriptor(width, height, mipLevels, format, usage, samplerFlags);
        if (record.type() == ResourceStreamFormat.RENDER_TARGET_CREATE) {
            require(mipLevels == 1, "render target must have one mip level");
            require((usage & ResourceStreamFormat.TEXTURE_USAGE_COLOR_ATTACHMENT) != 0,
                    "render target lacks color-attachment usage");
        }
        return new TextureDescriptor(width, height, mipLevels, format, usage, samplerFlags);
    }

    public static TextureTransfer decodeTextureTransfer(ResourceStreamReader stream,
                                                          int recordIndex) {
        ResourceStreamReader.Record record = stream.record(recordIndex);
        requireTransferType(record.type());
        require((record.flags() & ~ResourceStreamFormat.RECORD_HAS_EXTERNAL_PAYLOAD) == 0,
                "texture transfer has invalid flags");
        ByteBuffer payload = stream.payload(recordIndex);
        require(payload.remaining() >= TEXTURE_TRANSFER_HEADER_BYTES,
                "truncated texture transfer header");
        int x = payload.getInt();
        int y = payload.getInt();
        int width = payload.getInt();
        int height = payload.getInt();
        int rowStride = payload.getInt();
        int format = payload.getInt();
        int dataBytes = payload.getInt();
        require(payload.getInt() == 0, "texture transfer reserved field is not zero");
        long arenaId = payload.getLong();
        long arenaOffset = payload.getLong();
        validateTransfer(x, y, width, height, rowStride, format, dataBytes);
        boolean external = (record.flags()
                & ResourceStreamFormat.RECORD_HAS_EXTERNAL_PAYLOAD) != 0;
        if (external) {
            require(arenaId > 0L && arenaOffset >= 0L,
                    "invalid external texture payload reference");
            try {
                Math.addExact(arenaOffset, (long) dataBytes);
            } catch (ArithmeticException overflow) {
                throw invalid("external texture payload range overflows");
            }
            require(record.payloadBytes() == TEXTURE_TRANSFER_HEADER_BYTES,
                    "external texture transfer contains inline bytes");
            return new TextureTransfer(x, y, width, height, rowStride, format, dataBytes,
                    arenaId, arenaOffset, null);
        }
        require(arenaId == 0L && arenaOffset == 0L,
                "inline texture transfer has an external reference");
        int expectedPayloadBytes = ResourceStreamFormat.align(
                Math.addExact(TEXTURE_TRANSFER_HEADER_BYTES, dataBytes));
        require(record.payloadBytes() == expectedPayloadBytes,
                "inline texture payload length mismatch");
        ByteBuffer pixels = stream.payload(recordIndex);
        pixels.position(TEXTURE_TRANSFER_HEADER_BYTES);
        pixels.limit(TEXTURE_TRANSFER_HEADER_BYTES + dataBytes);
        return new TextureTransfer(x, y, width, height, rowStride, format, dataBytes,
                0L, 0L, pixels.slice().asReadOnlyBuffer());
    }

    public static TextureReadback decodeTextureReadback(ResourceStreamReader stream,
                                                          int recordIndex) {
        ResourceStreamReader.Record record = stream.record(recordIndex);
        require(record.type() == ResourceStreamFormat.TEXTURE_READBACK,
                "record is not a texture readback");
        require(record.flags() == ResourceStreamFormat.RECORD_EXPECTS_RESULT,
                "texture readback must request a result");
        ByteBuffer payload = exactPayload(stream, recordIndex, TEXTURE_READBACK_BYTES);
        int x = payload.getInt();
        int y = payload.getInt();
        int width = payload.getInt();
        int height = payload.getInt();
        int format = payload.getInt();
        int rowStride = payload.getInt();
        require(payload.getLong() == 0L, "texture readback reserved field is not zero");
        validateRegion(x, y, width, height);
        requireFormat(format);
        require(rowStride == checkedRgbaRowBytes(width), "invalid texture readback row stride");
        return new TextureReadback(x, y, width, height, format, rowStride);
    }

    public static LifecycleBarrier decodeLifecycleBarrier(ResourceStreamReader stream,
                                                            int recordIndex) {
        ResourceStreamReader.Record record = stream.record(recordIndex);
        require(record.type() == ResourceStreamFormat.LIFECYCLE_BARRIER,
                "record is not a lifecycle barrier");
        require((record.flags() & ~ResourceStreamFormat.RECORD_EXPECTS_RESULT) == 0,
                "lifecycle barrier has invalid flags");
        ByteBuffer payload = exactPayload(stream, recordIndex, LIFECYCLE_BARRIER_BYTES);
        int scope = payload.getInt();
        int flags = payload.getInt();
        long waitSequence = payload.getLong();
        require(scope == ResourceStreamFormat.BARRIER_RESOURCE_TABLE
                        || scope == ResourceStreamFormat.BARRIER_RENDERER_LIFECYCLE,
                "unknown lifecycle barrier scope");
        require(flags == 0, "unknown lifecycle barrier flags");
        require(waitSequence >= 0L && waitSequence <= record.sequence(),
                "invalid lifecycle barrier sequence");
        return new LifecycleBarrier(scope, flags, waitSequence);
    }

    public static ShaderProgram decodeShaderProgram(ResourceStreamReader stream, int recordIndex) {
        ResourceStreamReader.Record record = stream.record(recordIndex);
        require(record.type() == ResourceStreamFormat.SHADER_PROGRAM_CREATE,
                "record is not a shader-program create");
        require(record.flags() == 0, "shader-program create has invalid flags");
        ByteBuffer payload = stream.payload(recordIndex);
        require(payload.remaining() >= SHADER_PROGRAM_HEADER_BYTES,
                "truncated shader-program header");
        int language = payload.getInt();
        int shaderFlags = payload.getInt();
        int nameBytes = payload.getInt();
        int vertexBytes = payload.getInt();
        int fragmentBytes = payload.getInt();
        require(payload.getInt() == 0, "shader-program reserved field is not zero");
        require(language == ResourceStreamFormat.SHADER_LANGUAGE_VULKAN_GLSL,
                "unsupported shader language");
        require((shaderFlags & ~ResourceStreamFormat.SHADER_HAS_VERTEX_SOURCE) == 0,
                "unknown shader-program flags");
        require(nameBytes > 0 && nameBytes <= MAX_SHADER_NAME_BYTES,
                "shader name length is out of range");
        boolean hasVertex = (shaderFlags & ResourceStreamFormat.SHADER_HAS_VERTEX_SOURCE) != 0;
        require(vertexBytes >= 0 && vertexBytes <= MAX_SHADER_SOURCE_BYTES
                        && hasVertex == (vertexBytes > 0),
                "shader vertex source length disagrees with its flag");
        require(fragmentBytes > 0 && fragmentBytes <= MAX_SHADER_SOURCE_BYTES,
                "shader fragment source length is out of range");
        int contentBytes;
        try {
            contentBytes = Math.addExact(SHADER_PROGRAM_HEADER_BYTES,
                    Math.addExact(nameBytes, Math.addExact(vertexBytes, fragmentBytes)));
        } catch (ArithmeticException overflow) {
            throw invalid("shader-program payload length overflows");
        }
        require(record.payloadBytes() == ResourceStreamFormat.align(contentBytes),
                "shader-program payload length mismatch");
        String name = utf8(payload, nameBytes, "shader name");
        String vertex = vertexBytes == 0 ? null : utf8(payload, vertexBytes,
                "vertex shader source");
        String fragment = utf8(payload, fragmentBytes, "fragment shader source");
        require(!name.isEmpty() && (vertex == null || !vertex.trim().isEmpty())
                        && !fragment.trim().isEmpty(),
                "shader-program contains empty text");
        return new ShaderProgram(language, shaderFlags, name, vertex, fragment);
    }

    public static void validateEmptyControl(ResourceStreamReader stream, int recordIndex) {
        ResourceStreamReader.Record record = stream.record(recordIndex);
        require(record.type() == ResourceStreamFormat.TEXTURE_DESTROY
                        || record.type() == ResourceStreamFormat.SHADER_PROGRAM_DESTROY
                        || record.type() == ResourceStreamFormat.FLUSH,
                "record is not an empty resource control");
        int allowedFlags = record.type() == ResourceStreamFormat.FLUSH
                ? ResourceStreamFormat.RECORD_EXPECTS_RESULT : 0;
        require((record.flags() & ~allowedFlags) == 0,
                "empty resource control has invalid flags");
        require(record.payloadBytes() == 0, "empty resource control has a payload");
    }

    private static ResourceStreamWriter inlineTransfer(ResourceStreamWriter writer, int recordType,
            long handle, int x, int y, int width, int height, int rowStride, int format,
            VulkanTextureData texture) {
        requireWriter(writer);
        int dataBytes = texture.byteSize();
        validateTransfer(x, y, width, height, rowStride, format, dataBytes);
        ByteBuffer payload = allocate(Math.addExact(TEXTURE_TRANSFER_HEADER_BYTES, dataBytes));
        putTransferHeader(payload, x, y, width, height, rowStride, format, dataBytes, 0L, 0L);
        texture.writeTo(payload);
        payload.flip();
        return writer.record(recordType, 0, handle, payload);
    }

    private static ResourceStreamWriter shaderCreate(ResourceStreamWriter writer, long handle,
            String name, String vertex, String fragment) {
        requireWriter(writer);
        if (name == null || name.isEmpty()) name = "custom";
        if (fragment == null || fragment.trim().isEmpty()) {
            throw invalid("fragment shader source must not be empty");
        }
        if (vertex != null && vertex.trim().isEmpty()) {
            throw invalid("vertex shader source must not be empty");
        }
        byte[] nameUtf8 = name.getBytes(StandardCharsets.UTF_8);
        byte[] vertexUtf8 = vertex == null ? new byte[0] : vertex.getBytes(StandardCharsets.UTF_8);
        byte[] fragmentUtf8 = fragment.getBytes(StandardCharsets.UTF_8);
        require(nameUtf8.length > 0 && nameUtf8.length <= MAX_SHADER_NAME_BYTES,
                "shader name length is out of range");
        require(vertexUtf8.length <= MAX_SHADER_SOURCE_BYTES
                        && fragmentUtf8.length <= MAX_SHADER_SOURCE_BYTES,
                "shader source length is out of range");
        int shaderFlags = vertex == null ? 0 : ResourceStreamFormat.SHADER_HAS_VERTEX_SOURCE;
        int bytes;
        try {
            bytes = Math.addExact(SHADER_PROGRAM_HEADER_BYTES,
                    Math.addExact(nameUtf8.length,
                            Math.addExact(vertexUtf8.length, fragmentUtf8.length)));
        } catch (ArithmeticException overflow) {
            throw invalid("shader-program payload length overflows");
        }
        ByteBuffer payload = allocate(bytes);
        payload.putInt(ResourceStreamFormat.SHADER_LANGUAGE_VULKAN_GLSL);
        payload.putInt(shaderFlags).putInt(nameUtf8.length).putInt(vertexUtf8.length);
        payload.putInt(fragmentUtf8.length).putInt(0);
        payload.put(nameUtf8).put(vertexUtf8).put(fragmentUtf8).flip();
        return writer.record(ResourceStreamFormat.SHADER_PROGRAM_CREATE, 0, handle, payload);
    }

    private static String utf8(ByteBuffer payload, int length, String field) {
        require(length >= 0 && payload.remaining() >= length, "truncated " + field);
        ByteBuffer bytes = payload.slice();
        bytes.limit(length);
        payload.position(payload.position() + length);
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(bytes).toString();
        } catch (CharacterCodingException invalidUtf8) {
            throw invalid(field + " is not valid UTF-8");
        }
    }

    private static ByteBuffer textureDescriptor(int width, int height, int mipLevels, int format,
                                                 int usage, int samplerFlags) {
        validateDescriptor(width, height, mipLevels, format, usage, samplerFlags);
        ByteBuffer payload = allocate(TEXTURE_DESCRIPTOR_BYTES);
        payload.putInt(width).putInt(height).putInt(mipLevels).putInt(format);
        payload.putInt(usage).putInt(samplerFlags).putLong(0L).flip();
        return payload;
    }

    private static void putTransferHeader(ByteBuffer payload, int x, int y, int width, int height,
            int rowStride, int format, int dataBytes, long arenaId, long arenaOffset) {
        payload.putInt(x).putInt(y).putInt(width).putInt(height);
        payload.putInt(rowStride).putInt(format).putInt(dataBytes).putInt(0);
        payload.putLong(arenaId).putLong(arenaOffset);
    }

    private static void validateDescriptor(int width, int height, int mipLevels, int format,
                                           int usage, int samplerFlags) {
        requireDimensions(width, height);
        require(mipLevels > 0 && mipLevels <= 16, "texture mip level count is out of range");
        requireFormat(format);
        require(usage != 0 && (usage & ~ResourceStreamFormat.KNOWN_TEXTURE_USAGE) == 0,
                "unknown texture usage flags");
        require((samplerFlags & ~ResourceStreamFormat.KNOWN_SAMPLER_FLAGS) == 0,
                "unknown sampler flags");
    }

    private static void validateTransfer(int x, int y, int width, int height, int rowStride,
                                         int format, int dataBytes) {
        validateRegion(x, y, width, height);
        requireFormat(format);
        int minimumRow = checkedRgbaRowBytes(width);
        require(rowStride >= minimumRow, "texture row stride is too small");
        int expected;
        try {
            expected = Math.multiplyExact(rowStride, height);
        } catch (ArithmeticException overflow) {
            throw invalid("texture transfer byte count overflows");
        }
        require(dataBytes == expected, "texture transfer byte count does not match its rows");
    }

    private static void validateRegion(int x, int y, int width, int height) {
        require(x >= 0 && y >= 0, "texture region origin is negative");
        requireDimensions(width, height);
        try {
            require(Math.addExact(x, width) <= ResourceStreamFormat.MAX_TEXTURE_DIMENSION
                            && Math.addExact(y, height)
                            <= ResourceStreamFormat.MAX_TEXTURE_DIMENSION,
                    "texture region exceeds the dimension limit");
        } catch (ArithmeticException overflow) {
            throw invalid("texture region overflows");
        }
    }

    private static void requireDimensions(int width, int height) {
        require(width > 0 && width <= ResourceStreamFormat.MAX_TEXTURE_DIMENSION
                        && height > 0 && height <= ResourceStreamFormat.MAX_TEXTURE_DIMENSION,
                "texture dimensions are out of range");
    }

    private static int checkedRgbaRowBytes(int width) {
        try {
            return Math.multiplyExact(width, 4);
        } catch (ArithmeticException overflow) {
            throw invalid("texture row byte count overflows");
        }
    }

    private static void requireFormat(int format) {
        require(format == ResourceStreamFormat.FORMAT_RGBA8_UNORM,
                "unsupported texture format");
    }

    private static void requireTransferType(int type) {
        require(type == ResourceStreamFormat.TEXTURE_UPLOAD
                        || type == ResourceStreamFormat.TEXTURE_REGION_UPDATE,
                "record is not a texture transfer");
    }

    private static ByteBuffer exactPayload(ResourceStreamReader stream, int index, int bytes) {
        ResourceStreamReader.Record record = stream.record(index);
        require(record.payloadBytes() == ResourceStreamFormat.align(bytes),
                "resource payload has the wrong length");
        ByteBuffer payload = stream.payload(index);
        payload.limit(bytes);
        return payload;
    }

    private static ByteBuffer allocate(int bytes) {
        return ByteBuffer.allocate(bytes).order(ByteOrder.LITTLE_ENDIAN);
    }

    private static void requireWriter(ResourceStreamWriter writer) {
        if (writer == null) throw new NullPointerException("writer");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw invalid(message);
    }

    private static ResourceStreamFormatException invalid(String message) {
        return new ResourceStreamFormatException(message);
    }

    public static final class TextureDescriptor {
        public final int width, height, mipLevels, format, usage, samplerFlags;
        private TextureDescriptor(int width, int height, int mipLevels, int format, int usage,
                                  int samplerFlags) {
            this.width = width; this.height = height; this.mipLevels = mipLevels;
            this.format = format; this.usage = usage; this.samplerFlags = samplerFlags;
        }
    }

    public static final class TextureTransfer {
        public final int x, y, width, height, rowStride, format, dataBytes;
        public final long arenaId, arenaOffset;
        public final ByteBuffer inlinePixels;
        private TextureTransfer(int x, int y, int width, int height, int rowStride, int format,
                int dataBytes, long arenaId, long arenaOffset, ByteBuffer inlinePixels) {
            this.x = x; this.y = y; this.width = width; this.height = height;
            this.rowStride = rowStride; this.format = format; this.dataBytes = dataBytes;
            this.arenaId = arenaId; this.arenaOffset = arenaOffset;
            this.inlinePixels = inlinePixels;
        }
        public boolean external() { return inlinePixels == null; }
    }

    public static final class TextureReadback {
        public final int x, y, width, height, format, rowStride;
        private TextureReadback(int x, int y, int width, int height, int format, int rowStride) {
            this.x = x; this.y = y; this.width = width; this.height = height;
            this.format = format; this.rowStride = rowStride;
        }
    }

    public static final class LifecycleBarrier {
        public final int scope, flags;
        public final long waitThroughSequence;
        private LifecycleBarrier(int scope, int flags, long waitThroughSequence) {
            this.scope = scope; this.flags = flags; this.waitThroughSequence = waitThroughSequence;
        }
    }

    public static final class ShaderProgram {
        public final int language, flags;
        public final String name, vertexSource, fragmentSource;
        private ShaderProgram(int language, int flags, String name, String vertexSource,
                              String fragmentSource) {
            this.language = language; this.flags = flags; this.name = name;
            this.vertexSource = vertexSource; this.fragmentSource = fragmentSource;
        }
        public boolean hasVertexSource() { return vertexSource != null; }
    }
}
