package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.NetworkCallbackEvents;
import io.github.endx.rustedfabricapi.api.networking.event.ConnectionEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "rustedwarfare.network.NetworkCallbacks", remap = false)
public abstract class NetworkCallbacksNamedMixin {
    @Inject(method = "allowClientChatMessage(Lrustedwarfare/network/NetworkConnection;Ljava/lang/String;Ljava/lang/String;)Z",
            at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeAllowClientChatMessage(@Coerce Object connection,
                                                              String senderName,
                                                              String message,
                                                              CallbackInfoReturnable<Boolean> cir) {
        NetworkCallbackEvents.BEFORE_ALLOW_CLIENT_CHAT_MESSAGE.invoker()
                .onEvent(this, connection, senderName, message);
    }

    @Inject(method = "allowClientChatMessage(Lrustedwarfare/network/NetworkConnection;Ljava/lang/String;Ljava/lang/String;)Z",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterAllowClientChatMessage(@Coerce Object connection,
                                                             String senderName,
                                                             String message,
                                                             CallbackInfoReturnable<Boolean> cir) {
        NetworkCallbackEvents.AFTER_ALLOW_CLIENT_CHAT_MESSAGE.invoker()
                .onEvent(this, connection, senderName, message, Boolean.TRUE.equals(cir.getReturnValue()));
    }

    @Inject(method = "allowServerChatMessage(Lrustedwarfare/network/NetworkConnection;Lrustedwarfare/game/Team;Ljava/lang/String;Z)Z",
            at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeAllowServerChatMessage(@Coerce Object connection,
                                                              @Coerce Object team,
                                                              String message,
                                                              boolean teamOnly,
                                                              CallbackInfoReturnable<Boolean> cir) {
        NetworkCallbackEvents.BEFORE_ALLOW_SERVER_CHAT_MESSAGE.invoker()
                .onEvent(this, connection, team, message, teamOnly);
    }

    @Inject(method = "allowServerChatMessage(Lrustedwarfare/network/NetworkConnection;Lrustedwarfare/game/Team;Ljava/lang/String;Z)Z",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterAllowServerChatMessage(@Coerce Object connection,
                                                             @Coerce Object team,
                                                             String message,
                                                             boolean teamOnly,
                                                             CallbackInfoReturnable<Boolean> cir) {
        NetworkCallbackEvents.AFTER_ALLOW_SERVER_CHAT_MESSAGE.invoker()
                .onEvent(this, connection, team, message, teamOnly, Boolean.TRUE.equals(cir.getReturnValue()));
    }

    @Inject(method = "onClientChatMessageAccepted(Lrustedwarfare/network/NetworkConnection;Ljava/lang/String;Ljava/lang/String;)V",
            at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeOnClientChatMessageAccepted(@Coerce Object connection,
                                                                   String senderName,
                                                                   String message,
                                                                   CallbackInfo ci) {
        NetworkCallbackEvents.BEFORE_ON_CLIENT_CHAT_MESSAGE_ACCEPTED.invoker()
                .onEvent(this, connection, senderName, message);
    }

    @Inject(method = "onClientChatMessageAccepted(Lrustedwarfare/network/NetworkConnection;Ljava/lang/String;Ljava/lang/String;)V",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterOnClientChatMessageAccepted(@Coerce Object connection,
                                                                  String senderName,
                                                                  String message,
                                                                  CallbackInfo ci) {
        NetworkCallbackEvents.AFTER_ON_CLIENT_CHAT_MESSAGE_ACCEPTED.invoker()
                .onEvent(this, connection, senderName, message);
    }

    @Inject(
            method = "validateNewPlayerJoin(Lrustedwarfare/network/NetworkConnection;Ljava/lang/String;IILjava/lang/String;Lcom/corrodinggames/rts/game/e;)Ljava/lang/String;",
            at = @At("HEAD"),
            require = 1
    )
    private void rustedfabricapi$beforeValidateNewPlayerJoin(@Coerce Object connection,
                                                             String playerName,
                                                             int networkVersion,
                                                             int appVersion,
                                                             String packageName,
                                                             @Coerce Object playerColor,
                                                             CallbackInfoReturnable<String> cir) {
        NetworkCallbackEvents.BEFORE_VALIDATE_NEW_PLAYER_JOIN.invoker()
                .onEvent(this, connection, playerName, networkVersion, appVersion, packageName, playerColor);
    }

    @Inject(
            method = "validateNewPlayerJoin(Lrustedwarfare/network/NetworkConnection;Ljava/lang/String;IILjava/lang/String;Lcom/corrodinggames/rts/game/e;)Ljava/lang/String;",
            at = @At("RETURN"),
            require = 1
    )
    private void rustedfabricapi$afterValidateNewPlayerJoin(@Coerce Object connection,
                                                            String playerName,
                                                            int networkVersion,
                                                            int appVersion,
                                                            String packageName,
                                                            @Coerce Object playerColor,
                                                            CallbackInfoReturnable<String> cir) {
        NetworkCallbackEvents.AFTER_VALIDATE_NEW_PLAYER_JOIN.invoker()
                .onEvent(this, connection, playerName, networkVersion, appVersion,
                        packageName, playerColor, cir.getReturnValue());
    }

    @Inject(method = "validatePlayerSlotJoin(Lrustedwarfare/network/NetworkConnection;Ljava/lang/String;)Ljava/lang/String;",
            at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeValidatePlayerSlotJoin(@Coerce Object connection,
                                                              String playerName,
                                                              CallbackInfoReturnable<String> cir) {
        NetworkCallbackEvents.BEFORE_VALIDATE_PLAYER_SLOT_JOIN.invoker()
                .onEvent(this, connection, playerName);
    }

    @Inject(method = "validatePlayerSlotJoin(Lrustedwarfare/network/NetworkConnection;Ljava/lang/String;)Ljava/lang/String;",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterValidatePlayerSlotJoin(@Coerce Object connection,
                                                             String playerName,
                                                             CallbackInfoReturnable<String> cir) {
        NetworkCallbackEvents.AFTER_VALIDATE_PLAYER_SLOT_JOIN.invoker()
                .onEvent(this, connection, playerName, cir.getReturnValue());
    }

    @Inject(method = "onPlayerRegistered(Lrustedwarfare/network/NetworkConnection;Ljava/lang/String;Ljava/lang/String;)V",
            at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeOnPlayerRegistered(@Coerce Object connection,
                                                          String playerName,
                                                          String playerIdText,
                                                          CallbackInfo ci) {
        NetworkCallbackEvents.BEFORE_ON_PLAYER_REGISTERED.invoker()
                .onEvent(this, connection, playerName, playerIdText);
    }

    @Inject(method = "onPlayerRegistered(Lrustedwarfare/network/NetworkConnection;Ljava/lang/String;Ljava/lang/String;)V",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterOnPlayerRegistered(@Coerce Object connection,
                                                         String playerName,
                                                         String playerIdText,
                                                         CallbackInfo ci) {
        NetworkCallbackEvents.AFTER_ON_PLAYER_REGISTERED.invoker()
                .onEvent(this, connection, playerName, playerIdText);
        ConnectionEvents.SERVER_PLAYER_REGISTERED.invoker().onRegistered(
                (rustedwarfare.network.NetworkConnection) connection, playerName, playerIdText);
    }

    @Inject(method = "onPlayerAdded(Lrustedwarfare/game/Team;)V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeOnPlayerAdded(@Coerce Object team, CallbackInfo ci) {
        NetworkCallbackEvents.BEFORE_ON_PLAYER_ADDED.invoker().onEvent(this, team);
    }

    @Inject(method = "onPlayerAdded(Lrustedwarfare/game/Team;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterOnPlayerAdded(@Coerce Object team, CallbackInfo ci) {
        NetworkCallbackEvents.AFTER_ON_PLAYER_ADDED.invoker().onEvent(this, team);
    }

    @Inject(method = "onAllPlayersReady()V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeOnAllPlayersReady(CallbackInfo ci) {
        NetworkCallbackEvents.BEFORE_ON_ALL_PLAYERS_READY.invoker().onEvent(this);
    }

    @Inject(method = "onAllPlayersReady()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterOnAllPlayersReady(CallbackInfo ci) {
        NetworkCallbackEvents.AFTER_ON_ALL_PLAYERS_READY.invoker().onEvent(this);
    }

    @Inject(method = "canGrantServerControl(Lrustedwarfare/network/NetworkConnection;)Z",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterCanGrantServerControl(@Coerce Object connection,
                                                            CallbackInfoReturnable<Boolean> cir) {
        NetworkCallbackEvents.AFTER_CAN_GRANT_SERVER_CONTROL.invoker()
                .onEvent(this, connection, Boolean.TRUE.equals(cir.getReturnValue()));
    }

    @Inject(method = "isProxyControllerConnection(Lrustedwarfare/network/NetworkConnection;)Z",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterIsProxyControllerConnection(@Coerce Object connection,
                                                                  CallbackInfoReturnable<Boolean> cir) {
        NetworkCallbackEvents.AFTER_IS_PROXY_CONTROLLER_CONNECTION.invoker()
                .onEvent(this, connection, Boolean.TRUE.equals(cir.getReturnValue()));
    }

    @Inject(method = "onBattleroomClosed()V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeOnBattleroomClosed(CallbackInfo ci) {
        NetworkCallbackEvents.BEFORE_ON_BATTLEROOM_CLOSED.invoker().onEvent(this);
    }

    @Inject(method = "onBattleroomClosed()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterOnBattleroomClosed(CallbackInfo ci) {
        NetworkCallbackEvents.AFTER_ON_BATTLEROOM_CLOSED.invoker().onEvent(this);
    }

    @Inject(method = "isGameStarting()Z", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterIsGameStarting(CallbackInfoReturnable<Boolean> cir) {
        NetworkCallbackEvents.AFTER_IS_GAME_STARTING.invoker()
                .onEvent(this, Boolean.TRUE.equals(cir.getReturnValue()));
    }
}
