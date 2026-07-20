package io.github.endx.rustedfabricapi.api.event;

import io.github.endx.rustedfabricapi.api.game.UnitView;
import io.github.endx.rustedfabricapi.api.game.Units;

import java.util.function.Consumer;

public final class UnitLifecycleEvents {
    public static final RustedFabricEvent<BeforeUnitRegister> BEFORE_UNIT_REGISTER =
            RustedFabricEvent.create(listeners -> unit -> {
                for (BeforeUnitRegister listener : listeners) {
                    listener.beforeUnitRegister(unit);
                }
            });

    public static final RustedFabricEvent<AfterUnitRegister> AFTER_UNIT_REGISTER =
            RustedFabricEvent.create(listeners -> unit -> {
                for (AfterUnitRegister listener : listeners) {
                    listener.afterUnitRegister(unit);
                }
            });

    public static final RustedFabricEvent<BeforeUnitUnregister> BEFORE_UNIT_UNREGISTER =
            RustedFabricEvent.create(listeners -> unit -> {
                for (BeforeUnitUnregister listener : listeners) {
                    listener.beforeUnitUnregister(unit);
                }
            });

    public static final RustedFabricEvent<AfterUnitUnregister> AFTER_UNIT_UNREGISTER =
            RustedFabricEvent.create(listeners -> unit -> {
                for (AfterUnitUnregister listener : listeners) {
                    listener.afterUnitUnregister(unit);
                }
            });

    private UnitLifecycleEvents() {
    }

    /** Registers a permanent typed listener without exposing mapped game classes. */
    public static void registerAfterUnitAdded(Consumer<? super UnitView> listener) {
        if (listener == null) throw new IllegalArgumentException("listener must not be null");
        AFTER_UNIT_REGISTER.register(unit -> listener.accept(Units.view(unit)));
    }

    /** Registers a removable typed listener without exposing mapped game classes. */
    public static RustedFabricEvent.Registration subscribeAfterUnitAdded(
            Consumer<? super UnitView> listener) {
        if (listener == null) throw new IllegalArgumentException("listener must not be null");
        return AFTER_UNIT_REGISTER.subscribe(unit -> listener.accept(Units.view(unit)));
    }

    public static void registerBeforeUnitRemoved(Consumer<? super UnitView> listener) {
        if (listener == null) throw new IllegalArgumentException("listener must not be null");
        BEFORE_UNIT_UNREGISTER.register(unit -> listener.accept(Units.view(unit)));
    }

    public static RustedFabricEvent.Registration subscribeBeforeUnitRemoved(
            Consumer<? super UnitView> listener) {
        if (listener == null) throw new IllegalArgumentException("listener must not be null");
        return BEFORE_UNIT_UNREGISTER.subscribe(unit -> listener.accept(Units.view(unit)));
    }

    @FunctionalInterface
    public interface BeforeUnitRegister {
        void beforeUnitRegister(Object unit);
    }

    @FunctionalInterface
    public interface AfterUnitRegister {
        void afterUnitRegister(Object unit);
    }

    @FunctionalInterface
    public interface BeforeUnitUnregister {
        void beforeUnitUnregister(Object unit);
    }

    @FunctionalInterface
    public interface AfterUnitUnregister {
        void afterUnitUnregister(Object unit);
    }
}
