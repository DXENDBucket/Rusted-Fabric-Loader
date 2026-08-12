package io.github.endx.vulkanmod.lwjgl3;

import io.github.endx.vulkanmod.framestream.DecodedFrameStream;
import io.github.endx.vulkanmod.framestream.FrameResourceHandle;
import io.github.endx.vulkanmod.framestream.FrameStreamRecordFormat;
import io.github.endx.vulkanmod.resourcestream.ResourceStreamFormat;
import io.github.endx.vulkanmod.resourcestream.ResourceStreamReader;
import io.github.endx.vulkanmod.resourcestream.ResourceStreamRecords;
import io.github.endx.vulkanmod.spi.VulkanDeviceInfo;
import io.github.endx.vulkanmod.spi.VulkanBlendMode;
import io.github.endx.vulkanmod.spi.VulkanColoredQuad;
import io.github.endx.vulkanmod.spi.VulkanColoredTriangle;
import io.github.endx.vulkanmod.spi.VulkanClipRect;
import io.github.endx.vulkanmod.spi.VulkanDrawCommand;
import io.github.endx.vulkanmod.spi.VulkanDrawState;
import io.github.endx.vulkanmod.spi.VulkanFrameCommands;
import io.github.endx.vulkanmod.spi.VulkanFrameSubmission;
import io.github.endx.vulkanmod.spi.VulkanRenderTargetPass;
import io.github.endx.vulkanmod.spi.VulkanTextureData;
import io.github.endx.vulkanmod.spi.VulkanTextureFilter;
import io.github.endx.vulkanmod.spi.VulkanTexturedQuad;
import io.github.endx.vulkanmod.spi.VulkanTexturedQuadBatch;
import io.github.endx.vulkanmod.spi.VulkanTexturedTriangle;
import io.github.endx.vulkanmod.spi.VulkanTransform2D;
import io.github.endx.vulkanmod.spi.VulkanPlatformDriver;
import io.github.endx.vulkanmod.spi.VulkanProbeResult;
import io.github.endx.vulkanmod.spi.VulkanSurfaceInfo;
import io.github.endx.vulkanmod.spi.VulkanSurfaceRequest;
import io.github.endx.vulkanmod.spi.VulkanWindowRequest;
import io.github.endx.vulkanmod.spi.VulkanInputEvent;
import io.github.endx.vulkanmod.spi.VulkanShaderState;
import io.github.endx.vulkanmod.spi.VulkanCustomFragmentShader;
import io.github.endx.vulkanmod.spi.VulkanCustomShaderProgram;
import io.github.endx.vulkanmod.spi.VulkanResourceStreamResult;
import io.github.endx.vulkanmod.spi.VulkanResourceArenaRegistration;
import io.github.endx.vulkanmod.spi.VulkanTextRasterizer;
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
import org.lwjgl.vulkan.VkDebugUtilsMessengerCallbackDataEXT;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCallbackEXT;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCreateInfoEXT;
import org.lwjgl.vulkan.VkExtensionProperties;
import org.lwjgl.vulkan.VkFenceCreateInfo;
import org.lwjgl.vulkan.VkFramebufferCreateInfo;
import org.lwjgl.vulkan.VkImageViewCreateInfo;
import org.lwjgl.vulkan.VkImageCreateInfo;
import org.lwjgl.vulkan.VkImageMemoryBarrier;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;
import org.lwjgl.vulkan.VkLayerProperties;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.KHRWin32Surface.*;
import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK11.VK_API_VERSION_1_1;
import static org.lwjgl.util.shaderc.Shaderc.*;

/** LWJGL 3 desktop driver, loaded in a child-first class loader beside the LWJGL 2 game. */
public final class Lwjgl3VulkanDriver implements VulkanPlatformDriver {
    private static final int MAX_PENDING_RESOURCE_STREAMS = 64;
    private AwtTextRasterizer textRasterizer;
    private static final int VERTEX_FLOATS = 6;
    private static final int VERTEX_STRIDE = VERTEX_FLOATS * Float.BYTES;
    private static final int TEXTURED_VERTEX_FLOATS = 8;
    private static final int TEXTURED_VERTEX_STRIDE = TEXTURED_VERTEX_FLOATS * Float.BYTES;
    private static final int CUSTOM_TEXTURED_VERTEX_FLOATS = 16;
    private static final int CUSTOM_TEXTURED_VERTEX_STRIDE =
            CUSTOM_TEXTURED_VERTEX_FLOATS * Float.BYTES;
    private static final int OFFSCREEN_SUBMISSION_SLOTS = 8;
    private static final int MAX_TEXTURES = 8192;
    private static final int MAX_TEXTURE_DESCRIPTOR_SETS =
            MAX_TEXTURES * VulkanTextureFilter.values().length * 2;
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
            + "layout(set=0,binding=1) uniform sampler2D secondaryImage;\n"
            + "layout(push_constant) uniform ShaderState {\n"
            + "  vec4 teamColor;\n"
            + "  float teamColorAmount;\n"
            + "  int effect;\n"
            + "  vec2 resolution;\n"
            + "  float displacementOffset;\n"
            + "  float uiScaling;\n"
            + "  vec2 screenBaseSize;\n"
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
            + "  }else if(shaderState.effect==5){\n"
            + "    vec2 usedScreenSize=shaderState.resolution/shaderState.screenBaseSize;\n"
            + "    vec2 screenUv=gl_FragCoord.xy/(shaderState.resolution*shaderState.uiScaling);\n"
            + "    screenUv*=usedScreenSize;\n"
            + "    vec2 screenOffset=shaderState.displacementOffset\n"
            + "      *(sampled.xy-vec2(128.0/255.0))*sampled.a*color.a;\n"
            + "    vec2 displacedUv=clamp(screenUv+screenOffset,vec2(0.0),usedScreenSize);\n"
            + "    outColor=texture(secondaryImage,displacedUv);\n"
            + "    return;\n"
            + "  }else if(shaderState.effect==6){\n"
            + "    sampled.rgb=vec3(1.0,1.0,0.0);\n"
            + "  }\n"
            + "  outColor=sampled*color;\n"
            + "}\n";
    private SurfaceSession surfaceSession;
    private final Map<Long, ByteBuffer> resourceUploadArenas =
            new LinkedHashMap<Long, ByteBuffer>();
    private final Map<Long, Integer> resourceArenaDecodeReferences =
            new LinkedHashMap<Long, Integer>();
    private final Map<Long, Set<Long>> resourceCompletionArenas =
            new LinkedHashMap<Long, Set<Long>>();
    private final Map<Long, VulkanResourceStreamResult> readyResourceCompletions =
            new LinkedHashMap<Long, VulkanResourceStreamResult>();
    private final Map<Long, Throwable> failedResourceCompletions =
            new LinkedHashMap<Long, Throwable>();
    private final Set<Long> outstandingResourceCompletions =
            new LinkedHashSet<Long>();
    private final Semaphore resourceDecodeSlots =
            new Semaphore(MAX_PENDING_RESOURCE_STREAMS, true);
    private long acceptedResourceSequence;
    private long decodedResourceSequence;
    private long resourceStreamsAccepted;
    private int pendingResourceStreams;
    private int peakPendingResourceStreams;
    private long resourceDecodeNanos;
    private long resourceDecodeBackpressureWaits;
    private long resourceDecodeBackpressureNanos;
    private long frameResourceDependencyWaits;
    private long frameResourceDependencyWaitNanos;
    private Throwable resourceDecodeFault;
    private final ExecutorService resourceDecodeExecutor =
            Executors.newSingleThreadExecutor(task -> {
                Thread worker = new Thread(task, "RustedVK resource decoder");
                worker.setDaemon(true);
                return worker;
            });
    private final ExecutorService resourceCompletionExecutor =
            Executors.newSingleThreadExecutor(task -> {
                Thread worker = new Thread(task, "RustedVK resource completion");
                worker.setDaemon(true);
                return worker;
            });

    @Override public String name() { return "LWJGL 3 Vulkan"; }

    @Override public synchronized VulkanTextRasterizer createTextRasterizer() {
        if (textRasterizer == null) textRasterizer = new AwtTextRasterizer();
        return textRasterizer;
    }

