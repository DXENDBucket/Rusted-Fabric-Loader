package io.github.endx.rustedfabricapi.api;

import java.nio.file.Path;
import java.util.Map;

public final class RustedFabricAPIContext {
    private final Map<String, Object> raw;

    public RustedFabricAPIContext(Map<String, Object> raw) {
        this.raw = raw;
    }

    public Path gameDir() {
        return (Path) raw.get(RustedFabricAPIKeys.K_GAME_DIR);
    }

    public Path gameJar() {
        return (Path) raw.get(RustedFabricAPIKeys.K_GAME_JAR);
    }

    public String[] gameArgs() {
        return (String[]) raw.get(RustedFabricAPIKeys.K_GAME_ARGS);
    }

    public String runtimeNamespace() {
        return (String) raw.get(RustedFabricAPIKeys.K_RUNTIME_NAMESPACE);
    }

    public boolean androidRuntime() {
        Object v = raw.get(RustedFabricAPIKeys.K_ANDROID);
        return v instanceof Boolean && (Boolean) v;
    }
}
