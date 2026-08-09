package io.github.endx.rustedfabricapi.api.client.render.event;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import rustedwarfare.game.Projectile;

/** Client render stages around one native projectile. */
public final class ProjectileRenderEvents {
    public static final RustedFabricEvent<Draw> DRAW =
            RustedFabricEvent.create(listeners -> (projectile, delta, stage) -> {
                for (Draw listener : listeners) listener.draw(projectile, delta, stage);
            });

    private ProjectileRenderEvents() { }

    public enum Stage {
        SHADOW,
        BEFORE_BODY,
        AFTER_BODY,
        ON_TOP,
        BEFORE_UI
    }

    @FunctionalInterface
    public interface Draw {
        void draw(Projectile projectile, float delta, Stage stage);
    }
}
