package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.GameLifecycleEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "rustedwarfare.ui.Minimap", remap = false)
public abstract class MinimapSetupNamedMixin {
    @Inject(method = "setupForMap(Lcom/corrodinggames/rts/game/b/b;Z)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterMapSetup(@Coerce Object map, boolean fogEnabled, CallbackInfo ci) {
        GameLifecycleEvents.AFTER_MAP_SETUP.invoker().afterMapSetup(this, map, fogEnabled);
    }
}
