package io.github.endx.rustedfabricapi.api.client.render;

import java.util.Objects;

import io.github.endx.rustedfabricapi.api.geometry.GeometryMask;
import rustedwarfare.client.render.GameImage;

/** Factories for image and geometry alpha-mask samplers. */
public final class AlphaMasks {
    private AlphaMasks() { }

    public static AlphaMask geometry(GeometryMask mask) {
        GeometryMask checked = Objects.requireNonNull(mask, "mask");
        return (x, y) -> checked.contains(x, y) ? 1.0F : 0.0F;
    }

    public static AlphaMask image(ClientImage image) {
        ClientImage checked = Objects.requireNonNull(image, "image");
        return image(checked, 0, 0, checked.width(), checked.height(),
                0.0F, 0.0F, 1.0F, 1.0F, 0.0F, 1.0F);
    }

    /**
     * Creates a bilinear image sampler centered at the supplied local point. Scale and rotation
     * transform mask pixels into caller coordinates; samples beyond the selected region are clear.
     */
    public static AlphaMask image(ClientImage image, int sourceX, int sourceY,
            int sourceWidth, int sourceHeight, float centerX, float centerY,
            float scaleX, float scaleY, float rotationDegrees, float alphaMultiplier) {
        requireFinite(scaleX, "scaleX");
        requireFinite(scaleY, "scaleY");
        requireFinite(rotationDegrees, "rotationDegrees");
        if (scaleX == 0.0F || scaleY == 0.0F) {
            throw new IllegalArgumentException("mask image scale must be non-zero");
        }
        final double radians = Math.toRadians(rotationDegrees);
        final float cosine = (float) Math.cos(radians);
        final float sine = (float) Math.sin(radians);
        return imageAffine(image, sourceX, sourceY, sourceWidth, sourceHeight,
                centerX, centerY,
                cosine * scaleX, sine * scaleX,
                -sine * scaleY, cosine * scaleY,
                alphaMultiplier);
    }

    /**
     * Creates an image sampler with arbitrary caller-coordinate axes for one source-pixel step.
     * This supports relative non-uniform scale and rotation without losing affine accuracy.
     */
    public static AlphaMask imageAffine(ClientImage image, int sourceX, int sourceY,
            int sourceWidth, int sourceHeight, float centerX, float centerY,
            float xAxisX, float xAxisY, float yAxisX, float yAxisY,
            float alphaMultiplier) {
        ClientImage checked = Objects.requireNonNull(image, "image");
        requireRegion(checked, sourceX, sourceY, sourceWidth, sourceHeight);
        requireFinite(centerX, "centerX");
        requireFinite(centerY, "centerY");
        requireFinite(xAxisX, "xAxisX");
        requireFinite(xAxisY, "xAxisY");
        requireFinite(yAxisX, "yAxisX");
        requireFinite(yAxisY, "yAxisY");
        requireFinite(alphaMultiplier, "alphaMultiplier");
        float determinant = xAxisX * yAxisY - yAxisX * xAxisY;
        if (!Float.isFinite(determinant) || Math.abs(determinant) < 0.000001F) {
            throw new IllegalArgumentException("mask image transform must be invertible");
        }
        GameImage nativeImage = checked.requireOpen();
        nativeImage.ensureImageDataAvailable();
        float[] alpha = new float[sourceWidth * sourceHeight];
        for (int y = 0; y < sourceHeight; y++) {
            for (int x = 0; x < sourceWidth; x++) {
                int color = nativeImage.getPixel(sourceX + x, sourceY + y);
                alpha[y * sourceWidth + x] = ((color >>> 24) & 0xff) / 255.0F;
            }
        }
        final float inverse = 1.0F / determinant;
        return (x, y) -> {
            float dx = x - centerX;
            float dy = y - centerY;
            float localX = (yAxisY * dx - yAxisX * dy) * inverse
                    + sourceWidth * 0.5F - 0.5F;
            float localY = (-xAxisY * dx + xAxisX * dy) * inverse
                    + sourceHeight * 0.5F - 0.5F;
            return bilinear(alpha, sourceWidth, sourceHeight, localX, localY) * alphaMultiplier;
        };
    }

    public static AlphaMask multiply(AlphaMask first, AlphaMask second) {
        AlphaMask checkedFirst = Objects.requireNonNull(first, "first");
        AlphaMask checkedSecond = Objects.requireNonNull(second, "second");
        return (x, y) -> checkedFirst.alphaAt(x, y) * checkedSecond.alphaAt(x, y);
    }

    private static float bilinear(float[] values, int width, int height, float x, float y) {
        if (x < -0.5F || y < -0.5F || x > width - 0.5F || y > height - 0.5F) return 0.0F;
        float checkedX = Math.max(0.0F, Math.min(width - 1.0F, x));
        float checkedY = Math.max(0.0F, Math.min(height - 1.0F, y));
        int x0 = (int) Math.floor(checkedX);
        int y0 = (int) Math.floor(checkedY);
        int x1 = Math.min(width - 1, x0 + 1);
        int y1 = Math.min(height - 1, y0 + 1);
        float tx = checkedX - x0;
        float ty = checkedY - y0;
        float top = values[y0 * width + x0] * (1.0F - tx) + values[y0 * width + x1] * tx;
        float bottom = values[y1 * width + x0] * (1.0F - tx) + values[y1 * width + x1] * tx;
        return top * (1.0F - ty) + bottom * ty;
    }

    private static void requireRegion(ClientImage image, int x, int y, int width, int height) {
        if (x < 0 || y < 0 || width <= 0 || height <= 0
                || (long) x + width > image.width() || (long) y + height > image.height()) {
            throw new IllegalArgumentException("mask source rectangle is outside the image");
        }
    }

    private static void requireFinite(float value, String name) {
        if (!Float.isFinite(value)) throw new IllegalArgumentException(name + " must be finite");
    }
}
