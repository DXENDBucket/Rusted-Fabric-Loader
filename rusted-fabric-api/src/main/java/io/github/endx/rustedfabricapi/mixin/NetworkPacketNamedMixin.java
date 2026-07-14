package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.NetworkPacketEvents;
import io.github.endx.rustedfabricapi.api.multiplayer.DesktopMultiplayerTransport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "rustedwarfare.network.NetworkEngine", remap = false)
public abstract class NetworkPacketNamedMixin {
    @Inject(method = "resetNetworkState(Z)V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeResetNetworkState(boolean chatOnly, CallbackInfo ci) {
        NetworkPacketEvents.BEFORE_RESET_NETWORK_STATE.invoker().onEvent(this, chatOnly);
    }

    @Inject(method = "resetNetworkState(Z)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterResetNetworkState(boolean chatOnly, CallbackInfo ci) {
        NetworkPacketEvents.AFTER_RESET_NETWORK_STATE.invoker().onEvent(this, chatOnly);
        if (!chatOnly) DesktopMultiplayerTransport.resetToSinglePlayer();
    }

    @Inject(method = "disconnectWithReason(Ljava/lang/String;)V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeDisconnectWithReason(String reason, CallbackInfo ci) {
        NetworkPacketEvents.BEFORE_DISCONNECT_WITH_REASON.invoker().onEvent(this, reason);
    }

    @Inject(method = "disconnectWithReason(Ljava/lang/String;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterDisconnectWithReason(String reason, CallbackInfo ci) {
        NetworkPacketEvents.AFTER_DISCONNECT_WITH_REASON.invoker().onEvent(this, reason);
    }

