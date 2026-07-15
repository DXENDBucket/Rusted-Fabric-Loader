package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.NetworkSyncEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "rustedwarfare.network.NetworkEngine", remap = false)
public abstract class NetworkSyncDesyncNamedMixin {
    @Inject(method = "updateDesyncDetectionAndResync(F)V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeUpdateDesyncDetectionAndResync(float delta, CallbackInfo ci) {
        NetworkSyncEvents.BEFORE_UPDATE_DESYNC_DETECTION.invoker().onEvent(this, delta);
    }

    @Inject(method = "updateDesyncDetectionAndResync(F)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterUpdateDesyncDetectionAndResync(float delta, CallbackInfo ci) {
        NetworkSyncEvents.AFTER_UPDATE_DESYNC_DETECTION.invoker().onEvent(this, delta);
    }

    @Inject(method = "queueQuickResyncCommand()V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeQueueQuickResyncCommand(CallbackInfo ci) {
        NetworkSyncEvents.BEFORE_QUEUE_QUICK_RESYNC_COMMAND.invoker().onEvent(this);
    }

    @Inject(method = "queueQuickResyncCommand()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterQueueQuickResyncCommand(CallbackInfo ci) {
        NetworkSyncEvents.AFTER_QUEUE_QUICK_RESYNC_COMMAND.invoker().onEvent(this);
    }

    @Inject(method = "applyQuickResyncSave()V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeApplyQuickResyncSave(CallbackInfo ci) {
        NetworkSyncEvents.BEFORE_APPLY_QUICK_RESYNC_SAVE.invoker().onEvent(this);
    }

    @Inject(method = "applyQuickResyncSave()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterApplyQuickResyncSave(CallbackInfo ci) {
        NetworkSyncEvents.AFTER_APPLY_QUICK_RESYNC_SAVE.invoker().onEvent(this);
    }

    @Inject(method = "resetResyncState()V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeResetResyncState(CallbackInfo ci) {
        NetworkSyncEvents.BEFORE_RESET_RESYNC_STATE.invoker().onEvent(this);
    }

    @Inject(method = "resetResyncState()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterResetResyncState(CallbackInfo ci) {
        NetworkSyncEvents.AFTER_RESET_RESYNC_STATE.invoker().onEvent(this);
    }

    @Inject(method = "scheduleReturnToBattleroom(F)V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeScheduleReturnToBattleroom(float seconds, CallbackInfo ci) {
        NetworkSyncEvents.BEFORE_SCHEDULE_RETURN_TO_BATTLEROOM.invoker().onEvent(this, seconds);
    }

    @Inject(method = "scheduleReturnToBattleroom(F)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterScheduleReturnToBattleroom(float seconds, CallbackInfo ci) {
        NetworkSyncEvents.AFTER_SCHEDULE_RETURN_TO_BATTLEROOM.invoker().onEvent(this, seconds);
    }

