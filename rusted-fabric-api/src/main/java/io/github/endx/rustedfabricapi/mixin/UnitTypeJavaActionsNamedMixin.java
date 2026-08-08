package io.github.endx.rustedfabricapi.mixin;

import java.util.ArrayList;

import io.github.endx.rustedfabricapi.internal.unit.action.JavaUnitActionRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rustedwarfare.unit.UnitType;

@Mixin(targets = {
        "rustedwarfare.unit.BuiltinUnitType",
        "rustedwarfare.custom.CustomUnitMetadata"
}, remap = false)
public abstract class UnitTypeJavaActionsNamedMixin {
    @Inject(method = "getActionsForTechLevel(I)Ljava/util/ArrayList;",
            at = @At("RETURN"), cancellable = true, require = 1)
    @SuppressWarnings("rawtypes")
    private void rustedfabricapi$appendJavaActions(int techLevel,
            CallbackInfoReturnable<ArrayList> cir) {
        cir.setReturnValue(JavaUnitActionRuntime.append(
                (UnitType) (Object) this, techLevel, cir.getReturnValue()));
    }
}
