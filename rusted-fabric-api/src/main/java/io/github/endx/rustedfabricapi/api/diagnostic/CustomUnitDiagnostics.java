package io.github.endx.rustedfabricapi.api.diagnostic;

import io.github.endx.rustedfabricapi.api.util.RustedReflection;

import java.util.ArrayList;
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
    private static final String[] CUSTOM_UNIT_CLASSES = {
            "rustedwarfare.custom.CustomUnit",
            "com.corrodinggames.rts.game.units.custom.j"
    };
    private static final String[] UNIT_CLASSES = {
            "rustedwarfare.unit.Unit",
            "com.corrodinggames.rts.game.units.am"
    };
    private static final String[] UNIT_ACTION_CLASSES = {
            "rustedwarfare.unit.action.UnitAction",
            "com.corrodinggames.rts.game.units.a.s"
    };
    private static final String[] UNIT_ACTION_ID_CLASSES = {
            "rustedwarfare.unit.action.UnitActionId",
            "com.corrodinggames.rts.game.units.a.c"
    };
    private static final String[] UNIT_TYPE_CLASSES = {
            "rustedwarfare.unit.UnitType",
            "com.corrodinggames.rts.game.units.as"
    };
    private static final String[] CUSTOM_UNIT_METADATA_CLASSES = {
            "rustedwarfare.custom.CustomUnitMetadata",
            "com.corrodinggames.rts.game.units.custom.l"
    };
    private static final String[] LEG_OR_ARM_TEMPLATE_CLASSES = {
            "rustedwarfare.custom.LegOrArmTemplate",
            "com.corrodinggames.rts.game.units.custom.ba"
    };
    private static final String[] LEG_RUNTIME_STATE_CLASSES = {
            "rustedwarfare.custom.runtime.LegRuntimeState",
            "com.corrodinggames.rts.game.units.custom.b.i"
    };
    private static final String[] LOCALIZED_STRING_CLASSES = {
            "rustedwarfare.custom.LocalizedString",
            "com.corrodinggames.rts.game.units.custom.aj"
    };
    private static final String[] LOCALIZED_STRING_DATA_CLASSES = {
            "rustedwarfare.custom.LocalizedStringData",
            "com.corrodinggames.rts.game.units.custom.bb"
    };
    private static final String[] LOCALIZED_STRING_ENTRY_CLASSES = {
            "rustedwarfare.custom.LocalizedStringEntry",
            "com.corrodinggames.rts.game.units.custom.bc"
    };
    private static final String[] ATTACHMENT_SLOT_CLASSES = {
            "rustedwarfare.custom.attachment.AttachmentSlot",
            "com.corrodinggames.rts.game.units.custom.b.n"
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
    private static final String[] MOD_MANAGER_CLASSES = {
            "rustedwarfare.mod.ModManager",
            "com.corrodinggames.rts.gameFramework.i.a"
    };
    private static final String[] CUSTOM_UNIT_LOADER_CLASSES = {
            "rustedwarfare.custom.CustomUnitLoader",
            "com.corrodinggames.rts.game.units.custom.ag"
    };
    private static final String[] AUTO_TRIGGER_EVENT_SPEC_CLASSES = {
            "rustedwarfare.custom.event.AutoTriggerEventSpec",
            "com.corrodinggames.rts.game.units.custom.ai"
    };

    private CustomUnitDiagnostics() {
    }

    public static Map<String, Object> describeCustomUnitMetadata(Object metadata) {
        requireCustomUnitMetadata(metadata);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putFloatField(result, metadata, "nanoUnbuildSpeed", new String[]{"nanoUnbuildSpeed", "be"});
        putField(result, metadata, "buildPrice", new String[]{"buildPrice", "ch"});
        putField(result, metadata, "reclaimPrice", new String[]{"reclaimPrice", "ci"});
        putField(result, metadata, "streamingCost", new String[]{"streamingCost", "cj"});
        putField(result, metadata, "generationResources", new String[]{"generationResources", "co"});
        putIntField(result, metadata, "generationDelay", new String[]{"generationDelay", "cr"});
        putBooleanField(result, metadata, "hasPeriodicResourceGeneration",
                new String[]{"hasPeriodicResourceGeneration", "cn"});
        putField(result, metadata, "generationResourcesPerSecond",
                new String[]{"generationResourcesPerSecond", "cp"});
        putField(result, metadata, "globalGenerationResourcesPerSecond",
                new String[]{"globalGenerationResourcesPerSecond", "cq"});
        putFloatField(result, metadata, "generationRateScale", new String[]{"generationRateScale", "cs"});
        putField(result, metadata, "generationActiveLogic", new String[]{"generationActiveLogic", "cx"});
        putField(result, metadata, "borrowResourcesWhileAlive", new String[]{"borrowResourcesWhileAlive", "cv"});
        putField(result, metadata, "borrowResourcesWhileBuilt", new String[]{"borrowResourcesWhileBuilt", "cw"});
        putField(result, metadata, "energyDisplayName", new String[]{"energyDisplayName", "cT"});
        putField(result, metadata, "placementRules", new String[]{"placementRules", "ff"});
        result.put("placementRulesFromGetter", invokeOrNull(metadata, new String[]{"getPlacementRules", "q"}));
        putField(result, metadata, "showActionsWithMixedSelectionIfOtherUnitsHaveTag",
                new String[]{"showActionsWithMixedSelectionIfOtherUnitsHaveTag", "fO"});
        putField(result, metadata, "canOnlyBeAttackedByUnitsWithTags",
                new String[]{"canOnlyBeAttackedByUnitsWithTags", "aS"});
        putField(result, metadata, "unitsSpawnedOnDeath", new String[]{"unitsSpawnedOnDeath", "bC"});
        putBooleanField(result, metadata, "hasLaserDefenceTurrets",
                new String[]{"hasLaserDefenceTurrets", "bE"});
        putBooleanField(result, metadata, "hasProjectileInterceptorTurrets",
                new String[]{"hasProjectileInterceptorTurrets", "bF"});
        putBooleanField(result, metadata, "hasTurretLimitingAngles",
                new String[]{"hasTurretLimitingAngles", "bG"});
        putBooleanField(result, metadata, "hasMovementEffect", new String[]{"hasMovementEffect", "bL"});
        putBooleanField(result, metadata, "hasRepairEffect", new String[]{"hasRepairEffect", "bS"});
        putBooleanField(result, metadata, "hasReclaimEffect", new String[]{"hasReclaimEffect", "bW"});
        putField(result, metadata, "legOrArmTemplates", new String[]{"legOrArmTemplates", "ax"});
        result.put("legOrArmTemplatesSize", Integer.valueOf(legOrArmTemplates(metadata).size()));
        putBooleanField(result, metadata, "hasDrawOverBodyLegs",
                new String[]{"hasDrawOverBodyLegs", "ay"});
        putBooleanField(result, metadata, "hasDrawUnderAllUnitsLegs",
                new String[]{"hasDrawUnderAllUnitsLegs", "az"});
        putField(result, metadata, "movementType", new String[]{"movementType", "fg"});
        putField(result, metadata, "pathingMovementType", new String[]{"pathingMovementType", "fh"});
        putBooleanField(result, metadata, "isBuilder", new String[]{"isBuilder", "fp"});
        putBooleanField(result, metadata, "useAsBuilder", new String[]{"useAsBuilder", "fq"});
        putBooleanField(result, metadata, "useAsHarvester", new String[]{"useAsHarvester", "fr"});
        putBooleanField(result, metadata, "useAsAttacker", new String[]{"useAsAttacker", "fs"});
        putBooleanField(result, metadata, "useAsTransport", new String[]{"useAsTransport", "ft"});
        putBooleanField(result, metadata, "hasBuildActions", new String[]{"hasBuildActions", "fu"});
        putField(result, metadata, "aiTags", new String[]{"aiTags", "fv"});
        putField(result, metadata, "onlyUseAsHarvesterIfBaseHasUnitTagged",
                new String[]{"onlyUseAsHarvesterIfBaseHasUnitTagged", "fH"});
        putBooleanField(result, metadata, "hasAiHighPriorityLogic", new String[]{"hasAiHighPriorityLogic", "fJ"});
        putFloatField(result, metadata, "moveAccelerationSpeed", new String[]{"moveAccelerationSpeed", "dN"});
        putFloatField(result, metadata, "moveDecelerationSpeed", new String[]{"moveDecelerationSpeed", "dO"});
        putBooleanField(result, metadata, "ignoreMoveOrders", new String[]{"ignoreMoveOrders", "dP"});
        putFloatField(result, metadata, "inverseMoveYAxisScaling",
                new String[]{"inverseMoveYAxisScaling", "ek"});
        putBooleanField(result, metadata, "landOnGround", new String[]{"landOnGround", "dQ"});
        putBooleanField(result, metadata, "landOnGroundOnlyIdle", new String[]{"landOnGroundOnlyIdle", "dR"});
        putFloatField(result, metadata, "turretSize", new String[]{"turretSize", "ea"});
        putField(result, metadata, "attackMovement", new String[]{"attackMovement", "ec"});
        putIntField(result, metadata, "mainTurretIndex", new String[]{"mainTurretIndex", "dG"});
        putField(result, metadata, "canAttackFlyingUnits", new String[]{"canAttackFlyingUnits", "eq"});
        putField(result, metadata, "canAttackLandUnits", new String[]{"canAttackLandUnits", "er"});
        putField(result, metadata, "canAttackUnderwaterUnits",
                new String[]{"canAttackUnderwaterUnits", "es"});
        putField(result, metadata, "canAttackNotTouchingWaterUnits",
                new String[]{"canAttackNotTouchingWaterUnits", "et"});
        putField(result, metadata, "canOnlyAttackUnitsWithTags",
                new String[]{"canOnlyAttackUnitsWithTags", "ev"});
        putField(result, metadata, "canOnlyAttackUnitsWithoutTags",
                new String[]{"canOnlyAttackUnitsWithoutTags", "ew"});
        putBooleanField(result, metadata, "hasAttackTagFilters", new String[]{"hasAttackTagFilters", "eu"});
        putBooleanField(result, metadata, "requiresTurretTagFilterTargetCheck",
                new String[]{"requiresTurretTagFilterTargetCheck", "ex"});
        putIntField(result, metadata, "reloadProgressTurretIndex",
                new String[]{"reloadProgressTurretIndex", "em"});
        putIntField(result, metadata, "warmupProgressTurretIndex",
                new String[]{"warmupProgressTurretIndex", "en"});
        putBooleanField(result, metadata, "hasAttachedTurretLinks",
                new String[]{"hasAttachedTurretLinks", "fU"});
        putField(result, metadata, "mainNanoTurret", new String[]{"mainNanoTurret", "fV"});
        putBooleanField(result, metadata, "hasChargeEffectImages",
                new String[]{"hasChargeEffectImages", "fP"});
        putBooleanField(result, metadata, "canReclaimResources", new String[]{"canReclaimResources", "fk"});
        putIntField(result, metadata, "canReclaimResourcesNextSearchRange",
                new String[]{"canReclaimResourcesNextSearchRange", "fm"});
        putField(result, metadata, "canReclaimResourcesOnlyWithTags",
                new String[]{"canReclaimResourcesOnlyWithTags", "fl"});
        putField(result, metadata, "canReclaimUnitsOnlyWithTags",
                new String[]{"canReclaimUnitsOnlyWithTags", "fn"});
        putField(result, metadata, "canRepairUnitsOnlyWithTags",
                new String[]{"canRepairUnitsOnlyWithTags", "fo"});
        putField(result, metadata, "similarResourcesHaveTag", new String[]{"similarResourcesHaveTag", "cH"});
        putBooleanField(result, metadata, "usesCreditResourcesField", new String[]{"usesCreditResources", "gr"});
        result.put("availableInDemo", Boolean.valueOf(metadataIsAvailableInDemo(metadata)));
        result.put("locked", Boolean.valueOf(metadataIsLocked(metadata)));
        result.put("building", Boolean.valueOf(metadataIsBuilding(metadata)));
        result.put("ignoredInUnitCapCalculation",
                Boolean.valueOf(metadataIsIgnoredInUnitCapCalculation(metadata)));
        result.put("builderFromGetter", Boolean.valueOf(metadataIsBuilder(metadata)));
        result.put("useAsBuilderFromGetter", Boolean.valueOf(metadataUseAsBuilder(metadata)));
        result.put("useAsHarvesterFromGetter", Boolean.valueOf(metadataUseAsHarvester(metadata)));
        result.put("placeOnlyOnResourcePool", Boolean.valueOf(metadataIsPlaceOnlyOnResourcePool(metadata)));
        result.put("usesCreditResources", Boolean.valueOf(metadataUsesCreditResources(metadata)));
        result.put("defaultAction", RustedReflection.invokeInstance(metadata,
                new String[]{"getDefaultAction", "d"}));
        putMethodCollection(result, metadata, "customUnitTypeIds", new String[]{"getCustomUnitTypeIds", "s"});
        result.put("tags", RustedReflection.invokeInstance(metadata, new String[]{"getTags", "x"}));
        return Collections.unmodifiableMap(result);
    }

    public static Object getPlacementRules(Object metadata) {
        requireCustomUnitMetadata(metadata);
        return RustedReflection.invokeInstance(metadata, new String[]{"getPlacementRules", "q"});
    }

    public static List<Object> customUnitTypeIds(Object metadata) {
        requireCustomUnitMetadata(metadata);
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(
                RustedReflection.invokeInstance(metadata, new String[]{"getCustomUnitTypeIds", "s"})));
    }

    public static Object getCustomUnitTags(Object metadata) {
        requireCustomUnitMetadata(metadata);
        return RustedReflection.invokeInstance(metadata, new String[]{"getTags", "x"});
    }

    public static boolean metadataIsAvailableInDemo(Object metadata) {
        requireCustomUnitMetadata(metadata);
        return invokeBooleanOrFalse(metadata, new String[]{"isAvailableInDemo", "C"});
    }

    public static boolean metadataIsLocked(Object metadata) {
        requireCustomUnitMetadata(metadata);
        return invokeBooleanOrFalse(metadata, new String[]{"isLocked", "w"});
    }

    public static boolean metadataIsBuilding(Object metadata) {
        requireCustomUnitMetadata(metadata);
        return invokeBooleanOrFalse(metadata, new String[]{"isBuilding", "j"});
    }

    public static boolean metadataIsIgnoredInUnitCapCalculation(Object metadata) {
        requireCustomUnitMetadata(metadata);
        return invokeBooleanOrFalse(metadata, new String[]{"isIgnoredInUnitCapCalculation", "k"});
    }

    public static boolean metadataIsBuilder(Object metadata) {
        requireCustomUnitMetadata(metadata);
        return invokeBooleanOrFalse(metadata, new String[]{"isBuilder", "l"});
    }

    public static boolean metadataUseAsBuilder(Object metadata) {
        requireCustomUnitMetadata(metadata);
        return invokeBooleanOrFalse(metadata, new String[]{"useAsBuilder", "m"});
    }

    public static boolean metadataUseAsHarvester(Object metadata) {
        requireCustomUnitMetadata(metadata);
        return invokeBooleanOrFalse(metadata, new String[]{"useAsHarvester", "n"});
    }

    public static boolean metadataIsPlaceOnlyOnResourcePool(Object metadata) {
        requireCustomUnitMetadata(metadata);
        return invokeBooleanOrFalse(metadata, new String[]{"isPlaceOnlyOnResourcePool", "p"});
    }

    public static boolean metadataUsesCreditResources(Object metadata) {
        requireCustomUnitMetadata(metadata);
        return invokeBooleanOrFalse(metadata, new String[]{"usesCreditResources", "y"});
    }

    public static List<Object> legOrArmTemplates(Object metadata) {
        requireCustomUnitMetadata(metadata);
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(
                RustedReflection.getFieldValue(metadata, new String[]{"legOrArmTemplates", "ax"})));
    }

    public static Map<String, Object> describeLegOrArmTemplate(Object template) {
        requireLegOrArmTemplate(template);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putIntField(result, template, "index", new String[]{"index", "a"});
        putField(result, template, "name", new String[]{"name", "b"});
        putBooleanField(result, template, "isLeg", new String[]{"isLeg", "c"});
        putFloatField(result, template, "x", new String[]{"x", "d"});
        putFloatField(result, template, "y", new String[]{"y", "e"});
        putFloatField(result, template, "liftingHeightOffset", new String[]{"liftingHeightOffset", "f"});
        putFloatField(result, template, "targetHeight", new String[]{"targetHeight", "g"});
        putBooleanField(result, template, "targetHeightRelative", new String[]{"targetHeightRelative", "h"});
        putFloatField(result, template, "endDirOffset", new String[]{"endDirOffset", "i"});
        putFloatField(result, template, "attachX", new String[]{"attachX", "j"});
        putFloatField(result, template, "attachY", new String[]{"attachY", "k"});
        putBooleanField(result, template, "lockMovement", new String[]{"lockMovement", "l"});
        putFloatField(result, template, "estimatingPositionMultiplier",
                new String[]{"estimatingPositionMultiplier", "m"});
        putBooleanField(result, template, "holdDisMinCheckNeighbours",
                new String[]{"holdDisMinCheckNeighbours", "n"});
        putBooleanField(result, template, "favourOppositeSideNeighbours",
                new String[]{"favourOppositeSideNeighbours", "o"});
        putBooleanField(result, template, "alwaysHidden", new String[]{"alwaysHidden", "p"});
        putField(result, template, "hidden", new String[]{"hidden", "q"});
        putFloatField(result, template, "alpha", new String[]{"alpha", "r"});
        putFloatField(result, template, "moveSpeed", new String[]{"moveSpeed", "s"});
        putFloatField(result, template, "moveWarmUp", new String[]{"moveWarmUp", "t"});
        putFloatField(result, template, "rotateSpeed", new String[]{"rotateSpeed", "u"});
        putFloatField(result, template, "heightSpeed", new String[]{"heightSpeed", "v"});
        putFloatField(result, template, "resetAngle", new String[]{"resetAngle", "w"});
        putField(result, template, "middleImage", new String[]{"middleImage", "x"});
        putField(result, template, "middleTeamImages", new String[]{"middleTeamImages", "y"});
        putField(result, template, "endImage", new String[]{"endImage", "B"});
        putField(result, template, "endTeamImages", new String[]{"endTeamImages", "C"});
        putField(result, template, "endShadowImage", new String[]{"endShadowImage", "D"});
        putBooleanField(result, template, "hasZoomedOutDrawOverride",
                new String[]{"hasZoomedOutDrawOverride", "E"});
        putBooleanField(result, template, "drawLegWhenZoomedOut",
                new String[]{"drawLegWhenZoomedOut", "F"});
        putBooleanField(result, template, "drawFootWhenZoomedOut",
                new String[]{"drawFootWhenZoomedOut", "G"});
        putBooleanField(result, template, "drawFootOnTop", new String[]{"drawFootOnTop", "H"});
        putBooleanField(result, template, "dustEffect", new String[]{"dustEffect", "I"});
        putBooleanField(result, template, "explodeOnDeath", new String[]{"explodeOnDeath", "J"});
        putFloatField(result, template, "holdDisMin", new String[]{"holdDisMin", "K"});
        putIntField(result, template, "holdDisMinMaxMovingLegs",
                new String[]{"holdDisMinMaxMovingLegs", "L"});
        putBooleanField(result, template, "holdMoveOnlyIfFurthest",
                new String[]{"holdMoveOnlyIfFurthest", "M"});
        putFloatField(result, template, "holdDisMax", new String[]{"holdDisMax", "N"});
        putFloatField(result, template, "hardLimit", new String[]{"hardLimit", "O"});
        putBooleanField(result, template, "drawOverBody", new String[]{"drawOverBody", "P"});
        putBooleanField(result, template, "drawUnderAllUnits", new String[]{"drawUnderAllUnits", "Q"});
        putFloatField(result, template, "drawDirOffset", new String[]{"drawDirOffset", "R"});
        Object neighboring = RustedReflection.getFieldValue(template, new String[]{"neighboringLegIndices", "S"});
        result.put("neighboringLegIndices", neighboring);
        result.put("neighboringLegIndicesSize", Integer.valueOf(arrayLength(neighboring)));
        putFloatField(result, template, "spinRate", new String[]{"spinRate", "T"});
        return Collections.unmodifiableMap(result);
    }

    public static boolean isCustomUnit(Object value) {
        return value != null && RustedReflection.isAnyClass(value.getClass(), CUSTOM_UNIT_CLASSES);
    }

    public static List<Object> getAttachedUnitActions(Object customUnit, boolean includeUnavailable) {
        requireCustomUnit(customUnit);
        Object value = RustedReflection.invokeInstance(customUnit, new String[]{"getAttachedUnitActions", "e"},
                Boolean.valueOf(includeUnavailable));
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(value));
    }

    public static List<Object> attachedUnitActionBuffer(Object customUnit) {
        requireCustomUnit(customUnit);
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(
                RustedReflection.getFieldValue(customUnit, new String[]{"attachedUnitActionBuffer", "eg"})));
    }

    public static List<Object> legRuntimeStates(Object customUnit) {
        requireCustomUnit(customUnit);
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(
                RustedReflection.getFieldValue(customUnit, new String[]{"legRuntimeStates", "dT"})));
    }

    public static List<Map<String, Object>> describeLegRuntimeStates(Object customUnit) {
        List<Object> states = legRuntimeStates(customUnit);
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>(states.size());
        for (Object state : states) {
            if (state != null && isLegRuntimeState(state)) {
                result.add(describeLegRuntimeState(state));
            }
        }
        return Collections.unmodifiableList(result);
    }

    public static Map<String, Object> describeLegRuntimeState(Object state) {
        requireLegRuntimeState(state);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putIntField(result, state, "index", new String[]{"index", "a"});
        putFloatField(result, state, "footX", new String[]{"footX", "b"});
        putFloatField(result, state, "footY", new String[]{"footY", "c"});
        putFloatField(result, state, "footHeight", new String[]{"footHeight", "d"});
        putFloatField(result, state, "moveWarmupTimer", new String[]{"moveWarmupTimer", "e"});
        putFloatField(result, state, "targetX", new String[]{"targetX", "f"});
        putFloatField(result, state, "targetY", new String[]{"targetY", "g"});
        putFloatField(result, state, "distanceToTargetSquared",
                new String[]{"distanceToTargetSquared", "h"});
        putFloatField(result, state, "footDir", new String[]{"footDir", "i"});
        putBooleanField(result, state, "landingEffectEmitted",
                new String[]{"landingEffectEmitted", "j"});
        putBooleanField(result, state, "moving", new String[]{"moving", "k"});
        putBooleanField(result, state, "needsPositionReset", new String[]{"needsPositionReset", "m"});
        putBooleanField(result, state, "fallingReset", new String[]{"fallingReset", "n"});
        putBooleanField(result, state, "positionDirty", new String[]{"positionDirty", "o"});
        putFloatField(result, state, "spinAngle", new String[]{"spinAngle", "r"});
        putFloatField(result, state, "alpha", new String[]{"alpha", "s"});
        return Collections.unmodifiableMap(result);
    }

    public static boolean isLegRuntimeState(Object value) {
        return value != null && RustedReflection.isAnyClass(value.getClass(), LEG_RUNTIME_STATE_CLASSES);
    }

    public static void ensureLegRuntimeStates(Object customUnit) {
        requireCustomUnit(customUnit);
        RustedReflection.invokeInstance(customUnit, new String[]{"ensureLegRuntimeStates", "du"});
    }

    public static void refreshLegRuntimeStates(Object customUnit) {
        requireCustomUnit(customUnit);
        RustedReflection.invokeInstance(customUnit, new String[]{"refreshLegRuntimeStates", "dv"});
    }

    public static void markLegsForFalling(Object customUnit) {
        requireCustomUnit(customUnit);
        RustedReflection.invokeInstance(customUnit, new String[]{"markLegsForFalling", "dB"});
    }

    public static Object emptyLocalizedString() {
        return RustedReflection.getStaticFieldValue(LOCALIZED_STRING_CLASSES, new String[]{"EMPTY", "a"});
    }

    public static Object localizedStringFromLiteral(String text) {
        return RustedReflection.invokeStatic(LOCALIZED_STRING_CLASSES,
                new String[]{"fromLiteral", "a"}, text);
    }

    public static Object localizedStringFromData(Object data) {
        requireLocalizedStringData(data);
        return RustedReflection.invokeStatic(LOCALIZED_STRING_CLASSES,
                new String[]{"fromData", "a"}, data);
    }

    public static boolean isLocalizedString(Object value) {
        return value != null && RustedReflection.isAnyClass(value.getClass(), LOCALIZED_STRING_CLASSES);
    }

    public static Map<String, Object> describeLocalizedString(Object localizedString) {
        requireLocalizedString(localizedString);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", localizedString.getClass().getName());
        putField(result, localizedString, "dynamicTextResolvers",
                new String[]{"dynamicTextResolvers", "b"});
        result.put("dynamicTextResolversSize", Integer.valueOf(dynamicTextResolvers(localizedString).size()));
        putField(result, localizedString, "localizedEntries", new String[]{"localizedEntries", "c"});
        result.put("localizedEntriesSize", Integer.valueOf(localizedStringEntries(localizedString).size()));
        putField(result, localizedString, "cachedText", new String[]{"cachedText", "d"});
        putIntField(result, localizedString, "cachedLocaleVersion",
                new String[]{"cachedLocaleVersion", "e"});
        putField(result, localizedString, "translationKey", new String[]{"translationKey", "f"});
        putField(result, localizedString, "dynamicParseError", new String[]{"dynamicParseError", "g"});
        putField(result, localizedString, "metadata", new String[]{"metadata", "h"});
        result.put("staticText", resolveStaticLocalizedText(localizedString));
        return Collections.unmodifiableMap(result);
    }

    public static List<Object> dynamicTextResolvers(Object localizedString) {
        requireLocalizedString(localizedString);
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(
                RustedReflection.getFieldValue(localizedString, new String[]{"dynamicTextResolvers", "b"})));
    }

    public static List<Object> localizedStringEntries(Object localizedString) {
        requireLocalizedString(localizedString);
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(
                RustedReflection.getFieldValue(localizedString, new String[]{"localizedEntries", "c"})));
    }

    public static void refreshTextAndDynamicResolvers(Object localizedString) {
        requireLocalizedString(localizedString);
        RustedReflection.invokeInstance(localizedString, new String[]{"refreshTextAndDynamicResolvers", "a"});
    }

    public static void refreshTextAndDynamicResolvers(Object localizedString, boolean forceDynamicParse) {
        requireLocalizedString(localizedString);
        RustedReflection.invokeInstance(localizedString, new String[]{"refreshTextAndDynamicResolvers", "a"},
                Boolean.valueOf(forceDynamicParse));
    }

    public static List<Object> parseDynamicTextResolvers(Object localizedString, String text,
                                                         boolean requireUnitContext) {
        requireLocalizedString(localizedString);
        Object value = RustedReflection.invokeInstance(localizedString,
                new String[]{"parseDynamicTextResolvers", "a"}, text, Boolean.valueOf(requireUnitContext));
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(value));
    }

    public static String resolveDynamicTextForUnit(Object localizedString, Object unit) {
        requireLocalizedString(localizedString);
        if (unit != null) {
            requireUnit(unit);
        }
        return invokeString(localizedString, new String[]{"resolveDynamicTextForUnit", "a"}, unit);
    }

    public static String resolveLocalizedTextForUnit(Object localizedString, Object unit) {
        requireLocalizedString(localizedString);
        if (unit != null) {
            requireUnit(unit);
        }
        return invokeString(localizedString, new String[]{"resolveForUnit", "b"}, unit);
    }

    public static String resolveStaticLocalizedText(Object localizedString) {
        requireLocalizedString(localizedString);
        return invokeString(localizedString, new String[]{"resolveStaticText", "b"});
    }

    public static void refreshLocalizedText(Object localizedString) {
        requireLocalizedString(localizedString);
        RustedReflection.invokeInstance(localizedString, new String[]{"refreshLocalizedText", "c"});
    }

    public static Map<String, Object> describeLocalizedStringData(Object data) {
        requireLocalizedStringData(data);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", data.getClass().getName());
        putField(result, data, "entries", new String[]{"entries", "b"});
        result.put("entriesSize", Integer.valueOf(RustedReflection.snapshotIterable(
                RustedReflection.getFieldValue(data, new String[]{"entries", "b"})).size()));
        putField(result, data, "cachedText", new String[]{"cachedText", "c"});
        putIntField(result, data, "cachedLocaleVersion", new String[]{"cachedLocaleVersion", "d"});
        putField(result, data, "translationKey", new String[]{"translationKey", "e"});
        result.put("empty", Boolean.valueOf(invokeBooleanOrFalse(data, new String[]{"isEmpty", "a"})));
        result.put("resolved", invokeString(data, new String[]{"resolve", "b"}));
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeLocalizedStringEntry(Object entry) {
        requireLocalizedStringEntry(entry);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", entry.getClass().getName());
        putField(result, entry, "locale", new String[]{"locale", "a"});
        putField(result, entry, "text", new String[]{"text", "b"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeCurrentActionContext() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("currentActionTargetPoint",
                RustedReflection.getStaticFieldValue(CUSTOM_UNIT_CLASSES,
                        new String[]{"currentActionTargetPoint", "dM"}));
        result.put("currentActionTargetUnit",
                RustedReflection.getStaticFieldValue(CUSTOM_UNIT_CLASSES,
                        new String[]{"currentActionTargetUnit", "dN"}));
        result.put("currentActionRepeatedCount", Integer.valueOf(((Number)
                RustedReflection.getStaticFieldValue(CUSTOM_UNIT_CLASSES,
                        new String[]{"currentActionRepeatedCount", "dO"})).intValue()));
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeCustomUnitActionRuntime(Object customUnit) {
        requireCustomUnit(customUnit);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.putAll(describeCurrentActionContext());
        putFloatField(result, customUnit, "generationDelayTimer", new String[]{"generationDelayTimer", "o"});
        putBooleanField(result, customUnit, "generationResourcesActive",
                new String[]{"generationResourcesActive", "p"});
        putFloatField(result, customUnit, "updateUnitMemoryTimer", new String[]{"updateUnitMemoryTimer", "q"});
        putField(result, customUnit, "upgradeActionScratchList",
                new String[]{"upgradeActionScratchList", "dU"});
        result.put("actionsForCurrentMetadataSize",
                Integer.valueOf(getActionsForCurrentMetadata(customUnit).size()));
        result.put("upgradeActionsSize", Integer.valueOf(getUpgradeActions(customUnit).size()));
        result.put("firstUpgradeActionId", getFirstUpgradeActionId(customUnit));
        result.put("secondaryUpgradeActionIdsSize",
                Integer.valueOf(collectSecondaryUpgradeActionIds(customUnit).size()));
        return Collections.unmodifiableMap(result);
    }

    public static List<Object> getActionsForCurrentMetadata(Object customUnit) {
        requireCustomUnit(customUnit);
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(
                RustedReflection.invokeInstance(customUnit,
                        new String[]{"getActionsForCurrentMetadata", "N"})));
    }

    public static Object findActionById(Object customUnit, Object actionId) {
        requireCustomUnit(customUnit);
        requireUnitActionId(actionId);
        return RustedReflection.invokeInstance(customUnit,
                new String[]{"findActionById", "a"}, actionId);
    }

    public static Object findBuildQueueActionForUnitType(Object customUnit, Object unitType) {
        requireCustomUnit(customUnit);
        requireUnitType(unitType);
        return RustedReflection.invokeInstance(customUnit,
                new String[]{"findBuildQueueActionForUnitType", "e"}, unitType);
    }

    public static boolean checkTargetedActionOrder(Object customUnit, Object action, float x, float y) {
        requireCustomUnit(customUnit);
        requireUnitAction(action);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(customUnit,
                new String[]{"checkTargetedActionOrder", "a"},
                action, Float.valueOf(x), Float.valueOf(y)));
    }

    public static void onTargetedActionQueued(Object customUnit, Object action, boolean queued, float x, float y) {
        requireCustomUnit(customUnit);
        requireUnitAction(action);
        RustedReflection.invokeInstance(customUnit, new String[]{"onTargetedActionQueued", "a"},
                action, Boolean.valueOf(queued), Float.valueOf(x), Float.valueOf(y));
    }

    public static List<Object> getUpgradeActions(Object customUnit) {
        requireCustomUnit(customUnit);
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(
                RustedReflection.invokeInstance(customUnit, new String[]{"getUpgradeActions", "dC"})));
    }

    public static Object getFirstUpgradeActionId(Object customUnit) {
        requireCustomUnit(customUnit);
        return RustedReflection.invokeInstance(customUnit, new String[]{"getFirstUpgradeActionId", "cm"});
    }

    public static List<Object> collectSecondaryUpgradeActionIds(Object customUnit) {
        requireCustomUnit(customUnit);
        ArrayList<Object> result = new ArrayList<Object>();
        RustedReflection.invokeInstance(customUnit,
                new String[]{"collectSecondaryUpgradeActionIds", "a"}, result);
        return Collections.unmodifiableList(result);
    }

    public static Map<String, Object> describeTransportMetadata(Object metadata) {
        requireCustomUnitMetadata(metadata);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putIntField(result, metadata, "maxTransportingUnits", new String[]{"maxTransportingUnits", "eM"});
        putFloatField(result, metadata, "transportUnitsUnloadDelayBetweenEachUnit",
                new String[]{"transportUnitsUnloadDelayBetweenEachUnit", "eN"});
        putBooleanField(result, metadata, "transportUnitsEachUnitAlwaysUsesSingleSlot",
                new String[]{"transportUnitsEachUnitAlwaysUsesSingleSlot", "eO"});
        putField(result, metadata, "transportUnitsRequireTag", new String[]{"transportUnitsRequireTag", "eP"});
        putField(result, metadata, "transportUnitsRequireMovementTypes",
                new String[]{"transportUnitsRequireMovementTypes", "eQ"});
        putBooleanField(result, metadata, "transportUnitsBlockAirAndWaterUnits",
                new String[]{"transportUnitsBlockAirAndWaterUnits", "eR"});
        putBooleanField(result, metadata, "transportUnitsBlockOtherTransports",
                new String[]{"transportUnitsBlockOtherTransports", "eS"});
        putBooleanField(result, metadata, "transportUnitsAddUnloadOption",
                new String[]{"transportUnitsAddUnloadOption", "eT"});
        putField(result, metadata, "transportUnitsKeepBuiltUnits",
                new String[]{"transportUnitsKeepBuiltUnits", "eU"});
        putField(result, metadata, "transportUnitsKillOnDeath",
                new String[]{"transportUnitsKillOnDeath", "eV"});
        putField(result, metadata, "transportUnitsKeepWaypoints",
                new String[]{"transportUnitsKeepWaypoints", "eW"});
        putBooleanField(result, metadata, "transportUnitsOnTeamChangeKeepCurrentTeam",
                new String[]{"transportUnitsOnTeamChangeKeepCurrentTeam", "eX"});
        putFloatField(result, metadata, "transportUnitsHealBy", new String[]{"transportUnitsHealBy", "eY"});
        putIntField(result, metadata, "transportSlotsNeeded", new String[]{"transportSlotsNeeded", "eZ"});
        putField(result, metadata, "transportUnitsCanUnloadUnitsCondition",
                new String[]{"transportUnitsCanUnloadUnitsCondition", "fc"});
        putField(result, metadata, "transportUnitsCanUnloadUnitsRelaxedCondition",
                new String[]{"transportUnitsCanUnloadUnitsRelaxedCondition", "fd"});
        return Collections.unmodifiableMap(result);
    }

    public static List<Object> attachmentSlots(Object metadata) {
        requireCustomUnitMetadata(metadata);
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(
                RustedReflection.getFieldValue(metadata, new String[]{"attachmentSlots", "aA"})));
    }

    public static Object getAttachmentSlotByName(Object metadata, String name) {
        requireCustomUnitMetadata(metadata);
        requireText(name, "name");
        return RustedReflection.invokeInstance(metadata, new String[]{"getAttachmentSlotByName", "i"}, name);
    }

    public static Map<String, Object> describeAttachmentSlot(Object attachmentSlot) {
        requireAttachmentSlot(attachmentSlot);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putIntField(result, attachmentSlot, "index", new String[]{"index", "a"});
        putField(result, attachmentSlot, "name", new String[]{"name", "b"});
        putFloatField(result, attachmentSlot, "x", new String[]{"x", "c"});
        putFloatField(result, attachmentSlot, "y", new String[]{"y", "d"});
        putFloatField(result, attachmentSlot, "height", new String[]{"height", "e"});
        putBooleanField(result, attachmentSlot, "setDrawLayerOnTop", new String[]{"setDrawLayerOnTop", "A"});
        putBooleanField(result, attachmentSlot, "setDrawLayerOnBottom", new String[]{"setDrawLayerOnBottom", "B"});
        putBooleanField(result, attachmentSlot, "reservedAttachmentFlagC",
                new String[]{"reservedAttachmentFlagC", "C"});
        putBooleanField(result, attachmentSlot, "addTransportedUnits", new String[]{"addTransportedUnits", "D"});
        putBooleanField(result, attachmentSlot, "unloadInCurrentPosition",
                new String[]{"unloadInCurrentPosition", "E"});
        putBooleanField(result, attachmentSlot, "smoothlyBlendPositionWhenExistingUnitAdded",
                new String[]{"smoothlyBlendPositionWhenExistingUnitAdded", "F"});
        putFloatField(result, attachmentSlot, "smoothlyBlendPositionBlendTime",
                new String[]{"smoothlyBlendPositionBlendTime", "G"});
        putBooleanField(result, attachmentSlot, "deattachIfWantingToMove",
                new String[]{"deattachIfWantingToMove", "H"});
        putBooleanField(result, attachmentSlot, "hidden", new String[]{"hidden", "I"});
        putBooleanField(result, attachmentSlot, "prioritizeParentsMainTarget",
                new String[]{"prioritizeParentsMainTarget", "J"});
        putBooleanField(result, attachmentSlot, "onlyAttackParentsMainTarget",
                new String[]{"onlyAttackParentsMainTarget", "K"});
        putBooleanField(result, attachmentSlot, "alwaysAllowedToAttackParentsMainTarget",
                new String[]{"alwaysAllowedToAttackParentsMainTarget", "L"});
        putBooleanField(result, attachmentSlot, "canAttack", new String[]{"canAttack", "M"});
        putField(result, attachmentSlot, "showAllActionsFrom", new String[]{"showAllActionsFrom", "N"});
        putBooleanField(result, attachmentSlot, "keepWaypointsNeedingMovement",
                new String[]{"keepWaypointsNeedingMovement", "O"});
        putBooleanField(result, attachmentSlot, "canBeAttackedAndDamaged",
                new String[]{"canBeAttackedAndDamaged", "l"});
        putBooleanField(result, attachmentSlot, "isVisible", new String[]{"isVisible", "o"});
        return Collections.unmodifiableMap(result);
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
        putFloatField(result, projectileTemplate, "ballisticDelayMoveHeight",
                new String[]{"ballisticDelayMoveHeight", "as"});
        putFloatField(result, projectileTemplate, "shieldDeflectionMultiplier",
                new String[]{"shieldDeflectionMultiplier", "aT"});
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

    public static Object currentModManager() {
        Object engine = GameEngineDiagnostics.currentEngineOrNull();
        return engine != null ? RustedReflection.getFieldValue(engine, new String[]{"modManager", "bZ"}) : null;
    }

    public static Map<String, Object> describeCurrentModManager() {
        Object modManager = currentModManager();
        return modManager != null ? describeModManager(modManager) : Collections.<String, Object>emptyMap();
    }

    public static Map<String, Object> describeModManager(Object modManager) {
        requireModManager(modManager);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putField(result, modManager, "modListLock", new String[]{"modListLock", "d"});
        putCollectionField(result, modManager, "mods", new String[]{"mods", "e"});
        putCollectionField(result, modManager, "extraMapRecords", new String[]{"extraMapRecords", "f"});
        result.put("enabledModsIncludingErrors",
                Integer.valueOf(invokeIntOrZero(modManager, new String[]{"countEnabledMods", "a"}, Boolean.FALSE)));
        result.put("enabledModsSkippingErrors",
                Integer.valueOf(invokeIntOrZero(modManager, new String[]{"countEnabledMods", "a"}, Boolean.TRUE)));
        result.put("enabledModsWithErrors",
                Integer.valueOf(invokeIntOrZero(modManager, new String[]{"countEnabledModsWithErrors", "b"})));
        result.put("enabledAndErrorFreeModsSize",
                Integer.valueOf(modManagerListMethodSize(modManager,
                        new String[]{"getEnabledAndErrorFreeMods", "j"})));
        result.put("musicTracksFromActiveModsSize",
                Integer.valueOf(modManagerListMethodSize(modManager,
                        new String[]{"getMusicTracksFromActiveMods", "i"})));
        result.put("allModsForCustomUnitLoadingSize",
                Integer.valueOf(modManagerListMethodSize(modManager,
                        new String[]{"getAllModsForCustomUnitLoading", "k"})));
        return Collections.unmodifiableMap(result);
    }

    public static List<Object> modsSnapshot(Object modManager) {
        requireModManager(modManager);
        Object mods = RustedReflection.getFieldValue(modManager, new String[]{"mods", "e"});
        Object lock = RustedReflection.getFieldValue(modManager, new String[]{"modListLock", "d"});
        if (lock != null) {
            synchronized (lock) {
                return Collections.unmodifiableList(RustedReflection.snapshotIterable(mods));
            }
        }
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(mods));
    }

    public static List<Object> enabledAndErrorFreeModsSnapshot(Object modManager) {
        requireModManager(modManager);
        Object value = RustedReflection.invokeInstance(modManager, new String[]{"getEnabledAndErrorFreeMods", "j"});
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(value));
    }

    public static List<Object> musicTracksFromActiveModsSnapshot(Object modManager) {
        requireModManager(modManager);
        Object value = RustedReflection.invokeInstance(modManager, new String[]{"getMusicTracksFromActiveMods", "i"});
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(value));
    }

    public static Object findModByHash(Object modManager, String hash) {
        requireModManager(modManager);
        return RustedReflection.invokeInstance(modManager, new String[]{"findModByHash", "c"}, hash);
    }

    public static Object findModByInternalId(Object modManager, int internalId) {
        requireModManager(modManager);
        return RustedReflection.invokeInstance(modManager,
                new String[]{"findModByInternalId", "a"}, Integer.valueOf(internalId));
    }

    public static Object findModByDisplayName(Object modManager, String displayName) {
        requireModManager(modManager);
        return RustedReflection.invokeInstance(modManager, new String[]{"findModByDisplayName", "f"}, displayName);
    }

    public static String normalizeModVersionString(String version) {
        return invokeStaticString(MOD_MANAGER_CLASSES, new String[]{"normalizeVersionString", "b"}, version);
    }

    public static Map<String, Object> describeModInfo(Object modInfo) {
        requireModInfo(modInfo);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("displayName", getModDisplayName(modInfo));
        result.put("shortDisplayName25", invokeString(modInfo, new String[]{"getShortDisplayName25", "b"}));
        result.put("shortDisplayName40", invokeString(modInfo, new String[]{"getShortDisplayName40", "c"}));
        result.put("statusText", getModStatusText(modInfo));
        result.put("enabledAndErrorFree", Boolean.valueOf(isModEnabledAndErrorFree(modInfo)));
        result.put("disabled", Boolean.valueOf(RustedReflection.getBooleanField(modInfo, new String[]{"disabled", "f"})));
        result.put("folderName", RustedReflection.getStringField(modInfo, new String[]{"folderName", "d"}));
        result.put("defaultDisplayName",
                RustedReflection.getStringField(modInfo, new String[]{"defaultDisplayName", "c"}));
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
        putBooleanField(result, modInfo, "savedDisabledSnapshot", new String[]{"savedDisabledSnapshot", "g"});
        putBooleanField(result, modInfo, "seenInSavedSelection", new String[]{"seenInSavedSelection", "i"});
        putBooleanField(result, modInfo, "rwmodPackage", new String[]{"rwmodPackage", "j"});
        putLongField(result, modInfo, "workshopId", new String[]{"workshopId", "k"});
        putBooleanField(result, modInfo, "seenInCurrentScan", new String[]{"seenInCurrentScan", "l"});
        putBooleanField(result, modInfo, "modInfoLoaded", new String[]{"modInfoLoaded", "r"});
        putIntField(result, modInfo, "scanOrder", new String[]{"scanOrder", "x"});
        putIntField(result, modInfo, "stableSortId", new String[]{"stableSortId", "L"});
        putCollectionField(result, modInfo, "loadWarnings", new String[]{"loadWarnings", "U"});
        putCollectionField(result, modInfo, "loadErrors", new String[]{"loadErrors", "V"});
        result.put("memoryUsageSummary", getModMemoryUsageSummary(modInfo));
        result.put("canDeleteModFile", Boolean.valueOf(canDeleteModFile(modInfo)));
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

    public static String getModInfoValue(Object modInfo, String key) {
        requireModInfo(modInfo);
        requireText(key, "key");
        return invokeString(modInfo, new String[]{"getModInfoValue", "c"}, key);
    }

    public static String getModSteamDatPath(Object modInfo) {
        requireModInfo(modInfo);
        return invokeString(modInfo, new String[]{"getSteamDatPath", "w"});
    }

    public static String getModMemoryUsageSummary(Object modInfo) {
        requireModInfo(modInfo);
        return invokeString(modInfo, new String[]{"getMemoryUsageSummary", "s"});
    }

    public static boolean canDeleteModFile(Object modInfo) {
        requireModInfo(modInfo);
        return invokeBooleanOrFalse(modInfo, new String[]{"canDeleteModFile", "v"});
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

    public static String joinUnitResourcePath(String basePath, String childPath) {
        Object value = RustedReflection.invokeStatic(CUSTOM_UNIT_LOADER_CLASSES,
                new String[]{"joinUnitResourcePath", "a"}, basePath, childPath);
        return value != null ? value.toString() : null;
    }

    public static String formatModRelativePathForError(Object modInfo, String path, boolean includeModName) {
        if (modInfo != null) {
            requireModInfo(modInfo);
        }
        Object value = RustedReflection.invokeStatic(CUSTOM_UNIT_LOADER_CLASSES,
                new String[]{"formatModRelativePathForError", "a"},
                modInfo, path, Boolean.valueOf(includeModName));
        return value != null ? value.toString() : null;
    }

    public static List<Object> parseAutoTriggerEventList(String rawEvents, String section, String key) {
        Object value = RustedReflection.invokeStatic(CUSTOM_UNIT_LOADER_CLASSES,
                new String[]{"parseAutoTriggerEventList", "a"}, rawEvents, section, key);
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(value));
    }

    public static List<Map<String, Object>> describeAutoTriggerEventSpecs(String rawEvents, String section, String key) {
        List<Object> specs = parseAutoTriggerEventList(rawEvents, section, key);
        java.util.ArrayList<Map<String, Object>> result = new java.util.ArrayList<Map<String, Object>>();
        for (Object spec : specs) {
            if (isAutoTriggerEventSpec(spec)) {
                result.add(describeAutoTriggerEventSpec(spec));
            }
        }
        return Collections.unmodifiableList(result);
    }

    public static boolean isAutoTriggerEventSpec(Object value) {
        return value != null && RustedReflection.isAnyClass(value.getClass(), AUTO_TRIGGER_EVENT_SPEC_CLASSES);
    }

    public static Map<String, Object> describeAutoTriggerEventSpec(Object spec) {
        requireAutoTriggerEventSpec(spec);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("eventTypeName", RustedReflection.getStringField(spec, new String[]{"eventTypeName", "a"}));
        Object parameterMap = RustedReflection.getFieldValue(spec, new String[]{"parameterMap", "b"});
        result.put("parameterMap", parameterMap);
        result.put("parameterMapSize", Integer.valueOf(mapSize(parameterMap)));
        return Collections.unmodifiableMap(result);
    }

    private static String invokeString(Object owner, String[] methodNames, Object... args) {
        Object value = RustedReflection.invokeInstance(owner, methodNames, args);
        return value != null ? value.toString() : null;
    }

    private static String invokeStaticString(String[] classNames, String[] methodNames, Object... args) {
        Object value = RustedReflection.invokeStatic(classNames, methodNames, args);
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

    private static void putCollectionField(Map<String, Object> result, Object owner, String key,
                                           String[] fieldNames) {
        Object value = RustedReflection.getFieldValue(owner, fieldNames);
        result.put(key, value);
        result.put(key + "Size", Integer.valueOf(RustedReflection.snapshotIterable(value).size()));
    }

    private static void putMethodCollection(Map<String, Object> result, Object owner, String key,
                                            String[] methodNames) {
        Object value = RustedReflection.invokeInstance(owner, methodNames);
        result.put(key, value);
        result.put(key + "Size", Integer.valueOf(RustedReflection.snapshotIterable(value).size()));
    }

    private static Object invokeOrNull(Object owner, String[] methodNames, Object... args) {
        try {
            return RustedReflection.invokeInstance(owner, methodNames, args);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static int modManagerListMethodSize(Object modManager, String[] methodNames) {
        try {
            Object value = RustedReflection.invokeInstance(modManager, methodNames);
            return RustedReflection.snapshotIterable(value).size();
        } catch (RuntimeException ignored) {
            return 0;
        }
    }

    private static int invokeIntOrZero(Object owner, String[] methodNames, Object... args) {
        try {
            Object value = RustedReflection.invokeInstance(owner, methodNames, args);
            return value instanceof Number ? ((Number) value).intValue() : 0;
        } catch (RuntimeException ignored) {
            return 0;
        }
    }

    private static boolean invokeBooleanOrFalse(Object owner, String[] methodNames, Object... args) {
        try {
            return Boolean.TRUE.equals(RustedReflection.invokeInstance(owner, methodNames, args));
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static int mapSize(Object value) {
        return value instanceof Map<?, ?> ? ((Map<?, ?>) value).size() : 0;
    }

    private static int arrayLength(Object value) {
        return value != null && value.getClass().isArray() ? java.lang.reflect.Array.getLength(value) : 0;
    }

    private static void requireTurretTemplate(Object turretTemplate) {
        requireAny(turretTemplate, TURRET_TEMPLATE_CLASSES, "TurretTemplate");
    }

    private static void requireCustomProjectileTemplate(Object projectileTemplate) {
        requireAny(projectileTemplate, CUSTOM_PROJECTILE_TEMPLATE_CLASSES, "CustomProjectileTemplate");
    }

    private static void requireCustomUnit(Object customUnit) {
        requireAny(customUnit, CUSTOM_UNIT_CLASSES, "CustomUnit");
    }

    private static void requireUnit(Object unit) {
        requireAny(unit, UNIT_CLASSES, "Unit");
    }

    private static void requireUnitAction(Object action) {
        requireAny(action, UNIT_ACTION_CLASSES, "UnitAction");
    }

    private static void requireUnitActionId(Object actionId) {
        requireAny(actionId, UNIT_ACTION_ID_CLASSES, "UnitActionId");
    }

    private static void requireUnitType(Object unitType) {
        requireAny(unitType, UNIT_TYPE_CLASSES, "UnitType");
    }

    private static void requireCustomUnitMetadata(Object metadata) {
        requireAny(metadata, CUSTOM_UNIT_METADATA_CLASSES, "CustomUnitMetadata");
    }

    private static void requireLegOrArmTemplate(Object template) {
        requireAny(template, LEG_OR_ARM_TEMPLATE_CLASSES, "LegOrArmTemplate");
    }

    private static void requireLegRuntimeState(Object state) {
        requireAny(state, LEG_RUNTIME_STATE_CLASSES, "LegRuntimeState");
    }

    private static void requireLocalizedString(Object localizedString) {
        requireAny(localizedString, LOCALIZED_STRING_CLASSES, "LocalizedString");
    }

    private static void requireLocalizedStringData(Object data) {
        requireAny(data, LOCALIZED_STRING_DATA_CLASSES, "LocalizedStringData");
    }

    private static void requireLocalizedStringEntry(Object entry) {
        requireAny(entry, LOCALIZED_STRING_ENTRY_CLASSES, "LocalizedStringEntry");
    }

    private static void requireAttachmentSlot(Object attachmentSlot) {
        requireAny(attachmentSlot, ATTACHMENT_SLOT_CLASSES, "AttachmentSlot");
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

    private static void requireModManager(Object modManager) {
        requireAny(modManager, MOD_MANAGER_CLASSES, "ModManager");
    }

    private static void requireAutoTriggerEventSpec(Object spec) {
        requireAny(spec, AUTO_TRIGGER_EVENT_SPEC_CLASSES, "AutoTriggerEventSpec");
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
