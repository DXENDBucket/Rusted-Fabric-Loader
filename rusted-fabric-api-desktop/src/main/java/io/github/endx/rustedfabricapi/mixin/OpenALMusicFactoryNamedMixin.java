package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.AudioRuntimeEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "rustedwarfare.audio.OpenALMusicFactory", remap = false)
public abstract class OpenALMusicFactoryNamedMixin {
    @Inject(method = "loadMusicTrack(Ljava/lang/String;)Lrustedwarfare/audio/MusicTrack;", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeMusicTrackLoad(String path, CallbackInfoReturnable<Object> cir) {
        Object override = AudioRuntimeEvents.BEFORE_MUSIC_TRACK_LOAD.invoker()
                .beforeMusicTrackLoad(this, path);
        if (override != null) {
            cir.setReturnValue(override);
        }
    }

    @Inject(method = "loadMusicTrack(Ljava/lang/String;)Lrustedwarfare/audio/MusicTrack;", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$afterMusicTrackLoad(String path, CallbackInfoReturnable<Object> cir) {
        cir.setReturnValue(AudioRuntimeEvents.AFTER_MUSIC_TRACK_LOAD.invoker()
                .afterMusicTrackLoad(this, path, cir.getReturnValue()));
    }

    @Inject(method = "newMusicPlayer()Lrustedwarfare/audio/MusicPlayer;", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$afterNewMusicPlayer(CallbackInfoReturnable<Object> cir) {
        cir.setReturnValue(AudioRuntimeEvents.AFTER_NEW_MUSIC_PLAYER.invoker()
                .afterNewMusicPlayer(this, cir.getReturnValue()));
    }
}
