package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.AudioRuntimeEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "rustedwarfare.audio.OpenALMusicPlayer", remap = false)
public abstract class OpenALMusicPlayerNamedMixin {
    @Inject(method = "setTrack(Lrustedwarfare/audio/MusicTrack;)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeMusicPlayerSetTrack(@Coerce Object track, CallbackInfo ci) {
        if (AudioRuntimeEvents.BEFORE_MUSIC_PLAYER_SET_TRACK.invoker()
                .beforeMusicPlayerSetTrack(this, track)) {
            ci.cancel();
        }
    }

    @Inject(method = "setTrack(Lrustedwarfare/audio/MusicTrack;)V", at = @At("TAIL"), require = 1)
    private void rustedfabricapi$afterMusicPlayerSetTrack(@Coerce Object track, CallbackInfo ci) {
        AudioRuntimeEvents.AFTER_MUSIC_PLAYER_SET_TRACK.invoker()
                .afterMusicPlayerSetTrack(this, track);
    }

    @Inject(method = "queuePlay(Z)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeMusicPlayerQueuePlay(boolean loop, CallbackInfo ci) {
        if (AudioRuntimeEvents.BEFORE_MUSIC_PLAYER_QUEUE_PLAY.invoker()
                .beforeMusicPlayerQueuePlay(this, loop)) {
            ci.cancel();
        }
    }

    @Inject(method = "queuePlay(Z)V", at = @At("TAIL"), require = 1)
    private void rustedfabricapi$afterMusicPlayerQueuePlay(boolean loop, CallbackInfo ci) {
        AudioRuntimeEvents.AFTER_MUSIC_PLAYER_QUEUE_PLAY.invoker()
                .afterMusicPlayerQueuePlay(this, loop);
    }

    @Inject(method = "playQueuedTrackNow()V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeMusicPlayerPlayQueuedTrackNow(CallbackInfo ci) {
        if (AudioRuntimeEvents.BEFORE_MUSIC_PLAYER_CONTROL.invoker()
                .beforeMusicPlayerControl(this, "playQueuedTrackNow")) {
            ci.cancel();
        }
    }

    @Inject(method = "playQueuedTrackNow()V", at = @At("TAIL"), require = 1)
    private void rustedfabricapi$afterMusicPlayerPlayQueuedTrackNow(CallbackInfo ci) {
        AudioRuntimeEvents.AFTER_MUSIC_PLAYER_CONTROL.invoker()
                .afterMusicPlayerControl(this, "playQueuedTrackNow");
    }

    @Inject(method = "stop()V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeMusicPlayerStop(CallbackInfo ci) {
        if (AudioRuntimeEvents.BEFORE_MUSIC_PLAYER_CONTROL.invoker()
                .beforeMusicPlayerControl(this, "stop")) {
            ci.cancel();
        }
    }

    @Inject(method = "stop()V", at = @At("TAIL"), require = 1)
    private void rustedfabricapi$afterMusicPlayerStop(CallbackInfo ci) {
        AudioRuntimeEvents.AFTER_MUSIC_PLAYER_CONTROL.invoker()
                .afterMusicPlayerControl(this, "stop");
    }

    @Inject(method = "pause()V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeMusicPlayerPause(CallbackInfo ci) {
        if (AudioRuntimeEvents.BEFORE_MUSIC_PLAYER_CONTROL.invoker()
                .beforeMusicPlayerControl(this, "pause")) {
            ci.cancel();
        }
    }

    @Inject(method = "pause()V", at = @At("TAIL"), require = 1)
    private void rustedfabricapi$afterMusicPlayerPause(CallbackInfo ci) {
        AudioRuntimeEvents.AFTER_MUSIC_PLAYER_CONTROL.invoker()
                .afterMusicPlayerControl(this, "pause");
    }

    @Inject(method = "resume()V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeMusicPlayerResume(CallbackInfo ci) {
        if (AudioRuntimeEvents.BEFORE_MUSIC_PLAYER_CONTROL.invoker()
                .beforeMusicPlayerControl(this, "resume")) {
            ci.cancel();
        }
    }

    @Inject(method = "resume()V", at = @At("TAIL"), require = 1)
    private void rustedfabricapi$afterMusicPlayerResume(CallbackInfo ci) {
        AudioRuntimeEvents.AFTER_MUSIC_PLAYER_CONTROL.invoker()
                .afterMusicPlayerControl(this, "resume");
    }

    @Inject(method = "dispose()V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeMusicPlayerDispose(CallbackInfo ci) {
        if (AudioRuntimeEvents.BEFORE_MUSIC_PLAYER_CONTROL.invoker()
                .beforeMusicPlayerControl(this, "dispose")) {
            ci.cancel();
        }
    }

    @Inject(method = "dispose()V", at = @At("TAIL"), require = 1)
    private void rustedfabricapi$afterMusicPlayerDispose(CallbackInfo ci) {
        AudioRuntimeEvents.AFTER_MUSIC_PLAYER_CONTROL.invoker()
                .afterMusicPlayerControl(this, "dispose");
    }
}
