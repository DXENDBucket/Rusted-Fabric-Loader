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
        io.github.endx.rustedfabricapi.api.unit.type.event.UnitTypeEvents.BEFORE_CUSTOM_CREATE
                .invoker().beforeCreate((rustedwarfare.custom.CustomUnitMetadata) (Object) this);
    }

    @Inject(method = "createUnit()Lrustedwarfare/unit/Unit;", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$afterRuntimeUnitCreate(CallbackInfoReturnable<Object> cir) {
        Object result = CustomUnitLifecycleEvents.AFTER_RUNTIME_UNIT_CREATE.invoker()
                .afterRuntimeUnitCreate(this, cir.getReturnValue());
        result = io.github.endx.rustedfabricapi.api.unit.type.event.UnitTypeEvents.AFTER_CUSTOM_CREATE
                .invoker().afterCreate((rustedwarfare.custom.CustomUnitMetadata) (Object) this,
                        (rustedwarfare.unit.Unit) result);
        cir.setReturnValue(result);
    }

    @Inject(method = "createUnitWithFlag(Z)Lrustedwarfare/unit/Unit;", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeRuntimeUnitCreateWithFlag(boolean createFlag, CallbackInfoReturnable<Object> cir) {
        CustomUnitLifecycleEvents.BEFORE_RUNTIME_UNIT_CREATE_WITH_FLAG.invoker().beforeRuntimeUnitCreateWithFlag(this, createFlag);
        io.github.endx.rustedfabricapi.api.unit.type.event.UnitTypeEvents.BEFORE_CUSTOM_CREATE_WITH_FLAG
                .invoker().beforeCreate((rustedwarfare.custom.CustomUnitMetadata) (Object) this,
                        createFlag);
    }

    @Inject(method = "createUnitWithFlag(Z)Lrustedwarfare/unit/Unit;", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$afterRuntimeUnitCreateWithFlag(boolean createFlag, CallbackInfoReturnable<Object> cir) {
        Object result = CustomUnitLifecycleEvents.AFTER_RUNTIME_UNIT_CREATE_WITH_FLAG.invoker()
                .afterRuntimeUnitCreateWithFlag(this, createFlag, cir.getReturnValue());
        result = io.github.endx.rustedfabricapi.api.unit.type.event.UnitTypeEvents.AFTER_CUSTOM_CREATE_WITH_FLAG
                .invoker().afterCreate((rustedwarfare.custom.CustomUnitMetadata) (Object) this,
                        createFlag, (rustedwarfare.unit.Unit) result);
        cir.setReturnValue(result);
    }
}
