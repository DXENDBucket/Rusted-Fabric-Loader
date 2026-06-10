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
