package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.AudioRuntimeEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "rustedwarfare.audio.OpenALSoundFactory", remap = false)
public abstract class OpenALSoundFactoryNamedMixin {
    @Inject(method = "init(Landroid/content/Context;)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeSoundFactoryInit(@Coerce Object androidContext, CallbackInfo ci) {
        if (AudioRuntimeEvents.BEFORE_SOUND_FACTORY_INIT.invoker()
                .beforeSoundFactoryInit(this, androidContext)) {
            ci.cancel();
        }
    }

    @Inject(method = "init(Landroid/content/Context;)V", at = @At("TAIL"), require = 1)
    private void rustedfabricapi$afterSoundFactoryInit(@Coerce Object androidContext, CallbackInfo ci) {
        AudioRuntimeEvents.AFTER_SOUND_FACTORY_INIT.invoker()
                .afterSoundFactoryInit(this, androidContext);
    }

    @Inject(method = "loadBuiltinSoundByResourceId(I)Lrustedwarfare/client/audio/GameSound;", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeLoadBuiltinSound(int resourceId, CallbackInfoReturnable<Object> cir) {
        Object override = AudioRuntimeEvents.BEFORE_LOAD_BUILTIN_SOUND.invoker()
                .beforeLoadBuiltinSound(this, resourceId);
        if (override != null) {
            cir.setReturnValue(override);
        }
    }

    @Inject(method = "loadBuiltinSoundByResourceId(I)Lrustedwarfare/client/audio/GameSound;", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$afterLoadBuiltinSound(int resourceId, CallbackInfoReturnable<Object> cir) {
        cir.setReturnValue(AudioRuntimeEvents.AFTER_LOAD_BUILTIN_SOUND.invoker()
                .afterLoadBuiltinSound(this, resourceId, cir.getReturnValue()));
    }

    @Inject(method = "loadSoundFromStream(Ljava/lang/String;Lrustedwarfare/io/NamedInputStream;Z)Lrustedwarfare/client/audio/GameSound;", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeLoadSoundFromStream(String name, @Coerce Object inputStream, boolean strict,
                                                           CallbackInfoReturnable<Object> cir) {
        Object override = AudioRuntimeEvents.BEFORE_LOAD_SOUND_FROM_STREAM.invoker()
                .beforeLoadSoundFromStream(this, name, inputStream, strict);
        if (override != null) {
            cir.setReturnValue(override);
        }
    }

    @Inject(method = "loadSoundFromStream(Ljava/lang/String;Lrustedwarfare/io/NamedInputStream;Z)Lrustedwarfare/client/audio/GameSound;", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$afterLoadSoundFromStream(String name, @Coerce Object inputStream, boolean strict,
                                                          CallbackInfoReturnable<Object> cir) {
        cir.setReturnValue(AudioRuntimeEvents.AFTER_LOAD_SOUND_FROM_STREAM.invoker()
                .afterLoadSoundFromStream(this, name, inputStream, strict, cir.getReturnValue()));
    }
}