    @Override public VulkanProbeResult probe() {
        try {
            int instanceVersion = VK.getInstanceVersionSupported();
            try (MemoryStack stack = MemoryStack.stackPush()) {
                VkInstanceCreateInfo createInfo = instanceCreateInfo(
                        stack, instanceVersion, false, false);
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
            ValidationConfig validation = validationConfig(stack);
            VkInstanceCreateInfo instanceInfo = instanceCreateInfo(
                    stack, instanceVersion, true, validation.enabled);
            VkInstance instance = createInstance(stack, instanceInfo);
            ValidationDebugMessenger debugMessenger = validation.enabled
                    ? ValidationDebugMessenger.create(stack, instance, validation.verbose) : null;
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
                        swapchainResult, overlay, nativeWindow, debugMessenger);
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
                    if (debugMessenger != null) debugMessenger.close(instance);
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
        awaitDecodedResourceSequence(acceptedResourceSequence);
        return surfaceSession.presentFrame(frame);
    }

    @Override public synchronized VulkanSurfaceInfo presentFrameAndReveal(
            VulkanFrameCommands frame) {
        if (surfaceSession == null) {
            throw new IllegalStateException("Vulkan surface has not been created");
        }
        if (frame == null) throw new NullPointerException("frame");
        awaitDecodedResourceSequence(acceptedResourceSequence);
        return surfaceSession.presentFrameAndReveal(frame);
    }

    @Override public synchronized long compileFragmentShader(
            VulkanCustomFragmentShader shader) {
        if (surfaceSession == null) {
            throw new IllegalStateException("Vulkan surface is not initialized");
        }
        awaitDecodedResourceSequence(acceptedResourceSequence);
        return surfaceSession.compileFragmentShader(shader);
    }

    @Override public synchronized void destroyFragmentShader(long shaderHandle) {
        if (surfaceSession != null) {
            awaitDecodedResourceSequence(acceptedResourceSequence);
            surfaceSession.destroyFragmentShader(shaderHandle);
        }
    }

    @Override public synchronized long compileShaderProgram(
            VulkanCustomShaderProgram program) {
        if (surfaceSession == null) {
            throw new IllegalStateException("Vulkan surface is not initialized");
        }
        awaitDecodedResourceSequence(acceptedResourceSequence);
        return surfaceSession.compileShaderProgram(program);
    }

    @Override public synchronized void destroyShaderProgram(long shaderHandle) {
        if (surfaceSession != null) {
            awaitDecodedResourceSequence(acceptedResourceSequence);
            surfaceSession.destroyShaderProgram(shaderHandle);
        }
    }

    @Override public boolean supportsFrameStream() { return true; }
    @Override public boolean supportsResourceStream() { return true; }

    @Override public synchronized boolean customShaderUsesExpandedVertexInput(long shaderHandle) {
        if (surfaceSession == null) {
            throw new IllegalStateException("Vulkan surface is not initialized");
        }
        awaitDecodedResourceSequence(acceptedResourceSequence);
        return surfaceSession.customShaderUsesExpandedVertexInput(shaderHandle);
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
        awaitDecodedResourceSequence(acceptedResourceSequence);
        return surfaceSession.uploadTexture(texture);
    }

    @Override public synchronized void updateTexture(long textureHandle,
                                                     VulkanTextureData texture) {
        if (surfaceSession == null) {
            throw new IllegalStateException("Vulkan surface has not been created");
        }
        if (texture == null) throw new NullPointerException("texture");
        awaitDecodedResourceSequence(acceptedResourceSequence);
        surfaceSession.updateTexture(textureHandle, texture);
    }

    @Override public synchronized void updateTextureRegion(long textureHandle, int x, int y,
                                                            VulkanTextureData texture) {
        if (surfaceSession == null) {
            throw new IllegalStateException("Vulkan surface has not been created");
        }
        if (texture == null) throw new NullPointerException("texture");
        awaitDecodedResourceSequence(acceptedResourceSequence);
        surfaceSession.updateTextureRegion(textureHandle, x, y, texture);
    }

    @Override public synchronized VulkanTextureData readTexture(long textureHandle) {
        if (surfaceSession == null) {
            throw new IllegalStateException("Vulkan surface has not been created");
        }
        awaitDecodedResourceSequence(acceptedResourceSequence);
        return surfaceSession.readTexture(textureHandle);
    }

    @Override public synchronized VulkanTextureData readTextureRegion(long textureHandle,
                                                                       int x, int y,
                                                                       int width, int height) {
        if (surfaceSession == null) {
            throw new IllegalStateException("Vulkan surface has not been created");
        }
        awaitDecodedResourceSequence(acceptedResourceSequence);
        return surfaceSession.readTextureRegion(textureHandle, x, y, width, height);
    }

    @Override public synchronized long createRenderTarget(int width, int height) {
        if (surfaceSession == null) {
            throw new IllegalStateException("Vulkan surface has not been created");
        }
        awaitDecodedResourceSequence(acceptedResourceSequence);
        return surfaceSession.createRenderTarget(width, height);
    }

    @Override public synchronized void renderToTexture(
            long textureHandle, VulkanFrameCommands frame) {
        if (surfaceSession == null) {
            throw new IllegalStateException("Vulkan surface has not been created");
        }
        if (frame == null) throw new NullPointerException("frame");
        awaitDecodedResourceSequence(acceptedResourceSequence);
        surfaceSession.renderToTexture(textureHandle, frame);
    }

    @Override public synchronized VulkanSurfaceInfo presentFrame(
            VulkanFrameSubmission submission) {
        if (surfaceSession == null) {
            throw new IllegalStateException("Vulkan surface has not been created");
        }
        if (submission == null) throw new NullPointerException("submission");
        awaitDecodedResourceSequence(acceptedResourceSequence);
        return surfaceSession.presentFrame(submission);
    }

    @Override public synchronized VulkanSurfaceInfo presentFrameStream(ByteBuffer frameStream) {
        if (surfaceSession == null) {
            throw new IllegalStateException("Vulkan surface has not been created");
        }
        if (frameStream == null) throw new NullPointerException("frameStream");
        DecodedFrameStream decoded = DecodedFrameStream.decode(frameStream);
        awaitDecodedResourceSequence(decoded.requiredResourceSequence());
        return surfaceSession.presentFrame(decoded);
    }

    @Override public VulkanResourceStreamResult submitResourceStream(
            ByteBuffer resourceStream) {
        if (resourceStream == null) throw new NullPointerException("resourceStream");
        ResourceStreamReader decoded = ResourceStreamReader.read(resourceStream);
        acquireResourceDecodeSlot();
        synchronized (this) {
            try {
                if (surfaceSession == null) throw new IllegalStateException(
                        "Vulkan surface has not been created");
                throwResourceDecodeFault();
                long expected = Math.addExact(acceptedResourceSequence, 1L);
                if (decoded.firstSequence() != expected) throw new IllegalArgumentException(
                        "ResourceStream starts at " + decoded.firstSequence()
                                + " but desktop accepted through "
                                + acceptedResourceSequence);
                Set<Long> referencedArenas = referencedResourceArenas(decoded);
                long completionId = decoded.completionId();
                if (completionId != 0L
                        && !outstandingResourceCompletions.add(completionId)) {
                    throw new IllegalArgumentException("duplicate outstanding completion ID");
                }
                if (completionId != 0L && !referencedArenas.isEmpty()) {
                    resourceCompletionArenas.put(completionId, referencedArenas);
                }
                acceptedResourceSequence = decoded.lastSequence();
                retainResourceArenas(referencedArenas);
                try {
                    enqueueResourceDecode(decoded, referencedArenas);
                } catch (RuntimeException | Error rejected) {
                    releaseResourceArenas(referencedArenas);
                    throw rejected;
                }
                pendingResourceStreams++;
                peakPendingResourceStreams = Math.max(
                        peakPendingResourceStreams, pendingResourceStreams);
                if (++resourceStreamsAccepted == 1L) {
                    System.out.println("[Vulkan Mod/Driver] Asynchronous ResourceStream decoder "
                            + "active (ordered, maxPending="
                            + MAX_PENDING_RESOURCE_STREAMS + ")");
                }
                return completionId == 0L
                        ? VulkanResourceStreamResult.applied(acceptedResourceSequence)
                        : VulkanResourceStreamResult.pending(
                                acceptedResourceSequence, completionId);
            } catch (RuntimeException | Error failure) {
                resourceDecodeSlots.release();
                throw failure;
            }
        }
    }

    private void acquireResourceDecodeSlot() {
        if (resourceDecodeSlots.tryAcquire()) return;
        long started = System.nanoTime();
        try {
            resourceDecodeSlots.acquire();
            synchronized (this) {
                resourceDecodeBackpressureWaits++;
                resourceDecodeBackpressureNanos += System.nanoTime() - started;
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "interrupted by ResourceStream decoder back-pressure", interrupted);
        }
    }

    private Set<Long> referencedResourceArenas(ResourceStreamReader stream) {
        LinkedHashSet<Long> referenced = new LinkedHashSet<Long>();
        for (int index = 0; index < stream.recordCount(); index++) {
            ResourceStreamReader.Record record = stream.record(index);
            if ((record.flags() & ResourceStreamFormat.RECORD_HAS_EXTERNAL_PAYLOAD) == 0) {
                continue;
            }
            long arenaId = ResourceStreamRecords.decodeTextureTransfer(stream, index).arenaId;
            if (!resourceUploadArenas.containsKey(arenaId)) throw new IllegalArgumentException(
                    "unknown external resource arena " + arenaId);
            referenced.add(arenaId);
        }
        return referenced;
    }

    private void retainResourceArenas(Set<Long> arenas) {
        for (long arenaId : arenas) {
            Integer references = resourceArenaDecodeReferences.get(arenaId);
            resourceArenaDecodeReferences.put(arenaId,
                    references == null ? 1 : Math.addExact(references, 1));
        }
    }

    private void releaseResourceArenas(Set<Long> arenas) {
        for (long arenaId : arenas) {
            Integer references = resourceArenaDecodeReferences.get(arenaId);
            if (references == null || references <= 0) throw new IllegalStateException(
                    "resource arena decode reference underflow");
            if (references == 1) resourceArenaDecodeReferences.remove(arenaId);
            else resourceArenaDecodeReferences.put(arenaId, references - 1);
        }
    }

    private void enqueueResourceDecode(ResourceStreamReader stream, Set<Long> referencedArenas) {
        try {
            resourceDecodeExecutor.execute(
                    () -> decodeResourceStream(stream, referencedArenas));
        } catch (RuntimeException rejected) {
            if (stream.completionId() != 0L) {
                outstandingResourceCompletions.remove(stream.completionId());
                resourceCompletionArenas.remove(stream.completionId());
            }
            resourceDecodeFault = rejected;
            throw rejected;
        }
    }

    private void decodeResourceStream(ResourceStreamReader stream,
                                      Set<Long> referencedArenas) {
        long started = System.nanoTime();
        synchronized (this) {
            try {
                if (surfaceSession == null) throw new IllegalStateException(
                        "Vulkan surface closed before ResourceStream decode");
                VulkanResourceStreamResult result = surfaceSession.submitResourceStream(
                        stream, resourceUploadArenas);
                if (result.appliedSequence() != stream.lastSequence()) {
                    throw new IllegalStateException("ResourceStream decoder applied through "
                            + result.appliedSequence() + " but accepted through "
                            + stream.lastSequence());
                }
                decodedResourceSequence = result.appliedSequence();
                if (stream.completionId() != 0L) {
                    if (result.completionPending()) {
                        scheduleResourceCompletion(stream.completionId());
                    } else {
                        readyResourceCompletions.put(stream.completionId(), result);
                    }
                }
            } catch (Throwable failure) {
                resourceDecodeFault = failure;
                if (stream.completionId() != 0L) {
                    failedResourceCompletions.put(stream.completionId(), failure);
                }
            } finally {
                try {
                    releaseResourceArenas(referencedArenas);
                } finally {
                    resourceDecodeNanos += System.nanoTime() - started;
                    pendingResourceStreams--;
                    resourceDecodeSlots.release();
                    notifyAll();
                }
            }
        }
    }

    private void awaitDecodedResourceSequence(long requiredSequence) {
        if (requiredSequence < 0L) throw new IllegalArgumentException(
                "negative required resource sequence");
        if (requiredSequence > acceptedResourceSequence) throw new IllegalStateException(
                "frame requires resource sequence " + requiredSequence
                        + " but desktop accepted through " + acceptedResourceSequence);
        long waitStarted = decodedResourceSequence < requiredSequence
                ? System.nanoTime() : 0L;
        while (decodedResourceSequence < requiredSequence) {
            throwResourceDecodeFault();
            try {
                wait();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(
                        "interrupted while waiting for ResourceStream decode", interrupted);
            }
        }
        if (waitStarted != 0L) {
            frameResourceDependencyWaits++;
            frameResourceDependencyWaitNanos += System.nanoTime() - waitStarted;
        }
        throwResourceDecodeFault();
    }

    private void throwResourceDecodeFault() {
        if (resourceDecodeFault != null) throw new IllegalStateException(
                "asynchronous ResourceStream decoder failed", resourceDecodeFault);
    }

    private void scheduleResourceCompletion(long completionId) {
        try {
            resourceCompletionExecutor.execute(() -> {
                synchronized (Lwjgl3VulkanDriver.this) {
                    try {
                        if (surfaceSession == null) throw new IllegalStateException(
                                "Vulkan surface closed before resource completion");
                        VulkanResourceStreamResult result =
                                surfaceSession.completeResourceStream(completionId);
                        readyResourceCompletions.put(completionId, result);
                    } catch (Throwable failure) {
                        failedResourceCompletions.put(completionId, failure);
                    } finally {
                        Lwjgl3VulkanDriver.this.notifyAll();
                    }
                }
            });
        } catch (RuntimeException rejected) {
            outstandingResourceCompletions.remove(completionId);
            throw rejected;
        }
    }

    @Override public synchronized VulkanResourceStreamResult pollResourceStreamCompletion(
            long completionId) {
        requireCompletionId(completionId);
        throwCompletionFailure(completionId);
        VulkanResourceStreamResult result = readyResourceCompletions.remove(completionId);
        if (result != null) {
            outstandingResourceCompletions.remove(completionId);
            resourceCompletionArenas.remove(completionId);
            return result;
        }
        if (!outstandingResourceCompletions.contains(completionId)) {
            throw new IllegalArgumentException("unknown resource completion ID " + completionId);
        }
        return null;
    }

    @Override public synchronized VulkanResourceStreamResult awaitResourceStreamCompletion(
            long completionId, long timeoutNanos) {
        requireCompletionId(completionId);
        if (timeoutNanos < -1L) throw new IllegalArgumentException("invalid completion timeout");
        long started = timeoutNanos < 0L ? 0L : System.nanoTime();
        for (;;) {
            throwCompletionFailure(completionId);
            VulkanResourceStreamResult result = readyResourceCompletions.remove(completionId);
            if (result != null) {
                outstandingResourceCompletions.remove(completionId);
                resourceCompletionArenas.remove(completionId);
                return result;
            }
            if (!outstandingResourceCompletions.contains(completionId)) {
                throw new IllegalArgumentException(
                        "unknown resource completion ID " + completionId);
            }
            if (timeoutNanos == 0L) return null;
            try {
                if (timeoutNanos < 0L) {
                    wait();
                } else {
                    long elapsed = System.nanoTime() - started;
                    long remaining = timeoutNanos - elapsed;
                    if (remaining <= 0L) return null;
                    long millis = remaining / 1_000_000L;
                    int nanos = (int) (remaining % 1_000_000L);
                    wait(millis, nanos);
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(
                        "interrupted while waiting for resource completion", interrupted);
            }
        }
    }

    private static void requireCompletionId(long completionId) {
        if (completionId <= 0L) throw new IllegalArgumentException(
                "completion ID must be positive");
    }

    private void throwCompletionFailure(long completionId) {
        Throwable failure = failedResourceCompletions.remove(completionId);
        if (failure != null) {
            outstandingResourceCompletions.remove(completionId);
            resourceCompletionArenas.remove(completionId);
            throw new IllegalStateException(
                    "asynchronous ResourceStream completion failed", failure);
        }
    }

    @Override public synchronized VulkanResourceArenaRegistration registerResourceUploadArena(
            long arenaId, ByteBuffer memory) {
        if (arenaId <= 0L) throw new IllegalArgumentException("resource arena ID must be positive");
        if (memory == null || !memory.isDirect()) {
            throw new IllegalArgumentException("resource arena must be a direct buffer");
        }
        if (resourceUploadArenas.containsKey(arenaId)) {
            throw new IllegalArgumentException("duplicate resource arena ID " + arenaId);
        }
        ByteBuffer registered = memory.duplicate().order(ByteOrder.LITTLE_ENDIAN);
        registered.clear();
        resourceUploadArenas.put(arenaId, registered.asReadOnlyBuffer()
                .order(ByteOrder.LITTLE_ENDIAN));
        return new VulkanResourceArenaRegistration(arenaId, registered.capacity(),
                MemoryUtil.memAddress(registered));
    }

    @Override public synchronized void unregisterResourceUploadArena(long arenaId) {
        Integer references = resourceArenaDecodeReferences.get(arenaId);
        if (references != null && references > 0) throw new IllegalStateException(
                "resource arena " + arenaId + " is still referenced by "
                        + references + " queued decode(s)");
        for (Set<Long> arenas : resourceCompletionArenas.values()) {
            if (arenas.contains(arenaId)) throw new IllegalStateException(
                    "resource arena " + arenaId
                            + " is awaiting consumption completion acknowledgement");
        }
        if (resourceUploadArenas.remove(arenaId) == null) {
            throw new IllegalArgumentException("unknown resource arena ID " + arenaId);
        }
    }

    @Override public synchronized Map<String, Long> performanceStatistics() {
        LinkedHashMap<String, Long> statistics = new LinkedHashMap<String, Long>();
        statistics.put("resource.accepted", resourceStreamsAccepted);
        statistics.put("resource.decoded", surfaceSession == null
                ? 0L : surfaceSession.resourceStreamSubmissions);
        statistics.put("resource.pending", (long) pendingResourceStreams);
        statistics.put("resource.pendingPeak", (long) peakPendingResourceStreams);
        statistics.put("resource.decodeNanos", resourceDecodeNanos);
        statistics.put("resource.backpressureWaits", resourceDecodeBackpressureWaits);
        statistics.put("resource.backpressureNanos", resourceDecodeBackpressureNanos);
        statistics.put("resource.frameDependencyWaits", frameResourceDependencyWaits);
        statistics.put("resource.frameDependencyWaitNanos",
                frameResourceDependencyWaitNanos);
        if (surfaceSession != null) surfaceSession.appendPerformanceStatistics(statistics);
        return Collections.unmodifiableMap(statistics);
    }

    @Override public synchronized void destroyTexture(long textureHandle) {
        if (surfaceSession != null) {
            awaitDecodedResourceSequence(acceptedResourceSequence);
            surfaceSession.destroyTexture(textureHandle);
        }
    }

    @Override public synchronized void close() {
        RuntimeException decodeFailure = null;
        try {
            awaitDecodedResourceSequence(acceptedResourceSequence);
        } catch (RuntimeException failure) {
            decodeFailure = failure;
        }
        resourceDecodeExecutor.shutdownNow();
        if (surfaceSession != null) {
            surfaceSession.close();
            surfaceSession = null;
        }
        resourceUploadArenas.clear();
        resourceArenaDecodeReferences.clear();
        resourceCompletionArenas.clear();
        readyResourceCompletions.clear();
        failedResourceCompletions.clear();
        outstandingResourceCompletions.clear();
        resourceCompletionExecutor.shutdownNow();
        if (textRasterizer != null) {
            textRasterizer.close();
            textRasterizer = null;
        }
        notifyAll();
        if (decodeFailure != null) throw decodeFailure;
    }

    synchronized long frameGraphQueueSubmissionCount() {
        return surfaceSession == null ? 0L : surfaceSession.frameGraphQueueSubmissions;
    }

    synchronized long immediateOffscreenQueueSubmissionCount() {
        return surfaceSession == null ? 0L : surfaceSession.immediateOffscreenQueueSubmissions;
    }

    synchronized long frameGraphPassCount() {
        return surfaceSession == null ? 0L : surfaceSession.frameGraphPassesSubmitted;
    }

    synchronized long frameUploadAllocationCount() {
        return surfaceSession == null ? 0L : surfaceSession.frameUploadAllocations;
    }

    synchronized long drawBatchAllocationCount() {
        return surfaceSession == null ? 0L : surfaceSession.drawBatchAllocations;
    }

    private static VkInstanceCreateInfo instanceCreateInfo(
            MemoryStack stack, int instanceVersion, boolean surfaceExtensions,
            boolean validation) {
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
            result.ppEnabledExtensionNames(validation
                    ? stack.pointers(stack.UTF8(VK_KHR_SURFACE_EXTENSION_NAME),
                            stack.UTF8(VK_KHR_WIN32_SURFACE_EXTENSION_NAME),
                            stack.UTF8(VK_EXT_DEBUG_UTILS_EXTENSION_NAME))
                    : stack.pointers(stack.UTF8(VK_KHR_SURFACE_EXTENSION_NAME),
                            stack.UTF8(VK_KHR_WIN32_SURFACE_EXTENSION_NAME)));
        } else if (validation) {
            result.ppEnabledExtensionNames(
                    stack.pointers(stack.UTF8(VK_EXT_DEBUG_UTILS_EXTENSION_NAME)));
        }
        if (validation) {
            result.ppEnabledLayerNames(
                    stack.pointers(stack.UTF8("VK_LAYER_KHRONOS_validation")));
        }
        return result;
    }

    private static ValidationConfig validationConfig(MemoryStack stack) {
        if (!Boolean.getBoolean("rusted.fabric.vulkan.validation")) {
            return ValidationConfig.DISABLED;
        }
        boolean layer = hasInstanceLayer(stack, "VK_LAYER_KHRONOS_validation");
        boolean extension = hasInstanceExtension(stack, VK_EXT_DEBUG_UTILS_EXTENSION_NAME);
        if (!layer || !extension) {
            System.out.println("[Vulkan Mod/Validation] Requested but unavailable: layer="
                    + layer + ", debugUtils=" + extension);
            return ValidationConfig.DISABLED;
        }
        boolean verbose = Boolean.getBoolean("rusted.fabric.vulkan.validationVerbose");
        System.out.println("[Vulkan Mod/Validation] VK_LAYER_KHRONOS_validation enabled"
                + (verbose ? " (verbose)" : ""));
        return new ValidationConfig(true, verbose);
    }

    private static boolean hasInstanceLayer(MemoryStack stack, String name) {
        IntBuffer count = stack.ints(0);
        check(vkEnumerateInstanceLayerProperties(count, null),
                "vkEnumerateInstanceLayerProperties(count)");
        VkLayerProperties.Buffer properties = VkLayerProperties.calloc(count.get(0), stack);
        check(vkEnumerateInstanceLayerProperties(count, properties),
                "vkEnumerateInstanceLayerProperties(values)");
        for (int index = 0; index < properties.remaining(); index++) {
            if (name.equals(properties.get(index).layerNameString())) return true;
        }
        return false;
    }

    private static boolean hasInstanceExtension(MemoryStack stack, String name) {
        IntBuffer count = stack.ints(0);
        check(vkEnumerateInstanceExtensionProperties((ByteBuffer) null, count, null),
                "vkEnumerateInstanceExtensionProperties(count)");
        VkExtensionProperties.Buffer properties = VkExtensionProperties.calloc(count.get(0), stack);
        check(vkEnumerateInstanceExtensionProperties((ByteBuffer) null, count, properties),
                "vkEnumerateInstanceExtensionProperties(values)");
        for (int index = 0; index < properties.remaining(); index++) {
            if (name.equals(properties.get(index).extensionNameString())) return true;
        }
        return false;
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

    private static final class ValidationConfig {
        private static final ValidationConfig DISABLED = new ValidationConfig(false, false);
        private final boolean enabled;
        private final boolean verbose;

        private ValidationConfig(boolean enabled, boolean verbose) {
            this.enabled = enabled;
            this.verbose = verbose;
        }
    }

    private static final class ValidationDebugMessenger {
        private final long handle;
        private final VkDebugUtilsMessengerCallbackEXT callback;

        private ValidationDebugMessenger(long handle,
                                         VkDebugUtilsMessengerCallbackEXT callback) {
            this.handle = handle;
            this.callback = callback;
        }

        private static ValidationDebugMessenger create(MemoryStack stack, VkInstance instance,
                                                       boolean verbose) {
            VkDebugUtilsMessengerCallbackEXT callback =
                    VkDebugUtilsMessengerCallbackEXT.create((severity, types, callbackData, user) -> {
                        String level = (severity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT) != 0
                                ? "ERROR"
                                : (severity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT) != 0
                                ? "WARN"
                                : (severity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT) != 0
                                ? "INFO" : "VERBOSE";
                        String message = VkDebugUtilsMessengerCallbackDataEXT
                                .create(callbackData).pMessageString();
                        System.err.println("[Vulkan Mod/Validation/" + level + "] " + message);
                        return VK_FALSE;
                    });
            int severities = VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT
                    | VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT;
            if (verbose) {
                severities |= VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT
                        | VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT;
            }
            VkDebugUtilsMessengerCreateInfoEXT info = VkDebugUtilsMessengerCreateInfoEXT
                    .calloc(stack).sType$Default()
                    .messageSeverity(severities)
                    .messageType(VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT
                            | VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT
                            | VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT)
                    .pfnUserCallback(callback);
            LongBuffer handle = stack.mallocLong(1);
            try {
                check(vkCreateDebugUtilsMessengerEXT(instance, info, null, handle),
                        "vkCreateDebugUtilsMessengerEXT");
                return new ValidationDebugMessenger(handle.get(0), callback);
            } catch (Throwable failure) {
                callback.free();
                throw failure;
            }
        }

        private void close(VkInstance instance) {
            vkDestroyDebugUtilsMessengerEXT(instance, handle, null);
            callback.free();
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
        private final ValidationDebugMessenger debugMessenger;
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
        private final Map<Long, CustomShaderResource> customShaders =
                new LinkedHashMap<Long, CustomShaderResource>();
        private long nextCustomShaderHandle = 1L;
        private long textureDescriptorSetLayout;
        private long textureDescriptorPool;
        private final Map<Long, TextureResource> textures =
                new LinkedHashMap<Long, TextureResource>();
        private final Map<TextureDescriptorKey, Long> pairedTextureDescriptors =
                new LinkedHashMap<TextureDescriptorKey, Long>();
        private final ArrayList<PendingTextureUpload> pendingTextureUploads =
                new ArrayList<PendingTextureUpload>();
        private final Map<Long, ResourceTextureBinding> resourceTextures =
                new LinkedHashMap<Long, ResourceTextureBinding>();
        private final Map<Long, ResourceStreamRecords.TextureDescriptor>
                pendingResourceTextures =
                new LinkedHashMap<Long, ResourceStreamRecords.TextureDescriptor>();
        private final Map<Long, Long> resourceShaders = new LinkedHashMap<Long, Long>();
        private final Map<Long, PendingResourceReadback> pendingResourceReadbacks =
                new LinkedHashMap<Long, PendingResourceReadback>();
        private long appliedResourceSequence;
        private boolean resourceStreamActive;
        private Throwable resourceStreamFault;
        private long resourceStreamSubmissions;
        private long externalResourceTransfers;
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
        private VkCommandBuffer[] offscreenCommandBuffers = new VkCommandBuffer[0];
        private long[] offscreenFences = new long[0];
        /** Fence of the submission that most recently used each offscreen upload slot. */
        private long[] offscreenSlotOwnerFences = new long[0];
        private BufferAllocation[] offscreenVertexAllocations = new BufferAllocation[0];
        private int[] offscreenVertexCapacities = new int[0];
        private int offscreenCursor;
        private int frameCursor;
        private final ArrayDeque<FrameUpload> frameUploadPool = new ArrayDeque<FrameUpload>();
        private final ArrayDeque<ColoredDrawBatch> coloredBatchPool =
                new ArrayDeque<ColoredDrawBatch>();
        private final ArrayDeque<TextureDrawBatch> textureBatchPool =
                new ArrayDeque<TextureDrawBatch>();
        /** Persistently mapped staging allocation owned by each frame/offscreen/readback slot. */
        private BufferAllocation[] textureUploadSlots = new BufferAllocation[0];
        private int[] textureUploadCapacities = new int[0];
        private long textureUploadBytes;
        private long textureUploadBatches;
        private long textureUploadSlotGrowths;
        private long textureMutationFenceWaits;
        private VulkanShaderState[] decodedMaterialShaders = new VulkanShaderState[0];
        private long[] decodedMaterialSecondaryTextures = new long[0];
        private int[] decodedMaterialEpochs = new int[0];
        private int decodedMaterialEpoch;
        private long decodedMaterialCacheHits;
        private long decodedMaterialCacheMisses;
        private long acquireSkips;
        private long fenceWaitSkips;
        private long successfulPresents;
        private long profiledSlowFrames;
        private long renderTargetSubmissions;
        private long immediateOffscreenQueueSubmissions;
        private long frameGraphQueueSubmissions;
        private long frameGraphPassesSubmitted;
        private long frameUploadAllocations;
        private long drawBatchAllocations;
        private List<FrameSource> frameGraphPasses = Collections.emptyList();
        private boolean frameGraphSubmitted;
        private boolean debugLargeTargetReadBack;
        private boolean debugMainTargetSamplesLogged;
        private boolean closed;

        private abstract class FrameSource {
            private final long targetHandle;

            private FrameSource(long targetHandle) { this.targetHandle = targetHandle; }
            long targetHandle() { return targetHandle; }
            abstract int width();
            abstract int height();
            abstract float clearRed();
            abstract float clearGreen();
            abstract float clearBlue();
            abstract float clearAlpha();
            abstract boolean clearRequested();
            abstract int drawCount();
            abstract FrameUpload upload(MemoryStack stack, int slot,
                                        BufferAllocation[] allocations, int[] capacities);
        }

        private final class ObjectFrameSource extends FrameSource {
            private final VulkanFrameCommands frame;

            private ObjectFrameSource(long targetHandle, VulkanFrameCommands frame) {
                super(targetHandle);
                this.frame = frame;
            }

            @Override int width() { return frame.width(); }
            @Override int height() { return frame.height(); }
            @Override float clearRed() { return frame.clearRed(); }
            @Override float clearGreen() { return frame.clearGreen(); }
            @Override float clearBlue() { return frame.clearBlue(); }
            @Override float clearAlpha() { return frame.clearAlpha(); }
            @Override boolean clearRequested() { return frame.clearRequested(); }
            @Override int drawCount() { return frame.commandCount(); }
            @Override FrameUpload upload(MemoryStack stack, int slot,
                                         BufferAllocation[] allocations, int[] capacities) {
                return uploadFrame(frame, stack, slot, allocations, capacities);
            }
        }

        private final class StreamFrameSource extends FrameSource {
            private final DecodedFrameStream stream;
            private final int passIndex;
            private final DecodedFrameStream.Pass pass;

            private StreamFrameSource(DecodedFrameStream stream, int passIndex) {
                super(rawTextureHandle(stream.pass(passIndex).targetHandle(), true));
                this.stream = stream;
                this.passIndex = passIndex;
                this.pass = stream.pass(passIndex);
            }

            @Override int width() { return pass.viewportWidth(); }
            @Override int height() { return pass.viewportHeight(); }
            @Override float clearRed() { return pass.clearRed(); }
            @Override float clearGreen() { return pass.clearGreen(); }
            @Override float clearBlue() { return pass.clearBlue(); }
            @Override float clearAlpha() { return pass.clearAlpha(); }
            @Override boolean clearRequested() {
                return (pass.flags() & FrameStreamRecordFormat.PASS_CLEAR_COLOR) != 0;
            }
            @Override int drawCount() { return pass.batchCount(); }
            @Override FrameUpload upload(MemoryStack stack, int slot,
                                         BufferAllocation[] allocations, int[] capacities) {
                return uploadFrame(stream, passIndex, stack, slot, allocations, capacities);
            }
        }

        private SurfaceSession(VkInstance instance, long surface, DeviceCandidate candidate,
                               VkDevice device, SwapchainResult swapchain,
                               Win32OverlayWindow overlay, boolean nativeWindowMode,
                               ValidationDebugMessenger debugMessenger) {
            this.instance = instance;
            this.surface = surface;
            this.candidate = candidate;
            this.device = device;
            this.swapchain = swapchain.handle;
            this.images = swapchain.images;
            this.info = swapchain.info;
            this.overlay = overlay;
            this.nativeWindowMode = nativeWindowMode;
            this.debugMessenger = debugMessenger;
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

        private long compileFragmentShader(VulkanCustomFragmentShader shader) {
            if (shader == null) throw new NullPointerException("shader");
            try (MemoryStack stack = MemoryStack.stackPush()) {
                long module = createShaderModule(stack, shader.source(),
                        shaderc_glsl_fragment_shader, shader.name() + ".frag");
                vkDestroyShaderModule(device, module, null);
            }
            long handle = nextCustomShaderHandle++;
            if (handle <= 0L) throw new IllegalStateException("custom shader handles exhausted");
            customShaders.put(handle, new CustomShaderResource(shader.name(),
                    TEXTURE_VERTEX_SHADER, shader.source(), false));
            return handle;
        }

        private void destroyFragmentShader(long shaderHandle) {
            CustomShaderResource shader = customShaders.remove(shaderHandle);
            if (shader != null) {
                check(vkDeviceWaitIdle(device), "vkDeviceWaitIdle(destroy custom shader)");
                shader.destroy(device);
            }
        }

        private long compileShaderProgram(VulkanCustomShaderProgram program) {
            if (program == null) throw new NullPointerException("program");
            try (MemoryStack stack = MemoryStack.stackPush()) {
                long vertexModule = createShaderModule(stack, program.vertexSource(),
                        shaderc_glsl_vertex_shader, program.name() + ".vert");
                long fragmentModule = VK_NULL_HANDLE;
                try {
                    fragmentModule = createShaderModule(stack, program.fragmentSource(),
                            shaderc_glsl_fragment_shader, program.name() + ".frag");
                } finally {
                    if (fragmentModule != VK_NULL_HANDLE) {
                        vkDestroyShaderModule(device, fragmentModule, null);
                    }
                    vkDestroyShaderModule(device, vertexModule, null);
                }
            }
            long handle = nextCustomShaderHandle++;
            if (handle <= 0L) throw new IllegalStateException("custom shader handles exhausted");
            customShaders.put(handle, new CustomShaderResource(program.name(),
                    program.vertexSource(), program.fragmentSource(), true));
            return handle;
        }

        private void destroyShaderProgram(long shaderHandle) {
            destroyFragmentShader(shaderHandle);
        }

        private boolean customShaderUsesExpandedVertexInput(long shaderHandle) {
            shaderHandle = resolveShaderHandle(shaderHandle);
            CustomShaderResource shader = customShaders.get(shaderHandle);
            if (shader == null) {
                throw new IllegalArgumentException("unknown custom shader handle "
                        + shaderHandle);
            }
            return shader.expandedVertexInput;
        }

        private VulkanResourceStreamResult submitResourceStream(ResourceStreamReader stream,
                                                                 Map<Long, ByteBuffer> arenas) {
            if (closed) throw new IllegalStateException("Vulkan surface is closed");
            if (resourceStreamFault != null) {
                throw new IllegalStateException("ResourceStream backend is faulted",
                        resourceStreamFault);
            }
            long expected;
            try {
                expected = Math.addExact(appliedResourceSequence, 1L);
            } catch (ArithmeticException overflow) {
                throw new IllegalStateException("ResourceStream sequence exhausted", overflow);
            }
            if (stream.firstSequence() != expected) {
                throw new IllegalArgumentException("ResourceStream starts at "
                        + stream.firstSequence() + " but desktop applied through "
                        + appliedResourceSequence);
            }
            resourceStreamActive = true;
            try {
                PendingResourceReadback readback = null;
                for (int index = 0; index < stream.recordCount(); index++) {
                    PendingResourceReadback recordResult =
                            applyResourceRecord(stream, index, arenas);
                    if (recordResult != null) {
                        if (readback != null) throw new IllegalArgumentException(
                                "ResourceStream contains multiple completion payloads");
                        readback = recordResult;
                    }
                }
                appliedResourceSequence = stream.lastSequence();
                if (++resourceStreamSubmissions == 1L) {
                    System.out.println("[Vulkan Mod/Driver] First ResourceStream applied: records="
                            + stream.recordCount() + ", sequence=" + stream.firstSequence()
                            + ".." + stream.lastSequence());
                }
                if (readback != null) {
                    if (pendingResourceReadbacks.put(stream.completionId(),
                            readback.withSequence(appliedResourceSequence)) != null) {
                        throw new IllegalArgumentException("duplicate pending completion ID");
                    }
                    return VulkanResourceStreamResult.pending(appliedResourceSequence,
                            stream.completionId());
                }
                if (stream.completionId() != 0L) {
                    return VulkanResourceStreamResult.completed(appliedResourceSequence,
                            stream.completionId());
                }
                return VulkanResourceStreamResult.applied(appliedResourceSequence);
            } catch (RuntimeException | Error failure) {
                resourceStreamFault = failure;
                throw failure;
            }
        }

        private PendingResourceReadback applyResourceRecord(ResourceStreamReader stream, int index,
                                                             Map<Long, ByteBuffer> arenas) {
            ResourceStreamReader.Record record = stream.record(index);
            long logical = record.handle();
            switch (record.type()) {
                case ResourceStreamFormat.TEXTURE_CREATE: {
                    if (resourceTextures.containsKey(logical)
                            || pendingResourceTextures.containsKey(logical)) {
                        throw new IllegalArgumentException("duplicate logical texture handle "
                                + Long.toUnsignedString(logical));
                    }
                    pendingResourceTextures.put(logical,
                            ResourceStreamRecords.decodeTextureDescriptor(stream, index));
                    return null;
                }
                case ResourceStreamFormat.TEXTURE_UPLOAD: {
                    ResourceStreamRecords.TextureDescriptor descriptor =
                            pendingResourceTextures.get(logical);
                    if (descriptor == null || resourceTextures.containsKey(logical)) {
                        throw new IllegalArgumentException(
                                "texture upload has no pending create");
                    }
                    ResourceStreamRecords.TextureTransfer transfer =
                            ResourceStreamRecords.decodeTextureTransfer(stream, index);
                    requireFullTextureTransfer(descriptor, transfer);
                    long raw = uploadTexture(textureData(transfer, arenas));
                    resourceTextures.put(logical, new ResourceTextureBinding(raw,
                            descriptor.width, descriptor.height, false));
                    pendingResourceTextures.remove(logical);
                    return null;
                }
                case ResourceStreamFormat.TEXTURE_REGION_UPDATE: {
                    ResourceTextureBinding binding = requireResourceTexture(logical);
                    ResourceStreamRecords.TextureTransfer transfer =
                            ResourceStreamRecords.decodeTextureTransfer(stream, index);
                    updateTextureRegion(binding.rawHandle, transfer.x, transfer.y,
                            textureData(transfer, arenas));
                    return null;
                }
                case ResourceStreamFormat.TEXTURE_DESTROY: {
                    ResourceTextureBinding binding = resourceTextures.remove(logical);
                    ResourceStreamRecords.TextureDescriptor pending =
                            pendingResourceTextures.remove(logical);
                    if (binding == null && pending == null) {
                        throw new IllegalArgumentException("destroy of unknown logical texture");
                    }
                    if (binding != null) destroyTexture(binding.rawHandle);
                    return null;
                }
                case ResourceStreamFormat.RENDER_TARGET_CREATE: {
                    if (resourceTextures.containsKey(logical)
                            || pendingResourceTextures.containsKey(logical)) {
                        throw new IllegalArgumentException("duplicate logical render target");
                    }
                    ResourceStreamRecords.TextureDescriptor descriptor =
                            ResourceStreamRecords.decodeTextureDescriptor(stream, index);
                    long raw = createRenderTarget(descriptor.width, descriptor.height);
                    resourceTextures.put(logical, new ResourceTextureBinding(raw,
                            descriptor.width, descriptor.height, true));
                    return null;
                }
                case ResourceStreamFormat.SHADER_PROGRAM_CREATE: {
                    if (resourceShaders.containsKey(logical)) {
                        throw new IllegalArgumentException("duplicate logical shader handle");
                    }
                    ResourceStreamRecords.ShaderProgram shader =
                            ResourceStreamRecords.decodeShaderProgram(stream, index);
                    long raw = shader.hasVertexSource()
                            ? compileShaderProgram(new VulkanCustomShaderProgram(shader.name,
                                    shader.vertexSource, shader.fragmentSource))
                            : compileFragmentShader(new VulkanCustomFragmentShader(shader.name,
                                    shader.fragmentSource));
                    resourceShaders.put(logical, raw);
                    return null;
                }
                case ResourceStreamFormat.SHADER_PROGRAM_DESTROY: {
                    Long raw = resourceShaders.remove(logical);
                    if (raw == null) {
                        throw new IllegalArgumentException("destroy of unknown logical shader");
                    }
                    destroyShaderProgram(raw.longValue());
                    return null;
                }
                case ResourceStreamFormat.TEXTURE_READBACK: {
                    ResourceTextureBinding binding = requireResourceTexture(logical);
                    ResourceStreamRecords.TextureReadback request =
                            ResourceStreamRecords.decodeTextureReadback(stream, index);
                    if ((long) request.x + request.width > binding.width
                            || (long) request.y + request.height > binding.height) {
                        throw new IllegalArgumentException(
                                "texture readback region exceeds its logical texture");
                    }
                    return new PendingResourceReadback(binding.rawHandle, request.x, request.y,
                            request.width, request.height, 0L);
                }
                case ResourceStreamFormat.FLUSH:
                case ResourceStreamFormat.LIFECYCLE_BARRIER:
                    return null; // Submission is synchronous; earlier CPU work is applied.
                default:
                    return null; // Unknown optional records were validated and skipped.
            }
        }

        private VulkanResourceStreamResult completeResourceStream(long completionId) {
            PendingResourceReadback request = pendingResourceReadbacks.remove(completionId);
            if (request == null) throw new IllegalArgumentException(
                    "unknown pending resource completion " + completionId);
            VulkanTextureData texture = readTextureRegion(request.rawTextureHandle,
                    request.x, request.y, request.width, request.height);
            return VulkanResourceStreamResult.textureReadback(
                    request.appliedSequence, completionId, texture);
        }

        private static void requireFullTextureTransfer(
                ResourceStreamRecords.TextureDescriptor descriptor,
                ResourceStreamRecords.TextureTransfer transfer) {
            if (transfer.x != 0 || transfer.y != 0
                    || transfer.width != descriptor.width
                    || transfer.height != descriptor.height) {
                throw new IllegalArgumentException(
                        "initial texture upload does not cover its descriptor");
            }
        }

        private VulkanTextureData textureData(
                ResourceStreamRecords.TextureTransfer transfer, Map<Long, ByteBuffer> arenas) {
            ByteBuffer pixels;
            if (transfer.external()) {
                ByteBuffer arena = arenas.get(transfer.arenaId);
                if (arena == null) throw new IllegalArgumentException(
                        "unknown external resource arena " + transfer.arenaId);
                long end;
                try { end = Math.addExact(transfer.arenaOffset, (long) transfer.dataBytes); }
                catch (ArithmeticException overflow) {
                    throw new IllegalArgumentException("external resource arena range overflows");
                }
                if (transfer.arenaOffset > Integer.MAX_VALUE || end > arena.capacity()) {
                    throw new IllegalArgumentException("external resource arena range is invalid");
                }
                pixels = arena.duplicate();
                pixels.position((int) transfer.arenaOffset).limit((int) end);
                pixels = pixels.slice();
                if (++externalResourceTransfers == 1L) {
                    System.out.println("[Vulkan Mod/Driver] First external ResourceStream upload: "
                            + "arena=" + transfer.arenaId + ", bytes=" + transfer.dataBytes);
                }
            } else {
                pixels = transfer.inlinePixels.duplicate();
            }
            return VulkanTextureData.copyOfRgbaBuffer(
                    transfer.width, transfer.height, pixels);
        }

        private ResourceTextureBinding requireResourceTexture(long logical) {
            ResourceTextureBinding binding = resourceTextures.get(logical);
            if (binding == null) {
                throw new IllegalArgumentException("unknown logical texture handle "
                        + Long.toUnsignedString(logical));
            }
            return binding;
        }

        private long resolveTextureHandle(long handle) {
            if (FrameResourceHandle.type(handle) != FrameResourceHandle.TYPE_TEXTURE) return handle;
            return requireResourceTexture(handle).rawHandle;
        }

        private long resolveShaderHandle(long handle) {
            if (FrameResourceHandle.type(handle)
                    != FrameResourceHandle.TYPE_SHADER_PROGRAM) return handle;
            Long raw = resourceShaders.get(handle);
            if (raw == null) throw new IllegalArgumentException("unknown logical shader handle "
                    + Long.toUnsignedString(handle));
            return raw.longValue();
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
            allocateInfo.commandBufferCount(OFFSCREEN_SUBMISSION_SLOTS);
            PointerBuffer offscreenPointers = stack.mallocPointer(OFFSCREEN_SUBMISSION_SLOTS);
            check(vkAllocateCommandBuffers(device, allocateInfo, offscreenPointers),
                    "vkAllocateCommandBuffers(offscreen)");
            offscreenCommandBuffers = new VkCommandBuffer[OFFSCREEN_SUBMISSION_SLOTS];
            for (int index = 0; index < OFFSCREEN_SUBMISSION_SLOTS; index++) {
                offscreenCommandBuffers[index] = new VkCommandBuffer(
                        offscreenPointers.get(index), device);
            }
            imageAvailableSemaphores = new long[images.length];
            inFlightFences = new long[images.length];
            vertexAllocations = new BufferAllocation[images.length];
            vertexCapacities = new int[images.length];
            offscreenFences = new long[OFFSCREEN_SUBMISSION_SLOTS];
            offscreenSlotOwnerFences = new long[OFFSCREEN_SUBMISSION_SLOTS];
            offscreenVertexAllocations = new BufferAllocation[OFFSCREEN_SUBMISSION_SLOTS];
            offscreenVertexCapacities = new int[OFFSCREEN_SUBMISSION_SLOTS];
            textureUploadSlots = new BufferAllocation[
                    images.length + OFFSCREEN_SUBMISSION_SLOTS + 1];
            textureUploadCapacities = new int[textureUploadSlots.length];
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
            for (int index = 0; index < OFFSCREEN_SUBMISSION_SLOTS; index++) {
                check(vkCreateFence(device, fenceInfo, null, handle),
                        "vkCreateFence(offscreen[" + index + "])");
                offscreenFences[index] = handle.get(0);
                offscreenSlotOwnerFences[index] = offscreenFences[index];
            }
            frameCursor = 0;
            offscreenCursor = 0;
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
                texturePipelines[index] = createTexturePipeline(stack, blendMode,
                        TEXTURE_FRAGMENT_SHADER, "textured-quad");
            }
            return texturePipelines[index];
        }

        private long texturePipeline(MemoryStack stack, VulkanBlendMode blendMode,
                                     VulkanShaderState shaderState) {
            if (shaderState.effect() != VulkanShaderState.CUSTOM) {
                return texturePipeline(stack, blendMode);
            }
            long shaderHandle = resolveShaderHandle(shaderState.customShaderHandle());
            CustomShaderResource custom = customShaders.get(shaderHandle);
            if (custom == null) {
                throw new IllegalArgumentException("unknown custom shader handle "
                        + shaderState.customShaderHandle());
            }
            int index = blendMode.ordinal();
            if (custom.pipelines[index] == VK_NULL_HANDLE) {
                custom.pipelines[index] = createTexturePipeline(stack, blendMode,
                        custom.vertexSource, custom.fragmentSource,
                        "custom-" + custom.name, custom.expandedVertexInput);
            }
            return custom.pipelines[index];
        }

        private long createTexturePipeline(MemoryStack stack, VulkanBlendMode blendMode,
                                           String fragmentSource, String label) {
            return createTexturePipeline(stack, blendMode, TEXTURE_VERTEX_SHADER,
                    fragmentSource, label, false);
        }

        private long createTexturePipeline(MemoryStack stack, VulkanBlendMode blendMode,
                                           String vertexSource, String fragmentSource,
                                           String label, boolean expandedVertexInput) {
            if (textureDescriptorSetLayout == VK_NULL_HANDLE) {
                throw new IllegalStateException("texture descriptor layout is unavailable");
            }
            long vertexModule = VK_NULL_HANDLE;
            long fragmentModule = VK_NULL_HANDLE;
            try {
                vertexModule = createShaderModule(stack, vertexSource,
                        shaderc_glsl_vertex_shader, label + ".vert");
                fragmentModule = createShaderModule(stack, fragmentSource,
                        shaderc_glsl_fragment_shader, label + ".frag");
                VkPipelineShaderStageCreateInfo.Buffer stages =
                        VkPipelineShaderStageCreateInfo.calloc(2, stack);
                stages.get(0).sType$Default().stage(VK_SHADER_STAGE_VERTEX_BIT)
                        .module(vertexModule).pName(stack.UTF8("main"));
                stages.get(1).sType$Default().stage(VK_SHADER_STAGE_FRAGMENT_BIT)
                        .module(fragmentModule).pName(stack.UTF8("main"));
                VkVertexInputBindingDescription.Buffer binding =
                        VkVertexInputBindingDescription.calloc(1, stack);
                binding.get(0).binding(0).stride(expandedVertexInput
                                ? CUSTOM_TEXTURED_VERTEX_STRIDE : TEXTURED_VERTEX_STRIDE)
                        .inputRate(VK_VERTEX_INPUT_RATE_VERTEX);
                VkVertexInputAttributeDescription.Buffer attributes =
                        VkVertexInputAttributeDescription.calloc(
                                expandedVertexInput ? 6 : 3, stack);
                attributes.get(0).location(0).binding(0)
                        .format(VK_FORMAT_R32G32_SFLOAT).offset(0);
                attributes.get(1).location(1).binding(0)
                        .format(VK_FORMAT_R32G32_SFLOAT).offset(2 * Float.BYTES);
                attributes.get(2).location(2).binding(0)
                        .format(VK_FORMAT_R32G32B32A32_SFLOAT).offset(4 * Float.BYTES);
                if (expandedVertexInput) {
                    attributes.get(3).location(3).binding(0)
                            .format(VK_FORMAT_R32G32B32_SFLOAT).offset(8 * Float.BYTES);
                    attributes.get(4).location(4).binding(0)
                            .format(VK_FORMAT_R32G32B32_SFLOAT).offset(11 * Float.BYTES);
                    attributes.get(5).location(5).binding(0)
                            .format(VK_FORMAT_R32G32_SFLOAT).offset(14 * Float.BYTES);
                }
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
                    pushConstants.get(0).stageFlags(VK_SHADER_STAGE_VERTEX_BIT
                                    | VK_SHADER_STAGE_FRAGMENT_BIT)
                            .offset(0).size(128);
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
                        pipelineInfo, null, handle), "vkCreateGraphicsPipelines(" + label + ")");
                return handle.get(0);
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
                queueTextureUpload(publicHandle, 0, 0, texture);
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
                                | VK_IMAGE_USAGE_TRANSFER_SRC_BIT
                                | VK_IMAGE_USAGE_TRANSFER_DST_BIT)
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

        private TextureResource requireRenderTarget(long textureHandle,
                                                    VulkanFrameCommands frame) {
            return requireRenderTarget(textureHandle,
                    new ObjectFrameSource(textureHandle, frame));
        }

        private TextureResource requireRenderTarget(long textureHandle,
                                                    FrameSource frame) {
            textureHandle = resolveTextureHandle(textureHandle);
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
            return target;
        }

        private void recordFrameGraphTarget(VkCommandBuffer command, MemoryStack stack,
                                            long textureHandle, FrameSource frame,
                                            TextureResource target, FrameUpload upload,
                                            int vertexSlot) {
            textureHandle = resolveTextureHandle(textureHandle);
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
            renderPassBegin.renderArea().extent().width(target.width).height(target.height);
            vkCmdBeginRenderPass(command, renderPassBegin, VK_SUBPASS_CONTENTS_INLINE);
            long vertexBuffer = offscreenVertexAllocations[vertexSlot] == null
                    ? VK_NULL_HANDLE : offscreenVertexAllocations[vertexSlot].buffer;
            if (upload.totalVertexCount() > 0) {
                VkViewport.Buffer viewport = VkViewport.calloc(1, stack);
                viewport.get(0).x(0.0f).y(0.0f)
                        .width(target.width).height(target.height)
                        .minDepth(0.0f).maxDepth(1.0f);
                vkCmdSetViewport(command, 0, viewport);
            }
            ByteBuffer shaderPushConstants = stack.malloc(128).order(ByteOrder.nativeOrder());
            LongBuffer drawVertexBuffer = stack.mallocLong(1).put(0, vertexBuffer);
            LongBuffer drawVertexOffset = stack.mallocLong(1);
            LongBuffer drawDescriptorSet = stack.mallocLong(1);
            VkRect2D.Buffer drawScissor = VkRect2D.calloc(1, stack);
            for (DrawBatch drawBatch : upload.batches) {
                if (drawBatch instanceof ColoredDrawBatch) {
                    ColoredDrawBatch batch = (ColoredDrawBatch) drawBatch;
                    vkCmdBindPipeline(command, VK_PIPELINE_BIND_POINT_GRAPHICS,
                            colorPipeline(stack, batch.blendMode));
                    drawVertexOffset.put(0, batch.vertexByteOffset);
                    vkCmdBindVertexBuffers(command, 0, drawVertexBuffer, drawVertexOffset);
                    if (setScissor(command, batch.clip, target.width, target.height,
                            drawScissor)) {
                        recordDraw(command, batch, vertexBuffer);
                    }
                } else {
                    TextureDrawBatch batch = (TextureDrawBatch) drawBatch;
                    if (resolveTextureHandle(batch.textureHandle) == textureHandle) {
                        throw new IllegalArgumentException(
                                "a render target cannot sample itself in the same pass");
                    }
                    if (batch.shaderState.secondaryTextureHandle() != 0L
                            && resolveTextureHandle(
                            batch.shaderState.secondaryTextureHandle()) == textureHandle) {
                        throw new IllegalArgumentException(
                                "a render target cannot be its own secondary texture");
                    }
                    vkCmdBindPipeline(command, VK_PIPELINE_BIND_POINT_GRAPHICS,
                            texturePipeline(stack, batch.blendMode, batch.shaderState));
                    drawVertexOffset.put(0, batch.vertexByteOffset);
                    vkCmdBindVertexBuffers(command, 0, drawVertexBuffer, drawVertexOffset);
                    if (setScissor(command, batch.clip, target.width, target.height,
                            drawScissor)) {
                        drawDescriptorSet.put(0, batch.descriptorSet);
                        vkCmdBindDescriptorSets(command, VK_PIPELINE_BIND_POINT_GRAPHICS,
                                texturePipelineLayout, 0, drawDescriptorSet, null);
                        pushShaderState(command, batch.shaderState, shaderPushConstants);
                        recordDraw(command, batch, vertexBuffer);
                    }
                }
            }
            releaseFrameUpload(upload);
            vkCmdEndRenderPass(command);
            target.initialized = true;
            renderTargetSubmissions++;
            if (renderTargetSubmissions == 1) {
                System.out.println("[Vulkan Mod/Driver] First native render target: "
                        + target.width + "x" + target.height + ", commands="
                        + frame.drawCount() + ", texture=" + textureHandle);
            }
            if (Boolean.getBoolean("rusted.fabric.vulkan.debugFrameGraph")
                    && renderTargetSubmissions <= 64) {
                System.out.println("[Vulkan Mod/Driver] Frame graph target pass #"
                        + renderTargetSubmissions + ": texture=" + textureHandle
                        + ", commands=" + frame.drawCount());
            }
        }

        private void renderToTexture(long textureHandle, VulkanFrameCommands frame) {
            renderToTexture(new ObjectFrameSource(textureHandle, frame));
        }

        private void renderToTexture(FrameSource frame) {
            long textureHandle = frame.targetHandle();
            TextureResource target = requireRenderTarget(textureHandle, frame);
            textureHandle = resolveTextureHandle(textureHandle);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                int slot = offscreenCursor;
                long fence = offscreenFences[slot];
                long ownerFence = offscreenSlotOwnerFences[slot];
                check(vkWaitForFences(device, stack.longs(ownerFence), true, -1L),
                        "vkWaitForFences(render target slot)");
                if (pendingTextureMutationRequiresGlobalWait()) {
                    // An already sampled image can still be in use by another frame slot.
                    waitForAllSubmissionFences(stack);
                    textureMutationFenceWaits++;
                }
                if (!retiredTextures.isEmpty() && allSubmissionFencesSignaled()) {
                    destroyRetiredTextures();
                }
                FrameUpload upload = frame.upload(stack, slot,
                        offscreenVertexAllocations, offscreenVertexCapacities);
                VkCommandBuffer command = offscreenCommandBuffers[slot];
                check(vkResetCommandBuffer(command, 0),
                        "vkResetCommandBuffer(render target)");
                VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack)
                        .sType$Default().flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
                check(vkBeginCommandBuffer(command, beginInfo),
                        "vkBeginCommandBuffer(render target)");
                recordPendingTextureUploads(command, stack,
                        inFlightFences.length + slot);
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
                long vertexBuffer = offscreenVertexAllocations[slot] == null
                        ? VK_NULL_HANDLE : offscreenVertexAllocations[slot].buffer;
                if (upload.totalVertexCount() > 0) {
                    VkViewport.Buffer viewport = VkViewport.calloc(1, stack);
                    viewport.get(0).x(0.0f).y(0.0f)
                            .width(target.width).height(target.height)
                            .minDepth(0.0f).maxDepth(1.0f);
                    vkCmdSetViewport(command, 0, viewport);
                }
                ByteBuffer shaderPushConstants =
                        stack.malloc(128).order(ByteOrder.nativeOrder());
                LongBuffer drawVertexBuffer = stack.mallocLong(1).put(0, vertexBuffer);
                LongBuffer drawVertexOffset = stack.mallocLong(1);
                LongBuffer drawDescriptorSet = stack.mallocLong(1);
                VkRect2D.Buffer drawScissor = VkRect2D.calloc(1, stack);
                for (DrawBatch drawBatch : upload.batches) {
                    if (drawBatch instanceof ColoredDrawBatch) {
                        ColoredDrawBatch batch = (ColoredDrawBatch) drawBatch;
                        vkCmdBindPipeline(command, VK_PIPELINE_BIND_POINT_GRAPHICS,
                                colorPipeline(stack, batch.blendMode));
                        drawVertexOffset.put(0, batch.vertexByteOffset);
                        vkCmdBindVertexBuffers(command, 0,
                                drawVertexBuffer, drawVertexOffset);
                        if (setScissor(command, batch.clip, target.width,
                                target.height, drawScissor)) {
                            recordDraw(command, batch, vertexBuffer);
                        }
                    } else {
                        TextureDrawBatch batch = (TextureDrawBatch) drawBatch;
                        if (resolveTextureHandle(batch.textureHandle) == textureHandle) {
                            throw new IllegalArgumentException(
                                    "a render target cannot sample itself in the same pass");
                        }
                        if (batch.shaderState.secondaryTextureHandle() != 0L
                                && resolveTextureHandle(
                                batch.shaderState.secondaryTextureHandle()) == textureHandle) {
                            throw new IllegalArgumentException(
                                    "a render target cannot be its own secondary texture");
                        }
                        vkCmdBindPipeline(command, VK_PIPELINE_BIND_POINT_GRAPHICS,
                                texturePipeline(stack, batch.blendMode, batch.shaderState));
                        drawVertexOffset.put(0, batch.vertexByteOffset);
                        vkCmdBindVertexBuffers(command, 0,
                                drawVertexBuffer, drawVertexOffset);
                        if (setScissor(command, batch.clip, target.width,
                                target.height, drawScissor)) {
                            drawDescriptorSet.put(0, batch.descriptorSet);
                            vkCmdBindDescriptorSets(command, VK_PIPELINE_BIND_POINT_GRAPHICS,
                                    texturePipelineLayout, 0,
                                    drawDescriptorSet, null);
                            pushShaderState(command, batch.shaderState,
                                    shaderPushConstants);
                            recordDraw(command, batch, vertexBuffer);
                        }
                    }
                }
                releaseFrameUpload(upload);
                vkCmdEndRenderPass(command);
                check(vkEndCommandBuffer(command), "vkEndCommandBuffer(render target)");
                VkSubmitInfo submit = VkSubmitInfo.calloc(stack).sType$Default()
                        .pCommandBuffers(stack.pointers(command.address()));
                check(vkResetFences(device, stack.longs(fence)),
                        "vkResetFences(render target)");
                check(vkQueueSubmit(graphicsQueue, submit, fence),
                        "vkQueueSubmit(render target)");
                immediateOffscreenQueueSubmissions++;
                offscreenSlotOwnerFences[slot] = fence;
                offscreenCursor = (slot + 1) % offscreenFences.length;
                target.initialized = true;
                renderTargetSubmissions++;
                if (renderTargetSubmissions == 1) {
                    System.out.println("[Vulkan Mod/Driver] First native render target: "
                            + target.width + "x" + target.height + ", commands="
                            + frame.drawCount() + ", texture=" + textureHandle);
                }
                if (Boolean.getBoolean("rusted.fabric.vulkan.debugRenderTargetPasses")
                        && renderTargetSubmissions <= 64
                        && frame instanceof ObjectFrameSource) {
                    VulkanFrameCommands objectFrame = ((ObjectFrameSource) frame).frame;
                    java.util.LinkedHashSet<Long> sampled = new java.util.LinkedHashSet<Long>();
                    for (int commandIndex = 0; commandIndex < objectFrame.commandCount();
                         commandIndex++) {
                        VulkanDrawCommand draw = objectFrame.command(commandIndex);
                        if (draw instanceof VulkanTexturedQuad) {
                            sampled.add(((VulkanTexturedQuad) draw).textureHandle());
                        } else if (draw instanceof VulkanTexturedQuadBatch) {
                            sampled.add(((VulkanTexturedQuadBatch) draw).textureHandle());
                        } else if (draw instanceof VulkanTexturedTriangle) {
                            sampled.add(((VulkanTexturedTriangle) draw).textureHandle());
                        }
                    }
                    System.out.println("[Vulkan Mod/Driver] Native target pass #"
                            + renderTargetSubmissions + ": texture=" + textureHandle
                            + ", size=" + target.width + "x" + target.height
                            + ", clear=" + frame.clearRed() + "," + frame.clearGreen()
                            + "," + frame.clearBlue() + "," + frame.clearAlpha()
                            + ", commands=" + frame.drawCount()
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
            VulkanTextureData snapshot = readTexture(textureHandle);
            byte[] pixels = snapshot.copyRgba();
            int sampledPixels = 0;
            int nonTransparent = 0;
            int nonBlack = 0;
            int stride = Math.max(1, target.width * target.height / 65536);
            for (int pixel = 0; pixel < target.width * target.height; pixel += stride) {
                int offset = pixel * 4;
                int red = pixels[offset] & 255;
                int green = pixels[offset + 1] & 255;
                int blue = pixels[offset + 2] & 255;
                int alpha = pixels[offset + 3] & 255;
                sampledPixels++;
                if (alpha != 0) nonTransparent++;
                if (red != 0 || green != 0 || blue != 0) nonBlack++;
            }
            System.out.println("[Vulkan Mod/Driver] Native target readback: texture="
                    + textureHandle + ", samples=" + sampledPixels
                    + ", nonTransparent=" + nonTransparent
                    + ", nonBlack=" + nonBlack);
        }

        private VulkanTextureData readTexture(long textureHandle) {
            textureHandle = resolveTextureHandle(textureHandle);
            TextureResource target = textures.get(textureHandle);
            if (target == null) {
                throw new IllegalArgumentException("unknown texture handle " + textureHandle);
            }
            if (!target.renderTarget) {
                throw new IllegalArgumentException(
                        "texture " + textureHandle + " is not a readable render target");
            }
            return readTextureRegion(target, 0, 0, target.width, target.height);
        }

        private VulkanTextureData readTextureRegion(long textureHandle, int x, int y,
                                                    int width, int height) {
            textureHandle = resolveTextureHandle(textureHandle);
            TextureResource target = textures.get(textureHandle);
            if (target == null) {
                throw new IllegalArgumentException("unknown texture handle " + textureHandle);
            }
            if (!target.renderTarget) {
                throw new IllegalArgumentException(
                        "texture " + textureHandle + " is not a readable render target");
            }
            if (x < 0 || y < 0 || width <= 0 || height <= 0
                    || (long) x + width > target.width
                    || (long) y + height > target.height) {
                throw new IllegalArgumentException("readback region is outside texture "
                        + target.width + "x" + target.height);
            }
            return readTextureRegion(target, x, y, width, height);
        }

        private VulkanTextureData readTextureRegion(TextureResource target, int x, int y,
                                                     int width, int height) {
            int byteCount = Math.multiplyExact(Math.multiplyExact(
                    width, height), 4);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                // The shared command buffer and texture staging allocation must no longer be in
                // flight, but unrelated future queue work need not be held behind a device idle.
                waitForAllSubmissionFences(stack);
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
                    // A reliable readback is ordered after every earlier ResourceStream update,
                    // including uploads that have not yet been folded into a presentation frame.
                    recordPendingTextureUploads(command, stack,
                            textureUploadSlots.length - 1);
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
                    region.get(0).imageOffset().x(x).y(y).z(0);
                    region.get(0).imageExtent()
                            .width(width).height(height).depth(1);
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
                    LongBuffer fenceHandle = stack.mallocLong(1);
                    VkFenceCreateInfo fenceInfo = VkFenceCreateInfo.calloc(stack).sType$Default();
                    check(vkCreateFence(device, fenceInfo, null, fenceHandle),
                            "vkCreateFence(render-target readback)");
                    long readbackFence = fenceHandle.get(0);
                    try {
                        check(vkQueueSubmit(graphicsQueue, submit, readbackFence),
                                "vkQueueSubmit(render-target readback)");
                        check(vkWaitForFences(device, stack.longs(readbackFence), true, -1L),
                                "vkWaitForFences(render-target readback)");
                    } finally {
                        vkDestroyFence(device, readbackFence, null);
                    }
                    PointerBuffer mapped = stack.mallocPointer(1);
                    check(vkMapMemory(device, readback.memory, 0, byteCount, 0, mapped),
                            "vkMapMemory(render-target readback)");
                    byte[] rgba = new byte[byteCount];
                    try {
                        ByteBuffer pixels = MemoryUtil.memByteBuffer(mapped.get(0), byteCount);
                        boolean blueFirst = isBlueFirstFormat(info.imageFormat());
                        for (int pixel = 0; pixel < width * height; pixel++) {
                            int offset = pixel * 4;
                            byte first = pixels.get(offset);
                            byte green = pixels.get(offset + 1);
                            byte third = pixels.get(offset + 2);
                            rgba[offset] = blueFirst ? third : first;
                            rgba[offset + 1] = green;
                            rgba[offset + 2] = blueFirst ? first : third;
                            rgba[offset + 3] = pixels.get(offset + 3);
                        }
                    } finally {
                        vkUnmapMemory(device, readback.memory);
                    }
                    return new VulkanTextureData(width, height, rgba);
                } finally {
                    destroyBufferAllocation(readback);
                }
            }
        }

        private static boolean isBlueFirstFormat(int format) {
            return format == VK_FORMAT_B8G8R8A8_UNORM
                    || format == VK_FORMAT_B8G8R8A8_SRGB;
        }

        private void updateTexture(long textureHandle, VulkanTextureData texture) {
            updateTextureRegion(textureHandle, 0, 0, texture);
        }

        private void updateTextureRegion(long textureHandle, int x, int y,
                                         VulkanTextureData texture) {
            textureHandle = resolveTextureHandle(textureHandle);
            TextureResource target = textures.get(textureHandle);
            if (target == null) {
                throw new IllegalArgumentException("unknown texture handle " + textureHandle);
            }
            if (x < 0 || y < 0 || (long) x + texture.width() > target.width
                    || (long) y + texture.height() > target.height) {
                throw new IllegalArgumentException("texture update region exceeds "
                        + target.width + "x" + target.height);
            }
            queueTextureUpload(textureHandle, x, y, texture);
        }

        private void queueTextureUpload(long textureHandle, int x, int y,
                                        VulkanTextureData texture) {
            TextureResource target = textures.get(textureHandle);
            if (target == null) throw new IllegalArgumentException(
                    "unknown texture handle " + textureHandle);
            boolean initialized = target.initialized;
            for (int index = pendingTextureUploads.size() - 1; index >= 0; index--) {
                if (pendingTextureUploads.get(index).textureHandle == textureHandle) {
                    initialized = true;
                    break;
                }
            }
            pendingTextureUploads.add(new PendingTextureUpload(textureHandle, x, y,
                    texture, initialized));
        }

        private BufferAllocation ensureTextureUploadSlot(MemoryStack stack, int slot,
                                                         int requiredBytes) {
            if (slot < 0 || slot >= textureUploadSlots.length) {
                throw new IllegalArgumentException("texture upload slot is out of range");
            }
            BufferAllocation existing = textureUploadSlots[slot];
            if (existing != null && textureUploadCapacities[slot] >= requiredBytes) {
                return existing;
            }
            if (existing != null) destroyBufferAllocation(existing);
            int capacity = 1;
            while (capacity < requiredBytes && capacity > 0) capacity <<= 1;
            if (capacity <= 0) capacity = requiredBytes;
            BufferAllocation created = createBufferAllocation(stack, capacity,
                    VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT
                            | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
            try {
                PointerBuffer mapped = stack.mallocPointer(1);
                check(vkMapMemory(device, created.memory, 0, capacity, 0, mapped),
                        "vkMapMemory(persistent texture upload slot)");
                created.mapped = MemoryUtil.memByteBuffer(mapped.get(0), capacity)
                        .order(ByteOrder.nativeOrder());
            } catch (Throwable failure) {
                destroyBufferAllocation(created);
                throw failure;
            }
            textureUploadSlots[slot] = created;
            textureUploadCapacities[slot] = capacity;
            textureUploadSlotGrowths++;
            if (textureUploadSlotGrowths == 1L) {
                System.out.println("[Vulkan Mod/Driver] Persistent mapped texture upload slots "
                        + "active (slots=" + textureUploadSlots.length + ")");
            }
            return created;
        }

        private boolean pendingTextureMutationRequiresGlobalWait() {
            for (PendingTextureUpload pending : pendingTextureUploads) {
                if (pending.initialized) return true;
            }
            return false;
        }

        private void appendPerformanceStatistics(Map<String, Long> statistics) {
            statistics.put("texture.uploadBytes", textureUploadBytes);
            statistics.put("texture.uploadBatches", textureUploadBatches);
            statistics.put("texture.uploadSlotGrowths", textureUploadSlotGrowths);
            statistics.put("texture.mutationFenceWaits", textureMutationFenceWaits);
            statistics.put("frame.materialCacheHits", decodedMaterialCacheHits);
            statistics.put("frame.materialCacheMisses", decodedMaterialCacheMisses);
        }

        private void ensureTextureDescriptors(MemoryStack stack) {
            if (textureDescriptorSetLayout != VK_NULL_HANDLE) return;
            VkDescriptorSetLayoutBinding.Buffer binding =
                    VkDescriptorSetLayoutBinding.calloc(2, stack);
            binding.get(0).binding(0).descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .descriptorCount(1).stageFlags(VK_SHADER_STAGE_VERTEX_BIT
                            | VK_SHADER_STAGE_FRAGMENT_BIT);
            binding.get(1).binding(1).descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .descriptorCount(1).stageFlags(VK_SHADER_STAGE_VERTEX_BIT
                            | VK_SHADER_STAGE_FRAGMENT_BIT);
            VkDescriptorSetLayoutCreateInfo layoutInfo =
                    VkDescriptorSetLayoutCreateInfo.calloc(stack).sType$Default()
                            .pBindings(binding);
            LongBuffer handle = stack.mallocLong(1);
            check(vkCreateDescriptorSetLayout(device, layoutInfo, null, handle),
                    "vkCreateDescriptorSetLayout(texture)");
            textureDescriptorSetLayout = handle.get(0);
            VkDescriptorPoolSize.Buffer poolSize = VkDescriptorPoolSize.calloc(1, stack);
            poolSize.get(0).type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .descriptorCount(MAX_TEXTURE_DESCRIPTOR_SETS * 2);
            VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.calloc(stack)
                    .sType$Default().flags(VK_DESCRIPTOR_POOL_CREATE_FREE_DESCRIPTOR_SET_BIT)
                    .maxSets(MAX_TEXTURE_DESCRIPTOR_SETS)
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
            VkDescriptorImageInfo.Buffer imageInfo = VkDescriptorImageInfo.calloc(2, stack);
            for (int index = 0; index < 2; index++) {
                imageInfo.get(index).sampler(texture.samplers[filter.ordinal()])
                        .imageView(texture.view)
                        .imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
            }
            VkWriteDescriptorSet.Buffer write = VkWriteDescriptorSet.calloc(2, stack);
            for (int index = 0; index < 2; index++) {
                write.get(index).sType$Default()
                        .dstSet(texture.descriptorSets[filter.ordinal()]).dstBinding(index)
                        .dstArrayElement(0)
                        .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                        .descriptorCount(1).pImageInfo(VkDescriptorImageInfo.create(
                                imageInfo.get(index).address(), 1));
            }
            vkUpdateDescriptorSets(device, write, null);
        }

        private long textureDescriptor(long primaryHandle,
                                       VulkanTextureFilter filter,
                                       VulkanShaderState shaderState) {
            primaryHandle = resolveTextureHandle(primaryHandle);
            TextureResource primary = textures.get(primaryHandle);
            if (primary == null) {
                throw new IllegalArgumentException("unknown texture handle " + primaryHandle);
            }
            long secondaryHandle = shaderState.secondaryTextureHandle();
            if (secondaryHandle != 0L) secondaryHandle = resolveTextureHandle(secondaryHandle);
            if (secondaryHandle == 0L) return primary.descriptorSets[filter.ordinal()];
            TextureResource secondary = textures.get(secondaryHandle);
            if (secondary == null) {
                throw new IllegalArgumentException(
                        "unknown secondary texture handle " + secondaryHandle);
            }
            TextureDescriptorKey key = new TextureDescriptorKey(
                    primaryHandle, secondaryHandle, filter);
            Long existing = pairedTextureDescriptors.get(key);
            if (existing != null) return existing.longValue();

            long descriptorSet;
            // A large first frame can create many unique texture pairs. Give each allocation a
            // bounded nested stack frame instead of retaining every temporary until present ends.
            try (MemoryStack descriptorStack = MemoryStack.stackPush()) {
                VkDescriptorSetAllocateInfo allocateInfo =
                        VkDescriptorSetAllocateInfo.calloc(descriptorStack)
                                .sType$Default().descriptorPool(textureDescriptorPool)
                                .pSetLayouts(descriptorStack.longs(textureDescriptorSetLayout));
                LongBuffer descriptor = descriptorStack.mallocLong(1);
                check(vkAllocateDescriptorSets(device, allocateInfo, descriptor),
                        "vkAllocateDescriptorSets(texture pair)");
                descriptorSet = descriptor.get(0);
                VkDescriptorImageInfo.Buffer imageInfo =
                        VkDescriptorImageInfo.calloc(2, descriptorStack);
                imageInfo.get(0).sampler(primary.samplers[filter.ordinal()])
                        .imageView(primary.view)
                        .imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
                imageInfo.get(1).sampler(secondary.samplers[filter.ordinal()])
                        .imageView(secondary.view)
                        .imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
                VkWriteDescriptorSet.Buffer write =
                        VkWriteDescriptorSet.calloc(2, descriptorStack);
                for (int index = 0; index < 2; index++) {
                    write.get(index).sType$Default().dstSet(descriptorSet).dstBinding(index)
                            .dstArrayElement(0)
                            .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                            .descriptorCount(1).pImageInfo(VkDescriptorImageInfo.create(
                                    imageInfo.get(index).address(), 1));
                }
                vkUpdateDescriptorSets(device, write, null);
            }
            pairedTextureDescriptors.put(key, descriptorSet);
            return descriptorSet;
        }

        private void recordPendingTextureUploads(VkCommandBuffer command, MemoryStack stack,
                                                 int uploadSlot) {
            if (pendingTextureUploads.isEmpty()) return;
            int byteCount = 0;
            for (PendingTextureUpload pending : pendingTextureUploads) {
                byteCount = Math.addExact(byteCount, pending.texture.byteSize());
            }
            BufferAllocation staging = ensureTextureUploadSlot(stack, uploadSlot, byteCount);
            ByteBuffer destination = staging.mapped.duplicate().order(ByteOrder.nativeOrder());
            destination.clear().limit(byteCount);
            for (PendingTextureUpload pending : pendingTextureUploads) {
                TextureResource target = textures.get(pending.textureHandle);
                if (target != null && target.renderTarget
                        && isBlueFirstFormat(info.imageFormat())) {
                    byte[] rgba = pending.texture.copyRgba();
                    for (int offset = 0; offset < rgba.length; offset += 4) {
                        destination.put(rgba[offset + 2]);
                        destination.put(rgba[offset + 1]);
                        destination.put(rgba[offset]);
                        destination.put(rgba[offset + 3]);
                    }
                } else {
                    pending.texture.writeTo(destination);
                }
            }

            int offset = 0;
            for (PendingTextureUpload pending : pendingTextureUploads) {
                TextureResource target = textures.get(pending.textureHandle);
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
                region.get(0).imageOffset().x(pending.x).y(pending.y).z(0);
                region.get(0).imageExtent().width(pending.texture.width())
                        .height(pending.texture.height()).depth(1);
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
            textureUploadBytes = Math.addExact(textureUploadBytes, byteCount);
            textureUploadBatches++;
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
            if (allocation.mapped != null) {
                vkUnmapMemory(device, allocation.memory);
                allocation.mapped = null;
            }
            if (allocation.buffer != VK_NULL_HANDLE) {
                vkDestroyBuffer(device, allocation.buffer, null);
            }
            if (allocation.memory != VK_NULL_HANDLE) vkFreeMemory(device, allocation.memory, null);
        }

        private void destroyTexture(long textureHandle) {
            textureHandle = resolveTextureHandle(textureHandle);
            final long destroyedHandle = textureHandle;
            TextureResource texture = textures.remove(textureHandle);
            if (texture == null) return;
            pendingTextureUploads.removeIf(
                    pending -> pending.textureHandle == destroyedHandle);
            java.util.Iterator<Map.Entry<TextureDescriptorKey, Long>> descriptors =
                    pairedTextureDescriptors.entrySet().iterator();
            while (descriptors.hasNext()) {
                Map.Entry<TextureDescriptorKey, Long> descriptor = descriptors.next();
                if (descriptor.getKey().uses(textureHandle)) {
                    texture.dependentDescriptorSets.add(descriptor.getValue());
                    descriptors.remove();
                }
            }
            // A previous submitted frame can still sample this image. Release it after that
            // frame's fence instead of forcing the whole device idle for every map-cache update.
            retiredTextures.addLast(texture);
        }

        private void destroyRetiredTextures() {
            while (!retiredTextures.isEmpty()) {
                destroyTextureResource(retiredTextures.removeFirst(), true);
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
            for (long descriptorSet : texture.dependentDescriptorSets) {
                if (freeDescriptor && descriptorSet != VK_NULL_HANDLE
                        && textureDescriptorPool != VK_NULL_HANDLE) {
                    check(vkFreeDescriptorSets(device, textureDescriptorPool,
                            stack.longs(descriptorSet)),
                            "vkFreeDescriptorSets(texture pair)");
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
            return prepareAndPresentFrame(new ObjectFrameSource(0L, frame), false);
        }

        private VulkanSurfaceInfo presentFrame(VulkanFrameSubmission submission) {
            List<VulkanRenderTargetPass> passes = submission.renderTargetPasses();
            if (passes.isEmpty()) return presentFrame(submission.presentationFrame());
            if (passes.size() > offscreenVertexAllocations.length) {
                // Preserve correctness for unusually deep graphs. The common terrain/minimap/UI
                // path fits the bounded ring and is encoded into one queue submission below.
                for (VulkanRenderTargetPass pass : passes) {
                    renderToTexture(pass.textureHandle(), pass.frame());
                }
                return presentFrame(submission.presentationFrame());
            }
            ArrayList<FrameSource> sources = new ArrayList<FrameSource>(passes.size());
            for (VulkanRenderTargetPass pass : passes) {
                sources.add(new ObjectFrameSource(pass.textureHandle(), pass.frame()));
            }
            frameGraphPasses = sources;
            frameGraphSubmitted = false;
            try {
                VulkanSurfaceInfo result = presentFrame(submission.presentationFrame());
                if (result == null && !frameGraphSubmitted) {
                    // Acquisition can be unavailable while occluded. Offscreen caches must still
                    // become durable or a one-shot terrain/minimap rebuild would be lost.
                    for (VulkanRenderTargetPass pass : passes) {
                        renderToTexture(pass.textureHandle(), pass.frame());
                    }
                }
                return result;
            } finally {
                frameGraphPasses = Collections.emptyList();
                frameGraphSubmitted = false;
            }
        }

        private VulkanSurfaceInfo presentFrame(DecodedFrameStream stream) {
            if (stream.requiredResourceSequence() > appliedResourceSequence) {
                throw new IllegalStateException("FrameStream requires resource sequence "
                        + stream.requiredResourceSequence() + " but desktop applied through "
                        + appliedResourceSequence);
            }
            beginDecodedMaterialFrame(stream.materialCount());
            int finalPassIndex = stream.passCount() - 1;
            StreamFrameSource presentation = new StreamFrameSource(stream, finalPassIndex);
            int graphCount = finalPassIndex;
            if (graphCount == 0) return prepareAndPresentFrame(presentation, false);
            ArrayList<FrameSource> passes = new ArrayList<FrameSource>(graphCount);
            for (int index = 0; index < graphCount; index++) {
                passes.add(new StreamFrameSource(stream, index));
            }
            if (passes.size() > offscreenVertexAllocations.length) {
                for (FrameSource pass : passes) renderToTexture(pass);
                return prepareAndPresentFrame(presentation, false);
            }
            frameGraphPasses = passes;
            frameGraphSubmitted = false;
            try {
                VulkanSurfaceInfo result = prepareAndPresentFrame(presentation, false);
                if (result == null && !frameGraphSubmitted) {
                    for (FrameSource pass : passes) renderToTexture(pass);
                }
                return result;
            } finally {
                frameGraphPasses = Collections.emptyList();
                frameGraphSubmitted = false;
            }
        }

        private VulkanSurfaceInfo presentFrameAndReveal(VulkanFrameCommands frame) {
            if (overlay == null) {
                throw new IllegalStateException("Vulkan surface has no independently owned overlay");
            }
            return prepareAndPresentFrame(new ObjectFrameSource(0L, frame), true);
        }

        private VulkanSurfaceInfo prepareAndPresentFrame(FrameSource frame,
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

        private VulkanSurfaceInfo presentFrame(FrameSource frame,
                                               boolean retryOutOfDate,
                                               boolean revealBeforePresent) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                if (Boolean.getBoolean("rusted.fabric.vulkan.debugRenderTargetPasses")
                        && !debugMainTargetSamplesLogged
                        && frame instanceof ObjectFrameSource) {
                    VulkanFrameCommands objectFrame = ((ObjectFrameSource) frame).frame;
                    int logged = 0;
                    for (int commandIndex = 0; commandIndex < objectFrame.commandCount();
                         commandIndex++) {
                        VulkanDrawCommand draw = objectFrame.command(commandIndex);
                        if (!(draw instanceof VulkanTexturedQuad)) continue;
                        VulkanTexturedQuad quad = (VulkanTexturedQuad) draw;
                        TextureResource sampled = textures.get(
                                resolveTextureHandle(quad.textureHandle()));
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
                // First uploads target images that cannot yet be sampled by an in-flight frame.
                // Only mutation of an initialized image requires all image users to finish.
                if (pendingTextureMutationRequiresGlobalWait()) {
                    int textureFenceWait = waitForAllSubmissionFences(
                            stack, fenceTimeout);
                    if (textureFenceWait == VK_TIMEOUT || textureFenceWait == VK_NOT_READY) {
                        fenceWaitSkips++;
                        return null;
                    }
                    check(textureFenceWait, "vkWaitForFences(texture uploads)");
                    textureMutationFenceWaits++;
                    destroyRetiredTextures();
                } else if (!retiredTextures.isEmpty() && allSubmissionFencesSignaled()) {
                    destroyRetiredTextures();
                }
                long afterFence = System.nanoTime();
                int graphCount = frameGraphPasses.size();
                FrameUpload[] graphUploads = graphCount == 0
                        ? null : new FrameUpload[graphCount];
                TextureResource[] graphTargets = graphCount == 0
                        ? null : new TextureResource[graphCount];
                int[] graphSlots = graphCount == 0 ? null : new int[graphCount];
                for (int graphIndex = 0; graphIndex < graphCount; graphIndex++) {
                    FrameSource pass = frameGraphPasses.get(graphIndex);
                    TextureResource target = requireRenderTarget(
                            pass.targetHandle(), pass);
                    int graphSlot = (offscreenCursor + graphIndex)
                            % offscreenVertexAllocations.length;
                    check(vkWaitForFences(device,
                                    stack.longs(offscreenSlotOwnerFences[graphSlot]), true, -1L),
                            "vkWaitForFences(frame graph slot)");
                    graphTargets[graphIndex] = target;
                    graphSlots[graphIndex] = graphSlot;
                    graphUploads[graphIndex] = pass.upload(stack, graphSlot,
                            offscreenVertexAllocations, offscreenVertexCapacities);
                }
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
                FrameUpload upload = frame.upload(stack, frameSlot,
                        vertexAllocations, vertexCapacities);
                long afterVertexUpload = System.nanoTime();
                VkCommandBuffer commandBuffer = commandBuffers[frameSlot];
                check(vkResetCommandBuffer(commandBuffer, 0), "vkResetCommandBuffer");
                VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack)
                        .sType$Default().flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
                check(vkBeginCommandBuffer(commandBuffer, beginInfo), "vkBeginCommandBuffer");
                // This frame slot owns its persistently mapped staging allocation until the same
                // in-flight fence becomes reusable.
                recordPendingTextureUploads(commandBuffer, stack, frameSlot);
                for (int graphIndex = 0; graphIndex < graphCount; graphIndex++) {
                    FrameSource pass = frameGraphPasses.get(graphIndex);
                    recordFrameGraphTarget(commandBuffer, stack, pass.targetHandle(),
                            pass, graphTargets[graphIndex], graphUploads[graphIndex],
                            graphSlots[graphIndex]);
                }
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
                ByteBuffer shaderPushConstants =
                        stack.malloc(128).order(ByteOrder.nativeOrder());
                LongBuffer drawVertexBuffer = stack.mallocLong(1).put(0, vertexBuffer);
                LongBuffer drawVertexOffset = stack.mallocLong(1);
                LongBuffer drawDescriptorSet = stack.mallocLong(1);
                VkRect2D.Buffer drawScissor = VkRect2D.calloc(1, stack);
                for (DrawBatch drawBatch : upload.batches) {
                    if (drawBatch instanceof ColoredDrawBatch) {
                        ColoredDrawBatch batch = (ColoredDrawBatch) drawBatch;
                        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS,
                                colorPipeline(stack, batch.blendMode));
                        drawVertexOffset.put(0, batch.vertexByteOffset);
                        vkCmdBindVertexBuffers(commandBuffer, 0,
                                drawVertexBuffer, drawVertexOffset);
                        if (setScissor(commandBuffer, batch.clip, info.width(), info.height(),
                                drawScissor)) {
                            recordDraw(commandBuffer, batch, vertexBuffer);
                        }
                    } else {
                        TextureDrawBatch batch = (TextureDrawBatch) drawBatch;
                        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS,
                                texturePipeline(stack, batch.blendMode, batch.shaderState));
                        drawVertexOffset.put(0, batch.vertexByteOffset);
                        vkCmdBindVertexBuffers(commandBuffer, 0,
                                drawVertexBuffer, drawVertexOffset);
                        if (setScissor(commandBuffer, batch.clip, info.width(), info.height(),
                                drawScissor)) {
                            drawDescriptorSet.put(0, batch.descriptorSet);
                            vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS,
                                    texturePipelineLayout, 0,
                                    drawDescriptorSet, null);
                            pushShaderState(commandBuffer, batch.shaderState,
                                    shaderPushConstants);
                            recordDraw(commandBuffer, batch, vertexBuffer);
                        }
                    }
                }
                releaseFrameUpload(upload);
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
                for (int graphIndex = 0; graphIndex < graphCount; graphIndex++) {
                    offscreenSlotOwnerFences[graphSlots[graphIndex]] = inFlightFence;
                }
                if (graphCount > 0) {
                    offscreenCursor = (offscreenCursor + graphCount)
                            % offscreenVertexAllocations.length;
                    frameGraphSubmitted = true;
                    frameGraphQueueSubmissions++;
                    frameGraphPassesSubmitted += graphCount;
                }
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
                                + ", commands=" + frame.drawCount());
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
                                + frame.drawCount());
                    }
                }
                return info;
            }
        }

