package io.github.endx.vulkanmod.lwjgl3;

import io.github.endx.vulkanmod.spi.VulkanTextureFilter;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorPoolCreateInfo;
import org.lwjgl.vulkan.VkDescriptorPoolSize;
import org.lwjgl.vulkan.VkDescriptorSetAllocateInfo;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkSamplerCreateInfo;

import java.nio.LongBuffer;
import java.util.ArrayDeque;
import java.util.Map;

import static org.lwjgl.vulkan.VK10.*;

/** Owns the shared sampler, descriptor layout/pool, and fence-released descriptor recycler. */
final class VulkanDescriptorAllocator implements AutoCloseable {
    private final VkDevice device;
    private final int maximumSets;
    private final long[] samplers = new long[VulkanTextureFilter.values().length];
    private final ArrayDeque<Long> recycled = new ArrayDeque<Long>();
    private long layout;
    private long pool;
    private long allocations;
    private long reuses;

    VulkanDescriptorAllocator(VkDevice device, int maximumSets) {
        if (device == null) throw new NullPointerException("device");
        if (maximumSets <= 0) throw new IllegalArgumentException("maximumSets");
        this.device = device;
        this.maximumSets = maximumSets;
    }

    void ensureInitialized(MemoryStack stack) {
        if (layout != VK_NULL_HANDLE) return;
        VkDescriptorSetLayoutBinding.Buffer bindings =
                VkDescriptorSetLayoutBinding.calloc(2, stack);
        for (int index = 0; index < bindings.capacity(); index++) {
            bindings.get(index).binding(index)
                    .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .descriptorCount(1).stageFlags(VK_SHADER_STAGE_VERTEX_BIT
                            | VK_SHADER_STAGE_FRAGMENT_BIT);
        }
        VkDescriptorSetLayoutCreateInfo layoutInfo =
                VkDescriptorSetLayoutCreateInfo.calloc(stack).sType$Default()
                        .pBindings(bindings);
        LongBuffer handle = stack.mallocLong(1);
        check(vkCreateDescriptorSetLayout(device, layoutInfo, null, handle),
                "vkCreateDescriptorSetLayout(texture)");
        layout = handle.get(0);

        VkDescriptorPoolSize.Buffer poolSize = VkDescriptorPoolSize.calloc(1, stack);
        poolSize.get(0).type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                .descriptorCount(maximumSets * 2);
        VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.calloc(stack)
                .sType$Default().maxSets(maximumSets).pPoolSizes(poolSize);
        check(vkCreateDescriptorPool(device, poolInfo, null, handle),
                "vkCreateDescriptorPool(texture)");
        pool = handle.get(0);

        for (VulkanTextureFilter filter : VulkanTextureFilter.values()) {
            int vkFilter = filter == VulkanTextureFilter.LINEAR
                    ? VK_FILTER_LINEAR : VK_FILTER_NEAREST;
            VkSamplerCreateInfo samplerInfo = VkSamplerCreateInfo.calloc(stack)
                    .sType$Default().magFilter(vkFilter).minFilter(vkFilter)
                    .mipmapMode(filter == VulkanTextureFilter.LINEAR
                            ? VK_SAMPLER_MIPMAP_MODE_LINEAR
                            : VK_SAMPLER_MIPMAP_MODE_NEAREST)
                    .addressModeU(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
                    .addressModeV(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
                    .addressModeW(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
                    .mipLodBias(0.0f).anisotropyEnable(false).maxAnisotropy(1.0f)
                    .compareEnable(false).compareOp(VK_COMPARE_OP_ALWAYS)
                    .minLod(0.0f).maxLod(0.0f)
                    .borderColor(VK_BORDER_COLOR_INT_OPAQUE_BLACK)
                    .unnormalizedCoordinates(false);
            check(vkCreateSampler(device, samplerInfo, null, handle),
                    "vkCreateSampler(shared " + filter + ")");
            samplers[filter.ordinal()] = handle.get(0);
        }
    }

    long acquire(MemoryStack stack) {
        Long descriptor = recycled.pollFirst();
        if (descriptor != null) {
            reuses++;
            return descriptor.longValue();
        }
        VkDescriptorSetAllocateInfo allocateInfo = VkDescriptorSetAllocateInfo.calloc(stack)
                .sType$Default().descriptorPool(pool)
                .pSetLayouts(stack.longs(layout));
        LongBuffer result = stack.mallocLong(1);
        check(vkAllocateDescriptorSets(device, allocateInfo, result),
                "vkAllocateDescriptorSets(texture)");
        allocations++;
        return result.get(0);
    }

    void recycle(long descriptorSet) {
        if (descriptorSet != VK_NULL_HANDLE && pool != VK_NULL_HANDLE) {
            recycled.addFirst(descriptorSet);
        }
    }

    long layout() { return layout; }
    long sampler(VulkanTextureFilter filter) { return samplers[filter.ordinal()]; }
    boolean initialized() { return layout != VK_NULL_HANDLE; }

    void appendStatistics(Map<String, Long> statistics) {
        statistics.put("descriptor.allocations", allocations);
        statistics.put("descriptor.reuses", reuses);
        statistics.put("descriptor.recycled", (long) recycled.size());
    }

    @Override public void close() {
        if (pool != VK_NULL_HANDLE) {
            vkDestroyDescriptorPool(device, pool, null);
            pool = VK_NULL_HANDLE;
        }
        recycled.clear();
        for (int index = 0; index < samplers.length; index++) {
            if (samplers[index] != VK_NULL_HANDLE) {
                vkDestroySampler(device, samplers[index], null);
                samplers[index] = VK_NULL_HANDLE;
            }
        }
        if (layout != VK_NULL_HANDLE) {
            vkDestroyDescriptorSetLayout(device, layout, null);
            layout = VK_NULL_HANDLE;
        }
    }

    private static void check(int result, String operation) {
        if (result != VK_SUCCESS) {
            throw new IllegalStateException(operation + " failed with VkResult " + result);
        }
    }
}
