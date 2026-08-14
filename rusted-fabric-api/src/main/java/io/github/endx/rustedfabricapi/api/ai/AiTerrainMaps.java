package io.github.endx.rustedfabricapi.api.ai;

import io.github.endx.rustedfabricapi.api.map.Maps;
import rustedwarfare.map.MapEngine;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** Per-loaded-map cache for the comparatively expensive static terrain analysis. */
public final class AiTerrainMaps {
    private static final Map<MapEngine, AiTerrainMapSnapshot> CACHE =
            Collections.synchronizedMap(new WeakHashMap<MapEngine, AiTerrainMapSnapshot>());

    private AiTerrainMaps() {
    }

    public static AiTerrainMapSnapshot current() {
        MapEngine map = Maps.requireCurrent();
        AiTerrainMapSnapshot existing = CACHE.get(map);
        if (existing != null) return existing;
        AiTerrainMapSnapshot captured = AiTerrainMapSnapshot.capture(
                map, AiTerrainMapSnapshot.DEFAULT_CELL_SIZE_TILES);
        synchronized (CACHE) {
            AiTerrainMapSnapshot raced = CACHE.get(map);
            if (raced != null) return raced;
            CACHE.put(map, captured);
        }
        return captured;
    }

    public static void invalidate() { CACHE.clear(); }
}
