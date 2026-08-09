package io.github.endx.rustedfabricapi.api.client.event;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import io.github.endx.rustedfabricapi.api.client.render.HudDrawContext;
import rustedwarfare.render.GraphicsEngine;
import rustedwarfare.ui.InterfaceEngine;

/** HUD rendering boundaries with mapped game types. */
public final class HudRenderEvents {
    /** Draws after the world but before the native game interface and HUD controls. */
    public static final RustedFabricEvent<DrawHud> BEFORE_HUD =
            RustedFabricEvent.create(listeners -> (gameInterface, context) -> {
                for (DrawHud listener : listeners) listener.draw(gameInterface, context);
            });

    /** Preferred frame-scoped drawing event with safe styles, clipping, transforms, and images. */
    public static final RustedFabricEvent<DrawHud> AFTER_HUD =
            RustedFabricEvent.create(listeners -> (gameInterface, context) -> {
                for (DrawHud listener : listeners) listener.draw(gameInterface, context);
            });

    /** Lower-level compatibility event for mods that need direct mapped graphics access. */
    public static final RustedFabricEvent<AfterHudRender> AFTER_HUD_RENDER =
            RustedFabricEvent.create(listeners -> (gameInterface, graphics, delta) -> {
                for (AfterHudRender listener : listeners) {
                    listener.afterHudRender(gameInterface, graphics, delta);
                }
            });

    private HudRenderEvents() {
    }

    @FunctionalInterface
    public interface AfterHudRender {
        void afterHudRender(InterfaceEngine gameInterface, GraphicsEngine graphics, float delta);
    }

    @FunctionalInterface
    public interface DrawHud {
        void draw(InterfaceEngine gameInterface, HudDrawContext context);
    }
}
