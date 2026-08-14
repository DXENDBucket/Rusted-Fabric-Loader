package io.github.endx.rustedfabricapi.impl.combat;

import io.github.endx.rustedfabricapi.api.unit.combat.TaggedDamage;
import io.github.endx.rustedfabricapi.api.game.Units;
import rustedwarfare.unit.Unit;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

/** Moves externally scheduled damage into the target unit's native update boundary. */
public final class DeferredDamageRuntime {
    private static final IdentityHashMap<Unit, List<PendingDamage>> PENDING =
            new IdentityHashMap<>();

    private DeferredDamageRuntime() { }

    public static void enqueue(Unit target, Unit attacker, float amount, String[] tags) {
        synchronized (PENDING) {
            PENDING.computeIfAbsent(target, ignored -> new ArrayList<>())
                    .add(new PendingDamage(attacker, amount, tags.clone()));
        }
    }

    public static void drain(Unit target) {
        List<PendingDamage> damage;
        synchronized (PENDING) {
            damage = PENDING.remove(target);
        }
        if (damage == null) return;
        for (PendingDamage pending : damage) {
            if (target.dead || target.hp <= 0.0F) break;
            TaggedDamage.apply(Units.view(target),
                    pending.attacker == null ? null : Units.view(pending.attacker),
                    pending.amount, pending.tags);
        }
    }

    public static void clear() {
        synchronized (PENDING) {
            PENDING.clear();
        }
    }

    private static final class PendingDamage {
        private final Unit attacker;
        private final float amount;
        private final String[] tags;

        private PendingDamage(Unit attacker, float amount, String[] tags) {
            this.attacker = attacker;
            this.amount = amount;
            this.tags = tags;
        }
    }
}
