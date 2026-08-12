package io.github.endx.vulkanmod.lwjgl3;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.vulkan.VK10.*;

/** Owns Vulkan device-memory selection and raw buffer/image allocation bookkeeping. */
final class VulkanMemoryAllocator {
    private final VkDevice device;
    private final int[] memoryTypeFlags;
    private final Map<Long, Long> imageAllocationSizes = new HashMap<Long, Long>();
    private long bufferAllocations;
    private long bufferFrees;
    private long imageAllocations;
    private long imageFrees;
    private long mapCalls;
    private long liveBytes;
    private long peakBytes;

    VulkanMemoryAllocator(VkPhysicalDevice physicalDevice, VkDevice device) {
        if (physicalDevice == null) throw new NullPointerException("physicalDevice");
        if (device == null) throw new NullPointerException("device");
        this.device = device;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPhysicalDeviceMemoryProperties properties =
                    VkPhysicalDeviceMemoryProperties.malloc(stack);
            vkGetPhysicalDeviceMemoryProperties(physicalDevice, properties);
            memoryTypeFlags = new int[properties.memoryTypeCount()];
            for (int index = 0; index < memoryTypeFlags.length; index++) {
                memoryTypeFlags[index] = properties.memoryTypes(index).propertyFlags();
            }
        }
    }

    BufferAllocation allocateBuffer(MemoryStack stack, long size,
                                    int usage, int requiredMemoryFlags) {
        if (size <= 0L) throw new IllegalArgumentException("buffer size must be positive");
        VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.calloc(stack).sType$Default()
                .size(size).usage(usage).sharingMode(VK_SHARING_MODE_EXCLUSIVE);
        LongBuffer handle = stack.mallocLong(1);
        check(vkCreateBuffer(device, bufferInfo, null, handle), "vkCreateBuffer");
        long buffer = handle.get(0);
        long memory = VK_NULL_HANDLE;
        try {
            VkMemoryRequirements requirements = VkMemoryRequirements.malloc(stack);
            vkGetBufferMemoryRequirements(device, buffer, requirements);
            VkMemoryAllocateInfo allocation = VkMemoryAllocateInfo.calloc(stack)
                    .sType$Default().allocationSize(requirements.size())
                    .memoryTypeIndex(findMemoryType(requirements.memoryTypeBits(),
                            requiredMemoryFlags));
            check(vkAllocateMemory(device, allocation, null, handle), "vkAllocateMemory(buffer)");
            memory = handle.get(0);
            check(vkBindBufferMemory(device, buffer, memory, 0), "vkBindBufferMemory");
            BufferAllocation created = new BufferAllocation(
                    buffer, memory, requirements.size());
            bufferAllocations++;
            retainBytes(created.allocationBytes);
            return created;
        } catch (Throwable failure) {
            if (memory != VK_NULL_HANDLE) vkFreeMemory(device, memory, null);
            vkDestroyBuffer(device, buffer, null);
            throw failure;
        }
    }

    long allocateAndBindImage(MemoryStack stack, long image,
                              int requiredMemoryFlags, String label) {
        if (image == VK_NULL_HANDLE) throw new IllegalArgumentException("image handle is zero");
        VkMemoryRequirements requirements = VkMemoryRequirements.malloc(stack);
        vkGetImageMemoryRequirements(device, image, requirements);
        VkMemoryAllocateInfo allocation = VkMemoryAllocateInfo.calloc(stack)
                .sType$Default().allocationSize(requirements.size())
                .memoryTypeIndex(findMemoryType(requirements.memoryTypeBits(),
                        requiredMemoryFlags));
        LongBuffer handle = stack.mallocLong(1);
        check(vkAllocateMemory(device, allocation, null, handle),
                "vkAllocateMemory(" + label + ")");
        long memory = handle.get(0);
        try {
            check(vkBindImageMemory(device, image, memory, 0),
                    "vkBindImageMemory(" + label + ")");
        } catch (Throwable failure) {
            vkFreeMemory(device, memory, null);
            throw failure;
        }
        imageAllocationSizes.put(Long.valueOf(memory), Long.valueOf(requirements.size()));
        imageAllocations++;
        retainBytes(requirements.size());
        return memory;
    }

    ByteBuffer map(BufferAllocation allocation, int byteCount,
                   MemoryStack stack, String label) {
        if (allocation == null) throw new NullPointerException("allocation");
        if (allocation.mapped != null) return allocation.mapped;
        if (byteCount <= 0 || (long) byteCount > allocation.allocationBytes) {
            throw new IllegalArgumentException("mapped byte count exceeds allocation");
        }
        PointerBuffer mapped = stack.mallocPointer(1);
        check(vkMapMemory(device, allocation.memory, 0, byteCount, 0, mapped),
                "vkMapMemory(" + label + ")");
        allocation.mapped = MemoryUtil.memByteBuffer(mapped.get(0), byteCount)
                .order(ByteOrder.nativeOrder());
        mapCalls++;
        return allocation.mapped;
    }

    void unmap(BufferAllocation allocation) {
        if (allocation == null || allocation.mapped == null) return;
        vkUnmapMemory(device, allocation.memory);
        allocation.mapped = null;
    }

    void destroyBuffer(BufferAllocation allocation) {
        if (allocation == null || allocation.destroyed) return;
        allocation.destroyed = true;
        unmap(allocation);
        if (allocation.buffer != VK_NULL_HANDLE) {
            vkDestroyBuffer(device, allocation.buffer, null);
        }
        if (allocation.memory != VK_NULL_HANDLE) {
            vkFreeMemory(device, allocation.memory, null);
        }
        bufferFrees++;
        releaseBytes(allocation.allocationBytes);
    }

    void freeImageMemory(long memory) {
        if (memory == VK_NULL_HANDLE) return;
        Long size = imageAllocationSizes.remove(Long.valueOf(memory));
        if (size == null) {
            throw new IllegalStateException("image memory is not owned by this allocator");
        }
        vkFreeMemory(device, memory, null);
        imageFrees++;
        releaseBytes(size.longValue());
    }

    void appendStatistics(Map<String, Long> statistics) {
        statistics.put("memory.bufferAllocations", bufferAllocations);
        statistics.put("memory.bufferFrees", bufferFrees);
        statistics.put("memory.imageAllocations", imageAllocations);
        statistics.put("memory.imageFrees", imageFrees);
        statistics.put("memory.mapCalls", mapCalls);
        statistics.put("memory.liveBytes", liveBytes);
        statistics.put("memory.peakBytes", peakBytes);
    }

    long liveBytes() { return liveBytes; }
    long peakBytes() { return peakBytes; }

    void assertFullyReleased() {
        if (liveBytes != 0L || !imageAllocationSizes.isEmpty()
                || bufferAllocations != bufferFrees || imageAllocations != imageFrees) {
            throw new IllegalStateException("Vulkan memory leak: liveBytes=" + liveBytes
                    + ", buffers=" + bufferAllocations + "/" + bufferFrees
                    + ", images=" + imageAllocations + "/" + imageFrees);
        }
    }

    private int findMemoryType(int typeBits, int requiredFlags) {
        for (int index = 0; index < memoryTypeFlags.length; index++) {
            if ((typeBits & (1 << index)) != 0
                    && (memoryTypeFlags[index] & requiredFlags) == requiredFlags) return index;
        }
        throw new IllegalStateException("No Vulkan memory type supports flags 0x"
                + Integer.toHexString(requiredFlags));
    }

    private void retainBytes(long size) {
        liveBytes = Math.addExact(liveBytes, size);
        peakBytes = Math.max(peakBytes, liveBytes);
    }

    private void releaseBytes(long size) {
        liveBytes = Math.subtractExact(liveBytes, size);
    }

    static final class BufferAllocation {
        final long buffer;
        final long memory;
        final long allocationBytes;
        ByteBuffer mapped;
        boolean destroyed;

        private BufferAllocation(long buffer, long memory, long allocationBytes) {
            this.buffer = buffer;
            this.memory = memory;
            this.allocationBytes = allocationBytes;
        }
    }

    private static void check(int result, String operation) {
        if (result != VK_SUCCESS) {
            throw new IllegalStateException(operation + " failed with VkResult " + result);
        }
    }
}
