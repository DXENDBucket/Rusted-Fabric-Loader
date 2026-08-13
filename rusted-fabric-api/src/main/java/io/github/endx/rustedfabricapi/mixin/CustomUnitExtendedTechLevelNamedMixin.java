package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.internal.unit.action.JavaUnitActionRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rustedwarfare.custom.CustomUnitMetadata;
import rustedwarfare.unit.UnitType;

import java.util.ArrayList;

/** Makes the native three-slot action table safe for custom unit tech levels above T3. */
@Mixin(targets = "rustedwarfare.custom.CustomUnitMetadata", remap = false)
public abstract class CustomUnitExtendedTechLevelNamedMixin {
    private static final int RUSTEDFABRICAPI_NATIVE_MAX_TECH_LEVEL = 3;

    @Inject(method = "getActionsForTechLevel(I)Ljava/util/ArrayList;",
            at = @At("HEAD"), cancellable = true, require = 1)
    @SuppressWarnings("rawtypes")
    private void rustedfabricapi$useHighestNativeActionsForExtendedLevel(int techLevel,
            CallbackInfoReturnable<ArrayList> cir) {
        if (techLevel <= RUSTEDFABRICAPI_NATIVE_MAX_TECH_LEVEL) return;
        CustomUnitMetadata self = (CustomUnitMetadata) (Object) this;
        ArrayList nativeActions = self.getActionsForTechLevel(
                RUSTEDFABRICAPI_NATIVE_MAX_TECH_LEVEL);
        // Preserve Java actions explicitly bound to T4+ while using T3 as the native fallback.
        cir.setReturnValue(JavaUnitActionRuntime.append((UnitType) self, techLevel, nativeActions));
    }
}
