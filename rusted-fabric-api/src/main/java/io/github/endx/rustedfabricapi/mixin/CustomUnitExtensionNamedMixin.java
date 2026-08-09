package io.github.endx.rustedfabricapi.mixin;

import android.graphics.PointF;
import io.github.endx.rustedfabricapi.api.custom.attachment.event.AttachmentEvents;
import io.github.endx.rustedfabricapi.api.custom.event.CustomUnitTriggerEvents;
import io.github.endx.rustedfabricapi.api.unit.tag.event.UnitTagEvents;
import io.github.endx.rustedfabricapi.impl.custom.DamageEventDataRuntime;
import io.github.endx.rustedfabricapi.impl.custom.NativeEventDataRuntime;
import io.github.endx.rustedfabricapi.impl.custom.QueuedEventActionRuntime;
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
import rustedwarfare.game.Team;
import rustedwarfare.unit.OrderableUnit;
import rustedwarfare.unit.Unit;
import rustedwarfare.unit.UnitOrder;
import rustedwarfare.unit.action.UnitAction;
import rustedwarfare.unit.build.BuildQueueItem;

@Mixin(targets = "rustedwarfare.custom.CustomUnit", remap = false)
public abstract class CustomUnitExtensionNamedMixin {
    @Inject(method = "completeBuildQueueItem(Lrustedwarfare/unit/build/BuildQueueItem;)V",
            at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beginFinishedQueueItem(BuildQueueItem item, CallbackInfo ci) {
        NativeEventDataRuntime.beginFinishedQueueItem(
                (CustomUnit) (Object) this, item);
    }

    @Inject(method = "completeBuildQueueItem(Lrustedwarfare/unit/build/BuildQueueItem;)V",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$endFinishedQueueItem(BuildQueueItem item, CallbackInfo ci) {
        NativeEventDataRuntime.endFinishedQueueItem(
                (CustomUnit) (Object) this, item);
    }

    @Inject(
            method = "queueAction(Lrustedwarfare/unit/action/UnitAction;ZLandroid/graphics/PointF;Lrustedwarfare/unit/Unit;)V",
            at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beginQueueEventContext(UnitAction action, boolean cancellation,
                                                        PointF targetPoint, Unit targetUnit,
                                                        CallbackInfo ci) {
        NativeEventDataRuntime.beginQueueAction(
                (CustomUnit) (Object) this, action, cancellation, targetPoint, targetUnit);
    }

    @Inject(
            method = "queueAction(Lrustedwarfare/unit/action/UnitAction;ZLandroid/graphics/PointF;Lrustedwarfare/unit/Unit;)V",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$endQueueEventContext(UnitAction action, boolean cancellation,
                                                      PointF targetPoint, Unit targetUnit,
                                                      CallbackInfo ci) {
        NativeEventDataRuntime.endQueueAction((CustomUnit) (Object) this, action);
    }

    @Inject(method = "a(Lrustedwarfare/unit/UnitOrder;)V",
            at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beginWaypointEventContext(UnitOrder order, CallbackInfo ci) {
        NativeEventDataRuntime.beginWaypoint((CustomUnit) (Object) this, order);
    }

    @Inject(method = "a(Lrustedwarfare/unit/UnitOrder;)V",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$endWaypointEventContext(UnitOrder order, CallbackInfo ci) {
        NativeEventDataRuntime.endWaypoint((CustomUnit) (Object) this);
    }

    @Inject(method = "changeTeam(Lrustedwarfare/game/Team;)V",
            at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beginTeamEventContext(Team newTeam, CallbackInfo ci) {
        NativeEventDataRuntime.beginTeamChange((CustomUnit) (Object) this, newTeam);
    }

    @Inject(method = "changeTeam(Lrustedwarfare/game/Team;)V",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$endTeamEventContext(Team newTeam, CallbackInfo ci) {
        NativeEventDataRuntime.endTeamChange((CustomUnit) (Object) this);
    }

    @Inject(method = "f(FF)V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beginTeleportEventContext(float x, float y, CallbackInfo ci) {
        NativeEventDataRuntime.beginTeleport((CustomUnit) (Object) this);
    }

    @Inject(method = "f(FF)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$endTeleportEventContext(float x, float y, CallbackInfo ci) {
        NativeEventDataRuntime.endTeleport((CustomUnit) (Object) this);
    }

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
            return;
        }
        NativeEventDataRuntime.beginAttachmentRemoval(
                (CustomUnit) (Object) this, child);
    }

    @Inject(method = "detachUnit(Lrustedwarfare/unit/OrderableUnit;)Z",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterDetachUnit(OrderableUnit child,
                                                 CallbackInfoReturnable<Boolean> cir) {
        AttachmentEvents.AFTER_DETACH.invoker().afterDetach(
                (CustomUnit) (Object) this, child,
                Boolean.TRUE.equals(cir.getReturnValue()));
        NativeEventDataRuntime.endAttachmentRemoval(
                (CustomUnit) (Object) this, child);
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
        CustomUnit unit = (CustomUnit) (Object) this;
        VariableScope nativeData = NativeEventDataRuntime.enrichQueuedEvent(
                unit, eventType, source, eventTags, eventData);
        VariableScope enriched = DamageEventDataRuntime.enrichQueuedEvent(
                unit, eventType, source, eventTags, nativeData);
        VariableScope prepared = enriched != null ? enriched : new VariableScope();
        if (CustomUnitTriggerEvents.PREPARE_QUEUE.invoker().beforeQueue(
                unit, eventType, source, eventTags, prepared)) {
            QueuedEventActionRuntime.cancel(prepared);
        }
        return prepared;
    }
}
