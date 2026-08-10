package io.github.endx.vulkanmod.lwjgl3;

import io.github.endx.vulkanmod.spi.VulkanDeviceInfo;
import io.github.endx.vulkanmod.spi.VulkanPlatformDriver;
import io.github.endx.vulkanmod.spi.VulkanProbeResult;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK;
import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK11.VK_API_VERSION_1_1;

/** LWJGL 3 desktop probe, loaded in a child-first class loader beside the LWJGL 2 game. */
public final class Lwjgl3VulkanDriver implements VulkanPlatformDriver {
    @Override public String name() { return "LWJGL 3 Vulkan"; }

    @Override public VulkanProbeResult probe() {
        try {
            int instanceVersion = VK.getInstanceVersionSupported();
            try (MemoryStack stack = MemoryStack.stackPush()) {
                VkApplicationInfo application = VkApplicationInfo.calloc(stack)
                        .sType$Default()
                        .pApplicationName(stack.UTF8("Rusted Fabric Vulkan Mod"))
                        .applicationVersion(1)
                        .pEngineName(stack.UTF8("Rusted Fabric"))
                        .engineVersion(1)
                        .apiVersion(Math.min(instanceVersion, VK_API_VERSION_1_1));
                VkInstanceCreateInfo createInfo = VkInstanceCreateInfo.calloc(stack)
                        .sType$Default().pApplicationInfo(application);
                PointerBuffer instancePointer = stack.mallocPointer(1);
                check(vkCreateInstance(createInfo, null, instancePointer), "vkCreateInstance");
                VkInstance instance = new VkInstance(instancePointer.get(0), createInfo);
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

    private static List<VulkanDeviceInfo> enumerateDevices(
            MemoryStack stack, VkInstance instance) {
        IntBuffer count = stack.ints(0);
        check(vkEnumeratePhysicalDevices(instance, count, null),
                "vkEnumeratePhysicalDevices(count)");
        PointerBuffer pointers = stack.mallocPointer(count.get(0));
        check(vkEnumeratePhysicalDevices(instance, count, pointers),
                "vkEnumeratePhysicalDevices(list)");
        List<VulkanDeviceInfo> result = new ArrayList<VulkanDeviceInfo>(count.get(0));
        VkPhysicalDeviceProperties properties = VkPhysicalDeviceProperties.malloc(stack);
        for (int index = 0; index < count.get(0); index++) {
            VkPhysicalDevice device = new VkPhysicalDevice(pointers.get(index), instance);
            vkGetPhysicalDeviceProperties(device, properties);
            result.add(new VulkanDeviceInfo(properties.deviceNameString(),
                    properties.vendorID(), properties.deviceID(), properties.deviceType(),
                    properties.apiVersion(), properties.driverVersion()));
        }
        return result;
    }

    private static void check(int result, String operation) {
        if (result != VK_SUCCESS) {
            throw new IllegalStateException(operation + " failed with VkResult " + result);
        }
    }
}
