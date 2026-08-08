package io.github.endx.rustedfabricapi.api.networking;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import io.github.endx.rustedfabricapi.api.multiplayer.MultiplayerManifest;
import io.github.endx.rustedfabricapi.desktop.DesktopMultiplayerTransport;
import rustedwarfare.network.NetworkConnection;
import rustedwarfare.network.NetworkEngine;

import java.util.Collections;
import java.util.Set;
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/** Fabric-style named networking channels received and sent by a multiplayer host. */
public final class ServerNetworking {
    private static final ConcurrentHashMap<ChannelId, Receiver> RECEIVERS =
            new ConcurrentHashMap<ChannelId, Receiver>();

    private ServerNetworking() {
    }

    /** Registers the sole host receiver for a channel until the returned handle is closed. */
    public static RustedFabricEvent.Registration registerGlobalReceiver(
            ChannelId channel, Receiver receiver) {
        if (channel == null) throw new NullPointerException("channel");
        if (receiver == null) throw new NullPointerException("receiver");
        if (RECEIVERS.putIfAbsent(channel, receiver) != null) {
            throw new IllegalStateException("Server receiver already registered for " + channel);
        }
        AtomicBoolean active = new AtomicBoolean(true);
        return new RustedFabricEvent.Registration() {
            @Override
            public boolean unregister() {
                return active.compareAndSet(true, false) && RECEIVERS.remove(channel, receiver);
            }

            @Override
            public void close() {
                unregister();
            }
        };
    }

    /** Registers a receiver that only sees fully decoded, trailing-byte-free values. */
    public static <T> RustedFabricEvent.Registration registerGlobalReceiver(
            ChannelId channel, PacketCodec<T> codec, TypedReceiver<T> receiver) {
        if (codec == null) throw new NullPointerException("codec");
        if (receiver == null) throw new NullPointerException("receiver");
        return registerGlobalReceiver(channel, (engine, connection, receivedChannel, payload) ->
                receiver.receive(engine, connection, receivedChannel, codec.decodePayload(payload)));
    }

    public static boolean unregisterGlobalReceiver(ChannelId channel) {
        return channel != null && RECEIVERS.remove(channel) != null;
    }

    public static Set<ChannelId> registeredChannels() {
        return Collections.unmodifiableSet(new TreeSet<ChannelId>(RECEIVERS.keySet()));
    }

    /** True only for a connection that completed the compatible Loader handshake. */
    public static boolean canSend(NetworkConnection connection) {
        return NamedChannelTransport.canSend(connection);
    }

    public static Optional<MultiplayerManifest> peerManifest(NetworkConnection connection) {
        return DesktopMultiplayerTransport.peerManifest(connection);
    }

    public static boolean isModPresent(NetworkConnection connection, String modId) {
        if (modId == null) return false;
        return peerManifest(connection).map(manifest -> manifest.find(modId) != null).orElse(false);
    }

    /** Sends to one Loader client, throwing before any packet reaches a vanilla connection. */
    public static void send(NetworkEngine engine, NetworkConnection connection,
            ChannelId channel, PacketPayload payload) {
        NamedChannelTransport.sendToClient(engine, connection, channel, payload);
    }

    public static <T> void send(NetworkEngine engine, NetworkConnection connection,
            ChannelId channel, PacketCodec<T> codec, T value) {
        if (codec == null) throw new NullPointerException("codec");
        send(engine, connection, channel, codec.encodePayload(value));
    }

    /** Sends only to validated Loader clients and returns the number of recipients. */
    public static int broadcast(NetworkEngine engine, ChannelId channel, PacketPayload payload) {
        return NamedChannelTransport.broadcast(engine, channel, payload);
    }

    public static <T> int broadcast(NetworkEngine engine, ChannelId channel,
            PacketCodec<T> codec, T value) {
        if (codec == null) throw new NullPointerException("codec");
        return broadcast(engine, channel, codec.encodePayload(value));
    }

    static void receive(NetworkEngine engine, NetworkConnection connection,
            ChannelId channel, PacketPayload payload) {
        Receiver receiver = RECEIVERS.get(channel);
        if (receiver != null) receiver.receive(engine, connection, channel, payload);
    }

    @FunctionalInterface
    public interface Receiver {
        /** Runs from the game's system-packet processing path. */
        void receive(NetworkEngine engine, NetworkConnection sender,
                ChannelId channel, PacketPayload payload);
    }

    @FunctionalInterface
    public interface TypedReceiver<T> {
        void receive(NetworkEngine engine, NetworkConnection sender,
                ChannelId channel, T value);
    }
}
