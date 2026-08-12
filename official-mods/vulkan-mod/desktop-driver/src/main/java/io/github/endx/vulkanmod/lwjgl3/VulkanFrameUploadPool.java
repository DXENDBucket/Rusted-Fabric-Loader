package io.github.endx.vulkanmod.lwjgl3;

import io.github.endx.vulkanmod.spi.VulkanBlendMode;
import io.github.endx.vulkanmod.spi.VulkanClipRect;
import io.github.endx.vulkanmod.spi.VulkanShaderState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.lwjgl.vulkan.VK10.VK_INDEX_TYPE_UINT16;

/** Owns reusable driver-side frame uploads and draw-batch metadata. */
final class VulkanFrameUploadPool {
    private final ArrayDeque<FrameUpload> uploads = new ArrayDeque<FrameUpload>();
    private final ArrayDeque<ColoredDrawBatch> colored =
            new ArrayDeque<ColoredDrawBatch>();
    private final ArrayDeque<TextureDrawBatch> textured =
            new ArrayDeque<TextureDrawBatch>();
    private long uploadAllocations;
    private long batchAllocations;
    private long acquisitions;
    private long releases;

    FrameUpload acquire(long texturedByteOffset, long customTexturedByteOffset) {
        FrameUpload upload = uploads.pollFirst();
        if (upload == null) {
            upload = new FrameUpload();
            uploadAllocations++;
        }
        upload.texturedByteOffset = texturedByteOffset;
        upload.customTexturedByteOffset = customTexturedByteOffset;
        acquisitions++;
        return upload;
    }

    ColoredDrawBatch acquireColored(VulkanClipRect clip, VulkanBlendMode blendMode,
                                     int firstVertex) {
        ColoredDrawBatch batch = colored.pollFirst();
        if (batch == null) {
            batch = new ColoredDrawBatch();
            batchAllocations++;
        }
        batch.reset(clip, blendMode, firstVertex);
        return batch;
    }

    TextureDrawBatch acquireTexture(long textureHandle, long descriptorSet,
                                    VulkanClipRect clip, VulkanBlendMode blendMode,
                                    VulkanShaderState shaderState, int firstVertex,
                                    boolean expandedVertexInput) {
        TextureDrawBatch batch = textured.pollFirst();
        if (batch == null) {
            batch = new TextureDrawBatch();
            batchAllocations++;
        }
        batch.reset(textureHandle, descriptorSet, clip, blendMode,
                shaderState, firstVertex, expandedVertexInput);
        return batch;
    }

    void release(FrameUpload upload) {
        if (upload == null) return;
        for (DrawBatch batch : upload.batches) {
            batch.clip = null;
            if (batch instanceof TextureDrawBatch) {
                TextureDrawBatch texture = (TextureDrawBatch) batch;
                texture.shaderState = null;
                textured.addFirst(texture);
            } else {
                colored.addFirst((ColoredDrawBatch) batch);
            }
        }
        upload.batches.clear();
        uploads.addFirst(upload);
        releases++;
    }

    long uploadAllocationCount() { return uploadAllocations; }
    long batchAllocationCount() { return batchAllocations; }

    void appendStatistics(Map<String, Long> statistics) {
        statistics.put("frame.uploadAllocations", uploadAllocations);
        statistics.put("frame.drawBatchAllocations", batchAllocations);
        statistics.put("frame.uploadAcquisitions", acquisitions);
        statistics.put("frame.uploadReleases", releases);
        statistics.put("frame.uploadPoolSize", (long) uploads.size());
        statistics.put("frame.coloredBatchPoolSize", (long) colored.size());
        statistics.put("frame.textureBatchPoolSize", (long) textured.size());
    }

    void assertBalanced() {
        if (acquisitions != releases) {
            throw new IllegalStateException("frame upload metadata remained checked out");
        }
    }
}

abstract class DrawBatch {
    VulkanClipRect clip;
    VulkanBlendMode blendMode;
    long vertexByteOffset;
    int firstVertex;
    int vertexCount;
    long indexByteOffset;
    int indexCount;
    int indexType;

    void reset(VulkanClipRect clip, VulkanBlendMode blendMode, int firstVertex) {
        this.clip = clip;
        this.blendMode = blendMode;
        this.vertexByteOffset = 0L;
        this.firstVertex = firstVertex;
        this.vertexCount = 0;
        this.indexByteOffset = 0L;
        this.indexCount = 0;
        this.indexType = VK_INDEX_TYPE_UINT16;
    }
}

final class TextureDrawBatch extends DrawBatch {
    long textureHandle;
    long descriptorSet;
    VulkanShaderState shaderState;
    boolean expandedVertexInput;

    void reset(long textureHandle, long descriptorSet,
               VulkanClipRect clip, VulkanBlendMode blendMode,
               VulkanShaderState shaderState, int firstVertex,
               boolean expandedVertexInput) {
        super.reset(clip, blendMode, firstVertex);
        this.textureHandle = textureHandle;
        this.descriptorSet = descriptorSet;
        this.shaderState = shaderState;
        this.expandedVertexInput = expandedVertexInput;
    }
}

final class ColoredDrawBatch extends DrawBatch { }

final class FrameUpload {
    long texturedByteOffset;
    long customTexturedByteOffset;
    final List<DrawBatch> batches = new ArrayList<DrawBatch>();

    int totalVertexCount() {
        int total = 0;
        for (DrawBatch batch : batches) total = Math.addExact(total, batch.vertexCount);
        return total;
    }
}
