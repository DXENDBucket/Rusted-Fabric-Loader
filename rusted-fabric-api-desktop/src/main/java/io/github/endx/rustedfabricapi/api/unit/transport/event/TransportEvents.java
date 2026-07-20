package io.github.endx.rustedfabricapi.api.unit.transport.event;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import rustedwarfare.custom.attachment.AttachmentSlot;
import rustedwarfare.unit.Unit;

/** Strongly typed transport capacity, cargo and unloading events. */
public final class TransportEvents {
    public static final RustedFabricEvent<ModifyCanLoad> MODIFY_CAN_LOAD =
            RustedFabricEvent.create(listeners -> (carrier, candidate, allowPartial, current) -> {
                Boolean result = Boolean.valueOf(current);
                for (ModifyCanLoad listener : listeners) {
                    Boolean replacement = listener.modify(carrier, candidate, allowPartial,
                            result.booleanValue());
                    if (replacement != null) result = replacement;
                }
                return result;
            });
    public static final RustedFabricEvent<ModifyCanLoad> MODIFY_CAN_LOAD_IGNORING_CURRENT_CONTAINER =
            RustedFabricEvent.create(listeners -> (carrier, candidate, allowPartial, current) -> {
                Boolean result = Boolean.valueOf(current);
                for (ModifyCanLoad listener : listeners) {
                    Boolean replacement = listener.modify(carrier, candidate, allowPartial,
                            result.booleanValue());
                    if (replacement != null) result = replacement;
                }
                return result;
            });
    public static final RustedFabricEvent<BeforeTryLoad> BEFORE_TRY_LOAD =
            RustedFabricEvent.create(listeners -> (carrier, candidate, allowPartial) -> {
                boolean cancelled = false;
                for (BeforeTryLoad listener : listeners) {
                    cancelled |= listener.beforeTryLoad(carrier, candidate, allowPartial);
                }
                return cancelled;
            });
    public static final RustedFabricEvent<AfterTryLoad> AFTER_TRY_LOAD =
            RustedFabricEvent.create(listeners -> (carrier, candidate, allowPartial, result) -> {
                for (AfterTryLoad listener : listeners) {
                    listener.afterTryLoad(carrier, candidate, allowPartial, result);
                }
            });
    public static final RustedFabricEvent<BeforeCargoChange> BEFORE_LOAD = beforeCargoChangeEvent();
    public static final RustedFabricEvent<CargoChange> AFTER_LOAD = cargoChangeEvent();
    public static final RustedFabricEvent<BeforeCargoChange> BEFORE_REMOVE = beforeCargoChangeEvent();
    public static final RustedFabricEvent<CargoChange> AFTER_REMOVE = cargoChangeEvent();

    public static final RustedFabricEvent<ModifyBoolean> MODIFY_HAS_CAPACITY = booleanEvent();
    public static final RustedFabricEvent<ModifyInteger> MODIFY_SLOTS_NEEDED = integerEvent();
    public static final RustedFabricEvent<ModifyInteger> MODIFY_USED_SLOTS = integerEvent();
    public static final RustedFabricEvent<ModifyInteger> MODIFY_MAX_SLOTS = integerEvent();
    public static final RustedFabricEvent<ModifyInteger> MODIFY_CARGO_COUNT = integerEvent();
    public static final RustedFabricEvent<ModifyBoolean> MODIFY_IS_UNLOADING = booleanEvent();
    public static final RustedFabricEvent<ModifyContainingUnit> MODIFY_CONTAINING_UNIT =
            RustedFabricEvent.create(listeners -> (unit, current) -> {
                Unit result = current;
                for (ModifyContainingUnit listener : listeners) {
                    Unit replacement = listener.modify(unit, result);
                    if (replacement != null) result = replacement;
                }
                return result;
            });
    public static final RustedFabricEvent<ModifyAttachmentSlot> MODIFY_ATTACHMENT_SLOT =
            RustedFabricEvent.create(listeners -> (unit, current) -> {
                AttachmentSlot result = current;
                for (ModifyAttachmentSlot listener : listeners) {
                    AttachmentSlot replacement = listener.modify(unit, result);
                    if (replacement != null) result = replacement;
                }
                return result;
            });

