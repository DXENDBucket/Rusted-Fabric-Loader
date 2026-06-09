package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.GameLifecycleEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.corrodinggames.rts.gameFramework.f.g", remap = false)
public abstract class InterfaceDrawOfficialMixin {
    @Inject(method = "b(F)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterGameInterfaceDraw(float delta, CallbackInfo ci) {
        GameLifecycleEvents.AFTER_GAME_INTERFACE_DRAW.invoker().afterGameInterfaceDraw(this, delta);
    }
}
