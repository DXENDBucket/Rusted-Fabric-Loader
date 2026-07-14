package io.github.endx.rustedfabricapi.api.multiplayer;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/** Canonical cross-platform mod list carried by the RFH1 network handshake. */
public final class MultiplayerManifest {
    public static final String WIRE_VERSION = "RFM1";
    private static final Pattern PLATFORM = Pattern.compile("[a-z][a-z0-9._-]{0,31}");

    private final String platform;
    private final List<MultiplayerMod> mods;
    private final Map<String, MultiplayerMod> byId;

    public MultiplayerManifest(String platform, Collection<MultiplayerMod> mods) {
        String normalizedPlatform = platform == null
                ? "unknown" : platform.trim().toLowerCase(Locale.ROOT);
        if (!PLATFORM.matcher(normalizedPlatform).matches()) {
            throw new IllegalArgumentException("Invalid manifest platform");
        }
        List<MultiplayerMod> sorted = new ArrayList<>(Objects.requireNonNull(mods, "mods"));
        Collections.sort(sorted);
        Map<String, MultiplayerMod> indexed = new LinkedHashMap<>();
        for (MultiplayerMod mod : sorted) {
            Objects.requireNonNull(mod, "mod");
            if (indexed.put(mod.id(), mod) != null) {
                throw new IllegalArgumentException("Duplicate multiplayer mod id: " + mod.id());
            }
        }
        this.platform = normalizedPlatform;
        this.mods = Collections.unmodifiableList(sorted);
        this.byId = Collections.unmodifiableMap(indexed);
    }

    public static MultiplayerManifest empty(String platform) {
        return new MultiplayerManifest(platform, Collections.emptyList());
    }

    public String platform() { return platform; }
    public List<MultiplayerMod> mods() { return mods; }
    public MultiplayerMod find(String id) { return byId.get(id); }

    public int count(MultiplayerMod.Mode mode) {
        int count = 0;
        for (MultiplayerMod mod : mods) if (mod.mode() == mode) count++;
        return count;
    }

    public boolean permitsModdedMultiplayer() {
        return count(MultiplayerMod.Mode.UNSAFE) == 0;
    }

    public String encode() {
        StringBuilder value = new StringBuilder(WIRE_VERSION).append('\t')
                .append(platform).append('\n');
        for (MultiplayerMod mod : mods) {
            value.append(mod.id()).append('\t').append(mod.version()).append('\t')
                    .append(mod.mode().wireName()).append('\t')
                    .append(mod.protocol().isEmpty() ? "-" : mod.protocol()).append('\t')
                    .append(mod.syncHash().isEmpty() ? "-" : mod.syncHash()).append('\n');
        }
        return value.toString();
    }

    public String fingerprint() {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(encode().getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(64);
            for (byte value : digest) result.append(String.format("%02x", value & 0xff));
            return result.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    public static MultiplayerManifest decode(String encoded) {
        if (encoded == null || encoded.length() > 256 * 1024) {
            throw new IllegalArgumentException("Multiplayer manifest is missing or too large");
        }
        String[] lines = encoded.split("\\n", -1);
        if (lines.length < 2) throw new IllegalArgumentException("Malformed multiplayer manifest");
        String[] header = lines[0].split("\\t", -1);
        if (header.length != 2 || !WIRE_VERSION.equals(header[0])) {
            throw new IllegalArgumentException("Unsupported multiplayer manifest version");
        }
        List<MultiplayerMod> mods = new ArrayList<>();
        for (int index = 1; index < lines.length; index++) {
            if (lines[index].isEmpty()) continue;
            String[] fields = lines[index].split("\\t", -1);
            if (fields.length != 5) {
                throw new IllegalArgumentException("Malformed multiplayer mod row");
            }
            MultiplayerMod.Mode mode = MultiplayerMod.Mode.parse(fields[2]);
            mods.add(new MultiplayerMod(fields[0], fields[1], mode,
                    "-".equals(fields[3]) ? "" : fields[3],
                    "-".equals(fields[4]) ? "" : fields[4]));
        }
        return new MultiplayerManifest(header[1], mods);
    }
}
