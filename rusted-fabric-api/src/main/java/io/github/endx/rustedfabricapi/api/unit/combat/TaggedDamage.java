package io.github.endx.rustedfabricapi.api.unit.combat;

import io.github.endx.rustedfabricapi.api.game.UnitView;
import io.github.endx.rustedfabricapi.api.unit.tag.UnitTags;
import rustedwarfare.custom.CustomTagList;
import rustedwarfare.game.Projectile;
import rustedwarfare.unit.Unit;

import java.util.Objects;

/** Applies one synchronous native damage instance with explicit custom-event damage tags. */
public final class TaggedDamage {
    private TaggedDamage() { }

    public static float apply(UnitView target, UnitView attacker, float amount,
                              String... tagNames) {
        return apply(asUnit(target, "target"), attacker == null ? null : asUnit(attacker, "attacker"),
                amount, UnitTags.of(tagNames));
    }

    /**
     * Runs the normal shield, hull, death, damage-event, and resistance paths immediately.
     * The synthetic projectile exists only for this synchronous call and must not be retained by
     * listeners.
     */
    private static float apply(Unit target, Unit attacker, float amount, CustomTagList tags) {
        Objects.requireNonNull(target, "target");
        if (!Float.isFinite(amount) || amount < 0.0F) {
            throw new IllegalArgumentException("damage amount must be finite and non-negative");
        }
        // true means "do not register" in the game's constructor. A false marker would leak
        // into activeProjectiles and the global render/update object lists after every pulse.
        Projectile marker = new Projectile(true);
        marker.sourceUnit = attacker;
        marker.targetUnit = target;
        marker.tags = tags != null ? tags : UnitTags.empty();
        marker.directDamage = amount;
        marker.invisible = true;
        marker.remainingLife = 0.0F;
        marker.shieldDamageMultiplier = 1.0F;
        marker.shieldDeflectionMultiplier = 1.0F;
        marker.hullDamageMultiplier = 1.0F;
        marker.buildingDamageMultiplier = 1.0F;
        marker.airUnitDamageMultiplier = 1.0F;
        return target.applyDamage(attacker, amount, marker);
    }

    private static Unit asUnit(UnitView view, String name) {
        Objects.requireNonNull(view, name);
        Object raw = view.raw();
        if (!(raw instanceof Unit)) {
            throw new IllegalArgumentException(name + " is not a mapped game unit");
        }
        return (Unit) raw;
    }
}
