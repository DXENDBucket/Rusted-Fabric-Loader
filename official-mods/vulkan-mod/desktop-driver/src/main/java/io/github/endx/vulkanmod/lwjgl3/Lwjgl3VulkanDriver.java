package io.github.endx.vulkanmod.lwjgl3;

import io.github.endx.vulkanmod.spi.VulkanDeviceInfo;
import io.github.endx.vulkanmod.spi.VulkanPlatformDriver;
import io.github.endx.vulkanmod.spi.VulkanProbeResult;
import io.github.endx.vulkanmod.spi.VulkanSurfaceInfo;
import io.github.endx.vulkanmod.spi.VulkanSurfaceRequest;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK;
import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkDeviceCreateInfo;
import org.lwjgl.vulkan.VkDeviceQueueCreateInfo;
import org.lwjgl.vulkan.VkExtensionProperties;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;
import org.lwjgl.vulkan.VkQueueFamilyProperties;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR;
import org.lwjgl.vulkan.VkWin32SurfaceCreateInfoKHR;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.KHRWin32Surface.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK11.VK_API_VERSION_1_1;

/** LWJGL 3 desktop driver, loaded in a child-first class loader beside the LWJGL 2 game. */
public final class Lwjgl3VulkanDriver implements VulkanPlatformDriver {
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
                SwapchainResult swapchainResult = createSwapchain(
                        stack, candidate, device, surface, request);
                swapchain = swapchainResult.handle;
                created = new SurfaceSession(instance, surface, device, swapchain,
                        swapchainResult.info);
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
            VulkanSurfaceRequest request) {
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
            width = clamp(request.width(), capabilities.minImageExtent().width(),
                    capabilities.maxImageExtent().width());
            height = clamp(request.height(), capabilities.minImageExtent().height(),
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
                .oldSwapchain(VK_NULL_HANDLE);
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
        VulkanSurfaceInfo info = new VulkanSurfaceInfo(candidate.name, width, height,
                actualImages.get(0), format.format(), format.colorSpace(), presentMode,
                candidate.queues.graphics, candidate.queues.present);
        return new SwapchainResult(swapchain, info);
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
        private final VulkanSurfaceInfo info;

        private SwapchainResult(long handle, VulkanSurfaceInfo info) {
            this.handle = handle;
            this.info = info;
        }
    }

    private static final class SurfaceSession {
        private final VkInstance instance;
        private final long surface;
        private final VkDevice device;
        private final long swapchain;
        private final VulkanSurfaceInfo info;
        private boolean closed;

        private SurfaceSession(VkInstance instance, long surface, VkDevice device,
                               long swapchain, VulkanSurfaceInfo info) {
            this.instance = instance;
            this.surface = surface;
            this.device = device;
            this.swapchain = swapchain;
            this.info = info;
        }

        private void close() {
            if (closed) return;
            closed = true;
            vkDeviceWaitIdle(device);
            vkDestroySwapchainKHR(device, swapchain, null);
            vkDestroyDevice(device, null);
            vkDestroySurfaceKHR(instance, surface, null);
            vkDestroyInstance(instance, null);
        }
    }
}