    public static final RustedFabricEvent<BeforeCommand> BEFORE_START_UNLOADING = beforeCommandEvent();
    public static final RustedFabricEvent<AfterCommand> AFTER_START_UNLOADING = commandEvent();
    public static final RustedFabricEvent<BeforeCommand> BEFORE_STOP_UNLOADING = beforeCommandEvent();
    public static final RustedFabricEvent<AfterCommand> AFTER_STOP_UNLOADING = commandEvent();
    public static final RustedFabricEvent<BeforeUnloadNext> BEFORE_UNLOAD_NEXT =
            RustedFabricEvent.create(listeners -> (carrier, forced) -> {
                boolean cancelled = false;
                for (BeforeUnloadNext listener : listeners) {
                    cancelled |= listener.beforeUnloadNext(carrier, forced);
                }
                return cancelled;
            });
    public static final RustedFabricEvent<AfterUnloadNext> AFTER_UNLOAD_NEXT =
            RustedFabricEvent.create(listeners -> (carrier, forced, result) -> {
                for (AfterUnloadNext listener : listeners) {
                    listener.afterUnloadNext(carrier, forced, result);
                }
            });
    public static final RustedFabricEvent<BeforeUnloadSpecific> BEFORE_UNLOAD_SPECIFIC =
            RustedFabricEvent.create(listeners -> (carrier, cargo, forcePlacement, alternateSide) -> {
                boolean cancelled = false;
                for (BeforeUnloadSpecific listener : listeners) {
                    cancelled |= listener.beforeUnloadSpecific(
                            carrier, cargo, forcePlacement, alternateSide);
                }
                return cancelled;
            });
    public static final RustedFabricEvent<AfterUnloadSpecific> AFTER_UNLOAD_SPECIFIC =
            RustedFabricEvent.create(listeners ->
                    (carrier, cargo, forcePlacement, alternateSide, result) -> {
                for (AfterUnloadSpecific listener : listeners) {
                    listener.afterUnloadSpecific(
                            carrier, cargo, forcePlacement, alternateSide, result);
                }
            });
    public static final RustedFabricEvent<BeforeReleaseAll> BEFORE_RELEASE_ALL =
            RustedFabricEvent.create(listeners -> (carrier, killUnits) -> {
                boolean cancelled = false;
                for (BeforeReleaseAll listener : listeners) {
                    cancelled |= listener.beforeReleaseAll(carrier, killUnits);
                }
                return cancelled;
            });
    public static final RustedFabricEvent<AfterReleaseAll> AFTER_RELEASE_ALL =
            RustedFabricEvent.create(listeners -> (carrier, killUnits) -> {
                for (AfterReleaseAll listener : listeners) listener.afterReleaseAll(carrier, killUnits);
            });
    public static final RustedFabricEvent<BeforeCommand> BEFORE_DEATH_CARGO_CLEANUP = beforeCommandEvent();
    public static final RustedFabricEvent<AfterCommand> AFTER_DEATH_CARGO_CLEANUP = commandEvent();

    private TransportEvents() {
    }

    private static RustedFabricEvent<BeforeCargoChange> beforeCargoChangeEvent() {
        return RustedFabricEvent.create(listeners -> (carrier, cargo) -> {
            boolean cancelled = false;
            for (BeforeCargoChange listener : listeners) {
                cancelled |= listener.beforeCargoChange(carrier, cargo);
            }
            return cancelled;
        });
    }

    private static RustedFabricEvent<CargoChange> cargoChangeEvent() {
        return RustedFabricEvent.create(listeners -> (carrier, cargo) -> {
            for (CargoChange listener : listeners) listener.onCargoChange(carrier, cargo);
        });
    }

