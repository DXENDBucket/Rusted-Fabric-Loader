package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.NetworkHandshakeEvents;
import io.github.endx.rustedfabricapi.api.networking.NamedChannelTransport;
import io.github.endx.rustedfabricapi.api.networking.event.ConnectionEvents;
import io.github.endx.rustedfabricapi.desktop.DesktopMultiplayerTransport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "rustedwarfare.network.NetworkEngine", remap = false)
public abstract class NetworkHandshakeNamedMixin {
    /** Native SERVER_INFO. Relay control packets occupy a separate 160+ range. */
    @Unique private static final int RUSTEDFABRICAPI_SERVER_INFO_PACKET = 106;

    @Shadow private boolean isServer;

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
        if (DesktopMultiplayerTransport.receive(this, packet)) {
            ci.cancel();
            return;
        }
        if (NamedChannelTransport.receive(
                (rustedwarfare.network.NetworkEngine) (Object) this,
                (rustedwarfare.network.Packet) packet)) {
            ci.cancel();
        }
    }

    @Inject(method = "processSystemPacket(Lrustedwarfare/network/Packet;)V",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterClientSystemPacket(@Coerce Object packet, CallbackInfo ci) {
        if (isServer || packet == null) return;
        rustedwarfare.network.Packet nativePacket = (rustedwarfare.network.Packet) packet;
        // Registration and Relay-version packets can mark the physical Relay socket usable
        // before it has redirected or attached a real peer. SERVER_INFO is the first native
        // packet that proves the remote endpoint is an actual game server.
        if (nativePacket.type != RUSTEDFABRICAPI_SERVER_INFO_PACKET) return;
        rustedwarfare.network.NetworkConnection connection = nativePacket.connection;
        if (connection == null || !connection.validated) return;
        if (DesktopMultiplayerTransport.clientConnectionReady(this, connection)) {
            ConnectionEvents.CLIENT_CONNECTION_READY.invoker().onReady(
                    (rustedwarfare.network.NetworkEngine) (Object) this, connection);
        }
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
