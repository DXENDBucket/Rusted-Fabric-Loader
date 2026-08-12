package io.github.endx.vulkanmod.lwjgl3;

import io.github.endx.vulkanmod.spi.VulkanFrameCommands;
import io.github.endx.vulkanmod.spi.VulkanTextureData;
import io.github.endx.vulkanmod.spi.VulkanWindowRequest;
import io.github.endx.vulkanmod.spi.VulkanDrawState;
import io.github.endx.vulkanmod.spi.VulkanTransform2D;

/** Real-device smoke test for native render-target readback and channel normalization. */
public final class StandaloneVulkanReadbackVerification {
    private StandaloneVulkanReadbackVerification() { }

    public static void main(String[] arguments) {
        try (Lwjgl3VulkanDriver driver = new Lwjgl3VulkanDriver()) {
            driver.createNativeWindowSurface(new VulkanWindowRequest(
                    "RustedVK readback verification", 64, 64, false));
            long target = driver.createRenderTarget(4, 4);
            long white = driver.uploadTexture(new VulkanTextureData(1, 1,
                    new byte[] {(byte) 255, (byte) 255, (byte) 255, (byte) 255}));
            try {
                driver.renderToTexture(target, VulkanFrameCommands.builder(4, 4)
                        .clear(1.0f, 0.25f, 0.5f, 1.0f)
                        .build());
                VulkanTextureData snapshot = driver.readTexture(target);
                byte[] rgba = snapshot.copyRgba();
                for (int pixel = 0; pixel < 16; pixel++) {
                    int offset = pixel * 4;
                    expect("red", 255, rgba[offset] & 255);
                    expect("green", 64, rgba[offset + 1] & 255);
                    expect("blue", 128, rgba[offset + 2] & 255);
                    expect("alpha", 255, rgba[offset + 3] & 255);
                }
                VulkanDrawState translated = VulkanDrawState.transformed(
                        VulkanTransform2D.translation(2.0f, 0.0f));
                VulkanFrameCommands spriteRun = VulkanFrameCommands.pooledBuilder(4, 4)
                        .clear(0.0f, 0.0f, 0.0f, 1.0f)
                        .texturedQuad(white, 0.0f, 0.0f, 2.0f, 4.0f,
                                0.0f, 0.0f, 1.0f, 1.0f,
                                1.0f, 0.0f, 0.0f, 1.0f, VulkanDrawState.DEFAULT)
                        .texturedQuad(white, 0.0f, 0.0f, 2.0f, 4.0f,
                                0.0f, 0.0f, 1.0f, 1.0f,
                                0.0f, 1.0f, 0.0f, 1.0f, translated)
                        .build();
                if (spriteRun.commandCount() != 1 || spriteRun.texturedQuadRunCount() != 1
                        || spriteRun.texturedQuadRunQuadCount() != 2) {
                    throw new AssertionError("sprite run was not compacted before object submit");
                }
                try {
                    driver.renderToTexture(target, spriteRun);
                } finally {
                    spriteRun.releasePooledCommands();
                }
                byte[] batched = driver.readTexture(target).copyRgba();
                for (int y = 0; y < 4; y++) {
                    for (int x = 0; x < 4; x++) {
                        int offset = (y * 4 + x) * 4;
                        expect("sprite run red", x < 2 ? 255 : 0,
                                batched[offset] & 255);
                        expect("sprite run green", x < 2 ? 0 : 255,
                                batched[offset + 1] & 255);
                        expect("sprite run blue", 0, batched[offset + 2] & 255);
                        expect("sprite run alpha", 255, batched[offset + 3] & 255);
                    }
                }
                byte[] replacement = new byte[4 * 4 * 4];
                for (int pixel = 0; pixel < 16; pixel++) {
                    int offset = pixel * 4;
                    replacement[offset] = 17;
                    replacement[offset + 1] = 34;
                    replacement[offset + 2] = 51;
                    replacement[offset + 3] = 68;
                }
                driver.updateTexture(target, new VulkanTextureData(4, 4, replacement));
                // An empty load pass flushes the queued transfer without changing its pixels.
                driver.renderToTexture(target, VulkanFrameCommands.builder(4, 4).build());
                byte[] uploaded = driver.readTexture(target).copyRgba();
                for (int pixel = 0; pixel < 16; pixel++) {
                    int offset = pixel * 4;
                    expect("uploaded red", 17, uploaded[offset] & 255);
                    expect("uploaded green", 34, uploaded[offset + 1] & 255);
                    expect("uploaded blue", 51, uploaded[offset + 2] & 255);
                    expect("uploaded alpha", 68, uploaded[offset + 3] & 255);
                }
            } finally {
                driver.destroyTexture(white);
                driver.destroyTexture(target);
            }
        }
        System.out.println("Native Vulkan render-target readback contract passed");
    }

    private static void expect(String channel, int expected, int actual) {
        if (Math.abs(expected - actual) > 1) {
            throw new AssertionError(channel + " expected " + expected + " but got " + actual);
        }
    }
}
