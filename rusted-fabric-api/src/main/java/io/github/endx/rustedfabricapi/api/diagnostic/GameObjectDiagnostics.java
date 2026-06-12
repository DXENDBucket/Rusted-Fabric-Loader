package io.github.endx.rustedfabricapi.api.diagnostic;

import io.github.endx.rustedfabricapi.api.util.RustedReflection;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class GameObjectDiagnostics {
    private static final String[] GAME_OBJECT_CLASSES = {
            "rustedwarfare.game.GameObject",
            "com.corrodinggames.rts.gameFramework.w"
    };

    private GameObjectDiagnostics() {
    }

    public static Map<String, Object> describeGameObject(Object gameObject) {
        requireGameObject(gameObject);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", gameObject.getClass().getName());
        putLongField(result, gameObject, "id", new String[]{"id", "eh"});
        putBooleanField(result, gameObject, "removed", new String[]{"removed", "ej"});
        putIntField(result, gameObject, "drawLayer", new String[]{"drawLayer", "em"});
        putFloatField(result, gameObject, "x", new String[]{"x", "eo"});
        putFloatField(result, gameObject, "y", new String[]{"y", "ep"});
        putFloatField(result, gameObject, "height", new String[]{"height", "eq"});
        return Collections.unmodifiableMap(result);
    }

    public static int getDrawLayer(Object gameObject) {
        requireGameObject(gameObject);
        return RustedReflection.getIntField(gameObject, new String[]{"drawLayer", "em"});
    }

    public static void setDrawLayer(Object gameObject, int drawLayer) {
        requireGameObject(gameObject);
        RustedReflection.invokeInstance(gameObject, new String[]{"setDrawLayer", "S"}, Integer.valueOf(drawLayer));
    }

    public static boolean isVisibleInCamera(Object gameObject, Object gameEngine) {
        requireGameObject(gameObject);
        if (gameEngine == null) {
            throw new IllegalArgumentException("GameEngine must not be null");
        }
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(gameObject,
                new String[]{"isVisibleInCamera", "a"},
                gameEngine));
    }

    public static void removeFromGame(Object gameObject) {
        requireGameObject(gameObject);
        RustedReflection.invokeInstance(gameObject, new String[]{"removeFromGame", "a"});
    }

    public static Object getGameObjectById(long id, Class<?> type, boolean includeRemoved) {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        return RustedReflection.invokeStatic(GAME_OBJECT_CLASSES,
                new String[]{"getGameObjectById", "a"},
                Long.valueOf(id), type, Boolean.valueOf(includeRemoved));
    }

    public static Object getUnitById(long id, boolean includeRemoved) {
        return RustedReflection.invokeStatic(GAME_OBJECT_CLASSES,
                new String[]{"getUnitById", "a"},
                Long.valueOf(id), Boolean.valueOf(includeRemoved));
    }

    public static Object getOrderableUnitById(long id, boolean includeRemoved) {
        return RustedReflection.invokeStatic(GAME_OBJECT_CLASSES,
                new String[]{"getOrderableUnitById", "b"},
                Long.valueOf(id), Boolean.valueOf(includeRemoved));
    }

    public static void compactObjectList() {
        RustedReflection.invokeStatic(GAME_OBJECT_CLASSES, new String[]{"compactObjectList", "dL"});
    }

    private static void requireGameObject(Object gameObject) {
        if (gameObject == null) {
            throw new IllegalArgumentException("GameObject must not be null");
        }
        if (!RustedReflection.isAnyClass(gameObject.getClass(), GAME_OBJECT_CLASSES)) {
            throw new IllegalArgumentException("Expected GameObject, got " + gameObject.getClass().getName());
        }
    }

    private static void putIntField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        result.put(key, Integer.valueOf(RustedReflection.getIntField(owner, fieldNames)));
    }

    private static void putLongField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        Object value = RustedReflection.getFieldValue(owner, fieldNames);
        result.put(key, Long.valueOf(value instanceof Number ? ((Number) value).longValue() : 0L));
    }

    private static void putFloatField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        result.put(key, Float.valueOf(RustedReflection.getFloatField(owner, fieldNames)));
    }

    private static void putBooleanField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        result.put(key, Boolean.valueOf(RustedReflection.getBooleanField(owner, fieldNames)));
    }
}
