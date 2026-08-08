package io.github.endx.rustedfabricapi.api.map;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import io.github.endx.rustedfabricapi.mixin.accessor.MapEngineAccessor;
import rustedwarfare.map.MapEngine;
import rustedwarfare.map.MapObject;
import rustedwarfare.map.MapObjectGroup;
import rustedwarfare.map.Tileset;

/** Snapshot access to custom points and regions loaded from TMX object groups. */
public final class MapObjects {
    private MapObjects() {
    }

    public static MapObjectCatalog snapshot() {
        return snapshot(Maps.requireCurrent());
    }

    /** Captures the supplied loaded map. Call from the update thread or a map-load event. */
    @SuppressWarnings("unchecked")
    public static MapObjectCatalog snapshot(MapEngine map) {
        if (map == null) throw new NullPointerException("map");
        List<MapObjectGroup> nativeGroups =
                ((MapEngineAccessor) (Object) map).rustedfabricapi$getObjectGroups();
        ArrayList<MapObjectGroupSnapshot> groups = new ArrayList<MapObjectGroupSnapshot>();
        if (nativeGroups == null) return new MapObjectCatalog(groups);
        for (MapObjectGroup group : nativeGroups) {
            if (group == null) continue;
            ArrayList<MapObjectSnapshot> objects = new ArrayList<MapObjectSnapshot>();
            if (group.objects != null) {
                for (Object raw : group.objects) {
                    if (!(raw instanceof MapObject)) continue;
                    MapObject object = (MapObject) raw;
                    Tileset tileset = object.tileset;
                    objects.add(new MapObjectSnapshot(group.index, group.name, object.index,
                            object.name, object.type, object.x, object.y, object.width, object.height,
                            object.rotation, object.gid, object.tileIndex,
                            tileset != null ? tileset.name : null,
                            tileset != null ? tileset.imageSource : null,
                            copyProperties(object.properties)));
                }
            }
            groups.add(new MapObjectGroupSnapshot(group.index, group.name, group.width, group.height,
                    copyProperties(group.properties), objects));
        }
        return new MapObjectCatalog(groups);
    }

    static MapProperties copyProperties(Properties source) {
        if (source == null || source.isEmpty()) return MapProperties.empty();
        Map<String, String> values = new LinkedHashMap<String, String>();
        for (Map.Entry<Object, Object> entry : source.entrySet()) {
            if (entry.getKey() instanceof String && entry.getValue() instanceof String) {
                values.put((String) entry.getKey(), (String) entry.getValue());
            }
        }
        return new MapProperties(values);
    }
}