    @Inject(method = "sendReturnToBattleroom(Lrustedwarfare/network/NetworkConnection;)V",
            at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeSendReturnToBattleroom(@Coerce Object connection, CallbackInfo ci) {
        NetworkSyncEvents.BEFORE_SEND_RETURN_TO_BATTLEROOM.invoker().onEvent(this, connection);
    }

    @Inject(method = "sendReturnToBattleroom(Lrustedwarfare/network/NetworkConnection;)V",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterSendReturnToBattleroom(@Coerce Object connection, CallbackInfo ci) {
        NetworkSyncEvents.AFTER_SEND_RETURN_TO_BATTLEROOM.invoker().onEvent(this, connection);
    }

    @Inject(method = "markReturnToBattleroomPending()V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeMarkReturnToBattleroomPending(CallbackInfo ci) {
        NetworkSyncEvents.BEFORE_MARK_RETURN_TO_BATTLEROOM_PENDING.invoker().onEvent(this);
    }

    @Inject(method = "markReturnToBattleroomPending()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterMarkReturnToBattleroomPending(CallbackInfo ci) {
        NetworkSyncEvents.AFTER_MARK_RETURN_TO_BATTLEROOM_PENDING.invoker().onEvent(this);
    }

    @Inject(method = "returnToBattleroom()V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeReturnToBattleroom(CallbackInfo ci) {
        NetworkSyncEvents.BEFORE_RETURN_TO_BATTLEROOM.invoker().onEvent(this);
    }

    @Inject(method = "returnToBattleroom()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterReturnToBattleroom(CallbackInfo ci) {
        NetworkSyncEvents.AFTER_RETURN_TO_BATTLEROOM.invoker().onEvent(this);
    }

    @Inject(method = "banConnection(Lrustedwarfare/network/NetworkConnection;Ljava/lang/String;I)Z",
            at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeBanConnection(@Coerce Object connection, String reason, int minutes,
                                                     CallbackInfoReturnable<Boolean> cir) {
        NetworkSyncEvents.BEFORE_BAN_CONNECTION.invoker().onEvent(this, connection, reason, minutes);
    }

    @Inject(method = "banConnection(Lrustedwarfare/network/NetworkConnection;Ljava/lang/String;I)Z",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterBanConnection(@Coerce Object connection, String reason, int minutes,
                                                    CallbackInfoReturnable<Boolean> cir) {
        NetworkSyncEvents.AFTER_BAN_CONNECTION.invoker()
                .onEvent(this, connection, reason, minutes, Boolean.TRUE.equals(cir.getReturnValue()));
    }

    @Inject(method = "clearBanEntries()V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeClearBanEntries(CallbackInfo ci) {
        NetworkSyncEvents.BEFORE_CLEAR_BAN_ENTRIES.invoker().onEvent(this);
    }

    @Inject(method = "clearBanEntries()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterClearBanEntries(CallbackInfo ci) {
        NetworkSyncEvents.AFTER_CLEAR_BAN_ENTRIES.invoker().onEvent(this);
    }

    @Inject(method = "pruneExpiredBanEntries()V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforePruneExpiredBanEntries(CallbackInfo ci) {
        NetworkSyncEvents.BEFORE_PRUNE_EXPIRED_BAN_ENTRIES.invoker().onEvent(this);
    }

    @Inject(method = "pruneExpiredBanEntries()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterPruneExpiredBanEntries(CallbackInfo ci) {
        NetworkSyncEvents.AFTER_PRUNE_EXPIRED_BAN_ENTRIES.invoker().onEvent(this);
    }

    @Inject(method = "removeConnection(Lrustedwarfare/network/NetworkConnection;)V",
            at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeRemoveConnection(@Coerce Object connection, CallbackInfo ci) {
        NetworkSyncEvents.BEFORE_REMOVE_CONNECTION.invoker().onEvent(this, connection);
    }

    @Inject(method = "removeConnection(Lrustedwarfare/network/NetworkConnection;)V",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterRemoveConnection(@Coerce Object connection, CallbackInfo ci) {
        NetworkSyncEvents.AFTER_REMOVE_CONNECTION.invoker().onEvent(this, connection);
    }

    @Inject(method = "pruneDisconnectedConnections()V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforePruneDisconnectedConnections(CallbackInfo ci) {
        NetworkSyncEvents.BEFORE_PRUNE_DISCONNECTED_CONNECTIONS.invoker().onEvent(this);
    }

    @Inject(method = "pruneDisconnectedConnections()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterPruneDisconnectedConnections(CallbackInfo ci) {
        NetworkSyncEvents.AFTER_PRUNE_DISCONNECTED_CONNECTIONS.invoker().onEvent(this);
    }

    @Inject(
            method = "createForwardedConnection(Lrustedwarfare/network/NetworkConnection;ILjava/lang/String;Ljava/lang/String;)Lrustedwarfare/network/NetworkConnection;",
            at = @At("HEAD"),
            require = 1
    )
    private void rustedfabricapi$beforeCreateForwardedConnection(@Coerce Object parentConnection,
                                                                 int forwardedClientId,
                                                                 String host,
                                                                 String queryString,
                                                                 CallbackInfoReturnable<Object> cir) {
        NetworkSyncEvents.BEFORE_CREATE_FORWARDED_CONNECTION.invoker()
                .onEvent(this, parentConnection, forwardedClientId, host, queryString);
    }

    @Inject(
            method = "createForwardedConnection(Lrustedwarfare/network/NetworkConnection;ILjava/lang/String;Ljava/lang/String;)Lrustedwarfare/network/NetworkConnection;",
            at = @At("RETURN"),
            require = 1
    )
    private void rustedfabricapi$afterCreateForwardedConnection(@Coerce Object parentConnection,
                                                                int forwardedClientId,
                                                                String host,
                                                                String queryString,
                                                                CallbackInfoReturnable<Object> cir) {
        NetworkSyncEvents.AFTER_CREATE_FORWARDED_CONNECTION.invoker()
                .onEvent(this, parentConnection, forwardedClientId, host, queryString, cir.getReturnValue());
    }

    @Inject(method = "closeConnectionWithReason(Lrustedwarfare/network/NetworkConnection;Ljava/lang/String;)V",
            at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeCloseConnectionWithReason(@Coerce Object connection, String reason,
                                                                 CallbackInfo ci) {
        NetworkSyncEvents.BEFORE_CLOSE_CONNECTION_WITH_REASON.invoker().onEvent(this, connection, reason);
    }

    @Inject(method = "closeConnectionWithReason(Lrustedwarfare/network/NetworkConnection;Ljava/lang/String;)V",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterCloseConnectionWithReason(@Coerce Object connection, String reason,
                                                                CallbackInfo ci) {
        NetworkSyncEvents.AFTER_CLOSE_CONNECTION_WITH_REASON.invoker().onEvent(this, connection, reason);
    }

    @Inject(method = "closeForwardedChildConnectionsWithReason(Lrustedwarfare/network/NetworkConnection;Ljava/lang/String;)V",
            at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeCloseForwardedChildConnectionsWithReason(@Coerce Object connection,
                                                                                String reason,
                                                                                CallbackInfo ci) {
        NetworkSyncEvents.BEFORE_CLOSE_FORWARDED_CHILD_CONNECTIONS.invoker().onEvent(this, connection, reason);
    }

    @Inject(method = "closeForwardedChildConnectionsWithReason(Lrustedwarfare/network/NetworkConnection;Ljava/lang/String;)V",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterCloseForwardedChildConnectionsWithReason(@Coerce Object connection,
                                                                               String reason,
                                                                               CallbackInfo ci) {
        NetworkSyncEvents.AFTER_CLOSE_FORWARDED_CHILD_CONNECTIONS.invoker().onEvent(this, connection, reason);
    }
}
