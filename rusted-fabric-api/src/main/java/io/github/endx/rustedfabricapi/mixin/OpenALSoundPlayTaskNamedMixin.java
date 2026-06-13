package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.AudioRuntimeEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "rustedwarfare.audio.OpenALSoundPlayTask", remap = false)
public abstract class OpenALSoundPlayTaskNamedMixin {
    @Inject(method = "play()V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeSoundPlayTaskRun(CallbackInfo ci) {
        if (AudioRuntimeEvents.BEFORE_SOUND_PLAY_TASK_RUN.invoker()
                .beforeSoundPlayTaskRun(this)) {
            ci.cancel();
        }
    }

    @Inject(method = "play()V", at = @At("TAIL"), require = 1)
    private void rustedfabricapi$afterSoundPlayTaskRun(CallbackInfo ci) {
        AudioRuntimeEvents.AFTER_SOUND_PLAY_TASK_RUN.invoker()
                .afterSoundPlayTaskRun(this);
    }
}
