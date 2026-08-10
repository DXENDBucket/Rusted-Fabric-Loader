package io.github.endx.vulkanmod.lwjgl3;

import io.github.endx.vulkanmod.spi.VulkanDeviceInfo;
import io.github.endx.vulkanmod.spi.VulkanColoredQuad;
import io.github.endx.vulkanmod.spi.VulkanFrameCommands;
import io.github.endx.vulkanmod.spi.VulkanPlatformDriver;
import io.github.endx.vulkanmod.spi.VulkanProbeResult;
import io.github.endx.vulkanmod.spi.VulkanSurfaceInfo;
import io.github.endx.vulkanmod.spi.VulkanSurfaceRequest;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK;
import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkAttachmentDescription;
import org.lwjgl.vulkan.VkAttachmentReference;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkClearValue;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkDeviceCreateInfo;
import org.lwjgl.vulkan.VkDeviceQueueCreateInfo;
import org.lwjgl.vulkan.VkExtensionProperties;
import org.lwjgl.vulkan.VkFenceCreateInfo;
import org.lwjgl.vulkan.VkFramebufferCreateInfo;
import org.lwjgl.vulkan.VkImageViewCreateInfo;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;
import org.lwjgl.vulkan.VkGraphicsPipelineCreateInfo;
import org.lwjgl.vulkan.VkPipelineColorBlendAttachmentState;
import org.lwjgl.vulkan.VkPipelineColorBlendStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineDynamicStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineInputAssemblyStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo;
import org.lwjgl.vulkan.VkPipelineMultisampleStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineRasterizationStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineViewportStateCreateInfo;
import org.lwjgl.vulkan.VkPresentInfoKHR;
import org.lwjgl.vulkan.VkQueue;
import org.lwjgl.vulkan.VkQueueFamilyProperties;
import org.lwjgl.vulkan.VkRenderPassBeginInfo;
import org.lwjgl.vulkan.VkRenderPassCreateInfo;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;
import org.lwjgl.vulkan.VkSubmitInfo;
import org.lwjgl.vulkan.VkSubpassDependency;
import org.lwjgl.vulkan.VkSubpassDescription;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;
import org.lwjgl.vulkan.VkViewport;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkWin32SurfaceCreateInfoKHR;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.KHRWin32Surface.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK11.VK_API_VERSION_1_1;
import static org.lwjgl.util.shaderc.Shaderc.*;

/** LWJGL 3 desktop driver, loaded in a child-first class loader beside the LWJGL 2 game. */
public final class Lwjgl3VulkanDriver implements VulkanPlatformDriver {
    private static final int VERTEX_FLOATS = 6;
    private static final int VERTEX_STRIDE = VERTEX_FLOATS * Float.BYTES;
    private static final String COLOR_VERTEX_SHADER = "#version 450\n"
            + "layout(location=0) in vec2 inPosition;\n"
            + "layout(location=1) in vec4 inColor;\n"
            + "layout(location=0) out vec4 color;\n"
            + "void main(){ gl_Position=vec4(inPosition,0.0,1.0); color=inColor; }\n";
    private static final String COLOR_FRAGMENT_SHADER = "#version 450\n"
            + "layout(location=0) in vec4 color;\n"
            + "layout(location=0) out vec4 outColor;\n"
            + "void main(){ outColor=color; }\n";
    private SurfaceSession surfaceSession;

    @Override public String name() { return "LWJGL 3 Vulkan"; }

    @Override public VulkanProbeResult probe() {
        try {
            int instanceVersion = VK.getInstanceVersionSupported();
            try (MemoryStack stack = MemoryStack.stackPush()) {
                VkInstanceCreateInfo createInfo = instanceCreateInfo(stack, instanceVersion, false);
                VkInstance instance = createInstance(stack, createInfo);
                try {
                    return VulkanProbeResult.available(instanceVersion,
                            enumerateDevices(stack, instance));
                } finally {
                    vkDestroyInstance(instance, null);
                }
            }
        } catch (Throwable failure) {
            return VulkanProbeResult.unavailable(failure.getClass().getSimpleName()
                    + ": " + String.valueOf(failure.getMessage()));
        }
    }

