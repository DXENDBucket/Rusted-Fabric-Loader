package io.github.endx.vulkanmod.lwjgl3;

import io.github.endx.vulkanmod.spi.VulkanSurfaceInfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkAttachmentDescription;
import org.lwjgl.vulkan.VkAttachmentReference;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkFramebufferCreateInfo;
import org.lwjgl.vulkan.VkImageViewCreateInfo;
import org.lwjgl.vulkan.VkRenderPassCreateInfo;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;
import org.lwjgl.vulkan.VkSubpassDependency;
import org.lwjgl.vulkan.VkSubpassDescription;

import java.nio.LongBuffer;
import java.util.Map;

import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

/** Owns one device's current WSI generation and every resource tied to its swapchain images. */
final class VulkanSwapchainLifecycle implements AutoCloseable {
    private final VkDevice device;
    private final VulkanPipelineLibrary pipelines;
    private long swapchain;
    private long[] images;
    private VulkanSurfaceInfo info;
    private long[] imageViews = new long[0];
    private long renderPass;
    private long[] framebuffers = new long[0];
    private long[] renderFinishedSemaphores = new long[0];
    private long generations;
    private long recreates;
    private long imageViewCreates;
    private long imageViewDestroys;
    private long framebufferCreates;
    private long framebufferDestroys;
    private long semaphoreCreates;
    private long semaphoreDestroys;
    private long swapchainDestroys;
    private boolean initialized;
    private boolean closed;

    VulkanSwapchainLifecycle(VkDevice device, VulkanPipelineLibrary pipelines,
                             long swapchain, long[] images, VulkanSurfaceInfo info) {
        if (device == null) throw new NullPointerException("device");
        if (pipelines == null) throw new NullPointerException("pipelines");
        validateGeneration(swapchain, images, info);
        this.device = device;
        this.pipelines = pipelines;
        this.swapchain = swapchain;
        this.images = images.clone();
        this.info = info;
        this.generations = 1L;
    }

    void initialize(MemoryStack stack) {
        ensureOpen();
        if (initialized) throw new IllegalStateException("swapchain is already initialized");
        createImageResources(stack);
        initialized = true;
    }

    void replace(MemoryStack stack, long replacementSwapchain,
                 long[] replacementImages, VulkanSurfaceInfo replacementInfo) {
        ensureInitialized();
        validateGeneration(replacementSwapchain, replacementImages, replacementInfo);
        releaseImageResources();
        destroySwapchainHandle();
        swapchain = replacementSwapchain;
        images = replacementImages.clone();
        info = replacementInfo;
        generations++;
        initialized = false;
        createImageResources(stack);
        initialized = true;
        recreates++;
    }

    long handle() {
        ensureInitialized();
        return swapchain;
    }

    int imageCount() {
        ensureInitialized();
        return images.length;
    }

    VulkanSurfaceInfo info() {
        ensureInitialized();
        return info;
    }

    long renderPass() {
        ensureInitialized();
        return renderPass;
    }

    long framebuffer(int imageIndex) {
        ensureImageIndex(imageIndex);
        return framebuffers[imageIndex];
    }

    long renderFinishedSemaphore(int imageIndex) {
        ensureImageIndex(imageIndex);
        return renderFinishedSemaphores[imageIndex];
    }

    void appendStatistics(Map<String, Long> statistics) {
        statistics.put("swapchain.generations", generations);
        statistics.put("swapchain.recreates", recreates);
        statistics.put("swapchain.images", initialized ? (long) images.length : 0L);
        statistics.put("swapchain.imageViewCreates", imageViewCreates);
        statistics.put("swapchain.imageViewDestroys", imageViewDestroys);
        statistics.put("swapchain.imageViewsLive", imageViewCreates - imageViewDestroys);
        statistics.put("swapchain.framebufferCreates", framebufferCreates);
        statistics.put("swapchain.framebufferDestroys", framebufferDestroys);
        statistics.put("swapchain.framebuffersLive", framebufferCreates - framebufferDestroys);
        statistics.put("swapchain.semaphoreCreates", semaphoreCreates);
        statistics.put("swapchain.semaphoreDestroys", semaphoreDestroys);
        statistics.put("swapchain.semaphoresLive", semaphoreCreates - semaphoreDestroys);
        statistics.put("swapchain.handleDestroys", swapchainDestroys);
    }

    @Override public void close() {
        if (closed) return;
        releaseImageResources();
        destroySwapchainHandle();
        images = new long[0];
        initialized = false;
        closed = true;
        if (imageViewCreates != imageViewDestroys
                || framebufferCreates != framebufferDestroys
                || semaphoreCreates != semaphoreDestroys
                || swapchainDestroys != generations) {
            throw new IllegalStateException("swapchain lifecycle leaked native resources");
        }
    }

