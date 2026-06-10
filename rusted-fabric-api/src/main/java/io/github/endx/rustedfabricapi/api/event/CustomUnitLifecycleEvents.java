package io.github.endx.rustedfabricapi.api.event;

public final class CustomUnitLifecycleEvents {
    private CustomUnitLifecycleEvents() {
    }

    public static final RustedFabricEvent<BeforeRuntimeUnitCreate> BEFORE_RUNTIME_UNIT_CREATE =
            RustedFabricEvent.create(listeners -> metadata -> {
                for (BeforeRuntimeUnitCreate listener : listeners) {
                    listener.beforeRuntimeUnitCreate(metadata);
                }
            });

    public static final RustedFabricEvent<AfterRuntimeUnitCreate> AFTER_RUNTIME_UNIT_CREATE =
            RustedFabricEvent.create(listeners -> (metadata, unit) -> {
                Object result = unit;
                for (AfterRuntimeUnitCreate listener : listeners) {
                    result = listener.afterRuntimeUnitCreate(metadata, result);
                }
                return result;
            });

    public static final RustedFabricEvent<BeforeRuntimeUnitCreateWithFlag> BEFORE_RUNTIME_UNIT_CREATE_WITH_FLAG =
            RustedFabricEvent.create(listeners -> (metadata, createFlag) -> {
                for (BeforeRuntimeUnitCreateWithFlag listener : listeners) {
                    listener.beforeRuntimeUnitCreateWithFlag(metadata, createFlag);
                }
            });

    public static final RustedFabricEvent<AfterRuntimeUnitCreateWithFlag> AFTER_RUNTIME_UNIT_CREATE_WITH_FLAG =
            RustedFabricEvent.create(listeners -> (metadata, createFlag, unit) -> {
                Object result = unit;
                for (AfterRuntimeUnitCreateWithFlag listener : listeners) {
                    result = listener.afterRuntimeUnitCreateWithFlag(metadata, createFlag, result);
                }
                return result;
            });

    public static final RustedFabricEvent<BeforeUnitMetadataApply> BEFORE_UNIT_METADATA_APPLY =
            RustedFabricEvent.create(listeners -> (unit, oldMetadata, newMetadata, conversion, initial, statOverrides) -> {
                for (BeforeUnitMetadataApply listener : listeners) {
                    listener.beforeUnitMetadataApply(unit, oldMetadata, newMetadata, conversion, initial, statOverrides);
                }
            });

    public static final RustedFabricEvent<AfterUnitMetadataApply> AFTER_UNIT_METADATA_APPLY =
            RustedFabricEvent.create(listeners -> (unit, oldMetadata, newMetadata, conversion, initial, statOverrides) -> {
                for (AfterUnitMetadataApply listener : listeners) {
                    listener.afterUnitMetadataApply(unit, oldMetadata, newMetadata, conversion, initial, statOverrides);
                }
            });

    public static final RustedFabricEvent<BeforeCustomUnitKilled> BEFORE_CUSTOM_UNIT_KILLED =
            RustedFabricEvent.create(listeners -> unit -> {
                boolean cancelled = false;
                for (BeforeCustomUnitKilled listener : listeners) {
                    cancelled |= listener.beforeCustomUnitKilled(unit);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterCustomUnitKilled> AFTER_CUSTOM_UNIT_KILLED =
            RustedFabricEvent.create(listeners -> unit -> {
                for (AfterCustomUnitKilled listener : listeners) {
                    listener.afterCustomUnitKilled(unit);
                }
            });

    public static final RustedFabricEvent<BeforeCustomUnitRemoved> BEFORE_CUSTOM_UNIT_REMOVED =
            RustedFabricEvent.create(listeners -> unit -> {
                boolean cancelled = false;
                for (BeforeCustomUnitRemoved listener : listeners) {
                    cancelled |= listener.beforeCustomUnitRemoved(unit);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterCustomUnitRemoved> AFTER_CUSTOM_UNIT_REMOVED =
            RustedFabricEvent.create(listeners -> unit -> {
                for (AfterCustomUnitRemoved listener : listeners) {
                    listener.afterCustomUnitRemoved(unit);
                }
            });

    @FunctionalInterface
    public interface BeforeRuntimeUnitCreate {
        void beforeRuntimeUnitCreate(Object metadata);
    }

    @FunctionalInterface
    public interface AfterRuntimeUnitCreate {
        Object afterRuntimeUnitCreate(Object metadata, Object unit);
    }

    @FunctionalInterface
    public interface BeforeRuntimeUnitCreateWithFlag {
        void beforeRuntimeUnitCreateWithFlag(Object metadata, boolean createFlag);
    }

    @FunctionalInterface
    public interface AfterRuntimeUnitCreateWithFlag {
        Object afterRuntimeUnitCreateWithFlag(Object metadata, boolean createFlag, Object unit);
    }

    @FunctionalInterface
    public interface BeforeUnitMetadataApply {
        void beforeUnitMetadataApply(Object unit, Object oldMetadata, Object newMetadata, boolean conversion, boolean initial, Object statOverrides);
    }

    @FunctionalInterface
    public interface AfterUnitMetadataApply {
        void afterUnitMetadataApply(Object unit, Object oldMetadata, Object newMetadata, boolean conversion, boolean initial, Object statOverrides);
    }

    @FunctionalInterface
    public interface BeforeCustomUnitKilled {
        boolean beforeCustomUnitKilled(Object unit);
    }

    @FunctionalInterface
    public interface AfterCustomUnitKilled {
        void afterCustomUnitKilled(Object unit);
    }

    @FunctionalInterface
    public interface BeforeCustomUnitRemoved {
        boolean beforeCustomUnitRemoved(Object unit);
    }

    @FunctionalInterface
    public interface AfterCustomUnitRemoved {
        void afterCustomUnitRemoved(Object unit);
    }
}
