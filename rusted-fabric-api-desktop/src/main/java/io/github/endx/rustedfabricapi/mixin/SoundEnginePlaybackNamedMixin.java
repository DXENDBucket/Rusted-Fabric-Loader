package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.audio.SoundEvents;
import io.github.endx.rustedfabricapi.api.audio.SoundPlayback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rustedwarfare.client.audio.GameSound;
import rustedwarfare.client.audio.SoundEngine;

@Mixin(value = SoundEngine.class, remap = false)
public abstract class SoundEnginePlaybackNamedMixin {
    @Inject(method = "playInterfaceSound(Lrustedwarfare/client/audio/GameSound;F)V",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeInterface(GameSound sound, float volume, CallbackInfo ci) {
        if (SoundEvents.BEFORE_PLAY.invoker().beforePlay(engine(),
                SoundPlayback.nonPositional(sound, SoundPlayback.Scope.INTERFACE, volume))) ci.cancel();
    }

    @Inject(method = "playInterfaceSound(Lrustedwarfare/client/audio/GameSound;F)V",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterInterface(GameSound sound, float volume, CallbackInfo ci) {
        SoundEvents.AFTER_PLAY.invoker().afterPlay(engine(),
                SoundPlayback.nonPositional(sound, SoundPlayback.Scope.INTERFACE, volume));
    }

    @Inject(method = "playGameSoundGlobal(Lrustedwarfare/client/audio/GameSound;F)V",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeGlobal(GameSound sound, float volume, CallbackInfo ci) {
        if (SoundEvents.BEFORE_PLAY.invoker().beforePlay(engine(),
                SoundPlayback.nonPositional(sound, SoundPlayback.Scope.GLOBAL, volume))) ci.cancel();
    }

    @Inject(method = "playGameSoundGlobal(Lrustedwarfare/client/audio/GameSound;F)V",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterGlobal(GameSound sound, float volume, CallbackInfo ci) {
        SoundEvents.AFTER_PLAY.invoker().afterPlay(engine(),
                SoundPlayback.nonPositional(sound, SoundPlayback.Scope.GLOBAL, volume));
    }

    @Inject(method = "playGameSoundAt(Lrustedwarfare/client/audio/GameSound;FFF)V",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforePosition(GameSound sound, float volume,
            float x, float y, CallbackInfo ci) {
        if (SoundEvents.BEFORE_PLAY.invoker().beforePlay(engine(),
                SoundPlayback.positional(sound, volume, x, y, 1.0F))) ci.cancel();
    }

    @Inject(method = "playGameSoundAt(Lrustedwarfare/client/audio/GameSound;FFF)V",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterPosition(GameSound sound, float volume,
            float x, float y, CallbackInfo ci) {
        SoundEvents.AFTER_PLAY.invoker().afterPlay(engine(),
                SoundPlayback.positional(sound, volume, x, y, 1.0F));
    }

    @Inject(method = "playGameSoundAtWithPitch(Lrustedwarfare/client/audio/GameSound;FFFF)V",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforePositionPitch(GameSound sound, float volume,
            float x, float y, float pitch, CallbackInfo ci) {
        if (SoundEvents.BEFORE_PLAY.invoker().beforePlay(engine(),
                SoundPlayback.positional(sound, volume, x, y, pitch))) ci.cancel();
    }

    @Inject(method = "playGameSoundAtWithPitch(Lrustedwarfare/client/audio/GameSound;FFFF)V",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterPositionPitch(GameSound sound, float volume,
            float x, float y, float pitch, CallbackInfo ci) {
        SoundEvents.AFTER_PLAY.invoker().afterPlay(engine(),
                SoundPlayback.positional(sound, volume, x, y, pitch));
    }

    private SoundEngine engine() {
        return (SoundEngine) (Object) this;
    }
}
