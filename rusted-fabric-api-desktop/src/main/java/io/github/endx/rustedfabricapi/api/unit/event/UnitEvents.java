package io.github.endx.rustedfabricapi.api.unit.event;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import rustedwarfare.unit.Unit;

/** Strongly typed events for membership in the game's global/team unit registries. */
public final class UnitEvents {
    public static final RustedFabricEvent<BeforeRegister> BEFORE_REGISTER =
            RustedFabricEvent.create(listeners -> unit -> {
                for (BeforeRegister listener : listeners) listener.onUnit(unit);
            });
    public static final RustedFabricEvent<AfterRegister> AFTER_REGISTER =
            RustedFabricEvent.create(listeners -> unit -> {
                for (AfterRegister listener : listeners) listener.onUnit(unit);
            });
    public static final RustedFabricEvent<BeforeUnregister> BEFORE_UNREGISTER =
            RustedFabricEvent.create(listeners -> unit -> {
                for (BeforeUnregister listener : listeners) listener.onUnit(unit);
            });
    public static final RustedFabricEvent<AfterUnregister> AFTER_UNREGISTER =
            RustedFabricEvent.create(listeners -> unit -> {
                for (AfterUnregister listener : listeners) listener.onUnit(unit);
            });

    private UnitEvents() {
    }

    @FunctionalInterface
    public interface BeforeRegister {
        void onUnit(Unit unit);

    }

    @FunctionalInterface
    public interface AfterRegister {
        void onUnit(Unit unit);

    }

    @FunctionalInterface
    public interface BeforeUnregister {
        void onUnit(Unit unit);

    }

    @FunctionalInterface
    public interface AfterUnregister {
        void onUnit(Unit unit);

    }
}
