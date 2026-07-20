package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.path.Pathfinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rustedwarfare.path.PathRequest;

@Mixin(targets = "rustedwarfare.path.PathEngine", remap = false)
public abstract class PathEngineRuntimeNamedMixin {
    @Inject(method = "queuePathRequest(Lrustedwarfare/path/PathRequest;Z)V",
            at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeQueue(PathRequest request, boolean refreshCosts,
            CallbackInfo ci) {
        Pathfinding.onNativeQueuing((rustedwarfare.path.PathEngine) (Object) this,
                request, refreshCosts);
    }

    @Inject(method = "queuePathRequest(Lrustedwarfare/path/PathRequest;Z)V",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterQueue(PathRequest request, boolean refreshCosts,
            CallbackInfo ci) {
        Pathfinding.onNativeQueued((rustedwarfare.path.PathEngine) (Object) this,
                request, refreshCosts);
    }

    @Inject(method = "onPathSolved(Lrustedwarfare/path/PathRequest;)V",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterSolved(PathRequest request, CallbackInfo ci) {
        Pathfinding.onNativeSolved((rustedwarfare.path.PathEngine) (Object) this, request);
    }
}
