package io.github.endx.rustedfabricapi.api.networking;

import io.github.endx.rustedfabricapi.desktop.DesktopMultiplayerTransport;
import rustedwarfare.network.NetworkConnection;
import rustedwarfare.network.NetworkEngine;
import rustedwarfare.network.Packet;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.zip.CRC32;

/** Internal packet envelope and game-transport adapter used by the public networking API. */
public final class NamedChannelTransport {
    public static final int GAME_PACKET_TYPE = 180;
    private static final int MAGIC = 0x52464E31; // RFN1
    private static final byte VERSION = 1;
    private static final int HEADER_BYTES = Integer.BYTES + 1 + Short.BYTES
            + Integer.BYTES + Integer.BYTES;

    private NamedChannelTransport() {
    }

    public static boolean receive(NetworkEngine engine, Packet packet) {
        if (packet == null || packet.type != GAME_PACKET_TYPE) return false;
        try {
            NetworkConnection connection = packet.connection;
            if (engine == null || connection == null
                    || !DesktopMultiplayerTransport.isLoaderPeer(connection)) {
                throw new IllegalArgumentException("Named payload came from a non-Loader peer");
            }
            Decoded decoded = decode(packet.bytes);
            if (engine.isServer) {
                ServerNetworking.receive(engine, connection, decoded.channel, decoded.payload);
            } else {
                ClientNetworking.receive(engine, connection, decoded.channel, decoded.payload);
            }
        } catch (ThreadDeath | VirtualMachineError critical) {
            throw critical;
        } catch (Throwable failure) {
            System.err.println("[Rusted Fabric networking] Rejected RFN1 payload: " + failure);
        }
        return true;
    }

    static boolean canSend(NetworkConnection connection) {
        return connection != null && connection.isOpen()
                && DesktopMultiplayerTransport.isLoaderPeer(connection);
    }

    static boolean canSendToServer() {
        return DesktopMultiplayerTransport.hasLoaderPeer();
    }

    static void sendToServer(NetworkEngine engine, ChannelId channel, PacketPayload payload) {
        requireClient(engine);
        if (!canSendToServer()) {
            throw new IllegalStateException("The server is not a compatible Rusted Fabric peer");
        }
        engine.sendPacketToServer(packet(channel, payload));
    }

    static void sendToClient(NetworkEngine engine, NetworkConnection connection,
            ChannelId channel, PacketPayload payload) {
        requireServer(engine);
        if (!canSend(connection)) {
            throw new IllegalArgumentException("Connection is not a compatible Rusted Fabric peer");
        }
        engine.sendPacketToConnection(connection, packet(channel, payload));
    }

    static int broadcast(NetworkEngine engine, ChannelId channel, PacketPayload payload) {
        requireServer(engine);
        Objects.requireNonNull(channel, "channel");
        Objects.requireNonNull(payload, "payload");
        int sent = 0;
        for (Object value : engine.connections) {
            if (value instanceof NetworkConnection) {
                NetworkConnection connection = (NetworkConnection) value;
                if (connection.validated && canSend(connection)) {
                    engine.sendPacketToConnection(connection, packet(channel, payload));
                    sent++;
                }
            }
        }
        return sent;
    }

    static byte[] encode(ChannelId channel, PacketPayload payload) {
        Objects.requireNonNull(channel, "channel");
        Objects.requireNonNull(payload, "payload");
        byte[] channelBytes = channel.toString().getBytes(StandardCharsets.UTF_8);
        byte[] payloadBytes = payload.copyBytes();
        CRC32 checksum = checksum(channelBytes, payloadBytes);
        ByteBuffer output = ByteBuffer.allocate(HEADER_BYTES
                + channelBytes.length + payloadBytes.length);
        output.putInt(MAGIC);
        output.put(VERSION);
        output.putShort((short) channelBytes.length);
        output.putInt(payloadBytes.length);
        output.putInt((int) checksum.getValue());
        output.put(channelBytes);
        output.put(payloadBytes);
        return output.array();
    }

    static Decoded decode(byte[] bytes) {
        if (bytes == null || bytes.length < HEADER_BYTES) {
            throw new IllegalArgumentException("Named payload is truncated");
        }
        ByteBuffer input = ByteBuffer.wrap(bytes);
        if (input.getInt() != MAGIC || input.get() != VERSION) {
            throw new IllegalArgumentException("Unsupported named payload envelope");
        }
        int channelLength = Short.toUnsignedInt(input.getShort());
        int payloadLength = input.getInt();
        long expectedChecksum = Integer.toUnsignedLong(input.getInt());
        if (channelLength == 0 || channelLength > ChannelId.MAX_ENCODED_BYTES
                || payloadLength < 0 || payloadLength > PacketPayload.MAX_BYTES
                || input.remaining() != channelLength + payloadLength) {
            throw new IllegalArgumentException("Invalid named payload lengths");
        }
        byte[] channelBytes = new byte[channelLength];
        byte[] payloadBytes = new byte[payloadLength];
        input.get(channelBytes);
        input.get(payloadBytes);
        if (checksum(channelBytes, payloadBytes).getValue() != expectedChecksum) {
            throw new IllegalArgumentException("Named payload checksum mismatch");
        }
        return new Decoded(ChannelId.parse(new String(channelBytes, StandardCharsets.UTF_8)),
                PacketPayload.of(payloadBytes));
    }

    private static Packet packet(ChannelId channel, PacketPayload payload) {
        Packet packet = new Packet(GAME_PACKET_TYPE);
        packet.bytes = encode(channel, payload);
        packet.reliable = true;
        return packet;
    }

    private static CRC32 checksum(byte[] channel, byte[] payload) {
        CRC32 checksum = new CRC32();
        checksum.update(channel, 0, channel.length);
        checksum.update(payload, 0, payload.length);
        return checksum;
    }

    private static void requireServer(NetworkEngine engine) {
        Objects.requireNonNull(engine, "engine");
        if (!engine.isServer) throw new IllegalStateException("Operation requires a multiplayer host");
    }

    private static void requireClient(NetworkEngine engine) {
        Objects.requireNonNull(engine, "engine");
        if (engine.isServer) throw new IllegalStateException("Operation requires a multiplayer client");
        if (!engine.networkingStarted) throw new IllegalStateException("Client is not connected");
    }

    static final class Decoded {
        final ChannelId channel;
        final PacketPayload payload;

        Decoded(ChannelId channel, PacketPayload payload) {
            this.channel = channel;
            this.payload = payload;
        }
    }
}
