package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.SelectionEvents;
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
        boolean cancelled = SelectionEvents.BEFORE_UNIT_SELECT.invoker().beforeUnitSelect(this, unit, append);
        cancelled |= io.github.endx.rustedfabricapi.api.client.event.SelectionEvents.BEFORE_SELECT.invoker()
                .beforeSelect((rustedwarfare.ui.InterfaceEngine) (Object) this,
                        (rustedwarfare.unit.Unit) unit, append);
        if (cancelled) {
            ci.cancel();
        }
    }

    @Inject(method = "selectUnit(Lrustedwarfare/unit/Unit;Z)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterUnitSelect(@Coerce Object unit, boolean append, CallbackInfo ci) {
        SelectionEvents.AFTER_UNIT_SELECT.invoker().afterUnitSelect(this, unit, append);
        io.github.endx.rustedfabricapi.api.client.event.SelectionEvents.AFTER_SELECT.invoker()
                .afterSelect((rustedwarfare.ui.InterfaceEngine) (Object) this,
                        (rustedwarfare.unit.Unit) unit, append);
    }

    @Inject(method = "addUnitToSelection(Lrustedwarfare/unit/Unit;)Z", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeUnitAddedToSelection(@Coerce Object unit, CallbackInfoReturnable<Boolean> cir) {
        boolean cancelled = SelectionEvents.BEFORE_UNIT_ADDED_TO_SELECTION.invoker()
                .beforeUnitAddedToSelection(this, unit);
        cancelled |= io.github.endx.rustedfabricapi.api.client.event.SelectionEvents.BEFORE_ADD.invoker()
                .beforeAdd((rustedwarfare.ui.InterfaceEngine) (Object) this,
                        (rustedwarfare.unit.Unit) unit);
        if (cancelled) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }

    @Inject(method = "addUnitToSelection(Lrustedwarfare/unit/Unit;)Z", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterUnitAddedToSelection(@Coerce Object unit, CallbackInfoReturnable<Boolean> cir) {
        SelectionEvents.AFTER_UNIT_ADDED_TO_SELECTION.invoker()
                .afterUnitAddedToSelection(this, unit, Boolean.TRUE.equals(cir.getReturnValue()));
        io.github.endx.rustedfabricapi.api.client.event.SelectionEvents.AFTER_ADD.invoker()
                .afterAdd((rustedwarfare.ui.InterfaceEngine) (Object) this,
                        (rustedwarfare.unit.Unit) unit, Boolean.TRUE.equals(cir.getReturnValue()));
    }

    @Inject(method = "deselectUnit(Lrustedwarfare/unit/Unit;)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeUnitDeselect(@Coerce Object unit, CallbackInfo ci) {
        boolean cancelled = SelectionEvents.BEFORE_UNIT_DESELECT.invoker().beforeUnitDeselect(this, unit);
        cancelled |= io.github.endx.rustedfabricapi.api.client.event.SelectionEvents.BEFORE_DESELECT.invoker()
                .beforeUnit((rustedwarfare.ui.InterfaceEngine) (Object) this,
                        (rustedwarfare.unit.Unit) unit);
        if (cancelled) {
            ci.cancel();
        }
    }

    @Inject(method = "deselectUnit(Lrustedwarfare/unit/Unit;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterUnitDeselect(@Coerce Object unit, CallbackInfo ci) {
        SelectionEvents.AFTER_UNIT_DESELECT.invoker().afterUnitDeselect(this, unit);
        io.github.endx.rustedfabricapi.api.client.event.SelectionEvents.AFTER_DESELECT.invoker()
                .afterUnit((rustedwarfare.ui.InterfaceEngine) (Object) this,
                        (rustedwarfare.unit.Unit) unit);
    }

    @Inject(method = "clearSelectedUnits()V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeSelectionClear(CallbackInfo ci) {
        boolean cancelled = SelectionEvents.BEFORE_SELECTION_CLEAR.invoker().beforeSelectionClear(this);
        cancelled |= io.github.endx.rustedfabricapi.api.client.event.SelectionEvents.BEFORE_CLEAR.invoker()
                .beforeClear((rustedwarfare.ui.InterfaceEngine) (Object) this);
        if (cancelled) {
            ci.cancel();
        }
    }

    @Inject(method = "clearSelectedUnits()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterSelectionClear(CallbackInfo ci) {
        SelectionEvents.AFTER_SELECTION_CLEAR.invoker().afterSelectionClear(this);
        io.github.endx.rustedfabricapi.api.client.event.SelectionEvents.AFTER_CLEAR.invoker()
                .afterClear((rustedwarfare.ui.InterfaceEngine) (Object) this);
    }
}
