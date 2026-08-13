package io.github.endx.rustedfabricapi.api.multiplayer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import io.github.endx.rustedfabricapi.api.RustedFabricRuntime;
import io.github.endx.rustedfabricapi.api.session.GameSession;
import io.github.endx.rustedfabricapi.api.session.GameSessionRuntime;

/**
 * Reflection-only transport SPI used by both platform backends. Game symbols stay in Mapping,
 * while mods consume the platform-neutral manifest/session/event API.
 */
public final class MultiplayerNetworkBridge {
    public enum Side { HOST, CLIENT }

    public interface Logger {
        void log(String message, Throwable failure);
    }

    public static final class Mapping {
        private final String packetClass;
        private final String packetTypeField;
        private final String packetBytesField;
        private final String packetConnectionField;
        private final String sendPacketMethod;
        private final String disconnectMethod;
        private final String connectionIdField;

        public Mapping(String packetClass, String packetTypeField, String packetBytesField,
                String packetConnectionField, String sendPacketMethod,
                String disconnectMethod, String connectionIdField) {
            this.packetClass = required(packetClass);
            this.packetTypeField = required(packetTypeField);
            this.packetBytesField = required(packetBytesField);
            this.packetConnectionField = required(packetConnectionField);
            this.sendPacketMethod = required(sendPacketMethod);
            this.disconnectMethod = required(disconnectMethod);
            this.connectionIdField = required(connectionIdField);
        }

        private static String required(String value) {
            if (value == null || value.isEmpty()) throw new IllegalArgumentException("mapping");
            return value;
        }
    }

    private static final long DEFAULT_TIMEOUT_MILLIS = 5000L;
    private final Mapping mapping;
    private final Logger logger;
    private final long timeoutMillis;
    private final Map<Object, PeerState> peers =
            Collections.synchronizedMap(new WeakHashMap<Object, PeerState>());
    private final ScheduledExecutorService scheduler;

    public MultiplayerNetworkBridge(Mapping mapping, Logger logger) {
        this(mapping, logger, DEFAULT_TIMEOUT_MILLIS);
    }

