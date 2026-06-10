package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.CustomUnitLifecycleEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "rustedwarfare.custom.CustomUnitMetadata", remap = false)
public abstract class CustomUnitMetadataLifecycleNamedMixin {
    @Inject(method = "createUnit()Lrustedwarfare/unit/Unit;", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeRuntimeUnitCreate(CallbackInfoReturnable<Object> cir) {
        CustomUnitLifecycleEvents.BEFORE_RUNTIME_UNIT_CREATE.invoker().beforeRuntimeUnitCreate(this);
    }

    @Inject(method = "createUnit()Lrustedwarfare/unit/Unit;", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$afterRuntimeUnitCreate(CallbackInfoReturnable<Object> cir) {
        cir.setReturnValue(CustomUnitLifecycleEvents.AFTER_RUNTIME_UNIT_CREATE.invoker().afterRuntimeUnitCreate(this, cir.getReturnValue()));
    }

    @Inject(method = "createUnitWithFlag(Z)Lrustedwarfare/unit/Unit;", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeRuntimeUnitCreateWithFlag(boolean createFlag, CallbackInfoReturnable<Object> cir) {
        CustomUnitLifecycleEvents.BEFORE_RUNTIME_UNIT_CREATE_WITH_FLAG.invoker().beforeRuntimeUnitCreateWithFlag(this, createFlag);
    }

    @Inject(method = "createUnitWithFlag(Z)Lrustedwarfare/unit/Unit;", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$afterRuntimeUnitCreateWithFlag(boolean createFlag, CallbackInfoReturnable<Object> cir) {
        cir.setReturnValue(CustomUnitLifecycleEvents.AFTER_RUNTIME_UNIT_CREATE_WITH_FLAG.invoker().afterRuntimeUnitCreateWithFlag(this, createFlag, cir.getReturnValue()));
    }
}
