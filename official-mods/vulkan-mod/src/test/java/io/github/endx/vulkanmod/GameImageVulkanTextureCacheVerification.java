package io.github.endx.vulkanmod;

public final class GameImageVulkanTextureCacheVerification {
    private GameImageVulkanTextureCacheVerification() { }

    public static void main(String[] args) {
        expect(4, 1024 * 1024 * 4, 1024, 1024);
        expect(4, 256 * 256 * 4, 250, 250);
        expect(3, 256 * 256 * 3, 250, 250);
        expect(0, 17, 250, 250);
        System.out.println("Vulkan texture readback inference contract passed");
    }

    private static void expect(int expected, int bytes, int width, int height) {
        int actual = TextureReadbackLayout.inferBytesPerPixel(bytes, width, height);
        if (actual != expected) {
            throw new AssertionError("expected " + expected + " but got " + actual
                    + " for " + width + "x" + height + " / " + bytes + " bytes");
        }
    }
}
