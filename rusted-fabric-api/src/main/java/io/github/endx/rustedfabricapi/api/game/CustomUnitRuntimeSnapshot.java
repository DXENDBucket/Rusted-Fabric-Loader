package io.github.endx.rustedfabricapi.api.game;

import io.github.endx.rustedfabricapi.api.util.RustedReflection;

/** Immutable view of the high-confidence v0.84 custom-unit construction/runtime latches. */
public final class CustomUnitRuntimeSnapshot {
    private final Object customUnit;
    private final Object unitMetadata;
    private final Object revertMetadata;
    private final boolean metadataHasBuildQueueRuntimeEffects;
    private final boolean revertMetadataHasBuildQueueRuntimeEffects;
    private final boolean currentBuildQueueActionBlocksMovement;
    private final boolean createdEventPending;
    private final boolean completeAndActiveEventPending;
    private final float autoTriggerCooldownTimer;
    private final float lastLegBaseX;
    private final float lastLegBaseY;
    private final float lastLegBaseHeight;
    private final float lastLegBaseDirection;
    private final boolean lastLegBasePositionAvailable;

    private CustomUnitRuntimeSnapshot(Object customUnit) {
        if (customUnit == null) throw new IllegalArgumentException("customUnit must not be null");
        boolean androidOfficial = hasTypeInHierarchy(customUnit,
                "com.corrodinggames.rts.gameFramework.ah");
        this.customUnit = customUnit;
        this.unitMetadata = field(customUnit, "unitMetadata", "x");
        this.revertMetadata = field(customUnit, "revertMetadata", "z");
        this.metadataHasBuildQueueRuntimeEffects = metadataFlag(unitMetadata);
        this.revertMetadataHasBuildQueueRuntimeEffects = metadataFlag(revertMetadata);
        this.currentBuildQueueActionBlocksMovement = bool(customUnit,
                "currentBuildQueueActionBlocksMovement", "g");
        this.createdEventPending = bool(customUnit, "createdEventPending", "i");
        this.completeAndActiveEventPending = bool(customUnit,
                "completeAndActiveEventPending", "h");
        this.autoTriggerCooldownTimer = number(customUnit,
                "autoTriggerCooldownTimer", "w");
        this.lastLegBasePositionAvailable = !androidOfficial;
        this.lastLegBaseX = androidOfficial
                ? Float.NaN : number(customUnit, "lastLegBaseX", "dP");
        this.lastLegBaseY = androidOfficial
                ? Float.NaN : number(customUnit, "lastLegBaseY", "dQ");
        this.lastLegBaseHeight = number(customUnit, "lastLegBaseHeight", "dR");
        this.lastLegBaseDirection = number(customUnit, "lastLegBaseDir", "dS");
    }

    public static CustomUnitRuntimeSnapshot capture(Object customUnit) {
        return new CustomUnitRuntimeSnapshot(customUnit);
    }

    public Object customUnit() { return customUnit; }
    public Object unitMetadata() { return unitMetadata; }
    public Object revertMetadata() { return revertMetadata; }
    public boolean metadataHasBuildQueueRuntimeEffects() {
        return metadataHasBuildQueueRuntimeEffects;
    }
    public boolean revertMetadataHasBuildQueueRuntimeEffects() {
        return revertMetadataHasBuildQueueRuntimeEffects;
    }
    public boolean currentBuildQueueActionBlocksMovement() {
        return currentBuildQueueActionBlocksMovement;
    }
    public boolean createdEventPending() { return createdEventPending; }
    public boolean completeAndActiveEventPending() { return completeAndActiveEventPending; }
    public float autoTriggerCooldownTimer() { return autoTriggerCooldownTimer; }
    public float lastLegBaseX() { return lastLegBaseX; }
    public float lastLegBaseY() { return lastLegBaseY; }
    public float lastLegBaseHeight() { return lastLegBaseHeight; }
    public float lastLegBaseDirection() { return lastLegBaseDirection; }
    /** False on Android 1.15, where X/Y are not yet present in the strict mapping. */
    public boolean hasLastLegBasePosition() { return lastLegBasePositionAvailable; }

    private static boolean metadataFlag(Object metadata) {
        return metadata != null && bool(metadata, "hasBuildQueueRuntimeEffects", "bg");
    }

    private static Object field(Object owner, String named, String official) {
        return RustedReflection.getFieldValue(owner, new String[]{named, official});
    }

    private static boolean bool(Object owner, String named, String official) {
        return Boolean.TRUE.equals(field(owner, named, official));
    }

    private static float number(Object owner, String named, String official) {
        Object value = field(owner, named, official);
        return value instanceof Number ? ((Number) value).floatValue() : 0.0F;
    }

    private static boolean hasTypeInHierarchy(Object owner, String typeName) {
        Class<?> current = owner.getClass();
        while (current != null) {
            if (typeName.equals(current.getName())) return true;
            current = current.getSuperclass();
        }
        return false;
    }
}
