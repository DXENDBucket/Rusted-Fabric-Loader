package io.github.endx.vulkanmod;

import io.github.endx.vulkanmod.framestream.FrameResourceHandle;
import io.github.endx.vulkanmod.resourcestream.ResourceHandleTable;
import io.github.endx.vulkanmod.resourcestream.ResourceSequenceClock;
import io.github.endx.vulkanmod.resourcestream.ResourceStreamFormat;
import io.github.endx.vulkanmod.resourcestream.ResourceStreamRecords;
import io.github.endx.vulkanmod.resourcestream.ResourceStreamWriter;
import io.github.endx.vulkanmod.resourcestream.ResourceUploadArenaPool;
import io.github.endx.vulkanmod.spi.VulkanTextureData;
import io.github.endx.vulkanmod.spi.VulkanCustomFragmentShader;
import io.github.endx.vulkanmod.spi.VulkanCustomShaderProgram;
import io.github.endx.vulkanmod.spi.VulkanResourceStreamResult;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/** Shared ordered client for the reliable RustedVK resource channel. */
final class ResourceStreamClient implements AutoCloseable {
    interface Submitter {
        VulkanResourceStreamResult submit(ByteBuffer stream);
        default VulkanResourceStreamResult pollCompletion(long completionId) {
            return null;
        }
        default VulkanResourceStreamResult awaitCompletion(long completionId,
                                                            long timeoutNanos) {
            throw new UnsupportedOperationException(
                    "asynchronous ResourceStream completions are not supported");
        }
    }

    private static final int DEFAULT_EXTERNAL_THRESHOLD = 256 * 1024;

    private final Submitter submitter;
    private final ResourceSequenceClock sequences = new ResourceSequenceClock();
    private final ResourceHandleTable<TextureMetadata> textures =
            new ResourceHandleTable<TextureMetadata>(FrameResourceHandle.TYPE_TEXTURE);
    private final ResourceHandleTable<ShaderMetadata> shaders =
            new ResourceHandleTable<ShaderMetadata>(FrameResourceHandle.TYPE_SHADER_PROGRAM);
    private final ResourceUploadArenaPool uploadArenas;
    private final int externalThreshold;
    private long nextCompletionId;
    private final LinkedHashMap<Long, ResourceUploadArenaPool.Lease> pendingArenaLeases =
            new LinkedHashMap<Long, ResourceUploadArenaPool.Lease>();
    private RuntimeException fault;
    private long arenaCompletionWaits;
    private long arenaCompletionWaitNanos;

    ResourceStreamClient(Submitter submitter) {
        this(submitter, null, 0, 0, Integer.MAX_VALUE);
    }

    ResourceStreamClient(Submitter submitter, ResourceUploadArenaPool.Registry registry,
                         int arenaCount, int arenaBytes, int externalThreshold) {
        if (submitter == null) throw new NullPointerException("submitter");
        if (externalThreshold < 0) throw new IllegalArgumentException(
                "negative external upload threshold");
        this.submitter = submitter;
        this.externalThreshold = externalThreshold;
        this.uploadArenas = registry == null ? null
                : new ResourceUploadArenaPool(registry, arenaCount, arenaBytes);
    }

