package io.github.endx.rustedfabricapi.verification;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import io.github.endx.rustedfabricapi.api.projectile.event.ProjectileCombatEvents;
import io.github.endx.rustedfabricapi.api.projectile.event.ProjectileSnapshotEvents;
import io.github.endx.rustedfabricapi.api.unit.combat.DamageZones;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class ProjectileCombatEventContractVerification {
    private ProjectileCombatEventContractVerification() { }

    static void verify() {
        verifyProjectileSelectionReduction();
        verifyDamageReduction();
        verifySnapshotImpactOrdering();
        verifyDamageZoneFalloff();
    }

    private static void verifyProjectileSelectionReduction() {
        RustedFabricEvent.Registration first =
                ProjectileCombatEvents.SELECT_TURRET_PROJECTILE.subscribe(
                        (shooter, target, turret, turretIndex, nativeIndex, currentIndex) -> {
                            require(nativeIndex == 3, "native projectile index changed between listeners");
                            return currentIndex + 1;
                        });
        RustedFabricEvent.Registration second =
                ProjectileCombatEvents.SELECT_TURRET_PROJECTILE.subscribe(
                        (shooter, target, turret, turretIndex, nativeIndex, currentIndex) ->
                                currentIndex * 2);
        try {
            int selected = ProjectileCombatEvents.SELECT_TURRET_PROJECTILE.invoker()
                    .select(null, null, null, 0, 3, 3);
            require(selected == 8, "projectile selection listeners were not reduced in order");
        } finally {
            first.close();
            second.close();
        }
    }

    private static void verifyDamageReduction() {
        RustedFabricEvent.Registration first = ProjectileCombatEvents.MODIFY_DAMAGE.subscribe(
                (projectile, target, original, nativeDamage, current, areaHit) -> {
                    require(original == 12.0f && nativeDamage == 10.0f,
                            "stable projectile damage inputs changed between listeners");
                    return current * 2.0f;
                });
        RustedFabricEvent.Registration second = ProjectileCombatEvents.MODIFY_DAMAGE.subscribe(
                (projectile, target, original, nativeDamage, current, areaHit) -> current + 1.0f);
        try {
            float damage = ProjectileCombatEvents.MODIFY_DAMAGE.invoker()
                    .modify(null, null, 12.0f, 10.0f, 10.0f, false);
            require(damage == 21.0f, "projectile damage listeners were not reduced in order");
        } finally {
            first.close();
            second.close();
        }
    }

    private static void verifySnapshotImpactOrdering() {
        List<String> calls = new ArrayList<String>();
        RustedFabricEvent.Registration first = ProjectileSnapshotEvents.AFTER_IMPACT
                .subscribe((projectile, impact) -> calls.add("first"));
        RustedFabricEvent.Registration second = ProjectileSnapshotEvents.AFTER_IMPACT
                .subscribe((projectile, impact) -> calls.add("second"));
        try {
            ProjectileSnapshotEvents.AFTER_IMPACT.invoker().afterImpact(null, null);
            require(calls.equals(Arrays.asList("first", "second")),
                    "snapshot projectile impacts must retain registration order");
        } finally {
            first.close();
            second.close();
        }
    }

    private static void verifyDamageZoneFalloff() {
        require(close(DamageZones.multiplier(DamageZones.Falloff.LINEAR, 0.0F, 80.0F), 1.0F),
                "linear damage zone center must retain full damage");
        require(close(DamageZones.multiplier(DamageZones.Falloff.LINEAR, 40.0F, 80.0F), 0.5F),
                "linear damage zone midpoint must retain half damage");
        require(close(DamageZones.multiplier(DamageZones.Falloff.LINEAR, 80.0F, 80.0F), 0.0F),
                "linear damage zone edge must deal no damage");
    }

    private static boolean close(float actual, float expected) {
        return Math.abs(actual - expected) < 0.00001F;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
