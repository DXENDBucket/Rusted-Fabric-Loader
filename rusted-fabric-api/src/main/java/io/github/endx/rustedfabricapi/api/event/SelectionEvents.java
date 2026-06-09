package io.github.endx.rustedfabricapi.api.event;

public final class SelectionEvents {
    public static final RustedFabricEvent<BeforeUnitSelect> BEFORE_UNIT_SELECT =
            RustedFabricEvent.create(listeners -> (interfaceEngine, unit, append) -> {
                boolean cancelled = false;
                for (BeforeUnitSelect listener : listeners) {
                    cancelled |= listener.beforeUnitSelect(interfaceEngine, unit, append);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterUnitSelect> AFTER_UNIT_SELECT =
            RustedFabricEvent.create(listeners -> (interfaceEngine, unit, append) -> {
                for (AfterUnitSelect listener : listeners) {
                    listener.afterUnitSelect(interfaceEngine, unit, append);
                }
            });

    public static final RustedFabricEvent<BeforeUnitAddedToSelection> BEFORE_UNIT_ADDED_TO_SELECTION =
            RustedFabricEvent.create(listeners -> (interfaceEngine, unit) -> {
                boolean cancelled = false;
                for (BeforeUnitAddedToSelection listener : listeners) {
                    cancelled |= listener.beforeUnitAddedToSelection(interfaceEngine, unit);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterUnitAddedToSelection> AFTER_UNIT_ADDED_TO_SELECTION =
            RustedFabricEvent.create(listeners -> (interfaceEngine, unit, result) -> {
                for (AfterUnitAddedToSelection listener : listeners) {
                    listener.afterUnitAddedToSelection(interfaceEngine, unit, result);
                }
            });

    public static final RustedFabricEvent<BeforeUnitDeselect> BEFORE_UNIT_DESELECT =
            RustedFabricEvent.create(listeners -> (interfaceEngine, unit) -> {
                boolean cancelled = false;
                for (BeforeUnitDeselect listener : listeners) {
                    cancelled |= listener.beforeUnitDeselect(interfaceEngine, unit);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterUnitDeselect> AFTER_UNIT_DESELECT =
            RustedFabricEvent.create(listeners -> (interfaceEngine, unit) -> {
                for (AfterUnitDeselect listener : listeners) {
                    listener.afterUnitDeselect(interfaceEngine, unit);
                }
            });

    public static final RustedFabricEvent<BeforeSelectionClear> BEFORE_SELECTION_CLEAR =
            RustedFabricEvent.create(listeners -> interfaceEngine -> {
                boolean cancelled = false;
                for (BeforeSelectionClear listener : listeners) {
                    cancelled |= listener.beforeSelectionClear(interfaceEngine);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterSelectionClear> AFTER_SELECTION_CLEAR =
            RustedFabricEvent.create(listeners -> interfaceEngine -> {
                for (AfterSelectionClear listener : listeners) {
                    listener.afterSelectionClear(interfaceEngine);
                }
            });

    private SelectionEvents() {
    }

    @FunctionalInterface
    public interface BeforeUnitSelect {
        boolean beforeUnitSelect(Object interfaceEngine, Object unit, boolean append);
    }

    @FunctionalInterface
    public interface AfterUnitSelect {
        void afterUnitSelect(Object interfaceEngine, Object unit, boolean append);
    }

    @FunctionalInterface
    public interface BeforeUnitAddedToSelection {
        boolean beforeUnitAddedToSelection(Object interfaceEngine, Object unit);
    }

    @FunctionalInterface
    public interface AfterUnitAddedToSelection {
        void afterUnitAddedToSelection(Object interfaceEngine, Object unit, boolean result);
    }

    @FunctionalInterface
    public interface BeforeUnitDeselect {
        boolean beforeUnitDeselect(Object interfaceEngine, Object unit);
    }

    @FunctionalInterface
    public interface AfterUnitDeselect {
        void afterUnitDeselect(Object interfaceEngine, Object unit);
    }

    @FunctionalInterface
    public interface BeforeSelectionClear {
        boolean beforeSelectionClear(Object interfaceEngine);
    }

    @FunctionalInterface
    public interface AfterSelectionClear {
        void afterSelectionClear(Object interfaceEngine);
    }
}
