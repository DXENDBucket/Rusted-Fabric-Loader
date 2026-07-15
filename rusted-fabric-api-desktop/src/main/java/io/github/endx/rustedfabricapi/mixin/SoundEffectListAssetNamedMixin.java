package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.CustomAssetEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "rustedwarfare.custom.SoundEffectList", remap = false)
public abstract class SoundEffectListAssetNamedMixin {
    @Inject(method = "parseSoundList(Lrustedwarfare/custom/CustomUnitMetadata;Ljava/lang/String;)Lrustedwarfare/custom/SoundEffectList;", at = @At("HEAD"), cancellable = true, require = 1)
    private static void rustedfabricapi$beforeParseSoundList(@Coerce Object metadata, String rawSoundList, CallbackInfoReturnable<Object> cir) {
        if (CustomAssetEvents.BEFORE_PARSE_SOUND_LIST.invoker().beforeParseSoundList(metadata, rawSoundList)) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "parseSoundList(Lrustedwarfare/custom/CustomUnitMetadata;Ljava/lang/String;)Lrustedwarfare/custom/SoundEffectList;", at = @At("RETURN"), cancellable = true, require = 1)
    private static void rustedfabricapi$afterParseSoundList(@Coerce Object metadata, String rawSoundList, CallbackInfoReturnable<Object> cir) {
        cir.setReturnValue(CustomAssetEvents.AFTER_PARSE_SOUND_LIST.invoker().afterParseSoundList(metadata, rawSoundList, cir.getReturnValue()));
    }
}
