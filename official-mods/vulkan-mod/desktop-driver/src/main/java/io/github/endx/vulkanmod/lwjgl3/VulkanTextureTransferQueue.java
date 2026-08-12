package io.github.endx.vulkanmod.lwjgl3;

import io.github.endx.vulkanmod.lwjgl3.VulkanMemoryAllocator.BufferAllocation;
import io.github.endx.vulkanmod.spi.VulkanTextureData;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkBufferImageCopy;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkImageMemoryBarrier;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Consumer;

import static org.lwjgl.vulkan.VK10.*;

/** Owns deferred texture mutations, persistent staging growth, and fence-safe retirement queues. */
final class VulkanTextureTransferQueue {
    private final VulkanMemoryAllocator memory;
    private final VulkanFrameExecutionResources execution;
    private final Map<Long, TextureResource> textures;
    private final ArrayList<PendingUpload> pending = new ArrayList<PendingUpload>();
    private final ArrayDeque<TextureResource> retired = new ArrayDeque<TextureResource>();
    private long uploadBytes;
    private long uploadBatches;
    private long uploadSlotGrowths;
    private long mutationFenceWaits;
    private long retiredResources;
    private long releasedResources;

    VulkanTextureTransferQueue(VulkanMemoryAllocator memory,
                               VulkanFrameExecutionResources execution,
                               Map<Long, TextureResource> textures) {
        if (memory == null) throw new NullPointerException("memory");
        if (execution == null) throw new NullPointerException("execution");
        if (textures == null) throw new NullPointerException("textures");
        this.memory = memory;
        this.execution = execution;
        this.textures = textures;
    }

    void queue(long textureHandle, int x, int y, VulkanTextureData texture) {
        TextureResource target = textures.get(textureHandle);
        if (target == null) {
            throw new IllegalArgumentException("unknown texture handle " + textureHandle);
        }
        boolean initialized = target.initialized;
        for (int index = pending.size() - 1; index >= 0; index--) {
            if (pending.get(index).textureHandle == textureHandle) {
                initialized = true;
                break;
            }
        }
        pending.add(new PendingUpload(textureHandle, x, y, texture, initialized));
    }

    void removePending(long textureHandle) {
        pending.removeIf(upload -> upload.textureHandle == textureHandle);
    }

    boolean mutationRequiresGlobalWait() {
        for (PendingUpload upload : pending) {
            if (upload.initialized) return true;
        }
        return false;
    }

    void noteMutationFenceWait() { mutationFenceWaits++; }

    boolean hasRetired() { return !retired.isEmpty(); }

    void retire(TextureResource texture) {
        if (texture == null) throw new NullPointerException("texture");
        retired.addLast(texture);
        retiredResources++;
    }

    void releaseRetired(Consumer<TextureResource> destroyer) {
        while (!retired.isEmpty()) {
            destroyer.accept(retired.removeFirst());
            releasedResources++;
        }
    }

