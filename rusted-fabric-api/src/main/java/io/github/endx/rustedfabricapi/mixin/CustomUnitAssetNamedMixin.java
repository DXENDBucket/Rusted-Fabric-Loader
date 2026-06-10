package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.CustomAssetEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "rustedwarfare.custom.CustomUnitMetadata", remap = false)
public abstract class CustomUnitAssetNamedMixin {
    @Inject(method = "createTeamColorImages(Lrustedwarfare/client/render/GameImage;Lcom/corrodinggames/rts/game/o;)[Lrustedwarfare/client/render/GameImage;", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeCreateTeamColorImages(@Coerce Object sourceImage, @Coerce Object teamColoringMode, CallbackInfoReturnable<Object> cir) {
        CustomAssetEvents.BEFORE_CREATE_TEAM_COLOR_IMAGES.invoker().beforeCreateTeamColorImages(this, sourceImage, teamColoringMode);
    }

    @Inject(method = "createTeamColorImages(Lrustedwarfare/client/render/GameImage;Lcom/corrodinggames/rts/game/o;)[Lrustedwarfare/client/render/GameImage;", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$afterCreateTeamColorImages(@Coerce Object sourceImage, @Coerce Object teamColoringMode, CallbackInfoReturnable<Object> cir) {
        cir.setReturnValue(CustomAssetEvents.AFTER_CREATE_TEAM_COLOR_IMAGES.invoker().afterCreateTeamColorImages(this, sourceImage, teamColoringMode, cir.getReturnValue()));
    }
}
