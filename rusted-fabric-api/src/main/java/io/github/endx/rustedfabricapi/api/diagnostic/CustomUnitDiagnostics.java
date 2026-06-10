package io.github.endx.rustedfabricapi.api.diagnostic;

import io.github.endx.rustedfabricapi.api.util.RustedReflection;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CustomUnitDiagnostics {
    private static final String[] BUILT_IN_EFFECT_TYPE_CLASSES = {
            "rustedwarfare.custom.effect.BuiltInEffectType",
            "com.corrodinggames.rts.game.units.custom.az"
    };
    private static final String[] CUSTOM_ACTION_AI_USE_CLASSES = {
            "rustedwarfare.custom.action.CustomActionAiUse",
            "com.corrodinggames.rts.game.units.custom.a.e"
    };
    private static final String[] CUSTOM_ACTION_TYPE_CLASSES = {
            "rustedwarfare.custom.action.CustomActionType",
            "com.corrodinggames.rts.game.units.custom.a.f"
    };
    private static final String[] TURRET_TEMPLATE_CLASSES = {
            "rustedwarfare.custom.TurretTemplate",
            "com.corrodinggames.rts.game.units.custom.bn"
    };
    private static final String[] CUSTOM_PROJECTILE_TEMPLATE_CLASSES = {
            "rustedwarfare.custom.CustomProjectileTemplate",
            "com.corrodinggames.rts.game.units.custom.bh"
    };
    private static final String[] EFFECT_TEMPLATE_CLASSES = {
            "rustedwarfare.custom.EffectTemplate",
            "com.corrodinggames.rts.game.units.custom.ay"
    };
    private static final String[] EFFECT_LIST_CLASSES = {
            "rustedwarfare.custom.EffectList",
            "com.corrodinggames.rts.game.units.custom.z"
    };
    private static final String[] MUTABLE_TAG_LIST_BUILDER_CLASSES = {
            "rustedwarfare.custom.MutableTagListBuilder",
            "com.corrodinggames.rts.game.units.custom.i"
    };
    private static final String[] RESOURCE_SHORTAGE_COLLECTOR_CLASSES = {
            "rustedwarfare.custom.resource.ResourceShortageCollector",
            "com.corrodinggames.rts.game.units.custom.e.c"
    };
    private static final String[] REQUIRED_UNIT_MISMATCH_REPORT_CLASSES = {
            "rustedwarfare.custom.sync.RequiredUnitMismatchReport",
            "com.corrodinggames.rts.game.units.custom.ab"
    };
    private static final String[] MOD_UNIT_AVAILABILITY_COUNTS_CLASSES = {
            "rustedwarfare.custom.sync.ModUnitAvailabilityCounts",
            "com.corrodinggames.rts.game.units.custom.ac"
    };
    private static final String[] MOD_INFO_CLASSES = {
            "rustedwarfare.mod.ModInfo",
            "com.corrodinggames.rts.gameFramework.i.b"
    };

    private CustomUnitDiagnostics() {
    }

    public static List<Object> builtInEffectTypes() {
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(
                RustedReflection.invokeStatic(BUILT_IN_EFFECT_TYPE_CLASSES, new String[]{"values"})));
    }

    public static Object builtInEffectType(String name) {
        requireText(name, "name");
        return RustedReflection.invokeStatic(BUILT_IN_EFFECT_TYPE_CLASSES, new String[]{"valueOf"}, name);
    }

    public static List<Object> customActionAiUses() {
        return enumValues(CUSTOM_ACTION_AI_USE_CLASSES);
    }

    public static Object customActionAiUse(String name) {
        requireText(name, "name");
        return RustedReflection.invokeStatic(CUSTOM_ACTION_AI_USE_CLASSES, new String[]{"valueOf"}, name);
    }

    public static List<Object> customActionTypes() {
        return enumValues(CUSTOM_ACTION_TYPE_CLASSES);
    }

    public static Object customActionType(String name) {
        requireText(name, "name");
        return RustedReflection.invokeStatic(CUSTOM_ACTION_TYPE_CLASSES, new String[]{"valueOf"}, name);
    }

    public static Map<String, Object> describeTurretTemplate(Object turretTemplate) {
        requireTurretTemplate(turretTemplate);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putField(result, turretTemplate, "name", new String[]{"name", "a"});
        putField(result, turretTemplate, "sectionName", new String[]{"sectionName", "b"});
        putIntField(result, turretTemplate, "index", new String[]{"index", "e"});
        putField(result, turretTemplate, "attachedTo", new String[]{"attachedTo", "y"});
        putField(result, turretTemplate, "slaveAttachedTurret", new String[]{"slaveAttachedTurret", "z"});
        putIntField(result, turretTemplate, "attachedToIndex", new String[]{"attachedToIndex", "w"});
        putIntField(result, turretTemplate, "linkedDelayWithTurretIndex",
                new String[]{"linkedDelayWithTurretIndex", "x"});
        putBooleanField(result, turretTemplate, "slave", new String[]{"slave", "A"});
        putBooleanField(result, turretTemplate, "canAttack", new String[]{"canAttack", "B"});
        putIntField(result, turretTemplate, "projectileIndex", new String[]{"projectileIndex", "R"});
        putIntField(result, turretTemplate, "altProjectileIndex", new String[]{"altProjectileIndex", "S"});
        putField(result, turretTemplate, "altProjectileCondition", new String[]{"altProjectileCondition", "T"});
        putBooleanField(result, turretTemplate, "hasIdleDirReversing", new String[]{"hasIdleDirReversing", "l"});
        putFloatField(result, turretTemplate, "idleDir", new String[]{"idleDir", "j"});
        putFloatField(result, turretTemplate, "idleDirReversing", new String[]{"idleDirReversing", "k"});
        putFloatField(result, turretTemplate, "delay", new String[]{"delay", "m"});
        putFloatField(result, turretTemplate, "warmup", new String[]{"warmup", "n"});
        putFloatField(result, turretTemplate, "energyUsage", new String[]{"energyUsage", "u"});
        putField(result, turretTemplate, "resourceUsage", new String[]{"resourceUsage", "v"});
        putBooleanField(result, turretTemplate, "hasTargetingRestrictions",
                new String[]{"hasTargetingRestrictions", "H"});
        putBooleanField(result, turretTemplate, "hasRangeRestriction", new String[]{"hasRangeRestriction", "I"});
        putField(result, turretTemplate, "canAttackCondition", new String[]{"canAttackCondition", "N"});
        putField(result, turretTemplate, "canOnlyAttackUnitsWithTags",
                new String[]{"canOnlyAttackUnitsWithTags", "O"});
        putField(result, turretTemplate, "canOnlyAttackUnitsWithoutTags",
                new String[]{"canOnlyAttackUnitsWithoutTags", "P"});
        putFloatField(result, turretTemplate, "canAttackMaxAngle", new String[]{"canAttackMaxAngle", "Q"});
        putFloatField(result, turretTemplate, "limitingRange", new String[]{"limitingRange", "ab"});
        putFloatField(result, turretTemplate, "resolvedLimitingRange", new String[]{"resolvedLimitingRange", "ad"});
        putFloatField(result, turretTemplate, "limitingRangeSquared", new String[]{"limitingRangeSquared", "ae"});
        putFloatField(result, turretTemplate, "limitingMinRange", new String[]{"limitingMinRange", "ag"});
        putFloatField(result, turretTemplate, "limitingMinRangeSquared",
                new String[]{"limitingMinRangeSquared", "ah"});
        putFloatField(result, turretTemplate, "limitingAngle", new String[]{"limitingAngle", "ai"});
        putField(result, turretTemplate, "showRangeUIGuide", new String[]{"showRangeUIGuide", "ac"});
        putField(result, turretTemplate, "interceptProjectilesWithTags",
                new String[]{"interceptProjectilesWithTags", "ak"});
        putFloatField(result, turretTemplate, "interceptProjectilesAndTargetingGroundUnderDistance",
                new String[]{"interceptProjectilesAndTargetingGroundUnderDistance", "al"});
        putFloatField(result, turretTemplate, "interceptProjectilesAndUnderDistance",
                new String[]{"interceptProjectilesAndUnderDistance", "am"});
        putFloatField(result, turretTemplate, "interceptProjectilesAndOverHeight",
                new String[]{"interceptProjectilesAndOverHeight", "an"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeCustomProjectileTemplate(Object projectileTemplate) {
        requireCustomProjectileTemplate(projectileTemplate);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putField(result, projectileTemplate, "name", new String[]{"name", "bh"});
        putIntField(result, projectileTemplate, "projectileIndex", new String[]{"projectileIndex", "bi"});
        putField(result, projectileTemplate, "unitMetadata", new String[]{"unitMetadata", "bj"});
        putIntField(result, projectileTemplate, "directDamage", new String[]{"directDamage", "b"});
        putIntField(result, projectileTemplate, "areaDamage", new String[]{"areaDamage", "c"});
        putIntField(result, projectileTemplate, "areaRadius", new String[]{"areaRadius", "i"});
        putBooleanField(result, projectileTemplate, "areaDamageNoFalloff",
                new String[]{"areaDamageNoFalloff", "g"});
        putBooleanField(result, projectileTemplate, "areaRadiusFromEdge", new String[]{"areaRadiusFromEdge", "h"});
        putFloatField(result, projectileTemplate, "areaIgnoreUnitsCloserThan",
                new String[]{"areaIgnoreUnitsCloserThan", "j"});
        putBooleanField(result, projectileTemplate, "friendlyFire", new String[]{"friendlyFire", "k"});
        putBooleanField(result, projectileTemplate, "targetGround", new String[]{"targetGround", "s"});
        putBooleanField(result, projectileTemplate, "targetGroundIncludeTargetHeight",
                new String[]{"targetGroundIncludeTargetHeight", "t"});
        putFloatField(result, projectileTemplate, "life", new String[]{"life", "v"});
        putFloatField(result, projectileTemplate, "speed", new String[]{"speed", "w"});
        putFloatField(result, projectileTemplate, "turnSpeed", new String[]{"turnSpeed", "O"});
        putFloatField(result, projectileTemplate, "targetSpeed", new String[]{"targetSpeed", "au"});
        putFloatField(result, projectileTemplate, "targetSpeedAcceleration",
                new String[]{"targetSpeedAcceleration", "av"});
        putFloatField(result, projectileTemplate, "targetGroundSpread", new String[]{"targetGroundSpread", "aK"});
        putFloatField(result, projectileTemplate, "targetGroundHeightOffset",
                new String[]{"targetGroundHeightOffset", "aL"});
        putBooleanField(result, projectileTemplate, "drawUnderUnits", new String[]{"drawUnderUnits", "U"});
        putBooleanField(result, projectileTemplate, "lightingEffect", new String[]{"lightingEffect", "V"});
        putBooleanField(result, projectileTemplate, "laserEffect", new String[]{"laserEffect", "W"});
        putBooleanField(result, projectileTemplate, "explodeOnEndOfLife",
                new String[]{"explodeOnEndOfLife", "aO"});
        putField(result, projectileTemplate, "trailEffect", new String[]{"trailEffect", "ah"});
        putFloatField(result, projectileTemplate, "trailEffectRate", new String[]{"trailEffectRate", "ag"});
        putField(result, projectileTemplate, "effectOnCreate", new String[]{"effectOnCreate", "ai"});
        putField(result, projectileTemplate, "explodeEffect", new String[]{"explodeEffect", "aX"});
        putField(result, projectileTemplate, "explodeEffectOnShield", new String[]{"explodeEffectOnShield", "aY"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeEffectTemplate(Object effectTemplate) {
        requireEffectTemplate(effectTemplate);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putField(result, effectTemplate, "name", new String[]{"name"});
        putField(result, effectTemplate, "builtInEffect", new String[]{"builtInEffect"});
        putFloatField(result, effectTemplate, "spawnChance", new String[]{"spawnChance"});
        putFloatField(result, effectTemplate, "life", new String[]{"life"});
        putFloatField(result, effectTemplate, "lifeRandom", new String[]{"lifeRandom"});
        putBooleanField(result, effectTemplate, "createWhenOffscreen", new String[]{"createWhenOffscreen"});
        putBooleanField(result, effectTemplate, "createWhenZoomedOut", new String[]{"createWhenZoomedOut"});
        putBooleanField(result, effectTemplate, "createWhenOverLiquid", new String[]{"createWhenOverLiquid"});
        putBooleanField(result, effectTemplate, "createWhenOverLand", new String[]{"createWhenOverLand"});
        putBooleanField(result, effectTemplate, "showInFog", new String[]{"showInFog"});
        putFloatField(result, effectTemplate, "scaleFrom", new String[]{"scaleFrom"});
        putFloatField(result, effectTemplate, "scaleTo", new String[]{"scaleTo"});
        putFloatField(result, effectTemplate, "alpha", new String[]{"alpha"});
        putIntField(result, effectTemplate, "color", new String[]{"color"});
        putFloatField(result, effectTemplate, "teamColorRatio", new String[]{"teamColorRatio"});
        putBooleanField(result, effectTemplate, "shadow", new String[]{"shadow"});
        putField(result, effectTemplate, "drawLayer", new String[]{"drawLayer"});
        putBooleanField(result, effectTemplate, "fadeOut", new String[]{"fadeOut"});
        putFloatField(result, effectTemplate, "fadeInTime", new String[]{"fadeInTime"});
        putIntField(result, effectTemplate, "frameIndex", new String[]{"frameIndex"});
        putIntField(result, effectTemplate, "frameIndexRandom", new String[]{"frameIndexRandom"});
        putBooleanField(result, effectTemplate, "attachedToUnit", new String[]{"attachedToUnit"});
        putBooleanField(result, effectTemplate, "atmospheric", new String[]{"atmospheric"});
        putBooleanField(result, effectTemplate, "physics", new String[]{"physics"});
        putFloatField(result, effectTemplate, "physicsGravity", new String[]{"physicsGravity"});
        putField(result, effectTemplate, "alsoEmitEffects", new String[]{"alsoEmitEffects"});
        putField(result, effectTemplate, "alsoEmitEffectsOnDeath", new String[]{"alsoEmitEffectsOnDeath"});
        putField(result, effectTemplate, "trailEffect", new String[]{"trailEffect"});
        putFloatField(result, effectTemplate, "trailEffectRate", new String[]{"trailEffectRate"});
        putField(result, effectTemplate, "alsoPlaySound", new String[]{"alsoPlaySound"});
        return Collections.unmodifiableMap(result);
    }

    public static String effectTemplateName(Object effectTemplate) {
        requireEffectTemplate(effectTemplate);
        return RustedReflection.getStringField(effectTemplate, new String[]{"name"});
    }

    public static Object defaultEffectTemplate() {
        return RustedReflection.getStaticFieldValue(EFFECT_TEMPLATE_CLASSES, new String[]{"defaultEffectTemplate"});
    }

    public static Map<String, Object> describeEffectList(Object effectList) {
        requireEffectList(effectList);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putField(result, effectList, "rawEffectList", new String[]{"rawEffectList", "a"});
        result.put("hasEffects", Boolean.valueOf(effectListHasEffects(effectList)));
        result.put("resolved", Boolean.valueOf(effectListIsResolved(effectList)));
        result.put("effectTemplates", effectListTemplates(effectList));
        result.put("metadata", RustedReflection.getFieldValue(effectList, new String[]{"metadata", "c"}));
        return Collections.unmodifiableMap(result);
    }

    public static boolean effectListHasEffects(Object effectList) {
        requireEffectList(effectList);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(effectList, new String[]{"hasEffects", "a"}));
    }

    public static boolean effectListIsResolved(Object effectList) {
        requireEffectList(effectList);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(effectList, new String[]{"isResolved", "b"}));
    }

    public static List<Object> effectListTemplates(Object effectList) {
        requireEffectList(effectList);
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(
                RustedReflection.getFieldValue(effectList, new String[]{"effectTemplates", "b"})));
    }

    public static Object newMutableTagListBuilder() {
        return RustedReflection.newInstance(MUTABLE_TAG_LIST_BUILDER_CLASSES);
    }

    public static Object newMutableTagListBuilder(Object baseTagList) {
        return RustedReflection.newInstance(MUTABLE_TAG_LIST_BUILDER_CLASSES, baseTagList);
    }

    public static boolean addAllTags(Object builder, Object tagList) {
        requireMutableTagListBuilder(builder);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(builder, new String[]{"addAll", "a"}, tagList));
    }

    public static boolean addTag(Object builder, Object unitTag) {
        requireMutableTagListBuilder(builder);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(builder, new String[]{"addTag", "a"}, unitTag));
    }

    public static boolean removeAllTags(Object builder, Object tagList) {
        requireMutableTagListBuilder(builder);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(builder, new String[]{"removeAll", "b"}, tagList));
    }

    public static Object toTagList(Object builder) {
        requireMutableTagListBuilder(builder);
        return RustedReflection.invokeInstance(builder, new String[]{"toTagList", "a"});
    }

    public static List<Object> mutableTags(Object builder) {
        requireMutableTagListBuilder(builder);
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(
                RustedReflection.getFieldValue(builder, new String[]{"mutableTags", "a"})));
    }

    public static Object newResourceShortageCollector() {
        return RustedReflection.newInstance(RESOURCE_SHORTAGE_COLLECTOR_CLASSES);
    }

    public static void addMissingResourceType(Object collector, Object resourceType) {
        requireResourceShortageCollector(collector);
        RustedReflection.invokeInstance(collector, new String[]{"addMissingResourceType", "a"}, resourceType);
    }

    public static void collectMissingFromStoredResources(Object collector, Object storedResourceSet,
                                                         Object unit, double multiplier) {
        requireResourceShortageCollector(collector);
        RustedReflection.invokeInstance(collector,
                new String[]{"collectMissingFromStoredResources", "a"},
                storedResourceSet, unit, Double.valueOf(multiplier));
    }

    public static void collectMissingFromPrice(Object collector, Object resourceAmount,
                                               Object unit, double multiplier) {
        requireResourceShortageCollector(collector);
        RustedReflection.invokeInstance(collector,
                new String[]{"collectMissingFromPrice", "a"},
                resourceAmount, unit, Double.valueOf(multiplier));
    }

    public static boolean containsAnyStoredResource(Object collector, Object storedResourceSet) {
        requireResourceShortageCollector(collector);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(collector,
                new String[]{"containsAnyStoredResource", "a"},
                storedResourceSet));
    }

    public static boolean containsAnyPriceResource(Object collector, Object resourceAmount) {
        requireResourceShortageCollector(collector);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(collector,
                new String[]{"containsAnyPriceResource", "a"},
                resourceAmount));
    }

    public static void clearResourceShortageCollector(Object collector) {
        requireResourceShortageCollector(collector);
        RustedReflection.invokeInstance(collector, new String[]{"clear", "a"});
    }

    public static List<Object> missingResourceTypes(Object collector) {
        requireResourceShortageCollector(collector);
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(
                RustedReflection.getFieldValue(collector, new String[]{"missingResourceTypes", "a"})));
    }

    public static String buildRequiredUnitMismatchReport(Object report) {
        requireRequiredUnitMismatchReport(report);
        Object value = RustedReflection.invokeInstance(report, new String[]{"buildHumanReadableReport", "a"});
        return value != null ? value.toString() : null;
    }

    public static Map<String, Object> describeRequiredUnitMismatchReport(Object report) {
        requireRequiredUnitMismatchReport(report);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("modId", RustedReflection.getStringField(report, new String[]{"modId", "a"}));
        result.put("unitId", RustedReflection.getStringField(report, new String[]{"unitId", "b"}));
        result.put("serverChecksum", Integer.valueOf(RustedReflection.getIntField(report,
                new String[]{"serverChecksum", "c"})));
        result.put("clientChecksum", Integer.valueOf(RustedReflection.getIntField(report,
                new String[]{"clientChecksum", "d"})));
        result.put("serverDebugSource", RustedReflection.getStringField(report,
                new String[]{"serverDebugSource", "e"}));
        result.put("localMismatchedMetadata", RustedReflection.getFieldValue(report,
                new String[]{"localMismatchedMetadata", "f"}));
        result.put("humanReadableReport", buildRequiredUnitMismatchReport(report));
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeModUnitAvailabilityCounts(Object counts) {
        requireModUnitAvailabilityCounts(counts);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("modId", RustedReflection.getStringField(counts, new String[]{"modId", "a"}));
        result.put("matchedUnitCount", Integer.valueOf(RustedReflection.getIntField(counts,
                new String[]{"matchedUnitCount", "b"})));
        result.put("missingUnitCount", Integer.valueOf(RustedReflection.getIntField(counts,
                new String[]{"missingUnitCount", "c"})));
        result.put("checksumMismatchCount", Integer.valueOf(RustedReflection.getIntField(counts,
                new String[]{"checksumMismatchCount", "d"})));
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeModInfo(Object modInfo) {
        requireModInfo(modInfo);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("displayName", getModDisplayName(modInfo));
        result.put("shortDisplayName25", invokeString(modInfo, new String[]{"getShortDisplayName25", "b"}));
        result.put("shortDisplayName40", invokeString(modInfo, new String[]{"getShortDisplayName40", "c"}));
        result.put("statusText", getModStatusText(modInfo));
        result.put("enabledAndErrorFree", Boolean.valueOf(isModEnabledAndErrorFree(modInfo)));
        result.put("folderName", RustedReflection.getStringField(modInfo, new String[]{"folderName", "d"}));
        result.put("title", RustedReflection.getStringField(modInfo, new String[]{"title", "s"}));
        result.put("fallbackTitle", RustedReflection.getStringField(modInfo, new String[]{"fallbackTitle", "t"}));
        result.put("description", RustedReflection.getStringField(modInfo, new String[]{"description", "u"}));
        result.put("sourcePath", RustedReflection.getStringField(modInfo, new String[]{"sourcePath", "q"}));
        result.put("archiveOrRawPath", RustedReflection.getStringField(modInfo, new String[]{"archiveOrRawPath", "p"}));
        result.put("modInfoPath", RustedReflection.getStringField(modInfo, new String[]{"modInfoPath", "o"}));
        result.put("unpackedStoragePath", RustedReflection.getStringField(modInfo, new String[]{"unpackedStoragePath", "n"}));
        result.put("normalizedSourcePath", getModNormalizedSourcePath(modInfo));
        result.put("normalizedArchiveOrRawPath", invokeString(modInfo, new String[]{"getNormalizedArchiveOrRawPath", "h"}));
        result.put("absoluteSourcePath", invokeString(modInfo, new String[]{"getAbsoluteSourcePath", "i"}));
        result.put("canonicalSourcePath", invokeString(modInfo, new String[]{"getCanonicalSourcePath", "k"}));
        result.put("loadError", RustedReflection.getStringField(modInfo, new String[]{"loadError", "R"}));
        result.put("loadWarning", RustedReflection.getStringField(modInfo, new String[]{"loadWarning", "S"}));
        result.put("loadWarningExtra", RustedReflection.getStringField(modInfo, new String[]{"loadWarningExtra", "T"}));
        return Collections.unmodifiableMap(result);
    }

    public static String getModDisplayName(Object modInfo) {
        requireModInfo(modInfo);
        return invokeString(modInfo, new String[]{"getDisplayName", "a"});
    }

    public static String getModStatusText(Object modInfo) {
        requireModInfo(modInfo);
        return invokeString(modInfo, new String[]{"getStatusText", "l"});
    }

    public static String getModNormalizedSourcePath(Object modInfo) {
        requireModInfo(modInfo);
        return invokeString(modInfo, new String[]{"getNormalizedSourcePath", "g"});
    }

    public static boolean isModEnabledAndErrorFree(Object modInfo) {
        requireModInfo(modInfo);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(modInfo,
                new String[]{"isEnabledAndErrorFree", "m"}));
    }

    public static void addModLoadWarning(Object modInfo, String warning) {
        requireModInfo(modInfo);
        RustedReflection.invokeInstance(modInfo, new String[]{"addLoadWarning", "b"}, warning);
    }

    public static String findModFileCaseInsensitive(Object modInfo, String relativePath, int recursionLimit) {
        requireModInfo(modInfo);
        Object value = RustedReflection.invokeInstance(modInfo, new String[]{"findFileCaseInsensitive", "a"},
                relativePath, Integer.valueOf(recursionLimit));
        return value != null ? value.toString() : null;
    }

    private static String invokeString(Object owner, String[] methodNames) {
        Object value = RustedReflection.invokeInstance(owner, methodNames);
        return value != null ? value.toString() : null;
    }

    private static List<Object> enumValues(String[] classNames) {
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(
                RustedReflection.invokeStatic(classNames, new String[]{"values"})));
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

    private static void requireTurretTemplate(Object turretTemplate) {
        requireAny(turretTemplate, TURRET_TEMPLATE_CLASSES, "TurretTemplate");
    }

    private static void requireCustomProjectileTemplate(Object projectileTemplate) {
        requireAny(projectileTemplate, CUSTOM_PROJECTILE_TEMPLATE_CLASSES, "CustomProjectileTemplate");
    }

    private static void requireEffectTemplate(Object effectTemplate) {
        requireAny(effectTemplate, EFFECT_TEMPLATE_CLASSES, "EffectTemplate");
    }

    private static void requireEffectList(Object effectList) {
        requireAny(effectList, EFFECT_LIST_CLASSES, "EffectList");
    }

    private static void requireMutableTagListBuilder(Object builder) {
        requireAny(builder, MUTABLE_TAG_LIST_BUILDER_CLASSES, "MutableTagListBuilder");
    }

    private static void requireResourceShortageCollector(Object collector) {
        requireAny(collector, RESOURCE_SHORTAGE_COLLECTOR_CLASSES, "ResourceShortageCollector");
    }

    private static void requireRequiredUnitMismatchReport(Object report) {
        requireAny(report, REQUIRED_UNIT_MISMATCH_REPORT_CLASSES, "RequiredUnitMismatchReport");
    }

    private static void requireModUnitAvailabilityCounts(Object counts) {
        requireAny(counts, MOD_UNIT_AVAILABILITY_COUNTS_CLASSES, "ModUnitAvailabilityCounts");
    }

    private static void requireModInfo(Object modInfo) {
        requireAny(modInfo, MOD_INFO_CLASSES, "ModInfo");
    }

    private static void requireAny(Object value, String[] classNames, String label) {
        if (value == null) {
            throw new IllegalArgumentException(label + " must not be null");
        }
        if (!RustedReflection.isAnyClass(value.getClass(), classNames)) {
            throw new IllegalArgumentException("Expected " + label + ", got " + value.getClass().getName());
        }
    }

    private static void requireText(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " must not be empty");
        }
    }
}
