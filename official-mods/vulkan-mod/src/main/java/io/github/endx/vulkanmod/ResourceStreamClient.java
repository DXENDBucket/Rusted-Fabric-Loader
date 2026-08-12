package io.github.endx.vulkanmod;

import io.github.endx.vulkanmod.framestream.FrameResourceHandle;
import io.github.endx.vulkanmod.resourcestream.ResourceHandleTable;
import io.github.endx.vulkanmod.resourcestream.ResourceSequenceClock;
import io.github.endx.vulkanmod.resourcestream.ResourceStreamFormat;
import io.github.endx.vulkanmod.resourcestream.ResourceStreamRecords;
import io.github.endx.vulkanmod.resourcestream.ResourceStreamWriter;
import io.github.endx.vulkanmod.spi.VulkanTextureData;
import io.github.endx.vulkanmod.spi.VulkanCustomFragmentShader;
import io.github.endx.vulkanmod.spi.VulkanCustomShaderProgram;

import java.nio.ByteBuffer;

/** Shared synchronous reference client for the reliable RustedVK resource channel. */
final class ResourceStreamClient {
    interface Submitter { long submit(ByteBuffer stream); }

    private final Submitter submitter;
    private final ResourceSequenceClock sequences = new ResourceSequenceClock();
    private final ResourceHandleTable<TextureMetadata> textures =
            new ResourceHandleTable<TextureMetadata>(FrameResourceHandle.TYPE_TEXTURE);
    private final ResourceHandleTable<ShaderMetadata> shaders =
            new ResourceHandleTable<ShaderMetadata>(FrameResourceHandle.TYPE_SHADER_PROGRAM);
    private RuntimeException fault;

    ResourceStreamClient(Submitter submitter) {
        if (submitter == null) throw new NullPointerException("submitter");
        this.submitter = submitter;
    }

    synchronized long uploadTexture(VulkanTextureData texture) {
        requireHealthy();
        if (texture == null) throw new NullPointerException("texture");
        validateDimensions(texture.width(), texture.height());
        ResourceSequenceClock.Reservation reservation = sequences.reserve(2);
        TextureMetadata metadata = new TextureMetadata(texture.width(), texture.height(), false);
        long handle = textures.reserve(metadata, reservation.first);
        try {
            ResourceStreamWriter writer = new ResourceStreamWriter(reservation.first, 0, 0L);
            ResourceStreamRecords.textureCreate(writer, handle, texture.width(), texture.height(),
                    1, ResourceStreamFormat.FORMAT_RGBA8_UNORM,
                    ResourceStreamFormat.TEXTURE_USAGE_SAMPLED
                            | ResourceStreamFormat.TEXTURE_USAGE_TRANSFER_DESTINATION,
                    ResourceStreamFormat.SAMPLER_CLAMP_TO_EDGE);
            ResourceStreamRecords.textureUpload(writer, handle, texture);
            submit(reservation, writer);
            return handle;
        } catch (RuntimeException failure) {
            textures.cancelReservation(handle);
            throw failure;
        }
    }

    synchronized long createRenderTarget(int width, int height) {
        requireHealthy();
        validateDimensions(width, height);
        ResourceSequenceClock.Reservation reservation = sequences.reserve(1);
        TextureMetadata metadata = new TextureMetadata(width, height, true);
        long handle = textures.reserve(metadata, reservation.first);
        try {
            ResourceStreamWriter writer = new ResourceStreamWriter(reservation.first, 0, 0L);
            ResourceStreamRecords.renderTargetCreate(writer, handle, width, height,
                    ResourceStreamFormat.FORMAT_RGBA8_UNORM,
                    ResourceStreamFormat.SAMPLER_CLAMP_TO_EDGE);
            submit(reservation, writer);
            return handle;
        } catch (RuntimeException failure) {
            textures.cancelReservation(handle);
            throw failure;
        }
    }

    synchronized void updateTexture(long handle, VulkanTextureData texture) {
        requireHealthy();
        if (texture == null) throw new NullPointerException("texture");
        validateDimensions(texture.width(), texture.height());
        TextureMetadata metadata = textures.requireVisible(handle, sequences.appliedThrough());
        if (metadata.width != texture.width() || metadata.height != texture.height()) {
            throw new IllegalArgumentException("texture update dimensions changed");
        }
        ResourceSequenceClock.Reservation reservation = sequences.reserve(1);
        ResourceStreamWriter writer = new ResourceStreamWriter(reservation.first, 0, 0L);
        ResourceStreamRecords.textureRegionUpdate(writer, handle, 0, 0, texture);
        submit(reservation, writer);
    }

