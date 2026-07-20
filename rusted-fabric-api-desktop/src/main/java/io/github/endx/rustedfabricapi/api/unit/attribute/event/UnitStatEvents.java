package io.github.endx.rustedfabricapi.api.unit.attribute.event;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import io.github.endx.rustedfabricapi.api.unit.attribute.UnitStat;
import io.github.endx.rustedfabricapi.api.unit.attribute.UnitStatChangeCause;
import rustedwarfare.custom.CustomUnit;

/** Events for API writes, native rebases and modifier-driven effective-value changes. */
public final class UnitStatEvents {
    public static final RustedFabricEvent<ModifySetValue> MODIFY_SET_VALUE =
            RustedFabricEvent.create(listeners -> (unit, stat, value) -> {
                double result = value;
                for (ModifySetValue listener : listeners) result = listener.modify(unit, stat, result);
                return result;
            });
    public static final RustedFabricEvent<AfterChange> AFTER_CHANGE =
            RustedFabricEvent.create(listeners -> (unit, stat, oldValue, newValue, cause) -> {
                for (AfterChange listener : listeners) {
                    listener.afterChange(unit, stat, oldValue, newValue, cause);
                }
            });

    private UnitStatEvents() {
    }

    @FunctionalInterface
    public interface ModifySetValue {
        double modify(CustomUnit unit, UnitStat stat, double value);
    }

    @FunctionalInterface
    public interface AfterChange {
        void afterChange(CustomUnit unit, UnitStat stat, double oldEffectiveValue,
                double newEffectiveValue, UnitStatChangeCause cause);
    }
}
