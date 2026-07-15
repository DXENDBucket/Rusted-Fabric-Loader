package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.diagnostic.NetworkRuntimeDiagnostics;
import io.github.endx.rustedfabricapi.api.event.NetworkLobbyChatEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "rustedwarfare.network.NetworkEngine", remap = false)
public abstract class NetworkLobbyChatNamedMixin {
    @Inject(method = "sendSystemMessage(Ljava/lang/String;)V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeSendSystemMessage(String text, CallbackInfo ci) {
        NetworkLobbyChatEvents.BEFORE_SEND_SYSTEM_MESSAGE.invoker().onEvent(this, text);
    }

    @Inject(method = "sendSystemMessage(Ljava/lang/String;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterSendSystemMessage(String text, CallbackInfo ci) {
        NetworkLobbyChatEvents.AFTER_SEND_SYSTEM_MESSAGE.invoker().onEvent(this, text);
    }

    @Inject(method = "sendQuickChatCommand(Ljava/lang/String;)V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeSendQuickChatCommand(String text, CallbackInfo ci) {
        NetworkLobbyChatEvents.BEFORE_SEND_QUICK_CHAT_COMMAND.invoker().onEvent(this, text);
    }

    @Inject(method = "sendQuickChatCommand(Ljava/lang/String;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterSendQuickChatCommand(String text, CallbackInfo ci) {
        NetworkLobbyChatEvents.AFTER_SEND_QUICK_CHAT_COMMAND.invoker().onEvent(this, text);
    }

    @Inject(method = "sendTeamChatMessage(Ljava/lang/String;)V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeSendTeamChatMessage(String text, CallbackInfo ci) {
        NetworkLobbyChatEvents.BEFORE_SEND_TEAM_CHAT_MESSAGE.invoker().onEvent(this, text);
    }

    @Inject(method = "sendTeamChatMessage(Ljava/lang/String;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterSendTeamChatMessage(String text, CallbackInfo ci) {
        NetworkLobbyChatEvents.AFTER_SEND_TEAM_CHAT_MESSAGE.invoker().onEvent(this, text);
    }

    @Inject(method = "sendChatMessage(Ljava/lang/String;)V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeSendChatMessage(String text, CallbackInfo ci) {
        NetworkLobbyChatEvents.BEFORE_SEND_CHAT_MESSAGE.invoker().onEvent(this, text);
    }

    @Inject(method = "sendChatMessage(Ljava/lang/String;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterSendChatMessage(String text, CallbackInfo ci) {
        NetworkLobbyChatEvents.AFTER_SEND_CHAT_MESSAGE.invoker().onEvent(this, text);
    }

    @Inject(
            method = "sendChatMessageFromServer(Lrustedwarfare/network/NetworkConnection;Lrustedwarfare/game/Team;Ljava/lang/String;Ljava/lang/String;)V",
            at = @At("HEAD"),
            require = 1
    )
    private void rustedfabricapi$beforeSendChatMessageFromServer(@Coerce Object sourceConnection,
                                                                 @Coerce Object team,
                                                                 String senderName,
                                                                 String message,
                                                                 CallbackInfo ci) {
        NetworkLobbyChatEvents.BEFORE_SEND_SERVER_CHAT_MESSAGE.invoker()
                .onEvent(this, sourceConnection, team, senderName, message, null);
    }

    @Inject(
            method = "sendChatMessageFromServer(Lrustedwarfare/network/NetworkConnection;Lrustedwarfare/game/Team;Ljava/lang/String;Ljava/lang/String;)V",
            at = @At("RETURN"),
            require = 1
    )
    private void rustedfabricapi$afterSendChatMessageFromServer(@Coerce Object sourceConnection,
                                                                @Coerce Object team,
                                                                String senderName,
                                                                String message,
                                                                CallbackInfo ci) {
        NetworkLobbyChatEvents.AFTER_SEND_SERVER_CHAT_MESSAGE.invoker()
                .onEvent(this, sourceConnection, team, senderName, message, null);
    }

    @Inject(
            method = "sendChatMessageFromServerToTarget(Lrustedwarfare/network/NetworkConnection;Lrustedwarfare/game/Team;Ljava/lang/String;Ljava/lang/String;Lrustedwarfare/network/NetworkConnection;)V",
            at = @At("HEAD"),
            require = 1
    )
    private void rustedfabricapi$beforeSendChatMessageFromServerToTarget(@Coerce Object sourceConnection,
                                                                         @Coerce Object team,
                                                                         String senderName,
                                                                         String message,
                                                                         @Coerce Object targetConnection,
                                                                         CallbackInfo ci) {
        NetworkLobbyChatEvents.BEFORE_SEND_SERVER_CHAT_MESSAGE.invoker()
                .onEvent(this, sourceConnection, team, senderName, message, targetConnection);
    }

