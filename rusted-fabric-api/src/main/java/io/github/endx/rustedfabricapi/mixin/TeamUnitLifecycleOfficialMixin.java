package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.UnitLifecycleEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.corrodinggames.rts.game.n", remap = false)
public abstract class TeamUnitLifecycleOfficialMixin {
    @Inject(method = "c(Lcom/corrodinggames/rts/game/units/am;)V", at = @At("HEAD"), require = 1)
    private static void rustedfabricapi$beforeUnitRegister(@Coerce Object unit, CallbackInfo ci) {
        UnitLifecycleEvents.BEFORE_UNIT_REGISTER.invoker().beforeUnitRegister(unit);
    }

    @Inject(method = "c(Lcom/corrodinggames/rts/game/units/am;)V", at = @At("RETURN"), require = 1)
    private static void rustedfabricapi$afterUnitRegister(@Coerce Object unit, CallbackInfo ci) {
        UnitLifecycleEvents.AFTER_UNIT_REGISTER.invoker().afterUnitRegister(unit);
    }

    @Inject(method = "b(Lcom/corrodinggames/rts/game/units/am;)V", at = @At("HEAD"), require = 1)
    private static void rustedfabricapi$beforeUnitUnregister(@Coerce Object unit, CallbackInfo ci) {
        UnitLifecycleEvents.BEFORE_UNIT_UNREGISTER.invoker().beforeUnitUnregister(unit);
    }

    @Inject(method = "b(Lcom/corrodinggames/rts/game/units/am;)V", at = @At("RETURN"), require = 1)
    private static void rustedfabricapi$afterUnitUnregister(@Coerce Object unit, CallbackInfo ci) {
        UnitLifecycleEvents.AFTER_UNIT_UNREGISTER.invoker().afterUnitUnregister(unit);
    }
}