        private static double elapsedMillis(long started, long finished) {
            return Math.round((finished - started) / 10_000.0) / 100.0;
        }

        private boolean allSubmissionFencesSignaled() {
            for (long fence : inFlightFences) {
                int status = vkGetFenceStatus(device, fence);
                if (status == VK_NOT_READY) return false;
                check(status, "vkGetFenceStatus");
            }
            for (long fence : offscreenFences) {
                int status = vkGetFenceStatus(device, fence);
                if (status == VK_NOT_READY) return false;
                check(status, "vkGetFenceStatus(offscreen)");
            }
            return true;
        }

        private void waitForAllSubmissionFences(MemoryStack stack) {
            check(waitForAllSubmissionFences(stack, -1L),
                    "vkWaitForFences(all submissions)");
        }

        private int waitForAllSubmissionFences(MemoryStack stack, long timeout) {
            LongBuffer fences = stack.mallocLong(
                    inFlightFences.length + offscreenFences.length);
            fences.put(inFlightFences).put(offscreenFences).flip();
            return vkWaitForFences(device, fences, true, timeout);
        }

        private int targetWidth(FrameSource frame) {
            return nativeWindow == null ? frame.width() : nativeWindow.clientWidth();
        }

        private int targetHeight(FrameSource frame) {
            return nativeWindow == null ? frame.height() : nativeWindow.clientHeight();
        }

