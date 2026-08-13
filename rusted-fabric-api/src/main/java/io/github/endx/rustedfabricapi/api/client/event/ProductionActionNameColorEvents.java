package io.github.endx.rustedfabricapi.api.client.event;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import rustedwarfare.unit.UnitType;
import rustedwarfare.unit.action.UnitAction;

/** Client-only overrides for the label color of actions which produce a unit. */
public final class ProductionActionNameColorEvents {
    /**
     * Resolves an ARGB label color. A listener should return {@code currentColor} when it does
     * not recognize the action or produced type; {@code null} preserves the native tech-level
     * color. Later listeners may replace an earlier override.
     */
    public static final RustedFabricEvent<Resolve> RESOLVE = RustedFabricEvent.create(listeners ->
            (action, producedType, currentColor) -> {
                Integer result = currentColor;
                for (Resolve listener : listeners) {
                    result = listener.resolve(action, producedType, result);
                }
                return result;
            });

    private ProductionActionNameColorEvents() { }

    @FunctionalInterface
    public interface Resolve {
        Integer resolve(UnitAction action, UnitType producedType, Integer currentColor);
    }
}
