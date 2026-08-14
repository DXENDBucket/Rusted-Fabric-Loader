package io.github.endx.rustedfabricapi.api.projectile.event;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import io.github.endx.rustedfabricapi.api.game.ProjectileImpactSnapshot;
import io.github.endx.rustedfabricapi.api.game.ProjectileSnapshot;

/** Namespace-neutral projectile lifecycle events for Java mods that avoid mapped game classes. */
public final class ProjectileSnapshotEvents {
    public static final RustedFabricEvent<Impact> AFTER_IMPACT =
            RustedFabricEvent.create(listeners -> (projectile, impact) -> {
                for (Impact listener : listeners) listener.afterImpact(projectile, impact);
            });

    private ProjectileSnapshotEvents() { }

    @FunctionalInterface
    public interface Impact {
        void afterImpact(ProjectileSnapshot projectile, ProjectileImpactSnapshot impact);
    }
}
