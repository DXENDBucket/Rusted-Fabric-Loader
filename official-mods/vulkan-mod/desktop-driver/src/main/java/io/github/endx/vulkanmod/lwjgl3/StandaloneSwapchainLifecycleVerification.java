package io.github.endx.vulkanmod.lwjgl3;

import io.github.endx.vulkanmod.spi.VulkanDrawState;
import io.github.endx.vulkanmod.spi.VulkanFrameCommands;
import io.github.endx.vulkanmod.spi.VulkanWindowRequest;
import org.lwjgl.system.windows.User32;

import java.lang.reflect.Field;
import java.util.Map;

/** Resizes a real Win32 surface and verifies every WSI generation is retired exactly once. */
public final class StandaloneSwapchainLifecycleVerification {
    private StandaloneSwapchainLifecycleVerification() { }

    public static void main(String[] arguments) {
        try (Lwjgl3VulkanDriver driver = new Lwjgl3VulkanDriver()) {
            driver.createNativeWindowSurface(new VulkanWindowRequest(
                    "RustedVK swapchain lifecycle verification", 64, 64, false));
            present(driver);
            require(stat(driver, "swapchain.generations") == 1L
                            && stat(driver, "swapchain.recreates") == 0L
                            && stat(driver, "swapchain.handleDestroys") == 0L,
                    "initial WSI generation accounting is invalid");
            requireLiveGeneration(driver);

            resize(driver, 128, 96);
            present(driver);
            require(stat(driver, "swapchain.generations") == 2L
                            && stat(driver, "swapchain.recreates") == 1L
                            && stat(driver, "swapchain.handleDestroys") == 1L,
                    "first resized WSI generation was not retired");
            requireLiveGeneration(driver);

            resize(driver, 192, 128);
            present(driver);
            require(stat(driver, "swapchain.generations") == 3L
                            && stat(driver, "swapchain.recreates") == 2L
                            && stat(driver, "swapchain.handleDestroys") == 2L,
                    "second resized WSI generation was not retired");
            requireLiveGeneration(driver);
            require(stat(driver, "pipeline.renderPassChanges") == 3L,
                    "pipeline library did not observe every WSI render-pass generation");
        }
        System.out.println("Native Vulkan swapchain lifecycle passed across two resizes");
    }

    private static void resize(Lwjgl3VulkanDriver driver, int width, int height) {
        long window = nativeWindowHandle(driver);
        require(window != 0L && User32.SetWindowPos(null, window, 0L,
                        0, 0, width, height,
                        User32.SWP_NOMOVE | User32.SWP_NOZORDER | User32.SWP_NOACTIVATE),
                "could not resize verification window");
        driver.maintainSurfaceWindow();
    }

    private static void present(Lwjgl3VulkanDriver driver) {
        VulkanFrameCommands frame = VulkanFrameCommands.builder(64, 64)
                .clear(0, 0, 0, 1)
                .coloredQuad(0, 0, 32, 32, 0, 1, 0, 1, VulkanDrawState.DEFAULT)
                .build();
        if (driver.presentFrame(frame) == null) {
            throw new AssertionError("verification frame was not presented");
        }
    }

    private static void requireLiveGeneration(Lwjgl3VulkanDriver driver) {
        long images = stat(driver, "swapchain.images");
        require(images > 0L
                        && stat(driver, "swapchain.imageViewsLive") == images
                        && stat(driver, "swapchain.framebuffersLive") == images
                        && stat(driver, "swapchain.semaphoresLive") == images,
                "live WSI resources do not match the current swapchain image count");
        require(stat(driver, "swapchain.imageViewCreates")
                            - stat(driver, "swapchain.imageViewDestroys") == images
                        && stat(driver, "swapchain.framebufferCreates")
                            - stat(driver, "swapchain.framebufferDestroys") == images
                        && stat(driver, "swapchain.semaphoreCreates")
                            - stat(driver, "swapchain.semaphoreDestroys") == images,
                "retired WSI resource counters are unbalanced");
    }

    private static long nativeWindowHandle(Lwjgl3VulkanDriver driver) {
        try {
            Field sessionField = Lwjgl3VulkanDriver.class.getDeclaredField("surfaceSession");
            sessionField.setAccessible(true);
            Object session = sessionField.get(driver);
            Field windowField = session.getClass().getDeclaredField("nativeWindow");
            windowField.setAccessible(true);
            Object window = windowField.get(session);
            Field handleField = window.getClass().getDeclaredField("handle");
            handleField.setAccessible(true);
            return handleField.getLong(window);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("could not access verification window", failure);
        }
    }

    private static long stat(Lwjgl3VulkanDriver driver, String name) {
        Map<String, Long> statistics = driver.performanceStatistics();
        Long value = statistics.get(name);
        if (value == null) throw new AssertionError("missing statistic: " + name);
        return value.longValue();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