    public MultiplayerNetworkBridge(Mapping mapping, Logger logger, long timeoutMillis) {
        this.mapping = Objects.requireNonNull(mapping, "mapping");
        this.logger = logger != null ? logger : (message, failure) -> { };
        if (timeoutMillis < 100L) throw new IllegalArgumentException("timeoutMillis");
        this.timeoutMillis = timeoutMillis;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override public Thread newThread(Runnable task) {
                Thread thread = new Thread(task, "rusted-fabric-handshake");
                thread.setDaemon(true);
                return thread;
            }
        });
    }

    public void connectionReady(Object networkEngine, Object connection, Side side) {
        if (networkEngine == null || connection == null || side == null) return;
        GameSessionRuntime.transition(side == Side.HOST
                ? GameSession.Kind.MULTIPLAYER_HOST : GameSession.Kind.MULTIPLAYER_CLIENT);
        PeerState state;
        synchronized (peers) {
            state = peers.get(connection);
            if (state == null) {
                state = new PeerState(peerId(connection));
                peers.put(connection, state);
            }
            if (state.helloSent) return;
            state.helloSent = true;
        }
        try {
            MultiplayerManifest local = localManifest();
            Object packet = newPacket(networkEngine, MultiplayerHandshake.encodeHello(local));
            invokeCompatible(networkEngine, mapping.sendPacketMethod, connection, packet);
            logger.log("Sent RFH1 hello to " + state.peerId, null);
        } catch (ThreadDeath | VirtualMachineError critical) {
            throw critical;
        } catch (Throwable failure) {
            logger.log("Could not send RFH1 hello to " + state.peerId, failure);
        }
        PeerState scheduledState = state;
        scheduler.schedule(() -> finishLegacyTimeout(connection, scheduledState),
                timeoutMillis, TimeUnit.MILLISECONDS);
    }

    /** Returns true when the packet belongs to RFH1 and the game's default unknown-packet log may be skipped. */
    public boolean receive(Object networkEngine, Object packet) {
        if (packet == null) return false;
        try {
            int type = ((Number) readField(packet, mapping.packetTypeField)).intValue();
            if (type != MultiplayerHandshake.GAME_PACKET_TYPE) return false;
            Object connection = readField(packet, mapping.packetConnectionField);
            if (connection == null) throw new IllegalArgumentException("Handshake has no connection");
            byte[] payload = (byte[]) readField(packet, mapping.packetBytesField);
            MultiplayerManifest remote = MultiplayerHandshake.decodeHello(payload);
            PeerState state;
            MultiplayerPeerCompatibility result;
            synchronized (peers) {
                state = peers.get(connection);
                if (state == null) {
                    state = new PeerState(peerId(connection));
                    peers.put(connection, state);
                }
                result = MultiplayerPeerCompatibility.evaluate(
                        state.peerId, localManifest(), remote);
                state.loaderPeer = true;
                state.remoteManifest = remote;
                state.compatible = result.compatible();
                state.resolved = true;
            }
            logger.log("RFH1 peer " + state.peerId + " compatible=" + result.compatible(), null);
            if (!result.compatible()) disconnect(connection, reason(result));
            return true;
        } catch (ThreadDeath | VirtualMachineError critical) {
            throw critical;
        } catch (Throwable failure) {
            logger.log("Invalid RFH1 handshake", failure);
            try {
                Object connection = readField(packet, mapping.packetConnectionField);
                if (connection != null) disconnect(connection,
                        "Rusted Fabric handshake is invalid");
            } catch (Throwable ignored) {
                logger.log("Could not close invalid RFH1 peer", ignored);
            }
            return true;
        }
    }

    public void resetToSinglePlayer() {
        peers.clear();
        GameSessionRuntime.transition(GameSession.Kind.SINGLE_PLAYER);
    }

    /** Drops all Loader state associated with a closed connection. */
    public void connectionClosed(Object connection) {
        if (connection == null) return;
        synchronized (peers) {
            peers.remove(connection);
        }
    }

    /** Synchronous host-side gate used immediately before the game's start packet is sent. */
    public boolean allowGameStart(Object connection) {
        if (connection == null) {
            // The native host uses null for its ordinary broadcast-to-all start packet. Check
            // every connection already registered by the handshake bridge; an empty set is the
            // normal single-player Advanced/Sandbox lobby and must never be rejected.
            List<Object> broadcastPeers;
            synchronized (peers) {
                if (peers.isEmpty()) return true;
                broadcastPeers = new ArrayList<Object>(peers.keySet());
            }
            for (Object peer : broadcastPeers) {
                if (peer != null && !allowGameStart(peer)) return false;
            }
            return true;
        }
        PeerState state;
        MultiplayerPeerCompatibility result;
        synchronized (peers) {
            state = peers.get(connection);
            if (state == null) {
                state = new PeerState(peerId(connection));
                peers.put(connection, state);
            }
            if (state.resolved) return Boolean.TRUE.equals(state.compatible);
            result = MultiplayerPeerCompatibility.evaluateVanilla(
                    state.peerId, localManifest());
            state.compatible = result.compatible();
            state.resolved = true;
        }
        try {
            if (!result.compatible()) disconnect(connection,
                    "Rusted Fabric mods require a compatible Loader peer");
            return result.compatible();
        } catch (ThreadDeath | VirtualMachineError critical) {
            throw critical;
        } catch (Throwable failure) {
            logger.log("Could not check peer before game start " + state.peerId, failure);
            return false;
        }
    }

    /** Returns whether this exact connection completed RFH1 as a compatible Loader peer. */
    public boolean isLoaderPeer(Object connection) {
        if (connection == null) return false;
        synchronized (peers) {
            PeerState state = peers.get(connection);
            return state != null && state.loaderPeer
                    && state.resolved && Boolean.TRUE.equals(state.compatible);
        }
    }

    /** Returns whether at least one connection completed RFH1 as a compatible Loader peer. */
    public boolean hasLoaderPeer() {
        synchronized (peers) {
            for (PeerState state : peers.values()) {
                if (state.loaderPeer && state.resolved && Boolean.TRUE.equals(state.compatible)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Returns the manifest received from this compatible Loader peer. */
    public Optional<MultiplayerManifest> peerManifest(Object connection) {
        if (connection == null) return Optional.empty();
        synchronized (peers) {
            PeerState state = peers.get(connection);
            return state != null && state.loaderPeer
                    && state.resolved && Boolean.TRUE.equals(state.compatible)
                    ? Optional.ofNullable(state.remoteManifest) : Optional.empty();
        }
    }

    /** Client-side convenience for the single compatible remote server manifest. */
    public Optional<MultiplayerManifest> firstLoaderPeerManifest() {
        synchronized (peers) {
            for (PeerState state : peers.values()) {
                if (state.loaderPeer && state.resolved && Boolean.TRUE.equals(state.compatible)
                        && state.remoteManifest != null) {
                    return Optional.of(state.remoteManifest);
                }
            }
        }
        return Optional.empty();
    }

    private void finishLegacyTimeout(Object connection, PeerState expected) {
        MultiplayerPeerCompatibility result;
        synchronized (peers) {
            PeerState current = peers.get(connection);
            if (current != expected || current.resolved) return;
            result = MultiplayerPeerCompatibility.evaluateVanilla(
                    expected.peerId, localManifest());
            expected.compatible = result.compatible();
            current.resolved = true;
        }
        try {
            logger.log("RFH1 timeout for " + expected.peerId
                    + "; vanilla-compatible=" + result.compatible(), null);
            if (!result.compatible()) disconnect(connection,
                    "Rusted Fabric mods require a compatible Loader peer");
        } catch (ThreadDeath | VirtualMachineError critical) {
            throw critical;
        } catch (Throwable failure) {
            logger.log("Could not resolve legacy peer " + expected.peerId, failure);
        }
    }

    private MultiplayerManifest localManifest() {
        MultiplayerManifest base = RustedFabricRuntime.currentContext()
                .flatMap(context -> context.multiplayerManifest())
                .orElseGet(() -> MultiplayerManifest.empty("unknown"));
        return MultiplayerRequirements.effective(base);
    }

    private Object newPacket(Object engine, byte[] payload) throws Exception {
        ClassLoader loader = engine.getClass().getClassLoader();
        Class<?> packetType = Class.forName(mapping.packetClass, true, loader);
        Constructor<?> constructor = packetType.getDeclaredConstructor(int.class);
        constructor.setAccessible(true);
        Object packet = constructor.newInstance(MultiplayerHandshake.GAME_PACKET_TYPE);
        writeField(packet, mapping.packetBytesField, payload);
        return packet;
    }

    private void disconnect(Object connection, String reason) throws Exception {
        invokeCompatible(connection, mapping.disconnectMethod, reason);
    }

    private String peerId(Object connection) {
        try {
            return "connection-" + readField(connection, mapping.connectionIdField);
        } catch (Throwable ignored) {
            return "connection-" + Integer.toHexString(System.identityHashCode(connection));
        }
    }

    private static String reason(MultiplayerPeerCompatibility result) {
        if (result.report().issues().isEmpty()) return "Rusted Fabric compatibility mismatch";
        MultiplayerCompatibility.Issue issue = result.report().issues().get(0);
        return "Rusted Fabric mismatch: " + issue.problem() + " (" + issue.modId() + ")";
    }

    private static Object readField(Object owner, String name) throws Exception {
        Field field = findField(owner.getClass(), name);
        return field.get(owner);
    }

    private static void writeField(Object owner, String name, Object value) throws Exception {
        Field field = findField(owner.getClass(), name);
        field.set(owner, value);
    }

    private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        for (String candidate : name.split("\\|")) {
            for (Class<?> current = type; current != null; current = current.getSuperclass()) {
                try {
                    Field field = current.getDeclaredField(candidate);
                    field.setAccessible(true);
                    return field;
                } catch (NoSuchFieldException ignored) { }
            }
        }
        throw new NoSuchFieldException(type.getName() + '#' + name);
    }

    private static Object invokeCompatible(Object owner, String name, Object... arguments)
            throws Exception {
        for (Class<?> current = owner.getClass(); current != null; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (!matchesName(method.getName(), name)
                        || method.getParameterCount() != arguments.length) continue;
                Class<?>[] parameters = method.getParameterTypes();
                boolean compatible = true;
                for (int index = 0; index < parameters.length; index++) {
                    if (arguments[index] != null
                            && !box(parameters[index]).isInstance(arguments[index])) {
                        compatible = false;
                        break;
                    }
                }
                if (!compatible) continue;
                method.setAccessible(true);
                return method.invoke(owner, arguments);
            }
        }
        throw new NoSuchMethodException(owner.getClass().getName() + '#' + name);
    }

    private static boolean matchesName(String actual, String candidates) {
        for (String candidate : candidates.split("\\|")) {
            if (actual.equals(candidate)) return true;
        }
        return false;
    }

    private static Class<?> box(Class<?> value) {
        if (!value.isPrimitive()) return value;
        if (value == boolean.class) return Boolean.class;
        if (value == byte.class) return Byte.class;
        if (value == short.class) return Short.class;
        if (value == int.class) return Integer.class;
        if (value == long.class) return Long.class;
        if (value == float.class) return Float.class;
        if (value == double.class) return Double.class;
        if (value == char.class) return Character.class;
        return value;
    }

    private static final class PeerState {
        final String peerId;
        boolean helloSent;
        boolean loaderPeer;
        boolean resolved;
        volatile Boolean compatible;
        MultiplayerManifest remoteManifest;
        PeerState(String peerId) { this.peerId = peerId; }
    }
}