    synchronized long uploadTexture(VulkanTextureData texture) {
        requireHealthy();
        if (texture == null) throw new NullPointerException("texture");
        validateDimensions(texture.width(), texture.height());
        ResourceSequenceClock.Reservation reservation = sequences.reserve(2);
        TextureMetadata metadata = new TextureMetadata(texture.width(), texture.height(), false);
        long handle = textures.reserve(metadata, reservation.first);
        try {
            boolean external = usesExternalTransfer(texture);
            long completionId = external ? allocateCompletionId() : 0L;
            ResourceStreamWriter writer = new ResourceStreamWriter(reservation.first,
                    external ? ResourceStreamFormat.FLAG_REQUIRES_COMPLETION : 0,
                    completionId);
            ResourceStreamRecords.textureCreate(writer, handle, texture.width(), texture.height(),
                    1, ResourceStreamFormat.FORMAT_RGBA8_UNORM,
                    ResourceStreamFormat.TEXTURE_USAGE_SAMPLED
                            | ResourceStreamFormat.TEXTURE_USAGE_TRANSFER_DESTINATION,
                    ResourceStreamFormat.SAMPLER_CLAMP_TO_EDGE);
            submitTextureTransfer(reservation, writer, ResourceStreamFormat.TEXTURE_UPLOAD,
                    handle, 0, 0, texture, external, completionId);
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
        updateTextureRegion(handle, 0, 0, texture);
    }

    synchronized void updateTextureRegion(long handle, int x, int y, VulkanTextureData texture) {
        requireHealthy();
        if (texture == null) throw new NullPointerException("texture");
        validateDimensions(texture.width(), texture.height());
        TextureMetadata metadata = textures.requireVisible(handle, sequences.appliedThrough());
        if (x < 0 || y < 0 || (long) x + texture.width() > metadata.width
                || (long) y + texture.height() > metadata.height) {
            throw new IllegalArgumentException("texture update region exceeds logical texture");
        }
        ResourceSequenceClock.Reservation reservation = sequences.reserve(1);
        boolean external = usesExternalTransfer(texture);
        long completionId = external ? allocateCompletionId() : 0L;
        ResourceStreamWriter writer = new ResourceStreamWriter(reservation.first,
                external ? ResourceStreamFormat.FLAG_REQUIRES_COMPLETION : 0,
                completionId);
        submitTextureTransfer(reservation, writer,
                ResourceStreamFormat.TEXTURE_REGION_UPDATE, handle, x, y, texture,
                external, completionId);
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

    synchronized VulkanTextureData readTexture(long handle) {
        requireHealthy();
        TextureMetadata metadata = textures.requireVisible(handle, sequences.appliedThrough());
        if (!metadata.renderTarget) throw new IllegalArgumentException(
                "only readable render targets support ResourceStream readback");
        long completionId = allocateCompletionId();
        ResourceSequenceClock.Reservation reservation = sequences.reserve(1);
        ResourceStreamWriter writer = new ResourceStreamWriter(reservation.first,
                ResourceStreamFormat.FLAG_REQUIRES_COMPLETION, completionId);
        ResourceStreamRecords.textureReadback(writer, handle, 0, 0,
                metadata.width, metadata.height, ResourceStreamFormat.FORMAT_RGBA8_UNORM);
        VulkanResourceStreamResult result = resolveCompletion(
                submit(reservation, writer), completionId);
        if (result.completionId() != completionId || result.textureReadback() == null) {
            fault = new IllegalStateException("ResourceStream readback completion mismatch");
            throw fault;
        }
        return result.textureReadback();
    }

    private void submitTextureTransfer(ResourceSequenceClock.Reservation reservation,
            ResourceStreamWriter writer, int recordType, long handle, int x, int y,
            VulkanTextureData texture, boolean external, long completionId) {
        if (!external) {
            if (recordType == ResourceStreamFormat.TEXTURE_UPLOAD) {
                ResourceStreamRecords.textureUpload(writer, handle, texture);
            } else {
                ResourceStreamRecords.textureRegionUpdate(writer, handle, x, y, texture);
            }
            submit(reservation, writer);
            return;
        }
        ResourceUploadArenaPool.Lease lease = null;
        try {
            prepareArenaLease(texture.byteSize());
            lease = uploadArenas.acquire(texture.byteSize());
            ByteBuffer pixels = lease.buffer();
            texture.writeTo(pixels);
            ResourceStreamRecords.externalTextureTransfer(writer, recordType, handle,
                    x, y, texture.width(), texture.height(), texture.width() * 4,
                    ResourceStreamFormat.FORMAT_RGBA8_UNORM, texture.byteSize(),
                    lease.arenaId(), 0L);
            VulkanResourceStreamResult result = submit(reservation, writer);
            if (result.completionPending()) {
                if (pendingArenaLeases.put(completionId, lease) != null) {
                    throw new IllegalStateException("duplicate pending arena completion ID");
                }
                lease = null; // Ownership remains with pendingArenaLeases until decode completion.
            } else {
                validateArenaCompletion(completionId, result);
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            fault = new IllegalStateException(
                    "interrupted while acquiring a ResourceStream upload arena", interrupted);
            throw fault;
        } catch (RuntimeException failure) {
            fault = failure;
            throw failure;
        } finally {
            if (lease != null) lease.close();
        }
    }

    private boolean usesExternalTransfer(VulkanTextureData texture) {
        return uploadArenas != null && texture.byteSize() >= externalThreshold;
    }

    private long allocateCompletionId() {
        if (nextCompletionId == Long.MAX_VALUE) throw new IllegalStateException(
                "ResourceStream completion IDs exhausted");
        return ++nextCompletionId;
    }

    private void prepareArenaLease(int requiredBytes) {
        reapReadyArenaLeases();
        if (requiredBytes > uploadArenas.arenaCapacity()) drainArenaLeases();
        while (pendingArenaLeases.size() >= uploadArenas.arenaCount()) {
            awaitOldestArenaLease();
        }
    }

    private void reapReadyArenaLeases() {
        Iterator<Map.Entry<Long, ResourceUploadArenaPool.Lease>> iterator =
                pendingArenaLeases.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, ResourceUploadArenaPool.Lease> pending = iterator.next();
            VulkanResourceStreamResult result;
            try {
                result = submitter.pollCompletion(pending.getKey());
            } catch (RuntimeException failure) {
                fault = failure;
                throw failure;
            }
            if (result == null) continue;
            validateArenaCompletion(pending.getKey(), result);
            pending.getValue().close();
            iterator.remove();
        }
    }

    private void awaitOldestArenaLease() {
        Map.Entry<Long, ResourceUploadArenaPool.Lease> pending =
                pendingArenaLeases.entrySet().iterator().next();
        long started = System.nanoTime();
        try {
            VulkanResourceStreamResult result =
                    submitter.awaitCompletion(pending.getKey(), -1L);
            validateArenaCompletion(pending.getKey(), result);
        } catch (RuntimeException failure) {
            fault = failure;
            throw failure;
        } finally {
            arenaCompletionWaits++;
            arenaCompletionWaitNanos += System.nanoTime() - started;
            pending.getValue().close();
            pendingArenaLeases.remove(pending.getKey());
        }
    }

    synchronized long arenaCompletionWaits() { return arenaCompletionWaits; }
    synchronized long arenaCompletionWaitNanos() { return arenaCompletionWaitNanos; }
    synchronized int pendingArenaLeases() { return pendingArenaLeases.size(); }

    private void drainArenaLeases() {
        while (!pendingArenaLeases.isEmpty()) awaitOldestArenaLease();
    }

    private static void validateArenaCompletion(long completionId,
                                                VulkanResourceStreamResult result) {
        if (result == null || result.completionPending()
                || result.completionId() != completionId
                || result.textureReadback() != null) {
            throw new IllegalStateException("invalid resource arena consumption completion");
        }
    }

    private VulkanResourceStreamResult submit(ResourceSequenceClock.Reservation reservation,
                                              ResourceStreamWriter writer) {
        VulkanResourceStreamResult result;
        try {
            result = submitter.submit(writer.toDirectBuffer());
        } catch (RuntimeException failure) {
            fault = failure;
            throw failure;
        }
        if (result == null) {
            fault = new IllegalStateException("ResourceStream backend returned no result");
            throw fault;
        }
        if (result.appliedSequence() != reservation.last) {
            fault = new IllegalStateException("ResourceStream backend applied through "
                    + result.appliedSequence()
                    + " but submission ended at " + reservation.last);
            throw fault;
        }
        if (result.completionId() != writer.completionId()) {
            fault = new IllegalStateException("ResourceStream backend acknowledged completion "
                    + result.completionId() + " but submission requested "
                    + writer.completionId());
            throw fault;
        }
        sequences.markApplied(reservation);
        return result;
    }

    private VulkanResourceStreamResult resolveCompletion(VulkanResourceStreamResult accepted,
                                                          long completionId) {
        if (!accepted.completionPending()) return accepted;
        VulkanResourceStreamResult completed;
        try {
            completed = submitter.awaitCompletion(completionId, -1L);
        } catch (RuntimeException failure) {
            fault = failure;
            throw failure;
        }
        if (completed == null || completed.completionPending()
                || completed.completionId() != completionId
                || completed.appliedSequence() < accepted.appliedSequence()) {
            fault = new IllegalStateException(
                    "ResourceStream backend returned an invalid completion result");
            throw fault;
        }
        return completed;
    }

    private void requireHealthy() {
        if (fault != null) throw new IllegalStateException(
                "ResourceStream client is faulted after a failed ordered submission", fault);
        reapReadyArenaLeases();
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

    @Override public synchronized void close() {
        RuntimeException failure = null;
        if (uploadArenas != null) {
            while (!pendingArenaLeases.isEmpty()) {
                try { awaitOldestArenaLease(); }
                catch (RuntimeException problem) {
                    if (failure == null) failure = problem;
                    else failure.addSuppressed(problem);
                }
            }
            uploadArenas.close();
        }
        if (failure != null) throw failure;
    }
}
