package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.AudioRuntimeEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "rustedwarfare.audio.OpenALGameSound", remap = false)
public abstract class OpenALGameSoundNamedMixin {
    @Inject(method = "play(FFIIF)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeGameSoundPlay(float leftVolume, float rightVolume, int priority, int loop,
                                                     float pitch, CallbackInfo ci) {
        if (AudioRuntimeEvents.BEFORE_GAME_SOUND_PLAY.invoker()
                .beforeGameSoundPlay(this, leftVolume, rightVolume, priority, loop, pitch)) {
            ci.cancel();
        }
    }

    @Inject(method = "play(FFIIF)V", at = @At("TAIL"), require = 1)
    private void rustedfabricapi$afterGameSoundPlay(float leftVolume, float rightVolume, int priority, int loop,
                                                    float pitch, CallbackInfo ci) {
        AudioRuntimeEvents.AFTER_GAME_SOUND_PLAY.invoker()
                .afterGameSoundPlay(this, leftVolume, rightVolume, priority, loop, pitch);
    }

    @Inject(method = "playNow(FFIIF)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeGameSoundPlayNow(float leftVolume, float rightVolume, int priority, int loop,
                                                        float pitch, CallbackInfo ci) {
        if (AudioRuntimeEvents.BEFORE_GAME_SOUND_PLAY_NOW.invoker()
                .beforeGameSoundPlayNow(this, leftVolume, rightVolume, priority, loop, pitch)) {
            ci.cancel();
        }
    }

    @Inject(method = "playNow(FFIIF)V", at = @At("TAIL"), require = 1)
    private void rustedfabricapi$afterGameSoundPlayNow(float leftVolume, float rightVolume, int priority, int loop,
                                                       float pitch, CallbackInfo ci) {
        AudioRuntimeEvents.AFTER_GAME_SOUND_PLAY_NOW.invoker()
                .afterGameSoundPlayNow(this, leftVolume, rightVolume, priority, loop, pitch);
    }
}
