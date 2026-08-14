package io.github.endx.rustedfabricapi.api.ai;

import io.github.endx.rustedfabricapi.api.game.UnitView;
import rustedwarfare.unit.OrderableUnit;
import rustedwarfare.unit.Unit;

/**
 * Target-specific native attack and range assessment for tactical AI.
 *
 * <p>The ranges use the game's own target-aware calculation, including collision radii for unit
 * implementations that opt into edge-to-edge range. This is deliberately different from merely
 * comparing the two unit types' displayed maximum ranges.</p>
 */
public final class AiEngagementAssessment {
    private final UnitView attacker;
    private final UnitView target;
    private final boolean canEngage;
    private final boolean canReturnFire;
    private final float attackerRange;
    private final float returnFireRange;
    private final float centerDistance;
    private final boolean attackerWithinRange;
    private final boolean defenderWithinRange;

    private AiEngagementAssessment(UnitView attacker, UnitView target,
            boolean canEngage, boolean canReturnFire, float attackerRange,
            float returnFireRange, float centerDistance, boolean attackerWithinRange,
            boolean defenderWithinRange) {
        this.attacker = attacker;
        this.target = target;
        this.canEngage = canEngage;
        this.canReturnFire = canReturnFire;
        this.attackerRange = attackerRange;
        this.returnFireRange = returnFireRange;
        this.centerDistance = centerDistance;
        this.attackerWithinRange = attackerWithinRange;
        this.defenderWithinRange = defenderWithinRange;
    }

    public static AiEngagementAssessment capture(UnitView attackerView, UnitView targetView) {
        if (attackerView == null) throw new IllegalArgumentException("attacker must not be null");
        if (targetView == null) throw new IllegalArgumentException("target must not be null");
        Unit attackerRaw = raw(attackerView, "attacker");
        Unit targetRaw = raw(targetView, "target");
        OrderableUnit attacker = attackerRaw instanceof OrderableUnit
                ? (OrderableUnit) attackerRaw : null;
        OrderableUnit defender = targetRaw instanceof OrderableUnit
                ? (OrderableUnit) targetRaw : null;
        boolean canEngage = canEngage(attacker, targetRaw);
        boolean canReturn = canEngage(defender, attackerRaw);
        float attackRange = canEngage
                ? finiteNonNegative(attacker.getAttackRangeAgainst(targetRaw)) : 0.0F;
        float returnRange = canReturn
                ? finiteNonNegative(defender.getAttackRangeAgainst(attackerRaw)) : 0.0F;
        float dx = attackerView.x() - targetView.x();
        float dy = attackerView.y() - targetView.y();
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        return new AiEngagementAssessment(attackerView, targetView, canEngage, canReturn,
                attackRange, returnRange, distance,
                canEngage && attacker.isTargetWithinAttackRange(targetRaw),
                canReturn && defender.isTargetWithinAttackRange(attackerRaw));
    }

    public UnitView attacker() { return attacker; }
    public UnitView target() { return target; }
    public boolean canEngage() { return canEngage; }
    public boolean canReturnFire() { return canReturnFire; }
    public float attackerRange() { return attackerRange; }
    public float returnFireRange() { return returnFireRange; }
    public float centerDistance() { return centerDistance; }
    public boolean attackerWithinRange() { return attackerWithinRange; }
    public boolean defenderWithinRange() { return defenderWithinRange; }
    public float rangeAdvantage() { return attackerRange - returnFireRange; }

    /** True when a non-empty center-distance band exists in which only the attacker can fire. */
    public boolean hasSafeStandoffWindow(float minimumWidth) {
        if (!Float.isFinite(minimumWidth) || minimumWidth < 0.0F) {
            throw new IllegalArgumentException("minimumWidth must be finite and non-negative");
        }
        return canEngage && canReturnFire && rangeAdvantage() >= minimumWidth;
    }

    private static boolean canEngage(OrderableUnit attacker, Unit target) {
        return attacker != null && attacker.canAttack()
                && attacker.canAttackTargetType(target)
                && attacker.canAnyTurretTargetIgnoringRange(target);
    }

    private static Unit raw(UnitView view, String role) {
        Object value = view.raw();
        if (!(value instanceof Unit)) {
            throw new IllegalArgumentException(role
                    + " view is not backed by the active game namespace");
        }
        return (Unit) value;
    }

    private static float finiteNonNegative(float value) {
        return Float.isFinite(value) ? Math.max(0.0F, value) : 0.0F;
    }
}
