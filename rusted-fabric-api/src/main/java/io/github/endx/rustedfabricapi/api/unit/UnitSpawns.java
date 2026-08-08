package io.github.endx.rustedfabricapi.api.unit;

import io.github.endx.rustedfabricapi.api.client.RustedWarfareClient;
import io.github.endx.rustedfabricapi.api.unit.event.UnitSpawnEvents;
import rustedwarfare.core.GameEngine;
import rustedwarfare.game.Team;
import rustedwarfare.unit.OrderableUnit;
import rustedwarfare.unit.Unit;
import rustedwarfare.unit.UnitType;

import java.util.Objects;
import java.util.Optional;

/** Consistent creation, registration, removal and death helpers for live units. */
public final class UnitSpawns {
    private UnitSpawns() {
    }

    public static Unit spawn(UnitType type, Team team, float x, float y) {
        return spawn(type, team, x, y, 0.0F, 0.0F);
    }

    public static Unit spawn(UnitType type, Team team, float x, float y,
            float height, float direction) {
        return trySpawn(type, team, x, y, height, direction).orElseThrow(() ->
                new IllegalStateException("Unit spawn was cancelled for " + type.getInternalName()));
    }

    /**
     * Creates a live unit through its {@link UnitType}, assigns position/team, registers team
     * accounting, and refreshes building path costs. Must run on the game update thread.
     */
    public static Optional<Unit> trySpawn(UnitType type, Team team, float x, float y,
            float height, float direction) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(team, "team");
        requireFinite(x, "x");
        requireFinite(y, "y");
        requireFinite(height, "height");
        requireFinite(direction, "direction");
        if (UnitSpawnEvents.BEFORE_SPAWN.invoker()
                .beforeSpawn(type, team, x, y, height, direction)) {
            return Optional.empty();
        }

        Unit unit = Objects.requireNonNull(type.createUnit(),
                "UnitType.createUnit returned null for " + type.getInternalName());
        boolean registered = false;
        try {
            unit.setTeamRaw(team);
            unit.x = x;
            unit.y = y;
            unit.height = height;
            if (!unit.isBuilding()) unit.setDirection(direction);
            Team.registerUnit(unit);
            registered = true;
            refreshBuildingPathCosts(unit);
            UnitSpawnEvents.AFTER_SPAWN.invoker().afterSpawn(unit, type, team);
            return Optional.of(unit);
        } catch (ThreadDeath | VirtualMachineError critical) {
            throw critical;
        } catch (Throwable failure) {
            if (!unit.removed) {
                try {
                    unit.removeFromGame();
                } catch (Throwable cleanupFailure) {
                    failure.addSuppressed(cleanupFailure);
                }
            }
            if (failure instanceof RuntimeException) throw (RuntimeException) failure;
            if (failure instanceof Error) throw (Error) failure;
            throw new IllegalStateException("Could not spawn " + type.getInternalName()
                    + (registered ? " after team registration" : ""), failure);
        }
    }

    /** Removes a unit without invoking its death effects. */
    public static boolean remove(Unit unit) {
        Objects.requireNonNull(unit, "unit");
        if (unit.removed) return false;
        unit.removeFromGame();
        return true;
    }

    public static boolean kill(Unit unit, boolean deathEffects) {
        Objects.requireNonNull(unit, "unit");
        if (unit.dead || unit.removed) return false;
        if (deathEffects) unit.killAndHandleDeathEffects();
        else unit.killWithoutDeathEffects();
        return true;
    }

    private static void refreshBuildingPathCosts(Unit unit) {
        if (!(unit instanceof OrderableUnit) || !unit.isBuilding()) return;
        GameEngine engine = RustedWarfareClient.getEngine();
        if (engine != null && engine.pathfindingEngine != null) {
            engine.pathfindingEngine.rebuildCostsAroundUnit((OrderableUnit) unit);
        }
    }

    private static void requireFinite(float value, String name) {
        if (!Float.isFinite(value)) throw new IllegalArgumentException(name + " must be finite");
    }
}
