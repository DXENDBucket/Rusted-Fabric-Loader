package io.github.endx.rustedfabricapi.api.diagnostic;

import io.github.endx.rustedfabricapi.api.util.RustedReflection;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class UiMinimapDiagnostics {
    private static final String[] GAME_ENGINE_CLASSES = {
            "rustedwarfare.core.GameEngine",
            "com.corrodinggames.rts.gameFramework.l"
    };
    private static final String[] MINIMAP_CLASSES = {
            "rustedwarfare.ui.Minimap",
            "com.corrodinggames.rts.gameFramework.f.o"
    };
    private static final String[] MINIMAP_DRAW_COMMAND_CLASSES = {
            "rustedwarfare.ui.MinimapDrawCommand",
            "com.corrodinggames.rts.gameFramework.f.o$1"
    };
    private static final String[] MINIMAP_DRAW_POINT_CLASSES = {
            "rustedwarfare.ui.MinimapDrawPoint",
            "com.corrodinggames.rts.gameFramework.f.p"
    };
    private static final String[] MINIMAP_SCAN_POINT_CLASSES = {
            "rustedwarfare.ui.MinimapScanPoint",
            "com.corrodinggames.rts.gameFramework.f.q"
    };
    private static final String[] MINIMAP_MARKER_TYPE_CLASSES = {
            "rustedwarfare.ui.MinimapMarkerType",
            "com.corrodinggames.rts.gameFramework.f.r"
    };
    private static final String[] MINIMAP_LINE_BATCH_CLASSES = {
            "rustedwarfare.ui.MinimapLineBatch",
            "com.corrodinggames.rts.gameFramework.f.s"
    };
    private static final String[] MINIMAP_TILE_ENTRY_CLASSES = {
            "rustedwarfare.ui.MinimapTileEntry",
            "com.corrodinggames.rts.gameFramework.f.t"
    };
    private static final String[] MINIMAP_UNIT_ENTRY_CLASSES = {
            "rustedwarfare.ui.MinimapUnitEntry",
            "com.corrodinggames.rts.gameFramework.f.u"
    };
    private static final String[] COMMAND_QUEUE_OVERLAY_CLASSES = {
            "rustedwarfare.ui.CommandQueueOverlay",
            "com.corrodinggames.rts.gameFramework.f.am"
    };

    private static final Alias[] MINIMAP_MARKER_TYPE_ALIASES = {
            new Alias("base", new String[]{"BASE", "a"}),
            new Alias("unit", new String[]{"UNIT", "b"}),
            new Alias("nuke", new String[]{"NUKE", "c"}),
            new Alias("message", new String[]{"MESSAGE", "d"})
    };

    private UiMinimapDiagnostics() {
    }

    public static Object minimapFromGameEngine(Object gameEngine) {
        requireAny(gameEngine, GAME_ENGINE_CLASSES, "GameEngine");
        return RustedReflection.getFieldValue(gameEngine, new String[]{"minimap", "bW"});
    }

    public static boolean isMinimap(Object value) {
        return value != null && RustedReflection.isAnyClass(value.getClass(), MINIMAP_CLASSES);
    }

    public static boolean isCommandQueueOverlay(Object value) {
        return value != null && RustedReflection.isAnyClass(value.getClass(), COMMAND_QUEUE_OVERLAY_CLASSES);
    }

    public static Map<String, Object> describeMinimap(Object minimap) {
        requireMinimap(minimap);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putFloatField(result, minimap, "screenX", new String[]{"screenX", "a"});
        putFloatField(result, minimap, "screenY", new String[]{"screenY", "b"});
        putFloatField(result, minimap, "width", new String[]{"width", "c"});
        putFloatField(result, minimap, "height", new String[]{"height", "d"});
        putBooleanField(result, minimap, "mapImageReady", new String[]{"mapImageReady", "e"});
        putBooleanField(result, minimap, "worldMappingReady", new String[]{"worldMappingReady", "f"});
        putIntField(result, minimap, "mapPixelWidth", new String[]{"mapPixelWidth", "g"});
        putIntField(result, minimap, "mapPixelHeight", new String[]{"mapPixelHeight", "h"});
        putFloatField(result, minimap, "inverseMapPixelWidth", new String[]{"inverseMapPixelWidth", "i"});
        putFloatField(result, minimap, "inverseMapPixelHeight", new String[]{"inverseMapPixelHeight", "j"});
        putIntField(result, minimap, "lastScreenX", new String[]{"lastScreenX", "k"});
        putIntField(result, minimap, "lastScreenY", new String[]{"lastScreenY", "l"});
        putBooleanField(result, minimap, "rebuildPending", new String[]{"rebuildPending", "m"});
        putField(result, minimap, "baseMapImage", new String[]{"baseMapImage", "F"});
        putField(result, minimap, "baseMapCanvas", new String[]{"baseMapCanvas", "G"});
        putField(result, minimap, "scratchScaledMapImage", new String[]{"scratchScaledMapImage", "H"});
        putField(result, minimap, "scratchScaledMapCanvas", new String[]{"scratchScaledMapCanvas", "I"});
        putField(result, minimap, "compositedMapImage", new String[]{"compositedMapImage", "J"});
        putField(result, minimap, "compositedMapCanvas", new String[]{"compositedMapCanvas", "K"});
        putFloatField(result, minimap, "positionRefreshTimer", new String[]{"positionRefreshTimer", "L"});
        putCollectionSizeField(result, minimap, "scanPulsePoints", new String[]{"scanPulsePoints", "Z"});
        putCollectionSizeField(result, minimap, "markerPoints", new String[]{"markerPoints", "aa"});
        putCollectionSizeField(result, minimap, "visibleTileEntries", new String[]{"visibleTileEntries", "ag"});
        putCollectionSizeField(result, minimap, "visibleUnitEntries", new String[]{"visibleUnitEntries", "af"});
        putField(result, minimap, "scratchDestRect", new String[]{"scratchDestRect", "V"});
        putField(result, minimap, "scratchRect", new String[]{"scratchRect", "ab"});
        putField(result, minimap, "scratchPoint", new String[]{"scratchPoint", "ad"});
        putField(result, minimap, "baseMapDrawCommand", new String[]{"baseMapDrawCommand", "ae"});
        result.put("bottom", Integer.valueOf(getBottom(minimap)));
        result.put("targetMinimapSize", Float.valueOf(getTargetMinimapSize(minimap)));
        return Collections.unmodifiableMap(result);
    }

    public static List<Object> scanPulsePointsSnapshot(Object minimap) {
        requireMinimap(minimap);
        return snapshotField(minimap, new String[]{"scanPulsePoints", "Z"});
    }

    public static List<Object> markerPointsSnapshot(Object minimap) {
        requireMinimap(minimap);
        return snapshotField(minimap, new String[]{"markerPoints", "aa"});
    }

    public static List<Object> visibleTileEntriesSnapshot(Object minimap) {
        requireMinimap(minimap);
        return snapshotField(minimap, new String[]{"visibleTileEntries", "ag"});
    }

    public static List<Object> visibleUnitEntriesSnapshot(Object minimap) {
        requireMinimap(minimap);
        return snapshotField(minimap, new String[]{"visibleUnitEntries", "af"});
    }

    public static Object baseMapDrawCommand(Object minimap) {
        requireMinimap(minimap);
        return RustedReflection.getFieldValue(minimap, new String[]{"baseMapDrawCommand", "ae"});
    }

    public static int getBottom(Object minimap) {
        requireMinimap(minimap);
        Object value = RustedReflection.invokeInstance(minimap, new String[]{"getBottom", "b"});
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    public static float getTargetMinimapSize(Object minimap) {
        requireMinimap(minimap);
        Object value = RustedReflection.invokeInstance(minimap, new String[]{"getTargetMinimapSize", "d"});
        return value instanceof Number ? ((Number) value).floatValue() : 0.0F;
    }

    public static Object worldToMinimapPoint(Object minimap, float worldX, float worldY) {
        requireMinimap(minimap);
        return RustedReflection.invokeInstance(minimap, new String[]{"worldToMinimapPoint", "b"},
                Float.valueOf(worldX), Float.valueOf(worldY));
    }

    public static Object screenToMapPoint(Object minimap, float screenX, float screenY) {
        requireMinimap(minimap);
        return RustedReflection.invokeInstance(minimap, new String[]{"screenToMapPoint", "c"},
                Float.valueOf(screenX), Float.valueOf(screenY));
    }

    public static float worldXToMinimapDelta(Object minimap, float worldX) {
        requireMinimap(minimap);
        Object value = RustedReflection.invokeInstance(minimap, new String[]{"worldXToMinimapDelta", "b"},
                Float.valueOf(worldX));
        return value instanceof Number ? ((Number) value).floatValue() : 0.0F;
    }

    public static float clampScreenX(Object minimap, float screenX) {
        requireMinimap(minimap);
        Object value = RustedReflection.invokeInstance(minimap, new String[]{"clampScreenX", "c"},
                Float.valueOf(screenX));
        return value instanceof Number ? ((Number) value).floatValue() : 0.0F;
    }

    public static float clampScreenY(Object minimap, float screenY) {
        requireMinimap(minimap);
        Object value = RustedReflection.invokeInstance(minimap, new String[]{"clampScreenY", "d"},
                Float.valueOf(screenY));
        return value instanceof Number ? ((Number) value).floatValue() : 0.0F;
    }

    public static void updateBounds(Object minimap) {
        requireMinimap(minimap);
        RustedReflection.invokeInstance(minimap, new String[]{"updateBounds", "a"});
    }

    public static void updateSizeAndPosition(Object minimap) {
        requireMinimap(minimap);
        RustedReflection.invokeInstance(minimap, new String[]{"updateSizeAndPosition", "f"});
    }

    public static void refreshCachedPositions(Object minimap) {
        requireMinimap(minimap);
        RustedReflection.invokeInstance(minimap, new String[]{"refreshCachedPositions", "h"});
    }

    public static void addDrawMarker(Object minimap, int x, int y, Object markerType) {
        requireMinimap(minimap);
        requireAny(markerType, MINIMAP_MARKER_TYPE_CLASSES, "MinimapMarkerType");
        RustedReflection.invokeInstance(minimap, new String[]{"addDrawMarker", "a"},
                Integer.valueOf(x), Integer.valueOf(y), markerType);
    }

    public static void addUnitScanPulse(Object minimap, int x, int y, float pulseTimer, Object unit) {
        requireMinimap(minimap);
        RustedReflection.invokeInstance(minimap, new String[]{"addUnitScanPulse", "a"},
                Integer.valueOf(x), Integer.valueOf(y), Float.valueOf(pulseTimer), unit);
    }

    public static void removeUnitFromMinimapCache(Object minimap, Object unit) {
        requireMinimap(minimap);
        RustedReflection.invokeInstance(minimap, new String[]{"removeUnitFromMinimapCache", "a"}, unit);
    }

    public static Map<String, Object> describeMinimapDrawCommand(Object drawCommand) {
        requireAny(drawCommand, MINIMAP_DRAW_COMMAND_CLASSES, "MinimapDrawCommand");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putField(result, drawCommand, "minimap", new String[]{"minimap", "a"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeMinimapDrawPoint(Object point) {
        requireAny(point, MINIMAP_DRAW_POINT_CLASSES, "MinimapDrawPoint");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putIntField(result, point, "x", new String[]{"x", "a"});
        putIntField(result, point, "y", new String[]{"y", "b"});
        putFloatField(result, point, "fadeAlpha", new String[]{"fadeAlpha", "c"});
        putFloatField(result, point, "scale", new String[]{"scale", "d"});
        Object markerType = RustedReflection.getFieldValue(point, new String[]{"markerType", "e"});
        result.put("markerType", markerType);
        result.put("markerTypeName", canonicalMinimapMarkerTypeName(markerType));
        putField(result, point, "minimap", new String[]{"minimap", "f"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeMinimapScanPoint(Object point) {
        requireAny(point, MINIMAP_SCAN_POINT_CLASSES, "MinimapScanPoint");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putBooleanField(result, point, "buildingPulse", new String[]{"buildingPulse", "a"});
        putIntField(result, point, "x", new String[]{"x", "b"});
        putIntField(result, point, "y", new String[]{"y", "c"});
        putFloatField(result, point, "pulseTimer", new String[]{"pulseTimer", "d"});
        putFloatField(result, point, "cooldownTimer", new String[]{"cooldownTimer", "e"});
        putField(result, point, "minimap", new String[]{"minimap", "f"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeMinimapLineBatch(Object batch) {
        requireAny(batch, MINIMAP_LINE_BATCH_CLASSES, "MinimapLineBatch");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putArrayLengthField(result, batch, "coordinatesLength", new String[]{"coordinates", "a"});
        putIntField(result, batch, "coordinateCount", new String[]{"coordinateCount", "b"});
        putField(result, batch, "paint", new String[]{"paint", "c"});
        putIntField(result, batch, "capacity", new String[]{"capacity", "d"});
        putBooleanField(result, batch, "drawAsRects", new String[]{"drawAsRects", "e"});
        putField(result, batch, "scratchRectF", new String[]{"scratchRectF", "f"});
        return Collections.unmodifiableMap(result);
    }

    public static List<Float> minimapLineBatchCoordinatesSnapshot(Object batch) {
        requireAny(batch, MINIMAP_LINE_BATCH_CLASSES, "MinimapLineBatch");
        Object coordinates = RustedReflection.getFieldValue(batch, new String[]{"coordinates", "a"});
        int count = Math.max(0, RustedReflection.getIntField(batch, new String[]{"coordinateCount", "b"}));
        return boundedFloatArraySnapshot(coordinates, count);
    }

    public static void addMinimapLineBatchPoint(Object batch, float x, float y) {
        requireAny(batch, MINIMAP_LINE_BATCH_CLASSES, "MinimapLineBatch");
        RustedReflection.invokeInstance(batch, new String[]{"addPoint", "a"}, Float.valueOf(x), Float.valueOf(y));
    }

    public static Map<String, Object> describeMinimapTileEntry(Object entry) {
        requireAny(entry, MINIMAP_TILE_ENTRY_CLASSES, "MinimapTileEntry");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putIntField(result, entry, "tileX", new String[]{"tileX", "a"});
        putIntField(result, entry, "tileY", new String[]{"tileY", "b"});
        putIntField(result, entry, "minimapX", new String[]{"minimapX", "c"});
        putIntField(result, entry, "minimapY", new String[]{"minimapY", "d"});
        putBooleanField(result, entry, "visibleToLocalTeam", new String[]{"visibleToLocalTeam", "e"});
        putField(result, entry, "minimap", new String[]{"minimap", "f"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeMinimapUnitEntry(Object entry) {
        requireAny(entry, MINIMAP_UNIT_ENTRY_CLASSES, "MinimapUnitEntry");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putField(result, entry, "unit", new String[]{"unit", "a"});
        putField(result, entry, "minimap", new String[]{"minimap", "b"});
        return Collections.unmodifiableMap(result);
    }

    public static List<String> minimapMarkerTypeNames() {
        List<String> result = new ArrayList<String>();
        for (Alias alias : MINIMAP_MARKER_TYPE_ALIASES) {
            result.add(alias.name);
        }
        return Collections.unmodifiableList(result);
    }

    public static List<Object> minimapMarkerTypes() {
        List<Object> result = new ArrayList<Object>();
        for (Alias alias : MINIMAP_MARKER_TYPE_ALIASES) {
            result.add(RustedReflection.getStaticFieldValue(MINIMAP_MARKER_TYPE_CLASSES, alias.fieldNames));
        }
        return Collections.unmodifiableList(result);
    }

    public static Object minimapMarkerType(String name) {
        String normalized = normalize(name);
        for (Alias alias : MINIMAP_MARKER_TYPE_ALIASES) {
            if (alias.name.equals(normalized)) {
                return RustedReflection.getStaticFieldValue(MINIMAP_MARKER_TYPE_CLASSES, alias.fieldNames);
            }
        }
        throw new IllegalArgumentException("Unknown minimap marker type: " + name);
    }

    public static String canonicalMinimapMarkerTypeName(Object markerType) {
        if (markerType == null) {
            return "";
        }
        requireAny(markerType, MINIMAP_MARKER_TYPE_CLASSES, "MinimapMarkerType");
        for (Alias alias : MINIMAP_MARKER_TYPE_ALIASES) {
            Object value = RustedReflection.getStaticFieldValue(MINIMAP_MARKER_TYPE_CLASSES, alias.fieldNames);
            if (value == markerType || value.equals(markerType)) {
                return alias.name;
            }
        }
        return markerType.toString();
    }

    public static Map<String, Object> describeCommandQueueOverlay(Object overlay) {
        requireAny(overlay, COMMAND_QUEUE_OVERLAY_CLASSES, "CommandQueueOverlay");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putField(result, overlay, "commandInterface", new String[]{"commandInterface", "i"});
        putCollectionSizeField(result, overlay, "trackedUnits", new String[]{"trackedUnits", "a"});
        putField(result, overlay, "lastCameraJumpTime", new String[]{"lastCameraJumpTime", "c"});
        putBooleanField(result, overlay, "appendMode", new String[]{"appendMode", "g"});
        return Collections.unmodifiableMap(result);
    }

    public static List<Object> trackedUnitsSnapshot(Object overlay) {
        requireAny(overlay, COMMAND_QUEUE_OVERLAY_CLASSES, "CommandQueueOverlay");
        return snapshotField(overlay, new String[]{"trackedUnits", "a"});
    }

    public static void jumpCameraToRecentTrackedUnit(Object overlay) {
        requireAny(overlay, COMMAND_QUEUE_OVERLAY_CLASSES, "CommandQueueOverlay");
        RustedReflection.invokeInstance(overlay, new String[]{"jumpCameraToRecentTrackedUnit", "a"});
    }

    public static void clearTrackedUnits(Object overlay) {
        requireAny(overlay, COMMAND_QUEUE_OVERLAY_CLASSES, "CommandQueueOverlay");
        RustedReflection.invokeInstance(overlay, new String[]{"clearTrackedUnits", "b"});
    }

    public static void collectSelectedUnits(Object overlay) {
        requireAny(overlay, COMMAND_QUEUE_OVERLAY_CLASSES, "CommandQueueOverlay");
        RustedReflection.invokeInstance(overlay, new String[]{"collectSelectedUnits", "c"});
    }

    public static void removeDeadTrackedUnits(Object overlay) {
        requireAny(overlay, COMMAND_QUEUE_OVERLAY_CLASSES, "CommandQueueOverlay");
        RustedReflection.invokeInstance(overlay, new String[]{"removeDeadTrackedUnits", "d"});
    }

    public static void refreshTrackedUnitReferencesById(Object overlay) {
        requireAny(overlay, COMMAND_QUEUE_OVERLAY_CLASSES, "CommandQueueOverlay");
        RustedReflection.invokeInstance(overlay, new String[]{"refreshTrackedUnitReferencesById", "e"});
    }

    private static List<Object> snapshotField(Object owner, String[] fieldNames) {
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(
                RustedReflection.getFieldValue(owner, fieldNames)));
    }

    private static List<Float> boundedFloatArraySnapshot(Object array, int count) {
        if (array == null || !array.getClass().isArray()) {
            return Collections.emptyList();
        }
        int length = Math.min(Math.max(0, count), Array.getLength(array));
        List<Float> result = new ArrayList<Float>(length);
        for (int i = 0; i < length; i++) {
            Object value = Array.get(array, i);
            if (value instanceof Number) {
                result.add(Float.valueOf(((Number) value).floatValue()));
            }
        }
        return Collections.unmodifiableList(result);
    }

    private static int arrayLength(Object array) {
        return array != null && array.getClass().isArray() ? Array.getLength(array) : 0;
    }

    private static void requireMinimap(Object value) {
        requireAny(value, MINIMAP_CLASSES, "Minimap");
    }

    private static void requireAny(Object value, String[] classNames, String label) {
        if (value == null || !RustedReflection.isAnyClass(value.getClass(), classNames)) {
            throw new IllegalArgumentException("Expected " + label + ", got " + describe(value));
        }
    }

    private static String describe(Object value) {
        return value == null ? "null" : value.getClass().getName();
    }

    private static void putField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        result.put(key, RustedReflection.getFieldValue(owner, fieldNames));
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

    private static void putArrayLengthField(Map<String, Object> result, Object owner, String key,
                                            String[] fieldNames) {
        result.put(key, Integer.valueOf(arrayLength(RustedReflection.getFieldValue(owner, fieldNames))));
    }

    private static void putCollectionSizeField(Map<String, Object> result, Object owner, String key,
                                               String[] fieldNames) {
        Object value = RustedReflection.getFieldValue(owner, fieldNames);
        result.put(key, value);
        result.put(key + "Size", Integer.valueOf(RustedReflection.snapshotIterable(value).size()));
    }

    private static String normalize(String value) {
        return value != null ? value.trim().toLowerCase(Locale.ROOT) : "";
    }

    private static final class Alias {
        private final String name;
        private final String[] fieldNames;

        private Alias(String name, String[] fieldNames) {
            this.name = name;
            this.fieldNames = fieldNames;
        }
    }
}