        private FrameUpload acquireFrameUpload(long texturedByteOffset,
                                               long customTexturedByteOffset) {
            FrameUpload upload = frameUploadPool.pollFirst();
            if (upload == null) {
                upload = new FrameUpload();
                frameUploadAllocations++;
            }
            upload.texturedByteOffset = texturedByteOffset;
            upload.customTexturedByteOffset = customTexturedByteOffset;
            return upload;
        }

        private ColoredDrawBatch acquireColoredBatch(
                VulkanClipRect clip, VulkanBlendMode blendMode, int firstVertex) {
            ColoredDrawBatch batch = coloredBatchPool.pollFirst();
            if (batch == null) {
                batch = new ColoredDrawBatch();
                drawBatchAllocations++;
            }
            batch.reset(clip, blendMode, firstVertex);
            return batch;
        }

        private TextureDrawBatch acquireTextureBatch(
                long textureHandle, long descriptorSet, VulkanClipRect clip,
                VulkanBlendMode blendMode, VulkanShaderState shaderState,
                int firstVertex, boolean expandedVertexInput) {
            TextureDrawBatch batch = textureBatchPool.pollFirst();
            if (batch == null) {
                batch = new TextureDrawBatch();
                drawBatchAllocations++;
            }
            batch.reset(textureHandle, descriptorSet, clip, blendMode,
                    shaderState, firstVertex, expandedVertexInput);
            return batch;
        }

