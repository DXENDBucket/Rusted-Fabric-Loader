package org.lwjgl.system;

import java.nio.file.Path;
import java.nio.file.Paths;

/** Maps the desktop game's mod paths to Android's directly accessible public workspace. */
public final class RustedFabricStorage {
    private static final String CONTENT_ROOT_PROPERTY = "rusted.android.contentRoot";

    private RustedFabricStorage() {
    }

    public static String remap(String path) {
        if (path == null) return null;
        String configured = System.getProperty(CONTENT_ROOT_PROPERTY, "").trim();
        if (configured.isEmpty()) return path;

        String normalized = path.replace('\\', '/');
        Match match = match(normalized);
        if (match == null) return path;

        Path root = Paths.get(configured).toAbsolutePath().normalize();
        Path category = root.resolve(match.directory).normalize();
        Path resolved = match.suffix.isEmpty()
                ? category : category.resolve(match.suffix).normalize();
        if (!resolved.startsWith(category)) {
            throw new IllegalArgumentException("Android shared-content path escaped its root");
        }
        return resolved.toString();
    }

    private static Match match(String path) {
        Match units = match(path, "mods/units", "units");
        if (units != null) return units;
        Match maps = match(path, "mods/maps", "maps");
        if (maps != null) return maps;

        units = match(path, "/SD/mods/units", "units");
        if (units != null) return units;
        maps = match(path, "/SD/mods/maps", "maps");
        if (maps != null) return maps;
        maps = match(path, "/SD/rusted_warfare_maps", "maps");
        if (maps != null) return maps;
        maps = match(path, "/SD/rustedWarfare/maps", "maps");
        if (maps != null) return maps;
        return match(path, "/SD/rustedWarfare/units", "units");
    }

    private static Match match(String path, String prefix, String directory) {
        if (path.equals(prefix)) return new Match(directory, "");
        String childPrefix = prefix + "/";
        if (!path.startsWith(childPrefix)) return null;
        return new Match(directory, path.substring(childPrefix.length()));
    }

    private static final class Match {
        final String directory;
        final String suffix;

        Match(String directory, String suffix) {
            this.directory = directory;
            this.suffix = suffix;
        }
    }
}
