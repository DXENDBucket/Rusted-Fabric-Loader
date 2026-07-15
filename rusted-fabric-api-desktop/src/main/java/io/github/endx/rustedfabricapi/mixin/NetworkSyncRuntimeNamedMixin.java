package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.SaveSyncEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.lang.reflect.Field;

@Mixin(targets = "rustedwarfare.network.NetworkEngine", remap = false)
public abstract class NetworkSyncRuntimeNamedMixin {
    @Inject(method = "sendResyncSave(ZZZ)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeNetworkResyncSave(boolean optionA, boolean optionB, boolean reloadCreatedSave, CallbackInfo ci) {
        if (SaveSyncEvents.BEFORE_NETWORK_RESYNC_SAVE.invoker()
                .beforeNetworkResyncSave(this, null, null, optionA, optionB, reloadCreatedSave, "sendResyncSave")) {
            ci.cancel();
        }
    }

    @Inject(
            method = "sendResyncSave(ZZZ)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lrustedwarfare/network/NetworkEngine;sendPacketToAll(Lrustedwarfare/network/Packet;)V"
            ),
            require = 1,
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void rustedfabricapi$afterNetworkResyncPacketCreated(boolean optionA, boolean optionB, boolean reloadCreatedSave, CallbackInfo ci, @Coerce Object gameEngine, @Coerce Object outputStream, @Coerce Object packet) {
        SaveSyncEvents.AFTER_NETWORK_RESYNC_PACKET_CREATED.invoker()
                .afterNetworkResyncPacketCreated(this, null, packet, null, optionA, optionB, reloadCreatedSave, "sendResyncSave");
    }

    @Inject(method = "sendResyncSaveBytesToConnection(Lrustedwarfare/network/NetworkConnection;[BZZ)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeNetworkResyncSaveBytes(@Coerce Object connection, byte[] saveBytes, boolean optionA, boolean optionB, CallbackInfo ci) {
        if (SaveSyncEvents.BEFORE_NETWORK_RESYNC_SAVE.invoker()
                .beforeNetworkResyncSave(this, connection, saveBytes, optionA, optionB, false, "sendResyncSaveBytesToConnection")) {
            ci.cancel();
        }
    }

    @Inject(
            method = "sendResyncSaveBytesToConnection(Lrustedwarfare/network/NetworkConnection;[BZZ)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lrustedwarfare/network/NetworkEngine;sendPacketToConnection(Lrustedwarfare/network/NetworkConnection;Lrustedwarfare/network/Packet;)V"
            ),
            require = 1,
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void rustedfabricapi$afterNetworkResyncBytesPacketCreated(@Coerce Object connection, byte[] saveBytes, boolean optionA, boolean optionB, CallbackInfo ci, @Coerce Object gameEngine, @Coerce Object outputStream, @Coerce Object packet) {
        SaveSyncEvents.AFTER_NETWORK_RESYNC_PACKET_CREATED.invoker()
                .afterNetworkResyncPacketCreated(this, connection, packet, saveBytes, optionA, optionB, false, "sendResyncSaveBytesToConnection");
    }

    @Inject(
            method = "sendSyncChecksumPackets(F)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lrustedwarfare/network/NetworkEngine;h(Lrustedwarfare/network/Packet;)V"
            ),
            cancellable = true,
            require = 1,
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void rustedfabricapi$beforeChecksumSend(float delta, CallbackInfo ci, @Coerce Object gameEngine, @Coerce Object outputStream, @Coerce Object packet) {
        if (SaveSyncEvents.BEFORE_CHECKSUM_SEND.invoker().beforeChecksumSend(this, packet, rustedfabricapi$getNetworkChecksum(), delta)) {
            ci.cancel();
        }
    }

    private Object rustedfabricapi$getNetworkChecksum() {
        try {
            Field field = this.getClass().getDeclaredField("networkChecksum");
            field.setAccessible(true);
            return field.get(this);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
