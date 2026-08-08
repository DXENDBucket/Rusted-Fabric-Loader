package io.github.endx.rustedfabricapi.api.custom.event;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import rustedwarfare.custom.CustomUnit;
import rustedwarfare.custom.CustomUnitMetadata;
import rustedwarfare.custom.MutableStatAccessor;

/** Strongly typed metadata, death, and removal events for custom-unit instances. */
public final class CustomUnitLifecycleEvents {
    public static final RustedFabricEvent<MetadataApply> BEFORE_METADATA_APPLY = metadataEvent();
    public static final RustedFabricEvent<MetadataApply> AFTER_METADATA_APPLY = metadataEvent();
    public static final RustedFabricEvent<BeforeUnit> BEFORE_KILLED = beforeUnitEvent();
    public static final RustedFabricEvent<AfterUnit> AFTER_KILLED = afterUnitEvent();
    public static final RustedFabricEvent<BeforeUnit> BEFORE_REMOVED = beforeUnitEvent();
    public static final RustedFabricEvent<AfterUnit> AFTER_REMOVED = afterUnitEvent();

    private CustomUnitLifecycleEvents() {
    }

    private static RustedFabricEvent<MetadataApply> metadataEvent() {
        return RustedFabricEvent.create(listeners ->
                (unit, oldMetadata, newMetadata, conversion, initial, statOverrides) -> {
                    for (MetadataApply listener : listeners) {
                        listener.onMetadataApply(unit, oldMetadata, newMetadata,
                                conversion, initial, statOverrides);
                    }
                });
    }

    private static RustedFabricEvent<BeforeUnit> beforeUnitEvent() {
        return RustedFabricEvent.create(listeners -> unit -> {
            boolean cancelled = false;
            for (BeforeUnit listener : listeners) {
                cancelled |= listener.beforeUnit(unit);
            }
            return cancelled;
        });
    }

    private static RustedFabricEvent<AfterUnit> afterUnitEvent() {
        return RustedFabricEvent.create(listeners -> unit -> {
            for (AfterUnit listener : listeners) {
                listener.afterUnit(unit);
            }
        });
    }

    @FunctionalInterface
    public interface MetadataApply {
        void onMetadataApply(CustomUnit unit, CustomUnitMetadata oldMetadata,
                             CustomUnitMetadata newMetadata, boolean conversion, boolean initial,
                             MutableStatAccessor[] statOverrides);
    }

    @FunctionalInterface
    public interface BeforeUnit {
        /** Return {@code true} to cancel the death/removal operation. */
        boolean beforeUnit(CustomUnit unit);
    }

    @FunctionalInterface
    public interface AfterUnit {
        void afterUnit(CustomUnit unit);
    }
}
