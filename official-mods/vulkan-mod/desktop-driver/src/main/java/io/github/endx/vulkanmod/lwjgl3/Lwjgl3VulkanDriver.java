package io.github.endx.vulkanmod.lwjgl3;

import io.github.endx.vulkanmod.spi.VulkanDeviceInfo;
import io.github.endx.vulkanmod.spi.VulkanBlendMode;
import io.github.endx.vulkanmod.spi.VulkanColoredQuad;
import io.github.endx.vulkanmod.spi.VulkanColoredTriangle;
import io.github.endx.vulkanmod.spi.VulkanClipRect;
import io.github.endx.vulkanmod.spi.VulkanDrawCommand;
import io.github.endx.vulkanmod.spi.VulkanDrawState;
import io.github.endx.vulkanmod.spi.VulkanFrameCommands;
import io.github.endx.vulkanmod.spi.VulkanTextureData;
import io.github.endx.vulkanmod.spi.VulkanTextureFilter;
import io.github.endx.vulkanmod.spi.VulkanTexturedQuad;
import io.github.endx.vulkanmod.spi.VulkanTexturedTriangle;
import io.github.endx.vulkanmod.spi.VulkanTransform2D;
import io.github.endx.vulkanmod.spi.VulkanPlatformDriver;
import io.github.endx.vulkanmod.spi.VulkanProbeResult;
import io.github.endx.vulkanmod.spi.VulkanSurfaceInfo;
import io.github.endx.vulkanmod.spi.VulkanSurfaceRequest;
import io.github.endx.vulkanmod.spi.VulkanWindowRequest;
import io.github.endx.vulkanmod.spi.VulkanInputEvent;
import io.github.endx.vulkanmod.spi.VulkanShaderState;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK;
import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkAttachmentDescription;
import org.lwjgl.vulkan.VkAttachmentReference;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkBufferImageCopy;
import org.lwjgl.vulkan.VkClearValue;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkDeviceCreateInfo;
import org.lwjgl.vulkan.VkDeviceQueueCreateInfo;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkDescriptorPoolCreateInfo;
import org.lwjgl.vulkan.VkDescriptorPoolSize;
import org.lwjgl.vulkan.VkDescriptorSetAllocateInfo;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo;
import org.lwjgl.vulkan.VkExtensionProperties;
import org.lwjgl.vulkan.VkFenceCreateInfo;
import org.lwjgl.vulkan.VkFramebufferCreateInfo;
import org.lwjgl.vulkan.VkImageViewCreateInfo;
import org.lwjgl.vulkan.VkImageCreateInfo;
import org.lwjgl.vulkan.VkImageMemoryBarrier;
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
import org.lwjgl.vulkan.VkPushConstantRange;
import org.lwjgl.vulkan.VkQueue;
import org.lwjgl.vulkan.VkQueueFamilyProperties;
import org.lwjgl.vulkan.VkRenderPassBeginInfo;
import org.lwjgl.vulkan.VkRenderPassCreateInfo;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;
import org.lwjgl.vulkan.VkSamplerCreateInfo;
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
import org.lwjgl.vulkan.VkWriteDescriptorSet;
import org.lwjgl.vulkan.VkWin32SurfaceCreateInfoKHR;
import org.lwjgl.system.windows.User32;
import org.lwjgl.system.windows.POINT;
import org.lwjgl.system.windows.RECT;
import org.lwjgl.system.windows.MSG;
import org.lwjgl.system.windows.WNDCLASSEX;
import org.lwjgl.system.windows.WindowProc;
import org.lwjgl.system.windows.WindowsLibrary;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    private static final int TEXTURED_VERTEX_FLOATS = 8;
    private static final int TEXTURED_VERTEX_STRIDE = TEXTURED_VERTEX_FLOATS * Float.BYTES;
    private static final int MAX_TEXTURES = 8192;
    private static final String COLOR_VERTEX_SHADER = "#version 450\n"
            + "layout(location=0) in vec2 inPosition;\n"
            + "layout(location=1) in vec4 inColor;\n"
            + "layout(location=0) out vec4 color;\n"
            + "void main(){ gl_Position=vec4(inPosition,0.0,1.0); color=inColor; }\n";
    private static final String COLOR_FRAGMENT_SHADER = "#version 450\n"
            + "layout(location=0) in vec4 color;\n"
            + "layout(location=0) out vec4 outColor;\n"
            + "void main(){ outColor=color; }\n";
    private static final String TEXTURE_VERTEX_SHADER = "#version 450\n"
            + "layout(location=0) in vec2 inPosition;\n"
            + "layout(location=1) in vec2 inUv;\n"
            + "layout(location=2) in vec4 inColor;\n"
            + "layout(location=0) out vec2 uv;\n"
            + "layout(location=1) out vec4 color;\n"
            + "void main(){ gl_Position=vec4(inPosition,0.0,1.0); uv=inUv; color=inColor; }\n";
    private static final String TEXTURE_FRAGMENT_SHADER = "#version 450\n"
            + "layout(set=0,binding=0) uniform sampler2D image;\n"
            + "layout(push_constant) uniform ShaderState {\n"
            + "  vec4 teamColor;\n"
            + "  float teamColorAmount;\n"
            + "  int effect;\n"
            + "} shaderState;\n"
            + "layout(location=0) in vec2 uv;\n"
            + "layout(location=1) in vec4 color;\n"
            + "layout(location=0) out vec4 outColor;\n"
            + "void main(){\n"
            + "  vec4 sampled=texture(image,uv);\n"
            + "  if(shaderState.effect==1){\n"
            + "    float threshold=0.04;\n"
            + "    if(sampled.g>0.0 && abs(sampled.r-sampled.b)<=threshold){\n"
            + "      float lightness=sampled.r;\n"
            + "      float greenness=sampled.g-lightness;\n"
            + "      sampled.rgb=vec3(lightness)+shaderState.teamColor.rgb*greenness;\n"
            + "    }\n"
            + "  }else if(shaderState.effect==2){\n"
            + "    sampled.rgb+=shaderState.teamColor.rgb*shaderState.teamColorAmount;\n"
            + "  }else if(shaderState.effect==3){\n"
            + "    float hueness=max(abs(sampled.r-sampled.g),\n"
            + "      max(abs(sampled.g-sampled.b),abs(sampled.b-sampled.r)));\n"
            + "    if(hueness>(15.0/256.0)){\n"
            + "      float lightness=min(sampled.r,min(sampled.g,sampled.b));\n"
            + "      sampled.rgb=vec3(lightness)+shaderState.teamColor.rgb*hueness;\n"
            + "    }\n"
            + "  }else if(shaderState.effect==4){\n"
            + "    sampled.a=1.0;\n"
            + "  }\n"
            + "  outColor=sampled*color;\n"
            + "}\n";
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

    @Override public synchronized VulkanSurfaceInfo createNativeWindowSurface(
            VulkanWindowRequest request) {
        if (surfaceSession != null) return surfaceSession.info;
        Win32NativeWindow window = Win32NativeWindow.create(request);
        boolean claimed = false;
        try {
            VulkanSurfaceInfo created = createSurfaceInternal(VulkanSurfaceRequest.win32(
                    window.handle, window.instance, request.width(), request.height()), true);
            surfaceSession.nativeWindow = window;
            claimed = true;
            window.show();
            return created;
        } finally {
            if (!claimed) window.close();
        }
    }

    @Override public synchronized VulkanSurfaceInfo createSurface(VulkanSurfaceRequest request) {
        return createSurfaceInternal(request, false);
    }

    private VulkanSurfaceInfo createSurfaceInternal(VulkanSurfaceRequest request,
                                                    boolean nativeWindow) {
        if (!"win32".equals(request.platform())) {
            throw new IllegalArgumentException("Unsupported Vulkan surface platform: "
                    + request.platform());
        }
        if (surfaceSession != null) return surfaceSession.info;
        SurfaceSession created = null;
        Win32OverlayWindow overlay = null;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int instanceVersion = VK.getInstanceVersionSupported();
            VkInstanceCreateInfo instanceInfo = instanceCreateInfo(stack, instanceVersion, true);
            VkInstance instance = createInstance(stack, instanceInfo);
            long surface = 0L;
            VkDevice device = null;
            long swapchain = 0L;
            try {
                VulkanSurfaceRequest surfaceRequest = request;
                if (request.childOverlay()) {
                    overlay = Win32OverlayWindow.create(request);
                    surfaceRequest = VulkanSurfaceRequest.win32(overlay.handle,
                            request.instanceHandle(), request.width(), request.height());
                }
                surface = createWin32Surface(stack, instance, surfaceRequest);
                DeviceCandidate candidate = selectDevice(stack, instance, surface);
                device = createDevice(stack, candidate);
                SwapchainResult swapchainResult = createSwapchain(stack, candidate, device,
                        surface, request.width(), request.height(), VK_NULL_HANDLE, nativeWindow);
                swapchain = swapchainResult.handle;
                created = new SurfaceSession(instance, surface, candidate, device,
                        swapchainResult, overlay, nativeWindow);
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
                    if (overlay != null) overlay.close();
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

    @Override public synchronized VulkanSurfaceInfo presentFrameAndReveal(
            VulkanFrameCommands frame) {
        if (surfaceSession == null) {
            throw new IllegalStateException("Vulkan surface has not been created");
        }
        if (frame == null) throw new NullPointerException("frame");
        return surfaceSession.presentFrameAndReveal(frame);
    }

    @Override public boolean setSurfaceVisible(boolean visible) {
        if (surfaceSession == null) return false;
        return surfaceSession.setVisible(visible);
    }

    @Override public boolean prepareSurfaceWindow(int width, int height, boolean visible) {
        SurfaceSession session = surfaceSession;
        return session != null && session.prepareWindow(width, height, visible);
    }

    @Override public void maintainSurfaceWindow() {
        SurfaceSession session = surfaceSession;
        if (session != null) session.maintainWindow();
    }

    @Override public boolean isSurfaceCloseRequested() {
        SurfaceSession session = surfaceSession;
        return session != null && session.isCloseRequested();
    }

    @Override public List<VulkanInputEvent> pollInputEvents() {
        SurfaceSession session = surfaceSession;
        return session == null ? Collections.emptyList() : session.pollInputEvents();
    }

    @Override public void setSystemCursorVisible(boolean visible) {
        SurfaceSession session = surfaceSession;
        if (session != null) session.setSystemCursorVisible(visible);
    }

    @Override public synchronized long uploadTexture(VulkanTextureData texture) {
        if (surfaceSession == null) {
            throw new IllegalStateException("Vulkan surface has not been created");
        }
        if (texture == null) throw new NullPointerException("texture");
        return surfaceSession.uploadTexture(texture);
    }

    @Override public synchronized void updateTexture(long textureHandle,
                                                     VulkanTextureData texture) {
        if (surfaceSession == null) {
            throw new IllegalStateException("Vulkan surface has not been created");
        }
        if (texture == null) throw new NullPointerException("texture");
        surfaceSession.updateTexture(textureHandle, texture);
    }

    @Override public synchronized long createRenderTarget(int width, int height) {
        if (surfaceSession == null) {
            throw new IllegalStateException("Vulkan surface has not been created");
        }
        return surfaceSession.createRenderTarget(width, height);
    }

    @Override public synchronized void renderToTexture(
            long textureHandle, VulkanFrameCommands frame) {
        if (surfaceSession == null) {
            throw new IllegalStateException("Vulkan surface has not been created");
        }
        if (frame == null) throw new NullPointerException("frame");
        surfaceSession.renderToTexture(textureHandle, frame);
    }

    @Override public synchronized void destroyTexture(long textureHandle) {
        if (surfaceSession != null) surfaceSession.destroyTexture(textureHandle);
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
            int requestedWidth, int requestedHeight, long oldSwapchain,
            boolean nativeWindow) {
        VkSurfaceCapabilitiesKHR capabilities = VkSurfaceCapabilitiesKHR.malloc(stack);
        check(vkGetPhysicalDeviceSurfaceCapabilitiesKHR(
                candidate.device, surface, capabilities),
                "vkGetPhysicalDeviceSurfaceCapabilitiesKHR");
        if ((capabilities.supportedUsageFlags() & VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT) == 0) {
            throw new IllegalStateException("Surface does not support color-attachment images");
        }
        VkSurfaceFormatKHR format = chooseSurfaceFormat(stack, candidate.device, surface);
        int presentMode = choosePresentMode(stack, candidate.device, surface, nativeWindow);
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
            MemoryStack stack, VkPhysicalDevice device, long surface,
            boolean nativeWindow) {
        IntBuffer count = stack.ints(0);
        check(vkGetPhysicalDeviceSurfacePresentModesKHR(device, surface, count, null),
                "vkGetPhysicalDeviceSurfacePresentModesKHR(count)");
        IntBuffer modes = stack.mallocInt(count.get(0));
        check(vkGetPhysicalDeviceSurfacePresentModesKHR(device, surface, count, modes),
                "vkGetPhysicalDeviceSurfacePresentModesKHR(list)");
        String requested = System.getProperty("rusted.fabric.vulkan.presentMode",
                nativeWindow ? "immediate" : "fifo").trim();
        int[] preferences;
        if ("fifo".equalsIgnoreCase(requested) || "vsync".equalsIgnoreCase(requested)) {
            preferences = new int[] { VK_PRESENT_MODE_FIFO_KHR };
        } else if ("mailbox".equalsIgnoreCase(requested)) {
            preferences = new int[] { VK_PRESENT_MODE_MAILBOX_KHR,
                    VK_PRESENT_MODE_FIFO_KHR };
        } else if ("immediate".equalsIgnoreCase(requested)
                || "uncapped".equalsIgnoreCase(requested)) {
            preferences = new int[] { VK_PRESENT_MODE_IMMEDIATE_KHR,
                    VK_PRESENT_MODE_MAILBOX_KHR, VK_PRESENT_MODE_FIFO_KHR };
        } else if ("auto".equalsIgnoreCase(requested)) {
            preferences = nativeWindow
                    ? new int[] { VK_PRESENT_MODE_IMMEDIATE_KHR,
                            VK_PRESENT_MODE_MAILBOX_KHR, VK_PRESENT_MODE_FIFO_KHR }
                    : new int[] { VK_PRESENT_MODE_FIFO_KHR };
        } else {
            System.out.println("[Vulkan Mod/Driver] Unknown present mode '" + requested
                    + "'; using " + (nativeWindow ? "immediate" : "fifo") + " policy");
            preferences = nativeWindow
                    ? new int[] { VK_PRESENT_MODE_IMMEDIATE_KHR,
                            VK_PRESENT_MODE_MAILBOX_KHR, VK_PRESENT_MODE_FIFO_KHR }
                    : new int[] { VK_PRESENT_MODE_FIFO_KHR };
        }
        for (int preference : preferences) {
            for (int index = 0; index < count.get(0); index++) {
                if (modes.get(index) == preference) {
                    System.out.println("[Vulkan Mod/Driver] Present mode: "
                            + presentModeName(preference) + " (requested=" + requested + ")");
                    return preference;
                }
            }
        }
        // FIFO support is guaranteed by Vulkan, but retain an explicit defensive fallback.
        return VK_PRESENT_MODE_FIFO_KHR;
    }

    private static String presentModeName(int mode) {
        if (mode == VK_PRESENT_MODE_IMMEDIATE_KHR) return "IMMEDIATE";
        if (mode == VK_PRESENT_MODE_MAILBOX_KHR) return "MAILBOX";
        if (mode == VK_PRESENT_MODE_FIFO_RELAXED_KHR) return "FIFO_RELAXED";
        if (mode == VK_PRESENT_MODE_FIFO_KHR) return "FIFO";
        return Integer.toString(mode);
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

    /**
     * A Vulkan-owned, non-activating owned window above Slick's WGL client area. Win32 does not
     * reliably expose Vulkan presentation on an HWND that already owns a WGL swap chain, and WGL
     * can also overdraw ordinary child windows. Hit testing falls through to the original game.
     */
    private static final class Win32OverlayWindow {
        private static final String CLASS_NAME = "RustedFabricVulkanOverlay";
        private static final WindowProc WINDOW_PROC = WindowProc.create(
                (window, message, wParam, lParam) -> message == User32.WM_NCHITTEST
                        ? User32.HTTRANSPARENT
                        : User32.DefWindowProc(window, message, wParam, lParam));
        private static long registeredInstance;

        private final long handle;
        private final long parentHandle;
        private final boolean detached;
        private volatile int width;
        private volatile int height;
        private volatile int x;
        private volatile int y;
        private volatile boolean visibleRequested;
        private volatile boolean presentable;
        private volatile boolean closed;
        private int lastParentLeft;
        private int lastParentTop;
        private int lastParentRight;
        private int lastParentBottom;
        private boolean hasLastParentBounds;
        private boolean parentWasMinimized;
        private long restoreParkingStartedNanos;

        private Win32OverlayWindow(long handle, long parentHandle, boolean detached,
                                   int width, int height, int x, int y) {
            this.handle = handle;
            this.parentHandle = parentHandle;
            this.detached = detached;
            this.width = width;
            this.height = height;
            this.x = x;
            this.y = y;
        }

        private static synchronized Win32OverlayWindow create(VulkanSurfaceRequest request) {
            long instance = request.instanceHandle();
            if (registeredInstance == 0L) {
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    WNDCLASSEX windowClass = WNDCLASSEX.calloc(stack)
                            .cbSize(WNDCLASSEX.SIZEOF)
                            .style(0)
                            .lpfnWndProc(WINDOW_PROC)
                            .hInstance(instance)
                            .lpszClassName(stack.UTF16(CLASS_NAME));
                    short atom = User32.RegisterClassEx(null, windowClass);
                    if (atom == 0) {
                        throw new IllegalStateException(
                                "RegisterClassEx(Vulkan overlay) failed");
                    }
                    registeredInstance = instance;
                }
            } else if (registeredInstance != instance) {
                throw new IllegalStateException("Vulkan overlay HINSTANCE changed");
            }
            // The production surface is a real child of the LWJGL window. Keeping it out of the
            // top-level owned-popup list prevents Windows from independently classifying the
            // presentation surface as hung while Slick owns the thread's message loop. The
            // detached popup remains available solely for WSI diagnostics.
            boolean detached = Boolean.getBoolean(
                    "rusted.fabric.vulkan.debugDetachedOverlay");
            int extendedStyle = detached
                    ? 0
                    : User32.WS_EX_NOACTIVATE;
            int style = detached ? User32.WS_POPUP : User32.WS_CHILD;
            int x;
            int y;
            if (detached) try (MemoryStack stack = MemoryStack.stackPush()) {
                POINT origin = POINT.calloc(stack).set(0, 0);
                if (!User32.ClientToScreen(request.windowHandle(), origin)) {
                    throw new IllegalStateException(
                            "ClientToScreen(Vulkan overlay) failed");
                }
                x = origin.x();
                y = origin.y();
            } else {
                x = 0;
                y = 0;
            }
            String title = detached ? "Rusted Fabric Vulkan Diagnostic" : "";
            long window = User32.CreateWindowEx(null, extendedStyle, CLASS_NAME, title,
                    style, x, y, request.width(), request.height(),
                    detached ? 0L : request.windowHandle(),
                    0L, instance, 0L);
            if (window == 0L) {
                throw new IllegalStateException("CreateWindowEx(Vulkan overlay) failed");
            }
            User32.SetWindowPos(null, window, User32.HWND_TOP, x, y,
                    request.width(), request.height(),
                    (detached ? 0 : User32.SWP_NOACTIVATE));
            User32.ShowWindow(window, User32.SW_HIDE);
            if (detached) {
                System.out.println("[Vulkan Mod/Driver] Using detached Win32 overlay diagnostic");
            }
            Win32OverlayWindow result = new Win32OverlayWindow(window, request.windowHandle(),
                    detached, request.width(), request.height(), x, y);
            result.pumpMessages();
            return result;
        }

        private boolean resize(int requestedWidth, int requestedHeight,
                               boolean allowHiddenPreparation) {
            int nextWidth = Math.max(1, requestedWidth);
            int nextHeight = Math.max(1, requestedHeight);
            if (closed) return false;
            pumpMessages();
            if ((!visibleRequested && !allowHiddenPreparation) || !isParentPresentable()) {
                User32.ShowWindow(handle, User32.SW_HIDE);
                presentable = false;
                return false;
            }
            int nextX = 0;
            int nextY = 0;
            if (detached) try (MemoryStack stack = MemoryStack.stackPush()) {
                POINT origin = POINT.calloc(stack).set(0, 0);
                if (!User32.ClientToScreen(parentHandle, origin)) return false;
                nextX = origin.x();
                nextY = origin.y();
            }
            if (visibleRequested) {
                User32.ShowWindow(handle,
                        detached ? User32.SW_SHOW : User32.SW_SHOWNOACTIVATE);
            }
            // Slick swaps its WGL surface immediately before this call. Reassert the popup order
            // even when its rectangle did not change so that Vulkan remains above the owner.
            if (!User32.SetWindowPos(null, handle, User32.HWND_TOP, nextX, nextY,
                    nextWidth, nextHeight,
                    (detached ? 0 : User32.SWP_NOACTIVATE)
                            | (visibleRequested ? User32.SWP_SHOWWINDOW : 0))) {
                throw new IllegalStateException("SetWindowPos(Vulkan overlay) failed");
            }
            width = nextWidth;
            height = nextHeight;
            x = nextX;
            y = nextY;
            presentable = true;
            return true;
        }

        private boolean isPreparedFor(int requestedWidth, int requestedHeight) {
            return !closed && presentable
                    && width == Math.max(1, requestedWidth)
                    && height == Math.max(1, requestedHeight);
        }

        private boolean isParentPresentable() {
            if (!User32.IsWindowVisible(parentHandle)) {
                return false;
            }
            try (MemoryStack stack = MemoryStack.stackPush()) {
                RECT bounds = RECT.calloc(stack);
                if (!User32.GetWindowRect(null, parentHandle, bounds)) return false;
                boolean parked = isParked(bounds);
                if (User32.IsIconic(parentHandle)) {
                    parentWasMinimized = true;
                    restoreParkingStartedNanos = 0L;
                    return false;
                }
                if (parked) return recoverParentFromParking();
                rememberParentBounds(bounds);
                parentWasMinimized = false;
                restoreParkingStartedNanos = 0L;
                return true;
            }
        }

        private void maintainParentWindow() {
            if (!closed) isParentPresentable();
        }

        private static boolean isParked(RECT bounds) {
            return bounds.left() <= -10_000 || bounds.top() <= -10_000
                    || bounds.right() <= bounds.left() || bounds.bottom() <= bounds.top();
        }

        private void rememberParentBounds(RECT bounds) {
            lastParentLeft = bounds.left();
            lastParentTop = bounds.top();
            lastParentRight = bounds.right();
            lastParentBottom = bounds.bottom();
            hasLastParentBounds = true;
        }

        private boolean recoverParentFromParking() {
            if (!parentWasMinimized || !hasLastParentBounds) return false;
            long now = System.nanoTime();
            if (restoreParkingStartedNanos == 0L) {
                restoreParkingStartedNanos = now;
                return false;
            }
            if (now - restoreParkingStartedNanos < 300_000_000L) return false;
            int restoredWidth = Math.max(1, lastParentRight - lastParentLeft);
            int restoredHeight = Math.max(1, lastParentBottom - lastParentTop);
            boolean restored = User32.SetWindowPos(null, parentHandle, 0L,
                    lastParentLeft, lastParentTop, restoredWidth, restoredHeight,
                    User32.SWP_NOACTIVATE | User32.SWP_NOZORDER);
            if (restored) {
                User32.ShowWindow(parentHandle, User32.SW_SHOWNOACTIVATE);
                restoreParkingStartedNanos = 0L;
                System.out.println("[Vulkan Mod/Driver] Recovered LWJGL parent window from "
                        + "stale minimized parking coordinates");
            }
            return restored;
        }

        private boolean setVisible(boolean visible) {
            if (closed) return false;
            visibleRequested = visible;
            if (!visible) {
                User32.ShowWindow(handle, User32.SW_HIDE);
                pumpMessages();
                return true;
            }
            if (!resize(width, height, false)) return false;
            User32.UpdateWindow(handle);
            return User32.IsWindowVisible(handle);
        }

        private void pumpMessages() {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                MSG message = MSG.calloc(stack);
                while (User32.PeekMessage(message, handle, 0, 0, User32.PM_REMOVE)) {
                    User32.TranslateMessage(message);
                    User32.DispatchMessage(message);
                }
            }
        }

        private void close() {
            if (closed) return;
            closed = true;
            User32.DestroyWindow(null, handle);
        }
    }

    /** Top-level application window used when Vulkan owns the display from process startup. */
    private static final class Win32NativeWindow {
        private static final String CLASS_NAME = "RustedFabricVulkanWindow";
        private static volatile boolean closeRequested;
        private static volatile Win32NativeWindow activeWindow;
        private static final WindowProc WINDOW_PROC = WindowProc.create(
                (window, message, wParam, lParam) -> {
                    Win32NativeWindow active = activeWindow;
                    if (active != null && active.handle == window
                            && active.handleMessage(message, wParam, lParam)) {
                        return 0L;
                    }
                    if (message == User32.WM_CLOSE) {
                        closeRequested = true;
                        return 0L;
                    }
                    return User32.DefWindowProc(window, message, wParam, lParam);
                });
        private static boolean registered;
        private static boolean dpiAwarenessConfigured;

        private final long handle;
        private final long instance;
        private volatile int width;
        private volatile int height;
        private volatile boolean minimized;
        private volatile boolean systemCursorVisible = true;
        private volatile boolean closed;
        private final ArrayDeque<VulkanInputEvent> inputEvents =
                new ArrayDeque<VulkanInputEvent>();
        private int pointerX;
        private int pointerY;

        private Win32NativeWindow(long handle, long instance, int width, int height) {
            this.handle = handle;
            this.instance = instance;
            this.width = width;
            this.height = height;
        }

        private static synchronized Win32NativeWindow create(VulkanWindowRequest request) {
            closeRequested = false;
            configureDpiAwareness();
            long instance = WindowsLibrary.HINSTANCE;
            if (!registered) {
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    WNDCLASSEX windowClass = WNDCLASSEX.calloc(stack)
                            .cbSize(WNDCLASSEX.SIZEOF)
                            .style(User32.CS_OWNDC)
                            .lpfnWndProc(WINDOW_PROC)
                            .hInstance(instance)
                            .lpszClassName(stack.UTF16(CLASS_NAME));
                    short atom = User32.RegisterClassEx(null, windowClass);
                    if (atom == 0) {
                        throw new IllegalStateException(
                                "RegisterClassEx(Vulkan native window) failed");
                    }
                    registered = true;
                }
            }
            int style = User32.WS_OVERLAPPEDWINDOW;
            if (!request.resizable()) {
                style &= ~(User32.WS_THICKFRAME | User32.WS_MAXIMIZEBOX);
            }
            int outerWidth = request.width();
            int outerHeight = request.height();
            try (MemoryStack stack = MemoryStack.stackPush()) {
                RECT bounds = RECT.calloc(stack).set(0, 0, request.width(), request.height());
                if (User32.AdjustWindowRectEx(null, bounds, style, false, 0)) {
                    outerWidth = bounds.right() - bounds.left();
                    outerHeight = bounds.bottom() - bounds.top();
                }
            }
            long window = User32.CreateWindowEx(null, 0, CLASS_NAME, request.title(), style,
                    User32.CW_USEDEFAULT, User32.CW_USEDEFAULT, outerWidth, outerHeight,
                    0L, 0L, instance, 0L);
            if (window == 0L) {
                throw new IllegalStateException("CreateWindowEx(Vulkan native window) failed");
            }
            Win32NativeWindow result = new Win32NativeWindow(window, instance,
                    request.width(), request.height());
            activeWindow = result;
            return result;
        }

        private static void configureDpiAwareness() {
            if (dpiAwarenessConfigured) return;
            dpiAwarenessConfigured = true;
            long previous = User32.SetThreadDpiAwarenessContext(
                    User32.DPI_AWARENESS_CONTEXT_PER_MONITOR_AWARE_V2);
            if (previous == 0L) {
                previous = User32.SetThreadDpiAwarenessContext(
                        User32.DPI_AWARENESS_CONTEXT_PER_MONITOR_AWARE);
            }
            if (previous == 0L) {
                System.out.println("[Vulkan Mod/Driver] Win32 per-monitor DPI awareness "
                        + "is unavailable; window coordinates may be virtualized");
            } else {
                System.out.println("[Vulkan Mod/Driver] Win32 Vulkan window uses "
                        + "per-monitor DPI-aware pixel coordinates");
            }
        }

        private boolean handleMessage(int message, long wParam, long lParam) {
            switch (message) {
                case 0x0005: { // WM_SIZE
                    int nextWidth = unsignedLowWord(lParam);
                    int nextHeight = unsignedHighWord(lParam);
                    minimized = wParam == 1L || nextWidth == 0 || nextHeight == 0;
                    if (!minimized) {
                        width = nextWidth;
                        height = nextHeight;
                    }
                    // Let DefWindowProc perform the ordinary non-client/window bookkeeping.
                    return false;
                }
                case 0x0020: // WM_SETCURSOR
                    if (!systemCursorVisible && unsignedLowWord(lParam) == 1) { // HTCLIENT
                        User32.SetCursor(0L);
                        return true;
                    }
                    return false;
                case 0x0200: // WM_MOUSEMOVE
                    updatePointer(lParam);
                    enqueue(VulkanInputEvent.pointer(
                            VulkanInputEvent.Type.POINTER_MOVE, pointerX, pointerY, -1));
                    return true;
                case 0x0201: // WM_LBUTTONDOWN
                    return enqueueButton(lParam, 0, true);
                case 0x0202: // WM_LBUTTONUP
                    return enqueueButton(lParam, 0, false);
                case 0x0204: // WM_RBUTTONDOWN
                    return enqueueButton(lParam, 1, true);
                case 0x0205: // WM_RBUTTONUP
                    return enqueueButton(lParam, 1, false);
                case 0x0207: // WM_MBUTTONDOWN
                    return enqueueButton(lParam, 2, true);
                case 0x0208: // WM_MBUTTONUP
                    return enqueueButton(lParam, 2, false);
                case 0x020A: // WM_MOUSEWHEEL (coordinates are screen-relative)
                    // Keep the last client-space pointer position. WM_MOUSEWHEEL carries screen
                    // coordinates, while the game only needs the wheel delta here.
                    enqueue(VulkanInputEvent.wheel(pointerX, pointerY,
                            signedHighWord(wParam)));
                    return true;
                case 0x0100: // WM_KEYDOWN
                case 0x0104: // WM_SYSKEYDOWN
                    enqueue(VulkanInputEvent.key(VulkanInputEvent.Type.KEY_DOWN,
                            (int) wParam, Math.max(1, (int) (lParam & 0xffffL))));
                    return true;
                case 0x0101: // WM_KEYUP
                case 0x0105: // WM_SYSKEYUP
                    enqueue(VulkanInputEvent.key(VulkanInputEvent.Type.KEY_UP,
                            (int) wParam, 0));
                    return true;
                case 0x0102: // WM_CHAR
                    enqueue(VulkanInputEvent.character((char) wParam));
                    return true;
                case 0x0008: // WM_KILLFOCUS
                    enqueue(VulkanInputEvent.focusLost());
                    return false;
                default:
                    return false;
            }
        }

        private boolean enqueueButton(long lParam, int button, boolean down) {
            updatePointer(lParam);
            enqueue(VulkanInputEvent.pointer(down ? VulkanInputEvent.Type.BUTTON_DOWN
                    : VulkanInputEvent.Type.BUTTON_UP, pointerX, pointerY, button));
            return true;
        }

        private void updatePointer(long lParam) {
            pointerX = signedLowWord(lParam);
            pointerY = signedHighWord(lParam);
        }

        private synchronized void enqueue(VulkanInputEvent event) {
            // Coalesce consecutive motion messages so a stalled render frame cannot accumulate
            // an unbounded queue while retaining every button/key transition in order.
            if (event.type() == VulkanInputEvent.Type.POINTER_MOVE
                    && !inputEvents.isEmpty()
                    && inputEvents.peekLast().type() == VulkanInputEvent.Type.POINTER_MOVE) {
                inputEvents.removeLast();
            }
            inputEvents.addLast(event);
        }

        private void setSystemCursorVisible(boolean visible) {
            systemCursorVisible = visible;
            if (!visible) User32.SetCursor(0L);
        }

        private synchronized List<VulkanInputEvent> drainInputEvents() {
            if (inputEvents.isEmpty()) return Collections.emptyList();
            ArrayList<VulkanInputEvent> drained =
                    new ArrayList<VulkanInputEvent>(inputEvents);
            inputEvents.clear();
            return drained;
        }

        private static int signedLowWord(long value) { return (short) (value & 0xffffL); }
        private static int signedHighWord(long value) {
            return (short) ((value >>> 16) & 0xffffL);
        }
        private static int unsignedLowWord(long value) { return (int) (value & 0xffffL); }
        private static int unsignedHighWord(long value) {
            return (int) ((value >>> 16) & 0xffffL);
        }

        private void show() {
            if (closed) return;
            User32.ShowWindow(handle, User32.SW_SHOW);
            User32.UpdateWindow(handle);
            pumpMessages();
        }

        private boolean setVisible(boolean visible) {
            if (closed) return false;
            User32.ShowWindow(handle, visible ? User32.SW_SHOW : User32.SW_HIDE);
            if (visible) User32.UpdateWindow(handle);
            pumpMessages();
            return visible ? User32.IsWindowVisible(handle) : !User32.IsWindowVisible(handle);
        }

        private boolean isPreparedFor(int requestedWidth, int requestedHeight) {
            return !closed && !minimized && !User32.IsIconic(handle);
        }

        private int clientWidth() { return Math.max(1, width); }
        private int clientHeight() { return Math.max(1, height); }
        private boolean isMinimized() {
            return closed || minimized || User32.IsIconic(handle);
        }

        private boolean prepare(int requestedWidth, int requestedHeight, boolean visible) {
            if (closed) return false;
            pumpMessages();
            if (visible && !User32.IsWindowVisible(handle)) show();
            return !isMinimized();
        }

        private void pumpMessages() {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                MSG message = MSG.calloc(stack);
                while (User32.PeekMessage(message, handle, 0, 0, User32.PM_REMOVE)) {
                    User32.TranslateMessage(message);
                    User32.DispatchMessage(message);
                }
            }
        }

        private boolean isCloseRequested() {
            pumpMessages();
            return closeRequested;
        }

        private void close() {
            if (closed) return;
            closed = true;
            if (activeWindow == this) activeWindow = null;
            User32.DestroyWindow(null, handle);
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
        private final Win32OverlayWindow overlay;
        private final boolean nativeWindowMode;
        private Win32NativeWindow nativeWindow;
        private long swapchain;
        private long[] images;
        private VulkanSurfaceInfo info;
        private long[] imageViews = new long[0];
        private long renderPass;
        private long offscreenClearRenderPass;
        private long offscreenLoadRenderPass;
        private long pipelineLayout;
        private final long[] colorPipelines =
                new long[VulkanBlendMode.values().length];
        private long texturePipelineLayout;
        private final long[] texturePipelines =
                new long[VulkanBlendMode.values().length];
        private long textureDescriptorSetLayout;
        private long textureDescriptorPool;
        private final Map<Long, TextureResource> textures =
                new LinkedHashMap<Long, TextureResource>();
        private final Map<Long, PendingTextureUpload> pendingTextureUploads =
                new LinkedHashMap<Long, PendingTextureUpload>();
        private final ArrayDeque<TextureResource> retiredTextures =
                new ArrayDeque<TextureResource>();
        private long nextTextureHandle = 1L;
        private long[] framebuffers = new long[0];
        private long commandPool;
        private VkCommandBuffer[] commandBuffers = new VkCommandBuffer[0];
        private long[] imageAvailableSemaphores = new long[0];
        private long[] renderFinishedSemaphores = new long[0];
        private long[] inFlightFences = new long[0];
        private BufferAllocation[] vertexAllocations = new BufferAllocation[0];
        private int[] vertexCapacities = new int[0];
        private int frameCursor;
        private BufferAllocation textureStaging;
        private int textureStagingCapacity;
        private long acquireSkips;
        private long fenceWaitSkips;
        private long successfulPresents;
        private long profiledSlowFrames;
        private long renderTargetSubmissions;
        private boolean debugLargeTargetReadBack;
        private boolean debugMainTargetSamplesLogged;
        private boolean closed;

        private SurfaceSession(VkInstance instance, long surface, DeviceCandidate candidate,
                               VkDevice device, SwapchainResult swapchain,
                               Win32OverlayWindow overlay, boolean nativeWindowMode) {
            this.instance = instance;
            this.surface = surface;
            this.candidate = candidate;
            this.device = device;
            this.swapchain = swapchain.handle;
            this.images = swapchain.images;
            this.info = swapchain.info;
            this.overlay = overlay;
            this.nativeWindowMode = nativeWindowMode;
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
                offscreenClearRenderPass = createOffscreenRenderPass(stack, false);
                offscreenLoadRenderPass = createOffscreenRenderPass(stack, true);
            }
        }

        private long createOffscreenRenderPass(MemoryStack stack, boolean preserveContents) {
            VkAttachmentDescription.Buffer attachments = VkAttachmentDescription.calloc(1, stack);
            attachments.get(0)
                    .format(info.imageFormat())
                    .samples(VK_SAMPLE_COUNT_1_BIT)
                    .loadOp(preserveContents
                            ? VK_ATTACHMENT_LOAD_OP_LOAD : VK_ATTACHMENT_LOAD_OP_CLEAR)
                    .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                    .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                    .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                    .initialLayout(preserveContents
                            ? VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL
                            : VK_IMAGE_LAYOUT_UNDEFINED)
                    .finalLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
            VkAttachmentReference.Buffer colorReference = VkAttachmentReference.calloc(1, stack);
            colorReference.get(0).attachment(0).layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
            VkSubpassDescription.Buffer subpass = VkSubpassDescription.calloc(1, stack);
            subpass.get(0).pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
                    .colorAttachmentCount(1).pColorAttachments(colorReference);
            VkSubpassDependency.Buffer dependencies = VkSubpassDependency.calloc(2, stack);
            dependencies.get(0)
                    .srcSubpass(VK_SUBPASS_EXTERNAL).dstSubpass(0)
                    .srcStageMask(VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT
                            | VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT)
                    .dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                    .srcAccessMask(VK_ACCESS_SHADER_READ_BIT)
                    .dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);
            dependencies.get(1)
                    .srcSubpass(0).dstSubpass(VK_SUBPASS_EXTERNAL)
                    .srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                    .dstStageMask(VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT)
                    .srcAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
                    .dstAccessMask(VK_ACCESS_SHADER_READ_BIT);
            VkRenderPassCreateInfo createInfo = VkRenderPassCreateInfo.calloc(stack)
                    .sType$Default().pAttachments(attachments)
                    .pSubpasses(subpass).pDependencies(dependencies);
            LongBuffer handle = stack.mallocLong(1);
            check(vkCreateRenderPass(device, createInfo, null, handle),
                    preserveContents ? "vkCreateRenderPass(offscreen load)"
                            : "vkCreateRenderPass(offscreen clear)");
            return handle.get(0);
        }

        private void createSwapchainResources(MemoryStack stack) {
            imageViews = new long[images.length];
            LongBuffer handle = stack.mallocLong(1);
            VkSemaphoreCreateInfo semaphoreInfo = VkSemaphoreCreateInfo.calloc(stack)
                    .sType$Default();
            renderFinishedSemaphores = new long[images.length];
            for (int index = 0; index < images.length; index++) {
                check(vkCreateSemaphore(device, semaphoreInfo, null, handle),
                        "vkCreateSemaphore(renderFinished[" + index + "])");
                renderFinishedSemaphores[index] = handle.get(0);
            }
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
            imageAvailableSemaphores = new long[images.length];
            inFlightFences = new long[images.length];
            vertexAllocations = new BufferAllocation[images.length];
            vertexCapacities = new int[images.length];
            VkFenceCreateInfo fenceInfo = VkFenceCreateInfo.calloc(stack)
                    .sType$Default().flags(VK_FENCE_CREATE_SIGNALED_BIT);
            for (int index = 0; index < images.length; index++) {
                check(vkCreateSemaphore(device, semaphoreInfo, null, handle),
                        "vkCreateSemaphore(imageAvailable[" + index + "])");
                imageAvailableSemaphores[index] = handle.get(0);
                check(vkCreateFence(device, fenceInfo, null, handle),
                        "vkCreateFence(inFlight[" + index + "])");
                inFlightFences[index] = handle.get(0);
            }
            frameCursor = 0;
        }

        private long colorPipeline(MemoryStack stack, VulkanBlendMode blendMode) {
            int index = blendMode.ordinal();
            if (colorPipelines[index] == VK_NULL_HANDLE) {
                createColorPipeline(stack, blendMode);
            }
            return colorPipelines[index];
        }

        private void createColorPipeline(MemoryStack stack, VulkanBlendMode blendMode) {
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
                configureBlend(blendAttachment.get(0), blendMode)
                        .colorWriteMask(VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT
                                | VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT);
                VkPipelineColorBlendStateCreateInfo colorBlending =
                        VkPipelineColorBlendStateCreateInfo.calloc(stack).sType$Default()
                                .logicOpEnable(false).pAttachments(blendAttachment);
                VkPipelineDynamicStateCreateInfo dynamicState =
                        VkPipelineDynamicStateCreateInfo.calloc(stack).sType$Default()
                                .pDynamicStates(stack.ints(
                                        VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR));
                LongBuffer handle = stack.mallocLong(1);
                if (pipelineLayout == VK_NULL_HANDLE) {
                    VkPipelineLayoutCreateInfo layoutInfo =
                            VkPipelineLayoutCreateInfo.calloc(stack).sType$Default();
                    check(vkCreatePipelineLayout(device, layoutInfo, null, handle),
                            "vkCreatePipelineLayout");
                    pipelineLayout = handle.get(0);
                }
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
                colorPipelines[blendMode.ordinal()] = handle.get(0);
            } finally {
                if (fragmentModule != VK_NULL_HANDLE) {
                    vkDestroyShaderModule(device, fragmentModule, null);
                }
                if (vertexModule != VK_NULL_HANDLE) {
                    vkDestroyShaderModule(device, vertexModule, null);
                }
            }
        }

        private long texturePipeline(MemoryStack stack, VulkanBlendMode blendMode) {
            int index = blendMode.ordinal();
            if (texturePipelines[index] == VK_NULL_HANDLE) {
                createTexturePipeline(stack, blendMode);
            }
            return texturePipelines[index];
        }

        private void createTexturePipeline(MemoryStack stack, VulkanBlendMode blendMode) {
            if (textureDescriptorSetLayout == VK_NULL_HANDLE) {
                throw new IllegalStateException("texture descriptor layout is unavailable");
            }
            long vertexModule = VK_NULL_HANDLE;
            long fragmentModule = VK_NULL_HANDLE;
            try {
                vertexModule = createShaderModule(stack, TEXTURE_VERTEX_SHADER,
                        shaderc_glsl_vertex_shader, "textured-quad.vert");
                fragmentModule = createShaderModule(stack, TEXTURE_FRAGMENT_SHADER,
                        shaderc_glsl_fragment_shader, "textured-quad.frag");
                VkPipelineShaderStageCreateInfo.Buffer stages =
                        VkPipelineShaderStageCreateInfo.calloc(2, stack);
                stages.get(0).sType$Default().stage(VK_SHADER_STAGE_VERTEX_BIT)
                        .module(vertexModule).pName(stack.UTF8("main"));
                stages.get(1).sType$Default().stage(VK_SHADER_STAGE_FRAGMENT_BIT)
                        .module(fragmentModule).pName(stack.UTF8("main"));
                VkVertexInputBindingDescription.Buffer binding =
                        VkVertexInputBindingDescription.calloc(1, stack);
                binding.get(0).binding(0).stride(TEXTURED_VERTEX_STRIDE)
                        .inputRate(VK_VERTEX_INPUT_RATE_VERTEX);
                VkVertexInputAttributeDescription.Buffer attributes =
                        VkVertexInputAttributeDescription.calloc(3, stack);
                attributes.get(0).location(0).binding(0)
                        .format(VK_FORMAT_R32G32_SFLOAT).offset(0);
                attributes.get(1).location(1).binding(0)
                        .format(VK_FORMAT_R32G32_SFLOAT).offset(2 * Float.BYTES);
                attributes.get(2).location(2).binding(0)
                        .format(VK_FORMAT_R32G32B32A32_SFLOAT).offset(4 * Float.BYTES);
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
                configureBlend(blendAttachment.get(0), blendMode)
                        .colorWriteMask(VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT
                                | VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT);
                VkPipelineColorBlendStateCreateInfo colorBlending =
                        VkPipelineColorBlendStateCreateInfo.calloc(stack).sType$Default()
                                .logicOpEnable(false).pAttachments(blendAttachment);
                VkPipelineDynamicStateCreateInfo dynamicState =
                        VkPipelineDynamicStateCreateInfo.calloc(stack).sType$Default()
                                .pDynamicStates(stack.ints(
                                        VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR));
                LongBuffer handle = stack.mallocLong(1);
                if (texturePipelineLayout == VK_NULL_HANDLE) {
                    VkPushConstantRange.Buffer pushConstants =
                            VkPushConstantRange.calloc(1, stack);
                    pushConstants.get(0).stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT)
                            .offset(0).size(24);
                    VkPipelineLayoutCreateInfo layoutInfo = VkPipelineLayoutCreateInfo.calloc(stack)
                            .sType$Default().pSetLayouts(stack.longs(textureDescriptorSetLayout))
                            .pPushConstantRanges(pushConstants);
                    check(vkCreatePipelineLayout(device, layoutInfo, null, handle),
                            "vkCreatePipelineLayout(texture)");
                    texturePipelineLayout = handle.get(0);
                }
                VkGraphicsPipelineCreateInfo.Buffer pipelineInfo =
                        VkGraphicsPipelineCreateInfo.calloc(1, stack);
                pipelineInfo.get(0).sType$Default().pStages(stages)
                        .pVertexInputState(vertexInput).pInputAssemblyState(inputAssembly)
                        .pViewportState(viewportState).pRasterizationState(rasterizer)
                        .pMultisampleState(multisampling).pColorBlendState(colorBlending)
                        .pDynamicState(dynamicState).layout(texturePipelineLayout)
                        .renderPass(renderPass).subpass(0);
                check(vkCreateGraphicsPipelines(device, VK_NULL_HANDLE,
                        pipelineInfo, null, handle), "vkCreateGraphicsPipelines(texturedQuad)");
                texturePipelines[blendMode.ordinal()] = handle.get(0);
            } finally {
                if (fragmentModule != VK_NULL_HANDLE) {
                    vkDestroyShaderModule(device, fragmentModule, null);
                }
                if (vertexModule != VK_NULL_HANDLE) {
                    vkDestroyShaderModule(device, vertexModule, null);
                }
            }
        }

        private static VkPipelineColorBlendAttachmentState configureBlend(
                VkPipelineColorBlendAttachmentState attachment,
                VulkanBlendMode blendMode) {
            attachment.blendEnable(true)
                    .colorBlendOp(VK_BLEND_OP_ADD)
                    .alphaBlendOp(VK_BLEND_OP_ADD);
            switch (blendMode) {
                case ADDITIVE:
                    return attachment
                            .srcColorBlendFactor(VK_BLEND_FACTOR_SRC_ALPHA)
                            .dstColorBlendFactor(VK_BLEND_FACTOR_ONE)
                            .srcAlphaBlendFactor(VK_BLEND_FACTOR_SRC_ALPHA)
                            .dstAlphaBlendFactor(VK_BLEND_FACTOR_ONE);
                case COPY:
                    return attachment
                            .srcColorBlendFactor(VK_BLEND_FACTOR_ONE)
                            .dstColorBlendFactor(VK_BLEND_FACTOR_ONE)
                            .srcAlphaBlendFactor(VK_BLEND_FACTOR_ONE)
                            .dstAlphaBlendFactor(VK_BLEND_FACTOR_ONE);
                case MODULATE:
                    return attachment
                            .srcColorBlendFactor(VK_BLEND_FACTOR_DST_COLOR)
                            .dstColorBlendFactor(VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA)
                            .srcAlphaBlendFactor(VK_BLEND_FACTOR_DST_COLOR)
                            .dstAlphaBlendFactor(VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA);
                case NORMAL:
                default:
                    return attachment
                            .srcColorBlendFactor(VK_BLEND_FACTOR_SRC_ALPHA)
                            .dstColorBlendFactor(VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA)
                            .srcAlphaBlendFactor(VK_BLEND_FACTOR_ONE)
                            .dstAlphaBlendFactor(VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA);
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

        private long uploadTexture(VulkanTextureData texture) {
            if (closed) throw new IllegalStateException("Vulkan surface is closed");
            long uploadStarted = System.nanoTime();
            TextureResource created = new TextureResource();
            boolean complete = false;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                ensureTextureDescriptors(stack);
                VkImageCreateInfo imageInfo = VkImageCreateInfo.calloc(stack).sType$Default()
                        .imageType(VK_IMAGE_TYPE_2D)
                        .format(VK_FORMAT_R8G8B8A8_UNORM)
                        .mipLevels(1).arrayLayers(1)
                        .samples(VK_SAMPLE_COUNT_1_BIT)
                        .tiling(VK_IMAGE_TILING_OPTIMAL)
                        .usage(VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT)
                        .sharingMode(VK_SHARING_MODE_EXCLUSIVE)
                        .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
                imageInfo.extent().width(texture.width()).height(texture.height()).depth(1);
                LongBuffer handle = stack.mallocLong(1);
                check(vkCreateImage(device, imageInfo, null, handle), "vkCreateImage(texture)");
                created.image = handle.get(0);
                created.width = texture.width();
                created.height = texture.height();
                VkMemoryRequirements requirements = VkMemoryRequirements.malloc(stack);
                vkGetImageMemoryRequirements(device, created.image, requirements);
                VkMemoryAllocateInfo allocation = VkMemoryAllocateInfo.calloc(stack)
                        .sType$Default().allocationSize(requirements.size())
                        .memoryTypeIndex(findMemoryType(stack, requirements.memoryTypeBits(),
                                VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT));
                check(vkAllocateMemory(device, allocation, null, handle),
                        "vkAllocateMemory(texture)");
                created.memory = handle.get(0);
                check(vkBindImageMemory(device, created.image, created.memory, 0),
                        "vkBindImageMemory(texture)");

                VkImageViewCreateInfo viewInfo = VkImageViewCreateInfo.calloc(stack)
                        .sType$Default().image(created.image)
                        .viewType(VK_IMAGE_VIEW_TYPE_2D).format(VK_FORMAT_R8G8B8A8_UNORM);
                viewInfo.components().r(VK_COMPONENT_SWIZZLE_IDENTITY)
                        .g(VK_COMPONENT_SWIZZLE_IDENTITY)
                        .b(VK_COMPONENT_SWIZZLE_IDENTITY)
                        .a(VK_COMPONENT_SWIZZLE_IDENTITY);
                viewInfo.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                        .baseMipLevel(0).levelCount(1).baseArrayLayer(0).layerCount(1);
                check(vkCreateImageView(device, viewInfo, null, handle),
                        "vkCreateImageView(texture)");
                created.view = handle.get(0);
                for (VulkanTextureFilter filter : VulkanTextureFilter.values()) {
                    int vkFilter = filter == VulkanTextureFilter.LINEAR
                            ? VK_FILTER_LINEAR : VK_FILTER_NEAREST;
                    VkSamplerCreateInfo samplerInfo = VkSamplerCreateInfo.calloc(stack)
                            .sType$Default()
                            .magFilter(vkFilter).minFilter(vkFilter)
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
                            "vkCreateSampler(texture " + filter + ")");
                    created.samplers[filter.ordinal()] = handle.get(0);
                    allocateTextureDescriptor(stack, created, filter);
                }

                long publicHandle = nextTextureHandle++;
                if (publicHandle <= 0L) throw new IllegalStateException("texture handles exhausted");
                textures.put(publicHandle, created);
                pendingTextureUploads.put(publicHandle,
                        new PendingTextureUpload(texture, false));
                complete = true;
                return publicHandle;
            } finally {
                if (!complete) destroyTextureResource(created, true);
                if (Boolean.getBoolean("rusted.fabric.vulkan.profileSlowFrames")) {
                    long uploadMicros = (System.nanoTime() - uploadStarted) / 1_000L;
                    if (uploadMicros >= 4_000L) {
                        System.out.println("[Vulkan Mod/Driver] Slow texture allocation: "
                                + texture.width() + "x" + texture.height() + " in "
                                + (uploadMicros / 1000.0) + "ms");
                    }
                }
            }
        }

        private long createRenderTarget(int width, int height) {
            if (closed) throw new IllegalStateException("Vulkan surface is closed");
            if (width <= 0 || height <= 0) {
                throw new IllegalArgumentException("render-target size must be positive");
            }
            TextureResource created = new TextureResource();
            boolean complete = false;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                ensureTextureDescriptors(stack);
                VkImageCreateInfo imageInfo = VkImageCreateInfo.calloc(stack).sType$Default()
                        .imageType(VK_IMAGE_TYPE_2D)
                        .format(info.imageFormat())
                        .mipLevels(1).arrayLayers(1)
                        .samples(VK_SAMPLE_COUNT_1_BIT)
                        .tiling(VK_IMAGE_TILING_OPTIMAL)
                        .usage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT
                                | VK_IMAGE_USAGE_SAMPLED_BIT
                                | VK_IMAGE_USAGE_TRANSFER_SRC_BIT)
                        .sharingMode(VK_SHARING_MODE_EXCLUSIVE)
                        .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
                imageInfo.extent().width(width).height(height).depth(1);
                LongBuffer handle = stack.mallocLong(1);
                check(vkCreateImage(device, imageInfo, null, handle),
                        "vkCreateImage(render target)");
                created.image = handle.get(0);
                created.width = width;
                created.height = height;
                created.renderTarget = true;
                VkMemoryRequirements requirements = VkMemoryRequirements.malloc(stack);
                vkGetImageMemoryRequirements(device, created.image, requirements);
                VkMemoryAllocateInfo allocation = VkMemoryAllocateInfo.calloc(stack)
                        .sType$Default().allocationSize(requirements.size())
                        .memoryTypeIndex(findMemoryType(stack, requirements.memoryTypeBits(),
                                VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT));
                check(vkAllocateMemory(device, allocation, null, handle),
                        "vkAllocateMemory(render target)");
                created.memory = handle.get(0);
                check(vkBindImageMemory(device, created.image, created.memory, 0),
                        "vkBindImageMemory(render target)");

                VkImageViewCreateInfo viewInfo = VkImageViewCreateInfo.calloc(stack)
                        .sType$Default().image(created.image)
                        .viewType(VK_IMAGE_VIEW_TYPE_2D).format(info.imageFormat());
                viewInfo.components().r(VK_COMPONENT_SWIZZLE_IDENTITY)
                        .g(VK_COMPONENT_SWIZZLE_IDENTITY)
                        .b(VK_COMPONENT_SWIZZLE_IDENTITY)
                        .a(VK_COMPONENT_SWIZZLE_IDENTITY);
                viewInfo.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                        .baseMipLevel(0).levelCount(1).baseArrayLayer(0).layerCount(1);
                check(vkCreateImageView(device, viewInfo, null, handle),
                        "vkCreateImageView(render target)");
                created.view = handle.get(0);
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
                            "vkCreateSampler(render target " + filter + ")");
                    created.samplers[filter.ordinal()] = handle.get(0);
                    allocateTextureDescriptor(stack, created, filter);
                }
                VkFramebufferCreateInfo framebufferInfo = VkFramebufferCreateInfo.calloc(stack)
                        .sType$Default().renderPass(offscreenClearRenderPass)
                        .pAttachments(stack.longs(created.view))
                        .width(width).height(height).layers(1);
                check(vkCreateFramebuffer(device, framebufferInfo, null, handle),
                        "vkCreateFramebuffer(render target)");
                created.framebuffer = handle.get(0);
                long publicHandle = nextTextureHandle++;
                if (publicHandle <= 0L) throw new IllegalStateException("texture handles exhausted");
                textures.put(publicHandle, created);
                complete = true;
                return publicHandle;
            } finally {
                if (!complete) destroyTextureResource(created, true);
            }
        }

        private void renderToTexture(long textureHandle, VulkanFrameCommands frame) {
            TextureResource target = textures.get(textureHandle);
            if (target == null || !target.renderTarget) {
                throw new IllegalArgumentException("unknown render-target handle "
                        + textureHandle);
            }
            if (target.width != frame.width() || target.height != frame.height()) {
                throw new IllegalArgumentException("render-target command size changed from "
                        + target.width + "x" + target.height + " to "
                        + frame.width() + "x" + frame.height());
            }
            // This first native implementation deliberately uses a conservative synchronization
            // boundary. It proves complete GPU ownership of child images; batching these passes
            // into the top-level frame is a later performance-only change.
            check(vkDeviceWaitIdle(device), "vkDeviceWaitIdle(render target)");
            try (MemoryStack stack = MemoryStack.stackPush()) {
                destroyRetiredTextures(stack);
                FrameUpload upload = uploadFrame(frame, stack, 0);
                VkCommandBuffer command = commandBuffers[0];
                check(vkResetCommandBuffer(command, 0),
                        "vkResetCommandBuffer(render target)");
                VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack)
                        .sType$Default().flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
                check(vkBeginCommandBuffer(command, beginInfo),
                        "vkBeginCommandBuffer(render target)");
                recordPendingTextureUploads(command, stack);
                VkClearValue.Buffer clearValue = VkClearValue.calloc(1, stack);
                clearValue.get(0).color()
                        .float32(0, frame.clearRed()).float32(1, frame.clearGreen())
                        .float32(2, frame.clearBlue()).float32(3, frame.clearAlpha());
                boolean clearTarget = frame.clearRequested() || !target.initialized;
                long targetRenderPass = clearTarget
                        ? offscreenClearRenderPass : offscreenLoadRenderPass;
                VkRenderPassBeginInfo renderPassBegin = VkRenderPassBeginInfo.calloc(stack)
                        .sType$Default().renderPass(targetRenderPass)
                        .framebuffer(target.framebuffer).pClearValues(clearValue);
                renderPassBegin.renderArea().offset().x(0).y(0);
                renderPassBegin.renderArea().extent()
                        .width(target.width).height(target.height);
                vkCmdBeginRenderPass(command, renderPassBegin, VK_SUBPASS_CONTENTS_INLINE);
                long vertexBuffer = vertexAllocations[0] == null
                        ? VK_NULL_HANDLE : vertexAllocations[0].buffer;
                if (upload.totalVertexCount() > 0) {
                    VkViewport.Buffer viewport = VkViewport.calloc(1, stack);
                    viewport.get(0).x(0.0f).y(0.0f)
                            .width(target.width).height(target.height)
                            .minDepth(0.0f).maxDepth(1.0f);
                    vkCmdSetViewport(command, 0, viewport);
                }
                for (DrawBatch drawBatch : upload.batches) {
                    if (drawBatch instanceof ColoredDrawBatch) {
                        ColoredDrawBatch batch = (ColoredDrawBatch) drawBatch;
                        vkCmdBindPipeline(command, VK_PIPELINE_BIND_POINT_GRAPHICS,
                                colorPipeline(stack, batch.blendMode));
                        vkCmdBindVertexBuffers(command, 0,
                                stack.longs(vertexBuffer), stack.longs(0L));
                        if (setScissor(command, batch.clip, target.width,
                                target.height, stack)) {
                            vkCmdDraw(command, batch.vertexCount, 1, batch.firstVertex, 0);
                        }
                    } else {
                        TextureDrawBatch batch = (TextureDrawBatch) drawBatch;
                        if (batch.textureHandle == textureHandle) {
                            throw new IllegalArgumentException(
                                    "a render target cannot sample itself in the same pass");
                        }
                        vkCmdBindPipeline(command, VK_PIPELINE_BIND_POINT_GRAPHICS,
                                texturePipeline(stack, batch.blendMode));
                        vkCmdBindVertexBuffers(command, 0, stack.longs(vertexBuffer),
                                stack.longs(upload.texturedByteOffset));
                        if (setScissor(command, batch.clip, target.width,
                                target.height, stack)) {
                            vkCmdBindDescriptorSets(command, VK_PIPELINE_BIND_POINT_GRAPHICS,
                                    texturePipelineLayout, 0,
                                    stack.longs(batch.descriptorSet), null);
                            pushShaderState(command, batch.shaderState, stack);
                            vkCmdDraw(command, batch.vertexCount, 1, batch.firstVertex, 0);
                        }
                    }
                }
                vkCmdEndRenderPass(command);
                check(vkEndCommandBuffer(command), "vkEndCommandBuffer(render target)");
                VkSubmitInfo submit = VkSubmitInfo.calloc(stack).sType$Default()
                        .pCommandBuffers(stack.pointers(command.address()));
                check(vkQueueSubmit(graphicsQueue, submit, VK_NULL_HANDLE),
                        "vkQueueSubmit(render target)");
                check(vkQueueWaitIdle(graphicsQueue), "vkQueueWaitIdle(render target)");
                target.initialized = true;
                renderTargetSubmissions++;
                if (renderTargetSubmissions == 1) {
                    System.out.println("[Vulkan Mod/Driver] First native render target: "
                            + target.width + "x" + target.height + ", commands="
                            + frame.commands().size() + ", texture=" + textureHandle);
                }
                if (Boolean.getBoolean("rusted.fabric.vulkan.debugRenderTargetPasses")
                        && renderTargetSubmissions <= 64) {
                    java.util.LinkedHashSet<Long> sampled = new java.util.LinkedHashSet<Long>();
                    for (VulkanDrawCommand draw : frame.commands()) {
                        if (draw instanceof VulkanTexturedQuad) {
                            sampled.add(((VulkanTexturedQuad) draw).textureHandle());
                        } else if (draw instanceof VulkanTexturedTriangle) {
                            sampled.add(((VulkanTexturedTriangle) draw).textureHandle());
                        }
                    }
                    System.out.println("[Vulkan Mod/Driver] Native target pass #"
                            + renderTargetSubmissions + ": texture=" + textureHandle
                            + ", size=" + target.width + "x" + target.height
                            + ", clear=" + frame.clearRed() + "," + frame.clearGreen()
                            + "," + frame.clearBlue() + "," + frame.clearAlpha()
                            + ", commands=" + frame.commands().size()
                            + ", sampled=" + sampled);
                }
                if (Boolean.getBoolean("rusted.fabric.vulkan.debugRenderTargetPasses")
                        && !debugLargeTargetReadBack
                        && target.width >= 1000 && target.height >= 1000) {
                    debugLargeTargetReadBack = true;
                    logRenderTargetPixels(textureHandle, target);
                }
            }
        }

        private void logRenderTargetPixels(long textureHandle, TextureResource target) {
            int byteCount = Math.multiplyExact(Math.multiplyExact(
                    target.width, target.height), 4);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                BufferAllocation readback = createBufferAllocation(stack, byteCount,
                        VK_BUFFER_USAGE_TRANSFER_DST_BIT,
                        VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT
                                | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
                try {
                    VkCommandBuffer command = commandBuffers[0];
                    check(vkResetCommandBuffer(command, 0),
                            "vkResetCommandBuffer(render-target readback)");
                    VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack)
                            .sType$Default().flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
                    check(vkBeginCommandBuffer(command, beginInfo),
                            "vkBeginCommandBuffer(render-target readback)");
                    VkImageMemoryBarrier.Buffer toCopy = VkImageMemoryBarrier.calloc(1, stack);
                    toCopy.get(0).sType$Default()
                            .oldLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
                            .newLayout(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL)
                            .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                            .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                            .image(target.image).srcAccessMask(VK_ACCESS_SHADER_READ_BIT)
                            .dstAccessMask(VK_ACCESS_TRANSFER_READ_BIT);
                    toCopy.get(0).subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                            .baseMipLevel(0).levelCount(1).baseArrayLayer(0).layerCount(1);
                    vkCmdPipelineBarrier(command, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,
                            VK_PIPELINE_STAGE_TRANSFER_BIT, 0, null, null, toCopy);
                    VkBufferImageCopy.Buffer region = VkBufferImageCopy.calloc(1, stack);
                    region.get(0).bufferOffset(0).bufferRowLength(0).bufferImageHeight(0);
                    region.get(0).imageSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                            .mipLevel(0).baseArrayLayer(0).layerCount(1);
                    region.get(0).imageOffset().x(0).y(0).z(0);
                    region.get(0).imageExtent()
                            .width(target.width).height(target.height).depth(1);
                    vkCmdCopyImageToBuffer(command, target.image,
                            VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, readback.buffer, region);
                    VkImageMemoryBarrier.Buffer toSample = VkImageMemoryBarrier.calloc(1, stack);
                    toSample.get(0).sType$Default()
                            .oldLayout(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL)
                            .newLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
                            .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                            .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                            .image(target.image).srcAccessMask(VK_ACCESS_TRANSFER_READ_BIT)
                            .dstAccessMask(VK_ACCESS_SHADER_READ_BIT);
                    toSample.get(0).subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                            .baseMipLevel(0).levelCount(1).baseArrayLayer(0).layerCount(1);
                    vkCmdPipelineBarrier(command, VK_PIPELINE_STAGE_TRANSFER_BIT,
                            VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, 0, null, null, toSample);
                    check(vkEndCommandBuffer(command),
                            "vkEndCommandBuffer(render-target readback)");
                    VkSubmitInfo submit = VkSubmitInfo.calloc(stack).sType$Default()
                            .pCommandBuffers(stack.pointers(command.address()));
                    check(vkQueueSubmit(graphicsQueue, submit, VK_NULL_HANDLE),
                            "vkQueueSubmit(render-target readback)");
                    check(vkQueueWaitIdle(graphicsQueue),
                            "vkQueueWaitIdle(render-target readback)");
                    PointerBuffer mapped = stack.mallocPointer(1);
                    check(vkMapMemory(device, readback.memory, 0, byteCount, 0, mapped),
                            "vkMapMemory(render-target readback)");
                    int sampledPixels = 0;
                    int nonTransparent = 0;
                    int nonBlack = 0;
                    try {
                        ByteBuffer pixels = MemoryUtil.memByteBuffer(mapped.get(0), byteCount);
                        int stride = Math.max(1, target.width * target.height / 65536);
                        for (int pixel = 0; pixel < target.width * target.height;
                             pixel += stride) {
                            int offset = pixel * 4;
                            int first = pixels.get(offset) & 255;
                            int second = pixels.get(offset + 1) & 255;
                            int third = pixels.get(offset + 2) & 255;
                            int alpha = pixels.get(offset + 3) & 255;
                            sampledPixels++;
                            if (alpha != 0) nonTransparent++;
                            if (first != 0 || second != 0 || third != 0) nonBlack++;
                        }
                    } finally {
                        vkUnmapMemory(device, readback.memory);
                    }
                    System.out.println("[Vulkan Mod/Driver] Native target readback: texture="
                            + textureHandle + ", samples=" + sampledPixels
                            + ", nonTransparent=" + nonTransparent
                            + ", nonBlack=" + nonBlack);
                } finally {
                    destroyBufferAllocation(readback);
                }
            }
        }

        private void updateTexture(long textureHandle, VulkanTextureData texture) {
            TextureResource target = textures.get(textureHandle);
            if (target == null) {
                throw new IllegalArgumentException("unknown texture handle " + textureHandle);
            }
            if (target.width != texture.width() || target.height != texture.height()) {
                throw new IllegalArgumentException("texture update size changed from "
                        + target.width + "x" + target.height + " to "
                        + texture.width() + "x" + texture.height());
            }
            PendingTextureUpload previous = pendingTextureUploads.get(textureHandle);
            pendingTextureUploads.put(textureHandle, new PendingTextureUpload(texture,
                    previous == null ? target.initialized : previous.initialized));
        }

        private BufferAllocation ensureTextureStaging(MemoryStack stack, int requiredBytes) {
            if (textureStaging != null && textureStagingCapacity >= requiredBytes) {
                return textureStaging;
            }
            if (textureStaging != null) destroyBufferAllocation(textureStaging);
            int capacity = 1;
            while (capacity < requiredBytes && capacity > 0) capacity <<= 1;
            if (capacity <= 0) capacity = requiredBytes;
            textureStaging = createBufferAllocation(stack, capacity,
                    VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT
                            | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
            textureStagingCapacity = capacity;
            return textureStaging;
        }

        private void ensureTextureDescriptors(MemoryStack stack) {
            if (textureDescriptorSetLayout != VK_NULL_HANDLE) return;
            VkDescriptorSetLayoutBinding.Buffer binding =
                    VkDescriptorSetLayoutBinding.calloc(1, stack);
            binding.get(0).binding(0).descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .descriptorCount(1).stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT);
            VkDescriptorSetLayoutCreateInfo layoutInfo =
                    VkDescriptorSetLayoutCreateInfo.calloc(stack).sType$Default()
                            .pBindings(binding);
            LongBuffer handle = stack.mallocLong(1);
            check(vkCreateDescriptorSetLayout(device, layoutInfo, null, handle),
                    "vkCreateDescriptorSetLayout(texture)");
            textureDescriptorSetLayout = handle.get(0);
            VkDescriptorPoolSize.Buffer poolSize = VkDescriptorPoolSize.calloc(1, stack);
            poolSize.get(0).type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .descriptorCount(MAX_TEXTURES * VulkanTextureFilter.values().length);
            VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.calloc(stack)
                    .sType$Default().flags(VK_DESCRIPTOR_POOL_CREATE_FREE_DESCRIPTOR_SET_BIT)
                    .maxSets(MAX_TEXTURES * VulkanTextureFilter.values().length)
                    .pPoolSizes(poolSize);
            check(vkCreateDescriptorPool(device, poolInfo, null, handle),
                    "vkCreateDescriptorPool(texture)");
            textureDescriptorPool = handle.get(0);
        }

        private void allocateTextureDescriptor(MemoryStack stack, TextureResource texture,
                                               VulkanTextureFilter filter) {
            VkDescriptorSetAllocateInfo allocateInfo = VkDescriptorSetAllocateInfo.calloc(stack)
                    .sType$Default().descriptorPool(textureDescriptorPool)
                    .pSetLayouts(stack.longs(textureDescriptorSetLayout));
            LongBuffer descriptor = stack.mallocLong(1);
            check(vkAllocateDescriptorSets(device, allocateInfo, descriptor),
                    "vkAllocateDescriptorSets(texture)");
            texture.descriptorSets[filter.ordinal()] = descriptor.get(0);
            VkDescriptorImageInfo.Buffer imageInfo = VkDescriptorImageInfo.calloc(1, stack);
            imageInfo.get(0).sampler(texture.samplers[filter.ordinal()]).imageView(texture.view)
                    .imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
            VkWriteDescriptorSet.Buffer write = VkWriteDescriptorSet.calloc(1, stack);
            write.get(0).sType$Default()
                    .dstSet(texture.descriptorSets[filter.ordinal()]).dstBinding(0)
                    .dstArrayElement(0).descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .descriptorCount(1).pImageInfo(imageInfo);
            vkUpdateDescriptorSets(device, write, null);
        }

        private void recordPendingTextureUploads(VkCommandBuffer command, MemoryStack stack) {
            if (pendingTextureUploads.isEmpty()) return;
            int byteCount = 0;
            for (PendingTextureUpload pending : pendingTextureUploads.values()) {
                byteCount = Math.addExact(byteCount, pending.texture.byteSize());
            }
            BufferAllocation staging = ensureTextureStaging(stack, byteCount);
            PointerBuffer mapped = stack.mallocPointer(1);
            check(vkMapMemory(device, staging.memory, 0, byteCount, 0, mapped),
                    "vkMapMemory(pending texture uploads)");
            try {
                ByteBuffer destination = MemoryUtil.memByteBuffer(mapped.get(0), byteCount);
                for (PendingTextureUpload pending : pendingTextureUploads.values()) {
                    pending.texture.writeTo(destination);
                }
            } finally {
                vkUnmapMemory(device, staging.memory);
            }

            int offset = 0;
            for (Map.Entry<Long, PendingTextureUpload> entry
                    : pendingTextureUploads.entrySet()) {
                TextureResource target = textures.get(entry.getKey());
                PendingTextureUpload pending = entry.getValue();
                if (target == null) {
                    offset += pending.texture.byteSize();
                    continue;
                }
                VkImageMemoryBarrier.Buffer toTransfer = VkImageMemoryBarrier.calloc(1, stack);
                toTransfer.get(0).sType$Default()
                        .oldLayout(pending.initialized
                                ? VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL
                                : VK_IMAGE_LAYOUT_UNDEFINED)
                        .newLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
                        .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                        .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                        .image(target.image).srcAccessMask(pending.initialized
                                ? VK_ACCESS_SHADER_READ_BIT : 0)
                        .dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
                toTransfer.get(0).subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                        .baseMipLevel(0).levelCount(1).baseArrayLayer(0).layerCount(1);
                vkCmdPipelineBarrier(command, pending.initialized
                                ? VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT
                                : VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT,
                        VK_PIPELINE_STAGE_TRANSFER_BIT, 0, null, null, toTransfer);
                VkBufferImageCopy.Buffer region = VkBufferImageCopy.calloc(1, stack);
                region.get(0).bufferOffset(offset).bufferRowLength(0).bufferImageHeight(0);
                region.get(0).imageSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                        .mipLevel(0).baseArrayLayer(0).layerCount(1);
                region.get(0).imageOffset().x(0).y(0).z(0);
                region.get(0).imageExtent().width(target.width).height(target.height).depth(1);
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
                offset += pending.texture.byteSize();
            }
            pendingTextureUploads.clear();
        }

        private BufferAllocation createBufferAllocation(MemoryStack stack, long size,
                                                        int usage, int memoryFlags) {
            VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.calloc(stack).sType$Default()
                    .size(size).usage(usage).sharingMode(VK_SHARING_MODE_EXCLUSIVE);
            LongBuffer handle = stack.mallocLong(1);
            check(vkCreateBuffer(device, bufferInfo, null, handle), "vkCreateBuffer");
            BufferAllocation result = new BufferAllocation();
            result.buffer = handle.get(0);
            try {
                VkMemoryRequirements requirements = VkMemoryRequirements.malloc(stack);
                vkGetBufferMemoryRequirements(device, result.buffer, requirements);
                VkMemoryAllocateInfo allocation = VkMemoryAllocateInfo.calloc(stack)
                        .sType$Default().allocationSize(requirements.size())
                        .memoryTypeIndex(findMemoryType(stack, requirements.memoryTypeBits(),
                                memoryFlags));
                check(vkAllocateMemory(device, allocation, null, handle), "vkAllocateMemory");
                result.memory = handle.get(0);
                check(vkBindBufferMemory(device, result.buffer, result.memory, 0),
                        "vkBindBufferMemory");
                return result;
            } catch (Throwable failure) {
                destroyBufferAllocation(result);
                throw failure;
            }
        }

        private void destroyBufferAllocation(BufferAllocation allocation) {
            if (allocation.buffer != VK_NULL_HANDLE) {
                vkDestroyBuffer(device, allocation.buffer, null);
            }
            if (allocation.memory != VK_NULL_HANDLE) vkFreeMemory(device, allocation.memory, null);
        }

        private void destroyTexture(long textureHandle) {
            TextureResource texture = textures.remove(textureHandle);
            if (texture == null) return;
            pendingTextureUploads.remove(textureHandle);
            // A previous submitted frame can still sample this image. Release it after that
            // frame's fence instead of forcing the whole device idle for every map-cache update.
            retiredTextures.addLast(texture);
        }

        private void destroyRetiredTextures(MemoryStack stack) {
            while (!retiredTextures.isEmpty()) {
                destroyTextureResource(retiredTextures.removeFirst(), true, stack);
            }
        }

        private void destroyTextureResource(TextureResource texture, boolean freeDescriptor) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                destroyTextureResource(texture, freeDescriptor, stack);
            }
        }

        private void destroyTextureResource(TextureResource texture, boolean freeDescriptor,
                                            MemoryStack stack) {
            if (texture.framebuffer != VK_NULL_HANDLE) {
                vkDestroyFramebuffer(device, texture.framebuffer, null);
            }
            for (long descriptorSet : texture.descriptorSets) {
                if (freeDescriptor && descriptorSet != VK_NULL_HANDLE
                        && textureDescriptorPool != VK_NULL_HANDLE) {
                    check(vkFreeDescriptorSets(device, textureDescriptorPool,
                            stack.longs(descriptorSet)), "vkFreeDescriptorSets(texture)");
                }
            }
            for (long sampler : texture.samplers) {
                if (sampler != VK_NULL_HANDLE) vkDestroySampler(device, sampler, null);
            }
            if (texture.view != VK_NULL_HANDLE) vkDestroyImageView(device, texture.view, null);
            if (texture.image != VK_NULL_HANDLE) vkDestroyImage(device, texture.image, null);
            if (texture.memory != VK_NULL_HANDLE) vkFreeMemory(device, texture.memory, null);
        }

        private VulkanSurfaceInfo presentFrame(VulkanFrameCommands frame) {
            return prepareAndPresentFrame(frame, false);
        }

        private VulkanSurfaceInfo presentFrameAndReveal(VulkanFrameCommands frame) {
            if (overlay == null) {
                throw new IllegalStateException("Vulkan surface has no independently owned overlay");
            }
            return prepareAndPresentFrame(frame, true);
        }

        private VulkanSurfaceInfo prepareAndPresentFrame(VulkanFrameCommands frame,
                                                         boolean revealBeforePresent) {
            if (closed) throw new IllegalStateException("Vulkan surface is closed");
            int width = frame.width();
            int height = frame.height();
            if (overlay != null && !overlay.isPreparedFor(width, height)) return null;
            if (nativeWindow != null) {
                if (nativeWindow.isMinimized()) return null;
                width = nativeWindow.clientWidth();
                height = nativeWindow.clientHeight();
            }
            if (width > 0 && height > 0
                    && (width != info.width() || height != info.height())) {
                recreateSwapchain(width, height);
            }
            return presentFrame(frame, true, revealBeforePresent);
        }

        private boolean setVisible(boolean visible) {
            if (nativeWindow != null) return nativeWindow.setVisible(visible);
            return overlay != null && overlay.setVisible(visible);
        }

        private boolean prepareWindow(int width, int height, boolean visible) {
            if (nativeWindow != null) return nativeWindow.prepare(width, height, visible);
            if (overlay == null) return true;
            overlay.visibleRequested = visible;
            return overlay.resize(width, height, !visible);
        }

        private void maintainWindow() {
            if (nativeWindow != null) nativeWindow.pumpMessages();
            if (overlay != null) overlay.maintainParentWindow();
        }

        private boolean isCloseRequested() {
            return nativeWindow != null && nativeWindow.isCloseRequested();
        }

        private List<VulkanInputEvent> pollInputEvents() {
            return nativeWindow == null
                    ? Collections.emptyList() : nativeWindow.drainInputEvents();
        }

        private void setSystemCursorVisible(boolean visible) {
            if (nativeWindow != null) nativeWindow.setSystemCursorVisible(visible);
        }

        private VulkanSurfaceInfo presentFrame(VulkanFrameCommands frame,
                                               boolean retryOutOfDate,
                                               boolean revealBeforePresent) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                if (Boolean.getBoolean("rusted.fabric.vulkan.debugRenderTargetPasses")
                        && !debugMainTargetSamplesLogged) {
                    int logged = 0;
                    for (VulkanDrawCommand draw : frame.commands()) {
                        if (!(draw instanceof VulkanTexturedQuad)) continue;
                        VulkanTexturedQuad quad = (VulkanTexturedQuad) draw;
                        TextureResource sampled = textures.get(quad.textureHandle());
                        if (sampled == null || !sampled.renderTarget) continue;
                        VulkanDrawState state = quad.state();
                        System.out.println("[Vulkan Mod/Driver] Main samples native target "
                                + quad.textureHandle() + " at " + quad.x() + "," + quad.y()
                                + " size=" + quad.width() + "x" + quad.height()
                                + " uv=" + quad.u0() + "," + quad.v0() + ".."
                                + quad.u1() + "," + quad.v1()
                                + " rgba=" + quad.red() + "," + quad.green() + ","
                                + quad.blue() + "," + quad.alpha()
                                + " transform=" + state.transform()
                                + " clip=" + state.clip());
                        if (++logged >= 16) break;
                    }
                    if (logged > 0) debugMainTargetSamplesLogged = true;
                }
                long profileStarted = System.nanoTime();
                boolean infiniteWait = Boolean.getBoolean(
                        "rusted.fabric.vulkan.debugInfiniteAcquire");
                // Keep swapchain acquisition bounded when the window is occluded, but do not
                // discard a submitted frame merely because a zoom cache refresh exceeded 16 ms.
                long fenceTimeout = infiniteWait ? -1L : 100_000_000L;
                long acquireTimeout = infiniteWait ? -1L : 16_000_000L;
                int frameSlot = frameCursor;
                long inFlightFence = inFlightFences[frameSlot];
                long imageAvailableSemaphore = imageAvailableSemaphores[frameSlot];
                int fenceWait = vkWaitForFences(device, stack.longs(inFlightFence), true,
                        fenceTimeout);
                if (fenceWait == VK_TIMEOUT || fenceWait == VK_NOT_READY) {
                    fenceWaitSkips++;
                    if (fenceWaitSkips == 1 || fenceWaitSkips % 300 == 0) {
                        System.out.println("[Vulkan Mod/Driver] In-flight fence wait skipped #"
                                + fenceWaitSkips + " (VkResult " + fenceWait + ")");
                    }
                    return null;
                }
                check(fenceWait, "vkWaitForFences");
                // Texture images and the shared transfer staging buffer are not duplicated per
                // frame. Synchronize all outstanding submissions only when a texture actually
                // changes; ordinary frames remain independently in flight.
                if (!pendingTextureUploads.isEmpty()) {
                    int textureFenceWait = vkWaitForFences(device,
                            stack.longs(inFlightFences), true, fenceTimeout);
                    if (textureFenceWait == VK_TIMEOUT || textureFenceWait == VK_NOT_READY) {
                        fenceWaitSkips++;
                        return null;
                    }
                    check(textureFenceWait, "vkWaitForFences(texture uploads)");
                    destroyRetiredTextures(stack);
                } else if (!retiredTextures.isEmpty() && allFrameFencesSignaled()) {
                    destroyRetiredTextures(stack);
                }
                long afterFence = System.nanoTime();
                IntBuffer imageIndex = stack.mallocInt(1);
                // A minimized/occluded Win32 surface is allowed to stop returning images.
                // Never let that suspend Rusted Warfare's game thread indefinitely.
                int acquire = vkAcquireNextImageKHR(device, swapchain, acquireTimeout,
                        imageAvailableSemaphore, VK_NULL_HANDLE, imageIndex);
                if (acquire == VK_TIMEOUT || acquire == VK_NOT_READY) {
                    acquireSkips++;
                    if (acquireSkips == 1 || acquireSkips % 300 == 0) {
                        System.out.println("[Vulkan Mod/Driver] Swapchain acquire skipped #"
                                + acquireSkips + " (VkResult " + acquire + ")");
                    }
                    return null;
                }
                if (acquire == VK_ERROR_OUT_OF_DATE_KHR && retryOutOfDate) {
                    recreateSwapchain(targetWidth(frame), targetHeight(frame));
                    return presentFrame(frame, false, revealBeforePresent);
                }
                if (acquire != VK_SUCCESS && acquire != VK_SUBOPTIMAL_KHR) {
                    check(acquire, "vkAcquireNextImageKHR");
                }
                long afterAcquire = System.nanoTime();
                check(vkResetFences(device, stack.longs(inFlightFence)), "vkResetFences");
                int index = imageIndex.get(0);
                long renderFinishedSemaphore = renderFinishedSemaphores[index];
                FrameUpload upload = uploadFrame(frame, stack, frameSlot);
                long afterVertexUpload = System.nanoTime();
                VkCommandBuffer commandBuffer = commandBuffers[frameSlot];
                check(vkResetCommandBuffer(commandBuffer, 0), "vkResetCommandBuffer");
                VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack)
                        .sType$Default().flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
                check(vkBeginCommandBuffer(commandBuffer, beginInfo), "vkBeginCommandBuffer");
                // Texture copies share this frame submission. The in-flight fence above protects
                // both the sampled images and the reusable staging buffer, so no queue-idle stall
                // is needed for terrain-cache refreshes.
                recordPendingTextureUploads(commandBuffer, stack);
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
                long vertexBuffer = vertexAllocations[frameSlot] == null
                        ? VK_NULL_HANDLE : vertexAllocations[frameSlot].buffer;
                if (upload.totalVertexCount() > 0) {
                    VkViewport.Buffer viewport = VkViewport.calloc(1, stack);
                    viewport.get(0).x(0.0f).y(0.0f)
                            .width(info.width()).height(info.height())
                            .minDepth(0.0f).maxDepth(1.0f);
                    vkCmdSetViewport(commandBuffer, 0, viewport);
                }
                for (DrawBatch drawBatch : upload.batches) {
                    if (drawBatch instanceof ColoredDrawBatch) {
                        ColoredDrawBatch batch = (ColoredDrawBatch) drawBatch;
                        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS,
                                colorPipeline(stack, batch.blendMode));
                        vkCmdBindVertexBuffers(commandBuffer, 0,
                                stack.longs(vertexBuffer), stack.longs(0L));
                        if (setScissor(commandBuffer, batch.clip, stack)) {
                            vkCmdDraw(commandBuffer, batch.vertexCount, 1,
                                    batch.firstVertex, 0);
                        }
                    } else {
                        TextureDrawBatch batch = (TextureDrawBatch) drawBatch;
                        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS,
                                texturePipeline(stack, batch.blendMode));
                        vkCmdBindVertexBuffers(commandBuffer, 0, stack.longs(vertexBuffer),
                                stack.longs(upload.texturedByteOffset));
                        if (setScissor(commandBuffer, batch.clip, stack)) {
                            vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS,
                                    texturePipelineLayout, 0,
                                    stack.longs(batch.descriptorSet), null);
                            pushShaderState(commandBuffer, batch.shaderState, stack);
                            vkCmdDraw(commandBuffer, batch.vertexCount, 1, batch.firstVertex, 0);
                        }
                    }
                }
                vkCmdEndRenderPass(commandBuffer);
                check(vkEndCommandBuffer(commandBuffer), "vkEndCommandBuffer");
                long afterRecording = System.nanoTime();

                VkSubmitInfo submit = VkSubmitInfo.calloc(stack).sType$Default()
                        .waitSemaphoreCount(1)
                        .pWaitSemaphores(stack.longs(imageAvailableSemaphore))
                        .pWaitDstStageMask(stack.ints(
                                VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT))
                        .pCommandBuffers(stack.pointers(commandBuffer.address()))
                        .pSignalSemaphores(stack.longs(renderFinishedSemaphore));
                check(vkQueueSubmit(graphicsQueue, submit, inFlightFence), "vkQueueSubmit");
                long afterSubmit = System.nanoTime();
                VkPresentInfoKHR present = VkPresentInfoKHR.calloc(stack).sType$Default()
                        .pWaitSemaphores(stack.longs(renderFinishedSemaphore))
                        .swapchainCount(1)
                        .pSwapchains(stack.longs(swapchain)).pImageIndices(imageIndex);
                int presented = vkQueuePresentKHR(presentQueue, present);
                if ((presented == VK_ERROR_OUT_OF_DATE_KHR || presented == VK_SUBOPTIMAL_KHR)
                        && retryOutOfDate) {
                    recreateSwapchain(targetWidth(frame), targetHeight(frame));
                    return null;
                } else if (presented != VK_SUCCESS) {
                    check(presented, "vkQueuePresentKHR");
                } else {
                    // The submit fence protects the single CPU upload/command resources. Present
                    // completion is tracked by render-finished semaphores indexed by swapchain
                    // image; reacquiring an image makes reuse of that image's semaphore safe.
                    // Waiting for the whole present queue here can hang the Win32 message thread
                    // indefinitely during an Alt-Tab or compositor transition.
                    successfulPresents++;
                    if (successfulPresents == 1) {
                        System.out.println("[Vulkan Mod/Driver] First frame presented; clear RGBA="
                                + frame.clearRed() + "," + frame.clearGreen() + ","
                                + frame.clearBlue() + "," + frame.clearAlpha()
                                + ", commands=" + frame.commands().size());
                    }
                }
                frameCursor = (frameSlot + 1) % inFlightFences.length;
                long afterPresent = System.nanoTime();
                if (Boolean.getBoolean("rusted.fabric.vulkan.profileFrameStages")
                        && afterPresent - profileStarted >= 8_000_000L) {
                    profiledSlowFrames++;
                    if (profiledSlowFrames == 1 || profiledSlowFrames % 10 == 0) {
                        System.out.println("[Vulkan Mod/Driver] Frame stages #"
                                + profiledSlowFrames + ": fence="
                                + elapsedMillis(profileStarted, afterFence) + "ms, acquire="
                                + elapsedMillis(afterFence, afterAcquire) + "ms, vertices="
                                + elapsedMillis(afterAcquire, afterVertexUpload) + "ms, record="
                                + elapsedMillis(afterVertexUpload, afterRecording) + "ms, submit="
                                + elapsedMillis(afterRecording, afterSubmit) + "ms, present="
                                + elapsedMillis(afterSubmit, afterPresent) + "ms, commands="
                                + frame.commands().size());
                    }
                }
                return info;
            }
        }

        private static double elapsedMillis(long started, long finished) {
            return Math.round((finished - started) / 10_000.0) / 100.0;
        }

        private boolean allFrameFencesSignaled() {
            for (long fence : inFlightFences) {
                int status = vkGetFenceStatus(device, fence);
                if (status == VK_NOT_READY) return false;
                check(status, "vkGetFenceStatus");
            }
            return true;
        }

        private int targetWidth(VulkanFrameCommands frame) {
            return nativeWindow == null ? frame.width() : nativeWindow.clientWidth();
        }

        private int targetHeight(VulkanFrameCommands frame) {
            return nativeWindow == null ? frame.height() : nativeWindow.clientHeight();
        }

        private FrameUpload uploadFrame(VulkanFrameCommands frame, MemoryStack stack,
                                        int frameSlot) {
            int coloredVertexCount = Math.addExact(
                    Math.multiplyExact(frame.coloredQuads().size(), 6),
                    Math.multiplyExact(frame.coloredTriangles().size(), 3));
            int texturedVertexCount = Math.addExact(
                    Math.multiplyExact(frame.texturedQuads().size(), 6),
                    Math.multiplyExact(frame.texturedTriangles().size(), 3));
            int coloredBytes = Math.multiplyExact(coloredVertexCount, VERTEX_STRIDE);
            int texturedBytes = Math.multiplyExact(texturedVertexCount, TEXTURED_VERTEX_STRIDE);
            int totalBytes = Math.addExact(coloredBytes, texturedBytes);
            FrameUpload result = new FrameUpload(coloredBytes);
            if (totalBytes == 0) return result;
            ensureVertexCapacity(frameSlot, totalBytes, stack);
            BufferAllocation vertexAllocation = vertexAllocations[frameSlot];
            PointerBuffer mapped = stack.mallocPointer(1);
            check(vkMapMemory(device, vertexAllocation.memory, 0, totalBytes, 0, mapped),
                    "vkMapMemory");
            try {
                ByteBuffer bytes = MemoryUtil.memByteBuffer(mapped.get(0), totalBytes);
                ByteBuffer coloredSlice = bytes.duplicate();
                coloredSlice.limit(coloredBytes);
                FloatBuffer coloredVertices = coloredSlice.slice()
                        .order(ByteOrder.nativeOrder()).asFloatBuffer();
                for (VulkanDrawCommand command : frame.commands()) {
                    if (command instanceof VulkanColoredQuad) {
                        VulkanColoredQuad quad = (VulkanColoredQuad) command;
                        VulkanTransform2D transform = quad.state().transform();
                        float left = quad.x();
                        float right = quad.x() + quad.width();
                        float top = quad.y();
                        float bottom = quad.y() + quad.height();
                        putVertex(coloredVertices, transform, left, top, frame, quad);
                        putVertex(coloredVertices, transform, left, bottom, frame, quad);
                        putVertex(coloredVertices, transform, right, bottom, frame, quad);
                        putVertex(coloredVertices, transform, left, top, frame, quad);
                        putVertex(coloredVertices, transform, right, bottom, frame, quad);
                        putVertex(coloredVertices, transform, right, top, frame, quad);
                    } else if (command instanceof VulkanColoredTriangle) {
                        VulkanColoredTriangle triangle = (VulkanColoredTriangle) command;
                        VulkanTransform2D transform = triangle.state().transform();
                        for (int vertex = 0; vertex < 3; vertex++) {
                            putColoredTriangleVertex(coloredVertices, transform,
                                    frame, triangle, vertex);
                        }
                    }
                }

                ByteBuffer texturedSlice = bytes.duplicate();
                texturedSlice.position(coloredBytes).limit(totalBytes);
                FloatBuffer texturedVertices = texturedSlice.slice()
                        .order(ByteOrder.nativeOrder()).asFloatBuffer();
                for (VulkanDrawCommand command : frame.commands()) {
                    if (command instanceof VulkanTexturedQuad) {
                        VulkanTexturedQuad quad = (VulkanTexturedQuad) command;
                        if (!textures.containsKey(quad.textureHandle())) {
                            throw new IllegalArgumentException(
                                    "unknown texture handle: " + quad.textureHandle());
                        }
                        VulkanTransform2D transform = quad.state().transform();
                        float left = quad.x();
                        float right = quad.x() + quad.width();
                        float top = quad.y();
                        float bottom = quad.y() + quad.height();
                        putTexturedVertex(texturedVertices, transform, left, top,
                                quad.u0(), quad.v0(), frame, quad);
                        putTexturedVertex(texturedVertices, transform, left, bottom,
                                quad.u0(), quad.v1(), frame, quad);
                        putTexturedVertex(texturedVertices, transform, right, bottom,
                                quad.u1(), quad.v1(), frame, quad);
                        putTexturedVertex(texturedVertices, transform, left, top,
                                quad.u0(), quad.v0(), frame, quad);
                        putTexturedVertex(texturedVertices, transform, right, bottom,
                                quad.u1(), quad.v1(), frame, quad);
                        putTexturedVertex(texturedVertices, transform, right, top,
                                quad.u1(), quad.v0(), frame, quad);
                    } else if (command instanceof VulkanTexturedTriangle) {
                        VulkanTexturedTriangle triangle = (VulkanTexturedTriangle) command;
                        if (!textures.containsKey(triangle.textureHandle())) {
                            throw new IllegalArgumentException(
                                    "unknown texture handle: " + triangle.textureHandle());
                        }
                        VulkanTransform2D transform = triangle.state().transform();
                        for (int vertex = 0; vertex < 3; vertex++) {
                            putTexturedTriangleVertex(texturedVertices, transform,
                                    frame, triangle, vertex);
                        }
                    }
                }
            } finally {
                vkUnmapMemory(device, vertexAllocation.memory);
            }
            int coloredFirstVertex = 0;
            int texturedFirstVertex = 0;
            DrawBatch currentBatch = null;
            for (VulkanDrawCommand command : frame.commands()) {
                if (command instanceof VulkanColoredQuad) {
                    VulkanColoredQuad quad = (VulkanColoredQuad) command;
                    if (!(currentBatch instanceof ColoredDrawBatch)
                            || !sameClip(currentBatch.clip, quad.state().clip())
                            || currentBatch.blendMode != quad.state().blendMode()) {
                        currentBatch = new ColoredDrawBatch(
                                quad.state().clip(), quad.state().blendMode(),
                                coloredFirstVertex);
                        result.batches.add(currentBatch);
                    }
                    currentBatch.vertexCount += 6;
                    coloredFirstVertex += 6;
                } else if (command instanceof VulkanColoredTriangle) {
                    VulkanColoredTriangle triangle = (VulkanColoredTriangle) command;
                    if (!(currentBatch instanceof ColoredDrawBatch)
                            || !sameClip(currentBatch.clip, triangle.state().clip())
                            || currentBatch.blendMode != triangle.state().blendMode()) {
                        currentBatch = new ColoredDrawBatch(
                                triangle.state().clip(), triangle.state().blendMode(),
                                coloredFirstVertex);
                        result.batches.add(currentBatch);
                    }
                    currentBatch.vertexCount += 3;
                    coloredFirstVertex += 3;
                } else if (command instanceof VulkanTexturedQuad) {
                    VulkanTexturedQuad quad = (VulkanTexturedQuad) command;
                    TextureResource texture = textures.get(quad.textureHandle());
                    long descriptorSet = texture.descriptorSets[
                            quad.state().textureFilter().ordinal()];
                    TextureDrawBatch textureBatch = currentBatch instanceof TextureDrawBatch
                            ? (TextureDrawBatch) currentBatch : null;
                    if (textureBatch == null
                            || textureBatch.textureHandle != quad.textureHandle()
                            || textureBatch.descriptorSet != descriptorSet
                            || !sameClip(textureBatch.clip, quad.state().clip())
                            || textureBatch.blendMode != quad.state().blendMode()
                            || !textureBatch.shaderState.equals(
                                    quad.state().shaderState())) {
                        currentBatch = new TextureDrawBatch(quad.textureHandle(),
                                descriptorSet,
                                quad.state().clip(),
                                quad.state().blendMode(), quad.state().shaderState(),
                                texturedFirstVertex);
                        result.batches.add(currentBatch);
                    }
                    currentBatch.vertexCount += 6;
                    texturedFirstVertex += 6;
                } else if (command instanceof VulkanTexturedTriangle) {
                    VulkanTexturedTriangle triangle = (VulkanTexturedTriangle) command;
                    TextureResource texture = textures.get(triangle.textureHandle());
                    long descriptorSet = texture.descriptorSets[
                            triangle.state().textureFilter().ordinal()];
                    TextureDrawBatch textureBatch = currentBatch instanceof TextureDrawBatch
                            ? (TextureDrawBatch) currentBatch : null;
                    if (textureBatch == null
                            || textureBatch.textureHandle != triangle.textureHandle()
                            || textureBatch.descriptorSet != descriptorSet
                            || !sameClip(textureBatch.clip, triangle.state().clip())
                            || textureBatch.blendMode != triangle.state().blendMode()
                            || !textureBatch.shaderState.equals(
                                    triangle.state().shaderState())) {
                        currentBatch = new TextureDrawBatch(triangle.textureHandle(),
                                descriptorSet,
                                triangle.state().clip(),
                                triangle.state().blendMode(), triangle.state().shaderState(),
                                texturedFirstVertex);
                        result.batches.add(currentBatch);
                    }
                    currentBatch.vertexCount += 3;
                    texturedFirstVertex += 3;
                } else {
                    throw new IllegalArgumentException("unsupported draw command: "
                            + command.getClass().getName());
                }
            }
            return result;
        }

        private void ensureVertexCapacity(int frameSlot, int requiredBytes,
                                          MemoryStack stack) {
            BufferAllocation current = vertexAllocations[frameSlot];
            if (current != null && vertexCapacities[frameSlot] >= requiredBytes) return;
            if (current != null) destroyBufferAllocation(current);
            int vertexCapacity = 64 * 1024;
            while (vertexCapacity < requiredBytes) {
                vertexCapacity = Math.multiplyExact(vertexCapacity, 2);
            }
            vertexAllocations[frameSlot] = createBufferAllocation(stack, vertexCapacity,
                    VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT
                            | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
            vertexCapacities[frameSlot] = vertexCapacity;
        }

        private void pushShaderState(VkCommandBuffer commandBuffer,
                                     VulkanShaderState shaderState,
                                     MemoryStack stack) {
            ByteBuffer values = stack.malloc(24).order(ByteOrder.nativeOrder());
            values.putFloat(0, shaderState.red());
            values.putFloat(4, shaderState.green());
            values.putFloat(8, shaderState.blue());
            values.putFloat(12, shaderState.alpha());
            values.putFloat(16, shaderState.teamColorAmount());
            values.putInt(20, shaderState.effect());
            vkCmdPushConstants(commandBuffer, texturePipelineLayout,
                    VK_SHADER_STAGE_FRAGMENT_BIT, 0, values);
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
            throw new IllegalStateException("No Vulkan memory type supports flags 0x"
                    + Integer.toHexString(requiredFlags));
        }

        private static float pixelToNdcX(float value, int width) {
            return value * 2.0f / width - 1.0f;
        }

        private static float pixelToNdcY(float value, int height) {
            // Vulkan's positive-height viewport maps NDC -1 to the top of the framebuffer.
            // Screen-space input and scissors are both top-down, so unlike OpenGL this axis
            // must not be inverted here. Inverting only the vertices separates them from their
            // unchanged scissors and can clip every LibRocket draw from the frame.
            return value * 2.0f / height - 1.0f;
        }

        private static void putVertex(FloatBuffer output, VulkanTransform2D transform,
                                      float x, float y, VulkanFrameCommands frame,
                                      VulkanColoredQuad quad) {
            float transformedX = transform.transformX(x, y);
            float transformedY = transform.transformY(x, y);
            output.put(pixelToNdcX(transformedX, frame.width()))
                    .put(pixelToNdcY(transformedY, frame.height()))
                    .put(quad.red()).put(quad.green())
                    .put(quad.blue()).put(quad.alpha());
        }

        private static void putTexturedVertex(FloatBuffer output, VulkanTransform2D transform,
                                              float x, float y, float u, float v,
                                              VulkanFrameCommands frame,
                                              VulkanTexturedQuad quad) {
            float transformedX = transform.transformX(x, y);
            float transformedY = transform.transformY(x, y);
            output.put(pixelToNdcX(transformedX, frame.width()))
                    .put(pixelToNdcY(transformedY, frame.height())).put(u).put(v)
                    .put(quad.red()).put(quad.green()).put(quad.blue()).put(quad.alpha());
        }

        private static void putColoredTriangleVertex(
                FloatBuffer output, VulkanTransform2D transform,
                VulkanFrameCommands frame, VulkanColoredTriangle triangle, int vertex) {
            float x = triangle.x(vertex);
            float y = triangle.y(vertex);
            output.put(pixelToNdcX(transform.transformX(x, y), frame.width()))
                    .put(pixelToNdcY(transform.transformY(x, y), frame.height()))
                    .put(triangle.red(vertex)).put(triangle.green(vertex))
                    .put(triangle.blue(vertex)).put(triangle.alpha(vertex));
        }

        private static void putTexturedTriangleVertex(
                FloatBuffer output, VulkanTransform2D transform,
                VulkanFrameCommands frame, VulkanTexturedTriangle triangle, int vertex) {
            float x = triangle.x(vertex);
            float y = triangle.y(vertex);
            output.put(pixelToNdcX(transform.transformX(x, y), frame.width()))
                    .put(pixelToNdcY(transform.transformY(x, y), frame.height()))
                    .put(triangle.u(vertex)).put(triangle.v(vertex))
                    .put(triangle.red(vertex)).put(triangle.green(vertex))
                    .put(triangle.blue(vertex)).put(triangle.alpha(vertex));
        }

        private boolean setScissor(VkCommandBuffer commandBuffer, VulkanClipRect clip,
                                   MemoryStack stack) {
            return setScissor(commandBuffer, clip, info.width(), info.height(), stack);
        }

        private boolean setScissor(VkCommandBuffer commandBuffer, VulkanClipRect clip,
                                   int targetWidth, int targetHeight, MemoryStack stack) {
            int left = clip == null ? 0 : Math.max(0, (int) Math.floor(clip.x()));
            int top = clip == null ? 0 : Math.max(0, (int) Math.floor(clip.y()));
            int right = clip == null ? targetWidth
                    : Math.min(targetWidth, (int) Math.ceil(clip.x() + clip.width()));
            int bottom = clip == null ? targetHeight
                    : Math.min(targetHeight, (int) Math.ceil(clip.y() + clip.height()));
            if (right <= left || bottom <= top) return false;
            VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack);
            scissor.get(0).offset().x(left).y(top);
            scissor.get(0).extent().width(right - left).height(bottom - top);
            vkCmdSetScissor(commandBuffer, 0, scissor);
            return true;
        }

        private static boolean sameClip(VulkanClipRect first, VulkanClipRect second) {
            return first == second || (first != null && first.equals(second));
        }

        private void recreateSwapchain(int width, int height) {
            check(vkDeviceWaitIdle(device), "vkDeviceWaitIdle(recreate)");
            try (MemoryStack stack = MemoryStack.stackPush()) {
                SwapchainResult replacement = createSwapchain(stack, candidate, device,
                        surface, Math.max(1, width), Math.max(1, height), swapchain,
                        nativeWindowMode);
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
            for (long fence : inFlightFences) {
                if (fence != VK_NULL_HANDLE) vkDestroyFence(device, fence, null);
            }
            inFlightFences = new long[0];
            for (long semaphore : imageAvailableSemaphores) {
                if (semaphore != VK_NULL_HANDLE) vkDestroySemaphore(device, semaphore, null);
            }
            imageAvailableSemaphores = new long[0];
            for (long semaphore : renderFinishedSemaphores) {
                if (semaphore != VK_NULL_HANDLE) vkDestroySemaphore(device, semaphore, null);
            }
            renderFinishedSemaphores = new long[0];
            for (BufferAllocation allocation : vertexAllocations) {
                if (allocation != null) destroyBufferAllocation(allocation);
            }
            vertexAllocations = new BufferAllocation[0];
            vertexCapacities = new int[0];
            if (commandPool != 0L) {
                vkDestroyCommandPool(device, commandPool, null);
                commandPool = 0L;
                commandBuffers = new VkCommandBuffer[0];
            }
            for (long framebuffer : framebuffers) {
                if (framebuffer != 0L) vkDestroyFramebuffer(device, framebuffer, null);
            }
            framebuffers = new long[0];
            for (int index = 0; index < colorPipelines.length; index++) {
                if (colorPipelines[index] != 0L) {
                    vkDestroyPipeline(device, colorPipelines[index], null);
                    colorPipelines[index] = 0L;
                }
            }
            if (pipelineLayout != 0L) {
                vkDestroyPipelineLayout(device, pipelineLayout, null);
                pipelineLayout = 0L;
            }
            for (int index = 0; index < texturePipelines.length; index++) {
                if (texturePipelines[index] != 0L) {
                    vkDestroyPipeline(device, texturePipelines[index], null);
                    texturePipelines[index] = 0L;
                }
            }
            if (texturePipelineLayout != 0L) {
                vkDestroyPipelineLayout(device, texturePipelineLayout, null);
                texturePipelineLayout = 0L;
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
            destroySwapchainResources();
            if (textureDescriptorPool != VK_NULL_HANDLE) {
                vkDestroyDescriptorPool(device, textureDescriptorPool, null);
                textureDescriptorPool = VK_NULL_HANDLE;
            }
            for (TextureResource texture : textures.values()) {
                destroyTextureResource(texture, false);
            }
            textures.clear();
            pendingTextureUploads.clear();
            while (!retiredTextures.isEmpty()) {
                destroyTextureResource(retiredTextures.removeFirst(), false);
            }
            if (textureDescriptorSetLayout != VK_NULL_HANDLE) {
                vkDestroyDescriptorSetLayout(device, textureDescriptorSetLayout, null);
                textureDescriptorSetLayout = VK_NULL_HANDLE;
            }
            if (textureStaging != null) {
                destroyBufferAllocation(textureStaging);
                textureStaging = null;
                textureStagingCapacity = 0;
            }
            if (offscreenLoadRenderPass != VK_NULL_HANDLE) {
                vkDestroyRenderPass(device, offscreenLoadRenderPass, null);
                offscreenLoadRenderPass = VK_NULL_HANDLE;
            }
            if (offscreenClearRenderPass != VK_NULL_HANDLE) {
                vkDestroyRenderPass(device, offscreenClearRenderPass, null);
                offscreenClearRenderPass = VK_NULL_HANDLE;
            }
            vkDestroySwapchainKHR(device, swapchain, null);
            vkDestroyDevice(device, null);
            vkDestroySurfaceKHR(instance, surface, null);
            vkDestroyInstance(instance, null);
            if (overlay != null) overlay.close();
            if (nativeWindow != null) nativeWindow.close();
        }

        private static final class BufferAllocation {
            private long buffer;
            private long memory;
        }

        private static final class TextureResource {
            private long image;
            private long memory;
            private long view;
            private int width;
            private int height;
            private boolean initialized;
            private boolean renderTarget;
            private long framebuffer;
            private final long[] samplers =
                    new long[VulkanTextureFilter.values().length];
            private final long[] descriptorSets =
                    new long[VulkanTextureFilter.values().length];
        }

        private static final class PendingTextureUpload {
            private final VulkanTextureData texture;
            private final boolean initialized;

            private PendingTextureUpload(VulkanTextureData texture, boolean initialized) {
                this.texture = texture;
                this.initialized = initialized;
            }
        }

        private abstract static class DrawBatch {
            final VulkanClipRect clip;
            final VulkanBlendMode blendMode;
            final int firstVertex;
            int vertexCount;

            private DrawBatch(VulkanClipRect clip, VulkanBlendMode blendMode,
                              int firstVertex) {
                this.clip = clip;
                this.blendMode = blendMode;
                this.firstVertex = firstVertex;
            }
        }

        private static final class TextureDrawBatch extends DrawBatch {
            private final long textureHandle;
            private final long descriptorSet;
            private final VulkanShaderState shaderState;

            private TextureDrawBatch(long textureHandle, long descriptorSet,
                                     VulkanClipRect clip, VulkanBlendMode blendMode,
                                     VulkanShaderState shaderState, int firstVertex) {
                super(clip, blendMode, firstVertex);
                this.textureHandle = textureHandle;
                this.descriptorSet = descriptorSet;
                this.shaderState = shaderState;
            }
        }

        private static final class ColoredDrawBatch extends DrawBatch {
            private ColoredDrawBatch(VulkanClipRect clip, VulkanBlendMode blendMode,
                                     int firstVertex) {
                super(clip, blendMode, firstVertex);
            }
        }

        private static final class FrameUpload {
            private final long texturedByteOffset;
            private final List<DrawBatch> batches = new ArrayList<DrawBatch>();

            private FrameUpload(long texturedByteOffset) {
                this.texturedByteOffset = texturedByteOffset;
            }

            private int totalVertexCount() {
                int total = 0;
                for (DrawBatch batch : batches) {
                    total = Math.addExact(total, batch.vertexCount);
                }
                return total;
            }
        }
    }
}
