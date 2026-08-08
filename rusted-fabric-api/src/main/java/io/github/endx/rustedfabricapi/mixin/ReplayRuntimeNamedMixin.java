package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.SaveSyncEvents;
import io.github.endx.rustedfabricapi.api.replay.event.ReplayEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rustedwarfare.replay.ReplayEngine;

@Mixin(targets = "rustedwarfare.replay.ReplayEngine", remap = false)
public abstract class ReplayRuntimeNamedMixin {
    @Inject(method = "startSavingReplay(Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeReplayRecord(String name, CallbackInfo ci) {
        ReplayEngine manager = (ReplayEngine) (Object) this;
        if (ReplayEvents.BEFORE_RECORD.invoker().beforeOperation(manager, name)) ci.cancel();
    }

    @Inject(method = "startSavingReplay(Ljava/lang/String;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterReplayRecord(String name, CallbackInfo ci) {
        ReplayEngine manager = (ReplayEngine) (Object) this;
        ReplayEvents.AFTER_RECORD.invoker().afterOperation(manager, name, manager.isRecording());
    }

    @Inject(method = "loadReplayByName(Ljava/lang/String;)Z", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeReplayPlay(String name, CallbackInfoReturnable<Boolean> cir) {
        ReplayEngine manager = (ReplayEngine) (Object) this;
        if (ReplayEvents.BEFORE_PLAY.invoker().beforeOperation(manager, name)) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }

    @Inject(method = "loadReplayByName(Ljava/lang/String;)Z", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterReplayPlay(String name, CallbackInfoReturnable<Boolean> cir) {
        ReplayEngine manager = (ReplayEngine) (Object) this;
        ReplayEvents.AFTER_PLAY.invoker().afterOperation(manager, name, cir.getReturnValueZ());
    }

    @Inject(method = "closeReplayStreams()V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeReplayStop(CallbackInfo ci) {
        ReplayEvents.BEFORE_STOP.invoker().beforeStop((ReplayEngine) (Object) this);
    }

    @Inject(method = "closeReplayStreams()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterReplayStop(CallbackInfo ci) {
        ReplayEvents.AFTER_STOP.invoker().afterStop((ReplayEngine) (Object) this);
    }

    @Inject(method = "deleteReplay(Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeReplayDelete(String name, CallbackInfo ci) {
        ReplayEngine manager = (ReplayEngine) (Object) this;
        if (ReplayEvents.BEFORE_DELETE.invoker().beforeOperation(manager, name)) ci.cancel();
    }

    @Inject(method = "deleteReplay(Ljava/lang/String;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterReplayDelete(String name, CallbackInfo ci) {
        ReplayEngine manager = (ReplayEngine) (Object) this;
        boolean absent = !manager.getReplayFile(name, true).exists();
        ReplayEvents.AFTER_DELETE.invoker().afterOperation(manager, name, absent);
    }

    @Inject(method = "recordCommand(Lrustedwarfare/command/Command;I)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeReplayRecordCommand(@Coerce Object command, int frame, CallbackInfo ci) {
        if (SaveSyncEvents.BEFORE_REPLAY_RECORD_COMMAND.invoker().beforeReplayRecordCommand(this, command, frame)) {
            ci.cancel();
        }
    }

    @Inject(method = "readNextReplayBlock()Z", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeReplayPlaybackBlock(CallbackInfoReturnable<Boolean> cir) {
        if (SaveSyncEvents.BEFORE_REPLAY_PLAYBACK_BLOCK.invoker().beforeReplayPlaybackBlock(this)) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }
}
