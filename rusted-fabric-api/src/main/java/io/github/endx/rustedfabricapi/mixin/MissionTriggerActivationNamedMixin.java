package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.mission.event.MissionTriggerEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rustedwarfare.mission.MissionEngine;
import rustedwarfare.mission.MissionTrigger;

@Mixin(value = MissionEngine.class, remap = false)
public abstract class MissionTriggerActivationNamedMixin {
    @Inject(method = "activateTrigger(Lrustedwarfare/mission/MissionTrigger;)V",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeActivate(MissionTrigger trigger, CallbackInfo ci) {
        if (MissionTriggerEvents.BEFORE_ACTIVATE.invoker().beforeActivate(engine(), trigger)) {
            ci.cancel();
        }
    }

    @Inject(method = "activateTrigger(Lrustedwarfare/mission/MissionTrigger;)V",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterActivate(MissionTrigger trigger, CallbackInfo ci) {
        MissionTriggerEvents.AFTER_ACTIVATE.invoker().afterActivate(engine(), trigger);
    }

    private MissionEngine engine() {
        return (MissionEngine) (Object) this;
    }
}
