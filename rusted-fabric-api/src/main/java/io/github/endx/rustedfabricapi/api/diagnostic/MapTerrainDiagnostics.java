package io.github.endx.rustedfabricapi.api.diagnostic;

import io.github.endx.rustedfabricapi.api.util.RustedReflection;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MapTerrainDiagnostics {
    private static final String[] MAP_ENGINE_CLASSES = {
            "rustedwarfare.map.MapEngine",
            "com.corrodinggames.rts.game.b.b"
    };
    private static final String[] MAP_LAYER_CLASSES = {
            "rustedwarfare.map.MapLayer",
            "com.corrodinggames.rts.game.b.e"
    };
    private static final String[] MAP_TILE_CLASSES = {
            "rustedwarfare.map.MapTile",
            "com.corrodinggames.rts.game.b.g"
    };
    private static final String[] TILESET_CLASSES = {
            "rustedwarfare.map.Tileset",
            "com.corrodinggames.rts.game.b.j"
    };
    private static final String[] TILESET_IMAGE_CACHE_ENTRY_CLASSES = {
            "rustedwarfare.map.TilesetImageCacheEntry",
            "com.corrodinggames.rts.game.b.k"
    };
    private static final String[] TILE_ATLAS_CLASSES = {
            "rustedwarfare.map.TileAtlas",
            "com.corrodinggames.rts.game.b.h"
    };
    private static final String[] MAP_TILE_RENDER_CACHE_CLASSES = {
            "rustedwarfare.map.MapTileRenderCache",
            "com.corrodinggames.rts.game.b.c"
    };
    private static final String[] MAP_TILE_RENDER_CACHE_REGION_CLASSES = {
            "rustedwarfare.map.MapTileRenderCacheRegion",
            "com.corrodinggames.rts.game.b.d"
    };

    private MapTerrainDiagnostics() {
    }

    public static Map<String, Object> describeMapEngine(Object map) {
        requireMapEngine(map);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putIntField(result, map, "width", new String[]{"width", "C"});
        putIntField(result, map, "height", new String[]{"height", "D"});
        putIntField(result, map, "tileWidth", new String[]{"tileWidth", "n"});
        putIntField(result, map, "tileHeight", new String[]{"tileHeight", "o"});
        putIntField(result, map, "halfTileWidth", new String[]{"halfTileWidth", "p"});
        putIntField(result, map, "halfTileHeight", new String[]{"halfTileHeight", "q"});
        putFloatField(result, map, "invTileWidth", new String[]{"invTileWidth", "r"});
        putFloatField(result, map, "invTileHeight", new String[]{"invTileHeight", "s"});
        putBooleanField(result, map, "mapLoaded", new String[]{"mapLoaded", "W"});
        putBooleanField(result, map, "useFogOfWar", new String[]{"useFogOfWar", "E"});
        putBooleanField(result, map, "useLineOfSightFog", new String[]{"useLineOfSightFog", "F"});
        putBooleanField(result, map, "revealedMap", new String[]{"revealedMap", "G"});
        putCollectionField(result, map, "tilesets", new String[]{"tilesets", "t"});
        putCollectionField(result, map, "layers", new String[]{"layers", "z"});
        putCollectionField(result, map, "objectGroups", new String[]{"objectGroups", "P"});
        putField(result, map, "groundLayer", new String[]{"groundLayer", "u"});
        putField(result, map, "groundDetailsLayer", new String[]{"groundDetailsLayer", "v"});
        putField(result, map, "groundDetails2Layer", new String[]{"groundDetails2Layer", "w"});
        putField(result, map, "pathingOverrideLayer", new String[]{"pathingOverrideLayer", "x"});
        putField(result, map, "itemsLayer", new String[]{"itemsLayer", "y"});
        putField(result, map, "tileRenderCache", new String[]{"tileRenderCache", "al"});
        putField(result, map, "fullScaleTileAtlas", new String[]{"fullScaleTileAtlas", "l"});
        putField(result, map, "halfScaleTileAtlas", new String[]{"halfScaleTileAtlas", "m"});
        putArrayLengthField(result, map, "registeredTilesLength", new String[]{"registeredTiles", "B"});
        putIntField(result, map, "nextRegisteredTileId", new String[]{"nextRegisteredTileId", "as"});
        return Collections.unmodifiableMap(result);
    }

    public static List<Object> tilesetsSnapshot(Object map) {
        requireMapEngine(map);
        return snapshotField(map, new String[]{"tilesets", "t"});
    }

    public static List<Object> layersSnapshot(Object map) {
        requireMapEngine(map);
        return snapshotField(map, new String[]{"layers", "z"});
    }

    public static List<Object> objectGroupsSnapshot(Object map) {
        requireMapEngine(map);
        return snapshotField(map, new String[]{"objectGroups", "P"});
    }

    public static Object tileRenderCacheFromMapEngine(Object map) {
        requireMapEngine(map);
        return RustedReflection.getFieldValue(map, new String[]{"tileRenderCache", "al"});
    }

    public static Object fullScaleTileAtlasFromMapEngine(Object map) {
        requireMapEngine(map);
        return RustedReflection.getFieldValue(map, new String[]{"fullScaleTileAtlas", "l"});
    }

    public static Object halfScaleTileAtlasFromMapEngine(Object map) {
        requireMapEngine(map);
        return RustedReflection.getFieldValue(map, new String[]{"halfScaleTileAtlas", "m"});
    }

    public static Object getRegisteredTile(Object map, short registeredTileId) {
        requireMapEngine(map);
        return RustedReflection.invokeInstance(map, new String[]{"getRegisteredTile", "a"},
                Short.valueOf(registeredTileId));
    }

    public static Object getTileAtTilePosition(Object map, int tileX, int tileY) {
        requireMapEngine(map);
        return RustedReflection.invokeInstance(map, new String[]{"getTileAtTilePosition", "d"},
                Integer.valueOf(tileX), Integer.valueOf(tileY));
    }

    public static Object getTileAtTilePositionSafe(Object map, int tileX, int tileY) {
        requireMapEngine(map);
        return RustedReflection.invokeInstance(map, new String[]{"getTileAtTilePositionSafe", "e"},
                Integer.valueOf(tileX), Integer.valueOf(tileY));
    }

    public static Object getTileAtWorldPosition(Object map, float worldX, float worldY) {
        requireMapEngine(map);
        return RustedReflection.invokeInstance(map, new String[]{"getTileAtWorldPosition", "c"},
                Float.valueOf(worldX), Float.valueOf(worldY));
    }

    public static boolean isInMapBounds(Object map, int tileX, int tileY) {
        requireMapEngine(map);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(map, new String[]{"isInMapBounds", "c"},
                Integer.valueOf(tileX), Integer.valueOf(tileY)));
    }

    public static float worldToTileX(Object map, float worldX) {
        requireMapEngine(map);
        Object value = RustedReflection.invokeInstance(map, new String[]{"worldToTileX", "a"},
                Float.valueOf(worldX));
        return value instanceof Number ? ((Number) value).floatValue() : 0.0F;
    }

    public static float worldToTileY(Object map, float worldY) {
        requireMapEngine(map);
        Object value = RustedReflection.invokeInstance(map, new String[]{"worldToTileY", "b"},
                Float.valueOf(worldY));
        return value instanceof Number ? ((Number) value).floatValue() : 0.0F;
    }

    public static Map<String, Object> describeMapLayer(Object layer) {
        requireMapLayer(layer);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putField(result, layer, "map", new String[]{"map", "i"});
        putIntField(result, layer, "layerIndex", new String[]{"layerIndex", "j"});
        putField(result, layer, "name", new String[]{"name", "k"});
        putField(result, layer, "lowerCaseName", new String[]{"lowerCaseName", "l"});
        putBooleanField(result, layer, "visible", new String[]{"visible", "m"});
        putIntField(result, layer, "width", new String[]{"width", "n"});
        putIntField(result, layer, "height", new String[]{"height", "o"});
        putField(result, layer, "properties", new String[]{"properties", "p"});
        putArrayLengthField(result, layer, "tileIdsLength", new String[]{"tileIds", "q"});
        putBooleanField(result, layer, "isGroundLayer", new String[]{"isGroundLayer", "r"});
        putBooleanField(result, layer, "isItemsLayer", new String[]{"isItemsLayer", "s"});
        putBooleanField(result, layer, "hasNonAtlasTiles", new String[]{"hasNonAtlasTiles", "w"});
        return Collections.unmodifiableMap(result);
    }

    public static List<Object> layerTileIdsSnapshot(Object layer) {
        requireMapLayer(layer);
        return snapshotField(layer, new String[]{"tileIds", "q"});
    }

    public static Object getLayerTile(Object layer, int tileX, int tileY) {
        requireMapLayer(layer);
        return RustedReflection.invokeInstance(layer, new String[]{"getTile", "a"},
                Integer.valueOf(tileX), Integer.valueOf(tileY));
    }

    public static Map<String, Object> describeMapTile(Object tile) {
        requireMapTile(tile);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putField(result, tile, "tileset", new String[]{"tileset", "a"});
        putIntField(result, tile, "localTileIndex", new String[]{"localTileIndex", "b"});
        putIntField(result, tile, "atlasIndex", new String[]{"atlasIndex", "c"});
        putIntField(result, tile, "registeredTileId", new String[]{"registeredTileId", "d"});
        result.putAll(tileTerrainFlags(tile));
        putBooleanField(result, tile, "isResourcePool", new String[]{"isResourcePool", "i"});
        putIntField(result, tile, "pathingCost", new String[]{"pathingCost", "j"});
        putArrayLengthField(result, tile, "variantsLength", new String[]{"variants", "m"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> tileTerrainFlags(Object tile) {
        requireMapTile(tile);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putBooleanField(result, tile, "isWater", new String[]{"isWater", "e"});
        putBooleanField(result, tile, "isWaterBridge", new String[]{"isWaterBridge", "f"});
        putBooleanField(result, tile, "isLava", new String[]{"isLava", "g"});
        putBooleanField(result, tile, "isCliff", new String[]{"isCliff", "h"});
        putBooleanField(result, tile, "isLargeCliffOrTreeBlocker",
                new String[]{"isLargeCliffOrTreeBlocker", "k"});
        putBooleanField(result, tile, "blocksBuildings", new String[]{"blocksBuildings", "l"});
        return Collections.unmodifiableMap(result);
    }

    public static List<Object> tileVariantsSnapshot(Object tile) {
        requireMapTile(tile);
        return snapshotField(tile, new String[]{"variants", "m"});
    }

    public static boolean isWater(Object tile) {
        requireMapTile(tile);
        return RustedReflection.getBooleanField(tile, new String[]{"isWater", "e"});
    }

    public static boolean isWaterBridge(Object tile) {
        requireMapTile(tile);
        return RustedReflection.getBooleanField(tile, new String[]{"isWaterBridge", "f"});
    }

    public static boolean isLava(Object tile) {
        requireMapTile(tile);
        return RustedReflection.getBooleanField(tile, new String[]{"isLava", "g"});
    }

    public static boolean isCliff(Object tile) {
        requireMapTile(tile);
        return RustedReflection.getBooleanField(tile, new String[]{"isCliff", "h"});
    }

    public static boolean blocksBuildings(Object tile) {
        requireMapTile(tile);
        return RustedReflection.getBooleanField(tile, new String[]{"blocksBuildings", "l"});
    }

    public static Map<String, Object> describeTileset(Object tileset) {
        requireTileset(tileset);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putField(result, tileset, "name", new String[]{"name", "a"});
        putField(result, tileset, "image", new String[]{"image", "b"});
        putField(result, tileset, "imageSource", new String[]{"imageSource", "c"});
        putIntField(result, tileset, "tileWidth", new String[]{"tileWidth", "d"});
        putIntField(result, tileset, "tileHeight", new String[]{"tileHeight", "e"});
        putIntField(result, tileset, "imageWidth", new String[]{"imageWidth", "f"});
        putIntField(result, tileset, "imageHeight", new String[]{"imageHeight", "g"});
        putIntField(result, tileset, "spacing", new String[]{"spacing", "h"});
        putIntField(result, tileset, "margin", new String[]{"margin", "i"});
        putIntField(result, tileset, "columns", new String[]{"columns", "j"});
        putFloatField(result, tileset, "scale", new String[]{"scale", "k"});
        putIntField(result, tileset, "firstGid", new String[]{"firstGid", "l"});
        putIntField(result, tileset, "lastGid", new String[]{"lastGid", "m"});
        putIntField(result, tileset, "tilesetIndex", new String[]{"tilesetIndex", "n"});
        putField(result, tileset, "map", new String[]{"map", "o"});
        putField(result, tileset, "tilePropertiesByIndex", new String[]{"tilePropertiesByIndex", "x"});
        putBooleanField(result, tileset, "usedInMap", new String[]{"usedInMap", "p"});
        putBooleanField(result, tileset, "usedOnItemsLayer", new String[]{"usedOnItemsLayer", "q"});
        putBooleanField(result, tileset, "usedOnNonGroundLayer", new String[]{"usedOnNonGroundLayer", "r"});
        putBooleanField(result, tileset, "hasUnitTileProperties", new String[]{"hasUnitTileProperties", "s"});
        putField(result, tileset, "cachedSourceRect", new String[]{"cachedSourceRect", "v"});
        putIntField(result, tileset, "cachedSourceRectTileIndex", new String[]{"cachedSourceRectTileIndex", "w"});
        return Collections.unmodifiableMap(result);
    }

    public static Object getTileProperties(Object tileset, int localTileIndex) {
        requireTileset(tileset);
        return RustedReflection.invokeInstance(tileset, new String[]{"getTileProperties", "a"},
                Integer.valueOf(localTileIndex));
    }

    public static Object getTilesetTileSourceRectCached(Object tileset, int localTileIndex) {
        requireTileset(tileset);
        return RustedReflection.invokeInstance(tileset, new String[]{"getTileSourceRectCached", "b"},
                Integer.valueOf(localTileIndex));
    }

    public static Integer getTilePropertyInt(Object tileset, String section, String key) {
        requireTileset(tileset);
        Object value = RustedReflection.invokeInstance(tileset, new String[]{"getTilePropertyInt", "b"},
                section, key);
        return value instanceof Number ? Integer.valueOf(((Number) value).intValue()) : null;
    }

    public static boolean tilesetContainsGid(Object tileset, int gid) {
        requireTileset(tileset);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(tileset, new String[]{"containsGid", "d"},
                Integer.valueOf(gid)));
    }

    public static String embeddedImagePrefix() {
        Object value = RustedReflection.getStaticFieldValue(TILESET_CLASSES, new String[]{"embeddedImagePrefix", "t"});
        return value != null ? value.toString() : null;
    }

    public static List<Object> tilesetImageCacheEntriesSnapshot() {
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(
                RustedReflection.getStaticFieldValue(TILESET_CLASSES,
                        new String[]{"tilesetImageCacheEntries", "u"})));
    }

    public static int nextEmbeddedImageId() {
        Object value = RustedReflection.getStaticFieldValue(TILESET_IMAGE_CACHE_ENTRY_CLASSES,
                new String[]{"nextEmbeddedImageId", "a"});
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    public static Map<String, Object> describeTilesetImageCacheEntry(Object entry) {
        requireTilesetImageCacheEntry(entry);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putBooleanField(result, entry, "markedInUse", new String[]{"markedInUse", "b"});
        putField(result, entry, "basePathPrefix", new String[]{"basePathPrefix", "c"});
        putField(result, entry, "imageKey", new String[]{"imageKey", "d"});
        putField(result, entry, "image", new String[]{"image", "e"});
        putField(result, entry, "embeddedBase64Data", new String[]{"embeddedBase64Data", "f"});
        putField(result, entry, "debugSourceName", new String[]{"debugSourceName", "g"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeTileAtlas(Object atlas) {
        requireTileAtlas(atlas);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putIntField(result, atlas, "tileCount", new String[]{"tileCount", "a"});
        putField(result, atlas, "atlasImage", new String[]{"atlasImage", "b"});
        putField(result, atlas, "atlasCanvas", new String[]{"atlasCanvas", "c"});
        putField(result, atlas, "drawPaint", new String[]{"drawPaint", "d"});
        putIntField(result, atlas, "tileWidth", new String[]{"tileWidth", "e"});
        putIntField(result, atlas, "tileHeight", new String[]{"tileHeight", "f"});
        putIntField(result, atlas, "slotWidth", new String[]{"slotWidth", "g"});
        putIntField(result, atlas, "slotHeight", new String[]{"slotHeight", "h"});
        putFloatField(result, atlas, "scale", new String[]{"scale", "i"});
        putBooleanField(result, atlas, "alphaAtlasMode", new String[]{"alphaAtlasMode", "j"});
        putField(result, atlas, "alphaTileAtlas", new String[]{"alphaTileAtlas", "k"});
        putField(result, atlas, "tileSourceRectScratch", new String[]{"tileSourceRectScratch", "l"});
        putField(result, atlas, "paddingSourceRectScratch", new String[]{"paddingSourceRectScratch", "m"});
        putField(result, atlas, "paddingDestRectScratch", new String[]{"paddingDestRectScratch", "n"});
        putField(result, atlas, "cachedTileSourceRect", new String[]{"cachedTileSourceRect", "o"});
        putIntField(result, atlas, "cachedTileSourceRectIndex", new String[]{"cachedTileSourceRectIndex", "p"});
        return Collections.unmodifiableMap(result);
    }

    public static Object getAtlasImageForTileIndex(Object atlas, int atlasTileIndex) {
        requireTileAtlas(atlas);
        return RustedReflection.invokeInstance(atlas, new String[]{"getAtlasImageForTileIndex", "a"},
                Integer.valueOf(atlasTileIndex));
    }

    public static Object getAtlasTileSourceRectCached(Object atlas, int atlasTileIndex) {
        requireTileAtlas(atlas);
        return RustedReflection.invokeInstance(atlas, new String[]{"getTileSourceRectCached", "b"},
                Integer.valueOf(atlasTileIndex));
    }

    public static Map<String, Object> describeRenderCache(Object cache) {
        requireRenderCache(cache);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putIntField(result, cache, "cacheSize", new String[]{"cacheSize", "a"});
        putField(result, cache, "bufferImage", new String[]{"bufferImage", "b"});
        putField(result, cache, "bufferCanvas", new String[]{"bufferCanvas", "c"});
        putArrayShape(result, cache, "regions", new String[]{"regions", "d"});
        putField(result, cache, "drawPaint", new String[]{"drawPaint", "e"});
        putIntField(result, cache, "originWorldX", new String[]{"originWorldX", "f"});
        putIntField(result, cache, "originWorldY", new String[]{"originWorldY", "g"});
        putIntField(result, cache, "regionPixelSize", new String[]{"regionPixelSize", "i"});
        putIntField(result, cache, "regionStride", new String[]{"regionStride", "k"});
        putFloatField(result, cache, "inverseRegionPixelSize", new String[]{"inverseRegionPixelSize", "l"});
        putFloatField(result, cache, "scale", new String[]{"scale", "m"});
        putField(result, cache, "scratchRect", new String[]{"scratchRect", "o"});
        return Collections.unmodifiableMap(result);
    }

    public static List<Object> renderCacheRegionsSnapshot(Object cache) {
        requireRenderCache(cache);
        Object regions = RustedReflection.getFieldValue(cache, new String[]{"regions", "d"});
        List<Object> result = new ArrayList<Object>();
        for (int x = 0; x < arrayLength(regions); x++) {
            Object column = arrayValueAt(regions, x);
            for (int y = 0; y < arrayLength(column); y++) {
                Object region = arrayValueAt(column, y);
                if (region != null) {
                    result.add(region);
                }
            }
        }
        return Collections.unmodifiableList(result);
    }

    public static Object getRenderCacheRegion(Object cache, int gridX, int gridY) {
        requireRenderCache(cache);
        return RustedReflection.invokeInstance(cache, new String[]{"getRegion", "a"},
                Integer.valueOf(gridX), Integer.valueOf(gridY));
    }

    public static float getCurrentRenderScale(Object cache) {
        requireRenderCache(cache);
        Object value = RustedReflection.invokeInstance(cache, new String[]{"getCurrentRenderScale", "g"});
        return value instanceof Number ? ((Number) value).floatValue() : 0.0F;
    }

    public static Map<String, Object> describeRenderCacheRegion(Object region) {
        requireRenderCacheRegion(region);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putField(result, region, "canvas", new String[]{"canvas", "a"});
        putField(result, region, "bufferImage", new String[]{"bufferImage", "d"});
        putField(result, region, "fadeOutImage", new String[]{"fadeOutImage", "e"});
        putField(result, region, "fadeOutCanvas", new String[]{"fadeOutCanvas", "f"});
        putField(result, region, "paint", new String[]{"paint", "h"});
        putIntField(result, region, "gridX", new String[]{"gridX", "i"});
        putIntField(result, region, "gridY", new String[]{"gridY", "j"});
        putBooleanField(result, region, "dirty", new String[]{"dirty", "k"});
        putBooleanField(result, region, "fadeDirty", new String[]{"fadeDirty", "l"});
        putField(result, region, "scratchSourceRect", new String[]{"scratchSourceRect", "o"});
        putField(result, region, "scratchDestRect", new String[]{"scratchDestRect", "p"});
        putField(result, region, "scratchDestRectF", new String[]{"scratchDestRectF", "q"});
        putField(result, region, "worldRectScratch", new String[]{"worldRectScratch", "r"});
        putField(result, region, "ownerCache", new String[]{"ownerCache", "s"});
        result.put("worldX", Integer.valueOf(getRegionWorldX(region)));
        result.put("worldY", Integer.valueOf(getRegionWorldY(region)));
        result.put("worldRect", getRegionWorldRect(region));
        return Collections.unmodifiableMap(result);
    }

    public static int getRegionWorldX(Object region) {
        requireRenderCacheRegion(region);
        Object value = RustedReflection.invokeInstance(region, new String[]{"getWorldX", "c"});
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    public static int getRegionWorldY(Object region) {
        requireRenderCacheRegion(region);
        Object value = RustedReflection.invokeInstance(region, new String[]{"getWorldY", "d"});
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    public static Object getRegionWorldRect(Object region) {
        requireRenderCacheRegion(region);
        return RustedReflection.invokeInstance(region, new String[]{"getWorldRect", "b"});
    }

    private static void requireMapEngine(Object map) {
        requireAny(map, MAP_ENGINE_CLASSES, "MapEngine");
    }

    private static void requireMapLayer(Object layer) {
        requireAny(layer, MAP_LAYER_CLASSES, "MapLayer");
    }

    private static void requireMapTile(Object tile) {
        requireAny(tile, MAP_TILE_CLASSES, "MapTile");
    }

    private static void requireTileset(Object tileset) {
        requireAny(tileset, TILESET_CLASSES, "Tileset");
    }

    private static void requireTilesetImageCacheEntry(Object entry) {
        requireAny(entry, TILESET_IMAGE_CACHE_ENTRY_CLASSES, "TilesetImageCacheEntry");
    }

    private static void requireTileAtlas(Object atlas) {
        requireAny(atlas, TILE_ATLAS_CLASSES, "TileAtlas");
    }

    private static void requireRenderCache(Object cache) {
        requireAny(cache, MAP_TILE_RENDER_CACHE_CLASSES, "MapTileRenderCache");
    }

    private static void requireRenderCacheRegion(Object region) {
        requireAny(region, MAP_TILE_RENDER_CACHE_REGION_CLASSES, "MapTileRenderCacheRegion");
    }

    private static void requireAny(Object value, String[] classNames, String label) {
        if (value == null) {
            throw new IllegalArgumentException(label + " must not be null");
        }
        if (!RustedReflection.isAnyClass(value.getClass(), classNames)) {
            throw new IllegalArgumentException("Expected " + label + ", got " + value.getClass().getName());
        }
    }

    private static List<Object> snapshotField(Object owner, String[] fieldNames) {
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(
                RustedReflection.getFieldValue(owner, fieldNames)));
    }

    private static void putField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        result.put(key, RustedReflection.getFieldValue(owner, fieldNames));
    }

    private static void putCollectionField(Map<String, Object> result, Object owner, String key,
                                           String[] fieldNames) {
        Object value = RustedReflection.getFieldValue(owner, fieldNames);
        result.put(key, value);
        result.put(key + "Size", Integer.valueOf(RustedReflection.snapshotIterable(value).size()));
    }

    private static void putArrayLengthField(Map<String, Object> result, Object owner, String key,
                                            String[] fieldNames) {
        result.put(key, Integer.valueOf(arrayLength(RustedReflection.getFieldValue(owner, fieldNames))));
    }

    private static void putArrayShape(Map<String, Object> result, Object owner, String key,
                                      String[] fieldNames) {
        Object array = RustedReflection.getFieldValue(owner, fieldNames);
        result.put(key, array);
        result.put(key + "ColumnCount", Integer.valueOf(arrayLength(array)));
        result.put(key + "RowCount", Integer.valueOf(nestedArrayLength(array)));
    }

    private static void putIntField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        result.put(key, Integer.valueOf(RustedReflection.getIntField(owner, fieldNames)));
    }

    private static void putFloatField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        result.put(key, Float.valueOf(RustedReflection.getFloatField(owner, fieldNames)));
    }

    private static void putBooleanField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        result.put(key, Boolean.valueOf(RustedReflection.getBooleanField(owner, fieldNames)));
    }

    private static int arrayLength(Object array) {
        return array != null && array.getClass().isArray() ? Array.getLength(array) : 0;
    }

    private static int nestedArrayLength(Object array) {
        Object first = arrayValueAt(array, 0);
        return arrayLength(first);
    }

    private static Object arrayValueAt(Object array, int index) {
        if (array == null || !array.getClass().isArray() || index < 0 || index >= Array.getLength(array)) {
            return null;
        }
        return Array.get(array, index);
    }
}