    private void createImageResources(MemoryStack stack) {
        LongBuffer handle = stack.mallocLong(1);
        VkSemaphoreCreateInfo semaphoreInfo = VkSemaphoreCreateInfo.calloc(stack)
                .sType$Default();
        renderFinishedSemaphores = new long[images.length];
        for (int index = 0; index < images.length; index++) {
            check(vkCreateSemaphore(device, semaphoreInfo, null, handle),
                    "vkCreateSemaphore(renderFinished[" + index + "])");
            renderFinishedSemaphores[index] = handle.get(0);
            semaphoreCreates++;
        }

        imageViews = new long[images.length];
        for (int index = 0; index < images.length; index++) {
            VkImageViewCreateInfo viewInfo = VkImageViewCreateInfo.calloc(stack)
                    .sType$Default().image(images[index])
                    .viewType(VK_IMAGE_VIEW_TYPE_2D).format(info.imageFormat());
            viewInfo.components()
                    .r(VK_COMPONENT_SWIZZLE_IDENTITY)
                    .g(VK_COMPONENT_SWIZZLE_IDENTITY)
                    .b(VK_COMPONENT_SWIZZLE_IDENTITY)
                    .a(VK_COMPONENT_SWIZZLE_IDENTITY);
            viewInfo.subresourceRange()
                    .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                    .baseMipLevel(0).levelCount(1)
                    .baseArrayLayer(0).layerCount(1);
            check(vkCreateImageView(device, viewInfo, null, handle),
                    "vkCreateImageView(swapchain[" + index + "])");
            imageViews[index] = handle.get(0);
            imageViewCreates++;
        }

        VkAttachmentDescription.Buffer attachments = VkAttachmentDescription.calloc(1, stack);
        attachments.get(0).format(info.imageFormat()).samples(VK_SAMPLE_COUNT_1_BIT)
                .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR).storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                .finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);
        VkAttachmentReference.Buffer colorReference = VkAttachmentReference.calloc(1, stack);
        colorReference.get(0).attachment(0).layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
        VkSubpassDescription.Buffer subpass = VkSubpassDescription.calloc(1, stack);
        subpass.get(0).pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
                .colorAttachmentCount(1).pColorAttachments(colorReference);
        VkSubpassDependency.Buffer dependency = VkSubpassDependency.calloc(1, stack);
        dependency.get(0).srcSubpass(VK_SUBPASS_EXTERNAL).dstSubpass(0)
                .srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                .dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                .srcAccessMask(0).dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);
        VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.calloc(stack)
                .sType$Default().pAttachments(attachments)
                .pSubpasses(subpass).pDependencies(dependency);
        check(vkCreateRenderPass(device, renderPassInfo, null, handle),
                "vkCreateRenderPass(swapchain)");
        renderPass = handle.get(0);
        pipelines.setRenderPass(renderPass);

        framebuffers = new long[imageViews.length];
        for (int index = 0; index < imageViews.length; index++) {
            VkFramebufferCreateInfo framebufferInfo = VkFramebufferCreateInfo.calloc(stack)
                    .sType$Default().renderPass(renderPass)
                    .pAttachments(stack.longs(imageViews[index]))
                    .width(info.width()).height(info.height()).layers(1);
            check(vkCreateFramebuffer(device, framebufferInfo, null, handle),
                    "vkCreateFramebuffer(swapchain[" + index + "])");
            framebuffers[index] = handle.get(0);
            framebufferCreates++;
        }
    }

    private void releaseImageResources() {
        for (long framebuffer : framebuffers) {
            if (framebuffer != VK_NULL_HANDLE) {
                vkDestroyFramebuffer(device, framebuffer, null);
                framebufferDestroys++;
            }
        }
        framebuffers = new long[0];
        pipelines.releaseRenderPass();
        if (renderPass != VK_NULL_HANDLE) {
            vkDestroyRenderPass(device, renderPass, null);
            renderPass = VK_NULL_HANDLE;
        }
        for (long imageView : imageViews) {
            if (imageView != VK_NULL_HANDLE) {
                vkDestroyImageView(device, imageView, null);
                imageViewDestroys++;
            }
        }
        imageViews = new long[0];
        for (long semaphore : renderFinishedSemaphores) {
            if (semaphore != VK_NULL_HANDLE) {
                vkDestroySemaphore(device, semaphore, null);
                semaphoreDestroys++;
            }
        }
        renderFinishedSemaphores = new long[0];
    }

    private void destroySwapchainHandle() {
        if (swapchain != VK_NULL_HANDLE) {
            vkDestroySwapchainKHR(device, swapchain, null);
            swapchain = VK_NULL_HANDLE;
            swapchainDestroys++;
        }
    }

    private void ensureImageIndex(int imageIndex) {
        ensureInitialized();
        if (imageIndex < 0 || imageIndex >= images.length) {
            throw new IndexOutOfBoundsException("swapchain image " + imageIndex);
        }
    }

    private void ensureInitialized() {
        ensureOpen();
        if (!initialized) throw new IllegalStateException("swapchain is not initialized");
    }

    private void ensureOpen() {
        if (closed) throw new IllegalStateException("swapchain lifecycle is closed");
    }

    private static void validateGeneration(long swapchain, long[] images,
                                           VulkanSurfaceInfo info) {
        if (swapchain == VK_NULL_HANDLE) throw new IllegalArgumentException("swapchain");
        if (images == null || images.length == 0) {
            throw new IllegalArgumentException("swapchain images must not be empty");
        }
        if (info == null) throw new NullPointerException("info");
    }

    private static void check(int result, String operation) {
        if (result != VK_SUCCESS) {
            throw new IllegalStateException(operation + " failed with VkResult " + result);
        }
    }
}