    synchronized void destroyTexture(long handle) {
        requireHealthy();
        textures.requireVisible(handle, sequences.appliedThrough());
        ResourceSequenceClock.Reservation reservation = sequences.reserve(1);
        ResourceStreamWriter writer = new ResourceStreamWriter(reservation.first, 0, 0L);
        ResourceStreamRecords.textureDestroy(writer, handle);
        submit(reservation, writer);
        textures.retire(handle, reservation.first);
        // submitResourceStream is synchronous in the reference backend. Native deferred GPU
        // destruction is independent, so the Java logical slot can safely advance generation.
        textures.releaseRetired(handle);
    }

    synchronized long compileFragmentShader(VulkanCustomFragmentShader shader) {
        requireHealthy();
        if (shader == null) throw new NullPointerException("shader");
        ResourceSequenceClock.Reservation reservation = sequences.reserve(1);
        long handle = shaders.reserve(new ShaderMetadata(false), reservation.first);
        try {
            ResourceStreamWriter writer = new ResourceStreamWriter(reservation.first, 0, 0L);
            ResourceStreamRecords.fragmentShaderCreate(writer, handle, shader);
            submit(reservation, writer);
            return handle;
        } catch (RuntimeException failure) {
            shaders.cancelReservation(handle);
            throw failure;
        }
    }

    synchronized long compileShaderProgram(VulkanCustomShaderProgram program) {
        requireHealthy();
        if (program == null) throw new NullPointerException("program");
        ResourceSequenceClock.Reservation reservation = sequences.reserve(1);
        long handle = shaders.reserve(new ShaderMetadata(true), reservation.first);
        try {
            ResourceStreamWriter writer = new ResourceStreamWriter(reservation.first, 0, 0L);
            ResourceStreamRecords.shaderProgramCreate(writer, handle, program);
            submit(reservation, writer);
            return handle;
        } catch (RuntimeException failure) {
            shaders.cancelReservation(handle);
            throw failure;
        }
    }

    synchronized void destroyShader(long handle) {
        requireHealthy();
        shaders.requireVisible(handle, sequences.appliedThrough());
        ResourceSequenceClock.Reservation reservation = sequences.reserve(1);
        ResourceStreamWriter writer = new ResourceStreamWriter(reservation.first, 0, 0L);
        ResourceStreamRecords.shaderProgramDestroy(writer, handle);
        submit(reservation, writer);
        shaders.retire(handle, reservation.first);
        shaders.releaseRetired(handle);
    }

    synchronized boolean shaderUsesExpandedVertexInput(long handle) {
        requireHealthy();
        return shaders.requireVisible(handle, sequences.appliedThrough()).expandedVertexInput;
    }

    synchronized long requiredForNextFrame() {
        requireHealthy();
        return sequences.requiredForNextFrame();
    }

    synchronized boolean isRenderTarget(long handle) {
        requireHealthy();
        return textures.requireVisible(handle, sequences.appliedThrough()).renderTarget;
    }

    private void submit(ResourceSequenceClock.Reservation reservation,
                        ResourceStreamWriter writer) {
        long applied;
        try {
            applied = submitter.submit(writer.toDirectBuffer());
        } catch (RuntimeException failure) {
            fault = failure;
            throw failure;
        }
        if (applied != reservation.last) {
            fault = new IllegalStateException("ResourceStream backend applied through " + applied
                    + " but submission ended at " + reservation.last);
            throw fault;
        }
        sequences.markApplied(reservation);
    }

    private void requireHealthy() {
        if (fault != null) throw new IllegalStateException(
                "ResourceStream client is faulted after a failed ordered submission", fault);
    }

    private static void validateDimensions(int width, int height) {
        if (width <= 0 || height <= 0
                || width > ResourceStreamFormat.MAX_TEXTURE_DIMENSION
                || height > ResourceStreamFormat.MAX_TEXTURE_DIMENSION) {
            throw new IllegalArgumentException("texture dimensions are outside ResourceStream limits");
        }
    }

    private static final class TextureMetadata {
        private final int width, height;
        private final boolean renderTarget;
        private TextureMetadata(int width, int height, boolean renderTarget) {
            if (width <= 0 || height <= 0) {
                throw new IllegalArgumentException("texture dimensions must be positive");
            }
            this.width = width; this.height = height; this.renderTarget = renderTarget;
        }
    }

    private static final class ShaderMetadata {
        private final boolean expandedVertexInput;
        private ShaderMetadata(boolean expandedVertexInput) {
            this.expandedVertexInput = expandedVertexInput;
        }
    }
}