        private void releaseFrameUpload(FrameUpload upload) {
            if (upload == null) return;
            for (DrawBatch batch : upload.batches) {
                batch.clip = null;
                if (batch instanceof TextureDrawBatch) {
                    TextureDrawBatch texture = (TextureDrawBatch) batch;
                    texture.shaderState = null;
                    textureBatchPool.addFirst(texture);
                } else {
                    coloredBatchPool.addFirst((ColoredDrawBatch) batch);
                }
            }
            upload.batches.clear();
            frameUploadPool.addFirst(upload);
        }

        private static void recordDraw(VkCommandBuffer command, DrawBatch batch,
                                       long vertexBuffer) {
            if (batch.indexCount != 0) {
                vkCmdBindIndexBuffer(command, vertexBuffer,
                        batch.indexByteOffset, batch.indexType);
                vkCmdDrawIndexed(command, batch.indexCount, 1, 0, 0, 0);
            } else {
                vkCmdDraw(command, batch.vertexCount, 1, batch.firstVertex, 0);
            }
        }

        private FrameUpload uploadFrame(DecodedFrameStream stream, int passIndex,
                                        MemoryStack stack, int frameSlot,
                                        BufferAllocation[] allocations, int[] capacities) {
            DecodedFrameStream.Pass pass = stream.readPass(passIndex, stream.passCursor());
            FrameUpload result = acquireFrameUpload(0L, 0L);
            if (pass.batchCount() == 0) return result;
            DecodedFrameStream.Batch first = stream.readBatch(
                    pass.firstBatch(), stream.batchCursor());
            DecodedFrameStream.Batch last = stream.readBatch(
                    pass.firstBatch() + pass.batchCount() - 1, stream.batchCursor());
            int passVertexStart = first.vertexByteOffset();
            int passVertexEnd = Math.addExact(last.vertexByteOffset(),
                    Math.multiplyExact(last.vertexCount(),
                            FrameStreamRecordFormat.vertexStride(last.vertexLayout())));
            int passVertexBytes = passVertexEnd - passVertexStart;
            int passIndexStart = -1;
            int passIndexEnd = -1;
            DecodedFrameStream.Batch indexScan = stream.batchCursor();
            for (int relative = 0; relative < pass.batchCount(); relative++) {
                stream.readBatch(pass.firstBatch() + relative, indexScan);
                if ((indexScan.flags() & FrameStreamRecordFormat.BATCH_INDEXED) == 0) continue;
                int stride = indexScan.indexType() == FrameStreamRecordFormat.INDEX_UINT16
                        ? Short.BYTES : Integer.BYTES;
                if (passIndexStart < 0) passIndexStart = indexScan.indexByteOffset();
                passIndexEnd = Math.addExact(indexScan.indexByteOffset(),
                        Math.multiplyExact(indexScan.indexCount(), stride));
            }
            int passIndexBytes = passIndexStart < 0 ? 0 : passIndexEnd - passIndexStart;
            int indexUploadOffset = passVertexBytes;
            if (passIndexStart >= 0) {
                indexUploadOffset = Math.addExact(indexUploadOffset,
                        (passIndexStart - indexUploadOffset) & 3);
            }
            int uploadBytes = Math.addExact(indexUploadOffset, passIndexBytes);
            ensureVertexCapacity(frameSlot, uploadBytes, stack, allocations, capacities);
            ByteBuffer source = stream.vertices();
            source.position(passVertexStart).limit(passVertexEnd);
            ByteBuffer destination = allocations[frameSlot].mapped.duplicate()
                    .order(ByteOrder.nativeOrder());
            destination.clear().limit(uploadBytes);
            destination.put(source);
            while (destination.position() < indexUploadOffset) destination.put((byte) 0);
            if (passIndexBytes != 0) {
                ByteBuffer indexSource = stream.indices();
                indexSource.position(passIndexStart).limit(passIndexEnd);
                destination.put(indexSource);
            }
            try {
                DecodedFrameStream.Batch encoded = stream.batchCursor();
                DecodedFrameStream.Material material = stream.materialCursor();
                for (int relative = 0; relative < pass.batchCount(); relative++) {
                    stream.readBatch(pass.firstBatch() + relative, encoded);
                    VulkanClipRect clip = decodedClip(encoded);
                    stream.readMaterial(encoded.materialIndex(), material);
                    VulkanBlendMode blendMode = decodedBlendMode(material.blendMode());
                    DrawBatch batch;
                    if (encoded.vertexLayout() == FrameStreamRecordFormat.VERTEX_COLORED) {
                        batch = acquireColoredBatch(clip, blendMode, 0);
                    } else {
                        long primary = rawTextureHandle(encoded.primaryTexture(), false);
                        long secondary = rawTextureHandle(encoded.secondaryTexture(), true);
                        VulkanTextureFilter filter = decodedTextureFilter(
                                material.textureFilter());
                        VulkanShaderState shaderState = decodedShaderState(
                                encoded.materialIndex(), material, secondary);
                        long descriptorSet = textureDescriptor(primary, filter, shaderState);
                        batch = acquireTextureBatch(primary, descriptorSet, clip, blendMode,
                                shaderState, 0, encoded.vertexLayout()
                                        == FrameStreamRecordFormat.VERTEX_CUSTOM_TEXTURED);
                    }
                    batch.vertexByteOffset = encoded.vertexByteOffset() - passVertexStart;
                    batch.vertexCount = encoded.vertexCount();
                    if ((encoded.flags() & FrameStreamRecordFormat.BATCH_INDEXED) != 0) {
                        batch.indexByteOffset = indexUploadOffset
                                + encoded.indexByteOffset() - passIndexStart;
                        batch.indexCount = encoded.indexCount();
                        batch.indexType = encoded.indexType()
                                == FrameStreamRecordFormat.INDEX_UINT16
                                ? VK_INDEX_TYPE_UINT16 : VK_INDEX_TYPE_UINT32;
                    }
                    result.batches.add(batch);
                }
                return result;
            } catch (Throwable failure) {
                releaseFrameUpload(result);
                throw failure;
            }
        }

