package io.github.endx.rustedfabricapi.api.multiplayer;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** Platform-neutral multiplayer identity for one enabled mod. */
public final class MultiplayerMod implements Comparable<MultiplayerMod> {
    private static final Pattern ID = Pattern.compile("[a-z][a-z0-9_-]{0,63}");
    private static final Pattern VERSION = Pattern.compile("[0-9A-Za-z][0-9A-Za-z._+\\-]{0,63}");
    private static final Pattern PROTOCOL = Pattern.compile("[0-9A-Za-z][0-9A-Za-z._+\\-]{0,63}");
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");

    public enum Mode {
        /** May change presentation or controls, but must not change synchronized game state. */
        CLIENT_ONLY("client_only"),
        /** Every peer must provide the same protocol and platform-neutral synchronized-data hash. */
        REQUIRED("required"),
        /** Has not declared a safe cross-platform contract; modded multiplayer must be blocked. */
        UNSAFE("unsafe");

        private final String wireName;

        Mode(String wireName) {
            this.wireName = wireName;
        }

        public String wireName() {
            return wireName;
        }

        public static Mode parse(String value) {
            for (Mode mode : values()) {
                if (mode.wireName.equals(value)) return mode;
            }
            throw new IllegalArgumentException("Unknown multiplayer mode: " + value);
        }
    }

    private final String id;
    private final String version;
    private final Mode mode;
    private final String protocol;
    private final String syncHash;

    public MultiplayerMod(String id, String version, Mode mode, String protocol, String syncHash) {
        this.id = require(ID, id, "mod id");
        this.version = require(VERSION, version, "mod version");
        this.mode = Objects.requireNonNull(mode, "mode");
        String normalizedProtocol = normalize(protocol);
        String normalizedHash = normalize(syncHash).toLowerCase(Locale.ROOT);
        if (mode == Mode.REQUIRED) {
            this.protocol = require(PROTOCOL, normalizedProtocol, "multiplayer protocol");
            this.syncHash = require(SHA256, normalizedHash, "synchronized-data SHA-256");
        } else {
            if (!normalizedProtocol.isEmpty() || !normalizedHash.isEmpty()) {
                throw new IllegalArgumentException(
                        "Only required multiplayer mods may declare protocol or sync hash");
            }
            this.protocol = "";
            this.syncHash = "";
        }
    }

    public static MultiplayerMod clientOnly(String id, String version) {
        return new MultiplayerMod(id, version, Mode.CLIENT_ONLY, "", "");
    }

    public static MultiplayerMod unsafe(String id, String version) {
        return new MultiplayerMod(id, version, Mode.UNSAFE, "", "");
    }

    public static MultiplayerMod required(String id, String version,
                                          String protocol, String syncHash) {
        return new MultiplayerMod(id, version, Mode.REQUIRED, protocol, syncHash);
    }

    public String id() { return id; }
    public String version() { return version; }
    public Mode mode() { return mode; }
    public String protocol() { return protocol; }
    public String syncHash() { return syncHash; }

    @Override
    public int compareTo(MultiplayerMod other) {
        return id.compareTo(other.id);
    }

    private static String require(Pattern pattern, String value, String label) {
        String normalized = normalize(value);
        if (!pattern.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid " + label);
        }
        return normalized;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
