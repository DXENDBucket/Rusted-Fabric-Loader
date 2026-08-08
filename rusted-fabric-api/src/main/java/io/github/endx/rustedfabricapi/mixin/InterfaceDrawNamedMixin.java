package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.GameLifecycleEvents;
import io.github.endx.rustedfabricapi.api.client.render.HudDrawContext;
import rustedwarfare.core.GameEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "rustedwarfare.ui.InterfaceEngine", remap = false)
public abstract class InterfaceDrawNamedMixin {
    @Inject(method = "drawGameInterface(F)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterGameInterfaceDraw(float delta, CallbackInfo ci) {
        GameLifecycleEvents.AFTER_GAME_INTERFACE_DRAW.invoker().afterGameInterfaceDraw(this, delta);
        GameEngine engine = GameEngine.getInstance();
        rustedwarfare.render.GraphicsEngine graphics =
                engine != null ? engine.renderGraphicsEngine : null;
        io.github.endx.rustedfabricapi.api.client.event.HudRenderEvents.AFTER_HUD_RENDER.invoker()
                .afterHudRender((rustedwarfare.ui.InterfaceEngine) (Object) this,
                        graphics, delta);
        if (graphics != null) {
            io.github.endx.rustedfabricapi.api.client.event.HudRenderEvents.AFTER_HUD.invoker()
                    .draw((rustedwarfare.ui.InterfaceEngine) (Object) this,
                            new HudDrawContext(graphics, delta));
        }
    }
}
