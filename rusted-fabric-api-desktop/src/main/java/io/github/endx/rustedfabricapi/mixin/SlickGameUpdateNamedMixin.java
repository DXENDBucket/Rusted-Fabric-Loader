package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.GameLifecycleEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "rustedwarfare.client.SlickGame", remap = false)
public abstract class SlickGameUpdateNamedMixin {
    @Inject(method = "update(Lorg/newdawn/slick/GameContainer;I)V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeFrameUpdate(@Coerce Object gameContainer, int delta, CallbackInfo ci) {
        GameLifecycleEvents.BEFORE_FRAME_UPDATE.invoker().beforeFrameUpdate(this, gameContainer, delta);
    }

    @Inject(method = "update(Lorg/newdawn/slick/GameContainer;I)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterFrameUpdate(@Coerce Object gameContainer, int delta, CallbackInfo ci) {
        GameLifecycleEvents.AFTER_FRAME_UPDATE.invoker().afterFrameUpdate(this, gameContainer, delta);
    }
}
