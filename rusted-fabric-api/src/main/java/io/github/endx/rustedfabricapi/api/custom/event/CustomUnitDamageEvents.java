package io.github.endx.rustedfabricapi.api.custom.event;

import io.github.endx.rustedfabricapi.api.custom.CustomUnitHandle;
import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;

/** Namespace-stable damage boundaries for INI-backed custom units. */
public final class CustomUnitDamageEvents {
    /**
     * Modifies health at the native lethal-damage clamp for a custom-unit victim.
     *
     * <p>This portable counterpart to the mapped desktop event deliberately exposes no game
     * classes. {@code attacker} is {@code null} when the source is absent or is not an INI-backed
     * custom unit. Returning {@code null} retains the result from earlier listeners.</p>
     */
    public static final RustedFabricEvent<ModifyLethalHealth> MODIFY_LETHAL_HEALTH =
            RustedFabricEvent.create(listeners -> (unit, attacker, requestedAmount,
                                                    nativeValue, unclampedValue, currentValue) -> {
                Float result = Float.valueOf(currentValue);
                for (ModifyLethalHealth listener : listeners) {
                    Float replacement = listener.modify(unit, attacker, requestedAmount,
                            nativeValue, unclampedValue, result.floatValue());
                    if (replacement != null) result = replacement;
                }
                return result;
            });

    private CustomUnitDamageEvents() { }

    @FunctionalInterface
    public interface ModifyLethalHealth {
        Float modify(CustomUnitHandle unit, CustomUnitHandle attacker, float requestedAmount,
                     float nativeValue, float unclampedValue, float currentValue);
    }
}
