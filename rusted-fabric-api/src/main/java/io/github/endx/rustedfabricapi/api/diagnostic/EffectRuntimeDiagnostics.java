package io.github.endx.rustedfabricapi.api.diagnostic;

import io.github.endx.rustedfabricapi.api.util.RustedReflection;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class EffectRuntimeDiagnostics {
    private static final String[] GAME_ENGINE_CLASSES = {
            "rustedwarfare.core.GameEngine",
            "com.corrodinggames.rts.gameFramework.l"
    };
    private static final String[] EFFECT_ENGINE_CLASSES = {
            "rustedwarfare.render.effect.EffectEngine",
            "com.corrodinggames.rts.gameFramework.d.c"
    };
    private static final String[] EFFECT_INSTANCE_CLASSES = {
            "rustedwarfare.render.effect.EffectInstance",
            "com.corrodinggames.rts.gameFramework.d.e"
    };
    private static final String[] EFFECT_PRIORITY_CLASSES = {
            "rustedwarfare.render.effect.EffectPriority",
            "com.corrodinggames.rts.gameFramework.d.h"
    };
    private static final String[] BUILT_IN_EFFECT_KIND_CLASSES = {
            "rustedwarfare.render.effect.BuiltInEffectKind",
            "com.corrodinggames.rts.gameFramework.d.d"
    };
    private static final String[] NOISE_CLOUD_OVERLAY_CLASSES = {
            "rustedwarfare.render.effect.NoiseCloudOverlay",
            "com.corrodinggames.rts.gameFramework.d.b"
    };
    private static final String[] GAME_OBJECT_CLASSES = {
            "rustedwarfare.game.GameObject",
            "com.corrodinggames.rts.gameFramework.w"
    };

    private static final Alias[] EFFECT_PRIORITY_ALIASES = {
            new Alias("verylow", new String[]{"verylow", "a"}),
            new Alias("low", new String[]{"low", "b"}),
            new Alias("high", new String[]{"high", "c"}),
            new Alias("veryhigh", new String[]{"veryhigh", "d"}),
            new Alias("critical", new String[]{"critical", "e"})
    };
    private static final Alias[] BUILT_IN_EFFECT_KIND_ALIASES = {
            new Alias("custom", new String[]{"custom", "a"}),
            new Alias("smoke", new String[]{"smoke", "b"}),
            new Alias("teleport", new String[]{"teleport", "c"}),
            new Alias("hitGround", new String[]{"hitGround", "d"}),
            new Alias("playerLand", new String[]{"playerLand", "e"}),
            new Alias("playerJump", new String[]{"playerJump", "f"}),
            new Alias("gemCollect", new String[]{"gemCollect", "g"}),
            new Alias("keyDoorOpen", new String[]{"keyDoorOpen", "h"}),
            new Alias("blood", new String[]{"blood", "i"})
    };

    private EffectRuntimeDiagnostics() {
    }

    public static Object effectEngineFromGameEngine(Object gameEngine) {
        requireAny(gameEngine, GAME_ENGINE_CLASSES, "GameEngine");
        return RustedReflection.getFieldValue(gameEngine, new String[]{"effectEngine", "bR"});
    }

    public static Map<String, Object> describeEffectEngine(Object engine) {
        requireEffectEngine(engine);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putIntField(result, engine, "activeEffectCount", new String[]{"activeEffectCount", "a"});
        putIntField(result, engine, "maxVeryLowEffects", new String[]{"maxVeryLowEffects", "b"});
        putIntField(result, engine, "maxLowEffects", new String[]{"maxLowEffects", "c"});
        putIntField(result, engine, "maxHighEffects", new String[]{"maxHighEffects", "d"});
        putIntField(result, engine, "maxVeryHighEffects", new String[]{"maxVeryHighEffects", "e"});
        putArrayLengthField(result, engine, "effectPoolLength", new String[]{"effectPool", "f"});
        putIntField(result, engine, "highestUsedEffectIndex", new String[]{"highestUsedEffectIndex", "g"});
        putBooleanField(result, engine, "effectPoolDirty", new String[]{"effectPoolDirty", "h"});
        putField(result, engine, "displacementShader", new String[]{"displacementShader", "k"});
        putField(result, engine, "effectsImage", new String[]{"effectsImage", "l"});
        putField(result, engine, "effects2Image", new String[]{"effects2Image", "m"});
        putField(result, engine, "scratchDestRectF", new String[]{"scratchDestRectF", "n"});
        putField(result, engine, "scratchSourceRect", new String[]{"scratchSourceRect", "o"});
        putField(result, engine, "scratchDestRect", new String[]{"scratchDestRect", "p"});
        putField(result, engine, "defaultEffectPaint", new String[]{"defaultEffectPaint", "q"});
        putField(result, engine, "shadowEffectPaint", new String[]{"shadowEffectPaint", "r"});
        putArrayLengthField(result, engine, "builtInImageStripsLength", new String[]{"builtInImageStrips", "s"});
        putField(result, engine, "nextEffectPriorityOverride", new String[]{"nextEffectPriorityOverride", "t"});
        putBooleanField(result, engine, "requireNextEffectOnscreen", new String[]{"requireNextEffectOnscreen", "u"});
        putBooleanField(result, engine, "allowNextEffectOffscreen", new String[]{"allowNextEffectOffscreen", "v"});
        putField(result, engine, "scratchPaint", new String[]{"scratchPaint", "w"});
        result.put("sharedLightingColorFilter", sharedLightingColorFilter());
        result.put("sharedLightingColorFilterColor", Integer.valueOf(sharedLightingColorFilterColor()));
        return Collections.unmodifiableMap(result);
    }

    public static List<Object> effectPoolSnapshot(Object engine) {
        requireEffectEngine(engine);
        return snapshotField(engine, new String[]{"effectPool", "f"});
    }

    public static List<Object> activeEffectsSnapshot(Object engine) {
        requireEffectEngine(engine);
        Object pool = RustedReflection.getFieldValue(engine, new String[]{"effectPool", "f"});
        List<Object> result = new ArrayList<Object>();
        for (int i = 0; i < arrayLength(pool); i++) {
            Object effect = arrayValueAt(pool, i);
            if (effect != null && RustedReflection.isAnyClass(effect.getClass(), EFFECT_INSTANCE_CLASSES)
                    && isEffectActive(effect)) {
                result.add(effect);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public static Object getBuiltInImageStrip(Object engine, int index) {
        requireEffectEngine(engine);
        return arrayValueAt(RustedReflection.getFieldValue(engine, new String[]{"builtInImageStrips", "s"}), index);
    }

    public static int getBuiltInImageStripIndex(Object engine, String name) {
        requireEffectEngine(engine);
        Object value = RustedReflection.invokeInstance(engine, new String[]{"getBuiltInImageStripIndex", "a"}, name);
        return value instanceof Number ? ((Number) value).intValue() : -1;
    }

    public static Map<String, Object> describeEffectInstance(Object effect) {
        requireEffectInstance(effect);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putField(result, effect, "engine", new String[]{"engine", "ay"});
        putField(result, effect, "effectTemplate", new String[]{"effectTemplate", "a"});
        putField(result, effect, "attachedObject", new String[]{"attachedObject", "b"});
        putBooleanField(result, effect, "castLightOnGround", new String[]{"castLightOnGround", "c"});
        putBooleanField(result, effect, "lightEffect", new String[]{"lightEffect", "d"});
        putBooleanField(result, effect, "showInFog", new String[]{"showInFog", "e"});
        putIntField(result, effect, "builtInEffectKind", new String[]{"builtInEffectKind", "g"});
        putBooleanField(result, effect, "active", new String[]{"active", "o"});
        putBooleanField(result, effect, "forceDraw", new String[]{"forceDraw", "p"});
        Object priority = RustedReflection.getFieldValue(effect, new String[]{"priority", "q"});
        result.put("priority", priority);
        result.put("priorityName", canonicalEffectPriorityName(priority));
        putBooleanField(result, effect, "fadeOut", new String[]{"fadeOut", "r"});
        putBooleanField(result, effect, "fadeIn", new String[]{"fadeIn", "s"});
        putFloatField(result, effect, "fadeInTime", new String[]{"fadeInTime", "t"});
        putBooleanField(result, effect, "atmospheric", new String[]{"atmospheric", "u"});
        putBooleanField(result, effect, "physics", new String[]{"physics", "v"});
        putFloatField(result, effect, "physicsGravity", new String[]{"physicsGravity", "w"});
        putIntField(result, effect, "color", new String[]{"color", "x"});
        putIntField(result, effect, "fadeToColor", new String[]{"fadeToColor", "y"});
        putFloatField(result, effect, "fadeToColorTime", new String[]{"fadeToColorTime", "z"});
        putIntField(result, effect, "spawnDepth", new String[]{"spawnDepth", "A"});
        putField(result, effect, "lightingColorFilter", new String[]{"lightingColorFilter", "B"});
        result.put("sharedLightingColorFilter", sharedLightingColorFilter());
        result.put("sharedLightingColorFilterColor", Integer.valueOf(sharedLightingColorFilterColor()));
        putFloatField(result, effect, "alpha", new String[]{"alpha", "E"});
        putFloatField(result, effect, "scaleTo", new String[]{"scaleTo", "F"});
        putFloatField(result, effect, "scaleFrom", new String[]{"scaleFrom", "G"});
        putFloatField(result, effect, "worldX", new String[]{"worldX", "I"});
        putFloatField(result, effect, "worldY", new String[]{"worldY", "J"});
        putFloatField(result, effect, "height", new String[]{"height", "K"});
        putBooleanField(result, effect, "drawLineTo", new String[]{"drawLineTo", "L"});
        putFloatField(result, effect, "lineTargetX", new String[]{"lineTargetX", "M"});
        putFloatField(result, effect, "lineTargetY", new String[]{"lineTargetY", "N"});
        putFloatField(result, effect, "lineTargetHeight", new String[]{"lineTargetHeight", "O"});
        putFloatField(result, effect, "velocityX", new String[]{"velocityX", "P"});
        putFloatField(result, effect, "velocityY", new String[]{"velocityY", "Q"});
        putFloatField(result, effect, "velocityHeight", new String[]{"velocityHeight", "R"});
        putFloatField(result, effect, "drawOscillationAmplitude",
                new String[]{"drawOscillationAmplitude", "S"});
        putFloatField(result, effect, "drawOscillationPeriod", new String[]{"drawOscillationPeriod", "T"});
        putFloatField(result, effect, "delayTimer", new String[]{"delayTimer", "U"});
        putFloatField(result, effect, "lifeRemaining", new String[]{"lifeRemaining", "V"});
        putFloatField(result, effect, "lifeMax", new String[]{"lifeMax", "W"});
        putFloatField(result, effect, "trailEffectTimer", new String[]{"trailEffectTimer", "X"});
        putFloatField(result, effect, "direction", new String[]{"direction", "Y"});
        putFloatField(result, effect, "angularVelocity", new String[]{"angularVelocity", "Z"});
        putField(result, effect, "text", new String[]{"text", "aa"});
        putField(result, effect, "textPaint", new String[]{"textPaint", "ab"});
        putFloatField(result, effect, "textOffsetX", new String[]{"textOffsetX", "ac"});
        putFloatField(result, effect, "textOffsetY", new String[]{"textOffsetY", "ad"});
        putBooleanField(result, effect, "animateFrames", new String[]{"animateFrames", "ae"});
        putIntField(result, effect, "animateFrameStart", new String[]{"animateFrameStart", "af"});
        putIntField(result, effect, "animateFrameEnd", new String[]{"animateFrameEnd", "ag"});
        putBooleanField(result, effect, "animateFramePingPong", new String[]{"animateFramePingPong", "ah"});
        putBooleanField(result, effect, "animateFrameLooping", new String[]{"animateFrameLooping", "ai"});
        putFloatField(result, effect, "animateFrameSpeed", new String[]{"animateFrameSpeed", "aj"});
        putFloatField(result, effect, "currentFrameFloat", new String[]{"currentFrameFloat", "ak"});
        putBooleanField(result, effect, "animateFrameReverse", new String[]{"animateFrameReverse", "al"});
        putIntField(result, effect, "frameIndex", new String[]{"frameIndex", "ap"});
        putIntField(result, effect, "imageStripIndex", new String[]{"imageStripIndex", "aq"});
        putIntField(result, effect, "drawLayer", new String[]{"drawLayer", "ar"});
        putBooleanField(result, effect, "shadow", new String[]{"shadow", "as"});
        putField(result, effect, "drawPaint", new String[]{"drawPaint", "at"});
        putFloatField(result, effect, "cachedPaintAlpha", new String[]{"cachedPaintAlpha", "au"});
        putIntField(result, effect, "cachedPaintColor", new String[]{"cachedPaintColor", "av"});
        putBooleanField(result, effect, "hasAppliedColorFilter", new String[]{"hasAppliedColorFilter", "aw"});
        putArrayLengthField(result, effect, "alphaPaintCacheLength", new String[]{"alphaPaintCache", "ax"});
        return Collections.unmodifiableMap(result);
    }

    public static Object createLineEffect(Object engine, float startX, float startY, float startHeight,
                                          float targetX, float targetY, float targetHeight) {
        requireEffectEngine(engine);
        return RustedReflection.invokeInstance(engine, new String[]{"createLineEffect", "a"},
                Float.valueOf(startX), Float.valueOf(startY), Float.valueOf(startHeight),
                Float.valueOf(targetX), Float.valueOf(targetY), Float.valueOf(targetHeight));
    }

    public static Object createLightEffect(Object engine, float x, float y, float height, int color) {
        requireEffectEngine(engine);
        return RustedReflection.invokeInstance(engine, new String[]{"createLightEffect", "b"},
                Float.valueOf(x), Float.valueOf(y), Float.valueOf(height), Integer.valueOf(color));
    }

    public static Object createAttachedLightEffect(Object engine, Object object, int color) {
        requireEffectEngine(engine);
        requireGameObject(object);
        return RustedReflection.invokeInstance(engine, new String[]{"createAttachedLightEffect", "a"},
                object, Integer.valueOf(color));
    }

    public static Object createAttachedLightEffect(Object engine, Object object, int color, float size) {
        requireEffectEngine(engine);
        requireGameObject(object);
        return RustedReflection.invokeInstance(engine, new String[]{"createAttachedLightEffect", "a"},
                object, Integer.valueOf(color), Float.valueOf(size));
    }

    public static void attachEffectToObject(Object effect, Object object) {
        requireEffectInstance(effect);
        requireGameObject(object);
        RustedReflection.invokeStatic(EFFECT_ENGINE_CLASSES, new String[]{"attachEffectToObject", "a"}, effect, object);
    }

    public static Object createSmallBuiltInEffect(Object engine, float x, float y, float height, float direction) {
        requireEffectEngine(engine);
        return RustedReflection.invokeInstance(engine, new String[]{"createSmallBuiltInEffect", "a"},
                Float.valueOf(x), Float.valueOf(y), Float.valueOf(height), Float.valueOf(direction));
    }

    public static Object createLargeBuiltInEffect(Object engine, float x, float y, float height,
                                                  float direction, int color) {
        requireEffectEngine(engine);
        return RustedReflection.invokeInstance(engine, new String[]{"createLargeBuiltInEffect", "b"},
                Float.valueOf(x), Float.valueOf(y), Float.valueOf(height),
                Float.valueOf(direction), Integer.valueOf(color));
    }

    public static Object createSmokeBuiltInEffect(Object engine, float x, float y, float height,
                                                 float direction, int color) {
        requireEffectEngine(engine);
        return RustedReflection.invokeInstance(engine, new String[]{"createSmokeBuiltInEffect", "c"},
                Float.valueOf(x), Float.valueOf(y), Float.valueOf(height),
                Float.valueOf(direction), Integer.valueOf(color));
    }

    public static Object createShockwaveBuiltInEffect(Object engine, float x, float y, float height, int color) {
        requireEffectEngine(engine);
        return RustedReflection.invokeInstance(engine, new String[]{"createShockwaveBuiltInEffect", "d"},
                Float.valueOf(x), Float.valueOf(y), Float.valueOf(height), Integer.valueOf(color));
    }

    public static Object createResourcePoolSmokeEffect(Object engine, float x, float y, float height, int color) {
        requireEffectEngine(engine);
        return RustedReflection.invokeInstance(engine, new String[]{"createResourcePoolSmokeEffect", "c"},
                Float.valueOf(x), Float.valueOf(y), Float.valueOf(height), Integer.valueOf(color));
    }

    public static Object createSmallExplosionBuiltInEffect(Object engine, float x, float y, float height) {
        requireEffectEngine(engine);
        return RustedReflection.invokeInstance(engine, new String[]{"createSmallExplosionBuiltInEffect", "b"},
                Float.valueOf(x), Float.valueOf(y), Float.valueOf(height));
    }

    public static void emitLargeExplosionBuiltInEffect(Object engine, float x, float y, float height) {
        requireEffectEngine(engine);
        RustedReflection.invokeInstance(engine, new String[]{"emitLargeExplosionBuiltInEffect", "a"},
                Float.valueOf(x), Float.valueOf(y), Float.valueOf(height));
    }

    public static void emitLargeExplosionBuiltInEffect(Object engine, float x, float y, float height,
                                                      float radius, float intensity) {
        requireEffectEngine(engine);
        RustedReflection.invokeInstance(engine, new String[]{"emitLargeExplosionBuiltInEffect", "a"},
                Float.valueOf(x), Float.valueOf(y), Float.valueOf(height),
                Float.valueOf(radius), Float.valueOf(intensity));
    }

    public static Object sharedLightingColorFilter() {
        return RustedReflection.getStaticFieldValue(EFFECT_INSTANCE_CLASSES,
                new String[]{"sharedLightingColorFilter", "C"});
    }

    public static int sharedLightingColorFilterColor() {
        Object value = RustedReflection.getStaticFieldValue(EFFECT_INSTANCE_CLASSES,
                new String[]{"sharedLightingColorFilterColor", "D"});
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    public static boolean isEffectActive(Object effect) {
        requireEffectInstance(effect);
        return RustedReflection.getBooleanField(effect, new String[]{"active", "o"});
    }

    public static Object getAlphaPaint(Object effect, float alpha) {
        requireEffectInstance(effect);
        return RustedReflection.invokeInstance(effect, new String[]{"getAlphaPaint", "a"}, Float.valueOf(alpha));
    }

    public static boolean isEffectEngine(Object value) {
        return value != null && RustedReflection.isAnyClass(value.getClass(), EFFECT_ENGINE_CLASSES);
    }

    public static boolean isEffectInstance(Object value) {
        return value != null && RustedReflection.isAnyClass(value.getClass(), EFFECT_INSTANCE_CLASSES);
    }

    public static List<String> effectPriorityNames() {
        return aliasNames(EFFECT_PRIORITY_ALIASES);
    }

    public static List<Object> effectPriorities() {
        return aliasValues(EFFECT_PRIORITY_CLASSES, EFFECT_PRIORITY_ALIASES);
    }

    public static Object effectPriority(String name) {
        return aliasValue(EFFECT_PRIORITY_CLASSES, EFFECT_PRIORITY_ALIASES, name, "effect priority");
    }

    public static String canonicalEffectPriorityName(Object priority) {
        return canonicalAliasName(EFFECT_PRIORITY_CLASSES, EFFECT_PRIORITY_ALIASES, priority);
    }

    public static boolean effectPriorityIsAtLeast(Object priority, Object minimum) {
        requireAny(priority, EFFECT_PRIORITY_CLASSES, "EffectPriority");
        requireAny(minimum, EFFECT_PRIORITY_CLASSES, "EffectPriority");
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(priority, new String[]{"isAtLeast", "a"}, minimum));
    }

    public static List<String> builtInEffectKindNames() {
        return aliasNames(BUILT_IN_EFFECT_KIND_ALIASES);
    }

    public static List<Object> builtInEffectKinds() {
        return aliasValues(BUILT_IN_EFFECT_KIND_CLASSES, BUILT_IN_EFFECT_KIND_ALIASES);
    }

    public static Object builtInEffectKind(String name) {
        return aliasValue(BUILT_IN_EFFECT_KIND_CLASSES, BUILT_IN_EFFECT_KIND_ALIASES, name, "built-in effect kind");
    }

    public static String canonicalBuiltInEffectKindName(Object kind) {
        return canonicalAliasName(BUILT_IN_EFFECT_KIND_CLASSES, BUILT_IN_EFFECT_KIND_ALIASES, kind);
    }

    public static Map<String, Object> describeNoiseCloudOverlay(Object overlay) {
        requireAny(overlay, NOISE_CLOUD_OVERLAY_CLASSES, "NoiseCloudOverlay");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putField(result, overlay, "noiseTexture", new String[]{"noiseTexture", "a"});
        putField(result, overlay, "sourceRect", new String[]{"sourceRect", "b"});
        putField(result, overlay, "destRect", new String[]{"destRect", "c"});
        putField(result, overlay, "paint", new String[]{"paint", "d"});
        putFloatField(result, overlay, "scrollX", new String[]{"scrollX", "e"});
        putFloatField(result, overlay, "scrollY", new String[]{"scrollY", "f"});
        result.put("enabled", Boolean.valueOf(isNoiseCloudOverlayEnabled(overlay)));
        return Collections.unmodifiableMap(result);
    }

    public static boolean isNoiseCloudOverlayEnabled(Object overlay) {
        requireAny(overlay, NOISE_CLOUD_OVERLAY_CLASSES, "NoiseCloudOverlay");
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(overlay, new String[]{"isEnabled", "a"}));
    }

    private static void requireEffectEngine(Object engine) {
        requireAny(engine, EFFECT_ENGINE_CLASSES, "EffectEngine");
    }

    private static void requireEffectInstance(Object effect) {
        requireAny(effect, EFFECT_INSTANCE_CLASSES, "EffectInstance");
    }

    private static void requireGameObject(Object object) {
        requireAny(object, GAME_OBJECT_CLASSES, "GameObject");
    }

    private static void requireAny(Object value, String[] classNames, String label) {
        if (value == null) {
            throw new IllegalArgumentException(label + " must not be null");
        }
        if (!RustedReflection.isAnyClass(value.getClass(), classNames)) {
            throw new IllegalArgumentException("Expected " + label + ", got " + value.getClass().getName());
        }
    }

    private static List<String> aliasNames(Alias[] aliases) {
        List<String> result = new ArrayList<String>(aliases.length);
        for (Alias alias : aliases) {
            result.add(alias.name);
        }
        return Collections.unmodifiableList(result);
    }

    private static List<Object> aliasValues(String[] classNames, Alias[] aliases) {
        List<Object> result = new ArrayList<Object>(aliases.length);
        for (Alias alias : aliases) {
            result.add(RustedReflection.getStaticFieldValue(classNames, alias.fieldNames));
        }
        return Collections.unmodifiableList(result);
    }

    private static Object aliasValue(String[] classNames, Alias[] aliases, String name, String label) {
        String normalized = normalizeAlias(name);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(label + " name must not be empty");
        }
        for (Alias alias : aliases) {
            if (alias.matches(normalized)) {
                return RustedReflection.getStaticFieldValue(classNames, alias.fieldNames);
            }
        }
        throw new IllegalArgumentException("Unknown " + label + " '" + name + "'");
    }

    private static String canonicalAliasName(String[] classNames, Alias[] aliases, Object value) {
        if (value == null) {
            return null;
        }
        String normalized = normalizeAlias(value instanceof Enum ? ((Enum<?>) value).name() : value.toString());
        for (Alias alias : aliases) {
            Object candidate = RustedReflection.getStaticFieldValue(classNames, alias.fieldNames);
            if (candidate == value || candidate.equals(value) || alias.matches(normalized)) {
                return alias.name;
            }
        }
        return value.toString();
    }

    private static List<Object> snapshotField(Object owner, String[] fieldNames) {
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(
                RustedReflection.getFieldValue(owner, fieldNames)));
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

    private static int arrayLength(Object array) {
        return array != null && array.getClass().isArray() ? Array.getLength(array) : 0;
    }

    private static Object arrayValueAt(Object array, int index) {
        if (array == null || !array.getClass().isArray() || index < 0 || index >= Array.getLength(array)) {
            return null;
        }
        return Array.get(array, index);
    }

    private static String normalizeAlias(String name) {
        if (name == null) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        String value = name.trim().toLowerCase(Locale.ROOT);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c != '_' && c != '-' && c != ' ') {
                result.append(c);
            }
        }
        return result.toString();
    }

    private static final class Alias {
        private final String name;
        private final String[] fieldNames;

        private Alias(String name, String[] fieldNames) {
            this.name = name;
            this.fieldNames = fieldNames;
        }

        private boolean matches(String normalizedName) {
            return normalizeAlias(name).equals(normalizedName);
        }
    }
}
