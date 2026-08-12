package io.github.endx.rustedfabricapi.api.development;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Read-only view of exploded Java-mod workspaces selected by Rusted Fabric Loader. */
public final class DevelopmentWorkspaces {
    private static final String ROOT_PROPERTY = "rusted.javamodsDevDir.resolved";
    private static final String IDS_PROPERTY = "rusted.javamodsDevWorkspaceIds";
    private static final String WORKSPACE_PREFIX = "rusted.javamodsDevWorkspace.";
    private static final String NATIVE_CONTENT_PREFIX =
            "rusted.javamodsDevNativeContent.";
    private static final String NATIVE_CONTENT_TARGET_PREFIX =
            "rusted.javamodsDevNativeContentTarget.";
    private static final String AUTO_RELOAD_PROPERTY = "rusted.javamodsDevAutoReload";

    private DevelopmentWorkspaces() { }

    public static Optional<Path> root() { return pathProperty(ROOT_PROPERTY); }

    public static Optional<Path> forMod(String modId) {
        String checked = checkModId(modId);
        return pathProperty(WORKSPACE_PREFIX + checked);
    }

    /** Source directory containing editable native INI content declared by this workspace. */
    public static Optional<Path> nativeContentForMod(String modId) {
        return pathProperty(NATIVE_CONTENT_PREFIX + checkModId(modId));
    }

    /** Loader-managed directory currently exposed to the game's native mod scanner. */
    public static Optional<Path> stagedNativeContentForMod(String modId) {
        return pathProperty(NATIVE_CONTENT_TARGET_PREFIX + checkModId(modId));
    }

    /** Deterministic ID-to-directory snapshot of workspaces loaded for this process. */
    public static Map<String, Path> loaded() {
        LinkedHashMap<String, Path> result = new LinkedHashMap<String, Path>();
        String raw = System.getProperty(IDS_PROPERTY, "");
        for (String candidate : raw.split(",")) {
            String id = candidate.trim();
            if (id.isEmpty()) continue;
            Optional<Path> path = pathProperty(WORKSPACE_PREFIX + id);
            if (path.isPresent()) result.put(id, path.get());
        }
        return Collections.unmodifiableMap(result);
    }

    /** Android defaults this to false so shared-storage polling never enters the frame loop. */
    public static boolean automaticReloadEnabled() {
        return Boolean.parseBoolean(System.getProperty(AUTO_RELOAD_PROPERTY, "false"));
    }

    private static Optional<Path> pathProperty(String key) {
        String value = System.getProperty(key, "").trim();
        if (value.isEmpty()) return Optional.empty();
        try {
            Path path = Paths.get(value).toAbsolutePath().normalize();
            return Files.isDirectory(path) ? Optional.of(path) : Optional.empty();
        } catch (RuntimeException invalid) {
            return Optional.empty();
        }
    }

    private static String checkModId(String modId) {
        if (modId == null) throw new NullPointerException("modId");
        String id = modId.trim().toLowerCase(java.util.Locale.ROOT);
        if (!id.matches("[a-z][a-z0-9_-]{1,63}")) {
            throw new IllegalArgumentException("Invalid Java mod ID: " + modId);
        }
        return id;
    }
}