    @Inject(method = "areAllClientsReady(ZI)Z", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeAreAllClientsReady(boolean includeSpectators, int minimumPlayerCount,
                                                          CallbackInfoReturnable<Boolean> cir) {
        NetworkPacketEvents.BEFORE_ARE_ALL_CLIENTS_READY.invoker()
                .onEvent(this, includeSpectators, minimumPlayerCount);
    }

    @Inject(method = "areAllClientsReady(ZI)Z", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterAreAllClientsReady(boolean includeSpectators, int minimumPlayerCount,
                                                         CallbackInfoReturnable<Boolean> cir) {
        NetworkPacketEvents.AFTER_ARE_ALL_CLIENTS_READY.invoker()
                .onEvent(this, includeSpectators, minimumPlayerCount, Boolean.TRUE.equals(cir.getReturnValue()));
    }

    @Inject(method = "resetClientReadyFlags()V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeResetClientReadyFlags(CallbackInfo ci) {
        NetworkPacketEvents.BEFORE_RESET_CLIENT_READY_FLAGS.invoker().onEvent(this);
    }

    @Inject(method = "resetClientReadyFlags()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterResetClientReadyFlags(CallbackInfo ci) {
        NetworkPacketEvents.AFTER_RESET_CLIENT_READY_FLAGS.invoker().onEvent(this);
    }

    @Inject(method = "sendPacketToValidatedConnections(Lrustedwarfare/network/Packet;)V",
            at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeSendPacketToValidatedConnections(@Coerce Object packet, CallbackInfo ci) {
        NetworkPacketEvents.BEFORE_SEND_PACKET_TO_VALIDATED_CONNECTIONS.invoker().onEvent(this, packet);
    }

    @Inject(method = "sendPacketToValidatedConnections(Lrustedwarfare/network/Packet;)V",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterSendPacketToValidatedConnections(@Coerce Object packet, CallbackInfo ci) {
        NetworkPacketEvents.AFTER_SEND_PACKET_TO_VALIDATED_CONNECTIONS.invoker().onEvent(this, packet);
    }

    @Inject(method = "sendPacketToAllIncludingRelay(Lrustedwarfare/network/Packet;)V",
            at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeSendPacketToAllIncludingRelay(@Coerce Object packet, CallbackInfo ci) {
        NetworkPacketEvents.BEFORE_SEND_PACKET_TO_ALL_INCLUDING_RELAY.invoker().onEvent(this, packet);
    }

    @Inject(method = "sendPacketToAllIncludingRelay(Lrustedwarfare/network/Packet;)V",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterSendPacketToAllIncludingRelay(@Coerce Object packet, CallbackInfo ci) {
        NetworkPacketEvents.AFTER_SEND_PACKET_TO_ALL_INCLUDING_RELAY.invoker().onEvent(this, packet);
    }

    @Inject(method = "sendPacketToServer(Lrustedwarfare/network/Packet;)V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeSendPacketToServer(@Coerce Object packet, CallbackInfo ci) {
        NetworkPacketEvents.BEFORE_SEND_PACKET_TO_SERVER.invoker().onEvent(this, packet);
    }

    @Inject(method = "sendPacketToServer(Lrustedwarfare/network/Packet;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterSendPacketToServer(@Coerce Object packet, CallbackInfo ci) {
        NetworkPacketEvents.AFTER_SEND_PACKET_TO_SERVER.invoker().onEvent(this, packet);
    }

    @Inject(method = "sendPacketToClientsIncludingRelay(Lrustedwarfare/network/Packet;)V",
            at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeSendPacketToClientsIncludingRelay(@Coerce Object packet, CallbackInfo ci) {
        NetworkPacketEvents.BEFORE_SEND_PACKET_TO_CLIENTS_INCLUDING_RELAY.invoker().onEvent(this, packet);
    }

    @Inject(method = "sendPacketToClientsIncludingRelay(Lrustedwarfare/network/Packet;)V",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterSendPacketToClientsIncludingRelay(@Coerce Object packet, CallbackInfo ci) {
        NetworkPacketEvents.AFTER_SEND_PACKET_TO_CLIENTS_INCLUDING_RELAY.invoker().onEvent(this, packet);
    }

    @Inject(method = "sendPacketToClients(Lrustedwarfare/network/Packet;)V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeSendPacketToClients(@Coerce Object packet, CallbackInfo ci) {
        NetworkPacketEvents.BEFORE_SEND_PACKET_TO_CLIENTS.invoker().onEvent(this, packet);
    }

    @Inject(method = "sendPacketToClients(Lrustedwarfare/network/Packet;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterSendPacketToClients(@Coerce Object packet, CallbackInfo ci) {
        NetworkPacketEvents.AFTER_SEND_PACKET_TO_CLIENTS.invoker().onEvent(this, packet);
    }

    @Inject(method = "sendRegisterConnectionToAll()V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeSendRegisterConnectionToAll(CallbackInfo ci) {
        NetworkPacketEvents.BEFORE_SEND_REGISTER_CONNECTION_TO_ALL.invoker().onEvent(this);
    }

    @Inject(method = "sendRegisterConnectionToAll()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterSendRegisterConnectionToAll(CallbackInfo ci) {
        NetworkPacketEvents.AFTER_SEND_REGISTER_CONNECTION_TO_ALL.invoker().onEvent(this);
    }

    @Inject(method = "resetNetworkClientId()V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeResetNetworkClientId(CallbackInfo ci) {
        NetworkPacketEvents.BEFORE_RESET_NETWORK_CLIENT_ID.invoker().onEvent(this);
    }

    @Inject(method = "resetNetworkClientId()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterResetNetworkClientId(CallbackInfo ci) {
        NetworkPacketEvents.AFTER_RESET_NETWORK_CLIENT_ID.invoker().onEvent(this);
    }

    @Inject(method = "generateNewServerId()V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeGenerateNewServerId(CallbackInfo ci) {
        NetworkPacketEvents.BEFORE_GENERATE_NEW_SERVER_ID.invoker().onEvent(this);
    }

    @Inject(method = "generateNewServerId()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterGenerateNewServerId(CallbackInfo ci) {
        NetworkPacketEvents.AFTER_GENERATE_NEW_SERVER_ID.invoker().onEvent(this);
    }

    @Inject(method = "updateSharedControlDueToDisconnects()V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeUpdateSharedControlDueToDisconnects(CallbackInfo ci) {
        NetworkPacketEvents.BEFORE_UPDATE_SHARED_CONTROL_DUE_TO_DISCONNECTS.invoker().onEvent(this);
    }

    @Inject(method = "updateSharedControlDueToDisconnects()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterUpdateSharedControlDueToDisconnects(CallbackInfo ci) {
        NetworkPacketEvents.AFTER_UPDATE_SHARED_CONTROL_DUE_TO_DISCONNECTS.invoker().onEvent(this);
    }

    @Inject(method = "setLocalPlayerName(Ljava/lang/String;)Ljava/lang/String;", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeSetLocalPlayerName(String playerName,
                                                          CallbackInfoReturnable<String> cir) {
        NetworkPacketEvents.BEFORE_SET_LOCAL_PLAYER_NAME.invoker().onEvent(this, playerName);
    }

    @Inject(method = "setLocalPlayerName(Ljava/lang/String;)Ljava/lang/String;", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterSetLocalPlayerName(String playerName,
                                                         CallbackInfoReturnable<String> cir) {
        NetworkPacketEvents.AFTER_SET_LOCAL_PLAYER_NAME.invoker()
                .onEvent(this, playerName, cir.getReturnValue());
    }

    @Inject(method = "updateAiDifficulty()V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeUpdateAiDifficulty(CallbackInfo ci) {
        NetworkPacketEvents.BEFORE_UPDATE_AI_DIFFICULTY.invoker().onEvent(this);
    }

    @Inject(method = "updateAiDifficulty()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterUpdateAiDifficulty(CallbackInfo ci) {
        NetworkPacketEvents.AFTER_UPDATE_AI_DIFFICULTY.invoker().onEvent(this);
    }

    @Inject(method = "applyAiDifficultyForTeam(Lrustedwarfare/game/Team;)V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeApplyAiDifficultyForTeam(@Coerce Object team, CallbackInfo ci) {
        NetworkPacketEvents.BEFORE_APPLY_AI_DIFFICULTY_FOR_TEAM.invoker().onEvent(this, team);
    }

    @Inject(method = "applyAiDifficultyForTeam(Lrustedwarfare/game/Team;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterApplyAiDifficultyForTeam(@Coerce Object team, CallbackInfo ci) {
        NetworkPacketEvents.AFTER_APPLY_AI_DIFFICULTY_FOR_TEAM.invoker().onEvent(this, team);
    }

    @Inject(method = "updateAiTeamName(Lrustedwarfare/game/Team;)Z", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeUpdateAiTeamName(@Coerce Object team, CallbackInfoReturnable<Boolean> cir) {
        NetworkPacketEvents.BEFORE_UPDATE_AI_TEAM_NAME.invoker().onEvent(this, team);
    }

    @Inject(method = "updateAiTeamName(Lrustedwarfare/game/Team;)Z", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterUpdateAiTeamName(@Coerce Object team, CallbackInfoReturnable<Boolean> cir) {
        NetworkPacketEvents.AFTER_UPDATE_AI_TEAM_NAME.invoker()
                .onEvent(this, team, Boolean.TRUE.equals(cir.getReturnValue()));
    }

    @Inject(method = "applyProxyControlSetup(Lrustedwarfare/network/GameSetup;)V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeApplyProxyControlSetup(@Coerce Object gameSetup, CallbackInfo ci) {
        NetworkPacketEvents.BEFORE_APPLY_PROXY_CONTROL_SETUP.invoker().onEvent(this, gameSetup);
    }

    @Inject(method = "applyProxyControlSetup(Lrustedwarfare/network/GameSetup;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterApplyProxyControlSetup(@Coerce Object gameSetup, CallbackInfo ci) {
        NetworkPacketEvents.AFTER_APPLY_PROXY_CONTROL_SETUP.invoker().onEvent(this, gameSetup);
    }
}
