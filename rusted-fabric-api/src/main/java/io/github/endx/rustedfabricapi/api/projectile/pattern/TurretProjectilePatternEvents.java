package io.github.endx.rustedfabricapi.api.projectile.pattern;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;

/** Precise extension point for replacing the projectile portion of a native turret shot. */
public final class TurretProjectilePatternEvents {
    public static final RustedFabricEvent<Plan> PLAN =
            RustedFabricEvent.create(listeners -> request -> {
                for (Plan listener : listeners) listener.plan(request);
            });

    private TurretProjectilePatternEvents() { }

    @FunctionalInterface
    public interface Plan {
        void plan(TurretProjectilePatternRequest request);
    }
}
