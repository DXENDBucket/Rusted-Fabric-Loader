package io.github.endx.vulkanmod.lwjgl3;

import io.github.endx.vulkanmod.spi.VulkanDrawState;
import io.github.endx.vulkanmod.spi.VulkanFrameCommands;
import io.github.endx.vulkanmod.spi.VulkanTextureData;
import io.github.endx.vulkanmod.spi.VulkanWindowRequest;

import java.util.Map;

/** Exercises cached memory-type selection, persistent mapping, and image retirement accounting. */
public final class StandaloneMemoryAllocatorVerification {
    private StandaloneMemoryAllocatorVerification() { }

    public static void main(String[] arguments) {
        try (Lwjgl3VulkanDriver driver = new Lwjgl3VulkanDriver()) {
            driver.createNativeWindowSurface(new VulkanWindowRequest(
                    "RustedVK memory allocator verification", 32, 32, false));
            require(stat(driver, "memory.liveBytes") == 0L,
                    "allocator did not start without owned resources");

            long texture = driver.uploadTexture(new VulkanTextureData(
                    8, 8, new byte[8 * 8 * 4]));
            long afterImage = stat(driver, "memory.liveBytes");
            require(stat(driver, "memory.imageAllocations") == 1L && afterImage > 0L,
                    "texture image allocation was not accounted");

            present(driver, sprite(texture));
            for (int frame = 0; frame < 8; frame++) present(driver, sprite(texture));
            long bufferAllocations = stat(driver, "memory.bufferAllocations");
            long mapCalls = stat(driver, "memory.mapCalls");
            require(bufferAllocations >= 2L && mapCalls == bufferAllocations,
                    "frame/upload buffers were not persistently mapped by the allocator");
            for (int frame = 0; frame < 8; frame++) present(driver, sprite(texture));
            require(stat(driver, "memory.bufferAllocations") == bufferAllocations
                            && stat(driver, "memory.mapCalls") == mapCalls,
                    "warm frame remapped or reallocated persistent buffers");

            long renderTarget = driver.createRenderTarget(16, 16);
            require(stat(driver, "memory.imageAllocations") == 2L
                            && stat(driver, "memory.liveBytes") > afterImage,
                    "render-target image allocation was not accounted");
            driver.destroyTexture(renderTarget);
            driver.destroyTexture(texture);
            long fenceFlush = driver.uploadTexture(new VulkanTextureData(
                    1, 1, new byte[] {0, 0, 0, (byte) 255}));
            present(driver, sprite(fenceFlush));
            driver.updateTexture(fenceFlush, new VulkanTextureData(
                    1, 1, new byte[] {(byte) 255, 0, 0, (byte) 255}));
            present(driver, sprite(fenceFlush));
            require(stat(driver, "memory.imageFrees") >= 2L,
                    "retired image memory was not released after its fences");
            require(stat(driver, "memory.peakBytes") >= stat(driver, "memory.liveBytes"),
                    "allocator peak accounting is below live ownership");
            driver.destroyTexture(fenceFlush);
        }
        System.out.println("Native Vulkan memory allocator lifecycle passed");
    }

    private static VulkanFrameCommands sprite(long texture) {
        return VulkanFrameCommands.builder(32, 32).clear(0, 0, 0, 1)
                .texturedQuad(texture, 0, 0, 16, 16, 0, 0, 1, 1,
                        1, 1, 1, 1, VulkanDrawState.DEFAULT)
                .build();
    }

    private static void present(Lwjgl3VulkanDriver driver, VulkanFrameCommands frame) {
        if (driver.presentFrame(frame) == null) {
            throw new AssertionError("verification frame was not presented");
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
