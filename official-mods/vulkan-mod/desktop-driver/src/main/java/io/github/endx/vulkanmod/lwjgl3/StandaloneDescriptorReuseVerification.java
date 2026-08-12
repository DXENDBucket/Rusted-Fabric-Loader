package io.github.endx.vulkanmod.lwjgl3;

import io.github.endx.vulkanmod.spi.VulkanBlendMode;
import io.github.endx.vulkanmod.spi.VulkanDrawState;
import io.github.endx.vulkanmod.spi.VulkanFrameCommands;
import io.github.endx.vulkanmod.spi.VulkanShaderState;
import io.github.endx.vulkanmod.spi.VulkanTextureData;
import io.github.endx.vulkanmod.spi.VulkanTextureFilter;
import io.github.endx.vulkanmod.spi.VulkanTransform2D;
import io.github.endx.vulkanmod.spi.VulkanWindowRequest;

import java.util.Map;

/** Exercises lazy descriptor creation, redundant-bind removal, and fence-safe set recycling. */
public final class StandaloneDescriptorReuseVerification {
    private StandaloneDescriptorReuseVerification() { }

    public static void main(String[] arguments) {
        try (Lwjgl3VulkanDriver driver = new Lwjgl3VulkanDriver()) {
            driver.createNativeWindowSurface(new VulkanWindowRequest(
                    "RustedVK descriptor reuse verification", 16, 16, false));
            long firstTexture = uploadWhitePixel(driver);
            require(stat(driver, "descriptor.allocations") == 0L,
                    "upload eagerly allocated a descriptor");

            present(driver, singleSprite(firstTexture));
            require(stat(driver, "descriptor.allocations") == 1L
                            && stat(driver, "descriptor.singleMisses") == 1L,
                    "first sampled texture did not lazily allocate one descriptor");

            long skippedBefore = stat(driver, "descriptor.bindSkips");
            present(driver, twoMaterialsOneTexture(firstTexture));
            require(stat(driver, "descriptor.bindSkips") == skippedBefore + 1L,
                    "compatible pipelines rebound the same descriptor set");
            require(stat(driver, "descriptor.allocations") == 1L,
                    "shader-only material split allocated another descriptor");

            driver.destroyTexture(firstTexture);
            for (int attempt = 0; attempt < 12
                    && stat(driver, "descriptor.recycled") == 0L; attempt++) {
                present(driver, VulkanFrameCommands.builder(16, 16)
                        .clear(0, 0, 0, 1).build());
            }
            require(stat(driver, "descriptor.recycled") == 1L,
                    "retired descriptor did not become recyclable after its fences");

            long replacement = uploadWhitePixel(driver);
            try {
                present(driver, singleSprite(replacement));
                require(stat(driver, "descriptor.allocations") == 1L
                                && stat(driver, "descriptor.reuses") == 1L
                                && stat(driver, "descriptor.recycled") == 0L,
                        "replacement texture did not reuse the retired descriptor");
                long secondary = uploadWhitePixel(driver);
                try {
                    present(driver, pairedSprite(replacement, secondary));
                    present(driver, pairedSprite(replacement, secondary));
                    require(stat(driver, "descriptor.pairMisses") == 1L
                                    && stat(driver, "descriptor.pairHits") >= 1L,
                            "dual-texture descriptor was not cached");
                } finally {
                    driver.destroyTexture(secondary);
                }
            } finally {
                driver.destroyTexture(replacement);
            }
        }
        System.out.println("Native Vulkan descriptor lifecycle and bind reuse passed");
    }

    private static long uploadWhitePixel(Lwjgl3VulkanDriver driver) {
        return driver.uploadTexture(new VulkanTextureData(1, 1,
                new byte[] {(byte) 255, (byte) 255, (byte) 255, (byte) 255}));
    }

    private static VulkanFrameCommands singleSprite(long texture) {
        return VulkanFrameCommands.builder(16, 16).clear(0, 0, 0, 1)
                .texturedQuad(texture, 0, 0, 8, 8, 0, 0, 1, 1,
                        1, 1, 1, 1, VulkanDrawState.DEFAULT)
                .build();
    }

    private static VulkanFrameCommands twoMaterialsOneTexture(long texture) {
        VulkanShaderState teamShader = new VulkanShaderState(
                VulkanShaderState.PURE_GREEN_TEAM_COLOR,
                1, 0, 0, 1, 0.5f);
        VulkanDrawState teamState = new VulkanDrawState(
                VulkanTransform2D.IDENTITY, null, VulkanBlendMode.NORMAL,
                VulkanTextureFilter.LINEAR, teamShader);
        return VulkanFrameCommands.builder(16, 16).clear(0, 0, 0, 1)
                .texturedQuad(texture, 0, 0, 8, 8, 0, 0, 1, 1,
                        1, 1, 1, 1, VulkanDrawState.DEFAULT)
                .texturedQuad(texture, 8, 0, 8, 8, 0, 0, 1, 1,
                        1, 1, 1, 1, teamState)
                .build();
    }

    private static VulkanFrameCommands pairedSprite(long primary, long secondary) {
        VulkanShaderState displacement = new VulkanShaderState(
                VulkanShaderState.POST_DISPLACEMENT,
                1, 1, 1, 1, 0, secondary,
                16, 16, 16, 16, 0.01f, 1);
        VulkanDrawState state = new VulkanDrawState(
                VulkanTransform2D.IDENTITY, null, VulkanBlendMode.NORMAL,
                VulkanTextureFilter.LINEAR, displacement);
        return VulkanFrameCommands.builder(16, 16).clear(0, 0, 0, 1)
                .texturedQuad(primary, 0, 0, 16, 16, 0, 0, 1, 1,
                        1, 1, 1, 1, state)
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
