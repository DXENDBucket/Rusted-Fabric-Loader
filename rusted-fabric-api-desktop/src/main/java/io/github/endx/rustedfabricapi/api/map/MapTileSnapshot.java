package io.github.endx.rustedfabricapi.api.map;

import java.util.Objects;
import java.util.Optional;

import io.github.endx.rustedfabricapi.api.world.WorldPoint;

/** Immutable tile value from one TMX layer and coordinate. */
public final class MapTileSnapshot {
    private final int layerIndex;
    private final String layerName;
    private final int tileX;
    private final int tileY;
    private final int tileWidth;
    private final int tileHeight;
    private final String tilesetName;
    private final String tilesetImageSource;
    private final int globalTileId;
    private final int localTileIndex;
    private final int atlasIndex;
    private final int registeredTileId;
    private final boolean water;
    private final boolean waterBridge;
    private final boolean lava;
    private final boolean cliff;
    private final boolean resourcePool;
    private final int pathingCost;
    private final boolean largeCliffOrTreeBlocker;
    private final boolean blocksBuildings;
    private final int variantCount;
    private final MapProperties properties;

    public MapTileSnapshot(int layerIndex, String layerName, int tileX, int tileY,
            int tileWidth, int tileHeight, String tilesetName, String tilesetImageSource,
            int globalTileId, int localTileIndex, int atlasIndex, int registeredTileId,
            boolean water, boolean waterBridge, boolean lava, boolean cliff, boolean resourcePool,
            int pathingCost, boolean largeCliffOrTreeBlocker, boolean blocksBuildings,
            int variantCount, MapProperties properties) {
        if (layerIndex < 0 || tileX < 0 || tileY < 0 || tileWidth <= 0 || tileHeight <= 0
                || globalTileId < 0 || localTileIndex < 0 || registeredTileId < 0
                || registeredTileId > 0xffff || variantCount < 0) {
            throw new IllegalArgumentException("invalid tile identity or dimensions");
        }
        this.layerIndex = layerIndex;
        this.layerName = layerName != null ? layerName : "";
        this.tileX = tileX;
        this.tileY = tileY;
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
        this.tilesetName = tilesetName != null ? tilesetName : "";
        this.tilesetImageSource = tilesetImageSource != null ? tilesetImageSource : "";
        this.globalTileId = globalTileId;
        this.localTileIndex = localTileIndex;
        this.atlasIndex = atlasIndex;
        this.registeredTileId = registeredTileId;
        this.water = water;
        this.waterBridge = waterBridge;
        this.lava = lava;
        this.cliff = cliff;
        this.resourcePool = resourcePool;
        this.pathingCost = pathingCost;
        this.largeCliffOrTreeBlocker = largeCliffOrTreeBlocker;
        this.blocksBuildings = blocksBuildings;
        this.variantCount = variantCount;
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    public int layerIndex() { return layerIndex; }
    public String layerName() { return layerName; }
    public int tileX() { return tileX; }
    public int tileY() { return tileY; }
    public int tileWidth() { return tileWidth; }
    public int tileHeight() { return tileHeight; }
    public float worldX() { return (float) tileX * tileWidth; }
    public float worldY() { return (float) tileY * tileHeight; }
    public WorldPoint center() {
        return new WorldPoint(worldX() + tileWidth * 0.5F, worldY() + tileHeight * 0.5F);
    }
    public Optional<String> tilesetName() { return optional(tilesetName); }
    public Optional<String> tilesetImageSource() { return optional(tilesetImageSource); }
    public int globalTileId() { return globalTileId; }
    public int localTileIndex() { return localTileIndex; }
    public int atlasIndex() { return atlasIndex; }
    public int registeredTileId() { return registeredTileId; }
    public boolean water() { return water; }
    public boolean waterBridge() { return waterBridge; }
    public boolean lava() { return lava; }
    public boolean cliff() { return cliff; }
    public boolean resourcePool() { return resourcePool; }
    public int pathingCost() { return pathingCost; }
    public boolean largeCliffOrTreeBlocker() { return largeCliffOrTreeBlocker; }
    public boolean blocksBuildings() { return blocksBuildings; }
    public int variantCount() { return variantCount; }
    public MapProperties properties() { return properties; }

    private static Optional<String> optional(String value) {
        return value.isEmpty() ? Optional.empty() : Optional.of(value);
    }

    @Override
    public String toString() {
        return "MapTileSnapshot{" + layerIndex + ':' + tileX + ',' + tileY
                + ", gid=" + globalTileId + ", tileset='" + tilesetName + "'}";
    }
}
