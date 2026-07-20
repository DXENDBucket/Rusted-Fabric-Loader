package io.github.endx.rustedfabricapi.api.client.render;

import io.github.endx.rustedfabricapi.api.world.WorldPoint;

/** Immutable camera values used for one world-render callback. */
public final class WorldViewport {
    private final float left;
    private final float top;
    private final float width;
    private final float height;
    private final float zoom;

    public WorldViewport(float left, float top, float width, float height, float zoom) {
        requireFinite(left, "left");
        requireFinite(top, "top");
        requirePositive(width, "width");
        requirePositive(height, "height");
        requirePositive(zoom, "zoom");
        if (!Float.isFinite(width * zoom) || !Float.isFinite(height * zoom)) {
            throw new IllegalArgumentException("viewport pixel dimensions must be finite");
        }
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
        this.zoom = zoom;
    }

    public float left() { return left; }
    public float top() { return top; }
    public float width() { return width; }
    public float height() { return height; }
    public float right() { return left + width; }
    public float bottom() { return top + height; }
    public float zoom() { return zoom; }
    public float screenWidth() { return width * zoom; }
    public float screenHeight() { return height * zoom; }
    public WorldPoint topLeft() { return new WorldPoint(left, top); }
    public WorldPoint center() { return new WorldPoint(left + width * 0.5F, top + height * 0.5F); }

    public ScreenPosition worldToScreen(float worldX, float worldY) {
        requireFinite(worldX, "worldX");
        requireFinite(worldY, "worldY");
        return new ScreenPosition((worldX - left) * zoom, (worldY - top) * zoom);
    }

    public ScreenPosition worldToScreen(WorldPoint point) {
        if (point == null) throw new NullPointerException("point");
        return worldToScreen(point.x(), point.y());
    }

    public WorldPoint screenToWorld(float screenX, float screenY) {
        requireFinite(screenX, "screenX");
        requireFinite(screenY, "screenY");
        return new WorldPoint(left + screenX / zoom, top + screenY / zoom);
    }

    public float worldLengthToPixels(float worldLength) {
        requireNonNegative(worldLength, "worldLength");
        return worldLength * zoom;
    }

    public float pixelsToWorldLength(float pixels) {
        requireNonNegative(pixels, "pixels");
        return pixels / zoom;
    }

    public boolean contains(float worldX, float worldY) {
        return Float.isFinite(worldX) && Float.isFinite(worldY)
                && worldX >= left && worldX <= right()
                && worldY >= top && worldY <= bottom();
    }

    /** Returns whether a world-space circle intersects this viewport. */
    public boolean isVisible(float worldX, float worldY, float radius) {
        requireFinite(worldX, "worldX");
        requireFinite(worldY, "worldY");
        requireNonNegative(radius, "radius");
        return worldX + radius >= left && worldX - radius <= right()
                && worldY + radius >= top && worldY - radius <= bottom();
    }

    private static void requirePositive(float value, String name) {
        requireFinite(value, name);
        if (!(value > 0.0F)) throw new IllegalArgumentException(name + " must be positive");
    }

    private static void requireNonNegative(float value, String name) {
        requireFinite(value, name);
        if (value < 0.0F) throw new IllegalArgumentException(name + " must be non-negative");
    }

    private static void requireFinite(float value, String name) {
        if (!Float.isFinite(value)) throw new IllegalArgumentException(name + " must be finite");
    }

    @Override
    public String toString() {
        return "WorldViewport{" + left + ", " + top + ", " + width + "x" + height
                + ", zoom=" + zoom + '}';
    }
}
