package io.github.endx.rustedfabricapi.api.path;

import io.github.endx.rustedfabricapi.api.world.WorldPoint;

/** Immutable tile and world-center position from a solved native path. */
public final class PathStep {
    private final int tileX;
    private final int tileY;
    private final WorldPoint worldCenter;

    PathStep(int tileX, int tileY, WorldPoint worldCenter) {
        this.tileX = tileX;
        this.tileY = tileY;
        this.worldCenter = worldCenter;
    }

    public int tileX() { return tileX; }
    public int tileY() { return tileY; }
    public WorldPoint worldCenter() { return worldCenter; }

    @Override
    public String toString() {
        return "PathStep{" + tileX + ',' + tileY + '}';
    }
}
