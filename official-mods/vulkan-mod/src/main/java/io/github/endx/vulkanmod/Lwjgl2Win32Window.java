package io.github.endx.vulkanmod;

import io.github.endx.vulkanmod.spi.VulkanSurfaceRequest;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Reads the existing LWJGL 2 Win32 window without adding its classes to the mod ABI. */
final class Lwjgl2Win32Window {
    private Lwjgl2Win32Window() { }

    static VulkanSurfaceRequest current() {
        try {
            ClassLoader loader = Lwjgl2Win32Window.class.getClassLoader();
            Class<?> display = Class.forName("org.lwjgl.opengl.Display", false, loader);
            if (!((Boolean) display.getMethod("isCreated").invoke(null)).booleanValue()) {
                throw new IllegalStateException("LWJGL 2 display has not been created");
            }
            Field implementationField = display.getDeclaredField("display_impl");
            implementationField.setAccessible(true);
            Object implementation = implementationField.get(null);
            Class<?> windowsDisplay = implementation.getClass();
            if (!"org.lwjgl.opengl.WindowsDisplay".equals(windowsDisplay.getName())) {
                throw new IllegalStateException("Unsupported desktop display: "
                        + windowsDisplay.getName());
            }
            Field hwndField = windowsDisplay.getDeclaredField("hwnd");
            hwndField.setAccessible(true);
            Method getDllInstance = windowsDisplay.getDeclaredMethod("getDllInstance");
            getDllInstance.setAccessible(true);
            long hwnd = hwndField.getLong(implementation);
            long hinstance = ((Long) getDllInstance.invoke(null)).longValue();
            int width = ((Integer) display.getMethod("getWidth").invoke(null)).intValue();
            int height = ((Integer) display.getMethod("getHeight").invoke(null)).intValue();
            return VulkanSurfaceRequest.win32(hwnd, hinstance, width, height);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Could not obtain the LWJGL 2 Win32 window", failure);
        }
    }
}