    private static RustedFabricEvent<ModifyBoolean> booleanEvent() {
        return RustedFabricEvent.create(listeners -> (unit, current) -> {
            Boolean result = Boolean.valueOf(current);
            for (ModifyBoolean listener : listeners) {
                Boolean replacement = listener.modify(unit, result.booleanValue());
                if (replacement != null) result = replacement;
            }
            return result;
        });
    }

    private static RustedFabricEvent<ModifyInteger> integerEvent() {
        return RustedFabricEvent.create(listeners -> (unit, current) -> {
            Integer result = Integer.valueOf(current);
            for (ModifyInteger listener : listeners) {
                Integer replacement = listener.modify(unit, result.intValue());
                if (replacement != null) result = replacement;
            }
            return result;
        });
    }

    private static RustedFabricEvent<BeforeCommand> beforeCommandEvent() {
        return RustedFabricEvent.create(listeners -> carrier -> {
            boolean cancelled = false;
            for (BeforeCommand listener : listeners) cancelled |= listener.beforeCommand(carrier);
            return cancelled;
        });
    }

    private static RustedFabricEvent<AfterCommand> commandEvent() {
        return RustedFabricEvent.create(listeners -> carrier -> {
            for (AfterCommand listener : listeners) listener.afterCommand(carrier);
        });
    }

    @FunctionalInterface
    public interface ModifyCanLoad {
        /** Return {@code null} to retain {@code currentResult}. */
        Boolean modify(Unit carrier, Unit candidate, boolean allowPartial, boolean currentResult);
    }

    @FunctionalInterface
    public interface BeforeTryLoad {
        boolean beforeTryLoad(Unit carrier, Unit candidate, boolean allowPartial);
    }

    @FunctionalInterface
    public interface AfterTryLoad {
        void afterTryLoad(Unit carrier, Unit candidate, boolean allowPartial, boolean result);
    }

    @FunctionalInterface
    public interface BeforeCargoChange {
        boolean beforeCargoChange(Unit carrier, Unit cargo);
    }

    @FunctionalInterface
    public interface CargoChange {
        void onCargoChange(Unit carrier, Unit cargo);
    }

    @FunctionalInterface
    public interface ModifyBoolean {
        /** Return {@code null} to retain {@code currentValue}. */
        Boolean modify(Unit unit, boolean currentValue);
    }

    @FunctionalInterface
    public interface ModifyInteger {
        /** Return {@code null} to retain {@code currentValue}. */
        Integer modify(Unit unit, int currentValue);
    }

    @FunctionalInterface
    public interface ModifyContainingUnit {
        /** Return {@code null} to retain {@code currentContainer}. */
        Unit modify(Unit unit, Unit currentContainer);
    }

    @FunctionalInterface
    public interface ModifyAttachmentSlot {
        /** Return {@code null} to retain {@code currentSlot}. */
        AttachmentSlot modify(Unit unit, AttachmentSlot currentSlot);
    }

    @FunctionalInterface
    public interface BeforeCommand {
        boolean beforeCommand(Unit carrier);
    }

    @FunctionalInterface
    public interface AfterCommand {
        void afterCommand(Unit carrier);
    }

    @FunctionalInterface
    public interface BeforeUnloadNext {
        boolean beforeUnloadNext(Unit carrier, boolean forced);
    }

    @FunctionalInterface
    public interface AfterUnloadNext {
        void afterUnloadNext(Unit carrier, boolean forced, boolean result);
    }

    @FunctionalInterface
    public interface BeforeUnloadSpecific {
        boolean beforeUnloadSpecific(Unit carrier, Unit cargo,
                                     boolean forcePlacement, boolean alternateSide);
    }

    @FunctionalInterface
    public interface AfterUnloadSpecific {
        void afterUnloadSpecific(Unit carrier, Unit cargo,
                                 boolean forcePlacement, boolean alternateSide, boolean result);
    }

    @FunctionalInterface
    public interface BeforeReleaseAll {
        boolean beforeReleaseAll(Unit carrier, boolean killUnits);
    }

    @FunctionalInterface
    public interface AfterReleaseAll {
        void afterReleaseAll(Unit carrier, boolean killUnits);
    }
}