    @Inject(
            method = "sendChatMessageFromServerToTarget(Lrustedwarfare/network/NetworkConnection;Lrustedwarfare/game/Team;Ljava/lang/String;Ljava/lang/String;Lrustedwarfare/network/NetworkConnection;)V",
            at = @At("RETURN"),
            require = 1
    )
    private void rustedfabricapi$afterSendChatMessageFromServerToTarget(@Coerce Object sourceConnection,
                                                                        @Coerce Object team,
                                                                        String senderName,
                                                                        String message,
                                                                        @Coerce Object targetConnection,
                                                                        CallbackInfo ci) {
        NetworkLobbyChatEvents.AFTER_SEND_SERVER_CHAT_MESSAGE.invoker()
                .onEvent(this, sourceConnection, team, senderName, message, targetConnection);
    }

    @Inject(
            method = "recordReceivedChatMessage(Lrustedwarfare/network/NetworkConnection;ILjava/lang/String;Ljava/lang/String;)V",
            at = @At("HEAD"),
            require = 1
    )
    private void rustedfabricapi$beforeRecordReceivedChatMessage(@Coerce Object connection,
                                                                 int teamId,
                                                                 String senderName,
                                                                 String message,
                                                                 CallbackInfo ci) {
        NetworkLobbyChatEvents.BEFORE_RECORD_RECEIVED_CHAT_MESSAGE.invoker()
                .onEvent(this, connection, teamId, senderName, message);
    }

    @Inject(
            method = "recordReceivedChatMessage(Lrustedwarfare/network/NetworkConnection;ILjava/lang/String;Ljava/lang/String;)V",
            at = @At("RETURN"),
            require = 1
    )
    private void rustedfabricapi$afterRecordReceivedChatMessage(@Coerce Object connection,
                                                                int teamId,
                                                                String senderName,
                                                                String message,
                                                                CallbackInfo ci) {
        NetworkLobbyChatEvents.AFTER_RECORD_RECEIVED_CHAT_MESSAGE.invoker()
                .onEvent(this, connection, teamId, senderName, message);
    }

