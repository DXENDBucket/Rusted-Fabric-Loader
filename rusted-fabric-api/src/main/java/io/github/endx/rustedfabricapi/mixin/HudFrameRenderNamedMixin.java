package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.client.event.HudRenderEvents;
import io.github.endx.rustedfabricapi.api.client.render.HudDrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rustedwarfare.core.RustedWarfareGameEngine;
import rustedwarfare.render.CanvasDrawTarget;
import rustedwarfare.render.GraphicsEngine;
import rustedwarfare.ui.InterfaceEngine;

/** HUD boundaries owned by the complete frame, not by the optional native interface draw. */
@Mixin(targets = "rustedwarfare.core.RustedWarfareGameEngine", remap = false)
public abstract class HudFrameRenderNamedMixin {
    @Inject(
            method = "drawFrame(Lrustedwarfare/render/CanvasDrawTarget;F)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lrustedwarfare/core/GameEngine;isGamePaused()Z",
                    ordinal = 0,
                    shift = At.Shift.BEFORE),
            require = 1)
    private void rustedfabricapi$beforeOptionalNativeHud(CanvasDrawTarget canvas, float delta,
                                                         CallbackInfo ci) {
        draw(HudRenderEvents.BEFORE_HUD, delta);
    }

    @Inject(
            method = "drawFrame(Lrustedwarfare/render/CanvasDrawTarget;F)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lrustedwarfare/render/GraphicsEngine;endFrame()V",
                    shift = At.Shift.BEFORE),
            require = 1)
    private void rustedfabricapi$afterOptionalNativeHud(CanvasDrawTarget canvas, float delta,
                                                        CallbackInfo ci) {
        draw(HudRenderEvents.AFTER_HUD, delta);
    }

    private void draw(io.github.endx.rustedfabricapi.api.event.RustedFabricEvent<
            HudRenderEvents.DrawHud> event, float delta) {
        RustedWarfareGameEngine engine = (RustedWarfareGameEngine) (Object) this;
        GraphicsEngine graphics = engine.renderGraphicsEngine;
        InterfaceEngine gameInterface = engine.gameUI;
        if (graphics != null && gameInterface != null) {
            event.invoker().draw(gameInterface, new HudDrawContext(graphics, delta));
        }
    }
}
