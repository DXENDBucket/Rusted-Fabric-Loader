package io.github.endx.rustedfabricapi.api.ai;

import io.github.endx.rustedfabricapi.api.world.WorldPoint;

/** One resource-pool tile in the loaded map. */
public final class AiResourceSite {
    private final int tileX;
    private final int tileY;
    private final WorldPoint center;

    AiResourceSite(int tileX, int tileY, WorldPoint center) {
        this.tileX = tileX;
        this.tileY = tileY;
        this.center = center;
    }

    public int tileX() { return tileX; }
    public int tileY() { return tileY; }
    public WorldPoint center() { return center; }
}
