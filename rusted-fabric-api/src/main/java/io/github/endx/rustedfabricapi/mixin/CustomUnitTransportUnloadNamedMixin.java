package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.TransportEvents;
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
        boolean cancelled = TransportEvents.BEFORE_UNLOAD_NEXT_TRANSPORTED_UNIT.invoker()
                .beforeUnloadNextTransportedUnit(this, forced);
        cancelled |= io.github.endx.rustedfabricapi.api.unit.transport.event.TransportEvents
                .BEFORE_UNLOAD_NEXT.invoker().beforeUnloadNext((Unit) (Object) this, forced);
        if (cancelled) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }

    @Inject(method = "unloadNextTransportedUnit(Z)Z", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterUnloadNextTransportedUnit(boolean forced,
                                                                CallbackInfoReturnable<Boolean> cir) {
        TransportEvents.AFTER_UNLOAD_NEXT_TRANSPORTED_UNIT.invoker()
                .afterUnloadNextTransportedUnit(this, forced, Boolean.TRUE.equals(cir.getReturnValue()));
        io.github.endx.rustedfabricapi.api.unit.transport.event.TransportEvents
                .AFTER_UNLOAD_NEXT.invoker().afterUnloadNext(
                        (Unit) (Object) this, forced, Boolean.TRUE.equals(cir.getReturnValue()));
    }

    @Inject(method = "unloadSpecificTransportedUnit(Lrustedwarfare/unit/Unit;ZZ)Z", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeUnloadSpecificTransportedUnit(@Coerce Object transportedUnit,
                                                                     boolean optionA,
                                                                     boolean optionB,
                                                                     CallbackInfoReturnable<Boolean> cir) {
        boolean cancelled = TransportEvents.BEFORE_UNLOAD_SPECIFIC_TRANSPORTED_UNIT.invoker()
                .beforeUnloadSpecificTransportedUnit(this, transportedUnit, optionA, optionB);
        cancelled |= io.github.endx.rustedfabricapi.api.unit.transport.event.TransportEvents
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
        TransportEvents.AFTER_UNLOAD_SPECIFIC_TRANSPORTED_UNIT.invoker()
                .afterUnloadSpecificTransportedUnit(this, transportedUnit, optionA, optionB,
                        Boolean.TRUE.equals(cir.getReturnValue()));
        io.github.endx.rustedfabricapi.api.unit.transport.event.TransportEvents
                .AFTER_UNLOAD_SPECIFIC.invoker().afterUnloadSpecific(
                        (Unit) (Object) this, (Unit) transportedUnit, optionA, optionB,
                        Boolean.TRUE.equals(cir.getReturnValue()));
    }

    @Inject(method = "releaseAllTransportedUnits(Z)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeReleaseAllTransportedUnits(boolean killUnits, CallbackInfo ci) {
        boolean cancelled = TransportEvents.BEFORE_RELEASE_ALL_TRANSPORTED_UNITS.invoker()
                .beforeReleaseAllTransportedUnits(this, killUnits);
        cancelled |= io.github.endx.rustedfabricapi.api.unit.transport.event.TransportEvents
                .BEFORE_RELEASE_ALL.invoker().beforeReleaseAll((Unit) (Object) this, killUnits);
        if (cancelled) {
            ci.cancel();
        }
    }

    @Inject(method = "releaseAllTransportedUnits(Z)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterReleaseAllTransportedUnits(boolean killUnits, CallbackInfo ci) {
        TransportEvents.AFTER_RELEASE_ALL_TRANSPORTED_UNITS.invoker()
                .afterReleaseAllTransportedUnits(this, killUnits);
        io.github.endx.rustedfabricapi.api.unit.transport.event.TransportEvents
                .AFTER_RELEASE_ALL.invoker().afterReleaseAll((Unit) (Object) this, killUnits);
    }

    @Inject(method = "killOrReleaseTransportedUnitsOnDeath()V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeTransportDeathCargoCleanup(CallbackInfo ci) {
        boolean cancelled = TransportEvents.BEFORE_TRANSPORT_DEATH_CARGO_CLEANUP.invoker()
                .beforeTransportDeathCargoCleanup(this);
        cancelled |= io.github.endx.rustedfabricapi.api.unit.transport.event.TransportEvents
                .BEFORE_DEATH_CARGO_CLEANUP.invoker().beforeCommand((Unit) (Object) this);
        if (cancelled) {
            ci.cancel();
        }
    }

    @Inject(method = "killOrReleaseTransportedUnitsOnDeath()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterTransportDeathCargoCleanup(CallbackInfo ci) {
        TransportEvents.AFTER_TRANSPORT_DEATH_CARGO_CLEANUP.invoker()
                .afterTransportDeathCargoCleanup(this);
        io.github.endx.rustedfabricapi.api.unit.transport.event.TransportEvents
                .AFTER_DEATH_CARGO_CLEANUP.invoker().afterCommand((Unit) (Object) this);
    }
}
