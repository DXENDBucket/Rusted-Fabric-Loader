package io.github.endx.rustedfabricapi.api.ai;

import io.github.endx.rustedfabricapi.api.world.WorldPoint;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.OptionalInt;

/** Static terrain summary for one strategic grid cell. */
public final class AiTerrainCell {
    private final int column;
    private final int row;
    private final int minTileX;
    private final int minTileY;
    private final int maxTileXExclusive;
    private final int maxTileYExclusive;
    private final float left;
    private final float top;
    private final float right;
    private final float bottom;
    private final float waterFraction;
    private final float mountainFraction;
    private final float largeBlockerFraction;
    private final float lavaFraction;
    private final float buildingBlockedFraction;
    private final Map<AiMovementDomain, Float> passability;
    private final Map<AiMovementDomain, Integer> dominantRegions;
    private float landChokeScore;

    AiTerrainCell(int column, int row, int minTileX, int minTileY,
            int maxTileXExclusive, int maxTileYExclusive, int tileWidth, int tileHeight,
            float waterFraction, float mountainFraction, float lavaFraction,
            float largeBlockerFraction, float buildingBlockedFraction,
            Map<AiMovementDomain, Float> passability,
            Map<AiMovementDomain, Integer> dominantRegions) {
        this.column = column;
        this.row = row;
        this.minTileX = minTileX;
        this.minTileY = minTileY;
        this.maxTileXExclusive = maxTileXExclusive;
        this.maxTileYExclusive = maxTileYExclusive;
        this.left = minTileX * (float) tileWidth;
        this.top = minTileY * (float) tileHeight;
        this.right = maxTileXExclusive * (float) tileWidth;
        this.bottom = maxTileYExclusive * (float) tileHeight;
        this.waterFraction = waterFraction;
        this.mountainFraction = mountainFraction;
        this.largeBlockerFraction = largeBlockerFraction;
        this.lavaFraction = lavaFraction;
        this.buildingBlockedFraction = buildingBlockedFraction;
        this.passability = Collections.unmodifiableMap(
                new EnumMap<AiMovementDomain, Float>(passability));
        this.dominantRegions = Collections.unmodifiableMap(
                new EnumMap<AiMovementDomain, Integer>(dominantRegions));
    }

    public int column() { return column; }
    public int row() { return row; }
    public int minTileX() { return minTileX; }
    public int minTileY() { return minTileY; }
    public int maxTileXExclusive() { return maxTileXExclusive; }
    public int maxTileYExclusive() { return maxTileYExclusive; }
    public float left() { return left; }
    public float top() { return top; }
    public float right() { return right; }
    public float bottom() { return bottom; }
    public WorldPoint center() { return new WorldPoint((left + right) * 0.5F, (top + bottom) * 0.5F); }
    public float waterFraction() { return waterFraction; }
    /** Fraction of tiles explicitly marked as cliffs/mountain terrain. */
    public float mountainFraction() { return mountainFraction; }
    /** Fraction marked as large cliff/tree blockers, retained separately from mountain tiles. */
    public float largeBlockerFraction() { return largeBlockerFraction; }
    public float lavaFraction() { return lavaFraction; }
    public float buildingBlockedFraction() { return buildingBlockedFraction; }
    public float passableFraction(AiMovementDomain domain) {
        Float value = passability.get(domain);
        return value != null ? value.floatValue() : 0.0F;
    }
    public OptionalInt dominantRegion(AiMovementDomain domain) {
        Integer value = dominantRegions.get(domain);
        return value != null && value.intValue() > 0
                ? OptionalInt.of(value.intValue()) : OptionalInt.empty();
    }
    /** Approximate 0..1 score for land corridors constrained by terrain and neighbors. */
    public float landChokeScore() { return landChokeScore; }
    void setLandChokeScore(float value) { landChokeScore = value; }
    public boolean contains(float x, float y) {
        return x >= left && y >= top && x < right && y < bottom;
    }
}
