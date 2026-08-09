package io.github.endx.rustedfabricapi.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "rustedwarfare.game.Team", remap = false)
public abstract class TeamUnitLifecycleNamedMixin {
    @Inject(method = "registerUnit(Lrustedwarfare/unit/Unit;)V", at = @At("HEAD"), require = 1)
    private static void rustedfabricapi$beforeUnitRegister(@Coerce Object unit, CallbackInfo ci) {
        io.github.endx.rustedfabricapi.api.unit.event.UnitEvents.BEFORE_REGISTER.invoker()
                .onUnit((rustedwarfare.unit.Unit) unit);
    }

    @Inject(method = "registerUnit(Lrustedwarfare/unit/Unit;)V", at = @At("RETURN"), require = 1)
    private static void rustedfabricapi$afterUnitRegister(@Coerce Object unit, CallbackInfo ci) {
        io.github.endx.rustedfabricapi.api.unit.event.UnitEvents.AFTER_REGISTER.invoker()
                .onUnit((rustedwarfare.unit.Unit) unit);
    }

    @Inject(method = "unregisterUnit(Lrustedwarfare/unit/Unit;)V", at = @At("HEAD"), require = 1)
    private static void rustedfabricapi$beforeUnitUnregister(@Coerce Object unit, CallbackInfo ci) {
        io.github.endx.rustedfabricapi.api.unit.event.UnitEvents.BEFORE_UNREGISTER.invoker()
                .onUnit((rustedwarfare.unit.Unit) unit);
    }

    @Inject(method = "unregisterUnit(Lrustedwarfare/unit/Unit;)V", at = @At("RETURN"), require = 1)
    private static void rustedfabricapi$afterUnitUnregister(@Coerce Object unit, CallbackInfo ci) {
        io.github.endx.rustedfabricapi.api.unit.event.UnitEvents.AFTER_UNREGISTER.invoker()
                .onUnit((rustedwarfare.unit.Unit) unit);
    }
}
