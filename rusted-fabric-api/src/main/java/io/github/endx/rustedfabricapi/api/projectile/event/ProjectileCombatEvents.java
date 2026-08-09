package io.github.endx.rustedfabricapi.api.projectile.event;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import rustedwarfare.custom.CustomUnit;
import rustedwarfare.custom.TurretTemplate;
import rustedwarfare.game.Projectile;
import rustedwarfare.unit.Unit;

/** Runtime extension points around native projectile selection and damage modifiers. */
public final class ProjectileCombatEvents {
    /** Runs after the native turret projectile/altProjectile selection. */
    public static final RustedFabricEvent<SelectTurretProjectile> SELECT_TURRET_PROJECTILE =
            RustedFabricEvent.create(listeners -> (shooter, target, turret, turretIndex,
                                                    nativeIndex, currentIndex) -> {
                int selected = currentIndex;
                for (SelectTurretProjectile listener : listeners) {
                    selected = listener.select(shooter, target, turret, turretIndex,
                            nativeIndex, selected);
                }
                return selected;
            });

    /** Runs after all native tag-based projectile damage mutators. */
    public static final RustedFabricEvent<ModifyDamage> MODIFY_DAMAGE =
            RustedFabricEvent.create(listeners -> (projectile, target, originalDamage,
                                                    nativeDamage, currentDamage, areaHit) -> {
                float current = currentDamage;
                for (ModifyDamage listener : listeners) {
                    current = listener.modify(projectile, target, originalDamage,
                            nativeDamage, current, areaHit);
                }
                return current;
            });

    private ProjectileCombatEvents() { }

    @FunctionalInterface
    public interface SelectTurretProjectile {
        int select(CustomUnit shooter, Unit target, TurretTemplate turret, int turretIndex,
                   int nativeIndex, int currentIndex);
    }

    @FunctionalInterface
    public interface ModifyDamage {
        float modify(Projectile projectile, Unit target, float originalDamage,
                     float nativeDamage, float currentDamage, boolean areaHit);
    }
}