        private static VulkanClipRect decodedClip(DecodedFrameStream.Batch batch) {
            return (batch.flags() & FrameStreamRecordFormat.BATCH_HAS_CLIP) == 0
                    ? null : new VulkanClipRect(batch.clipX(), batch.clipY(),
                            batch.clipWidth(), batch.clipHeight());
        }

        private static VulkanBlendMode decodedBlendMode(int value) {
            switch (value) {
                case FrameStreamRecordFormat.MATERIAL_BLEND_NORMAL:
                    return VulkanBlendMode.NORMAL;
                case FrameStreamRecordFormat.MATERIAL_BLEND_ADDITIVE:
                    return VulkanBlendMode.ADDITIVE;
                case FrameStreamRecordFormat.MATERIAL_BLEND_COPY:
                    return VulkanBlendMode.COPY;
                case FrameStreamRecordFormat.MATERIAL_BLEND_MODULATE:
                    return VulkanBlendMode.MODULATE;
                default: throw new IllegalArgumentException("unknown FrameStream blend " + value);
            }
        }

        private static VulkanTextureFilter decodedTextureFilter(int value) {
            switch (value) {
                case FrameStreamRecordFormat.MATERIAL_FILTER_LINEAR:
                    return VulkanTextureFilter.LINEAR;
                case FrameStreamRecordFormat.MATERIAL_FILTER_NEAREST:
                    return VulkanTextureFilter.NEAREST;
                default: throw new IllegalArgumentException("unknown FrameStream filter " + value);
            }
        }

