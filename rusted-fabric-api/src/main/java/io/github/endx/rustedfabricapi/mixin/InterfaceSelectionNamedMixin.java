package io.github.endx.rustedfabricapi.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "rustedwarfare.ui.InterfaceEngine", remap = false)
public abstract class InterfaceSelectionNamedMixin {
    @Inject(method = "selectUnit(Lrustedwarfare/unit/Unit;Z)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeUnitSelect(@Coerce Object unit, boolean append, CallbackInfo ci) {
        boolean cancelled = io.github.endx.rustedfabricapi.api.client.event.SelectionEvents.BEFORE_SELECT.invoker()
                .beforeSelect((rustedwarfare.ui.InterfaceEngine) (Object) this,
                        (rustedwarfare.unit.Unit) unit, append);
        if (cancelled) {
            ci.cancel();
        }
    }

    @Inject(method = "selectUnit(Lrustedwarfare/unit/Unit;Z)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterUnitSelect(@Coerce Object unit, boolean append, CallbackInfo ci) {
        io.github.endx.rustedfabricapi.api.client.event.SelectionEvents.AFTER_SELECT.invoker()
                .afterSelect((rustedwarfare.ui.InterfaceEngine) (Object) this,
                        (rustedwarfare.unit.Unit) unit, append);
    }

    @Inject(method = "addUnitToSelection(Lrustedwarfare/unit/Unit;)Z", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeUnitAddedToSelection(@Coerce Object unit, CallbackInfoReturnable<Boolean> cir) {
        boolean cancelled = io.github.endx.rustedfabricapi.api.client.event.SelectionEvents.BEFORE_ADD.invoker()
                .beforeAdd((rustedwarfare.ui.InterfaceEngine) (Object) this,
                        (rustedwarfare.unit.Unit) unit);
        if (cancelled) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }

    @Inject(method = "addUnitToSelection(Lrustedwarfare/unit/Unit;)Z", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterUnitAddedToSelection(@Coerce Object unit, CallbackInfoReturnable<Boolean> cir) {
        io.github.endx.rustedfabricapi.api.client.event.SelectionEvents.AFTER_ADD.invoker()
                .afterAdd((rustedwarfare.ui.InterfaceEngine) (Object) this,
                        (rustedwarfare.unit.Unit) unit, Boolean.TRUE.equals(cir.getReturnValue()));
    }

    @Inject(method = "deselectUnit(Lrustedwarfare/unit/Unit;)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeUnitDeselect(@Coerce Object unit, CallbackInfo ci) {
        boolean cancelled = io.github.endx.rustedfabricapi.api.client.event.SelectionEvents.BEFORE_DESELECT.invoker()
                .beforeUnit((rustedwarfare.ui.InterfaceEngine) (Object) this,
                        (rustedwarfare.unit.Unit) unit);
        if (cancelled) {
            ci.cancel();
        }
    }

    @Inject(method = "deselectUnit(Lrustedwarfare/unit/Unit;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterUnitDeselect(@Coerce Object unit, CallbackInfo ci) {
        io.github.endx.rustedfabricapi.api.client.event.SelectionEvents.AFTER_DESELECT.invoker()
                .afterUnit((rustedwarfare.ui.InterfaceEngine) (Object) this,
                        (rustedwarfare.unit.Unit) unit);
    }

    @Inject(method = "clearSelectedUnits()V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeSelectionClear(CallbackInfo ci) {
        boolean cancelled = io.github.endx.rustedfabricapi.api.client.event.SelectionEvents.BEFORE_CLEAR.invoker()
                .beforeClear((rustedwarfare.ui.InterfaceEngine) (Object) this);
        if (cancelled) {
            ci.cancel();
        }
    }

    @Inject(method = "clearSelectedUnits()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterSelectionClear(CallbackInfo ci) {
        io.github.endx.rustedfabricapi.api.client.event.SelectionEvents.AFTER_CLEAR.invoker()
                .afterClear((rustedwarfare.ui.InterfaceEngine) (Object) this);
    }
}
