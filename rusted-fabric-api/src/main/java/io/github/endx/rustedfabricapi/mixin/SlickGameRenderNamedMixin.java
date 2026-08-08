package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.GameLifecycleEvents;
import io.github.endx.rustedfabricapi.api.thread.GameThreadScheduler;
import rustedwarfare.core.GameEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Coerce;

@Mixin(targets = "rustedwarfare.client.SlickGame", remap = false)
public abstract class SlickGameRenderNamedMixin {
    @Inject(method = "render(Lorg/newdawn/slick/GameContainer;Lorg/newdawn/slick/Graphics;)V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeFrameRender(@Coerce Object gameContainer,
                                                   @Coerce Object graphics,
                                                   CallbackInfo ci) {
        GameThreadScheduler.executeRenderPhase();
        GameLifecycleEvents.BEFORE_FRAME_RENDER.invoker()
                .beforeFrameRender(this, gameContainer, graphics);
        GameEngine engine = GameEngine.getInstance();
        io.github.endx.rustedfabricapi.api.client.event.ClientRenderEvents.START_CLIENT_RENDER.invoker()
                .onRender(engine, engine != null ? engine.renderGraphicsEngine : null);
    }

    @Inject(method = "render(Lorg/newdawn/slick/GameContainer;Lorg/newdawn/slick/Graphics;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterFrameRender(@Coerce Object gameContainer,
                                                  @Coerce Object graphics,
                                                  CallbackInfo ci) {
        GameLifecycleEvents.AFTER_FRAME_RENDER.invoker().afterFrameRender(this);
        GameEngine engine = GameEngine.getInstance();
        io.github.endx.rustedfabricapi.api.client.event.ClientRenderEvents.END_CLIENT_RENDER.invoker()
                .onRender(engine, engine != null ? engine.renderGraphicsEngine : null);
    }
}