        private VulkanShaderState decodedShaderState(
                int materialIndex, DecodedFrameStream.Material material,
                long secondaryTexture) {
            if (decodedMaterialEpochs[materialIndex] == decodedMaterialEpoch
                    && decodedMaterialSecondaryTextures[materialIndex] == secondaryTexture) {
                decodedMaterialCacheHits++;
                return decodedMaterialShaders[materialIndex];
            }
            VulkanShaderState decoded;
            if (material.shaderEffect() == VulkanShaderState.CUSTOM) {
                float[] custom = new float[material.customValueCount()];
                for (int index = 0; index < custom.length; index++) {
                    custom[index] = material.customValue(index);
                }
                decoded = VulkanShaderState.custom(rawShaderHandle(material.shaderHandle()),
                        secondaryTexture, custom);
            } else {
                decoded = new VulkanShaderState(material.shaderEffect(),
                        material.shaderFloat(0), material.shaderFloat(1),
                        material.shaderFloat(2), material.shaderFloat(3),
                        material.shaderFloat(4), secondaryTexture,
                        material.shaderFloat(5), material.shaderFloat(6),
                        material.shaderFloat(7), material.shaderFloat(8),
                        material.shaderFloat(9), material.shaderFloat(10));
            }
            decodedMaterialShaders[materialIndex] = decoded;
            decodedMaterialSecondaryTextures[materialIndex] = secondaryTexture;
            decodedMaterialEpochs[materialIndex] = decodedMaterialEpoch;
            decodedMaterialCacheMisses++;
            return decoded;
        }

        private void beginDecodedMaterialFrame(int materialCount) {
            if (decodedMaterialShaders.length < materialCount) {
                int capacity = Math.max(16, decodedMaterialShaders.length);
                while (capacity < materialCount) capacity = Math.multiplyExact(capacity, 2);
                decodedMaterialShaders = Arrays.copyOf(decodedMaterialShaders, capacity);
                decodedMaterialSecondaryTextures = Arrays.copyOf(
                        decodedMaterialSecondaryTextures, capacity);
                decodedMaterialEpochs = Arrays.copyOf(decodedMaterialEpochs, capacity);
            }
            if (++decodedMaterialEpoch == 0) {
                Arrays.fill(decodedMaterialEpochs, 0);
                decodedMaterialEpoch = 1;
            }
        }

        private long rawTextureHandle(long handle, boolean allowNull) {
            if (handle == 0L && allowNull) return 0L;
            if (resourceStreamActive) return resolveTextureHandle(handle);
            if (handle == 0L || FrameResourceHandle.type(handle)
                    != FrameResourceHandle.TYPE_TEXTURE
                    || FrameResourceHandle.generation(handle) != 1) {
                throw new IllegalArgumentException(
                        "desktop FrameStream texture handle is not a generation-one slot");
            }
            long slot = FrameResourceHandle.slot(handle);
            if (slot == 0L) throw new IllegalArgumentException("texture slot zero is invalid");
            return slot;
        }

        private long rawShaderHandle(long handle) {
            if (resourceShaders.containsKey(handle)) return resolveShaderHandle(handle);
            if (handle == 0L || FrameResourceHandle.type(handle)
                    != FrameResourceHandle.TYPE_SHADER_PROGRAM
                    || FrameResourceHandle.generation(handle) != 1) {
                throw new IllegalArgumentException(
                        "desktop FrameStream shader handle is not a generation-one slot");
            }
            long slot = FrameResourceHandle.slot(handle);
            if (slot == 0L) throw new IllegalArgumentException("shader slot zero is invalid");
            return slot;
        }

        private FrameUpload uploadFrame(VulkanFrameCommands frame, MemoryStack stack,
                                        int frameSlot, BufferAllocation[] allocations,
                                        int[] capacities) {
            int coloredVertexCount = Math.addExact(
                    Math.multiplyExact(frame.coloredQuadCount(), 6),
                    Math.multiplyExact(frame.coloredTriangleCount(), 3));
            int texturedVertexCount = 0;
            int customTexturedVertexCount = 0;
            for (int commandIndex = 0; commandIndex < frame.commandCount(); commandIndex++) {
                VulkanDrawCommand command = frame.command(commandIndex);
                int vertices;
                VulkanShaderState shaderState;
                if (command instanceof VulkanTexturedQuad) {
                    vertices = 6;
                    shaderState = ((VulkanTexturedQuad) command).state().shaderState();
                } else if (command instanceof VulkanTexturedQuadBatch) {
                    VulkanTexturedQuadBatch batch = (VulkanTexturedQuadBatch) command;
                    vertices = Math.multiplyExact(batch.quadCount(), 6);
                    shaderState = batch.state().shaderState();
                } else if (command instanceof VulkanTexturedTriangle) {
                    vertices = 3;
                    shaderState = ((VulkanTexturedTriangle) command).state().shaderState();
                } else {
                    continue;
                }
                if (usesExpandedVertexInput(shaderState)) {
                    customTexturedVertexCount = Math.addExact(
                            customTexturedVertexCount, vertices);
                } else {
                    texturedVertexCount = Math.addExact(texturedVertexCount, vertices);
                }
            }
            int coloredBytes = Math.multiplyExact(coloredVertexCount, VERTEX_STRIDE);
            int texturedBytes = Math.multiplyExact(texturedVertexCount, TEXTURED_VERTEX_STRIDE);
            int customTexturedBytes = Math.multiplyExact(customTexturedVertexCount,
                    CUSTOM_TEXTURED_VERTEX_STRIDE);
            int customTexturedOffset = Math.addExact(coloredBytes, texturedBytes);
            int totalBytes = Math.addExact(customTexturedOffset, customTexturedBytes);
            FrameUpload result = acquireFrameUpload(coloredBytes, customTexturedOffset);
            if (totalBytes == 0) return result;
            ensureVertexCapacity(frameSlot, totalBytes, stack, allocations, capacities);
            BufferAllocation vertexAllocation = allocations[frameSlot];
            {
                ByteBuffer bytes = vertexAllocation.mapped.duplicate()
                        .order(ByteOrder.nativeOrder());
                bytes.clear().limit(totalBytes);
                ByteBuffer coloredSlice = bytes.duplicate();
                coloredSlice.limit(coloredBytes);
                FloatBuffer coloredVertices = coloredSlice.slice()
                        .order(ByteOrder.nativeOrder()).asFloatBuffer();
                ByteBuffer texturedSlice = bytes.duplicate();
                texturedSlice.position(coloredBytes).limit(customTexturedOffset);
                FloatBuffer texturedVertices = texturedSlice.slice()
                        .order(ByteOrder.nativeOrder()).asFloatBuffer();
                ByteBuffer customTexturedSlice = bytes.duplicate();
                customTexturedSlice.position(customTexturedOffset).limit(totalBytes);
                FloatBuffer customTexturedVertices = customTexturedSlice.slice()
                        .order(ByteOrder.nativeOrder()).asFloatBuffer();
                for (int commandIndex = 0; commandIndex < frame.commandCount(); commandIndex++) {
                    VulkanDrawCommand command = frame.command(commandIndex);
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
                    } else if (command instanceof VulkanTexturedQuad) {
                        VulkanTexturedQuad quad = (VulkanTexturedQuad) command;
                        if (!textures.containsKey(resolveTextureHandle(quad.textureHandle()))) {
                            throw new IllegalArgumentException(
                                    "unknown texture handle: " + quad.textureHandle());
                        }
                        VulkanTransform2D transform = quad.state().transform();
                        float left = quad.x();
                        float right = quad.x() + quad.width();
                        float top = quad.y();
                        float bottom = quad.y() + quad.height();
                        boolean expanded = usesExpandedVertexInput(
                                quad.state().shaderState());
                        FloatBuffer output = expanded
                                ? customTexturedVertices : texturedVertices;
                        putTexturedVertex(output, transform, left, top,
                                quad.u0(), quad.v0(), frame, quad, expanded);
                        putTexturedVertex(output, transform, left, bottom,
                                quad.u0(), quad.v1(), frame, quad, expanded);
                        putTexturedVertex(output, transform, right, bottom,
                                quad.u1(), quad.v1(), frame, quad, expanded);
                        putTexturedVertex(output, transform, left, top,
                                quad.u0(), quad.v0(), frame, quad, expanded);
                        putTexturedVertex(output, transform, right, bottom,
                                quad.u1(), quad.v1(), frame, quad, expanded);
                        putTexturedVertex(output, transform, right, top,
                                quad.u1(), quad.v0(), frame, quad, expanded);
                    } else if (command instanceof VulkanTexturedQuadBatch) {
                        VulkanTexturedQuadBatch batch = (VulkanTexturedQuadBatch) command;
                        if (!textures.containsKey(resolveTextureHandle(batch.textureHandle()))) {
                            throw new IllegalArgumentException(
                                    "unknown texture handle: " + batch.textureHandle());
                        }
                        VulkanTransform2D transform = batch.state().transform();
                        boolean expanded = usesExpandedVertexInput(
                                batch.state().shaderState());
                        FloatBuffer output = expanded
                                ? customTexturedVertices : texturedVertices;
                        for (int quad = 0; quad < batch.quadCount(); quad++) {
                            float left = batch.originX() + batch.x(quad);
                            float right = left + batch.width(quad);
                            float top = batch.originY() + batch.y(quad);
                            float bottom = top + batch.height(quad);
                            putTexturedVertex(output, transform, left, top,
                                    batch.u0(quad), batch.v0(quad), frame,
                                    batch.red(), batch.green(), batch.blue(), batch.alpha(),
                                    expanded);
                            putTexturedVertex(output, transform, left, bottom,
                                    batch.u0(quad), batch.v1(quad), frame,
                                    batch.red(), batch.green(), batch.blue(), batch.alpha(),
                                    expanded);
                            putTexturedVertex(output, transform, right, bottom,
                                    batch.u1(quad), batch.v1(quad), frame,
                                    batch.red(), batch.green(), batch.blue(), batch.alpha(),
                                    expanded);
                            putTexturedVertex(output, transform, left, top,
                                    batch.u0(quad), batch.v0(quad), frame,
                                    batch.red(), batch.green(), batch.blue(), batch.alpha(),
                                    expanded);
                            putTexturedVertex(output, transform, right, bottom,
                                    batch.u1(quad), batch.v1(quad), frame,
                                    batch.red(), batch.green(), batch.blue(), batch.alpha(),
                                    expanded);
                            putTexturedVertex(output, transform, right, top,
                                    batch.u1(quad), batch.v0(quad), frame,
                                    batch.red(), batch.green(), batch.blue(), batch.alpha(),
                                    expanded);
                        }
                    } else if (command instanceof VulkanTexturedTriangle) {
                        VulkanTexturedTriangle triangle = (VulkanTexturedTriangle) command;
                        if (!textures.containsKey(
                                resolveTextureHandle(triangle.textureHandle()))) {
                            throw new IllegalArgumentException(
                                    "unknown texture handle: " + triangle.textureHandle());
                        }
                        VulkanTransform2D transform = triangle.state().transform();
                        boolean expanded = usesExpandedVertexInput(
                                triangle.state().shaderState());
                        FloatBuffer output = expanded
                                ? customTexturedVertices : texturedVertices;
                        for (int vertex = 0; vertex < 3; vertex++) {
                            putTexturedTriangleVertex(output, transform,
                                    frame, triangle, vertex, expanded);
                        }
                    }
                }
            }
            int coloredFirstVertex = 0;
            int texturedFirstVertex = 0;
            int customTexturedFirstVertex = 0;
            DrawBatch currentBatch = null;
            for (int commandIndex = 0; commandIndex < frame.commandCount(); commandIndex++) {
                VulkanDrawCommand command = frame.command(commandIndex);
                if (command instanceof VulkanColoredQuad) {
                    VulkanColoredQuad quad = (VulkanColoredQuad) command;
                    if (!(currentBatch instanceof ColoredDrawBatch)
                            || !sameClip(currentBatch.clip, quad.state().clip())
                            || currentBatch.blendMode != quad.state().blendMode()) {
                        currentBatch = acquireColoredBatch(
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
                        currentBatch = acquireColoredBatch(
                                triangle.state().clip(), triangle.state().blendMode(),
                                coloredFirstVertex);
                        result.batches.add(currentBatch);
                    }
                    currentBatch.vertexCount += 3;
                    coloredFirstVertex += 3;
                } else if (command instanceof VulkanTexturedQuad) {
                    VulkanTexturedQuad quad = (VulkanTexturedQuad) command;
                    boolean expanded = usesExpandedVertexInput(
                            quad.state().shaderState());
                    long descriptorSet = textureDescriptor(quad.textureHandle(),
                            quad.state().textureFilter(), quad.state().shaderState());
                    TextureDrawBatch textureBatch = currentBatch instanceof TextureDrawBatch
                            ? (TextureDrawBatch) currentBatch : null;
                    if (textureBatch == null
                            || textureBatch.textureHandle != quad.textureHandle()
                            || textureBatch.descriptorSet != descriptorSet
                            || textureBatch.expandedVertexInput != expanded
                            || !sameClip(textureBatch.clip, quad.state().clip())
                            || textureBatch.blendMode != quad.state().blendMode()
                            || !textureBatch.shaderState.equals(
                                    quad.state().shaderState())) {
                        currentBatch = acquireTextureBatch(quad.textureHandle(),
                                descriptorSet,
                                quad.state().clip(),
                                quad.state().blendMode(), quad.state().shaderState(),
                                expanded ? customTexturedFirstVertex : texturedFirstVertex,
                                expanded);
                        result.batches.add(currentBatch);
                    }
                    currentBatch.vertexCount += 6;
                    if (expanded) customTexturedFirstVertex += 6;
                    else texturedFirstVertex += 6;
                } else if (command instanceof VulkanTexturedQuadBatch) {
                    VulkanTexturedQuadBatch batch = (VulkanTexturedQuadBatch) command;
                    boolean expanded = usesExpandedVertexInput(
                            batch.state().shaderState());
                    long descriptorSet = textureDescriptor(batch.textureHandle(),
                            batch.state().textureFilter(), batch.state().shaderState());
                    TextureDrawBatch textureBatch = currentBatch instanceof TextureDrawBatch
                            ? (TextureDrawBatch) currentBatch : null;
                    if (textureBatch == null
                            || textureBatch.textureHandle != batch.textureHandle()
                            || textureBatch.descriptorSet != descriptorSet
                            || textureBatch.expandedVertexInput != expanded
                            || !sameClip(textureBatch.clip, batch.state().clip())
                            || textureBatch.blendMode != batch.state().blendMode()
                            || !textureBatch.shaderState.equals(batch.state().shaderState())) {
                        currentBatch = acquireTextureBatch(batch.textureHandle(), descriptorSet,
                                batch.state().clip(), batch.state().blendMode(),
                                batch.state().shaderState(), expanded
                                        ? customTexturedFirstVertex : texturedFirstVertex,
                                expanded);
                        result.batches.add(currentBatch);
                    }
                    int vertices = Math.multiplyExact(batch.quadCount(), 6);
                    currentBatch.vertexCount += vertices;
                    if (expanded) customTexturedFirstVertex += vertices;
                    else texturedFirstVertex += vertices;
                } else if (command instanceof VulkanTexturedTriangle) {
                    VulkanTexturedTriangle triangle = (VulkanTexturedTriangle) command;
                    boolean expanded = usesExpandedVertexInput(
                            triangle.state().shaderState());
                    long descriptorSet = textureDescriptor(triangle.textureHandle(),
                            triangle.state().textureFilter(), triangle.state().shaderState());
                    TextureDrawBatch textureBatch = currentBatch instanceof TextureDrawBatch
                            ? (TextureDrawBatch) currentBatch : null;
                    if (textureBatch == null
                            || textureBatch.textureHandle != triangle.textureHandle()
                            || textureBatch.descriptorSet != descriptorSet
                            || textureBatch.expandedVertexInput != expanded
                            || !sameClip(textureBatch.clip, triangle.state().clip())
                            || textureBatch.blendMode != triangle.state().blendMode()
                            || !textureBatch.shaderState.equals(
                                    triangle.state().shaderState())) {
                        currentBatch = acquireTextureBatch(triangle.textureHandle(),
                                descriptorSet,
                                triangle.state().clip(),
                                triangle.state().blendMode(), triangle.state().shaderState(),
                                expanded ? customTexturedFirstVertex : texturedFirstVertex,
                                expanded);
                        result.batches.add(currentBatch);
                    }
                    currentBatch.vertexCount += 3;
                    if (expanded) customTexturedFirstVertex += 3;
                    else texturedFirstVertex += 3;
                } else {
                    throw new IllegalArgumentException("unsupported draw command: "
                            + command.getClass().getName());
                }
            }
            for (DrawBatch batch : result.batches) {
                if (batch instanceof ColoredDrawBatch) {
                    batch.vertexByteOffset = 0L;
                } else {
                    TextureDrawBatch textured = (TextureDrawBatch) batch;
                    batch.vertexByteOffset = textured.expandedVertexInput
                            ? result.customTexturedByteOffset : result.texturedByteOffset;
                }
            }
            return result;
        }

