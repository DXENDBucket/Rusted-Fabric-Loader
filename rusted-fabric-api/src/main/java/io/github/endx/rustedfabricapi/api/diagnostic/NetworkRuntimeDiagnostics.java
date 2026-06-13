package io.github.endx.rustedfabricapi.api.diagnostic;

import io.github.endx.rustedfabricapi.api.util.RustedReflection;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class NetworkRuntimeDiagnostics {
    private static final String[] NETWORK_ENGINE_CLASSES = {
            "rustedwarfare.network.NetworkEngine",
            "com.corrodinggames.rts.gameFramework.j.ad"
    };
    private static final String[] GAME_SETUP_CLASSES = {
            "rustedwarfare.network.GameSetup",
            "com.corrodinggames.rts.gameFramework.j.ah"
    };
    private static final String[] MAP_TYPE_CLASSES = {
            "rustedwarfare.network.MapType",
            "com.corrodinggames.rts.gameFramework.j.ai"
    };
    private static final String[] PACKET_CLASSES = {
            "rustedwarfare.network.Packet",
            "com.corrodinggames.rts.gameFramework.j.au"
    };
    private static final String[] NETWORK_CONNECTION_CLASSES = {
            "rustedwarfare.network.NetworkConnection",
            "com.corrodinggames.rts.gameFramework.j.c"
    };
    private static final String[] FORWARDED_SOCKET_CLASSES = {
            "rustedwarfare.network.ForwardedSocket",
            "com.corrodinggames.rts.gameFramework.j.h"
    };
    private static final String[] FORWARDED_PACKET_CLASSES = {
            "rustedwarfare.network.ForwardedPacket",
            "com.corrodinggames.rts.gameFramework.j.ay"
    };
    private static final String[] GAME_SERVER_INFO_CLASSES = {
            "rustedwarfare.network.GameServerInfo",
            "com.corrodinggames.rts.gameFramework.j.g"
    };
    private static final String[] MASTER_SERVER_CLIENT_CLASSES = {
            "rustedwarfare.network.MasterServerClient",
            "com.corrodinggames.rts.gameFramework.j.n"
    };
    private static final String[] PASSWORD_PROMPT_CLASSES = {
            "rustedwarfare.network.PasswordPrompt",
            "com.corrodinggames.rts.gameFramework.j.ae"
    };

    private NetworkRuntimeDiagnostics() {
    }

    public static Object currentNetworkEngine() {
        Object engine = GameEngineDiagnostics.currentEngineOrNull();
        if (engine == null) {
            return null;
        }

        try {
            Object value = RustedReflection.getFieldValue(engine, new String[]{"networkEngine", "bX"});
            if (isNetworkEngine(value)) {
                return value;
            }
        } catch (RuntimeException ignored) {
        }

        return firstFieldAssignableTo(engine, NETWORK_ENGINE_CLASSES);
    }

    public static boolean isNetworkEngine(Object value) {
        return isAny(value, NETWORK_ENGINE_CLASSES);
    }

    public static boolean isGameSetup(Object value) {
        return isAny(value, GAME_SETUP_CLASSES);
    }

    public static boolean isMapType(Object value) {
        return isAny(value, MAP_TYPE_CLASSES);
    }

    public static boolean isPacket(Object value) {
        return isAny(value, PACKET_CLASSES);
    }

    public static boolean isNetworkConnection(Object value) {
        return isAny(value, NETWORK_CONNECTION_CLASSES);
    }

    public static boolean isForwardedSocket(Object value) {
        return isAny(value, FORWARDED_SOCKET_CLASSES);
    }

    public static boolean isForwardedPacket(Object value) {
        return isAny(value, FORWARDED_PACKET_CLASSES);
    }

    public static boolean isGameServerInfo(Object value) {
        return isAny(value, GAME_SERVER_INFO_CLASSES);
    }

    public static boolean isMasterServerClient(Object value) {
        return isAny(value, MASTER_SERVER_CLIENT_CLASSES);
    }

    public static boolean isPasswordPrompt(Object value) {
        return isAny(value, PASSWORD_PROMPT_CLASSES);
    }

    public static Map<String, Object> describeCurrentNetworkEngine() {
        Object networkEngine = currentNetworkEngine();
        return networkEngine != null ? describeNetworkEngine(networkEngine) : Collections.emptyMap();
    }

    public static Map<String, Object> describeNetworkEngine(Object networkEngine) {
        requireAny(networkEngine, NETWORK_ENGINE_CLASSES, "NetworkEngine");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", networkEngine.getClass().getName());
        putBooleanField(result, networkEngine, "isServer", new String[]{"isServer", "C"});
        putIntField(result, networkEngine, "networkProtocolVersion", new String[]{"networkProtocolVersion", "e"});
        putIntField(result, networkEngine, "serverPort", new String[]{"serverPort", "m"});
        putIntField(result, networkEngine, "udpDiscoveryPort", new String[]{"udpDiscoveryPort", "t"});
        putStringField(result, networkEngine, "localPlayerName", new String[]{"localPlayerName", "y"});
        putStringField(result, networkEngine, "ownServerId", new String[]{"ownServerId", "bw"});
        putStringField(result, networkEngine, "resolvedNetworkMapPath",
                new String[]{"resolvedNetworkMapPath", "az"});
        putBooleanField(result, networkEngine, "hasPendingQuickResync",
                new String[]{"hasPendingQuickResync", "N"});
        putIntField(result, networkEngine, "nextChecksumFrame", new String[]{"nextChecksumFrame", "ah"});
        putIntField(result, networkEngine, "checksumInterval", new String[]{"checksumInterval", "ai"});
        putCollectionSizeField(result, networkEngine, "connectionCount", new String[]{"connections", "aM"});
        putCollectionSizeField(result, networkEngine, "serverListSize", new String[]{"serverList", "bi"});
        putField(result, networkEngine, "activeConnectThread", new String[]{"activeConnectThread", "bF"});
        putField(result, networkEngine, "defaultPasswordPrompt", new String[]{"defaultPasswordPrompt", "bE"});
        Object gameSetup = fieldValueOrNull(networkEngine, new String[]{"gameSetup", "ay"});
        result.put("gameSetup", gameSetup);
        result.put("gameSetupDescription", gameSetup != null && isGameSetup(gameSetup)
                ? describeGameSetup(gameSetup) : Collections.emptyMap());
        result.put("enabledModsSummary", invokeStringOrEmpty(networkEngine,
                new String[]{"getEnabledModsSummary", "au"}));
        return Collections.unmodifiableMap(result);
    }

    public static List<Object> currentConnections() {
        Object networkEngine = currentNetworkEngine();
        return networkEngine != null ? connections(networkEngine) : Collections.<Object>emptyList();
    }

    public static List<Object> connections(Object networkEngine) {
        requireAny(networkEngine, NETWORK_ENGINE_CLASSES, "NetworkEngine");
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(
                fieldValueOrNull(networkEngine, new String[]{"connections", "aM"})));
    }

    public static List<Object> currentServerList() {
        Object networkEngine = currentNetworkEngine();
        return networkEngine != null ? serverList(networkEngine) : Collections.<Object>emptyList();
    }

    public static List<Object> serverList(Object networkEngine) {
        requireAny(networkEngine, NETWORK_ENGINE_CLASSES, "NetworkEngine");
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(
                fieldValueOrNull(networkEngine, new String[]{"serverList", "bi"})));
    }

    public static Object gameSetup(Object networkEngine) {
        requireAny(networkEngine, NETWORK_ENGINE_CLASSES, "NetworkEngine");
        return fieldValueOrNull(networkEngine, new String[]{"gameSetup", "ay"});
    }

    public static Map<String, Object> describeGameSetup(Object gameSetup) {
        requireAny(gameSetup, GAME_SETUP_CLASSES, "GameSetup");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", gameSetup.getClass().getName());
        Object mapType = fieldValueOrNull(gameSetup, new String[]{"mapType", "a"});
        result.put("mapType", mapType);
        result.put("mapTypeName", mapType != null ? String.valueOf(mapType) : "");
        result.put("mapTypeDisplayName", mapType != null
                ? invokeStringOrEmpty(mapType, new String[]{"getDisplayName", "a"}) : "");
        putStringField(result, gameSetup, "mapPath", new String[]{"mapPath", "b"});
        putIntField(result, gameSetup, "startingCredits", new String[]{"startingCredits", "c"});
        putIntField(result, gameSetup, "fogMode", new String[]{"fogMode", "d"});
        putBooleanField(result, gameSetup, "revealedMap", new String[]{"revealedMap", "e"});
        putIntField(result, gameSetup, "aiDifficulty", new String[]{"aiDifficulty", "f"});
        putIntField(result, gameSetup, "startingUnits", new String[]{"startingUnits", "g"});
        putFloatField(result, gameSetup, "incomeMultiplier", new String[]{"incomeMultiplier", "h"});
        putBooleanField(result, gameSetup, "noNukes", new String[]{"noNukes", "i"});
        putBooleanField(result, gameSetup, "sharedControl", new String[]{"sharedControl", "l"});
        putIntField(result, gameSetup, "randomSeed", new String[]{"randomSeed", "q"});
        result.put("description", invokeStringOrEmpty(gameSetup, new String[]{"describe", "b"}));
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describePacket(Object packet) {
        requireAny(packet, PACKET_CLASSES, "Packet");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", packet.getClass().getName());
        putField(result, packet, "connection", new String[]{"connection", "a"});
        putIntField(result, packet, "type", new String[]{"type", "b"});
        putByteArrayLengthField(result, packet, "bytesLength", new String[]{"bytes", "c"});
        putIntField(result, packet, "extraData", new String[]{"extraData", "d"});
        putBooleanField(result, packet, "reliable", new String[]{"reliable", "e"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeNetworkConnection(Object connection) {
        requireAny(connection, NETWORK_CONNECTION_CLASSES, "NetworkConnection");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", connection.getClass().getName());
        putBooleanField(result, connection, "disconnected", new String[]{"disconnected", "a"});
        putIntField(result, connection, "connectionId", new String[]{"connectionId", "c"});
        putBooleanField(result, connection, "validated", new String[]{"validated", "p"});
        putBooleanField(result, connection, "isLocalOrClosed", new String[]{"isLocalOrClosed", "s"});
        putBooleanField(result, connection, "hasMinorDesync", new String[]{"hasMinorDesync", "v"});
        putBooleanField(result, connection, "hasCompleteDesync", new String[]{"hasCompleteDesync", "w"});
        putIntField(result, connection, "syncMatches", new String[]{"syncMatches", "x"});
        putIntField(result, connection, "desyncCount", new String[]{"desyncCount", "y"});
        result.put("displayName", invokeStringOrEmpty(connection, new String[]{"getDisplayName", "e"}));
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeForwardedSocket(Object socket) {
        requireAny(socket, FORWARDED_SOCKET_CLASSES, "ForwardedSocket");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", socket.getClass().getName());
        putField(result, socket, "parentConnection", new String[]{"parentConnection", "a"});
        putIntField(result, socket, "forwardedConnectionId", new String[]{"forwardedConnectionId", "b"});
        putBooleanField(result, socket, "closed", new String[]{"closed", "c"});
        putField(result, socket, "inputStream", new String[]{"inputStream", "d"});
        putField(result, socket, "outputStream", new String[]{"outputStream", "e"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeForwardedPacket(Object packet) {
        requireAny(packet, FORWARDED_PACKET_CLASSES, "ForwardedPacket");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", packet.getClass().getName());
        putField(result, packet, "wrappedPacket", new String[]{"wrappedPacket", "f"});
        putIntField(result, packet, "forwardedConnectionId", new String[]{"forwardedConnectionId", "g"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeGameServerInfo(Object serverInfo) {
        requireAny(serverInfo, GAME_SERVER_INFO_CLASSES, "GameServerInfo");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", serverInfo.getClass().getName());
        putBooleanField(result, serverInfo, "isLan", new String[]{"isLan", "a"});
        putStringField(result, serverInfo, "serverId", new String[]{"serverId", "b"});
        putStringField(result, serverInfo, "privateIp", new String[]{"privateIp", "c"});
        putStringField(result, serverInfo, "publicIp", new String[]{"publicIp", "d"});
        putStringField(result, serverInfo, "directUrl", new String[]{"directUrl", "e"});
        putIntField(result, serverInfo, "port", new String[]{"port", "g"});
        putBooleanField(result, serverInfo, "portOpen", new String[]{"portOpen", "h"});
        putStringField(result, serverInfo, "gameVersionDisplay", new String[]{"gameVersionDisplay", "k"});
        putBooleanField(result, serverInfo, "passwordRequired", new String[]{"passwordRequired", "m"});
        putStringField(result, serverInfo, "createdBy", new String[]{"createdBy", "n"});
        putStringField(result, serverInfo, "mapName", new String[]{"mapName", "q"});
        putStringField(result, serverInfo, "gameMode", new String[]{"gameMode", "r"});
        putStringField(result, serverInfo, "gameStatus", new String[]{"gameStatus", "s"});
        putIntField(result, serverInfo, "playerCount", new String[]{"playerCount", "v"});
        putIntField(result, serverInfo, "maxPlayerCount", new String[]{"maxPlayerCount", "w"});
        putBooleanField(result, serverInfo, "isDedicatedServer", new String[]{"isDedicatedServer", "y"});
        putStringField(result, serverInfo, "modsNeeded", new String[]{"modsNeeded", "z"});
        putIntField(result, serverInfo, "connectionRelayId", new String[]{"connectionRelayId", "A"});
        putLongField(result, serverInfo, "lastSeenTimeMillis", new String[]{"lastSeenTimeMillis", "o"});
        result.put("ownServer", invokeBooleanOrFalse(serverInfo, new String[]{"isOwnServer", "a"}));
        result.put("compatibleVersion", invokeBooleanOrFalse(serverInfo, new String[]{"isCompatibleVersion", "g"}));
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeMasterServerClient(Object client) {
        requireAny(client, MASTER_SERVER_CLIENT_CLASSES, "MasterServerClient");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", client.getClass().getName());
        putBooleanField(result, client, "debugRequests", new String[]{"debugRequests", "a"});
        putBooleanField(result, client, "debugVerbose", new String[]{"debugVerbose", "b"});
        putArrayLengthField(result, client, "masterServerUrlsCount", new String[]{"masterServerUrls", "c"});
        putField(result, client, "httpClientProvider", new String[]{"httpClientProvider", "d"});
        putIntField(result, client, "nextServerListRequestId", new String[]{"nextServerListRequestId", "e"});
        putStringField(result, client, "lastBadMasterServerResponse",
                new String[]{"lastBadMasterServerResponse", "g"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describePasswordPrompt(Object prompt) {
        requireAny(prompt, PASSWORD_PROMPT_CLASSES, "PasswordPrompt");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", prompt.getClass().getName());
        putStringField(result, prompt, "promptMessage", new String[]{"promptMessage", "b"});
        putIntField(result, prompt, "targetConnectionId", new String[]{"targetConnectionId", "c"});
        putBooleanField(result, prompt, "replyToRemoteConnection", new String[]{"replyToRemoteConnection", "d"});
        putStringField(result, prompt, "customTitle", new String[]{"customTitle", "e"});
        putStringField(result, prompt, "positiveButtonText", new String[]{"positiveButtonText", "f"});
        putStringField(result, prompt, "negativeButtonText", new String[]{"negativeButtonText", "g"});
        return Collections.unmodifiableMap(result);
    }

    public static void setPasswordPromptLabels(Object prompt, String title,
                                               String positiveButtonText, String negativeButtonText) {
        requireAny(prompt, PASSWORD_PROMPT_CLASSES, "PasswordPrompt");
        RustedReflection.setFieldValue(prompt, new String[]{"customTitle", "e"}, title);
        RustedReflection.setFieldValue(prompt, new String[]{"positiveButtonText", "f"}, positiveButtonText);
        RustedReflection.setFieldValue(prompt, new String[]{"negativeButtonText", "g"}, negativeButtonText);
    }

    private static Object firstFieldAssignableTo(Object owner, String[] classNames) {
        Class<?> expected;
        try {
            expected = RustedReflection.findClass(classNames);
        } catch (RuntimeException e) {
            return null;
        }

        Class<?> current = owner.getClass();
        while (current != null) {
            Field[] fields = current.getDeclaredFields();
            for (Field field : fields) {
                if (!expected.isAssignableFrom(field.getType())) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    return field.get(owner);
                } catch (IllegalAccessException ignored) {
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private static boolean isAny(Object value, String[] classNames) {
        return value != null && RustedReflection.isAnyClass(value.getClass(), classNames);
    }

    private static void requireAny(Object value, String[] classNames, String label) {
        if (value == null || !RustedReflection.isAnyClass(value.getClass(), classNames)) {
            throw new IllegalArgumentException("Expected " + label + ", got " + describe(value));
        }
    }

    private static String describe(Object value) {
        return value == null ? "null" : value.getClass().getName();
    }

    private static Object fieldValueOrNull(Object owner, String[] fieldNames) {
        try {
            return RustedReflection.getFieldValue(owner, fieldNames);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String invokeStringOrEmpty(Object owner, String[] methodNames, Object... args) {
        try {
            Object value = RustedReflection.invokeInstance(owner, methodNames, args);
            return value != null ? value.toString() : "";
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static boolean invokeBooleanOrFalse(Object owner, String[] methodNames, Object... args) {
        try {
            return Boolean.TRUE.equals(RustedReflection.invokeInstance(owner, methodNames, args));
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static int collectionSize(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof java.util.Collection<?>) {
            return ((java.util.Collection<?>) value).size();
        }
        if (value instanceof Map<?, ?>) {
            return ((Map<?, ?>) value).size();
        }
        if (value.getClass().isArray()) {
            return java.lang.reflect.Array.getLength(value);
        }
        return 1;
    }

    private static void putField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        try {
            result.put(key, RustedReflection.getFieldValue(owner, fieldNames));
        } catch (RuntimeException ignored) {
        }
    }

    private static void putStringField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        try {
            result.put(key, RustedReflection.getStringField(owner, fieldNames));
        } catch (RuntimeException ignored) {
        }
    }

    private static void putIntField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        try {
            result.put(key, Integer.valueOf(RustedReflection.getIntField(owner, fieldNames)));
        } catch (RuntimeException ignored) {
        }
    }

    private static void putLongField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        try {
            Object value = RustedReflection.getFieldValue(owner, fieldNames);
            if (value instanceof Number) {
                result.put(key, Long.valueOf(((Number) value).longValue()));
            }
        } catch (RuntimeException ignored) {
        }
    }

    private static void putFloatField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        try {
            result.put(key, Float.valueOf(RustedReflection.getFloatField(owner, fieldNames)));
        } catch (RuntimeException ignored) {
        }
    }

    private static void putBooleanField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        try {
            result.put(key, Boolean.valueOf(RustedReflection.getBooleanField(owner, fieldNames)));
        } catch (RuntimeException ignored) {
        }
    }

    private static void putCollectionSizeField(Map<String, Object> result, Object owner, String key,
                                               String[] fieldNames) {
        try {
            result.put(key, Integer.valueOf(collectionSize(RustedReflection.getFieldValue(owner, fieldNames))));
        } catch (RuntimeException ignored) {
        }
    }

    private static void putArrayLengthField(Map<String, Object> result, Object owner, String key,
                                            String[] fieldNames) {
        try {
            Object array = RustedReflection.getFieldValue(owner, fieldNames);
            result.put(key, Integer.valueOf(array != null && array.getClass().isArray()
                    ? java.lang.reflect.Array.getLength(array) : 0));
        } catch (RuntimeException ignored) {
        }
    }

    private static void putByteArrayLengthField(Map<String, Object> result, Object owner, String key,
                                                String[] fieldNames) {
        try {
            Object value = RustedReflection.getFieldValue(owner, fieldNames);
            result.put(key, Integer.valueOf(value instanceof byte[] ? ((byte[]) value).length : 0));
        } catch (RuntimeException ignored) {
        }
    }
}
