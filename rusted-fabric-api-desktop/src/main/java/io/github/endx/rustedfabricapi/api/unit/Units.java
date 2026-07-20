package io.github.endx.rustedfabricapi.api.unit;

import rustedwarfare.framework.GameObject;
import rustedwarfare.game.Team;
import rustedwarfare.unit.Unit;
import rustedwarfare.unit.UnitType;
import rustedwarfare.unit.OrderableUnit;
import rustedwarfare.util.UnitArrayList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** Query helpers for the live desktop unit registry. */
public final class Units {
    private Units() {
    }

    public static int size() {
        return Unit.allUnits.size();
    }

    /**
     * Takes a defensive snapshot of the global unit list.
     * The returned list is immutable, while the contained game objects remain live and mutable.
     */
    public static List<Unit> snapshot() {
        return snapshot(Unit.allUnits);
    }

    /** Takes a defensive snapshot of a game-owned unit list. */
    public static List<Unit> snapshot(UnitArrayList units) {
        Objects.requireNonNull(units, "units");
        Unit[] backing = units.getBackingArray();
        int size = Math.min(units.size(), backing.length);
        List<Unit> result = new ArrayList<Unit>(size);
        for (int index = 0; index < size; index++) {
            Unit unit = backing[index];
            if (unit != null) {
                result.add(unit);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public static List<Unit> snapshot(Predicate<? super Unit> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        List<Unit> result = new ArrayList<Unit>();
        for (Unit unit : snapshot()) {
            if (predicate.test(unit)) {
                result.add(unit);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /** Visits a point-in-time snapshot, avoiding mutation of the game's backing collection. */
    public static void forEach(Consumer<? super Unit> consumer) {
        Objects.requireNonNull(consumer, "consumer");
        snapshot().forEach(consumer);
    }

    /** Returns a snapshot containing non-dead, non-removed units. */
    public static List<Unit> alive() {
        return snapshot(Units::isAlive);
    }

    /** Returns live units owned by the exact team instance. */
    public static List<Unit> ownedBy(Team team) {
        Objects.requireNonNull(team, "team");
        return snapshot(unit -> isAlive(unit) && unit.team == team);
    }

    /** Returns live units whose centers are within {@code radius} world units. */
    public static List<Unit> withinRadius(float x, float y, float radius) {
        requireRadius(radius);
        float radiusSquared = radius * radius;
        return snapshot(unit -> isAlive(unit) && distanceSquared(unit, x, y) <= radiusSquared);
    }

    /** Returns live units inside an inclusive axis-aligned world rectangle. */
    public static List<Unit> withinBox(float minX, float minY, float maxX, float maxY) {
        requireFinite(minX, "minX");
        requireFinite(minY, "minY");
        requireFinite(maxX, "maxX");
        requireFinite(maxY, "maxY");
        if (maxX < minX || maxY < minY) {
            throw new IllegalArgumentException("maximum bounds must not be below minimum bounds");
        }
        return snapshot(unit -> isAlive(unit) && unit.x >= minX && unit.x <= maxX
                && unit.y >= minY && unit.y <= maxY);
    }

    public static List<Unit> ofType(UnitType type) {
        Objects.requireNonNull(type, "type");
        return snapshot(unit -> isAlive(unit) && unit.r() == type);
    }

    public static List<OrderableUnit> orderable() {
        List<OrderableUnit> result = new ArrayList<OrderableUnit>();
        for (Unit unit : alive()) {
            if (unit instanceof OrderableUnit) result.add((OrderableUnit) unit);
        }
        return Collections.unmodifiableList(result);
    }

    public static List<Unit> enemiesWithin(Team team, float x, float y, float radius) {
        Objects.requireNonNull(team, "team");
        return withinRadius(x, y, radius).stream()
                .filter(unit -> unit.team != null && team.isEnemy(unit.team))
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toList(), Collections::unmodifiableList));
    }

    public static List<Unit> alliesWithin(Team team, float x, float y, float radius,
            boolean includeOwnTeam) {
        Objects.requireNonNull(team, "team");
        return withinRadius(x, y, radius).stream()
                .filter(unit -> unit.team != null && team.isAlly(unit.team)
                        && (includeOwnTeam || unit.team != team))
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toList(), Collections::unmodifiableList));
    }

    public static Optional<Unit> nearestEnemy(Team team, float x, float y, float maxDistance) {
        Objects.requireNonNull(team, "team");
        return nearest(x, y, maxDistance,
                unit -> unit.team != null && team.isEnemy(unit.team));
    }

    /** Finds the nearest live unit matching a filter, limited to {@code maxDistance}. */
    public static Optional<Unit> nearest(float x, float y, float maxDistance,
                                         Predicate<? super Unit> predicate) {
        requireRadius(maxDistance);
        Objects.requireNonNull(predicate, "predicate");
        float bestDistanceSquared = maxDistance * maxDistance;
        Unit best = null;
        for (Unit unit : snapshot()) {
            if (!isAlive(unit) || !predicate.test(unit)) {
                continue;
            }
            float distanceSquared = distanceSquared(unit, x, y);
            if (distanceSquared <= bestDistanceSquared) {
                bestDistanceSquared = distanceSquared;
                best = unit;
            }
        }
        return Optional.ofNullable(best);
    }

    public static Optional<Unit> findById(long id) {
        return Optional.ofNullable(GameObject.getUnitById(id, false));
    }

    public static Optional<Unit> findById(long id, boolean includeRemoved) {
        return Optional.ofNullable(GameObject.getUnitById(id, includeRemoved));
    }

    public static boolean isAlive(Unit unit) {
        return unit != null && !unit.dead && !unit.removed;
    }

    public static float distanceSquared(Unit unit, float x, float y) {
        Objects.requireNonNull(unit, "unit");
        float dx = unit.x - x;
        float dy = unit.y - y;
        return dx * dx + dy * dy;
    }

    private static void requireRadius(float radius) {
        if (!Float.isFinite(radius) || radius < 0.0f) {
            throw new IllegalArgumentException("radius must be finite and non-negative");
        }
    }

    private static void requireFinite(float value, String name) {
        if (!Float.isFinite(value)) throw new IllegalArgumentException(name + " must be finite");
    }
}
