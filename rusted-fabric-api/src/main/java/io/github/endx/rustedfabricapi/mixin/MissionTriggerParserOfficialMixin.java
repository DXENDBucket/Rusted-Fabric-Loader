package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.MapMissionEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.corrodinggames.rts.gameFramework.n.c", remap = false)
public abstract class MissionTriggerParserOfficialMixin {
    @Inject(method = "a(Lcom/corrodinggames/rts/gameFramework/n/f;Lcom/corrodinggames/rts/game/b/a;)Lcom/corrodinggames/rts/gameFramework/n/a;", at = @At("HEAD"), cancellable = true, require = 1)
    private static void rustedfabricapi$beforeMissionTriggersParse(@Coerce Object missionEngine, @Coerce Object mapObject, CallbackInfoReturnable<Object> cir) {
        if (MapMissionEvents.BEFORE_MISSION_TRIGGERS_PARSE.invoker().beforeMissionTriggersParse(missionEngine, mapObject)) {
            cir.setReturnValue(null);
        }
    }
}
