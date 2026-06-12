package io.github.endx.rustedfabricapi.api.event;

public final class TransportEvents {
    private TransportEvents() {
    }

    public static final RustedFabricEvent<ModifyCanTransportUnit> MODIFY_CAN_TRANSPORT_UNIT =
            RustedFabricEvent.create(listeners -> (carrier, candidate, allowPartial, vanillaResult) -> {
                Boolean result = Boolean.valueOf(vanillaResult);
                for (ModifyCanTransportUnit listener : listeners) {
                    Boolean override = listener.modifyCanTransportUnit(carrier, candidate, allowPartial,
                            result.booleanValue());
                    if (override != null) {
                        result = override;
                    }
                }
                return result;
            });

    public static final RustedFabricEvent<ModifyCanTransportUnitIgnoringCurrentContainer> MODIFY_CAN_TRANSPORT_UNIT_IGNORING_CURRENT_CONTAINER =
            RustedFabricEvent.create(listeners -> (carrier, candidate, allowPartial, vanillaResult) -> {
                Boolean result = Boolean.valueOf(vanillaResult);
                for (ModifyCanTransportUnitIgnoringCurrentContainer listener : listeners) {
                    Boolean override = listener.modifyCanTransportUnitIgnoringCurrentContainer(carrier, candidate,
                            allowPartial, result.booleanValue());
                    if (override != null) {
                        result = override;
                    }
                }
                return result;
            });

