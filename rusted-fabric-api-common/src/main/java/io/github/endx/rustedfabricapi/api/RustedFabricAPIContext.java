package io.github.endx.rustedfabricapi.api;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Optional;

import io.github.endx.rustedfabricapi.api.multiplayer.MultiplayerManifest;

/** Immutable context shared by Windows Fabric mods and Android DEX mods. */
public final class RustedFabricAPIContext {
    private final Map<String, Object> raw;

    public RustedFabricAPIContext(Map<String, Object> raw) {
        Objects.requireNonNull(raw, "raw");
        Map<String, Object> copy = new HashMap<String, Object>(raw);
        Object args = copy.get(RustedFabricAPIKeys.K_GAME_ARGS);
        if (args instanceof String[]) {
            copy.put(RustedFabricAPIKeys.K_GAME_ARGS, ((String[]) args).clone());
        }
        copy.put(RustedFabricAPIKeys.K_CAPABILITIES,
                immutableStrings(copy.get(RustedFabricAPIKeys.K_CAPABILITIES)));
        this.raw = Collections.unmodifiableMap(copy);
    }

    public int contextVersion() {
        Object value = raw.get(RustedFabricAPIKeys.K_CONTEXT_VERSION);
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    public String loaderVersion() {
        return stringValue(RustedFabricAPIKeys.K_LOADER_VERSION);
    }

    public String gameVersion() {
        return stringValue(RustedFabricAPIKeys.K_GAME_VERSION);
    }

    public String mappingsVersion() {
        return stringValue(RustedFabricAPIKeys.K_MAPPINGS_VERSION);
    }

    public String mappingProfileId() {
        return stringValue(RustedFabricAPIKeys.K_MAPPING_PROFILE_ID);
    }

    public RustedFabricPlatform platform() {
        Object legacy = raw.get(RustedFabricAPIKeys.K_ANDROID);
        boolean android = legacy instanceof Boolean && (Boolean) legacy;
        return RustedFabricPlatform.parse(raw.get(RustedFabricAPIKeys.K_PLATFORM), android);
    }

    @SuppressWarnings("unchecked")
    public Set<String> capabilities() {
        return (Set<String>) raw.get(RustedFabricAPIKeys.K_CAPABILITIES);
    }

    public boolean hasCapability(String capability) {
        return capability != null && capabilities().contains(capability);
    }

    public String packageName() {
        return stringValue(RustedFabricAPIKeys.K_PACKAGE_NAME);
    }

    public String processName() {
        return stringValue(RustedFabricAPIKeys.K_PROCESS_NAME);
    }

    public Path gameDir() {
        return (Path) raw.get(RustedFabricAPIKeys.K_GAME_DIR);
    }

    public Path gameJar() {
        return (Path) raw.get(RustedFabricAPIKeys.K_GAME_JAR);
    }

    public String[] gameArgs() {
        String[] args = (String[]) raw.get(RustedFabricAPIKeys.K_GAME_ARGS);
        return args != null ? args.clone() : new String[0];
    }

    public String runtimeNamespace() {
        return stringValue(RustedFabricAPIKeys.K_RUNTIME_NAMESPACE);
    }

    public String entrypointKey() {
        return stringValue(RustedFabricAPIKeys.K_ENTRYPOINT_KEY);
    }

    /** Enabled-mod compatibility data; the same wire format is used on Windows and Android. */
    public Optional<MultiplayerManifest> multiplayerManifest() {
        Object encoded = raw.get(RustedFabricAPIKeys.K_MULTIPLAYER_MANIFEST);
        if (!(encoded instanceof String) || ((String) encoded).isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(MultiplayerManifest.decode((String) encoded));
        } catch (IllegalArgumentException invalid) {
            return Optional.empty();
        }
    }

    public boolean androidRuntime() {
        return platform() == RustedFabricPlatform.ANDROID;
    }

    public Map<String, Object> asMap() {
        return raw;
    }

    private String stringValue(String key) {
        Object value = raw.get(key);
        return value != null ? value.toString() : "";
    }

    private static Set<String> immutableStrings(Object value) {
        LinkedHashSet<String> strings = new LinkedHashSet<String>();
        if (value instanceof Collection<?>) {
            for (Object item : (Collection<?>) value) {
                if (item != null) {
                    strings.add(item.toString());
                }
            }
        } else if (value instanceof String[]) {
            Collections.addAll(strings, (String[]) value);
        }
        return Collections.unmodifiableSet(strings);
    }
}
