package io.github.endx.rustedfabricapi.api.diagnostic;

import io.github.endx.rustedfabricapi.api.util.RustedReflection;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class VisibilityDiagnostics {
    private static final String[] TEAM_CLASSES = {
            "rustedwarfare.game.Team",
            "com.corrodinggames.rts.game.n"
    };
    private static final String[] MAP_ENGINE_CLASSES = {
            "rustedwarfare.map.MapEngine",
            "com.corrodinggames.rts.game.b.b"
    };
    private static final String[] UNIT_CLASSES = {
            "rustedwarfare.unit.Unit",
            "com.corrodinggames.rts.game.units.am"
    };
    private static final String[] PER_TEAM_VISIBILITY_STATE_CLASSES = {
            "rustedwarfare.unit.visibility.PerTeamUnitVisibilityState",
            "com.corrodinggames.rts.game.units.an"
    };
    private static final String[] FOG_GHOST_CLASSES = {
            "rustedwarfare.visibility.FogGhost",
            "com.corrodinggames.rts.gameFramework.d.a"
    };

    private VisibilityDiagnostics() {
    }

    public static Map<String, Object> describeTeamFog(Object team) {
        requireTeam(team);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putIntField(result, team, "fogMapWidth", new String[]{"fogMapWidth", "L"});
        putIntField(result, team, "fogMapHeight", new String[]{"fogMapHeight", "M"});
        Object fogOfWarMap = RustedReflection.getFieldValue(team, new String[]{"fogOfWarMap", "N"});
        result.put("fogOfWarMap", fogOfWarMap);
        result.put("fogColumnCount", Integer.valueOf(arrayLength(fogOfWarMap)));
        result.put("fogRowCount", Integer.valueOf(nestedArrayLength(fogOfWarMap)));
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeMapFog(Object map) {
        requireMapEngine(map);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putBooleanField(result, map, "useFogOfWar", new String[]{"useFogOfWar", "E"});
        putBooleanField(result, map, "useLineOfSightFog", new String[]{"useLineOfSightFog", "F"});
        putBooleanField(result, map, "revealedMap", new String[]{"revealedMap", "G"});
        putField(result, map, "smoothFogAtlasImage", new String[]{"smoothFogAtlasImage", "K"});
        putField(result, map, "smoothFogAtlasCanvas", new String[]{"smoothFogAtlasCanvas", "L"});
        putArrayShape(result, map, "smoothFogCacheA", new String[]{"smoothFogCacheA", "M"});
        putArrayShape(result, map, "smoothFogCacheB", new String[]{"smoothFogCacheB", "N"});
        putField(result, map, "scratchFogSourceRect", new String[]{"scratchFogSourceRect", "O"});
        putBooleanField(result, map, "softFogSettingsInitialized",
                new String[]{"softFogSettingsInitialized", "H"});
        putBooleanField(result, map, "softFogFadingEnabled", new String[]{"softFogFadingEnabled", "I"});
        return Collections.unmodifiableMap(result);
    }

    public static Object getFogOfWarMap(Object team) {
        requireTeam(team);
        return RustedReflection.getFieldValue(team, new String[]{"fogOfWarMap", "N"});
    }

    public static Integer getFogByte(Object team, int tileX, int tileY) {
        Object fogOfWarMap = getFogOfWarMap(team);
        Object column = arrayValueAt(fogOfWarMap, tileX);
        Object value = arrayValueAt(column, tileY);
        return value instanceof Number ? Integer.valueOf(((Number) value).intValue()) : null;
    }

    public static void clearFogOfWar(Object team) {
        requireTeam(team);
        RustedReflection.invokeInstance(team, new String[]{"clearFogOfWar", "a"});
    }

    public static boolean isTileExploredByTeam(Object map, Object team, int tileX, int tileY) {
        requireMapEngine(map);
        requireTeam(team);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(map,
                new String[]{"isTileExploredByTeam", "a"},
                team, Integer.valueOf(tileX), Integer.valueOf(tileY)));
    }

    public static boolean isTileCurrentlyVisibleToTeam(Object map, int tileX, int tileY, Object team) {
        requireMapEngine(map);
        requireTeam(team);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(map,
                new String[]{"isTileCurrentlyVisibleToTeam", "a"},
                Integer.valueOf(tileX), Integer.valueOf(tileY), team));
    }

    public static boolean isWorldPointCurrentlyVisibleToTeam(Object map, float worldX, float worldY, Object team) {
        requireMapEngine(map);
        requireTeam(team);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(map,
                new String[]{"isWorldPointCurrentlyVisibleToTeam", "a"},
                Float.valueOf(worldX), Float.valueOf(worldY), team));
    }

    public static void revealMapArea(Object map, float worldX, float worldY, int radius, Object team,
                                     boolean refreshFogCache) {
        requireMapEngine(map);
        requireTeam(team);
        RustedReflection.invokeInstance(map, new String[]{"revealMapArea", "a"},
                Float.valueOf(worldX), Float.valueOf(worldY), Integer.valueOf(radius), team,
                Boolean.valueOf(refreshFogCache));
    }

    public static void revealMapAreaForSingleTeam(Object map, float worldX, float worldY, int radius,
                                                  Object team, boolean refreshFogCache) {
        requireMapEngine(map);
        requireTeam(team);
        RustedReflection.invokeInstance(map, new String[]{"revealMapAreaForSingleTeam", "b"},
                Float.valueOf(worldX), Float.valueOf(worldY), Integer.valueOf(radius), team,
                Boolean.valueOf(refreshFogCache));
    }

    public static byte calculateSmoothFogNeighborMask(Object map, int tileX, int tileY, Object fogMap,
                                                      byte threshold) {
        requireMapEngine(map);
        Object value = RustedReflection.invokeInstance(map, new String[]{"calculateSmoothFogNeighborMask", "a"},
                Integer.valueOf(tileX), Integer.valueOf(tileY), fogMap, Byte.valueOf(threshold));
        return value instanceof Number ? ((Number) value).byteValue() : 0;
    }

    public static void updateLineOfSightFog(Object map, float delta) {
        requireMapEngine(map);
        RustedReflection.invokeInstance(map, new String[]{"updateLineOfSightFog", "f"}, Float.valueOf(delta));
    }

    public static Map<String, Object> describeUnitVisibility(Object unit) {
        requireUnit(unit);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        Object states = RustedReflection.getFieldValue(unit, new String[]{"perTeamVisibilityStates", "dF"});
        result.put("perTeamVisibilityStates", states);
        result.put("perTeamVisibilityStateCount", Integer.valueOf(arrayLength(states)));
        result.put("techLevel", Integer.valueOf(getTechLevel(unit)));
        result.put("building", Boolean.valueOf(UnitRuntimeDiagnostics.isBuilding(unit)));
        result.put("spatial", SpatialIndexDiagnostics.describeUnitSpatialFields(unit));
        return Collections.unmodifiableMap(result);
    }

    public static Object getPerTeamVisibilityStates(Object unit) {
        requireUnit(unit);
        return RustedReflection.getFieldValue(unit, new String[]{"perTeamVisibilityStates", "dF"});
    }

    public static Object getPerTeamVisibilityState(Object unit, int teamId) {
        Object states = getPerTeamVisibilityStates(unit);
        return arrayValueAt(states, teamId);
    }

    public static Object getPerTeamVisibilityState(Object unit, Object team) {
        requireTeam(team);
        return getPerTeamVisibilityState(unit, getTeamId(team));
    }

    public static Map<String, Object> describePerTeamVisibilityState(Object state) {
        requireVisibilityState(state);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putBooleanField(result, state, "wasVisible", new String[]{"wasVisible", "a"});
        putIntField(result, state, "lastKnownTechLevel", new String[]{"lastKnownTechLevel", "b"});
        putField(result, state, "fogGhost", new String[]{"fogGhost", "c"});
        return Collections.unmodifiableMap(result);
    }

    public static boolean isHiddenByFogForTeam(Object unit, Object team) {
        requireUnit(unit);
        requireTeam(team);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(unit,
                new String[]{"isHiddenByFogForTeam", "d"}, team));
    }

    public static void updateVisibilityStateForTeam(Object unit, Object team) {
        requireUnit(unit);
        requireTeam(team);
        RustedReflection.invokeInstance(unit, new String[]{"updateVisibilityStateForTeam", "g"}, team);
    }

    public static void updateFogGhostForLocalPlayer(Object unit) {
        requireUnit(unit);
        RustedReflection.invokeInstance(unit, new String[]{"updateFogGhostForLocalPlayer", "cX"});
    }

    public static int getTechLevel(Object unit) {
        requireUnit(unit);
        Object value = RustedReflection.invokeInstance(unit, new String[]{"getTechLevel", "V", "g"});
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    public static Map<String, Object> describeFogGhost(Object fogGhost) {
        requireFogGhost(fogGhost);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putFloatField(result, fogGhost, "updateTickCounter", new String[]{"updateTickCounter", "a"});
        putFloatField(result, fogGhost, "ageTimer", new String[]{"ageTimer", "b"});
        putBooleanField(result, fogGhost, "removed", new String[]{"removed", "c"});
        putField(result, fogGhost, "unitType", new String[]{"unitType", "d"});
        putField(result, fogGhost, "sourceTeam", new String[]{"sourceTeam", "e"});
        putIntField(result, fogGhost, "techLevel", new String[]{"techLevel", "f"});
        putFloatField(result, fogGhost, "worldX", new String[]{"worldX", "g"});
        putFloatField(result, fogGhost, "worldY", new String[]{"worldY", "h"});
        putField(result, fogGhost, "viewerTeam", new String[]{"viewerTeam", "j"});
        putBooleanField(result, fogGhost, "blueprintGhost", new String[]{"blueprintGhost", "n"});
        putField(result, fogGhost, "builderUnit", new String[]{"builderUnit", "o"});
        putBooleanField(result, fogGhost, "underConstructionSnapshot",
                new String[]{"underConstructionSnapshot", "u"});
        putField(result, fogGhost, "sourceUnit", new String[]{"sourceUnit", "v"});
        result.put("stillValid", Boolean.valueOf(isFogGhostStillValid(fogGhost)));
        return Collections.unmodifiableMap(result);
    }

    public static List<Object> activeFogGhostsSnapshot() {
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(
                RustedReflection.getStaticFieldValue(FOG_GHOST_CLASSES, new String[]{"activeFogGhosts", "w"})));
    }

    public static void clearAllFogGhosts() {
        RustedReflection.invokeStatic(FOG_GHOST_CLASSES, new String[]{"clearAllFogGhosts", "a"});
    }

    public static void updateAllFogGhosts(float delta) {
        RustedReflection.invokeStatic(FOG_GHOST_CLASSES, new String[]{"updateAllFogGhosts", "a"},
                Float.valueOf(delta));
    }

    public static Object findNearbyBlueprintGhost(Object team, float worldX, float worldY) {
        requireTeam(team);
        return RustedReflection.invokeStatic(FOG_GHOST_CLASSES, new String[]{"findNearbyBlueprintGhost", "a"},
                team, Float.valueOf(worldX), Float.valueOf(worldY));
    }

    public static boolean isBlueprintGhostAtTile(Object team, int tileX, int tileY, int unitTypeId) {
        requireTeam(team);
        return Boolean.TRUE.equals(RustedReflection.invokeStatic(FOG_GHOST_CLASSES,
                new String[]{"isBlueprintGhostAtTile", "a"},
                team, Integer.valueOf(tileX), Integer.valueOf(tileY), Integer.valueOf(unitTypeId)));
    }

    public static boolean isRectOverBlueprintGhost(Object team, Object rectF, int unitTypeId) {
        requireTeam(team);
        return Boolean.TRUE.equals(RustedReflection.invokeStatic(FOG_GHOST_CLASSES,
                new String[]{"isRectOverBlueprintGhost", "a"},
                team, rectF, Integer.valueOf(unitTypeId)));
    }

    public static boolean isFogGhostStillValid(Object fogGhost) {
        requireFogGhost(fogGhost);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(fogGhost, new String[]{"isStillValid", "b"}));
    }

    public static void updateFogGhost(Object fogGhost, float delta) {
        requireFogGhost(fogGhost);
        RustedReflection.invokeInstance(fogGhost, new String[]{"update", "c"}, Float.valueOf(delta));
    }

    private static void requireTeam(Object team) {
        requireAny(team, TEAM_CLASSES, "Team");
    }

    private static void requireMapEngine(Object map) {
        requireAny(map, MAP_ENGINE_CLASSES, "MapEngine");
    }

    private static void requireUnit(Object unit) {
        requireAny(unit, UNIT_CLASSES, "Unit");
    }

    private static void requireVisibilityState(Object state) {
        requireAny(state, PER_TEAM_VISIBILITY_STATE_CLASSES, "PerTeamUnitVisibilityState");
    }

    private static void requireFogGhost(Object fogGhost) {
        requireAny(fogGhost, FOG_GHOST_CLASSES, "FogGhost");
    }

    private static void requireAny(Object value, String[] classNames, String label) {
        if (value == null) {
            throw new IllegalArgumentException(label + " must not be null");
        }
        if (!RustedReflection.isAnyClass(value.getClass(), classNames)) {
            throw new IllegalArgumentException("Expected " + label + ", got " + value.getClass().getName());
        }
    }

    private static int getTeamId(Object team) {
        return RustedReflection.getIntField(team, new String[]{"teamId", "k"});
    }

    private static void putField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        result.put(key, RustedReflection.getFieldValue(owner, fieldNames));
    }

    private static void putArrayShape(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
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