    void record(VkCommandBuffer command, MemoryStack stack,
                BufferAllocation[] uploadAllocations, int[] uploadCapacities,
                int uploadSlot, int imageFormat) {
        if (pending.isEmpty()) return;
        int byteCount = 0;
        for (PendingUpload upload : pending) {
            byteCount = Math.addExact(byteCount, upload.texture.byteSize());
        }
        BufferAllocation staging = ensureUploadSlot(stack, uploadAllocations,
                uploadCapacities, uploadSlot, byteCount);
        ByteBuffer destination = staging.mapped.duplicate().order(ByteOrder.nativeOrder());
        destination.clear().limit(byteCount);
        boolean blueFirst = isBlueFirstFormat(imageFormat);
        for (PendingUpload upload : pending) {
            TextureResource target = textures.get(upload.textureHandle);
            if (target != null && target.renderTarget && blueFirst) {
                byte[] rgba = upload.texture.copyRgba();
                for (int offset = 0; offset < rgba.length; offset += 4) {
                    destination.put(rgba[offset + 2]);
                    destination.put(rgba[offset + 1]);
                    destination.put(rgba[offset]);
                    destination.put(rgba[offset + 3]);
                }
            } else {
                upload.texture.writeTo(destination);
            }
        }

        int offset = 0;
        for (PendingUpload upload : pending) {
            TextureResource target = textures.get(upload.textureHandle);
            if (target == null) {
                offset += upload.texture.byteSize();
                continue;
            }
            VkImageMemoryBarrier.Buffer toTransfer = VkImageMemoryBarrier.calloc(1, stack);
            toTransfer.get(0).sType$Default()
                    .oldLayout(upload.initialized
                            ? VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL
                            : VK_IMAGE_LAYOUT_UNDEFINED)
                    .newLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
                    .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .image(target.image).srcAccessMask(upload.initialized
                            ? VK_ACCESS_SHADER_READ_BIT : 0)
                    .dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
            toTransfer.get(0).subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                    .baseMipLevel(0).levelCount(1).baseArrayLayer(0).layerCount(1);
            vkCmdPipelineBarrier(command, upload.initialized
                            ? VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT
                            : VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT,
                    VK_PIPELINE_STAGE_TRANSFER_BIT, 0, null, null, toTransfer);
            VkBufferImageCopy.Buffer region = VkBufferImageCopy.calloc(1, stack);
            region.get(0).bufferOffset(offset).bufferRowLength(0).bufferImageHeight(0);
            region.get(0).imageSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                    .mipLevel(0).baseArrayLayer(0).layerCount(1);
            region.get(0).imageOffset().x(upload.x).y(upload.y).z(0);
            region.get(0).imageExtent().width(upload.texture.width())
                    .height(upload.texture.height()).depth(1);
            vkCmdCopyBufferToImage(command, staging.buffer, target.image,
                    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, region);
            VkImageMemoryBarrier.Buffer toShader = VkImageMemoryBarrier.calloc(1, stack);
            toShader.get(0).sType$Default()
                    .oldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
                    .newLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
                    .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .image(target.image).srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                    .dstAccessMask(VK_ACCESS_SHADER_READ_BIT);
            toShader.get(0).subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                    .baseMipLevel(0).levelCount(1).baseArrayLayer(0).layerCount(1);
            vkCmdPipelineBarrier(command, VK_PIPELINE_STAGE_TRANSFER_BIT,
                    VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, 0, null, null, toShader);
            target.initialized = true;
            offset += upload.texture.byteSize();
        }
        uploadBytes = Math.addExact(uploadBytes, byteCount);
        uploadBatches++;
        pending.clear();
    }

    void appendStatistics(Map<String, Long> statistics) {
        statistics.put("texture.uploadBytes", uploadBytes);
        statistics.put("texture.uploadBatches", uploadBatches);
        statistics.put("texture.uploadSlotGrowths", uploadSlotGrowths);
        statistics.put("texture.mutationFenceWaits", mutationFenceWaits);
        statistics.put("texture.pendingUploads", (long) pending.size());
        statistics.put("texture.retired", retiredResources);
        statistics.put("texture.retiredReleased", releasedResources);
        statistics.put("texture.retiredPending", (long) retired.size());
    }

    void close(Consumer<TextureResource> destroyer) {
        pending.clear();
        releaseRetired(destroyer);
        if (retiredResources != releasedResources) {
            throw new IllegalStateException("texture retirement queue leaked resources");
        }
    }

    private BufferAllocation ensureUploadSlot(
            MemoryStack stack, BufferAllocation[] allocations,
            int[] capacities, int slot, int requiredBytes) {
        if (slot < 0 || slot >= allocations.length) {
            throw new IllegalArgumentException("texture upload slot is out of range");
        }
        BufferAllocation existing = allocations[slot];
        if (existing != null && capacities[slot] >= requiredBytes) return existing;
        if (existing != null) memory.destroyBuffer(existing);
        int capacity = 1;
        while (capacity < requiredBytes && capacity > 0) capacity <<= 1;
        if (capacity <= 0) capacity = requiredBytes;
        BufferAllocation created = memory.allocateBuffer(stack, capacity,
                VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
        try {
            memory.map(created, capacity, stack, "persistent texture upload slot");
        } catch (Throwable failure) {
            memory.destroyBuffer(created);
            throw failure;
        }
        allocations[slot] = created;
        capacities[slot] = capacity;
        uploadSlotGrowths++;
        if (uploadSlotGrowths == 1L) {
            System.out.println("[Vulkan Mod/Driver] Persistent mapped texture upload slots "
                    + "active (slots=" + execution.totalUploadSlotCount() + ")");
        }
        return created;
    }

    private static boolean isBlueFirstFormat(int format) {
        return format == VK_FORMAT_B8G8R8A8_UNORM || format == VK_FORMAT_B8G8R8A8_SRGB;
    }

    private static final class PendingUpload {
        private final long textureHandle;
        private final int x;
        private final int y;
        private final VulkanTextureData texture;
        private final boolean initialized;

        private PendingUpload(long textureHandle, int x, int y,
                              VulkanTextureData texture, boolean initialized) {
            this.textureHandle = textureHandle;
            this.x = x;
            this.y = y;
            this.texture = texture;
            this.initialized = initialized;
        }
    }
}
