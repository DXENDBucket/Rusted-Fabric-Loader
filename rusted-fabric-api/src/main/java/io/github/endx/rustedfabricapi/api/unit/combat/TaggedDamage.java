package io.github.endx.rustedfabricapi.api.unit.combat;

import io.github.endx.rustedfabricapi.api.game.UnitView;
import io.github.endx.rustedfabricapi.api.unit.tag.UnitTags;
import io.github.endx.rustedfabricapi.impl.combat.TaggedDamageRuntime;
import rustedwarfare.custom.CustomTagList;
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

    /** Runs the normal shield, hull, death, damage-event, and resistance paths immediately. */
    private static float apply(Unit target, Unit attacker, float amount, CustomTagList tags) {
        Objects.requireNonNull(target, "target");
        if (!Float.isFinite(amount) || amount < 0.0F) {
            throw new IllegalArgumentException("damage amount must be finite and non-negative");
        }
        CustomTagList checkedTags = tags != null ? tags : UnitTags.empty();
        return TaggedDamageRuntime.apply(target, checkedTags,
                () -> Float.valueOf(target.applyDamage(attacker, amount, null)));
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
