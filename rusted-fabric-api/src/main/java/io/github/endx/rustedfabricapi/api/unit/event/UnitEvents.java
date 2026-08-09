package io.github.endx.rustedfabricapi.api.unit.event;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import io.github.endx.rustedfabricapi.api.game.UnitView;
import io.github.endx.rustedfabricapi.api.game.Units;
import rustedwarfare.unit.Unit;

import java.util.function.Consumer;

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

    /** Registers a permanent listener using the namespace-neutral unit view. */
    public static void registerAfterUnitAdded(Consumer<? super UnitView> listener) {
        if (listener == null) throw new IllegalArgumentException("listener must not be null");
        AFTER_REGISTER.register(unit -> listener.accept(Units.view(unit)));
    }

    /** Registers a removable listener using the namespace-neutral unit view. */
    public static RustedFabricEvent.Registration subscribeAfterUnitAdded(
            Consumer<? super UnitView> listener) {
        if (listener == null) throw new IllegalArgumentException("listener must not be null");
        return AFTER_REGISTER.subscribe(unit -> listener.accept(Units.view(unit)));
    }

    public static void registerBeforeUnitRemoved(Consumer<? super UnitView> listener) {
        if (listener == null) throw new IllegalArgumentException("listener must not be null");
        BEFORE_UNREGISTER.register(unit -> listener.accept(Units.view(unit)));
    }

    public static RustedFabricEvent.Registration subscribeBeforeUnitRemoved(
            Consumer<? super UnitView> listener) {
        if (listener == null) throw new IllegalArgumentException("listener must not be null");
        return BEFORE_UNREGISTER.subscribe(unit -> listener.accept(Units.view(unit)));
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
