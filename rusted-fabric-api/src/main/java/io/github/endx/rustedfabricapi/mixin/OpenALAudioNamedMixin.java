package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.AudioRuntimeEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "rustedwarfare.audio.openal.OpenALAudio", remap = false)
public abstract class OpenALAudioNamedMixin {
    @Inject(method = "newSound(Lrustedwarfare/audio/util/AudioFileHandle;)Lrustedwarfare/audio/openal/OpenALSound;", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeOpenALNewSound(@Coerce Object fileHandle,
                                                      CallbackInfoReturnable<Object> cir) {
        Object override = AudioRuntimeEvents.BEFORE_OPENAL_NEW_SOUND.invoker()
                .beforeOpenALNewSound(this, fileHandle);
        if (override != null) {
            cir.setReturnValue(override);
        }
    }

    @Inject(method = "newSound(Lrustedwarfare/audio/util/AudioFileHandle;)Lrustedwarfare/audio/openal/OpenALSound;", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$afterOpenALNewSound(@Coerce Object fileHandle,
                                                     CallbackInfoReturnable<Object> cir) {
        cir.setReturnValue(AudioRuntimeEvents.AFTER_OPENAL_NEW_SOUND.invoker()
                .afterOpenALNewSound(this, fileHandle, cir.getReturnValue()));
    }

    @Inject(method = "newMusic(Lrustedwarfare/audio/util/AudioFileHandle;)Lrustedwarfare/audio/openal/OpenALMusic;", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeOpenALNewMusic(@Coerce Object fileHandle,
                                                      CallbackInfoReturnable<Object> cir) {
        Object override = AudioRuntimeEvents.BEFORE_OPENAL_NEW_MUSIC.invoker()
                .beforeOpenALNewMusic(this, fileHandle);
        if (override != null) {
            cir.setReturnValue(override);
        }
    }

    @Inject(method = "newMusic(Lrustedwarfare/audio/util/AudioFileHandle;)Lrustedwarfare/audio/openal/OpenALMusic;", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$afterOpenALNewMusic(@Coerce Object fileHandle,
                                                     CallbackInfoReturnable<Object> cir) {
        cir.setReturnValue(AudioRuntimeEvents.AFTER_OPENAL_NEW_MUSIC.invoker()
                .afterOpenALNewMusic(this, fileHandle, cir.getReturnValue()));
    }
}
