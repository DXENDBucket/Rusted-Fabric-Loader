package io.github.endx.rustedfabricapi.api.client.render;

/** Allocation-free ARGB color packing and component helpers. */
public final class ArgbColor {
    public static final int TRANSPARENT = 0x00000000;
    public static final int BLACK = 0xff000000;
    public static final int WHITE = 0xffffffff;
    public static final int RED = 0xffff0000;
    public static final int GREEN = 0xff00ff00;
    public static final int BLUE = 0xff0000ff;

    private ArgbColor() {
    }

    public static int argb(int alpha, int red, int green, int blue) {
        requireComponent(alpha, "alpha");
        requireComponent(red, "red");
        requireComponent(green, "green");
        requireComponent(blue, "blue");
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    public static int rgb(int red, int green, int blue) {
        return argb(255, red, green, blue);
    }

    public static int alpha(int color) { return color >>> 24; }
    public static int red(int color) { return color >>> 16 & 0xff; }
    public static int green(int color) { return color >>> 8 & 0xff; }
    public static int blue(int color) { return color & 0xff; }

    public static int withAlpha(int color, int alpha) {
        requireComponent(alpha, "alpha");
        return color & 0x00ffffff | alpha << 24;
    }

    public static int multiplyAlpha(int color, float multiplier) {
        if (!Float.isFinite(multiplier) || multiplier < 0.0F || multiplier > 1.0F) {
            throw new IllegalArgumentException("multiplier must be finite from 0.0 to 1.0");
        }
        return withAlpha(color, Math.round(alpha(color) * multiplier));
    }

    private static void requireComponent(int component, String name) {
        if (component < 0 || component > 255) {
            throw new IllegalArgumentException(name + " must be between 0 and 255");
        }
    }
}
