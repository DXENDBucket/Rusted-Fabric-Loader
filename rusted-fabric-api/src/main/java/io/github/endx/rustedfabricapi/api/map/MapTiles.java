package io.github.endx.rustedfabricapi.api.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import rustedwarfare.map.MapEngine;
import rustedwarfare.map.MapLayer;
import rustedwarfare.map.MapTile;
import rustedwarfare.map.Tileset;

/** On-demand immutable access to loaded TMX tile layers. */
public final class MapTiles {
    private MapTiles() {
    }

    public static List<MapLayerSnapshot> layers() {
        MapEngine map = Maps.requireCurrent();
        ArrayList<MapLayerSnapshot> result = new ArrayList<MapLayerSnapshot>();
        if (map.layers != null) {
            for (Object raw : map.layers) {
                if (raw instanceof MapLayer) result.add(captureLayer((MapLayer) raw));
            }
        }
        return Collections.unmodifiableList(result);
    }

    public static Optional<MapLayerSnapshot> layer(String name) {
        return findLayer(Maps.requireCurrent(), Objects.requireNonNull(name, "name"))
                .map(MapTiles::captureLayer);
    }

    public static Optional<MapLayerSnapshot> layer(int index) {
        MapLayer layer = findLayer(Maps.requireCurrent(), index);
        return layer != null ? Optional.of(captureLayer(layer)) : Optional.empty();
    }

    public static Optional<MapTileSnapshot> groundAt(int tileX, int tileY) {
        MapEngine map = Maps.requireCurrent();
        return captureAt(map, map.groundLayer, tileX, tileY);
    }

    public static Optional<MapTileSnapshot> groundAtWorld(float worldX, float worldY) {
        if (!Float.isFinite(worldX) || !Float.isFinite(worldY)) {
            throw new IllegalArgumentException("world coordinates must be finite");
        }
        MapEngine map = Maps.requireCurrent();
        return groundAt((int) Math.floor(map.worldToTileX(worldX)),
                (int) Math.floor(map.worldToTileY(worldY)));
    }

    public static Optional<MapTileSnapshot> tileAt(String layerName, int tileX, int tileY) {
        MapEngine map = Maps.requireCurrent();
        return findLayer(map, Objects.requireNonNull(layerName, "layerName"))
                .flatMap(layer -> captureAt(map, layer, tileX, tileY));
    }

    public static Optional<MapTileSnapshot> tileAt(int layerIndex, int tileX, int tileY) {
        MapEngine map = Maps.requireCurrent();
        return captureAt(map, findLayer(map, layerIndex), tileX, tileY);
    }

    /** Returns every non-empty layer value at a tile in native draw order. */
    public static List<MapTileSnapshot> stackAt(int tileX, int tileY) {
        MapEngine map = Maps.requireCurrent();
        ArrayList<MapTileSnapshot> result = new ArrayList<MapTileSnapshot>();
        if (map.layers != null) {
            for (Object raw : map.layers) {
                if (!(raw instanceof MapLayer)) continue;
                captureAt(map, (MapLayer) raw, tileX, tileY).ifPresent(result::add);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /** Captures non-empty tiles in row-major order from a rectangular part of one layer. */
    public static List<MapTileSnapshot> region(int layerIndex, int tileX, int tileY,
            int width, int height) {
        if (tileX < 0 || tileY < 0 || width < 0 || height < 0) {
            throw new IllegalArgumentException("region coordinates and size must be non-negative");
        }
        MapEngine map = Maps.requireCurrent();
        MapLayer layer = findLayer(map, layerIndex);
        if (layer == null) return Collections.emptyList();
        long right = (long) tileX + width;
        long bottom = (long) tileY + height;
        if (right > layer.width || bottom > layer.height) {
            throw new IllegalArgumentException("region exceeds the selected layer");
        }
        ArrayList<MapTileSnapshot> result = new ArrayList<MapTileSnapshot>();
        for (int y = tileY; y < bottom; y++) {
            for (int x = tileX; x < right; x++) {
                captureAt(map, layer, x, y).ifPresent(result::add);
            }
        }
        return Collections.unmodifiableList(result);
    }

    private static Optional<MapTileSnapshot> captureAt(MapEngine map, MapLayer layer,
            int tileX, int tileY) {
        if (layer == null || tileX < 0 || tileY < 0
                || tileX >= layer.width || tileY >= layer.height) return Optional.empty();
        MapTile tile = layer.getTile(tileX, tileY);
        return tile != null ? Optional.of(captureTile(map, layer, tile, tileX, tileY))
                : Optional.empty();
    }

    private static MapLayerSnapshot captureLayer(MapLayer layer) {
        return new MapLayerSnapshot(layer.layerIndex, layer.name, layer.width, layer.height,
                layer.visible, layer.isGroundLayer, layer.isItemsLayer, layer.hasNonAtlasTiles,
                MapObjects.copyProperties(layer.properties));
    }

    private static MapTileSnapshot captureTile(MapEngine map, MapLayer layer, MapTile tile,
            int tileX, int tileY) {
        Tileset tileset = tile.tileset;
        int gid = tileset != null ? tileset.firstGid + tile.localTileIndex : tile.localTileIndex;
        int variantCount = tile.variants != null ? tile.variants.length : 0;
        return new MapTileSnapshot(layer.layerIndex, layer.name, tileX, tileY,
                map.tileWidth, map.tileHeight,
                tileset != null ? tileset.name : null,
                tileset != null ? tileset.imageSource : null,
                gid, tile.localTileIndex, tile.atlasIndex,
                Short.toUnsignedInt(tile.registeredTileId), tile.isWater, tile.isWaterBridge,
                tile.isLava, tile.isCliff, tile.isResourcePool, tile.pathingCost,
                tile.isLargeCliffOrTreeBlocker, tile.blocksBuildings, variantCount,
                tileset != null ? MapObjects.copyProperties(
                        tileset.getTileProperties(tile.localTileIndex)) : MapProperties.empty());
    }

    private static Optional<MapLayer> findLayer(MapEngine map, String name) {
        if (map.layers == null) return Optional.empty();
        for (Object raw : map.layers) {
            if (raw instanceof MapLayer) {
                MapLayer layer = (MapLayer) raw;
                if (layer.name != null && layer.name.equalsIgnoreCase(name)) {
                    return Optional.of(layer);
                }
            }
        }
        return Optional.empty();
    }

    private static MapLayer findLayer(MapEngine map, int index) {
        if (index < 0 || map.layers == null) return null;
        for (Object raw : map.layers) {
            if (raw instanceof MapLayer && ((MapLayer) raw).layerIndex == index) {
                return (MapLayer) raw;
            }
        }
        return null;
    }
}