    @Inject(method = "sendCommandError(Ljava/lang/String;Lrustedwarfare/network/NetworkConnection;)V",
            at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeSendCommandError(String message,
                                                        @Coerce Object targetConnection,
                                                        CallbackInfo ci) {
        NetworkLobbyChatEvents.BEFORE_SEND_COMMAND_ERROR.invoker().onEvent(this, message, targetConnection);
    }

    @Inject(method = "sendCommandError(Ljava/lang/String;Lrustedwarfare/network/NetworkConnection;)V",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterSendCommandError(String message,
                                                       @Coerce Object targetConnection,
                                                       CallbackInfo ci) {
        NetworkLobbyChatEvents.AFTER_SEND_COMMAND_ERROR.invoker().onEvent(this, message, targetConnection);
    }

    @Inject(
            method = "handleChatCommand(Lrustedwarfare/network/NetworkConnection;Lrustedwarfare/game/Team;Ljava/lang/String;Ljava/lang/String;)Z",
            at = @At("HEAD"),
            require = 1
    )
    private void rustedfabricapi$beforeHandleChatCommand(@Coerce Object connection,
                                                         @Coerce Object team,
                                                         String senderName,
                                                         String message,
                                                         CallbackInfoReturnable<Boolean> cir) {
        NetworkLobbyChatEvents.BEFORE_HANDLE_CHAT_COMMAND.invoker()
                .onEvent(this, connection, team, senderName, message, rustedfabricapi$commandName(message));
    }

    @Inject(
            method = "handleChatCommand(Lrustedwarfare/network/NetworkConnection;Lrustedwarfare/game/Team;Ljava/lang/String;Ljava/lang/String;)Z",
            at = @At("RETURN"),
            require = 1
    )
    private void rustedfabricapi$afterHandleChatCommand(@Coerce Object connection,
                                                        @Coerce Object team,
                                                        String senderName,
                                                        String message,
                                                        CallbackInfoReturnable<Boolean> cir) {
        NetworkLobbyChatEvents.AFTER_HANDLE_CHAT_COMMAND.invoker()
                .onEvent(this, connection, team, senderName, message,
                        rustedfabricapi$commandName(message), Boolean.TRUE.equals(cir.getReturnValue()));
    }

    @Inject(method = "setGamePaused(Z)V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeSetGamePaused(boolean paused, CallbackInfo ci) {
        NetworkLobbyChatEvents.BEFORE_SET_GAME_PAUSED.invoker().onEvent(this, paused);
    }

    @Inject(method = "setGamePaused(Z)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterSetGamePaused(boolean paused, CallbackInfo ci) {
        NetworkLobbyChatEvents.AFTER_SET_GAME_PAUSED.invoker().onEvent(this, paused);
    }

    @Inject(method = "requestKickTeamAndPlayer(Lrustedwarfare/game/Team;)V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeRequestKickTeamAndPlayer(@Coerce Object team, CallbackInfo ci) {
        NetworkLobbyChatEvents.BEFORE_REQUEST_KICK_TEAM_AND_PLAYER.invoker().onEvent(this, team);
    }

    @Inject(method = "requestKickTeamAndPlayer(Lrustedwarfare/game/Team;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterRequestKickTeamAndPlayer(@Coerce Object team, CallbackInfo ci) {
        NetworkLobbyChatEvents.AFTER_REQUEST_KICK_TEAM_AND_PLAYER.invoker().onEvent(this, team);
    }

    @Inject(method = "kickTeamAndAttachedPlayer(Lrustedwarfare/game/Team;)V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeKickTeamAndAttachedPlayer(@Coerce Object team, CallbackInfo ci) {
        NetworkLobbyChatEvents.BEFORE_KICK_TEAM_AND_ATTACHED_PLAYER.invoker().onEvent(this, team);
    }

    @Inject(method = "kickTeamAndAttachedPlayer(Lrustedwarfare/game/Team;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterKickTeamAndAttachedPlayer(@Coerce Object team, CallbackInfo ci) {
        NetworkLobbyChatEvents.AFTER_KICK_TEAM_AND_ATTACHED_PLAYER.invoker().onEvent(this, team);
    }

    @Inject(method = "addAIToGame()V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeAddAIToGame(CallbackInfo ci) {
        NetworkLobbyChatEvents.BEFORE_ADD_AI_TO_GAME.invoker().onEvent(this);
    }

    @Inject(method = "addAIToGame()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterAddAIToGame(CallbackInfo ci) {
        NetworkLobbyChatEvents.AFTER_ADD_AI_TO_GAME.invoker().onEvent(this);
    }

    @Inject(method = "updateAiNames()Z", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterUpdateAiNames(CallbackInfoReturnable<Boolean> cir) {
        NetworkLobbyChatEvents.AFTER_UPDATE_AI_NAMES.invoker().onEvent(this, Boolean.TRUE.equals(cir.getReturnValue()));
    }

    @Inject(method = "movePlayerToSlot(Lrustedwarfare/game/Team;I)V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeMovePlayerToSlot(@Coerce Object team, int slot, CallbackInfo ci) {
        NetworkLobbyChatEvents.BEFORE_MOVE_PLAYER_TO_SLOT.invoker().onEvent(this, team, slot);
    }

    @Inject(method = "movePlayerToSlot(Lrustedwarfare/game/Team;I)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterMovePlayerToSlot(@Coerce Object team, int slot, CallbackInfo ci) {
        NetworkLobbyChatEvents.AFTER_MOVE_PLAYER_TO_SLOT.invoker().onEvent(this, team, slot);
    }

    @Inject(method = "applyMovePlayerToSlot(Lrustedwarfare/game/Team;I)V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeApplyMovePlayerToSlot(@Coerce Object team, int slot, CallbackInfo ci) {
        NetworkLobbyChatEvents.BEFORE_APPLY_MOVE_PLAYER_TO_SLOT.invoker().onEvent(this, team, slot);
    }

    @Inject(method = "applyMovePlayerToSlot(Lrustedwarfare/game/Team;I)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterApplyMovePlayerToSlot(@Coerce Object team, int slot, CallbackInfo ci) {
        NetworkLobbyChatEvents.AFTER_APPLY_MOVE_PLAYER_TO_SLOT.invoker().onEvent(this, team, slot);
    }

    @Inject(method = "requestMovePlayerSlot(Lrustedwarfare/game/Team;ILjava/lang/Integer;)V",
            at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeRequestMovePlayerSlot(@Coerce Object team, int slot,
                                                             Integer optionalValue, CallbackInfo ci) {
        NetworkLobbyChatEvents.BEFORE_REQUEST_MOVE_PLAYER_SLOT.invoker()
                .onEvent(this, team, slot, optionalValue);
    }

    @Inject(method = "requestMovePlayerSlot(Lrustedwarfare/game/Team;ILjava/lang/Integer;)V",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterRequestMovePlayerSlot(@Coerce Object team, int slot,
                                                            Integer optionalValue, CallbackInfo ci) {
        NetworkLobbyChatEvents.AFTER_REQUEST_MOVE_PLAYER_SLOT.invoker()
                .onEvent(this, team, slot, optionalValue);
    }

    @Inject(method = "requestSetAllyTeam(Lrustedwarfare/game/Team;I)V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeRequestSetAllyTeam(@Coerce Object team, int allyTeam, CallbackInfo ci) {
        NetworkLobbyChatEvents.BEFORE_REQUEST_SET_ALLY_TEAM.invoker().onEvent(this, team, allyTeam);
    }

    @Inject(method = "requestSetAllyTeam(Lrustedwarfare/game/Team;I)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterRequestSetAllyTeam(@Coerce Object team, int allyTeam, CallbackInfo ci) {
        NetworkLobbyChatEvents.AFTER_REQUEST_SET_ALLY_TEAM.invoker().onEvent(this, team, allyTeam);
    }

    @Inject(method = "announcePlayerVictory(Lrustedwarfare/game/Team;)V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeAnnouncePlayerVictory(@Coerce Object team, CallbackInfo ci) {
        NetworkLobbyChatEvents.BEFORE_ANNOUNCE_PLAYER_VICTORY.invoker().onEvent(this, team);
    }

    @Inject(method = "announcePlayerVictory(Lrustedwarfare/game/Team;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterAnnouncePlayerVictory(@Coerce Object team, CallbackInfo ci) {
        NetworkLobbyChatEvents.AFTER_ANNOUNCE_PLAYER_VICTORY.invoker().onEvent(this, team);
    }

    @Inject(method = "announcePlayerDefeated(Lrustedwarfare/game/Team;)V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeAnnouncePlayerDefeated(@Coerce Object team, CallbackInfo ci) {
        NetworkLobbyChatEvents.BEFORE_ANNOUNCE_PLAYER_DEFEATED.invoker().onEvent(this, team);
    }

    @Inject(method = "announcePlayerDefeated(Lrustedwarfare/game/Team;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterAnnouncePlayerDefeated(@Coerce Object team, CallbackInfo ci) {
        NetworkLobbyChatEvents.AFTER_ANNOUNCE_PLAYER_DEFEATED.invoker().onEvent(this, team);
    }

    @Inject(method = "announcePlayerWipedOut(Lrustedwarfare/game/Team;)V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeAnnouncePlayerWipedOut(@Coerce Object team, CallbackInfo ci) {
        NetworkLobbyChatEvents.BEFORE_ANNOUNCE_PLAYER_WIPED_OUT.invoker().onEvent(this, team);
    }

    @Inject(method = "announcePlayerWipedOut(Lrustedwarfare/game/Team;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterAnnouncePlayerWipedOut(@Coerce Object team, CallbackInfo ci) {
        NetworkLobbyChatEvents.AFTER_ANNOUNCE_PLAYER_WIPED_OUT.invoker().onEvent(this, team);
    }

    @Inject(method = "applyTeamLayoutLocked(Lrustedwarfare/network/TeamLayout;)V",
            at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeApplyTeamLayoutLocked(@Coerce Object teamLayout, CallbackInfo ci) {
        NetworkLobbyChatEvents.BEFORE_APPLY_TEAM_LAYOUT_LOCKED.invoker().onEvent(this, teamLayout);
    }

    @Inject(method = "applyTeamLayoutLocked(Lrustedwarfare/network/TeamLayout;)V",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterApplyTeamLayoutLocked(@Coerce Object teamLayout, CallbackInfo ci) {
        NetworkLobbyChatEvents.AFTER_APPLY_TEAM_LAYOUT_LOCKED.invoker().onEvent(this, teamLayout);
    }

    @Inject(method = "applyTeamLayout(Lrustedwarfare/network/TeamLayout;)V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeApplyTeamLayout(@Coerce Object teamLayout, CallbackInfo ci) {
        NetworkLobbyChatEvents.BEFORE_APPLY_TEAM_LAYOUT.invoker().onEvent(this, teamLayout);
    }

    @Inject(method = "applyTeamLayout(Lrustedwarfare/network/TeamLayout;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterApplyTeamLayout(@Coerce Object teamLayout, CallbackInfo ci) {
        NetworkLobbyChatEvents.AFTER_APPLY_TEAM_LAYOUT.invoker().onEvent(this, teamLayout);
    }

    private String rustedfabricapi$commandName(String message) {
        try {
            return NetworkRuntimeDiagnostics.extractChatCommandName(this, message);
        } catch (RuntimeException e) {
            return "";
        }
    }
}
