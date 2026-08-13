package io.github.endx.rustedfabricapi.api.unit.event;

import io.github.endx.rustedfabricapi.api.game.UnitView;
import io.github.endx.rustedfabricapi.api.game.Units;
import rustedwarfare.game.Projectile;
import rustedwarfare.unit.Unit;

import java.util.Objects;
import java.util.Optional;

/** Immutable before/after snapshot for one completed native damage application. */
public final class UnitDamageResult {
    private final Unit unit;
    private final Unit attacker;
    private final Projectile projectile;
    private final float requestedDamage;
    private final float nativeRemainingDamage;
    private final float hpBefore;
    private final float hpAfter;
    private final float shieldBefore;
    private final float shieldAfter;

    public UnitDamageResult(Unit unit, Unit attacker, Projectile projectile,
                            float requestedDamage, float nativeRemainingDamage,
                            float hpBefore, float hpAfter,
                            float shieldBefore, float shieldAfter) {
        this.unit = Objects.requireNonNull(unit, "unit");
        this.attacker = attacker;
        this.projectile = projectile;
        this.requestedDamage = requestedDamage;
        this.nativeRemainingDamage = nativeRemainingDamage;
        this.hpBefore = hpBefore;
        this.hpAfter = hpAfter;
        this.shieldBefore = shieldBefore;
        this.shieldAfter = shieldAfter;
    }

    public Unit unit() { return unit; }
    /** Namespace-stable live view of the damaged unit. */
    public UnitView unitView() { return Units.view(unit); }
    public Optional<Unit> attacker() { return Optional.ofNullable(attacker); }
    /**
     * Namespace-stable live view of the source, when the damage has one.
     * Projectile damage may pass a null direct attacker, so this falls back to the projectile's
     * source unit.
     */
    public Optional<UnitView> attackerView() {
        Unit source = attacker != null ? attacker
                : projectile != null ? projectile.sourceUnit : null;
        return source == null ? Optional.empty() : Optional.of(Units.view(source));
    }
    public Optional<Projectile> projectile() { return Optional.ofNullable(projectile); }
    public float requestedDamage() { return requestedDamage; }
    public float nativeRemainingDamage() { return nativeRemainingDamage; }
    public float hpBefore() { return hpBefore; }
    public float hpAfter() { return hpAfter; }
    public float shieldBefore() { return shieldBefore; }
    public float shieldAfter() { return shieldAfter; }
    public float hpDamage() { return Math.max(0.0F, hpBefore - hpAfter); }
    public float shieldDamage() { return Math.max(0.0F, shieldBefore - shieldAfter); }
    public boolean wasLethal() { return hpBefore > 0.0F && hpAfter <= 0.0F; }
}