        private void ensureVertexCapacity(int frameSlot, int requiredBytes,
                                          MemoryStack stack,
                                          BufferAllocation[] allocations,
                                          int[] capacities) {
            BufferAllocation current = allocations[frameSlot];
            if (current != null && capacities[frameSlot] >= requiredBytes) return;
            if (current != null) destroyBufferAllocation(current);
            int vertexCapacity = 64 * 1024;
            while (vertexCapacity < requiredBytes) {
                vertexCapacity = Math.multiplyExact(vertexCapacity, 2);
            }
            BufferAllocation created = createBufferAllocation(stack, vertexCapacity,
                    VK_BUFFER_USAGE_VERTEX_BUFFER_BIT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT,
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT
                            | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
            try {
                PointerBuffer mapped = stack.mallocPointer(1);
                check(vkMapMemory(device, created.memory, 0, vertexCapacity, 0, mapped),
                        "vkMapMemory(persistent vertex buffer)");
                created.mapped = MemoryUtil.memByteBuffer(mapped.get(0), vertexCapacity)
                        .order(ByteOrder.nativeOrder());
            } catch (Throwable failure) {
                destroyBufferAllocation(created);
                throw failure;
            }
            allocations[frameSlot] = created;
            capacities[frameSlot] = vertexCapacity;
        }

        private boolean usesExpandedVertexInput(VulkanShaderState shaderState) {
            if (shaderState.effect() != VulkanShaderState.CUSTOM) return false;
            long shaderHandle = resolveShaderHandle(shaderState.customShaderHandle());
            CustomShaderResource shader = customShaders.get(shaderHandle);
            if (shader == null) {
                throw new IllegalArgumentException("unknown custom shader handle "
                        + shaderState.customShaderHandle());
            }
            return shader.expandedVertexInput;
        }

        private void pushShaderState(VkCommandBuffer commandBuffer,
                                     VulkanShaderState shaderState,
                                     ByteBuffer values) {
            for (int index = 0; index < 128; index++) values.put(index, (byte) 0);
            values.putFloat(0, shaderState.red());
            values.putFloat(4, shaderState.green());
            values.putFloat(8, shaderState.blue());
            values.putFloat(12, shaderState.alpha());
            values.putFloat(16, shaderState.teamColorAmount());
            values.putInt(20, shaderState.effect());
            values.putFloat(24, shaderState.resolutionWidth());
            values.putFloat(28, shaderState.resolutionHeight());
            values.putFloat(32, shaderState.displacementOffset());
            values.putFloat(36, shaderState.uiScaling());
            values.putFloat(40, shaderState.screenBaseWidth());
            values.putFloat(44, shaderState.screenBaseHeight());
            float[] custom = shaderState.customValues();
            for (int index = 0; index < custom.length; index++) {
                values.putFloat(48 + index * Float.BYTES, custom[index]);
            }
            vkCmdPushConstants(commandBuffer, texturePipelineLayout,
                    VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT,
                    0, values.position(0).limit(128));
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
                                              VulkanTexturedQuad quad,
                                              boolean expandedVertexInput) {
            putTexturedVertex(output, transform, x, y, u, v, frame,
                    quad.red(), quad.green(), quad.blue(), quad.alpha(), expandedVertexInput);
        }

        private static void putTexturedVertex(FloatBuffer output, VulkanTransform2D transform,
                                              float x, float y, float u, float v,
                                              VulkanFrameCommands frame,
                                              float red, float green, float blue, float alpha,
                                              boolean expandedVertexInput) {
            if (expandedVertexInput) {
                putCustomTexturedVertex(output, transform, x, y, u, v, frame,
                        red, green, blue, alpha);
                return;
            }
            float transformedX = transform.transformX(x, y);
            float transformedY = transform.transformY(x, y);
            output.put(pixelToNdcX(transformedX, frame.width()))
                    .put(pixelToNdcY(transformedY, frame.height())).put(u).put(v)
                    .put(red).put(green).put(blue).put(alpha);
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
                VulkanFrameCommands frame, VulkanTexturedTriangle triangle, int vertex,
                boolean expandedVertexInput) {
            float x = triangle.x(vertex);
            float y = triangle.y(vertex);
            if (expandedVertexInput) {
                putCustomTexturedVertex(output, transform, x, y,
                        triangle.u(vertex), triangle.v(vertex), frame,
                        triangle.red(vertex), triangle.green(vertex),
                        triangle.blue(vertex), triangle.alpha(vertex));
                return;
            }
            output.put(pixelToNdcX(transform.transformX(x, y), frame.width()))
                    .put(pixelToNdcY(transform.transformY(x, y), frame.height()))
                    .put(triangle.u(vertex)).put(triangle.v(vertex))
                    .put(triangle.red(vertex)).put(triangle.green(vertex))
                    .put(triangle.blue(vertex)).put(triangle.alpha(vertex));
        }

        private static void putCustomTexturedVertex(
                FloatBuffer output, VulkanTransform2D transform,
                float x, float y, float u, float v, VulkanFrameCommands frame,
                float red, float green, float blue, float alpha) {
            output.put(x).put(y).put(u).put(v)
                    .put(red).put(green).put(blue).put(alpha)
                    .put(transform.m00()).put(transform.m01()).put(transform.m02())
                    .put(transform.m10()).put(transform.m11()).put(transform.m12())
                    .put(frame.width()).put(frame.height());
        }

        private boolean setScissor(VkCommandBuffer commandBuffer, VulkanClipRect clip,
                                   int targetWidth, int targetHeight,
                                   VkRect2D.Buffer scissor) {
            int left = clip == null ? 0 : Math.max(0, (int) Math.floor(clip.x()));
            int top = clip == null ? 0 : Math.max(0, (int) Math.floor(clip.y()));
            int right = clip == null ? targetWidth
                    : Math.min(targetWidth, (int) Math.ceil(clip.x() + clip.width()));
            int bottom = clip == null ? targetHeight
                    : Math.min(targetHeight, (int) Math.ceil(clip.y() + clip.height()));
            if (right <= left || bottom <= top) return false;
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
            for (long fence : offscreenFences) {
                if (fence != VK_NULL_HANDLE) vkDestroyFence(device, fence, null);
            }
            offscreenFences = new long[0];
            offscreenSlotOwnerFences = new long[0];
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
            for (BufferAllocation allocation : offscreenVertexAllocations) {
                if (allocation != null) destroyBufferAllocation(allocation);
            }
            offscreenVertexAllocations = new BufferAllocation[0];
            offscreenVertexCapacities = new int[0];
            for (BufferAllocation allocation : textureUploadSlots) {
                if (allocation != null) destroyBufferAllocation(allocation);
            }
            textureUploadSlots = new BufferAllocation[0];
            textureUploadCapacities = new int[0];
            if (commandPool != 0L) {
                vkDestroyCommandPool(device, commandPool, null);
                commandPool = 0L;
                commandBuffers = new VkCommandBuffer[0];
                offscreenCommandBuffers = new VkCommandBuffer[0];
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
            for (CustomShaderResource shader : customShaders.values()) {
                shader.destroyPipelines(device);
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
            customShaders.clear();
            if (textureDescriptorPool != VK_NULL_HANDLE) {
                vkDestroyDescriptorPool(device, textureDescriptorPool, null);
                textureDescriptorPool = VK_NULL_HANDLE;
            }
            for (TextureResource texture : textures.values()) {
                destroyTextureResource(texture, false);
            }
            textures.clear();
            pendingTextureUploads.clear();
            pendingResourceReadbacks.clear();
            while (!retiredTextures.isEmpty()) {
                destroyTextureResource(retiredTextures.removeFirst(), false);
            }
            if (textureDescriptorSetLayout != VK_NULL_HANDLE) {
                vkDestroyDescriptorSetLayout(device, textureDescriptorSetLayout, null);
                textureDescriptorSetLayout = VK_NULL_HANDLE;
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
            if (debugMessenger != null) debugMessenger.close(instance);
            vkDestroyInstance(instance, null);
            if (overlay != null) overlay.close();
            if (nativeWindow != null) nativeWindow.close();
        }

        private static final class BufferAllocation {
            private long buffer;
            private long memory;
            private ByteBuffer mapped;
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
            private final List<Long> dependentDescriptorSets = new ArrayList<Long>();
        }

        private static final class ResourceTextureBinding {
            private final long rawHandle;
            private final int width, height;
            private final boolean renderTarget;

            private ResourceTextureBinding(long rawHandle, int width, int height,
                                           boolean renderTarget) {
                this.rawHandle = rawHandle;
                this.width = width;
                this.height = height;
                this.renderTarget = renderTarget;
            }
        }

        private static final class PendingResourceReadback {
            private final long rawTextureHandle;
            private final int x;
            private final int y;
            private final int width;
            private final int height;
            private final long appliedSequence;

            private PendingResourceReadback(long rawTextureHandle, int x, int y,
                                            int width, int height, long appliedSequence) {
                this.rawTextureHandle = rawTextureHandle;
                this.x = x;
                this.y = y;
                this.width = width;
                this.height = height;
                this.appliedSequence = appliedSequence;
            }

            private PendingResourceReadback withSequence(long sequence) {
                return new PendingResourceReadback(rawTextureHandle, x, y,
                        width, height, sequence);
            }
        }

        private static final class CustomShaderResource {
            private final String name;
            private final String vertexSource;
            private final String fragmentSource;
            private final boolean expandedVertexInput;
            private final long[] pipelines = new long[VulkanBlendMode.values().length];

            private CustomShaderResource(String name, String vertexSource,
                                         String fragmentSource, boolean expandedVertexInput) {
                this.name = name;
                this.vertexSource = vertexSource;
                this.fragmentSource = fragmentSource;
                this.expandedVertexInput = expandedVertexInput;
            }

            private void destroyPipelines(VkDevice device) {
                for (int index = 0; index < pipelines.length; index++) {
                    if (pipelines[index] != VK_NULL_HANDLE) {
                        vkDestroyPipeline(device, pipelines[index], null);
                        pipelines[index] = VK_NULL_HANDLE;
                    }
                }
            }

            private void destroy(VkDevice device) {
                destroyPipelines(device);
            }
        }

        private static final class TextureDescriptorKey {
            private final long primary;
            private final long secondary;
            private final VulkanTextureFilter filter;

            private TextureDescriptorKey(long primary, long secondary,
                                         VulkanTextureFilter filter) {
                this.primary = primary;
                this.secondary = secondary;
                this.filter = filter;
            }

            private boolean uses(long textureHandle) {
                return primary == textureHandle || secondary == textureHandle;
            }

            @Override public boolean equals(Object other) {
                if (this == other) return true;
                if (!(other instanceof TextureDescriptorKey)) return false;
                TextureDescriptorKey key = (TextureDescriptorKey) other;
                return primary == key.primary && secondary == key.secondary
                        && filter == key.filter;
            }

            @Override public int hashCode() {
                int result = Long.hashCode(primary);
                result = 31 * result + Long.hashCode(secondary);
                result = 31 * result + filter.hashCode();
                return result;
            }
        }

        private static final class PendingTextureUpload {
            private final long textureHandle;
            private final int x, y;
            private final VulkanTextureData texture;
            private final boolean initialized;

            private PendingTextureUpload(long textureHandle, int x, int y,
                                         VulkanTextureData texture, boolean initialized) {
                this.textureHandle = textureHandle;
                this.x = x;
                this.y = y;
                this.texture = texture;
                this.initialized = initialized;
            }
        }

        private abstract static class DrawBatch {
            VulkanClipRect clip;
            VulkanBlendMode blendMode;
            long vertexByteOffset;
            int firstVertex;
            int vertexCount;
            long indexByteOffset;
            int indexCount;
            int indexType;

            void reset(VulkanClipRect clip, VulkanBlendMode blendMode,
                       int firstVertex) {
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

        private static final class TextureDrawBatch extends DrawBatch {
            private long textureHandle;
            private long descriptorSet;
            private VulkanShaderState shaderState;
            private boolean expandedVertexInput;

            private void reset(long textureHandle, long descriptorSet,
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

        private static final class ColoredDrawBatch extends DrawBatch { }

        private static final class FrameUpload {
            private long texturedByteOffset;
            private long customTexturedByteOffset;
            private final List<DrawBatch> batches = new ArrayList<DrawBatch>();

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
