package io.github.endx.rustedfabricapi.api.diagnostic;

import io.github.endx.rustedfabricapi.api.util.RustedReflection;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ProjectileRuntimeDiagnostics {
    private static final String[] PROJECTILE_CLASSES = {
            "rustedwarfare.game.Projectile",
            "com.corrodinggames.rts.game.f"
    };
    private static final String[] PROJECTILE_SPAWN_ENTRY_CLASSES = {
            "rustedwarfare.custom.spawn.ProjectileSpawnEntry",
            "com.corrodinggames.rts.game.units.custom.bk"
    };
    private static final String[] PROJECTILE_SPAWN_SEARCH_CALLBACK_CLASSES = {
            "rustedwarfare.custom.spawn.ProjectileSpawnSearchCallback",
            "com.corrodinggames.rts.game.units.custom.bj"
    };
    private static final String[] EFFECT_IMAGE_STRIP_CLASSES = {
            "rustedwarfare.render.EffectImageStrip",
            "com.corrodinggames.rts.gameFramework.d.g"
    };
    private static final String[] TURRET_PROJECTILE_BEHAVIOR_CLASSES = {
            "rustedwarfare.custom.runtime.TurretProjectileBehavior",
            "com.corrodinggames.rts.game.units.custom.b.k"
    };

    private ProjectileRuntimeDiagnostics() {
    }

    public static List<Object> activeProjectilesSnapshot() {
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(
                RustedReflection.getStaticFieldValue(PROJECTILE_CLASSES, new String[]{"activeProjectiles", "a"})));
    }

    public static Map<String, Object> describeProjectile(Object projectile) {
        requireProjectile(projectile);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putField(result, projectile, "projectileTemplate", new String[]{"projectileTemplate", "g"});
        putFloatField(result, projectile, "remainingLife", new String[]{"remainingLife", "h"});
        putFloatField(result, projectile, "delayedStartTimer", new String[]{"delayedStartTimer", "i"});
        putField(result, projectile, "sourceUnit", new String[]{"sourceUnit", "j"});
        putField(result, projectile, "sourceTurretIndex", new String[]{"sourceTurretIndex", "k"});
        putField(result, projectile, "targetUnit", new String[]{"targetUnit", "l"});
        putBooleanField(result, projectile, "targetGround", new String[]{"targetGround", "m"});
        putFloatField(result, projectile, "targetX", new String[]{"targetX", "n"});
        putFloatField(result, projectile, "targetY", new String[]{"targetY", "o"});
        putFloatField(result, projectile, "targetHeight", new String[]{"targetHeight", "p"});
        putField(result, projectile, "targetProjectile", new String[]{"targetProjectile", "q"});
        putFloatField(result, projectile, "targetSpeed", new String[]{"targetSpeed", "r"});
        putFloatField(result, projectile, "targetSpeedAcceleration", new String[]{"targetSpeedAcceleration", "s"});
        putFloatField(result, projectile, "speed", new String[]{"speed", "t"});
        putFloatField(result, projectile, "initialUnguidedSpeedX", new String[]{"initialUnguidedSpeedX", "u"});
        putFloatField(result, projectile, "initialUnguidedSpeedY", new String[]{"initialUnguidedSpeedY", "v"});
        putFloatField(result, projectile, "initialUnguidedSpeedHeight", new String[]{"initialUnguidedSpeedHeight", "w"});
        putFloatField(result, projectile, "drawSize", new String[]{"drawSize", "x"});
        putBooleanField(result, projectile, "instant", new String[]{"instant", "A"});
        putBooleanField(result, projectile, "laserEffect", new String[]{"laserEffect", "B"});
        putBooleanField(result, projectile, "lightingEffect", new String[]{"lightingEffect", "M"});
        putField(result, projectile, "frame", new String[]{"frame", "P"});
        putField(result, projectile, "shadowFrame", new String[]{"shadowFrame", "Q"});
        putField(result, projectile, "drawType", new String[]{"drawType", "R"});
        putBooleanField(result, projectile, "invisible", new String[]{"invisible", "S"});
        putFloatField(result, projectile, "directDamage", new String[]{"directDamage", "U"});
        putFloatField(result, projectile, "areaExpandTime", new String[]{"areaExpandTime", "W"});
        putFloatField(result, projectile, "areaDamage", new String[]{"areaDamage", "Y"});
        putFloatField(result, projectile, "areaRadius", new String[]{"areaRadius", "Z"});
        putField(result, projectile, "color", new String[]{"color", "ar"});
        putIntField(result, projectile, "spawnRecursionDepth", new String[]{"spawnRecursionDepth", "aD"});
        putField(result, projectile, "tags", new String[]{"tags", "aE"});
        putBooleanField(result, projectile, "autoTargetingOnDeadTarget",
                new String[]{"autoTargetingOnDeadTarget", "aG"});
        putBooleanField(result, projectile, "ballistic", new String[]{"ballistic", "aH"});
        putFloatField(result, projectile, "ballisticDelayMoveHeight",
                new String[]{"ballisticDelayMoveHeight", "aI"});
        putFloatField(result, projectile, "ballisticHeight", new String[]{"ballisticHeight", "aJ"});
        putBooleanField(result, projectile, "largeHitEffect", new String[]{"largeHitEffect", "aQ"});
        putBooleanField(result, projectile, "hitSound", new String[]{"hitSound", "aR"});
        return Collections.unmodifiableMap(result);
    }

    public static boolean hasExistingInterceptorForProjectile(Object projectile) {
        requireProjectile(projectile);
        return Boolean.TRUE.equals(RustedReflection.invokeStatic(TURRET_PROJECTILE_BEHAVIOR_CLASSES,
                new String[]{"hasExistingInterceptorForProjectile", "a"}, projectile));
    }

    public static Object turretProjectileBehaviorInstance() {
        return RustedReflection.getStaticFieldValue(TURRET_PROJECTILE_BEHAVIOR_CLASSES, new String[]{"instance", "a"});
    }

    public static Map<String, Object> describeTurretProjectileBehaviorStatics() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("instance", turretProjectileBehaviorInstance());
        result.put("scratchTurretPoint", RustedReflection.getStaticFieldValue(TURRET_PROJECTILE_BEHAVIOR_CLASSES,
                new String[]{"scratchTurretPoint", "b"}));
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeProjectileSpawnEntry(Object entry) {
        requireProjectileSpawnEntry(entry);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putField(result, entry, "projectileReference", new String[]{"projectileReference", "a"});
        putIntField(result, entry, "count", new String[]{"count", "b"});
        putFloatField(result, entry, "spawnChance", new String[]{"spawnChance", "c"});
        putIntField(result, entry, "maxSpawnLimit", new String[]{"maxSpawnLimit", "d"});
        putFloatField(result, entry, "xOffsetAbsolute", new String[]{"xOffsetAbsolute", "e"});
        putFloatField(result, entry, "yOffsetAbsolute", new String[]{"yOffsetAbsolute", "f"});
        putFloatField(result, entry, "offsetHeight", new String[]{"offsetHeight", "g"});
        putFloatField(result, entry, "offsetDir", new String[]{"offsetDir", "h"});
        putFloatField(result, entry, "xOffsetRelative", new String[]{"xOffsetRelative", "i"});
        putFloatField(result, entry, "yOffsetRelative", new String[]{"yOffsetRelative", "j"});
        putFloatField(result, entry, "offsetRandomX", new String[]{"offsetRandomX", "k"});
        putFloatField(result, entry, "offsetRandomY", new String[]{"offsetRandomY", "l"});
        putFloatField(result, entry, "offsetRandomDir", new String[]{"offsetRandomDir", "m"});
        putIntField(result, entry, "recursionLimit", new String[]{"recursionLimit", "n"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeProjectileSpawnSearchCallback(Object callback) {
        requireProjectileSpawnSearchCallback(callback);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putField(result, callback, "spawnedProjectile", new String[]{"spawnedProjectile", "a"});
        putField(result, callback, "spawnEntry", new String[]{"spawnEntry", "b"});
        putField(result, callback, "sourceUnit", new String[]{"sourceUnit", "c"});
        putField(result, callback, "parentProjectile", new String[]{"parentProjectile", "d"});
        putField(result, callback, "targetUnit", new String[]{"targetUnit", "e"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeEffectImageStrip(Object imageStrip) {
        requireEffectImageStrip(imageStrip);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putField(result, imageStrip, "name", new String[]{"name", "a"});
        putIntField(result, imageStrip, "frameWidth", new String[]{"frameWidth", "b"});
        putIntField(result, imageStrip, "frameHeight", new String[]{"frameHeight", "c"});
        putIntField(result, imageStrip, "frameXOffset", new String[]{"frameXOffset", "d"});
        putIntField(result, imageStrip, "frameYOffset", new String[]{"frameYOffset", "e"});
        putIntField(result, imageStrip, "frameStrideX", new String[]{"frameStrideX", "f"});
        putIntField(result, imageStrip, "frameStrideY", new String[]{"frameStrideY", "g"});
        putIntField(result, imageStrip, "framesPerRow", new String[]{"framesPerRow", "h"});
        putField(result, imageStrip, "image", new String[]{"image", "i"});
        putField(result, imageStrip, "shadowImage", new String[]{"shadowImage", "j"});
        putBooleanField(result, imageStrip, "singleFrame", new String[]{"singleFrame", "k"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> effectImageStripStaticScratchRects() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("scratchSourceRect", RustedReflection.getStaticFieldValue(EFFECT_IMAGE_STRIP_CLASSES,
                new String[]{"scratchSourceRect", "l"}));
        result.put("scratchDestRect", RustedReflection.getStaticFieldValue(EFFECT_IMAGE_STRIP_CLASSES,
                new String[]{"scratchDestRect", "m"}));
        return Collections.unmodifiableMap(result);
    }

    private static void requireProjectile(Object projectile) {
        requireAny(projectile, PROJECTILE_CLASSES, "Projectile");
    }

    private static void requireProjectileSpawnEntry(Object entry) {
        requireAny(entry, PROJECTILE_SPAWN_ENTRY_CLASSES, "ProjectileSpawnEntry");
    }

    private static void requireProjectileSpawnSearchCallback(Object callback) {
        requireAny(callback, PROJECTILE_SPAWN_SEARCH_CALLBACK_CLASSES, "ProjectileSpawnSearchCallback");
    }

    private static void requireEffectImageStrip(Object imageStrip) {
        requireAny(imageStrip, EFFECT_IMAGE_STRIP_CLASSES, "EffectImageStrip");
    }

    private static void requireAny(Object value, String[] classNames, String label) {
        if (value == null) {
            throw new IllegalArgumentException(label + " must not be null");
        }
        if (!RustedReflection.isAnyClass(value.getClass(), classNames)) {
            throw new IllegalArgumentException("Expected " + label + ", got " + value.getClass().getName());
        }
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
}
