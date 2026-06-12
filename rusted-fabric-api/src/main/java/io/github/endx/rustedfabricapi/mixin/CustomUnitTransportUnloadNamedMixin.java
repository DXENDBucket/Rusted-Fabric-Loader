package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.TransportEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "rustedwarfare.custom.CustomUnit", remap = false)
public abstract class CustomUnitTransportUnloadNamedMixin {
    @Inject(method = "unloadNextTransportedUnit(Z)Z", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeUnloadNextTransportedUnit(boolean forced,
                                                                 CallbackInfoReturnable<Boolean> cir) {
        if (TransportEvents.BEFORE_UNLOAD_NEXT_TRANSPORTED_UNIT.invoker()
                .beforeUnloadNextTransportedUnit(this, forced)) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }

    @Inject(method = "unloadNextTransportedUnit(Z)Z", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterUnloadNextTransportedUnit(boolean forced,
                                                                CallbackInfoReturnable<Boolean> cir) {
        TransportEvents.AFTER_UNLOAD_NEXT_TRANSPORTED_UNIT.invoker()
                .afterUnloadNextTransportedUnit(this, forced, Boolean.TRUE.equals(cir.getReturnValue()));
    }

    @Inject(method = "unloadSpecificTransportedUnit(Lrustedwarfare/unit/Unit;ZZ)Z", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeUnloadSpecificTransportedUnit(@Coerce Object transportedUnit,
                                                                     boolean optionA,
                                                                     boolean optionB,
                                                                     CallbackInfoReturnable<Boolean> cir) {
        if (TransportEvents.BEFORE_UNLOAD_SPECIFIC_TRANSPORTED_UNIT.invoker()
                .beforeUnloadSpecificTransportedUnit(this, transportedUnit, optionA, optionB)) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }

    @Inject(method = "unloadSpecificTransportedUnit(Lrustedwarfare/unit/Unit;ZZ)Z", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterUnloadSpecificTransportedUnit(@Coerce Object transportedUnit,
                                                                    boolean optionA,
                                                                    boolean optionB,
                                                                    CallbackInfoReturnable<Boolean> cir) {
        TransportEvents.AFTER_UNLOAD_SPECIFIC_TRANSPORTED_UNIT.invoker()
                .afterUnloadSpecificTransportedUnit(this, transportedUnit, optionA, optionB,
                        Boolean.TRUE.equals(cir.getReturnValue()));
    }

    @Inject(method = "releaseAllTransportedUnits(Z)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeReleaseAllTransportedUnits(boolean killUnits, CallbackInfo ci) {
        if (TransportEvents.BEFORE_RELEASE_ALL_TRANSPORTED_UNITS.invoker()
                .beforeReleaseAllTransportedUnits(this, killUnits)) {
            ci.cancel();
        }
    }

    @Inject(method = "releaseAllTransportedUnits(Z)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterReleaseAllTransportedUnits(boolean killUnits, CallbackInfo ci) {
        TransportEvents.AFTER_RELEASE_ALL_TRANSPORTED_UNITS.invoker()
                .afterReleaseAllTransportedUnits(this, killUnits);
    }

    @Inject(method = "killOrReleaseTransportedUnitsOnDeath()V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeTransportDeathCargoCleanup(CallbackInfo ci) {
        if (TransportEvents.BEFORE_TRANSPORT_DEATH_CARGO_CLEANUP.invoker()
                .beforeTransportDeathCargoCleanup(this)) {
            ci.cancel();
        }
    }

    @Inject(method = "killOrReleaseTransportedUnitsOnDeath()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterTransportDeathCargoCleanup(CallbackInfo ci) {
        TransportEvents.AFTER_TRANSPORT_DEATH_CARGO_CLEANUP.invoker()
                .afterTransportDeathCargoCleanup(this);
    }
}
