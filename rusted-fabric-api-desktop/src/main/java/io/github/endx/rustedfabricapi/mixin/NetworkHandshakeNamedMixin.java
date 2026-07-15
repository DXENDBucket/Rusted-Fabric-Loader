package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.NetworkHandshakeEvents;
import io.github.endx.rustedfabricapi.desktop.DesktopMultiplayerTransport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "rustedwarfare.network.NetworkEngine", remap = false)
public abstract class NetworkHandshakeNamedMixin {
    @Inject(method = "sendPreRegisterInfoRequest(Lrustedwarfare/network/NetworkConnection;)V",
            at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeSendPreRegisterInfoRequest(@Coerce Object connection, CallbackInfo ci) {
        NetworkHandshakeEvents.BEFORE_SEND_PRE_REGISTER_INFO_REQUEST.invoker().onPacket(this, connection);
    }

    @Inject(method = "sendPreRegisterInfoRequest(Lrustedwarfare/network/NetworkConnection;)V",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterSendPreRegisterInfoRequest(@Coerce Object connection, CallbackInfo ci) {
        NetworkHandshakeEvents.AFTER_SEND_PRE_REGISTER_INFO_REQUEST.invoker().onPacket(this, connection);
    }

    @Inject(method = "sendPreRegisterInfo(Lrustedwarfare/network/NetworkConnection;)V",
            at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeSendPreRegisterInfo(@Coerce Object connection, CallbackInfo ci) {
        NetworkHandshakeEvents.BEFORE_SEND_PRE_REGISTER_INFO.invoker().onPacket(this, connection);
    }

    @Inject(method = "sendPreRegisterInfo(Lrustedwarfare/network/NetworkConnection;)V",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterSendPreRegisterInfo(@Coerce Object connection, CallbackInfo ci) {
        NetworkHandshakeEvents.AFTER_SEND_PRE_REGISTER_INFO.invoker().onPacket(this, connection);
    }

    @Inject(method = "sendRegisterConnection(Lrustedwarfare/network/NetworkConnection;)V",
            at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeSendRegisterConnection(@Coerce Object connection, CallbackInfo ci) {
        NetworkHandshakeEvents.BEFORE_SEND_REGISTER_CONNECTION.invoker().onPacket(this, connection);
    }

    @Inject(method = "sendRegisterConnection(Lrustedwarfare/network/NetworkConnection;)V",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterSendRegisterConnection(@Coerce Object connection, CallbackInfo ci) {
        NetworkHandshakeEvents.AFTER_SEND_REGISTER_CONNECTION.invoker().onPacket(this, connection);
        DesktopMultiplayerTransport.afterClientRegistration(this, connection);
    }

    @Inject(method = "sendServerInfo(Lrustedwarfare/network/NetworkConnection;)V",
            at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeSendServerInfo(@Coerce Object connection, CallbackInfo ci) {
        NetworkHandshakeEvents.BEFORE_SEND_SERVER_INFO.invoker().onPacket(this, connection);
    }

    @Inject(method = "sendServerInfo(Lrustedwarfare/network/NetworkConnection;)V",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterSendServerInfo(@Coerce Object connection, CallbackInfo ci) {
        NetworkHandshakeEvents.AFTER_SEND_SERVER_INFO.invoker().onPacket(this, connection);
        DesktopMultiplayerTransport.afterServerInfo(this, connection);
    }

    @Inject(method = "processSystemPacket(Lrustedwarfare/network/Packet;)V",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$receiveHandshake(@Coerce Object packet, CallbackInfo ci) {
        if (DesktopMultiplayerTransport.receive(this, packet)) ci.cancel();
    }

    @Inject(method = "sendStartGamePacket(Lrustedwarfare/network/NetworkConnection;Z)Z",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$gateGameStart(@Coerce Object connection, boolean reconnect,
            CallbackInfoReturnable<Boolean> callback) {
        if (!DesktopMultiplayerTransport.allowGameStart(connection)) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "sendIncorrectPassword(Lrustedwarfare/network/NetworkConnection;)V",
            at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeSendIncorrectPassword(@Coerce Object connection, CallbackInfo ci) {
        NetworkHandshakeEvents.BEFORE_SEND_INCORRECT_PASSWORD.invoker().onPacket(this, connection);
    }

    @Inject(method = "sendIncorrectPassword(Lrustedwarfare/network/NetworkConnection;)V",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterSendIncorrectPassword(@Coerce Object connection, CallbackInfo ci) {
        NetworkHandshakeEvents.AFTER_SEND_INCORRECT_PASSWORD.invoker().onPacket(this, connection);
    }

    @Inject(method = "sendKick(Lrustedwarfare/network/NetworkConnection;Ljava/lang/String;)V",
            at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeSendKick(@Coerce Object connection, String reason, CallbackInfo ci) {
        NetworkHandshakeEvents.BEFORE_SEND_KICK.invoker().onPacket(this, connection, reason);
    }

    @Inject(method = "sendKick(Lrustedwarfare/network/NetworkConnection;Ljava/lang/String;)V",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterSendKick(@Coerce Object connection, String reason, CallbackInfo ci) {
        NetworkHandshakeEvents.AFTER_SEND_KICK.invoker().onPacket(this, connection, reason);
    }

    @Inject(method = "sendUpdatePlayer(Lrustedwarfare/network/NetworkConnection;)V",
            at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeSendUpdatePlayer(@Coerce Object connection, CallbackInfo ci) {
        NetworkHandshakeEvents.BEFORE_SEND_UPDATE_PLAYER.invoker().onPacket(this, connection);
    }

    @Inject(method = "sendUpdatePlayer(Lrustedwarfare/network/NetworkConnection;)V",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterSendUpdatePlayer(@Coerce Object connection, CallbackInfo ci) {
        NetworkHandshakeEvents.AFTER_SEND_UPDATE_PLAYER.invoker().onPacket(this, connection);
    }
}
