package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.custom.attachment.event.AttachmentEvents;
import io.github.endx.rustedfabricapi.api.custom.event.CustomUnitTriggerEvents;
import io.github.endx.rustedfabricapi.api.unit.tag.event.UnitTagEvents;
import io.github.endx.rustedfabricapi.impl.custom.DamageEventDataRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rustedwarfare.custom.CustomTagList;
import rustedwarfare.custom.CustomUnit;
import rustedwarfare.custom.attachment.AttachmentSlot;
import rustedwarfare.custom.event.CustomUnitEventType;
import rustedwarfare.custom.logic.VariableScope;
import rustedwarfare.game.Projectile;
import rustedwarfare.unit.OrderableUnit;
import rustedwarfare.unit.Unit;

@Mixin(targets = "rustedwarfare.custom.CustomUnit", remap = false)
public abstract class CustomUnitExtensionNamedMixin {
    @Inject(method = "applyDamage(Lrustedwarfare/unit/Unit;FLrustedwarfare/game/Projectile;)F",
            at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beginEnhancedDamageEvent(Unit attacker, float amount,
                                                          Projectile projectile,
                                                          CallbackInfoReturnable<Float> cir) {
        DamageEventDataRuntime.beginCustomDamage(
                (CustomUnit) (Object) this, attacker, amount, projectile);
    }

    @Inject(method = "applyDamage(Lrustedwarfare/unit/Unit;FLrustedwarfare/game/Projectile;)F",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$endEnhancedDamageEvent(Unit attacker, float amount,
                                                        Projectile projectile,
                                                        CallbackInfoReturnable<Float> cir) {
        DamageEventDataRuntime.endCustomDamage((CustomUnit) (Object) this);
    }

    @Inject(method = "setRuntimeTags(Lrustedwarfare/custom/CustomTagList;Z)V",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeSetRuntimeTags(CustomTagList replacement,
                                                      boolean skipTeamIndexRefresh,
                                                      CallbackInfo ci) {
        CustomUnit unit = (CustomUnit) (Object) this;
        if (UnitTagEvents.BEFORE_SET.invoker().beforeSet(
                unit, unit.getRuntimeTags(), replacement, skipTeamIndexRefresh)) {
            ci.cancel();
        }
    }

    @Inject(method = "setRuntimeTags(Lrustedwarfare/custom/CustomTagList;Z)V",
            at = @At("TAIL"), require = 1)
    private void rustedfabricapi$afterSetRuntimeTags(CustomTagList replacement,
                                                     boolean skipTeamIndexRefresh,
                                                     CallbackInfo ci) {
        CustomUnit unit = (CustomUnit) (Object) this;
        UnitTagEvents.AFTER_SET.invoker().afterSet(
                unit, unit.getRuntimeTags(), skipTeamIndexRefresh);
    }

    @Inject(method = "attachUnitToSlot(Lrustedwarfare/unit/OrderableUnit;Lrustedwarfare/custom/attachment/AttachmentSlot;)Z",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeAttachUnitToSlot(OrderableUnit child, AttachmentSlot slot,
                                                        CallbackInfoReturnable<Boolean> cir) {
        if (AttachmentEvents.BEFORE_ATTACH.invoker().beforeAttach(
                (CustomUnit) (Object) this, child, slot)) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }

    @Inject(method = "attachUnitToSlot(Lrustedwarfare/unit/OrderableUnit;Lrustedwarfare/custom/attachment/AttachmentSlot;)Z",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterAttachUnitToSlot(OrderableUnit child, AttachmentSlot slot,
                                                       CallbackInfoReturnable<Boolean> cir) {
        AttachmentEvents.AFTER_ATTACH.invoker().afterAttach(
                (CustomUnit) (Object) this, child, slot,
                Boolean.TRUE.equals(cir.getReturnValue()));
    }

    @Inject(method = "detachUnit(Lrustedwarfare/unit/OrderableUnit;)Z",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeDetachUnit(OrderableUnit child,
                                                  CallbackInfoReturnable<Boolean> cir) {
        if (AttachmentEvents.BEFORE_DETACH.invoker().beforeDetach(
                (CustomUnit) (Object) this, child,
                child != null ? child.getAttachmentSlot() : null)) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }

    @Inject(method = "detachUnit(Lrustedwarfare/unit/OrderableUnit;)Z",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterDetachUnit(OrderableUnit child,
                                                 CallbackInfoReturnable<Boolean> cir) {
        AttachmentEvents.AFTER_DETACH.invoker().afterDetach(
                (CustomUnit) (Object) this, child,
                Boolean.TRUE.equals(cir.getReturnValue()));
    }

    @Inject(method = "triggerCustomEvent(Lrustedwarfare/custom/event/CustomUnitEventType;)V",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeTriggerCustomEvent(CustomUnitEventType eventType,
                                                          CallbackInfo ci) {
        if (CustomUnitTriggerEvents.BEFORE_TRIGGER.invoker().beforeTrigger(
                (CustomUnit) (Object) this, eventType)) {
            ci.cancel();
        }
    }

    @Inject(method = "triggerCustomEvent(Lrustedwarfare/custom/event/CustomUnitEventType;)V",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterTriggerCustomEvent(CustomUnitEventType eventType,
                                                         CallbackInfo ci) {
        CustomUnitTriggerEvents.AFTER_TRIGGER.invoker().afterTrigger(
                (CustomUnit) (Object) this, eventType);
    }

    @Inject(method = "queueCustomEventWithContext(Lrustedwarfare/custom/event/CustomUnitEventType;Lrustedwarfare/unit/Unit;Lrustedwarfare/custom/CustomTagList;Lrustedwarfare/custom/logic/VariableScope;)V",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeQueueCustomEvent(
            CustomUnitEventType eventType, Unit source, CustomTagList eventTags,
            VariableScope eventData, CallbackInfo ci) {
        if (CustomUnitTriggerEvents.BEFORE_QUEUE.invoker().beforeQueue(
                (CustomUnit) (Object) this, eventType, source, eventTags, eventData)) {
            ci.cancel();
        }
    }

    @Inject(method = "queueCustomEventWithContext(Lrustedwarfare/custom/event/CustomUnitEventType;Lrustedwarfare/unit/Unit;Lrustedwarfare/custom/CustomTagList;Lrustedwarfare/custom/logic/VariableScope;)V",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterQueueCustomEvent(
            CustomUnitEventType eventType, Unit source, CustomTagList eventTags,
            VariableScope eventData, CallbackInfo ci) {
        CustomUnitTriggerEvents.AFTER_QUEUE.invoker().afterQueue(
                (CustomUnit) (Object) this, eventType, source, eventTags, eventData);
    }

    @ModifyVariable(
            method = "queueCustomEventWithContext(Lrustedwarfare/custom/event/CustomUnitEventType;Lrustedwarfare/unit/Unit;Lrustedwarfare/custom/CustomTagList;Lrustedwarfare/custom/logic/VariableScope;)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 1)
    private VariableScope rustedfabricapi$enrichQueuedEventData(
            VariableScope eventData, CustomUnitEventType eventType, Unit source,
            CustomTagList eventTags, VariableScope originalEventData) {
        return DamageEventDataRuntime.enrichQueuedEvent(
                (CustomUnit) (Object) this, eventType, source, eventTags, eventData);
    }
}
