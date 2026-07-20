package io.github.endx.rustedfabricapi.api.client;

import io.github.endx.rustedfabricapi.api.unit.Units;
import rustedwarfare.core.GameEngine;
import rustedwarfare.ui.InterfaceEngine;
import rustedwarfare.unit.OrderableUnit;
import rustedwarfare.unit.Unit;
import rustedwarfare.util.UnitArrayList;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Typed access to the local player's current unit selection. */
public final class Selection {
    private Selection() {
    }

    /** Returns the current HUD/interface engine, or {@code null} before it is initialized. */
    public static InterfaceEngine getInterfaceOrNull() {
        GameEngine engine = RustedWarfareClient.getEngine();
        return engine != null ? engine.gameUI : null;
    }

    public static InterfaceEngine requireInterface() {
        InterfaceEngine gameInterface = getInterfaceOrNull();
        if (gameInterface == null) {
            throw new IllegalStateException("The Rusted Warfare interface is not initialized yet");
        }
        return gameInterface;
    }

    /** Returns an immutable point-in-time copy of the selected units. */
    public static List<Unit> snapshot() {
        InterfaceEngine gameInterface = getInterfaceOrNull();
        UnitArrayList selectedUnits = gameInterface != null ? gameInterface.selectedUnits : null;
        return selectedUnits == null ? Collections.emptyList() : Units.snapshot(selectedUnits);
    }

    public static Optional<OrderableUnit> firstOrderable() {
        InterfaceEngine gameInterface = getInterfaceOrNull();
        return Optional.ofNullable(gameInterface != null
                ? gameInterface.getFirstSelectedOrderableUnit() : null);
    }

    public static Optional<OrderableUnit> primaryOrderable() {
        InterfaceEngine gameInterface = getInterfaceOrNull();
        return Optional.ofNullable(gameInterface != null
                ? gameInterface.getPrimarySelectedOrderableUnit() : null);
    }

    /** Adds a unit through the game's normal selection path. Call from the update thread. */
    public static boolean add(Unit unit) {
        return requireInterface().addUnitToSelection(Objects.requireNonNull(unit, "unit"));
    }

    /** Selects a unit through the game's normal selection path. Call from the update thread. */
    public static void select(Unit unit, boolean append) {
        requireInterface().selectUnit(Objects.requireNonNull(unit, "unit"), append);
    }

    /** Deselects a unit through the game's normal selection path. Call from the update thread. */
    public static void deselect(Unit unit) {
        requireInterface().deselectUnit(Objects.requireNonNull(unit, "unit"));
    }

    /** Clears the current selection through the game's normal path. Call from the update thread. */
    public static void clear() {
        requireInterface().clearSelectedUnits();
    }
}
