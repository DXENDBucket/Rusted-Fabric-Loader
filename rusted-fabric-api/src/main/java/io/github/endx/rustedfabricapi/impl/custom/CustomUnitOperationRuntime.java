package io.github.endx.rustedfabricapi.impl.custom;

import io.github.endx.rustedfabricapi.api.custom.event.CustomUnitEventData;
import io.github.endx.rustedfabricapi.api.custom.event.CustomUnitOperationEvents;
import io.github.endx.rustedfabricapi.api.custom.event.DamageEventData;
import io.github.endx.rustedfabricapi.api.custom.event.MutableCustomUnitEventContext;
import io.github.endx.rustedfabricapi.impl.combat.TaggedDamageRuntime;
import rustedwarfare.custom.CustomTagList;
import rustedwarfare.custom.CustomUnit;
import rustedwarfare.custom.event.CustomUnitEventType;
import rustedwarfare.game.Projectile;
import rustedwarfare.unit.Unit;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Supplier;

/** Internal operation preparation and recursion bypass for native method re-entry. */
public final class CustomUnitOperationRuntime {
    private static final ThreadLocal<Deque<DamageBypass>> DAMAGE_BYPASS =
            ThreadLocal.withInitial(ArrayDeque::new);

    private CustomUnitOperationRuntime() { }

    public static boolean consumeDamagePreparationBypass(CustomUnit unit) {
        Deque<DamageBypass> bypasses = DAMAGE_BYPASS.get();
        DamageBypass bypass = bypasses.peek();
        if (bypass == null || bypass.unit != unit) return false;
        bypasses.pop();
        bypass.consumed = true;
        if (bypasses.isEmpty()) DAMAGE_BYPASS.remove();
        return true;
    }

    public static MutableCustomUnitEventContext prepareDamage(
            CustomUnit unit, Unit attacker, float amount, Projectile projectile) {
        CustomUnitEventData data = CustomUnitEventData.create()
                .putNumber(DamageEventData.RAW_DAMAGE, amount)
                .putNumber(DamageEventData.HP_BEFORE, unit.hp)
                .putNumber(DamageEventData.SHIELD_BEFORE, unit.shield);
        CustomTagList tags = projectile != null ? projectile.tags
                : TaggedDamageRuntime.tags(unit);
        MutableCustomUnitEventContext context = new MutableCustomUnitEventContext(
                unit, CustomUnitEventType.TOOK_DAMAGE, attacker,
                tags,
                data, DamageEventData.DAMAGE, amount);
        CustomUnitOperationEvents.BEFORE_EVENT.invoker().beforeEvent(context);
        return context;
    }

    public static float runPreparedDamage(CustomUnit unit, Supplier<Float> operation) {
        Deque<DamageBypass> bypasses = DAMAGE_BYPASS.get();
        DamageBypass bypass = new DamageBypass(unit);
        bypasses.push(bypass);
        try {
            Float result = operation.get();
            return result != null ? result.floatValue() : 0.0F;
        } finally {
            if (!bypass.consumed) bypasses.remove(bypass);
            if (bypasses.isEmpty()) DAMAGE_BYPASS.remove();
        }
    }

    private static final class DamageBypass {
        private final CustomUnit unit;
        private boolean consumed;

        private DamageBypass(CustomUnit unit) { this.unit = unit; }
    }
}
