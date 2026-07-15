package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.HudCommandEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "rustedwarfare.ui.InterfaceEngine", remap = false)
public abstract class InterfaceCommandNamedMixin {
    @Inject(method = "issueMoveCommandAtWorldPosition(FFLandroid/graphics/Point;)V",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeIssueMoveCommandAtWorldPosition(
            float worldX, float worldY, @Coerce Object screenPoint, CallbackInfo ci) {
        if (HudCommandEvents.BEFORE_ISSUE_MOVE_COMMAND_AT_WORLD_POSITION.invoker()
                .beforeWorldPointCommand(this, worldX, worldY, screenPoint)) {
            ci.cancel();
        }
    }

    @Inject(method = "issueMoveCommandAtWorldPosition(FFLandroid/graphics/Point;)V",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterIssueMoveCommandAtWorldPosition(
            float worldX, float worldY, @Coerce Object screenPoint, CallbackInfo ci) {
        HudCommandEvents.AFTER_ISSUE_MOVE_COMMAND_AT_WORLD_POSITION.invoker()
                .afterWorldPointCommand(this, worldX, worldY, screenPoint);
    }

    @Inject(method = "issueDefaultMoveOrAttackMove(FFLandroid/graphics/Point;)V",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeIssueDefaultMoveOrAttackMove(
            float worldX, float worldY, @Coerce Object screenPoint, CallbackInfo ci) {
        if (HudCommandEvents.BEFORE_ISSUE_DEFAULT_MOVE_OR_ATTACK_MOVE.invoker()
                .beforeWorldPointCommand(this, worldX, worldY, screenPoint)) {
            ci.cancel();
        }
    }

    @Inject(method = "issueDefaultMoveOrAttackMove(FFLandroid/graphics/Point;)V",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterIssueDefaultMoveOrAttackMove(
            float worldX, float worldY, @Coerce Object screenPoint, CallbackInfo ci) {
        HudCommandEvents.AFTER_ISSUE_DEFAULT_MOVE_OR_ATTACK_MOVE.invoker()
                .afterWorldPointCommand(this, worldX, worldY, screenPoint);
    }

    @Inject(method = "issueAttackMoveAtWorldPosition(FFLandroid/graphics/Point;)V",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeIssueAttackMoveAtWorldPosition(
            float worldX, float worldY, @Coerce Object screenPoint, CallbackInfo ci) {
        if (HudCommandEvents.BEFORE_ISSUE_ATTACK_MOVE_AT_WORLD_POSITION.invoker()
                .beforeWorldPointCommand(this, worldX, worldY, screenPoint)) {
            ci.cancel();
        }
    }

    @Inject(method = "issueAttackMoveAtWorldPosition(FFLandroid/graphics/Point;)V",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterIssueAttackMoveAtWorldPosition(
            float worldX, float worldY, @Coerce Object screenPoint, CallbackInfo ci) {
        HudCommandEvents.AFTER_ISSUE_ATTACK_MOVE_AT_WORLD_POSITION.invoker()
                .afterWorldPointCommand(this, worldX, worldY, screenPoint);
    }

    @Inject(method = "issueQuickRallyAtWorldPosition(FF)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeIssueQuickRallyAtWorldPosition(
            float worldX, float worldY, CallbackInfo ci) {
        if (HudCommandEvents.BEFORE_ISSUE_QUICK_RALLY_AT_WORLD_POSITION.invoker()
                .beforeWorldCommand(this, worldX, worldY)) {
            ci.cancel();
        }
    }

    @Inject(method = "issueQuickRallyAtWorldPosition(FF)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterIssueQuickRallyAtWorldPosition(
            float worldX, float worldY, CallbackInfo ci) {
        HudCommandEvents.AFTER_ISSUE_QUICK_RALLY_AT_WORLD_POSITION.invoker()
                .afterWorldCommand(this, worldX, worldY);
    }

    @Inject(method = "sendMapPingAtWorldPosition(FFLandroid/graphics/Point;Lrustedwarfare/unit/action/PingMapAction;)V",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeSendMapPingAtWorldPosition(
            float worldX, float worldY, @Coerce Object screenPoint, @Coerce Object pingAction, CallbackInfo ci) {
        if (HudCommandEvents.BEFORE_SEND_MAP_PING_AT_WORLD_POSITION.invoker()
                .beforeSendMapPing(this, worldX, worldY, screenPoint, pingAction)) {
            ci.cancel();
        }
    }

    @Inject(method = "sendMapPingAtWorldPosition(FFLandroid/graphics/Point;Lrustedwarfare/unit/action/PingMapAction;)V",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterSendMapPingAtWorldPosition(
            float worldX, float worldY, @Coerce Object screenPoint, @Coerce Object pingAction, CallbackInfo ci) {
        HudCommandEvents.AFTER_SEND_MAP_PING_AT_WORLD_POSITION.invoker()
                .afterSendMapPing(this, worldX, worldY, screenPoint, pingAction);
    }

    @Inject(method = "showMapPingEffect(FFLrustedwarfare/game/Team;Lrustedwarfare/unit/action/PingMapAction;)V",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeShowMapPingEffect(
            float worldX, float worldY, @Coerce Object team, @Coerce Object pingAction, CallbackInfo ci) {
        if (HudCommandEvents.BEFORE_SHOW_MAP_PING_EFFECT.invoker()
                .beforeShowMapPingEffect(this, worldX, worldY, team, pingAction)) {
            ci.cancel();
        }
    }

    @Inject(method = "showMapPingEffect(FFLrustedwarfare/game/Team;Lrustedwarfare/unit/action/PingMapAction;)V",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterShowMapPingEffect(
            float worldX, float worldY, @Coerce Object team, @Coerce Object pingAction, CallbackInfo ci) {
        HudCommandEvents.AFTER_SHOW_MAP_PING_EFFECT.invoker()
                .afterShowMapPingEffect(this, worldX, worldY, team, pingAction);
    }
}
