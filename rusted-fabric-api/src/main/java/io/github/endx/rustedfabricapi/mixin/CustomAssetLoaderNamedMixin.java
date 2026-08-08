package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.CustomAssetEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "rustedwarfare.custom.CustomUnitLoader", remap = false)
public abstract class CustomAssetLoaderNamedMixin {
    @Inject(method = "loadImageInConfigWithContext(Ljava/lang/String;Ljava/lang/String;ZLrustedwarfare/custom/CustomUnitMetadata;Ljava/lang/String;Ljava/lang/String;)Lrustedwarfare/client/render/GameImage;", at = @At("HEAD"), cancellable = true, require = 1)
    private static void rustedfabricapi$beforeLoadImage(String path, String basePath, boolean smooth, @Coerce Object metadata, String section, String key, CallbackInfoReturnable<Object> cir) {
        if (CustomAssetEvents.BEFORE_LOAD_IMAGE.invoker().beforeLoadImage(path, basePath, smooth, metadata, section, key)) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "loadImageInConfigWithContext(Ljava/lang/String;Ljava/lang/String;ZLrustedwarfare/custom/CustomUnitMetadata;Ljava/lang/String;Ljava/lang/String;)Lrustedwarfare/client/render/GameImage;", at = @At("RETURN"), cancellable = true, require = 1)
    private static void rustedfabricapi$afterLoadImage(String path, String basePath, boolean smooth, @Coerce Object metadata, String section, String key, CallbackInfoReturnable<Object> cir) {
        cir.setReturnValue(CustomAssetEvents.AFTER_LOAD_IMAGE.invoker().afterLoadImage(path, basePath, smooth, metadata, section, key, cir.getReturnValue()));
    }

    @Inject(method = "loadSoundInConfigTimed(Ljava/lang/String;Ljava/lang/String;Lrustedwarfare/custom/CustomUnitMetadata;)Lrustedwarfare/client/audio/GameSound;", at = @At("HEAD"), cancellable = true, require = 1)
    private static void rustedfabricapi$beforeLoadSound(String basePath, String soundPath, @Coerce Object metadata, CallbackInfoReturnable<Object> cir) {
        if (CustomAssetEvents.BEFORE_LOAD_SOUND.invoker().beforeLoadSound(basePath, soundPath, metadata)) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "loadSoundInConfigTimed(Ljava/lang/String;Ljava/lang/String;Lrustedwarfare/custom/CustomUnitMetadata;)Lrustedwarfare/client/audio/GameSound;", at = @At("RETURN"), cancellable = true, require = 1)
    private static void rustedfabricapi$afterLoadSound(String basePath, String soundPath, @Coerce Object metadata, CallbackInfoReturnable<Object> cir) {
        cir.setReturnValue(CustomAssetEvents.AFTER_LOAD_SOUND.invoker().afterLoadSound(basePath, soundPath, metadata, cir.getReturnValue()));
    }
}
