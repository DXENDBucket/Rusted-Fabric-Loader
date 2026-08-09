package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.fog.FogSources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rustedwarfare.map.MapEngine;

@Mixin(targets = "rustedwarfare.map.MapEngine", remap = false)
public abstract class MapFogSourceNamedMixin {
    @Shadow private float lineOfSightFogRefreshTimer;

    @Inject(method = "updateLineOfSightFog(F)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$applyGeometryFogSources(float delta, CallbackInfo ci) {
        MapEngine map = (MapEngine) (Object) this;
        boolean nativeLosRefresh = map.useLineOfSightFog
                && lineOfSightFogRefreshTimer == 0.0F;
        FogSources.updateAfterNativeLos(map, delta, nativeLosRefresh);
    }
}
