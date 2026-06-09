package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.SelectionEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.corrodinggames.rts.gameFramework.f.g", remap = false)
public abstract class InterfaceSelectionOfficialMixin {
    @Inject(method = "a(Lcom/corrodinggames/rts/game/units/am;Z)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeUnitSelect(@Coerce Object unit, boolean append, CallbackInfo ci) {
        if (SelectionEvents.BEFORE_UNIT_SELECT.invoker().beforeUnitSelect(this, unit, append)) {
            ci.cancel();
        }
    }

    @Inject(method = "a(Lcom/corrodinggames/rts/game/units/am;Z)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterUnitSelect(@Coerce Object unit, boolean append, CallbackInfo ci) {
        SelectionEvents.AFTER_UNIT_SELECT.invoker().afterUnitSelect(this, unit, append);
    }

    @Inject(method = "j(Lcom/corrodinggames/rts/game/units/am;)Z", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeUnitAddedToSelection(@Coerce Object unit, CallbackInfoReturnable<Boolean> cir) {
        if (SelectionEvents.BEFORE_UNIT_ADDED_TO_SELECTION.invoker().beforeUnitAddedToSelection(this, unit)) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }

    @Inject(method = "j(Lcom/corrodinggames/rts/game/units/am;)Z", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterUnitAddedToSelection(@Coerce Object unit, CallbackInfoReturnable<Boolean> cir) {
        SelectionEvents.AFTER_UNIT_ADDED_TO_SELECTION.invoker()
                .afterUnitAddedToSelection(this, unit, Boolean.TRUE.equals(cir.getReturnValue()));
    }

    @Inject(method = "l(Lcom/corrodinggames/rts/game/units/am;)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeUnitDeselect(@Coerce Object unit, CallbackInfo ci) {
        if (SelectionEvents.BEFORE_UNIT_DESELECT.invoker().beforeUnitDeselect(this, unit)) {
            ci.cancel();
        }
    }

    @Inject(method = "l(Lcom/corrodinggames/rts/game/units/am;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterUnitDeselect(@Coerce Object unit, CallbackInfo ci) {
        SelectionEvents.AFTER_UNIT_DESELECT.invoker().afterUnitDeselect(this, unit);
    }

    @Inject(method = "y()V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeSelectionClear(CallbackInfo ci) {
        if (SelectionEvents.BEFORE_SELECTION_CLEAR.invoker().beforeSelectionClear(this)) {
            ci.cancel();
        }
    }

    @Inject(method = "y()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterSelectionClear(CallbackInfo ci) {
        SelectionEvents.AFTER_SELECTION_CLEAR.invoker().afterSelectionClear(this);
    }
}
