package io.github.endx.rustedfabricapi.api.client;

import io.github.endx.rustedfabricapi.api.world.WorldPoint;

/** Immutable values captured from the current world camera. */
public final class CameraSnapshot {
    private final float left;
    private final float top;
    private final float width;
    private final float height;
    private final float zoom;
    private final float targetZoom;

    CameraSnapshot(float left, float top, float width, float height, float zoom, float targetZoom) {
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
        this.zoom = zoom;
        this.targetZoom = targetZoom;
    }

    public float left() {
        return left;
    }

    public float top() {
        return top;
    }

    public float width() {
        return width;
    }

    public float height() {
        return height;
    }

    public float right() {
        return left + width;
    }

    public float bottom() {
        return top + height;
    }

    public float zoom() {
        return zoom;
    }

    public float targetZoom() {
        return targetZoom;
    }

    public WorldPoint topLeft() {
        return new WorldPoint(left, top);
    }

    public WorldPoint center() {
        return new WorldPoint(left + width * 0.5F, top + height * 0.5F);
    }

    public boolean contains(float worldX, float worldY) {
        return Float.isFinite(worldX) && Float.isFinite(worldY)
                && worldX >= left && worldX <= right()
                && worldY >= top && worldY <= bottom();
    }

    @Override
    public String toString() {
        return "CameraSnapshot{" + left + ", " + top + ", " + width + "x" + height
                + ", zoom=" + zoom + '}';
    }
}
