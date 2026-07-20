package io.github.endx.rustedfabricapi.api.config;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;
import java.util.Locale;

/** Factory and path policy for mod-owned configuration files. */
public final class ModConfigFiles {
    public static final int MAX_READ_BYTES = 8 * 1024 * 1024;

    private ModConfigFiles() {
    }

    public static Path root() {
        return FabricLoader.getInstance().getConfigDir().toAbsolutePath().normalize();
    }

    public static Path directory(String modId) {
        return root().resolve(validateModId(modId)).normalize();
    }

    public static ModConfigFile file(String modId, String relativePath) {
        return new ModConfigFile(validateModId(modId), validateRelativePath(relativePath));
    }

    static String validateModId(String modId) {
        if (modId == null) throw new NullPointerException("modId");
        String value = modId.trim().toLowerCase(Locale.ROOT);
        if (value.length() < 2 || value.length() > 64) {
            throw new IllegalArgumentException("modId length must be between 2 and 64 characters");
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!(c >= 'a' && c <= 'z') && !(c >= '0' && c <= '9')
                    && c != '_' && c != '-') {
                throw new IllegalArgumentException("modId must use lowercase letters, digits, '_' or '-'");
            }
        }
        return value;
    }

    static Path validateRelativePath(String relativePath) {
        if (relativePath == null) throw new NullPointerException("relativePath");
        String value = relativePath.trim().replace('\\', '/');
        if (value.isEmpty() || value.length() > 240 || value.startsWith("/")) {
            throw new IllegalArgumentException("Configuration path must be a non-empty relative path");
        }
        String[] rawSegments = value.split("/", -1);
        for (String segment : rawSegments) {
            if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)) {
                throw new IllegalArgumentException("Invalid configuration path segment");
            }
        }
        Path path;
        try {
            path = Path.of(value).normalize();
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid configuration path", exception);
        }
        if (path.isAbsolute() || path.getNameCount() == 0 || path.startsWith("..")) {
            throw new IllegalArgumentException("Configuration path escapes the mod directory");
        }
        for (Path segment : path) {
            String name = segment.toString();
            if (name.isEmpty() || ".".equals(name) || "..".equals(name)) {
                throw new IllegalArgumentException("Invalid configuration path segment");
            }
            if (name.endsWith(".") || name.endsWith(" ") || isWindowsDeviceName(name)) {
                throw new IllegalArgumentException("Unsupported configuration path segment: " + name);
            }
            for (int i = 0; i < name.length(); i++) {
                char c = name.charAt(i);
                if (c < 32 || "<>:\"|?*".indexOf(c) >= 0) {
                    throw new IllegalArgumentException("Unsupported character in configuration path");
                }
            }
        }
        return path;
    }

    private static boolean isWindowsDeviceName(String name) {
        String upper = name.toUpperCase(Locale.ROOT);
        int dot = upper.indexOf('.');
        String stem = dot >= 0 ? upper.substring(0, dot) : upper;
        if ("CON".equals(stem) || "PRN".equals(stem) || "AUX".equals(stem)
                || "NUL".equals(stem)) return true;
        if (stem.length() == 4 && (stem.startsWith("COM") || stem.startsWith("LPT"))) {
            char number = stem.charAt(3);
            return number >= '1' && number <= '9';
        }
        return false;
    }
}
