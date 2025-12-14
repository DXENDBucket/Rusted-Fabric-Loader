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

    public String[] gameArgs() {
        return (String[]) raw.get(RustedFabricAPIKeys.K_GAME_ARGS);
    }

    public boolean androidRuntime() {
        Object v = raw.get(RustedFabricAPIKeys.K_ANDROID);
        return v instanceof Boolean && (Boolean) v;
    }
}