    public static final RustedFabricEvent<BeforeTryAddUnitToTransport> BEFORE_TRY_ADD_UNIT_TO_TRANSPORT =
            RustedFabricEvent.create(listeners -> (carrier, candidate, allowPartial) -> {
                boolean cancelled = false;
                for (BeforeTryAddUnitToTransport listener : listeners) {
                    cancelled |= listener.beforeTryAddUnitToTransport(carrier, candidate, allowPartial);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterTryAddUnitToTransport> AFTER_TRY_ADD_UNIT_TO_TRANSPORT =
            RustedFabricEvent.create(listeners -> (carrier, candidate, allowPartial, result) -> {
                for (AfterTryAddUnitToTransport listener : listeners) {
                    listener.afterTryAddUnitToTransport(carrier, candidate, allowPartial, result);
                }
            });

    public static final RustedFabricEvent<BeforeAddUnitToTransport> BEFORE_ADD_UNIT_TO_TRANSPORT =
            RustedFabricEvent.create(listeners -> (carrier, transportedUnit) -> {
                boolean cancelled = false;
                for (BeforeAddUnitToTransport listener : listeners) {
                    cancelled |= listener.beforeAddUnitToTransport(carrier, transportedUnit);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterAddUnitToTransport> AFTER_ADD_UNIT_TO_TRANSPORT =
            RustedFabricEvent.create(listeners -> (carrier, transportedUnit) -> {
                for (AfterAddUnitToTransport listener : listeners) {
                    listener.afterAddUnitToTransport(carrier, transportedUnit);
                }
            });

    public static final RustedFabricEvent<BeforeRemoveUnitFromTransport> BEFORE_REMOVE_UNIT_FROM_TRANSPORT =
            RustedFabricEvent.create(listeners -> (carrier, transportedUnit) -> {
                boolean cancelled = false;
                for (BeforeRemoveUnitFromTransport listener : listeners) {
                    cancelled |= listener.beforeRemoveUnitFromTransport(carrier, transportedUnit);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterRemoveUnitFromTransport> AFTER_REMOVE_UNIT_FROM_TRANSPORT =
            RustedFabricEvent.create(listeners -> (carrier, transportedUnit) -> {
                for (AfterRemoveUnitFromTransport listener : listeners) {
                    listener.afterRemoveUnitFromTransport(carrier, transportedUnit);
                }
            });

    public static final RustedFabricEvent<ModifyHasTransportCapacity> MODIFY_HAS_TRANSPORT_CAPACITY =
            RustedFabricEvent.create(listeners -> (unit, vanillaResult) -> {
                Boolean result = Boolean.valueOf(vanillaResult);
                for (ModifyHasTransportCapacity listener : listeners) {
                    Boolean override = listener.modifyHasTransportCapacity(unit, result.booleanValue());
                    if (override != null) {
                        result = override;
                    }
                }
                return result;
            });

    public static final RustedFabricEvent<ModifyTransportSlotsNeeded> MODIFY_TRANSPORT_SLOTS_NEEDED =
            RustedFabricEvent.create(listeners -> (unit, vanillaSlots) -> {
                Integer result = Integer.valueOf(vanillaSlots);
                for (ModifyTransportSlotsNeeded listener : listeners) {
                    Integer override = listener.modifyTransportSlotsNeeded(unit, result.intValue());
                    if (override != null) {
                        result = override;
                    }
                }
                return result;
            });

    public static final RustedFabricEvent<ModifyTransportBarSlots> MODIFY_TRANSPORT_BAR_USED_SLOTS =
            RustedFabricEvent.create(listeners -> (unit, vanillaSlots) -> {
                Integer result = Integer.valueOf(vanillaSlots);
                for (ModifyTransportBarSlots listener : listeners) {
                    Integer override = listener.modifyTransportBarSlots(unit, result.intValue());
                    if (override != null) {
                        result = override;
                    }
                }
                return result;
            });

    public static final RustedFabricEvent<ModifyTransportBarSlots> MODIFY_TRANSPORT_BAR_MAX_SLOTS =
            RustedFabricEvent.create(listeners -> (unit, vanillaSlots) -> {
                Integer result = Integer.valueOf(vanillaSlots);
                for (ModifyTransportBarSlots listener : listeners) {
                    Integer override = listener.modifyTransportBarSlots(unit, result.intValue());
                    if (override != null) {
                        result = override;
                    }
                }
                return result;
            });

    public static final RustedFabricEvent<ModifyTransportedUnitCount> MODIFY_TRANSPORTED_UNIT_COUNT =
            RustedFabricEvent.create(listeners -> (unit, vanillaCount) -> {
                Integer result = Integer.valueOf(vanillaCount);
                for (ModifyTransportedUnitCount listener : listeners) {
                    Integer override = listener.modifyTransportedUnitCount(unit, result.intValue());
                    if (override != null) {
                        result = override;
                    }
                }
                return result;
            });

    public static final RustedFabricEvent<ModifyTransportUnloading> MODIFY_TRANSPORT_UNLOADING =
            RustedFabricEvent.create(listeners -> (unit, vanillaResult) -> {
                Boolean result = Boolean.valueOf(vanillaResult);
                for (ModifyTransportUnloading listener : listeners) {
                    Boolean override = listener.modifyTransportUnloading(unit, result.booleanValue());
                    if (override != null) {
                        result = override;
                    }
                }
                return result;
            });

    public static final RustedFabricEvent<ModifyContainingUnit> MODIFY_CONTAINING_UNIT =
            RustedFabricEvent.create(listeners -> (unit, currentContainer) -> {
                Object result = currentContainer;
                for (ModifyContainingUnit listener : listeners) {
                    Object override = listener.modifyContainingUnit(unit, result);
                    if (override != null) {
                        result = override;
                    }
                }
                return result;
            });

    public static final RustedFabricEvent<ModifyAttachmentSlot> MODIFY_ATTACHMENT_SLOT =
            RustedFabricEvent.create(listeners -> (unit, currentSlot) -> {
                Object result = currentSlot;
                for (ModifyAttachmentSlot listener : listeners) {
                    Object override = listener.modifyAttachmentSlot(unit, result);
                    if (override != null) {
                        result = override;
                    }
                }
                return result;
            });

    public static final RustedFabricEvent<BeforeTransportUnloadingCommand> BEFORE_START_TRANSPORT_UNLOADING =
            RustedFabricEvent.create(listeners -> unit -> {
                boolean cancelled = false;
                for (BeforeTransportUnloadingCommand listener : listeners) {
                    cancelled |= listener.beforeTransportUnloadingCommand(unit);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterTransportUnloadingCommand> AFTER_START_TRANSPORT_UNLOADING =
            RustedFabricEvent.create(listeners -> unit -> {
                for (AfterTransportUnloadingCommand listener : listeners) {
                    listener.afterTransportUnloadingCommand(unit);
                }
            });

    public static final RustedFabricEvent<BeforeTransportUnloadingCommand> BEFORE_STOP_TRANSPORT_UNLOADING =
            RustedFabricEvent.create(listeners -> unit -> {
                boolean cancelled = false;
                for (BeforeTransportUnloadingCommand listener : listeners) {
                    cancelled |= listener.beforeTransportUnloadingCommand(unit);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterTransportUnloadingCommand> AFTER_STOP_TRANSPORT_UNLOADING =
            RustedFabricEvent.create(listeners -> unit -> {
                for (AfterTransportUnloadingCommand listener : listeners) {
                    listener.afterTransportUnloadingCommand(unit);
                }
            });

    public static final RustedFabricEvent<BeforeUnloadNextTransportedUnit> BEFORE_UNLOAD_NEXT_TRANSPORTED_UNIT =
            RustedFabricEvent.create(listeners -> (unit, forced) -> {
                boolean cancelled = false;
                for (BeforeUnloadNextTransportedUnit listener : listeners) {
                    cancelled |= listener.beforeUnloadNextTransportedUnit(unit, forced);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterUnloadNextTransportedUnit> AFTER_UNLOAD_NEXT_TRANSPORTED_UNIT =
            RustedFabricEvent.create(listeners -> (unit, forced, result) -> {
                for (AfterUnloadNextTransportedUnit listener : listeners) {
                    listener.afterUnloadNextTransportedUnit(unit, forced, result);
                }
            });

    public static final RustedFabricEvent<BeforeUnloadSpecificTransportedUnit> BEFORE_UNLOAD_SPECIFIC_TRANSPORTED_UNIT =
            RustedFabricEvent.create(listeners -> (unit, transportedUnit, optionA, optionB) -> {
                boolean cancelled = false;
                for (BeforeUnloadSpecificTransportedUnit listener : listeners) {
                    cancelled |= listener.beforeUnloadSpecificTransportedUnit(unit, transportedUnit, optionA, optionB);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterUnloadSpecificTransportedUnit> AFTER_UNLOAD_SPECIFIC_TRANSPORTED_UNIT =
            RustedFabricEvent.create(listeners -> (unit, transportedUnit, optionA, optionB, result) -> {
                for (AfterUnloadSpecificTransportedUnit listener : listeners) {
                    listener.afterUnloadSpecificTransportedUnit(unit, transportedUnit, optionA, optionB, result);
                }
            });

    public static final RustedFabricEvent<BeforeReleaseAllTransportedUnits> BEFORE_RELEASE_ALL_TRANSPORTED_UNITS =
            RustedFabricEvent.create(listeners -> (unit, killUnits) -> {
                boolean cancelled = false;
                for (BeforeReleaseAllTransportedUnits listener : listeners) {
                    cancelled |= listener.beforeReleaseAllTransportedUnits(unit, killUnits);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterReleaseAllTransportedUnits> AFTER_RELEASE_ALL_TRANSPORTED_UNITS =
            RustedFabricEvent.create(listeners -> (unit, killUnits) -> {
                for (AfterReleaseAllTransportedUnits listener : listeners) {
                    listener.afterReleaseAllTransportedUnits(unit, killUnits);
                }
            });

    public static final RustedFabricEvent<BeforeTransportDeathCargoCleanup> BEFORE_TRANSPORT_DEATH_CARGO_CLEANUP =
            RustedFabricEvent.create(listeners -> unit -> {
                boolean cancelled = false;
                for (BeforeTransportDeathCargoCleanup listener : listeners) {
                    cancelled |= listener.beforeTransportDeathCargoCleanup(unit);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterTransportDeathCargoCleanup> AFTER_TRANSPORT_DEATH_CARGO_CLEANUP =
            RustedFabricEvent.create(listeners -> unit -> {
                for (AfterTransportDeathCargoCleanup listener : listeners) {
                    listener.afterTransportDeathCargoCleanup(unit);
                }
            });

    @FunctionalInterface
    public interface ModifyCanTransportUnit {
        Boolean modifyCanTransportUnit(Object carrier, Object candidate, boolean allowPartial, boolean currentResult);
    }

    @FunctionalInterface
    public interface ModifyCanTransportUnitIgnoringCurrentContainer {
        Boolean modifyCanTransportUnitIgnoringCurrentContainer(Object carrier, Object candidate,
                                                               boolean allowPartial, boolean currentResult);
    }

    @FunctionalInterface
    public interface BeforeTryAddUnitToTransport {
        boolean beforeTryAddUnitToTransport(Object carrier, Object candidate, boolean allowPartial);
    }

    @FunctionalInterface
    public interface AfterTryAddUnitToTransport {
        void afterTryAddUnitToTransport(Object carrier, Object candidate, boolean allowPartial, boolean result);
    }

    @FunctionalInterface
    public interface BeforeAddUnitToTransport {
        boolean beforeAddUnitToTransport(Object carrier, Object transportedUnit);
    }

    @FunctionalInterface
    public interface AfterAddUnitToTransport {
        void afterAddUnitToTransport(Object carrier, Object transportedUnit);
    }

    @FunctionalInterface
    public interface BeforeRemoveUnitFromTransport {
        boolean beforeRemoveUnitFromTransport(Object carrier, Object transportedUnit);
    }

    @FunctionalInterface
    public interface AfterRemoveUnitFromTransport {
        void afterRemoveUnitFromTransport(Object carrier, Object transportedUnit);
    }

    @FunctionalInterface
    public interface ModifyHasTransportCapacity {
        Boolean modifyHasTransportCapacity(Object unit, boolean currentResult);
    }

    @FunctionalInterface
    public interface ModifyTransportSlotsNeeded {
        Integer modifyTransportSlotsNeeded(Object unit, int currentSlots);
    }

    @FunctionalInterface
    public interface ModifyTransportBarSlots {
        Integer modifyTransportBarSlots(Object unit, int currentSlots);
    }

    @FunctionalInterface
    public interface ModifyTransportedUnitCount {
        Integer modifyTransportedUnitCount(Object unit, int currentCount);
    }

    @FunctionalInterface
    public interface ModifyTransportUnloading {
        Boolean modifyTransportUnloading(Object unit, boolean currentResult);
    }

    @FunctionalInterface
    public interface ModifyContainingUnit {
        Object modifyContainingUnit(Object unit, Object currentContainer);
    }

    @FunctionalInterface
    public interface ModifyAttachmentSlot {
        Object modifyAttachmentSlot(Object unit, Object currentSlot);
    }

    @FunctionalInterface
    public interface BeforeTransportUnloadingCommand {
        boolean beforeTransportUnloadingCommand(Object unit);
    }

    @FunctionalInterface
    public interface AfterTransportUnloadingCommand {
        void afterTransportUnloadingCommand(Object unit);
    }

    @FunctionalInterface
    public interface BeforeUnloadNextTransportedUnit {
        boolean beforeUnloadNextTransportedUnit(Object unit, boolean forced);
    }

    @FunctionalInterface
    public interface AfterUnloadNextTransportedUnit {
        void afterUnloadNextTransportedUnit(Object unit, boolean forced, boolean result);
    }

    @FunctionalInterface
    public interface BeforeUnloadSpecificTransportedUnit {
        boolean beforeUnloadSpecificTransportedUnit(Object unit, Object transportedUnit,
                                                    boolean optionA, boolean optionB);
    }

    @FunctionalInterface
    public interface AfterUnloadSpecificTransportedUnit {
        void afterUnloadSpecificTransportedUnit(Object unit, Object transportedUnit,
                                                boolean optionA, boolean optionB, boolean result);
    }

    @FunctionalInterface
    public interface BeforeReleaseAllTransportedUnits {
        boolean beforeReleaseAllTransportedUnits(Object unit, boolean killUnits);
    }

    @FunctionalInterface
    public interface AfterReleaseAllTransportedUnits {
        void afterReleaseAllTransportedUnits(Object unit, boolean killUnits);
    }

    @FunctionalInterface
    public interface BeforeTransportDeathCargoCleanup {
        boolean beforeTransportDeathCargoCleanup(Object unit);
    }

    @FunctionalInterface
    public interface AfterTransportDeathCargoCleanup {
        void afterTransportDeathCargoCleanup(Object unit);
    }
}
