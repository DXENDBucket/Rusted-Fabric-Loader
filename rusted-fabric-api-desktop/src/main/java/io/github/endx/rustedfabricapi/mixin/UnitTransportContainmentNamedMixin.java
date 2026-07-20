package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.TransportEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rustedwarfare.custom.attachment.AttachmentSlot;
import rustedwarfare.unit.Unit;

@Mixin(targets = "rustedwarfare.unit.Unit", remap = false)
public abstract class UnitTransportContainmentNamedMixin {
    @Inject(method = "canTransportUnitIgnoringCurrentContainer(Lrustedwarfare/unit/Unit;Z)Z", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$modifyCanTransportUnitIgnoringCurrentContainer(@Coerce Object candidate,
                                                                               boolean allowPartial,
                                                                               CallbackInfoReturnable<Boolean> cir) {
        boolean result = TransportEvents.MODIFY_CAN_TRANSPORT_UNIT_IGNORING_CURRENT_CONTAINER.invoker()
                .modifyCanTransportUnitIgnoringCurrentContainer(this, candidate, allowPartial,
                        Boolean.TRUE.equals(cir.getReturnValue()));
        Boolean typed = io.github.endx.rustedfabricapi.api.unit.transport.event.TransportEvents
                .MODIFY_CAN_LOAD_IGNORING_CURRENT_CONTAINER.invoker().modify(
                        (Unit) (Object) this, (Unit) candidate, allowPartial, result);
        cir.setReturnValue(Boolean.valueOf(Boolean.TRUE.equals(typed)));
    }

    @Inject(method = "getContainingUnit()Lrustedwarfare/unit/Unit;", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$modifyContainingUnit(CallbackInfoReturnable<Unit> cir) {
        Object common = TransportEvents.MODIFY_CONTAINING_UNIT.invoker()
                .modifyContainingUnit(this, cir.getReturnValue());
        Unit typed = io.github.endx.rustedfabricapi.api.unit.transport.event.TransportEvents
                .MODIFY_CONTAINING_UNIT.invoker().modify(
                        (Unit) (Object) this, common instanceof Unit ? (Unit) common : null);
        cir.setReturnValue(typed);
    }

    @Inject(method = "getAttachmentSlot()Lrustedwarfare/custom/attachment/AttachmentSlot;", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$modifyAttachmentSlot(CallbackInfoReturnable<AttachmentSlot> cir) {
        Object common = TransportEvents.MODIFY_ATTACHMENT_SLOT.invoker()
                .modifyAttachmentSlot(this, cir.getReturnValue());
        AttachmentSlot typed = io.github.endx.rustedfabricapi.api.unit.transport.event.TransportEvents
                .MODIFY_ATTACHMENT_SLOT.invoker().modify((Unit) (Object) this,
                        common instanceof AttachmentSlot ? (AttachmentSlot) common : null);
        cir.setReturnValue(typed);
    }
}
