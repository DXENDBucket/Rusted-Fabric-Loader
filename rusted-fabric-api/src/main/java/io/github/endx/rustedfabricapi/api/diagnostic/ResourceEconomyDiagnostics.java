package io.github.endx.rustedfabricapi.api.diagnostic;

import io.github.endx.rustedfabricapi.api.util.RustedReflection;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ResourceEconomyDiagnostics {
    private static final String[] RESOURCE_TYPE_CLASSES = {
            "rustedwarfare.custom.resource.ResourceType",
            "com.corrodinggames.rts.game.units.custom.e.a"
    };
    private static final String[] RESOURCE_AMOUNT_CLASSES = {
            "rustedwarfare.custom.resource.ResourceAmount",
            "com.corrodinggames.rts.game.units.custom.d.b"
    };
    private static final String[] STORED_RESOURCE_SET_CLASSES = {
            "rustedwarfare.custom.resource.StoredResourceSet",
            "com.corrodinggames.rts.game.units.custom.e.f"
    };
    private static final String[] STORED_RESOURCE_ENTRY_CLASSES = {
            "rustedwarfare.custom.resource.StoredResourceEntry",
            "com.corrodinggames.rts.game.units.custom.e.e"
    };
    private static final String[] RESOURCE_DIGIT_GROUPING_CLASSES = {
            "rustedwarfare.custom.resource.ResourceDigitGrouping",
            "com.corrodinggames.rts.game.units.custom.e.b"
    };
    private static final String[] TEAM_CLASSES = {
            "rustedwarfare.game.Team",
            "com.corrodinggames.rts.game.n"
    };
    private static final String[] UNIT_CLASSES = {
            "rustedwarfare.unit.Unit",
            "com.corrodinggames.rts.game.units.am"
    };
    private static final String[] ORDERABLE_UNIT_CLASSES = {
            "rustedwarfare.unit.OrderableUnit",
            "com.corrodinggames.rts.game.units.y"
    };
    private static final String[] CUSTOM_TAG_LIST_CLASSES = {
            "rustedwarfare.custom.CustomTagList",
            "com.corrodinggames.rts.game.units.custom.h"
    };

    private ResourceEconomyDiagnostics() {
    }

    public static Map<String, Object> describeResourceType(Object resourceType) {
        requireResourceType(resourceType);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putField(result, resourceType, "internalName", new String[]{"internalName", "b"});
        putField(result, resourceType, "displayName", new String[]{"displayName", "c"});
        putField(result, resourceType, "displayNameShort", new String[]{"displayNameShort", "d"});
        putBooleanField(result, resourceType, "builtIn", new String[]{"builtIn", "a"});
        putBooleanField(result, resourceType, "globalResource", new String[]{"globalResource", "t"});
        putBooleanField(result, resourceType, "builtInResource", new String[]{"builtInResource", "u"});
        putBooleanField(result, resourceType, "hidden", new String[]{"hidden", "z"});
        putBooleanField(result, resourceType, "includeInStats", new String[]{"includeInStats", "w"});
        putFloatField(result, resourceType, "priority", new String[]{"priority", "s"});
        putFloatField(result, resourceType, "valueInStats", new String[]{"valueInStats", "x"});
        putField(result, resourceType, "displayColor", new String[]{"displayColor", "m"});
        putField(result, resourceType, "displayDigitGrouping", new String[]{"displayDigitGrouping", "q"});
        putField(result, resourceType, "equivalentGlobalResourceForAI",
                new String[]{"equivalentGlobalResourceForAI", "v"});
        putField(result, resourceType, "iconImage", new String[]{"iconImage", "y"});
        result.put("displayNameShortText", getDisplayNameShort(resourceType));
        result.put("displayPriority", Float.valueOf(getDisplayPriority(resourceType)));
        result.put("displayWhenZero", Boolean.valueOf(shouldDisplayWhenZero(resourceType)));
        result.put("global", Boolean.valueOf(isGlobalResource(resourceType)));
        result.put("builtInByMethod", Boolean.valueOf(isBuiltInResource(resourceType)));
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeResourceAmount(Object resourceAmount) {
        requireResourceAmount(resourceAmount);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putIntField(result, resourceAmount, "credits", new String[]{"credits", "b"});
        putFloatField(result, resourceAmount, "energy", new String[]{"energy", "c"});
        putFloatField(result, resourceAmount, "hp", new String[]{"hp", "d"});
        putFloatField(result, resourceAmount, "shield", new String[]{"shield", "e"});
        putIntField(result, resourceAmount, "ammo", new String[]{"ammo", "f"});
        putIntField(result, resourceAmount, "setFlagMask", new String[]{"setFlagMask", "g"});
        putIntField(result, resourceAmount, "unsetFlagMask", new String[]{"unsetFlagMask", "h"});
        putIntField(result, resourceAmount, "requiredFlagMask", new String[]{"requiredFlagMask", "i"});
        putIntField(result, resourceAmount, "missingFlagMask", new String[]{"missingFlagMask", "j"});
        putField(result, resourceAmount, "customResources", new String[]{"customResources", "k"});
        putIntField(result, resourceAmount, "displayColor", new String[]{"displayColor", "l"});
        result.put("empty", Boolean.valueOf(isResourceAmountEmpty(resourceAmount)));
        result.put("nonEmpty", Boolean.valueOf(isResourceAmountNonEmpty(resourceAmount)));
        result.put("estimatedValue", Integer.valueOf(getEstimatedValue(resourceAmount)));
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeStoredResourceSet(Object storedResourceSet) {
        requireStoredResourceSet(storedResourceSet);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("entries", storedResourceEntries(storedResourceSet));
        putBooleanField(result, storedResourceSet, "locked", new String[]{"locked", "c"});
        result.put("empty", Boolean.valueOf(isStoredResourceSetEmpty(storedResourceSet)));
        result.put("size", Integer.valueOf(storedResourceSetSize(storedResourceSet)));
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeStoredResourceEntry(Object storedResourceEntry) {
        requireStoredResourceEntry(storedResourceEntry);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("resourceType", RustedReflection.getFieldValue(storedResourceEntry,
                new String[]{"resourceType", "a"}));
        result.put("amount", Double.valueOf(getStoredResourceEntryAmount(storedResourceEntry)));
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeTeamResourceState(Object team) {
        requireTeam(team);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("teamTags", getTeamTags(team));
        result.put("globalResources", getGlobalResources(team));
        result.put("resourceShortageCollector", RustedReflection.getFieldValue(team,
                new String[]{"resourceShortageCollector", "am"}));
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeUnitResourceEconomy(Object unit) {
        requireUnit(unit);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        Object generationResources = getGenerationResourcesPerSecond(unit);
        Object globalGenerationResources = getGlobalGenerationResourcesPerSecond(unit);
        Object similarTags = getSimilarResourcesHaveTag(unit);
        result.put("className", unit.getClass().getName());
        result.put("creditGenerationPerSecond", Float.valueOf(getCreditGenerationPerSecond(unit)));
        result.put("generationResourcesPerSecond", generationResources);
        result.put("generationResourcesPerSecondDetails", describeStoredResourceSetOrRaw(generationResources));
        result.put("globalGenerationResourcesPerSecond", globalGenerationResources);
        result.put("globalGenerationResourcesPerSecondDetails",
                describeStoredResourceSetOrRaw(globalGenerationResources));
        result.put("resourceRate", Float.valueOf(getResourceRate(unit)));
        result.put("resourceMaxConcurrentReclaimingThis",
                Integer.valueOf(getResourceMaxConcurrentReclaimingThis(unit)));
        result.put("similarResourcesHaveTag", similarTags);
        result.put("similarResourcesHaveTagDetails", describeCustomTagListOrRaw(similarTags));
        if (isOrderableUnit(unit)) {
            Object queuedDelta = getQueuedActionResourceDelta(unit);
            result.put("queuedActionResourceDelta", queuedDelta);
            result.put("queuedActionResourceDeltaDetails", describeResourceAmountOrRaw(queuedDelta));
        } else {
            result.put("queuedActionResourceDelta", null);
            result.put("queuedActionResourceDeltaDetails", null);
        }
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeCustomTagList(Object tagList) {
        requireCustomTagList(tagList);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", tagList.getClass().getName());
        result.put("empty", Boolean.valueOf(isCustomTagListEmpty(tagList)));
        result.put("size", Integer.valueOf(customTagListSize(tagList)));
        result.put("asString", tagList.toString());
        return Collections.unmodifiableMap(result);
    }

    public static List<Object> allResourceTypes() {
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(
                RustedReflection.getStaticFieldValue(RESOURCE_TYPE_CLASSES, new String[]{"allResources", "A"})));
    }

    public static List<Object> activeDisplayResources() {
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(
                RustedReflection.getStaticFieldValue(RESOURCE_TYPE_CLASSES,
                        new String[]{"activeDisplayResources", "B"})));
    }

    public static List<Object> builtInResources() {
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(
                RustedReflection.getStaticFieldValue(RESOURCE_TYPE_CLASSES, new String[]{"builtInResources", "C"})));
    }

    public static Object creditsResourceType() {
        return RustedReflection.getStaticFieldValue(RESOURCE_TYPE_CLASSES, new String[]{"CREDITS", "D"});
    }

    public static Object energyResourceType() {
        return RustedReflection.getStaticFieldValue(RESOURCE_TYPE_CLASSES, new String[]{"ENERGY", "E"});
    }

    public static Object ammoResourceType() {
        return RustedReflection.getStaticFieldValue(RESOURCE_TYPE_CLASSES, new String[]{"AMMO", "F"});
    }

    public static Object hpResourceType() {
        return RustedReflection.getStaticFieldValue(RESOURCE_TYPE_CLASSES, new String[]{"HP", "G"});
    }

    public static Object shieldResourceType() {
        return RustedReflection.getStaticFieldValue(RESOURCE_TYPE_CLASSES, new String[]{"SHIELD", "H"});
    }

    public static List<Object> resourceDigitGroupings() {
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(
                RustedReflection.invokeStatic(RESOURCE_DIGIT_GROUPING_CLASSES, new String[]{"values"})));
    }

    public static Object resourceDigitGrouping(String name) {
        return RustedReflection.invokeStatic(RESOURCE_DIGIT_GROUPING_CLASSES, new String[]{"valueOf"}, name);
    }

    public static Object getBuiltInResourceTypeByName(String name) {
        return RustedReflection.invokeStatic(RESOURCE_TYPE_CLASSES,
                new String[]{"getBuiltInResourceTypeByName", "a"}, name);
    }

    public static Object getAnyResourceTypeByName(String name) {
        return RustedReflection.invokeStatic(RESOURCE_TYPE_CLASSES,
                new String[]{"getAnyResourceTypeByName", "b"}, name);
    }

    public static Object getOrCreateResourceType(String name, boolean globalResource, boolean builtInResource) {
        return RustedReflection.invokeStatic(RESOURCE_TYPE_CLASSES,
                new String[]{"getOrCreateResourceType", "a"},
                name, Boolean.valueOf(globalResource), Boolean.valueOf(builtInResource));
    }

    public static Object registerBuiltInResourceType(Object resourceType) {
        requireResourceType(resourceType);
        return RustedReflection.invokeStatic(RESOURCE_TYPE_CLASSES,
                new String[]{"registerBuiltInResourceType", "a"}, resourceType);
    }

    public static void refreshAllResourceDisplayDefinitions() {
        RustedReflection.invokeStatic(RESOURCE_TYPE_CLASSES,
                new String[]{"refreshAllResourceDisplayDefinitions", "e"});
    }

    public static void refreshDisplayDefinition(Object resourceType) {
        requireResourceType(resourceType);
        RustedReflection.invokeInstance(resourceType, new String[]{"refreshDisplayDefinition", "g"});
    }

    public static double getResourceAmount(Object resourceType, Object unit) {
        requireResourceType(resourceType);
        requireUnit(unit);
        Object value = RustedReflection.invokeInstance(resourceType, new String[]{"getAmount", "a"}, unit);
        return value instanceof Number ? ((Number) value).doubleValue() : 0.0D;
    }

    public static void setResourceAmount(Object resourceType, Object unit, double amount) {
        requireResourceType(resourceType);
        requireUnit(unit);
        RustedReflection.invokeInstance(resourceType, new String[]{"setAmount", "a"}, unit, Double.valueOf(amount));
    }

    public static void addResourceAmount(Object resourceType, Object unit, double amount) {
        requireResourceType(resourceType);
        requireUnit(unit);
        RustedReflection.invokeInstance(resourceType, new String[]{"addAmount", "b"}, unit, Double.valueOf(amount));
    }

    public static boolean shouldDisplayWhenZero(Object resourceType) {
        requireResourceType(resourceType);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(resourceType,
                new String[]{"shouldDisplayWhenZero", "a"}));
    }

    public static float getDisplayPriority(Object resourceType) {
        requireResourceType(resourceType);
        Object value = RustedReflection.invokeInstance(resourceType, new String[]{"getDisplayPriority", "b"});
        return value instanceof Number ? ((Number) value).floatValue() : 0.0F;
    }

    public static boolean isBuiltInResource(Object resourceType) {
        requireResourceType(resourceType);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(resourceType,
                new String[]{"isBuiltInResource", "c"}));
    }

    public static boolean isGlobalResource(Object resourceType) {
        requireResourceType(resourceType);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(resourceType,
                new String[]{"isGlobalResource", "d"}));
    }

    public static String getDisplayNameShort(Object resourceType) {
        requireResourceType(resourceType);
        Object value = RustedReflection.invokeInstance(resourceType, new String[]{"getDisplayNameShort", "j"});
        return value != null ? value.toString() : null;
    }

    public static String formatAmountWithDisplayName(Object resourceType, double amount, boolean signed) {
        requireResourceType(resourceType);
        Object value = RustedReflection.invokeInstance(resourceType,
                new String[]{"formatAmountWithDisplayName", "a"},
                Double.valueOf(amount), Boolean.valueOf(signed));
        return value != null ? value.toString() : null;
    }

    public static String getDisplayPrefix(Object resourceType, boolean inHud) {
        requireResourceType(resourceType);
        Object value = RustedReflection.invokeInstance(resourceType, new String[]{"getDisplayPrefix", "a"},
                Boolean.valueOf(inHud));
        return value != null ? value.toString() : null;
    }

    public static String getDisplayPostfix(Object resourceType, boolean inHud) {
        requireResourceType(resourceType);
        Object value = RustedReflection.invokeInstance(resourceType, new String[]{"getDisplayPostfix", "b"},
                Boolean.valueOf(inHud));
        return value != null ? value.toString() : null;
    }

    public static Object getIconImage(Object resourceType) {
        requireResourceType(resourceType);
        return RustedReflection.invokeInstance(resourceType, new String[]{"getIconImage", "k"});
    }

    public static float getCreditGenerationPerSecond(Object unit) {
        requireUnit(unit);
        Object value = RustedReflection.invokeInstance(unit, new String[]{"getCreditGenerationPerSecond", "cy"});
        return value instanceof Number ? ((Number) value).floatValue() : 0.0F;
    }

    public static Object getGenerationResourcesPerSecond(Object unit) {
        requireUnit(unit);
        return RustedReflection.invokeInstance(unit, new String[]{"getGenerationResourcesPerSecond", "cz"});
    }

    public static Object getGlobalGenerationResourcesPerSecond(Object unit) {
        requireUnit(unit);
        return RustedReflection.invokeInstance(unit, new String[]{"getGlobalGenerationResourcesPerSecond", "cA"});
    }

    public static float getResourceRate(Object unit) {
        requireUnit(unit);
        Object value = RustedReflection.invokeInstance(unit, new String[]{"getResourceRate", "g"});
        return value instanceof Number ? ((Number) value).floatValue() : 0.0F;
    }

    public static int getResourceMaxConcurrentReclaimingThis(Object unit) {
        requireUnit(unit);
        Object value = RustedReflection.invokeInstance(unit,
                new String[]{"getResourceMaxConcurrentReclaimingThis", "cQ"});
        return value instanceof Number ? ((Number) value).intValue() : Integer.MAX_VALUE;
    }

    public static Object getSimilarResourcesHaveTag(Object unit) {
        requireUnit(unit);
        return RustedReflection.invokeInstance(unit, new String[]{"getSimilarResourcesHaveTag", "cR"});
    }

    public static Object getQueuedActionResourceDelta(Object unit) {
        requireOrderableUnit(unit);
        return RustedReflection.invokeInstance(unit, new String[]{"getQueuedActionResourceDelta", "by"});
    }

    public static boolean isOrderableUnit(Object unit) {
        return unit != null && RustedReflection.isAnyClass(unit.getClass(), ORDERABLE_UNIT_CLASSES);
    }

    public static boolean isResourceAmountEmpty(Object resourceAmount) {
        requireResourceAmount(resourceAmount);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(resourceAmount, new String[]{"isEmpty", "c"}));
    }

    public static boolean isResourceAmountNonEmpty(Object resourceAmount) {
        requireResourceAmount(resourceAmount);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(resourceAmount, new String[]{"isNonEmpty", "d"}));
    }

    public static int getEstimatedValue(Object resourceAmount) {
        requireResourceAmount(resourceAmount);
        Object value = RustedReflection.invokeInstance(resourceAmount, new String[]{"getEstimatedValue", "b"});
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    public static int getCredits(Object resourceAmount) {
        requireResourceAmount(resourceAmount);
        Object value = RustedReflection.invokeInstance(resourceAmount, new String[]{"getCredits", "a"});
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    public static String formatResourceAmountList(Object resourceAmount,
                                                  boolean includeBuiltIns,
                                                  boolean includeCustom,
                                                  int color,
                                                  boolean includeZero) {
        requireResourceAmount(resourceAmount);
        Object value = RustedReflection.invokeInstance(resourceAmount,
                new String[]{"formatResourceAmountList", "a"},
                Boolean.valueOf(includeBuiltIns), Boolean.valueOf(includeCustom),
                Integer.valueOf(color), Boolean.valueOf(includeZero));
        return value != null ? value.toString() : null;
    }

    public static Object newStoredResourceSet() {
        return RustedReflection.newInstance(STORED_RESOURCE_SET_CLASSES);
    }

    public static List<Object> storedResourceEntries(Object storedResourceSet) {
        requireStoredResourceSet(storedResourceSet);
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(
                RustedReflection.getFieldValue(storedResourceSet, new String[]{"entries", "b"})));
    }

    public static double getStoredResourceAmount(Object storedResourceSet, Object resourceType) {
        requireStoredResourceSet(storedResourceSet);
        requireResourceType(resourceType);
        Object value = RustedReflection.invokeInstance(storedResourceSet, new String[]{"getAmount", "a"},
                resourceType);
        return value instanceof Number ? ((Number) value).doubleValue() : 0.0D;
    }

    public static double getStoredResourcePositiveAmount(Object storedResourceSet, Object resourceType) {
        requireStoredResourceSet(storedResourceSet);
        requireResourceType(resourceType);
        Object value = RustedReflection.invokeInstance(storedResourceSet, new String[]{"getPositiveAmount", "b"},
                resourceType);
        return value instanceof Number ? ((Number) value).doubleValue() : 0.0D;
    }

    public static void setStoredResourceAmount(Object storedResourceSet, Object resourceType, double amount) {
        requireStoredResourceSet(storedResourceSet);
        requireResourceType(resourceType);
        RustedReflection.invokeInstance(storedResourceSet, new String[]{"setAmount", "a"},
                resourceType, Double.valueOf(amount));
    }

    public static void addStoredResourceAmount(Object storedResourceSet, Object resourceType, double amount) {
        requireStoredResourceSet(storedResourceSet);
        requireResourceType(resourceType);
        RustedReflection.invokeInstance(storedResourceSet, new String[]{"addAmount", "b"},
                resourceType, Double.valueOf(amount));
    }

    public static void addStoredResourceAmountRaw(Object storedResourceSet, Object resourceType, double amount) {
        requireStoredResourceSet(storedResourceSet);
        requireResourceType(resourceType);
        RustedReflection.invokeInstance(storedResourceSet, new String[]{"addAmountRaw", "c"},
                resourceType, Double.valueOf(amount));
    }

    public static void subtractStoredResourceAmount(Object storedResourceSet, Object resourceType, double amount) {
        requireStoredResourceSet(storedResourceSet);
        requireResourceType(resourceType);
        RustedReflection.invokeInstance(storedResourceSet, new String[]{"subtractAmount", "d"},
                resourceType, Double.valueOf(amount));
    }

    public static void copyStoredResourceSetFrom(Object target, Object source) {
        requireStoredResourceSet(target);
        requireStoredResourceSet(source);
        RustedReflection.invokeInstance(target, new String[]{"copyFrom", "a"}, source);
    }

    public static void addAllStoredResources(Object target, Object source) {
        requireStoredResourceSet(target);
        requireStoredResourceSet(source);
        RustedReflection.invokeInstance(target, new String[]{"addAll", "b"}, source);
    }

    public static void subtractAllStoredResources(Object target, Object source) {
        requireStoredResourceSet(target);
        requireStoredResourceSet(source);
        RustedReflection.invokeInstance(target, new String[]{"subtractAll", "c"}, source);
    }

    public static void clearStoredResourceSet(Object storedResourceSet) {
        requireStoredResourceSet(storedResourceSet);
        RustedReflection.invokeInstance(storedResourceSet, new String[]{"clear", "b"});
    }

    public static Object copyStoredResourceSet(Object storedResourceSet) {
        requireStoredResourceSet(storedResourceSet);
        return RustedReflection.invokeInstance(storedResourceSet, new String[]{"copy", "d"}, storedResourceSet);
    }

    public static Object lockStoredResourceSet(Object storedResourceSet) {
        requireStoredResourceSet(storedResourceSet);
        return RustedReflection.invokeInstance(storedResourceSet, new String[]{"lock", "a"});
    }

    public static boolean isStoredResourceSetEmpty(Object storedResourceSet) {
        requireStoredResourceSet(storedResourceSet);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(storedResourceSet, new String[]{"isEmpty", "c"}));
    }

    public static int storedResourceSetSize(Object storedResourceSet) {
        requireStoredResourceSet(storedResourceSet);
        Object value = RustedReflection.invokeInstance(storedResourceSet, new String[]{"size", "d"});
        return value instanceof Number ? ((Number) value).intValue() : storedResourceEntries(storedResourceSet).size();
    }

    public static String formatStoredResourceSet(Object storedResourceSet,
                                                 boolean includeZero,
                                                 boolean includeIcons,
                                                 int color,
                                                 boolean includeBuiltIns,
                                                 boolean includeCustom) {
        requireStoredResourceSet(storedResourceSet);
        Object value = RustedReflection.invokeInstance(storedResourceSet,
                new String[]{"formatResourceSet", "a"},
                Boolean.valueOf(includeZero), Boolean.valueOf(includeIcons),
                Integer.valueOf(color), Boolean.valueOf(includeBuiltIns), Boolean.valueOf(includeCustom));
        return value != null ? value.toString() : null;
    }

    public static Object getMissingResourcesForUnit(Object storedResourceSet, Object unit) {
        requireStoredResourceSet(storedResourceSet);
        requireUnit(unit);
        return RustedReflection.invokeInstance(storedResourceSet,
                new String[]{"getMissingResourcesForUnit", "a"}, unit);
    }

    public static String formatMissingResourceNames(Object storedResourceSet,
                                                    Object unit,
                                                    String delimiter,
                                                    int maxItems,
                                                    boolean includeDisplayNames) {
        requireStoredResourceSet(storedResourceSet);
        requireUnit(unit);
        Object value = RustedReflection.invokeInstance(storedResourceSet,
                new String[]{"formatMissingResourceNames", "a"},
                unit, delimiter, Integer.valueOf(maxItems), Boolean.valueOf(includeDisplayNames));
        return value != null ? value.toString() : null;
    }

    public static void ensureResourceEntry(Object storedResourceSet, Object resourceType) {
        requireStoredResourceSet(storedResourceSet);
        requireResourceType(resourceType);
        RustedReflection.invokeInstance(storedResourceSet, new String[]{"ensureResourceEntry", "c"}, resourceType);
    }

    public static void sortStoredResourceSetByDisplayPriority(Object storedResourceSet) {
        requireStoredResourceSet(storedResourceSet);
        RustedReflection.invokeInstance(storedResourceSet, new String[]{"sortByDisplayPriority", "e"});
    }

    public static double getStoredResourceEntryAmount(Object storedResourceEntry) {
        requireStoredResourceEntry(storedResourceEntry);
        Object value = RustedReflection.getFieldValue(storedResourceEntry, new String[]{"amount", "b"});
        return value instanceof Number ? ((Number) value).doubleValue() : 0.0D;
    }

    public static Object getStoredResourceEntryType(Object storedResourceEntry) {
        requireStoredResourceEntry(storedResourceEntry);
        return RustedReflection.getFieldValue(storedResourceEntry, new String[]{"resourceType", "a"});
    }

    public static Object getTeamTags(Object team) {
        requireTeam(team);
        return RustedReflection.invokeInstance(team, new String[]{"getTeamTags", "U"});
    }

    public static void setTeamTags(Object team, Object tagList) {
        requireTeam(team);
        RustedReflection.invokeInstance(team, new String[]{"setTeamTags", "a"}, tagList);
    }

    public static void addTeamTags(Object team, Object tagList) {
        requireTeam(team);
        RustedReflection.invokeInstance(team, new String[]{"addTeamTags", "b"}, tagList);
    }

    public static void removeTeamTags(Object team, Object tagList) {
        requireTeam(team);
        RustedReflection.invokeInstance(team, new String[]{"removeTeamTags", "c"}, tagList);
    }

    public static Object getGlobalResources(Object team) {
        requireTeam(team);
        return RustedReflection.invokeInstance(team, new String[]{"getGlobalResources", "V"});
    }

    public static double getGlobalResourceAmount(Object team, Object resourceType) {
        requireTeam(team);
        requireResourceType(resourceType);
        Object value = RustedReflection.invokeInstance(team, new String[]{"getGlobalResourceAmount", "c"},
                resourceType);
        return value instanceof Number ? ((Number) value).doubleValue() : 0.0D;
    }

    public static boolean isCustomTagList(Object tagList) {
        return tagList != null && RustedReflection.isAnyClass(tagList.getClass(), CUSTOM_TAG_LIST_CLASSES);
    }

    public static boolean isCustomTagListEmpty(Object tagList) {
        requireCustomTagList(tagList);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(tagList, new String[]{"isEmpty", "a"}));
    }

    public static int customTagListSize(Object tagList) {
        requireCustomTagList(tagList);
        Object value = RustedReflection.invokeInstance(tagList, new String[]{"size", "b"});
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    private static Object describeResourceAmountOrRaw(Object resourceAmount) {
        if (resourceAmount == null || !RustedReflection.isAnyClass(resourceAmount.getClass(), RESOURCE_AMOUNT_CLASSES)) {
            return resourceAmount;
        }
        return describeResourceAmount(resourceAmount);
    }

    private static Object describeStoredResourceSetOrRaw(Object storedResourceSet) {
        if (storedResourceSet == null
                || !RustedReflection.isAnyClass(storedResourceSet.getClass(), STORED_RESOURCE_SET_CLASSES)) {
            return storedResourceSet;
        }
        return describeStoredResourceSet(storedResourceSet);
    }

    private static Object describeCustomTagListOrRaw(Object tagList) {
        if (tagList == null || !isCustomTagList(tagList)) {
            return tagList;
        }
        return describeCustomTagList(tagList);
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

    private static void requireResourceType(Object resourceType) {
        requireAny(resourceType, RESOURCE_TYPE_CLASSES, "ResourceType");
    }

    private static void requireResourceAmount(Object resourceAmount) {
        requireAny(resourceAmount, RESOURCE_AMOUNT_CLASSES, "ResourceAmount");
    }

    private static void requireStoredResourceSet(Object storedResourceSet) {
        requireAny(storedResourceSet, STORED_RESOURCE_SET_CLASSES, "StoredResourceSet");
    }

    private static void requireStoredResourceEntry(Object storedResourceEntry) {
        requireAny(storedResourceEntry, STORED_RESOURCE_ENTRY_CLASSES, "StoredResourceEntry");
    }

    private static void requireTeam(Object team) {
        requireAny(team, TEAM_CLASSES, "Team");
    }

    private static void requireUnit(Object unit) {
        requireAny(unit, UNIT_CLASSES, "Unit");
    }

    private static void requireOrderableUnit(Object unit) {
        requireAny(unit, ORDERABLE_UNIT_CLASSES, "OrderableUnit");
    }

    private static void requireCustomTagList(Object tagList) {
        requireAny(tagList, CUSTOM_TAG_LIST_CLASSES, "CustomTagList");
    }

    private static void requireAny(Object value, String[] classNames, String label) {
        if (value == null) {
            throw new IllegalArgumentException(label + " must not be null");
        }
        if (!RustedReflection.isAnyClass(value.getClass(), classNames)) {
            throw new IllegalArgumentException("Expected " + label + ", got " + value.getClass().getName());
        }
    }
}
