package io.github.endx.vulkanmod;

/** Infers Slick readback pixel formats from logical and padded texture dimensions. */
final class TextureReadbackLayout {
    private TextureReadbackLayout() { }

    static int inferBytesPerPixel(int byteCount, int width, int height) {
        if (byteCount <= 0 || width <= 0 || height <= 0) return 0;
        int logicalPixels = Math.multiplyExact(width, height);
        if (byteCount == logicalPixels * 4) return 4;
        if (byteCount == logicalPixels * 3) return 3;

        // Slick commonly backs a logical 250x250 FBO with a 256x256 texture. ImageData uses
        // that backing width as its row stride, so the readback buffer includes the padded
        // rows and columns. Account for Slick's power-of-two allocation before giving up.
        int textureWidth = nextPowerOfTwo(width);
        int textureHeight = nextPowerOfTwo(height);
        int texturePixels = Math.multiplyExact(textureWidth, textureHeight);
        if (byteCount == texturePixels * 4) return 4;
        if (byteCount == texturePixels * 3) return 3;
        return 0;
    }

    private static int nextPowerOfTwo(int value) {
        if (value <= 1) return 1;
        int highest = Integer.highestOneBit(value - 1);
        if (highest > (1 << 29)) throw new ArithmeticException("image dimension overflow");
        return highest << 1;
    }
}
