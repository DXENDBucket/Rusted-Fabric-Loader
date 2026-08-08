package io.github.endx.rustedfabricapi.api.client.minimap;

/** Immutable layout and readiness snapshot of the local minimap. */
public final class MinimapSnapshot {
    private final float screenX;
    private final float screenY;
    private final float width;
    private final float height;
    private final boolean imageReady;
    private final boolean worldMappingReady;
    private final int markerCount;
    private final int scanPulseCount;

    MinimapSnapshot(float screenX, float screenY, float width, float height,
            boolean imageReady, boolean worldMappingReady, int markerCount, int scanPulseCount) {
        this.screenX = screenX;
        this.screenY = screenY;
        this.width = width;
        this.height = height;
        this.imageReady = imageReady;
        this.worldMappingReady = worldMappingReady;
        this.markerCount = markerCount;
        this.scanPulseCount = scanPulseCount;
    }

    public float getScreenX() { return screenX; }

    public float getScreenY() { return screenY; }

    public float getWidth() { return width; }

    public float getHeight() { return height; }

    public boolean isImageReady() { return imageReady; }

    public boolean isWorldMappingReady() { return worldMappingReady; }

    public int getMarkerCount() { return markerCount; }

    public int getScanPulseCount() { return scanPulseCount; }

    public boolean containsScreen(float x, float y) {
        return Float.isFinite(x) && Float.isFinite(y)
                && Float.isFinite(screenX) && Float.isFinite(screenY)
                && x >= screenX && y >= screenY && x <= screenX + width && y <= screenY + height;
    }
}
