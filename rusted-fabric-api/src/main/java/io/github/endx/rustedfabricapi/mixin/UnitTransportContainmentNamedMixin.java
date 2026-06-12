package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.TransportEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "rustedwarfare.unit.Unit", remap = false)
public abstract class UnitTransportContainmentNamedMixin {
    @Inject(method = "canTransportUnitIgnoringCurrentContainer(Lrustedwarfare/unit/Unit;Z)Z", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$modifyCanTransportUnitIgnoringCurrentContainer(@Coerce Object candidate,
                                                                               boolean allowPartial,
                                                                               CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(TransportEvents.MODIFY_CAN_TRANSPORT_UNIT_IGNORING_CURRENT_CONTAINER.invoker()
                .modifyCanTransportUnitIgnoringCurrentContainer(this, candidate, allowPartial,
                        Boolean.TRUE.equals(cir.getReturnValue())));
    }

    @Inject(method = "getContainingUnit()Lrustedwarfare/unit/Unit;", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$modifyContainingUnit(CallbackInfoReturnable<Object> cir) {
        cir.setReturnValue(TransportEvents.MODIFY_CONTAINING_UNIT.invoker()
                .modifyContainingUnit(this, cir.getReturnValue()));
    }

    @Inject(method = "getAttachmentSlot()Lrustedwarfare/custom/attachment/AttachmentSlot;", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$modifyAttachmentSlot(CallbackInfoReturnable<Object> cir) {
        cir.setReturnValue(TransportEvents.MODIFY_ATTACHMENT_SLOT.invoker()
                .modifyAttachmentSlot(this, cir.getReturnValue()));
    }
}
