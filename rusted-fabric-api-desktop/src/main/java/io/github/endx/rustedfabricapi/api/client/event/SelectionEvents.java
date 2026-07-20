package io.github.endx.rustedfabricapi.api.client.event;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import rustedwarfare.ui.InterfaceEngine;
import rustedwarfare.unit.Unit;

/** Strongly typed local-selection events. */
public final class SelectionEvents {
    public static final RustedFabricEvent<BeforeSelect> BEFORE_SELECT =
            RustedFabricEvent.create(listeners -> (gameInterface, unit, append) -> {
                boolean cancelled = false;
                for (BeforeSelect listener : listeners) {
                    cancelled |= listener.beforeSelect(gameInterface, unit, append);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterSelect> AFTER_SELECT =
            RustedFabricEvent.create(listeners -> (gameInterface, unit, append) -> {
                for (AfterSelect listener : listeners) {
                    listener.afterSelect(gameInterface, unit, append);
                }
            });

    public static final RustedFabricEvent<BeforeAdd> BEFORE_ADD =
            RustedFabricEvent.create(listeners -> (gameInterface, unit) -> {
                boolean cancelled = false;
                for (BeforeAdd listener : listeners) {
                    cancelled |= listener.beforeAdd(gameInterface, unit);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterAdd> AFTER_ADD =
            RustedFabricEvent.create(listeners -> (gameInterface, unit, added) -> {
                for (AfterAdd listener : listeners) {
                    listener.afterAdd(gameInterface, unit, added);
                }
            });

    public static final RustedFabricEvent<BeforeUnit> BEFORE_DESELECT = beforeUnitEvent();
    public static final RustedFabricEvent<AfterUnit> AFTER_DESELECT = afterUnitEvent();

    public static final RustedFabricEvent<BeforeClear> BEFORE_CLEAR =
            RustedFabricEvent.create(listeners -> gameInterface -> {
                boolean cancelled = false;
                for (BeforeClear listener : listeners) {
                    cancelled |= listener.beforeClear(gameInterface);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterClear> AFTER_CLEAR =
            RustedFabricEvent.create(listeners -> gameInterface -> {
                for (AfterClear listener : listeners) {
                    listener.afterClear(gameInterface);
                }
            });

    private SelectionEvents() {
    }

    private static RustedFabricEvent<BeforeUnit> beforeUnitEvent() {
        return RustedFabricEvent.create(listeners -> (gameInterface, unit) -> {
            boolean cancelled = false;
            for (BeforeUnit listener : listeners) {
                cancelled |= listener.beforeUnit(gameInterface, unit);
            }
            return cancelled;
        });
    }

    private static RustedFabricEvent<AfterUnit> afterUnitEvent() {
        return RustedFabricEvent.create(listeners -> (gameInterface, unit) -> {
            for (AfterUnit listener : listeners) {
                listener.afterUnit(gameInterface, unit);
            }
        });
    }

    @FunctionalInterface
    public interface BeforeSelect {
        boolean beforeSelect(InterfaceEngine gameInterface, Unit unit, boolean append);
    }

    @FunctionalInterface
    public interface AfterSelect {
        void afterSelect(InterfaceEngine gameInterface, Unit unit, boolean append);
    }

    @FunctionalInterface
    public interface BeforeAdd {
        boolean beforeAdd(InterfaceEngine gameInterface, Unit unit);
    }

    @FunctionalInterface
    public interface AfterAdd {
        void afterAdd(InterfaceEngine gameInterface, Unit unit, boolean added);
    }

    @FunctionalInterface
    public interface BeforeUnit {
        boolean beforeUnit(InterfaceEngine gameInterface, Unit unit);
    }

    @FunctionalInterface
    public interface AfterUnit {
        void afterUnit(InterfaceEngine gameInterface, Unit unit);
    }

    @FunctionalInterface
    public interface BeforeClear {
        boolean beforeClear(InterfaceEngine gameInterface);
    }

    @FunctionalInterface
    public interface AfterClear {
        void afterClear(InterfaceEngine gameInterface);
    }
}
