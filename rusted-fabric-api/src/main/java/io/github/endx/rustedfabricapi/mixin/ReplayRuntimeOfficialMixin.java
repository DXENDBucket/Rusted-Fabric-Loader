package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.SaveSyncEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.corrodinggames.rts.gameFramework.ba", remap = false)
public abstract class ReplayRuntimeOfficialMixin {
    @Inject(method = "a(Lcom/corrodinggames/rts/gameFramework/e;I)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeReplayRecordCommand(@Coerce Object command, int frame, CallbackInfo ci) {
        if (SaveSyncEvents.BEFORE_REPLAY_RECORD_COMMAND.invoker().beforeReplayRecordCommand(this, command, frame)) {
            ci.cancel();
        }
    }

    @Inject(method = "h()Z", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeReplayPlaybackBlock(CallbackInfoReturnable<Boolean> cir) {
        if (SaveSyncEvents.BEFORE_REPLAY_PLAYBACK_BLOCK.invoker().beforeReplayPlaybackBlock(this)) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }
}
