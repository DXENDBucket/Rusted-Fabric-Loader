package io.github.endx.rustedfabricapi.api.unit.combat;

import rustedwarfare.unit.OrderableUnit;
import rustedwarfare.unit.Unit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Strongly typed combat targeting and turret helpers. */
public final class CombatUnits {
    private CombatUnits() {
    }

    public static Optional<Unit> currentTarget(OrderableUnit unit) {
        Objects.requireNonNull(unit, "unit");
        return Optional.ofNullable(unit.getCurrentTargetUnit());
    }

    public static boolean hasAttackTarget(OrderableUnit unit) {
        Objects.requireNonNull(unit, "unit");
        return unit.attackTarget != null;
    }

    public static boolean hasFiringTurretTarget(OrderableUnit unit) {
        Objects.requireNonNull(unit, "unit");
        return unit.hasFiringTurretTarget();
    }

    public static boolean isWithinAttackRange(OrderableUnit attacker, Unit target) {
        Objects.requireNonNull(attacker, "attacker");
        Objects.requireNonNull(target, "target");
        return attacker.isTargetWithinAttackRange(target);
    }

    public static boolean canAutoAttack(OrderableUnit attacker, Unit target,
                                        boolean checkSearchRange) {
        Objects.requireNonNull(attacker, "attacker");
        Objects.requireNonNull(target, "target");
        return attacker.canAutoAttackTarget(target, checkSearchRange);
    }

    public static boolean canAutoAttackVisible(OrderableUnit attacker, Unit target,
                                               boolean checkSearchRange) {
        Objects.requireNonNull(attacker, "attacker");
        Objects.requireNonNull(target, "target");
        return attacker.canAutoAttackVisibleTarget(target, checkSearchRange);
    }

    public static float autoAttackSearchRange(OrderableUnit unit, boolean includeExtraRange) {
        Objects.requireNonNull(unit, "unit");
        return unit.getAutoAttackSearchRange(includeExtraRange);
    }

    public static int turretCount(OrderableUnit unit) {
        Objects.requireNonNull(unit, "unit");
        return Math.max(0, unit.getTurretCount());
    }

    public static TurretSnapshot turret(OrderableUnit unit, int turretIndex) {
        requireTurretIndex(unit, turretIndex);
        return TurretSnapshot.capture(unit, turretIndex);
    }

    public static List<TurretSnapshot> turrets(OrderableUnit unit) {
        Objects.requireNonNull(unit, "unit");
        int count = turretCount(unit);
        if (count == 0) return Collections.emptyList();
        List<TurretSnapshot> result = new ArrayList<TurretSnapshot>(count);
        for (int index = 0; index < count; index++) {
            result.add(TurretSnapshot.capture(unit, index));
        }
        return Collections.unmodifiableList(result);
    }

    public static boolean canTurretAttack(OrderableUnit attacker, int turretIndex,
                                          Unit target, boolean ignoreRange, boolean requireRange) {
        requireTurretIndex(attacker, turretIndex);
        Objects.requireNonNull(target, "target");
        return attacker.canTurretAttackTarget(turretIndex, target, ignoreRange, requireRange);
    }

    /**
     * Advances the game's normal warmup/reload path and fires only when it becomes ready.
     * This mutates simulation state and must run on the update thread.
     */
    public static boolean tryFire(OrderableUnit attacker, float delta,
                                  Unit target, int turretIndex) {
        requireTurretIndex(attacker, turretIndex);
        Objects.requireNonNull(target, "target");
        if (!Float.isFinite(delta) || delta < 0.0F) {
            throw new IllegalArgumentException("delta must be finite and non-negative");
        }
        return attacker.tryFireTurretAtTarget(delta, target, turretIndex);
    }

    public static void addAimAngle(OrderableUnit unit, int turretIndex, float angleDelta) {
        requireTurretIndex(unit, turretIndex);
        if (!Float.isFinite(angleDelta)) {
            throw new IllegalArgumentException("angleDelta must be finite");
        }
        unit.addTurretAimAngle(turretIndex, angleDelta);
    }

    private static void requireTurretIndex(OrderableUnit unit, int turretIndex) {
        Objects.requireNonNull(unit, "unit");
        int count = turretCount(unit);
        if (turretIndex < 0 || turretIndex >= count) {
            throw new IndexOutOfBoundsException("turretIndex=" + turretIndex + ", count=" + count);
        }
    }
}
