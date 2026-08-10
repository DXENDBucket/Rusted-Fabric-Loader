package io.github.endx.rustedfabricapi.impl.custom;

import android.graphics.PointF;
import io.github.endx.rustedfabricapi.mixin.accessor.CustomUnitMetadataAccessor;
import rustedwarfare.custom.CustomUnit;
import rustedwarfare.custom.CustomUnitMetadata;
import rustedwarfare.custom.action.CustomAction;
import rustedwarfare.custom.action.CustomActionConfig;
import rustedwarfare.unit.Unit;
import rustedwarfare.unit.action.UnitAction;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

/** Internal deterministic timers backing the public per-action auto-trigger API. */
public final class PerActionAutoTriggerCooldownRuntime {
    private static final Map<Object, IdentityHashMap<Object, Float>> CONFIGURED =
            new WeakHashMap<Object, IdentityHashMap<Object, Float>>();
    private static final Map<CustomUnit, IdentityHashMap<Object, Float>> REMAINING =
            new WeakHashMap<CustomUnit, IdentityHashMap<Object, Float>>();

    private PerActionAutoTriggerCooldownRuntime() { }

    public static synchronized void configureFrames(Object metadata, Object actionConfig,
                                                     float frames) {
        IdentityHashMap<Object, Float> actions = CONFIGURED.get(metadata);
        if (actions == null) {
            actions = new IdentityHashMap<Object, Float>();
            CONFIGURED.put(metadata, actions);
        }
        actions.put(actionConfig, frames);
    }

    public static synchronized boolean isEnabled(Object metadata) {
        IdentityHashMap<Object, Float> actions = CONFIGURED.get(metadata);
        return actions != null && !actions.isEmpty();
    }

    /** Advances independent timers and reports whether the native unit-wide latch must be bypassed. */
    public static synchronized boolean beforeAutoTriggerUpdate(CustomUnit unit, float deltaFrames) {
        if (!isEnabled(unit.unitMetadata)) return false;
        IdentityHashMap<Object, Float> timers = REMAINING.get(unit);
        if (timers == null || timers.isEmpty()) return true;
        Iterator<Map.Entry<Object, Float>> iterator = timers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Object, Float> entry = iterator.next();
            float remaining = entry.getValue() - deltaFrames;
            if (remaining <= 0.0F) iterator.remove();
            else entry.setValue(remaining);
        }
        if (timers.isEmpty()) REMAINING.remove(unit);
        return true;
    }

    /** Replaces only the native execution call inside the auto-trigger scan. */
    public static synchronized boolean execute(CustomUnit unit, UnitAction action,
                                               PointF targetPoint, Unit targetUnit,
                                               int recursionDepth, int repeatedCount) {
        CustomUnitMetadata metadata = unit.unitMetadata;
        if (!isEnabled(metadata) || !(action instanceof CustomAction)) {
            return unit.executeActionWithContext(action, targetPoint, targetUnit,
                    recursionDepth, repeatedCount);
        }
        CustomActionConfig actionConfig = ((CustomAction) action).config;
        IdentityHashMap<Object, Float> timers = REMAINING.get(unit);
        if (timers != null) {
            Float remaining = timers.get(actionConfig);
            if (remaining != null && remaining > 0.0F) return false;
        }

        boolean executed = unit.executeActionWithContext(action, targetPoint, targetUnit,
                recursionDepth, repeatedCount);
        float cooldown = configuredOrDefault(metadata, actionConfig);
        if (cooldown > 0.0F) {
            if (timers == null) {
                timers = new IdentityHashMap<Object, Float>();
                REMAINING.put(unit, timers);
            }
            timers.put(actionConfig, cooldown);
        }
        return executed;
    }

    private static float configuredOrDefault(CustomUnitMetadata metadata,
                                             CustomActionConfig actionConfig) {
        IdentityHashMap<Object, Float> configured = CONFIGURED.get(metadata);
        Float duration = configured != null ? configured.get(actionConfig) : null;
        if (duration != null) return duration;
        return ((CustomUnitMetadataAccessor) (Object) metadata)
                .rustedfabricapi$getAutoTriggerCooldownTime();
    }
}
