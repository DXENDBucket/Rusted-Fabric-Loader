package io.github.endx.rustedfabricapi.verification;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import io.github.endx.rustedfabricapi.api.projectile.event.ProjectileCombatEvents;

final class ProjectileCombatEventContractVerification {
    private ProjectileCombatEventContractVerification() { }

    static void verify() {
        verifyProjectileSelectionReduction();
        verifyDamageReduction();
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

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