    @Override public synchronized VulkanSurfaceInfo createSurface(VulkanSurfaceRequest request) {
        if (!"win32".equals(request.platform())) {
            throw new IllegalArgumentException("Unsupported Vulkan surface platform: "
                    + request.platform());
        }
        if (surfaceSession != null) return surfaceSession.info;
        SurfaceSession created = null;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int instanceVersion = VK.getInstanceVersionSupported();
            VkInstanceCreateInfo instanceInfo = instanceCreateInfo(stack, instanceVersion, true);
            VkInstance instance = createInstance(stack, instanceInfo);
            long surface = 0L;
            VkDevice device = null;
            long swapchain = 0L;
            try {
                surface = createWin32Surface(stack, instance, request);
                DeviceCandidate candidate = selectDevice(stack, instance, surface);
                device = createDevice(stack, candidate);
                SwapchainResult swapchainResult = createSwapchain(stack, candidate, device,
                        surface, request.width(), request.height(), VK_NULL_HANDLE);
                swapchain = swapchainResult.handle;
                created = new SurfaceSession(instance, surface, candidate, device,
                        swapchainResult);
                try {
                    created.initialize();
                } catch (Throwable failure) {
                    created.close();
                    throw failure;
                }
                surfaceSession = created;
                return created.info;
            } finally {
                if (created == null) {
                    if (swapchain != 0L && device != null) {
                        vkDestroySwapchainKHR(device, swapchain, null);
                    }
                    if (device != null) vkDestroyDevice(device, null);
                    if (surface != 0L) vkDestroySurfaceKHR(instance, surface, null);
                    vkDestroyInstance(instance, null);
                }
            }
        }
    }

    @Override public synchronized VulkanSurfaceInfo presentFrame(VulkanFrameCommands frame) {
        if (surfaceSession == null) {
            throw new IllegalStateException("Vulkan surface has not been created");
        }
        if (frame == null) throw new NullPointerException("frame");
        return surfaceSession.presentFrame(frame);
    }

    @Override public synchronized void close() {
        if (surfaceSession != null) {
            surfaceSession.close();
            surfaceSession = null;
        }
    }

    private static VkInstanceCreateInfo instanceCreateInfo(
            MemoryStack stack, int instanceVersion, boolean surfaceExtensions) {
        VkApplicationInfo application = VkApplicationInfo.calloc(stack)
                .sType$Default()
                .pApplicationName(stack.UTF8("Rusted Fabric Vulkan Mod"))
                .applicationVersion(1)
                .pEngineName(stack.UTF8("Rusted Fabric"))
                .engineVersion(1)
                .apiVersion(Math.min(instanceVersion, VK_API_VERSION_1_1));
        VkInstanceCreateInfo result = VkInstanceCreateInfo.calloc(stack)
                .sType$Default().pApplicationInfo(application);
        if (surfaceExtensions) {
            result.ppEnabledExtensionNames(stack.pointers(
                    stack.UTF8(VK_KHR_SURFACE_EXTENSION_NAME),
                    stack.UTF8(VK_KHR_WIN32_SURFACE_EXTENSION_NAME)));
        }
        return result;
    }

    private static VkInstance createInstance(MemoryStack stack, VkInstanceCreateInfo createInfo) {
        PointerBuffer pointer = stack.mallocPointer(1);
        check(vkCreateInstance(createInfo, null, pointer), "vkCreateInstance");
        return new VkInstance(pointer.get(0), createInfo);
    }

    private static long createWin32Surface(MemoryStack stack, VkInstance instance,
                                           VulkanSurfaceRequest request) {
        VkWin32SurfaceCreateInfoKHR createInfo = VkWin32SurfaceCreateInfoKHR.calloc(stack)
                .sType$Default()
                .hinstance(request.instanceHandle())
                .hwnd(request.windowHandle());
        LongBuffer pointer = stack.mallocLong(1);
        check(vkCreateWin32SurfaceKHR(instance, createInfo, null, pointer),
                "vkCreateWin32SurfaceKHR");
        return pointer.get(0);
    }

    private static List<VulkanDeviceInfo> enumerateDevices(
            MemoryStack stack, VkInstance instance) {
        PointerBuffer pointers = physicalDevices(stack, instance);
        List<VulkanDeviceInfo> result = new ArrayList<VulkanDeviceInfo>(pointers.remaining());
        VkPhysicalDeviceProperties properties = VkPhysicalDeviceProperties.malloc(stack);
        for (int index = 0; index < pointers.remaining(); index++) {
            VkPhysicalDevice device = new VkPhysicalDevice(pointers.get(index), instance);
            vkGetPhysicalDeviceProperties(device, properties);
            result.add(new VulkanDeviceInfo(properties.deviceNameString(),
                    properties.vendorID(), properties.deviceID(), properties.deviceType(),
                    properties.apiVersion(), properties.driverVersion()));
        }
        return result;
    }

    private static PointerBuffer physicalDevices(MemoryStack stack, VkInstance instance) {
        IntBuffer count = stack.ints(0);
        check(vkEnumeratePhysicalDevices(instance, count, null),
                "vkEnumeratePhysicalDevices(count)");
        if (count.get(0) == 0) throw new IllegalStateException("No Vulkan physical devices found");
        PointerBuffer pointers = stack.mallocPointer(count.get(0));
        check(vkEnumeratePhysicalDevices(instance, count, pointers),
                "vkEnumeratePhysicalDevices(list)");
        return pointers;
    }

    private static DeviceCandidate selectDevice(
            MemoryStack stack, VkInstance instance, long surface) {
        PointerBuffer pointers = physicalDevices(stack, instance);
        DeviceCandidate fallback = null;
        for (int index = 0; index < pointers.remaining(); index++) {
            VkPhysicalDevice physicalDevice = new VkPhysicalDevice(pointers.get(index), instance);
            QueueFamilies queues = findQueueFamilies(stack, physicalDevice, surface);
            if (queues == null || !supportsSwapchain(stack, physicalDevice, surface)) continue;
            VkPhysicalDeviceProperties properties = VkPhysicalDeviceProperties.malloc(stack);
            vkGetPhysicalDeviceProperties(physicalDevice, properties);
            DeviceCandidate candidate = new DeviceCandidate(physicalDevice,
                    properties.deviceNameString(), properties.deviceType(), queues);
            if (properties.deviceType() == VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU) return candidate;
            if (fallback == null) fallback = candidate;
        }
        if (fallback == null) {
            throw new IllegalStateException(
                    "No Vulkan device supports graphics and presentation for this window");
        }
        return fallback;
    }

    private static QueueFamilies findQueueFamilies(
            MemoryStack stack, VkPhysicalDevice device, long surface) {
        IntBuffer count = stack.ints(0);
        vkGetPhysicalDeviceQueueFamilyProperties(device, count, null);
        VkQueueFamilyProperties.Buffer properties = VkQueueFamilyProperties.malloc(count.get(0), stack);
        vkGetPhysicalDeviceQueueFamilyProperties(device, count, properties);
        int graphics = -1;
        int present = -1;
        IntBuffer supported = stack.ints(VK_FALSE);
        for (int index = 0; index < count.get(0); index++) {
            if ((properties.get(index).queueFlags() & VK_QUEUE_GRAPHICS_BIT) != 0) graphics = index;
            supported.put(0, VK_FALSE);
            check(vkGetPhysicalDeviceSurfaceSupportKHR(device, index, surface, supported),
                    "vkGetPhysicalDeviceSurfaceSupportKHR");
            if (supported.get(0) == VK_TRUE) present = index;
            if (graphics >= 0 && present >= 0 && graphics == present) break;
        }
        return graphics >= 0 && present >= 0 ? new QueueFamilies(graphics, present) : null;
    }

    private static boolean supportsSwapchain(
            MemoryStack stack, VkPhysicalDevice device, long surface) {
        IntBuffer count = stack.ints(0);
        check(vkEnumerateDeviceExtensionProperties(device, (java.nio.ByteBuffer) null,
                count, null), "vkEnumerateDeviceExtensionProperties(count)");
        boolean swapchain = false;
        VkExtensionProperties.Buffer extensions = VkExtensionProperties.malloc(count.get(0));
        try {
            check(vkEnumerateDeviceExtensionProperties(device, (java.nio.ByteBuffer) null,
                    count, extensions), "vkEnumerateDeviceExtensionProperties(list)");
            for (int index = 0; index < count.get(0); index++) {
                if (VK_KHR_SWAPCHAIN_EXTENSION_NAME.equals(
                        extensions.get(index).extensionNameString())) {
                    swapchain = true;
                    break;
                }
            }
        } finally {
            extensions.free();
        }
        if (!swapchain) return false;
        count.put(0, 0);
        check(vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface, count, null),
                "vkGetPhysicalDeviceSurfaceFormatsKHR(count)");
        if (count.get(0) == 0) return false;
        count.put(0, 0);
        check(vkGetPhysicalDeviceSurfacePresentModesKHR(device, surface, count, null),
                "vkGetPhysicalDeviceSurfacePresentModesKHR(count)");
        return count.get(0) > 0;
    }

    private static VkDevice createDevice(MemoryStack stack, DeviceCandidate candidate) {
        int queueCount = candidate.queues.graphics == candidate.queues.present ? 1 : 2;
        VkDeviceQueueCreateInfo.Buffer queueInfos = VkDeviceQueueCreateInfo.calloc(queueCount, stack);
        queueInfos.get(0).sType$Default()
                .queueFamilyIndex(candidate.queues.graphics)
                .pQueuePriorities(stack.floats(1.0f));
        if (queueCount == 2) {
            queueInfos.get(1).sType$Default()
                    .queueFamilyIndex(candidate.queues.present)
                    .pQueuePriorities(stack.floats(1.0f));
        }
        VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.calloc(stack)
                .sType$Default()
                .pQueueCreateInfos(queueInfos)
                .ppEnabledExtensionNames(stack.pointers(
                        stack.UTF8(VK_KHR_SWAPCHAIN_EXTENSION_NAME)));
        PointerBuffer pointer = stack.mallocPointer(1);
        check(vkCreateDevice(candidate.device, createInfo, null, pointer), "vkCreateDevice");
        return new VkDevice(pointer.get(0), candidate.device, createInfo);
    }

    private static SwapchainResult createSwapchain(
            MemoryStack stack, DeviceCandidate candidate, VkDevice device, long surface,
            int requestedWidth, int requestedHeight, long oldSwapchain) {
        VkSurfaceCapabilitiesKHR capabilities = VkSurfaceCapabilitiesKHR.malloc(stack);
        check(vkGetPhysicalDeviceSurfaceCapabilitiesKHR(
                candidate.device, surface, capabilities),
                "vkGetPhysicalDeviceSurfaceCapabilitiesKHR");
        if ((capabilities.supportedUsageFlags() & VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT) == 0) {
            throw new IllegalStateException("Surface does not support color-attachment images");
        }
        VkSurfaceFormatKHR format = chooseSurfaceFormat(stack, candidate.device, surface);
        int presentMode = choosePresentMode(stack, candidate.device, surface);
        int width = capabilities.currentExtent().width();
        int height = capabilities.currentExtent().height();
        if (width == -1 || height == -1) {
            width = clamp(requestedWidth, capabilities.minImageExtent().width(),
                    capabilities.maxImageExtent().width());
            height = clamp(requestedHeight, capabilities.minImageExtent().height(),
                    capabilities.maxImageExtent().height());
        }
        int imageCount = capabilities.minImageCount() + 1;
        if (capabilities.maxImageCount() > 0) {
            imageCount = Math.min(imageCount, capabilities.maxImageCount());
        }
        VkSwapchainCreateInfoKHR createInfo = VkSwapchainCreateInfoKHR.calloc(stack)
                .sType$Default()
                .surface(surface)
                .minImageCount(imageCount)
                .imageFormat(format.format())
                .imageColorSpace(format.colorSpace())
                .imageArrayLayers(1)
                .imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)
                .preTransform(capabilities.currentTransform())
                .compositeAlpha(chooseCompositeAlpha(capabilities.supportedCompositeAlpha()))
                .presentMode(presentMode)
                .clipped(true)
                .oldSwapchain(oldSwapchain);
        createInfo.imageExtent().width(width).height(height);
        if (candidate.queues.graphics == candidate.queues.present) {
            createInfo.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE);
        } else {
            createInfo.imageSharingMode(VK_SHARING_MODE_CONCURRENT)
                    .pQueueFamilyIndices(stack.ints(
                            candidate.queues.graphics, candidate.queues.present));
        }
        LongBuffer pointer = stack.mallocLong(1);
        check(vkCreateSwapchainKHR(device, createInfo, null, pointer), "vkCreateSwapchainKHR");
        long swapchain = pointer.get(0);
        IntBuffer actualImages = stack.ints(0);
        check(vkGetSwapchainImagesKHR(device, swapchain, actualImages, null),
                "vkGetSwapchainImagesKHR");
        LongBuffer imageHandles = stack.mallocLong(actualImages.get(0));
        check(vkGetSwapchainImagesKHR(device, swapchain, actualImages, imageHandles),
                "vkGetSwapchainImagesKHR(list)");
        long[] images = new long[actualImages.get(0)];
        imageHandles.get(images);
        VulkanSurfaceInfo info = new VulkanSurfaceInfo(candidate.name, width, height,
                images.length, format.format(), format.colorSpace(), presentMode,
                candidate.queues.graphics, candidate.queues.present);
        return new SwapchainResult(swapchain, images, info);
    }

    private static VkSurfaceFormatKHR chooseSurfaceFormat(
            MemoryStack stack, VkPhysicalDevice device, long surface) {
        IntBuffer count = stack.ints(0);
        check(vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface, count, null),
                "vkGetPhysicalDeviceSurfaceFormatsKHR(count)");
        if (count.get(0) == 0) throw new IllegalStateException("Surface has no image formats");
        VkSurfaceFormatKHR.Buffer formats = VkSurfaceFormatKHR.malloc(count.get(0), stack);
        check(vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface, count, formats),
                "vkGetPhysicalDeviceSurfaceFormatsKHR(list)");
        for (int index = 0; index < count.get(0); index++) {
            VkSurfaceFormatKHR candidate = formats.get(index);
            if (candidate.format() == VK_FORMAT_B8G8R8A8_UNORM
                    && candidate.colorSpace() == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR) {
                return candidate;
            }
        }
        return formats.get(0);
    }

    private static int choosePresentMode(
            MemoryStack stack, VkPhysicalDevice device, long surface) {
        IntBuffer count = stack.ints(0);
        check(vkGetPhysicalDeviceSurfacePresentModesKHR(device, surface, count, null),
                "vkGetPhysicalDeviceSurfacePresentModesKHR(count)");
        IntBuffer modes = stack.mallocInt(count.get(0));
        check(vkGetPhysicalDeviceSurfacePresentModesKHR(device, surface, count, modes),
                "vkGetPhysicalDeviceSurfacePresentModesKHR(list)");
        for (int index = 0; index < count.get(0); index++) {
            if (modes.get(index) == VK_PRESENT_MODE_MAILBOX_KHR) {
                return VK_PRESENT_MODE_MAILBOX_KHR;
            }
        }
        return VK_PRESENT_MODE_FIFO_KHR;
    }

    private static int chooseCompositeAlpha(int supported) {
        int[] candidates = {
                VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR,
                VK_COMPOSITE_ALPHA_PRE_MULTIPLIED_BIT_KHR,
                VK_COMPOSITE_ALPHA_POST_MULTIPLIED_BIT_KHR,
                VK_COMPOSITE_ALPHA_INHERIT_BIT_KHR
        };
        for (int candidate : candidates) {
            if ((supported & candidate) != 0) return candidate;
        }
        throw new IllegalStateException("Surface has no supported composite-alpha mode");
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static void check(int result, String operation) {
        if (result != VK_SUCCESS) {
            throw new IllegalStateException(operation + " failed with VkResult " + result);
        }
    }

    private static final class QueueFamilies {
        private final int graphics;
        private final int present;

        private QueueFamilies(int graphics, int present) {
            this.graphics = graphics;
            this.present = present;
        }
    }

    private static final class DeviceCandidate {
        private final VkPhysicalDevice device;
        private final String name;
        private final int type;
        private final QueueFamilies queues;

        private DeviceCandidate(VkPhysicalDevice device, String name, int type,
                                QueueFamilies queues) {
            this.device = device;
            this.name = name;
            this.type = type;
            this.queues = queues;
        }
    }

    private static final class SwapchainResult {
        private final long handle;
        private final long[] images;
        private final VulkanSurfaceInfo info;

        private SwapchainResult(long handle, long[] images, VulkanSurfaceInfo info) {
            this.handle = handle;
            this.images = images;
            this.info = info;
        }
    }

    private static final class SurfaceSession {
        private final VkInstance instance;
        private final long surface;
        private final DeviceCandidate candidate;
        private final VkDevice device;
        private final VkQueue graphicsQueue;
        private final VkQueue presentQueue;
        private long swapchain;
        private long[] images;
        private VulkanSurfaceInfo info;
        private long[] imageViews = new long[0];
        private long renderPass;
        private long pipelineLayout;
        private long colorPipeline;
        private long[] framebuffers = new long[0];
        private long commandPool;
        private VkCommandBuffer[] commandBuffers = new VkCommandBuffer[0];
        private long imageAvailableSemaphore;
        private long renderFinishedSemaphore;
        private long inFlightFence;
        private long vertexBuffer;
        private long vertexMemory;
        private int vertexCapacity;
        private boolean closed;

        private SurfaceSession(VkInstance instance, long surface, DeviceCandidate candidate,
                               VkDevice device, SwapchainResult swapchain) {
            this.instance = instance;
            this.surface = surface;
            this.candidate = candidate;
            this.device = device;
            this.swapchain = swapchain.handle;
            this.images = swapchain.images;
            this.info = swapchain.info;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer queue = stack.mallocPointer(1);
                vkGetDeviceQueue(device, candidate.queues.graphics, 0, queue);
                graphicsQueue = new VkQueue(queue.get(0), device);
                vkGetDeviceQueue(device, candidate.queues.present, 0, queue);
                presentQueue = new VkQueue(queue.get(0), device);
            }
        }

        private void initialize() {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                createSwapchainResources(stack);
                VkSemaphoreCreateInfo semaphoreInfo = VkSemaphoreCreateInfo.calloc(stack)
                        .sType$Default();
                LongBuffer handle = stack.mallocLong(1);
                check(vkCreateSemaphore(device, semaphoreInfo, null, handle),
                        "vkCreateSemaphore(imageAvailable)");
                imageAvailableSemaphore = handle.get(0);
                check(vkCreateSemaphore(device, semaphoreInfo, null, handle),
                        "vkCreateSemaphore(renderFinished)");
                renderFinishedSemaphore = handle.get(0);
                VkFenceCreateInfo fenceInfo = VkFenceCreateInfo.calloc(stack)
                        .sType$Default().flags(VK_FENCE_CREATE_SIGNALED_BIT);
                check(vkCreateFence(device, fenceInfo, null, handle), "vkCreateFence");
                inFlightFence = handle.get(0);
            }
        }

        private void createSwapchainResources(MemoryStack stack) {
            imageViews = new long[images.length];
            LongBuffer handle = stack.mallocLong(1);
            for (int index = 0; index < images.length; index++) {
                VkImageViewCreateInfo viewInfo = VkImageViewCreateInfo.calloc(stack)
                        .sType$Default()
                        .image(images[index])
                        .viewType(VK_IMAGE_VIEW_TYPE_2D)
                        .format(info.imageFormat());
                viewInfo.components()
                        .r(VK_COMPONENT_SWIZZLE_IDENTITY)
                        .g(VK_COMPONENT_SWIZZLE_IDENTITY)
                        .b(VK_COMPONENT_SWIZZLE_IDENTITY)
                        .a(VK_COMPONENT_SWIZZLE_IDENTITY);
                viewInfo.subresourceRange()
                        .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                        .baseMipLevel(0).levelCount(1)
                        .baseArrayLayer(0).layerCount(1);
                check(vkCreateImageView(device, viewInfo, null, handle), "vkCreateImageView");
                imageViews[index] = handle.get(0);
            }

            VkAttachmentDescription.Buffer attachments = VkAttachmentDescription.calloc(1, stack);
            attachments.get(0)
                    .format(info.imageFormat())
                    .samples(VK_SAMPLE_COUNT_1_BIT)
                    .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                    .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
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
            dependency.get(0)
                    .srcSubpass(VK_SUBPASS_EXTERNAL).dstSubpass(0)
                    .srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                    .dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                    .srcAccessMask(0).dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);
            VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.calloc(stack)
                    .sType$Default().pAttachments(attachments)
                    .pSubpasses(subpass).pDependencies(dependency);
            check(vkCreateRenderPass(device, renderPassInfo, null, handle),
                    "vkCreateRenderPass");
            renderPass = handle.get(0);

            framebuffers = new long[imageViews.length];
            for (int index = 0; index < imageViews.length; index++) {
                VkFramebufferCreateInfo framebufferInfo = VkFramebufferCreateInfo.calloc(stack)
                        .sType$Default().renderPass(renderPass)
                        .pAttachments(stack.longs(imageViews[index]))
                        .width(info.width()).height(info.height()).layers(1);
                check(vkCreateFramebuffer(device, framebufferInfo, null, handle),
                        "vkCreateFramebuffer");
                framebuffers[index] = handle.get(0);
            }

            VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.calloc(stack)
                    .sType$Default()
                    .flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)
                    .queueFamilyIndex(candidate.queues.graphics);
            check(vkCreateCommandPool(device, poolInfo, null, handle), "vkCreateCommandPool");
            commandPool = handle.get(0);
            VkCommandBufferAllocateInfo allocateInfo = VkCommandBufferAllocateInfo.calloc(stack)
                    .sType$Default().commandPool(commandPool)
                    .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                    .commandBufferCount(images.length);
            PointerBuffer pointers = stack.mallocPointer(images.length);
            check(vkAllocateCommandBuffers(device, allocateInfo, pointers),
                    "vkAllocateCommandBuffers");
            commandBuffers = new VkCommandBuffer[images.length];
            for (int index = 0; index < images.length; index++) {
                commandBuffers[index] = new VkCommandBuffer(pointers.get(index), device);
            }
        }

        private void createColorPipeline(MemoryStack stack) {
            long vertexModule = VK_NULL_HANDLE;
            long fragmentModule = VK_NULL_HANDLE;
            try {
                vertexModule = createShaderModule(stack, COLOR_VERTEX_SHADER,
                        shaderc_glsl_vertex_shader, "colored-quad.vert");
                fragmentModule = createShaderModule(stack, COLOR_FRAGMENT_SHADER,
                        shaderc_glsl_fragment_shader, "colored-quad.frag");
                VkPipelineShaderStageCreateInfo.Buffer stages =
                        VkPipelineShaderStageCreateInfo.calloc(2, stack);
                stages.get(0).sType$Default().stage(VK_SHADER_STAGE_VERTEX_BIT)
                        .module(vertexModule).pName(stack.UTF8("main"));
                stages.get(1).sType$Default().stage(VK_SHADER_STAGE_FRAGMENT_BIT)
                        .module(fragmentModule).pName(stack.UTF8("main"));

                VkVertexInputBindingDescription.Buffer binding =
                        VkVertexInputBindingDescription.calloc(1, stack);
                binding.get(0).binding(0).stride(VERTEX_STRIDE)
                        .inputRate(VK_VERTEX_INPUT_RATE_VERTEX);
                VkVertexInputAttributeDescription.Buffer attributes =
                        VkVertexInputAttributeDescription.calloc(2, stack);
                attributes.get(0).location(0).binding(0)
                        .format(VK_FORMAT_R32G32_SFLOAT).offset(0);
                attributes.get(1).location(1).binding(0)
                        .format(VK_FORMAT_R32G32B32A32_SFLOAT).offset(2 * Float.BYTES);
                VkPipelineVertexInputStateCreateInfo vertexInput =
                        VkPipelineVertexInputStateCreateInfo.calloc(stack).sType$Default()
                                .pVertexBindingDescriptions(binding)
                                .pVertexAttributeDescriptions(attributes);
                VkPipelineInputAssemblyStateCreateInfo inputAssembly =
                        VkPipelineInputAssemblyStateCreateInfo.calloc(stack).sType$Default()
                                .topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)
                                .primitiveRestartEnable(false);
                VkPipelineViewportStateCreateInfo viewportState =
                        VkPipelineViewportStateCreateInfo.calloc(stack).sType$Default()
                                .viewportCount(1).scissorCount(1);
                VkPipelineRasterizationStateCreateInfo rasterizer =
                        VkPipelineRasterizationStateCreateInfo.calloc(stack).sType$Default()
                                .depthClampEnable(false).rasterizerDiscardEnable(false)
                                .polygonMode(VK_POLYGON_MODE_FILL).cullMode(VK_CULL_MODE_NONE)
                                .frontFace(VK_FRONT_FACE_COUNTER_CLOCKWISE)
                                .depthBiasEnable(false).lineWidth(1.0f);
                VkPipelineMultisampleStateCreateInfo multisampling =
                        VkPipelineMultisampleStateCreateInfo.calloc(stack).sType$Default()
                                .rasterizationSamples(VK_SAMPLE_COUNT_1_BIT)
                                .sampleShadingEnable(false);
                VkPipelineColorBlendAttachmentState.Buffer blendAttachment =
                        VkPipelineColorBlendAttachmentState.calloc(1, stack);
                blendAttachment.get(0)
                        .colorWriteMask(VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT
                                | VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT)
                        .blendEnable(true)
                        .srcColorBlendFactor(VK_BLEND_FACTOR_SRC_ALPHA)
                        .dstColorBlendFactor(VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA)
                        .colorBlendOp(VK_BLEND_OP_ADD)
                        .srcAlphaBlendFactor(VK_BLEND_FACTOR_ONE)
                        .dstAlphaBlendFactor(VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA)
                        .alphaBlendOp(VK_BLEND_OP_ADD);
                VkPipelineColorBlendStateCreateInfo colorBlending =
                        VkPipelineColorBlendStateCreateInfo.calloc(stack).sType$Default()
                                .logicOpEnable(false).pAttachments(blendAttachment);
                VkPipelineDynamicStateCreateInfo dynamicState =
                        VkPipelineDynamicStateCreateInfo.calloc(stack).sType$Default()
                                .pDynamicStates(stack.ints(
                                        VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR));
                VkPipelineLayoutCreateInfo layoutInfo =
                        VkPipelineLayoutCreateInfo.calloc(stack).sType$Default();
                LongBuffer handle = stack.mallocLong(1);
                check(vkCreatePipelineLayout(device, layoutInfo, null, handle),
                        "vkCreatePipelineLayout");
                pipelineLayout = handle.get(0);
                VkGraphicsPipelineCreateInfo.Buffer pipelineInfo =
                        VkGraphicsPipelineCreateInfo.calloc(1, stack);
                pipelineInfo.get(0).sType$Default().pStages(stages)
                        .pVertexInputState(vertexInput)
                        .pInputAssemblyState(inputAssembly)
                        .pViewportState(viewportState)
                        .pRasterizationState(rasterizer)
                        .pMultisampleState(multisampling)
                        .pColorBlendState(colorBlending)
                        .pDynamicState(dynamicState)
                        .layout(pipelineLayout).renderPass(renderPass).subpass(0);
                check(vkCreateGraphicsPipelines(device, VK_NULL_HANDLE,
                        pipelineInfo, null, handle), "vkCreateGraphicsPipelines(coloredQuad)");
                colorPipeline = handle.get(0);
            } finally {
                if (fragmentModule != VK_NULL_HANDLE) {
                    vkDestroyShaderModule(device, fragmentModule, null);
                }
                if (vertexModule != VK_NULL_HANDLE) {
                    vkDestroyShaderModule(device, vertexModule, null);
                }
            }
        }

        private long createShaderModule(MemoryStack stack, String source,
                                        int shaderKind, String name) {
            long compiler = shaderc_compiler_initialize();
            if (compiler == MemoryUtil.NULL) {
                throw new IllegalStateException("shaderc_compiler_initialize failed");
            }
            long result = MemoryUtil.NULL;
            try {
                result = shaderc_compile_into_spv(
                        compiler, source, shaderKind, name, "main", MemoryUtil.NULL);
                if (result == MemoryUtil.NULL) {
                    throw new IllegalStateException("shaderc_compile_into_spv returned null");
                }
                int status = shaderc_result_get_compilation_status(result);
                if (status != shaderc_compilation_status_success) {
                    throw new IllegalStateException("Could not compile " + name + ": "
                            + shaderc_result_get_error_message(result));
                }
                ByteBuffer code = shaderc_result_get_bytes(result);
                VkShaderModuleCreateInfo createInfo = VkShaderModuleCreateInfo.calloc(stack)
                        .sType$Default().pCode(code);
                LongBuffer handle = stack.mallocLong(1);
                check(vkCreateShaderModule(device, createInfo, null, handle),
                        "vkCreateShaderModule(" + name + ")");
                return handle.get(0);
            } finally {
                if (result != MemoryUtil.NULL) shaderc_result_release(result);
                shaderc_compiler_release(compiler);
            }
        }

        private VulkanSurfaceInfo presentFrame(VulkanFrameCommands frame) {
            if (closed) throw new IllegalStateException("Vulkan surface is closed");
            int width = frame.width();
            int height = frame.height();
            if (width > 0 && height > 0
                    && (width != info.width() || height != info.height())) {
                recreateSwapchain(width, height);
            }
            return presentFrame(frame, true);
        }

        private VulkanSurfaceInfo presentFrame(VulkanFrameCommands frame,
                                               boolean retryOutOfDate) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                if (!frame.coloredQuads().isEmpty() && colorPipeline == VK_NULL_HANDLE) {
                    createColorPipeline(stack);
                }
                check(vkWaitForFences(device, stack.longs(inFlightFence), true, Long.MAX_VALUE),
                        "vkWaitForFences");
                IntBuffer imageIndex = stack.mallocInt(1);
                int acquire = vkAcquireNextImageKHR(device, swapchain, Long.MAX_VALUE,
                        imageAvailableSemaphore, VK_NULL_HANDLE, imageIndex);
                if (acquire == VK_ERROR_OUT_OF_DATE_KHR && retryOutOfDate) {
                    recreateSwapchain(frame.width(), frame.height());
                    return presentFrame(frame, false);
                }
                if (acquire != VK_SUCCESS && acquire != VK_SUBOPTIMAL_KHR) {
                    check(acquire, "vkAcquireNextImageKHR");
                }
                check(vkResetFences(device, stack.longs(inFlightFence)), "vkResetFences");
                int index = imageIndex.get(0);
                int vertexCount = uploadColoredQuads(frame, stack);
                VkCommandBuffer commandBuffer = commandBuffers[index];
                check(vkResetCommandBuffer(commandBuffer, 0), "vkResetCommandBuffer");
                VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack)
                        .sType$Default().flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
                check(vkBeginCommandBuffer(commandBuffer, beginInfo), "vkBeginCommandBuffer");
                VkClearValue.Buffer clearValue = VkClearValue.calloc(1, stack);
                clearValue.get(0).color()
                        .float32(0, frame.clearRed()).float32(1, frame.clearGreen())
                        .float32(2, frame.clearBlue()).float32(3, frame.clearAlpha());
                VkRenderPassBeginInfo renderPassBegin = VkRenderPassBeginInfo.calloc(stack)
                        .sType$Default().renderPass(renderPass)
                        .framebuffer(framebuffers[index]).pClearValues(clearValue);
                renderPassBegin.renderArea().offset().x(0).y(0);
                renderPassBegin.renderArea().extent().width(info.width()).height(info.height());
                vkCmdBeginRenderPass(commandBuffer, renderPassBegin, VK_SUBPASS_CONTENTS_INLINE);
                if (vertexCount > 0) {
                    vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS,
                            colorPipeline);
                    VkViewport.Buffer viewport = VkViewport.calloc(1, stack);
                    viewport.get(0).x(0.0f).y(0.0f)
                            .width(info.width()).height(info.height())
                            .minDepth(0.0f).maxDepth(1.0f);
                    vkCmdSetViewport(commandBuffer, 0, viewport);
                    VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack);
                    scissor.get(0).offset().x(0).y(0);
                    scissor.get(0).extent().width(info.width()).height(info.height());
                    vkCmdSetScissor(commandBuffer, 0, scissor);
                    vkCmdBindVertexBuffers(commandBuffer, 0,
                            stack.longs(vertexBuffer), stack.longs(0L));
                    vkCmdDraw(commandBuffer, vertexCount, 1, 0, 0);
                }
                vkCmdEndRenderPass(commandBuffer);
                check(vkEndCommandBuffer(commandBuffer), "vkEndCommandBuffer");

                VkSubmitInfo submit = VkSubmitInfo.calloc(stack).sType$Default()
                        .pWaitSemaphores(stack.longs(imageAvailableSemaphore))
                        .pWaitDstStageMask(stack.ints(
                                VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT))
                        .pCommandBuffers(stack.pointers(commandBuffer.address()))
                        .pSignalSemaphores(stack.longs(renderFinishedSemaphore));
                check(vkQueueSubmit(graphicsQueue, submit, inFlightFence), "vkQueueSubmit");
                VkPresentInfoKHR present = VkPresentInfoKHR.calloc(stack).sType$Default()
                        .pWaitSemaphores(stack.longs(renderFinishedSemaphore))
                        .pSwapchains(stack.longs(swapchain)).pImageIndices(imageIndex);
                int presented = vkQueuePresentKHR(presentQueue, present);
                if ((presented == VK_ERROR_OUT_OF_DATE_KHR || presented == VK_SUBOPTIMAL_KHR)
                        && retryOutOfDate) {
                    recreateSwapchain(frame.width(), frame.height());
                } else if (presented != VK_SUCCESS) {
                    check(presented, "vkQueuePresentKHR");
                }
                return info;
            }
        }

        private int uploadColoredQuads(VulkanFrameCommands frame, MemoryStack stack) {
            int quadCount = frame.coloredQuads().size();
            if (quadCount == 0) return 0;
            int vertexCount = Math.multiplyExact(quadCount, 6);
            int byteCount = Math.multiplyExact(vertexCount, VERTEX_STRIDE);
            ensureVertexCapacity(byteCount, stack);
            PointerBuffer mapped = stack.mallocPointer(1);
            check(vkMapMemory(device, vertexMemory, 0, byteCount, 0, mapped), "vkMapMemory");
            try {
                FloatBuffer vertices = MemoryUtil.memByteBuffer(mapped.get(0), byteCount)
                        .asFloatBuffer();
                for (VulkanColoredQuad quad : frame.coloredQuads()) {
                    float left = pixelToNdcX(quad.x(), frame.width());
                    float right = pixelToNdcX(quad.x() + quad.width(), frame.width());
                    float top = pixelToNdcY(quad.y(), frame.height());
                    float bottom = pixelToNdcY(quad.y() + quad.height(), frame.height());
                    putVertex(vertices, left, top, quad);
                    putVertex(vertices, left, bottom, quad);
                    putVertex(vertices, right, bottom, quad);
                    putVertex(vertices, left, top, quad);
                    putVertex(vertices, right, bottom, quad);
                    putVertex(vertices, right, top, quad);
                }
            } finally {
                vkUnmapMemory(device, vertexMemory);
            }
            return vertexCount;
        }

        private void ensureVertexCapacity(int requiredBytes, MemoryStack stack) {
            if (vertexBuffer != 0L && vertexCapacity >= requiredBytes) return;
            if (vertexBuffer != 0L) vkDestroyBuffer(device, vertexBuffer, null);
            if (vertexMemory != 0L) vkFreeMemory(device, vertexMemory, null);
            vertexBuffer = 0L;
            vertexMemory = 0L;
            vertexCapacity = 64 * 1024;
            while (vertexCapacity < requiredBytes) {
                vertexCapacity = Math.multiplyExact(vertexCapacity, 2);
            }
            VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.calloc(stack).sType$Default()
                    .size(vertexCapacity).usage(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT)
                    .sharingMode(VK_SHARING_MODE_EXCLUSIVE);
            LongBuffer handle = stack.mallocLong(1);
            check(vkCreateBuffer(device, bufferInfo, null, handle), "vkCreateBuffer(vertex)");
            vertexBuffer = handle.get(0);
            VkMemoryRequirements requirements = VkMemoryRequirements.malloc(stack);
            vkGetBufferMemoryRequirements(device, vertexBuffer, requirements);
            VkMemoryAllocateInfo allocation = VkMemoryAllocateInfo.calloc(stack).sType$Default()
                    .allocationSize(requirements.size())
                    .memoryTypeIndex(findMemoryType(stack, requirements.memoryTypeBits(),
                            VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT
                                    | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT));
            check(vkAllocateMemory(device, allocation, null, handle),
                    "vkAllocateMemory(vertex)");
            vertexMemory = handle.get(0);
            check(vkBindBufferMemory(device, vertexBuffer, vertexMemory, 0),
                    "vkBindBufferMemory(vertex)");
        }

        private int findMemoryType(MemoryStack stack, int typeBits, int requiredFlags) {
            VkPhysicalDeviceMemoryProperties properties =
                    VkPhysicalDeviceMemoryProperties.malloc(stack);
            vkGetPhysicalDeviceMemoryProperties(candidate.device, properties);
            for (int index = 0; index < properties.memoryTypeCount(); index++) {
                int flags = properties.memoryTypes(index).propertyFlags();
                if ((typeBits & (1 << index)) != 0
                        && (flags & requiredFlags) == requiredFlags) return index;
            }
            throw new IllegalStateException("No host-visible coherent Vulkan memory type");
        }

        private static float pixelToNdcX(float value, int width) {
            return value * 2.0f / width - 1.0f;
        }

        private static float pixelToNdcY(float value, int height) {
            return 1.0f - value * 2.0f / height;
        }

        private static void putVertex(FloatBuffer output, float x, float y,
                                      VulkanColoredQuad quad) {
            output.put(x).put(y).put(quad.red()).put(quad.green())
                    .put(quad.blue()).put(quad.alpha());
        }

        private void recreateSwapchain(int width, int height) {
            check(vkDeviceWaitIdle(device), "vkDeviceWaitIdle(recreate)");
            try (MemoryStack stack = MemoryStack.stackPush()) {
                SwapchainResult replacement = createSwapchain(stack, candidate, device,
                        surface, Math.max(1, width), Math.max(1, height), swapchain);
                long previousSwapchain = swapchain;
                destroySwapchainResources();
                swapchain = replacement.handle;
                images = replacement.images;
                info = replacement.info;
                vkDestroySwapchainKHR(device, previousSwapchain, null);
                createSwapchainResources(stack);
            }
        }

        private void destroySwapchainResources() {
            if (commandPool != 0L) {
                vkDestroyCommandPool(device, commandPool, null);
                commandPool = 0L;
                commandBuffers = new VkCommandBuffer[0];
            }
            for (long framebuffer : framebuffers) {
                if (framebuffer != 0L) vkDestroyFramebuffer(device, framebuffer, null);
            }
            framebuffers = new long[0];
            if (colorPipeline != 0L) {
                vkDestroyPipeline(device, colorPipeline, null);
                colorPipeline = 0L;
            }
            if (pipelineLayout != 0L) {
                vkDestroyPipelineLayout(device, pipelineLayout, null);
                pipelineLayout = 0L;
            }
            if (renderPass != 0L) {
                vkDestroyRenderPass(device, renderPass, null);
                renderPass = 0L;
            }
            for (long imageView : imageViews) {
                if (imageView != 0L) vkDestroyImageView(device, imageView, null);
            }
            imageViews = new long[0];
        }

        private void close() {
            if (closed) return;
            closed = true;
            vkDeviceWaitIdle(device);
            if (inFlightFence != 0L) vkDestroyFence(device, inFlightFence, null);
            if (renderFinishedSemaphore != 0L) {
                vkDestroySemaphore(device, renderFinishedSemaphore, null);
            }
            if (imageAvailableSemaphore != 0L) {
                vkDestroySemaphore(device, imageAvailableSemaphore, null);
            }
            destroySwapchainResources();
            if (vertexBuffer != 0L) vkDestroyBuffer(device, vertexBuffer, null);
            if (vertexMemory != 0L) vkFreeMemory(device, vertexMemory, null);
            vkDestroySwapchainKHR(device, swapchain, null);
            vkDestroyDevice(device, null);
            vkDestroySurfaceKHR(instance, surface, null);
            vkDestroyInstance(instance, null);
        }
    }
}
