package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.GameLifecycleEvents;
import io.github.endx.rustedfabricapi.api.thread.GameThreadScheduler;
import io.github.endx.rustedfabricapi.api.scheduler.GameTickScheduler;
import io.github.endx.rustedfabricapi.api.client.input.KeyBindings;
import rustedwarfare.core.GameEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "rustedwarfare.client.SlickGame", remap = false)
public abstract class SlickGameUpdateNamedMixin {
    @Inject(method = "update(Lorg/newdawn/slick/GameContainer;I)V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeFrameUpdate(@Coerce Object gameContainer, int delta, CallbackInfo ci) {
        GameThreadScheduler.executeUpdatePhase();
        KeyBindings.pollRegisteredBindings();
        GameEngine engine = GameEngine.getInstance();
        if (engine != null) {
            // Timed simulation mutations must run before the game's native update. Running them
            // from this method's RETURN creates effects (notably unit death explosions) after the
            // effect engine has already updated, so their first operation becomes a render with
            // partially initialized state.
            GameTickScheduler.executeUpdateTick(engine.currentTick);
            io.github.endx.rustedfabricapi.api.client.event.ClientTickEvents.START_CLIENT_TICK.invoker()
                    .onStartTick(engine);
        }
        GameLifecycleEvents.BEFORE_FRAME_UPDATE.invoker().beforeFrameUpdate(this, gameContainer, delta);
    }

    @Inject(method = "update(Lorg/newdawn/slick/GameContainer;I)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterFrameUpdate(@Coerce Object gameContainer, int delta, CallbackInfo ci) {
        GameLifecycleEvents.AFTER_FRAME_UPDATE.invoker().afterFrameUpdate(this, gameContainer, delta);
        GameEngine engine = GameEngine.getInstance();
        if (engine != null) {
            io.github.endx.rustedfabricapi.api.client.event.ClientTickEvents.END_CLIENT_TICK.invoker()
                    .onEndTick(engine);
        }
    }
}
