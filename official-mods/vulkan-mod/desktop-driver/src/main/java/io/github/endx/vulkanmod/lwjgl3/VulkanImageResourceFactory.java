package io.github.endx.vulkanmod.lwjgl3;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkFramebufferCreateInfo;
import org.lwjgl.vulkan.VkImageCreateInfo;
import org.lwjgl.vulkan.VkImageViewCreateInfo;

import java.nio.LongBuffer;
import java.util.Map;

import static org.lwjgl.vulkan.VK10.*;

/** Creates and destroys sampled images and offscreen color targets for one Vulkan device. */
final class VulkanImageResourceFactory {
    private final VkDevice device;
    private final VulkanMemoryAllocator memory;
    private long sampledCreates;
    private long renderTargetCreates;
    private long destroys;
    private long liveResources;
    private long peakResources;

    VulkanImageResourceFactory(VkDevice device, VulkanMemoryAllocator memory) {
        if (device == null) throw new NullPointerException("device");
        if (memory == null) throw new NullPointerException("memory");
        this.device = device;
        this.memory = memory;
    }

    TextureResource createSampled(MemoryStack stack, int width, int height) {
        TextureResource created = createImage(stack, width, height,
                VK_FORMAT_R8G8B8A8_UNORM,
                VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT,
                false, "texture");
        sampledCreates++;
        retainResource();
        return created;
    }

    TextureResource createRenderTarget(MemoryStack stack, int width, int height,
                                           int format, long renderPass) {
        if (renderPass == VK_NULL_HANDLE) {
            throw new IllegalArgumentException("render pass handle is zero");
        }
        TextureResource created = createImage(stack, width, height, format,
                VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT | VK_IMAGE_USAGE_SAMPLED_BIT
                        | VK_IMAGE_USAGE_TRANSFER_SRC_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT,
                true, "render target");
        boolean complete = false;
        try {
            VkFramebufferCreateInfo framebufferInfo = VkFramebufferCreateInfo.calloc(stack)
                    .sType$Default().renderPass(renderPass)
                    .pAttachments(stack.longs(created.view))
                    .width(width).height(height).layers(1);
            LongBuffer handle = stack.mallocLong(1);
            check(vkCreateFramebuffer(device, framebufferInfo, null, handle),
                    "vkCreateFramebuffer(render target)");
            created.framebuffer = handle.get(0);
            renderTargetCreates++;
            retainResource();
            complete = true;
            return created;
        } finally {
            if (!complete) destroyUntracked(created);
        }
    }

    void destroy(TextureResource resource) {
        if (resource == null || resource.destroyed) return;
        resource.destroyed = true;
        destroyHandles(resource);
        destroys++;
        liveResources = Math.subtractExact(liveResources, 1L);
    }

    void appendStatistics(Map<String, Long> statistics) {
        statistics.put("image.sampledCreates", sampledCreates);
        statistics.put("image.renderTargetCreates", renderTargetCreates);
        statistics.put("image.destroys", destroys);
        statistics.put("image.liveResources", liveResources);
        statistics.put("image.peakResources", peakResources);
    }

    void assertFullyReleased() {
        long creates = Math.addExact(sampledCreates, renderTargetCreates);
        if (liveResources != 0L || creates != destroys) {
            throw new IllegalStateException("Vulkan image resource leak: live=" + liveResources
                    + ", creates=" + creates + ", destroys=" + destroys);
        }
    }

    private TextureResource createImage(MemoryStack stack, int width, int height,
                                            int format, int usage,
                                            boolean renderTarget, String label) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("image size must be positive");
        }
        TextureResource created = new TextureResource();
        created.width = width;
        created.height = height;
        created.renderTarget = renderTarget;
        boolean complete = false;
        try {
            VkImageCreateInfo imageInfo = VkImageCreateInfo.calloc(stack).sType$Default()
                    .imageType(VK_IMAGE_TYPE_2D).format(format)
                    .mipLevels(1).arrayLayers(1).samples(VK_SAMPLE_COUNT_1_BIT)
                    .tiling(VK_IMAGE_TILING_OPTIMAL).usage(usage)
                    .sharingMode(VK_SHARING_MODE_EXCLUSIVE)
                    .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            imageInfo.extent().width(width).height(height).depth(1);
            LongBuffer handle = stack.mallocLong(1);
            check(vkCreateImage(device, imageInfo, null, handle),
                    "vkCreateImage(" + label + ")");
            created.image = handle.get(0);
            created.memory = memory.allocateAndBindImage(stack, created.image,
                    VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, label);

            VkImageViewCreateInfo viewInfo = VkImageViewCreateInfo.calloc(stack)
                    .sType$Default().image(created.image)
                    .viewType(VK_IMAGE_VIEW_TYPE_2D).format(format);
            viewInfo.components().r(VK_COMPONENT_SWIZZLE_IDENTITY)
                    .g(VK_COMPONENT_SWIZZLE_IDENTITY)
                    .b(VK_COMPONENT_SWIZZLE_IDENTITY)
                    .a(VK_COMPONENT_SWIZZLE_IDENTITY);
            viewInfo.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                    .baseMipLevel(0).levelCount(1).baseArrayLayer(0).layerCount(1);
            check(vkCreateImageView(device, viewInfo, null, handle),
                    "vkCreateImageView(" + label + ")");
            created.view = handle.get(0);
            complete = true;
            return created;
        } finally {
            if (!complete) destroyUntracked(created);
        }
    }

    private void destroyUntracked(TextureResource resource) {
        if (resource == null || resource.destroyed) return;
        resource.destroyed = true;
        destroyHandles(resource);
    }

    private void destroyHandles(TextureResource resource) {
        if (resource.framebuffer != VK_NULL_HANDLE) {
            vkDestroyFramebuffer(device, resource.framebuffer, null);
            resource.framebuffer = VK_NULL_HANDLE;
        }
        if (resource.view != VK_NULL_HANDLE) {
            vkDestroyImageView(device, resource.view, null);
            resource.view = VK_NULL_HANDLE;
        }
        if (resource.image != VK_NULL_HANDLE) {
            vkDestroyImage(device, resource.image, null);
            resource.image = VK_NULL_HANDLE;
        }
        if (resource.memory != VK_NULL_HANDLE) {
            memory.freeImageMemory(resource.memory);
            resource.memory = VK_NULL_HANDLE;
        }
    }

    private void retainResource() {
        liveResources = Math.addExact(liveResources, 1L);
        peakResources = Math.max(peakResources, liveResources);
    }

    private static void check(int result, String operation) {
        if (result != VK_SUCCESS) {
            throw new IllegalStateException(operation + " failed with VkResult " + result);
        }
    }
}
