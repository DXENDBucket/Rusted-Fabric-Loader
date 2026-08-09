package io.github.endx.rustedfabricapi.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rustedwarfare.unit.Unit;

@Mixin(targets = "rustedwarfare.custom.CustomUnit", remap = false)
public abstract class CustomUnitTransportUnloadNamedMixin {
    @Inject(method = "unloadNextTransportedUnit(Z)Z", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeUnloadNextTransportedUnit(boolean forced,
                                                                 CallbackInfoReturnable<Boolean> cir) {
        boolean cancelled = io.github.endx.rustedfabricapi.api.unit.transport.event.TransportEvents
                .BEFORE_UNLOAD_NEXT.invoker().beforeUnloadNext((Unit) (Object) this, forced);
        if (cancelled) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }

    @Inject(method = "unloadNextTransportedUnit(Z)Z", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterUnloadNextTransportedUnit(boolean forced,
                                                                CallbackInfoReturnable<Boolean> cir) {
        io.github.endx.rustedfabricapi.api.unit.transport.event.TransportEvents
                .AFTER_UNLOAD_NEXT.invoker().afterUnloadNext(
                        (Unit) (Object) this, forced, Boolean.TRUE.equals(cir.getReturnValue()));
    }

    @Inject(method = "unloadSpecificTransportedUnit(Lrustedwarfare/unit/Unit;ZZ)Z", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeUnloadSpecificTransportedUnit(@Coerce Object transportedUnit,
                                                                     boolean optionA,
                                                                     boolean optionB,
                                                                     CallbackInfoReturnable<Boolean> cir) {
        boolean cancelled = io.github.endx.rustedfabricapi.api.unit.transport.event.TransportEvents
                .BEFORE_UNLOAD_SPECIFIC.invoker().beforeUnloadSpecific(
                        (Unit) (Object) this, (Unit) transportedUnit, optionA, optionB);
        if (cancelled) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }

    @Inject(method = "unloadSpecificTransportedUnit(Lrustedwarfare/unit/Unit;ZZ)Z", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterUnloadSpecificTransportedUnit(@Coerce Object transportedUnit,
                                                                    boolean optionA,
                                                                    boolean optionB,
                                                                    CallbackInfoReturnable<Boolean> cir) {
        io.github.endx.rustedfabricapi.api.unit.transport.event.TransportEvents
                .AFTER_UNLOAD_SPECIFIC.invoker().afterUnloadSpecific(
                        (Unit) (Object) this, (Unit) transportedUnit, optionA, optionB,
                        Boolean.TRUE.equals(cir.getReturnValue()));
    }

    @Inject(method = "releaseAllTransportedUnits(Z)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeReleaseAllTransportedUnits(boolean killUnits, CallbackInfo ci) {
        boolean cancelled = io.github.endx.rustedfabricapi.api.unit.transport.event.TransportEvents
                .BEFORE_RELEASE_ALL.invoker().beforeReleaseAll((Unit) (Object) this, killUnits);
        if (cancelled) {
            ci.cancel();
        }
    }

    @Inject(method = "releaseAllTransportedUnits(Z)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterReleaseAllTransportedUnits(boolean killUnits, CallbackInfo ci) {
        io.github.endx.rustedfabricapi.api.unit.transport.event.TransportEvents
                .AFTER_RELEASE_ALL.invoker().afterReleaseAll((Unit) (Object) this, killUnits);
    }

    @Inject(method = "killOrReleaseTransportedUnitsOnDeath()V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeTransportDeathCargoCleanup(CallbackInfo ci) {
        boolean cancelled = io.github.endx.rustedfabricapi.api.unit.transport.event.TransportEvents
                .BEFORE_DEATH_CARGO_CLEANUP.invoker().beforeCommand((Unit) (Object) this);
        if (cancelled) {
            ci.cancel();
        }
    }

    @Inject(method = "killOrReleaseTransportedUnitsOnDeath()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterTransportDeathCargoCleanup(CallbackInfo ci) {
        io.github.endx.rustedfabricapi.api.unit.transport.event.TransportEvents
                .AFTER_DEATH_CARGO_CLEANUP.invoker().afterCommand((Unit) (Object) this);
    }
}
