package io.github.endx.rustedfabricapi.api.event;

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
