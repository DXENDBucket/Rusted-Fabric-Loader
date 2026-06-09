package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.MapDiscoveryEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "rustedwarfare.ui.android.MapSelectActivity", remap = false)
public abstract class AndroidMapStartNamedMixin {
    @Inject(method = "startMapWithSkirmishSettings(Ljava/lang/String;ZIIZZ)V", at = @At("HEAD"), cancellable = true, require = 1)
    private static void rustedfabricapi$beforeMapStartFromAndroidUi(String mapPath, boolean customMap, int playerCount, int aiDifficulty, boolean fog, boolean revealedMap, CallbackInfo ci) {
        if (MapDiscoveryEvents.BEFORE_MAP_START_FROM_ANDROID_UI.invoker()
                .beforeMapStartFromAndroidUi(mapPath, customMap, playerCount, aiDifficulty, fog, revealedMap)) {
            ci.cancel();
        }
    }
}
