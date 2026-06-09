package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.MapMissionEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.corrodinggames.rts.gameFramework.n.d", remap = false)
public abstract class MissionTriggerLinkerOfficialMixin {
    @Inject(method = "a(Lcom/corrodinggames/rts/gameFramework/n/f;Lcom/corrodinggames/rts/gameFramework/n/a;)V", at = @At("RETURN"), require = 1)
    private static void rustedfabricapi$afterMissionTriggersLinked(@Coerce Object missionEngine, @Coerce Object trigger, CallbackInfo ci) {
        MapMissionEvents.AFTER_MISSION_TRIGGERS_LINKED.invoker().afterMissionTriggersLinked(missionEngine, trigger);
    }
}
