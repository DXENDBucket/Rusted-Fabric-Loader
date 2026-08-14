package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.ai.AiControllers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rustedwarfare.ai.AiTeam;

@Mixin(targets = "rustedwarfare.ai.AiTeam", remap = false)
public abstract class AiTeamControlNamedMixin {
    @Inject(method = "updateAiTeam(F)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeAiTick(float delta, CallbackInfo ci) {
        AiTeam team = (AiTeam) (Object) this;
        if (AiControllers.beforeNativeTick(team, delta)) ci.cancel();
    }

    @Inject(method = "updateAiTeam(F)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterNativeAiTick(float delta, CallbackInfo ci) {
        AiControllers.afterNativeTick((AiTeam) (Object) this, delta);
    }
}
