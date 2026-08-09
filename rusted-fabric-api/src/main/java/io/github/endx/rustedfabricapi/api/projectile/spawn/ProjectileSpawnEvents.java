package io.github.endx.rustedfabricapi.api.projectile.spawn;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import rustedwarfare.game.Projectile;

/** Events around fully described projectiles created through {@link ProjectileSpawner}. */
public final class ProjectileSpawnEvents {
    public static final RustedFabricEvent<BeforeSpawn> BEFORE_SPAWN =
            RustedFabricEvent.create(listeners -> request -> {
                for (BeforeSpawn listener : listeners) listener.beforeSpawn(request);
            });

    public static final RustedFabricEvent<AfterSpawn> AFTER_SPAWN =
            RustedFabricEvent.create(listeners -> (projectile, spec) -> {
                for (AfterSpawn listener : listeners) listener.afterSpawn(projectile, spec);
            });

    private ProjectileSpawnEvents() { }

    @FunctionalInterface
    public interface BeforeSpawn {
        void beforeSpawn(ProjectileSpawnRequest request);
    }

    @FunctionalInterface
    public interface AfterSpawn {
        void afterSpawn(Projectile projectile, ProjectileSpawnSpec finalSpec);
    }
}
