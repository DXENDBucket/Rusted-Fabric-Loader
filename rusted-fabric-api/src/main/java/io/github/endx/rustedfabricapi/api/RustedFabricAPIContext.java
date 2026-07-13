package io.github.endx.rustedfabricapi.api;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class RustedFabricAPIContext {
    private final Map<String, Object> raw;

    public RustedFabricAPIContext(Map<String, Object> raw) {
        Objects.requireNonNull(raw, "raw");
        Map<String, Object> copy = new HashMap<String, Object>(raw);
        Object args = copy.get(RustedFabricAPIKeys.K_GAME_ARGS);
        if (args instanceof String[]) {
            copy.put(RustedFabricAPIKeys.K_GAME_ARGS, ((String[]) args).clone());
        }
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
        return (String) raw.get(RustedFabricAPIKeys.K_RUNTIME_NAMESPACE);
    }

    public String entrypointKey() {
        return (String) raw.get(RustedFabricAPIKeys.K_ENTRYPOINT_KEY);
    }

    public boolean androidRuntime() {
        Object v = raw.get(RustedFabricAPIKeys.K_ANDROID);
        return v instanceof Boolean && (Boolean) v;
    }

    public Map<String, Object> asMap() {
        return raw;
    }

    private String stringValue(String key) {
        Object value = raw.get(key);
        return value != null ? value.toString() : "";
    }
}
