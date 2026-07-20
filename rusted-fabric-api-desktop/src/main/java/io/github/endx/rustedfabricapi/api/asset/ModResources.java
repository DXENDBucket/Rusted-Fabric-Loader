package io.github.endx.rustedfabricapi.api.asset;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

/** Factories and safety policy for resources bundled in ordinary mod Jars. */
public final class ModResources {
    public static final int MAX_READ_BYTES = 128 * 1024 * 1024;

    private ModResources() {
    }

    public static ModResourcePack forMod(String modId) {
        String checked = validateModId(modId);
        ModContainer container = FabricLoader.getInstance().getModContainer(checked)
                .orElseThrow(() -> new IllegalArgumentException("Unknown Fabric mod: " + checked));
        return new ModResourcePack(checked, path -> {
            Optional<Path> found = container.findPath(path.toString().replace('\\', '/'));
            return found.isPresent()
                    ? Optional.of(java.nio.file.Files.newInputStream(found.get()))
                    : Optional.empty();
        }, prefix -> discover(container, prefix));
    }

    /** Useful for libraries, tests, and mods that intentionally anchor resources to one class loader. */
    public static ModResourcePack forClass(String modId, Class<?> anchor) {
        String checked = validateModId(modId);
        Class<?> type = java.util.Objects.requireNonNull(anchor, "anchor");
        return new ModResourcePack(checked, path -> {
            String name = path.toString().replace('\\', '/');
            ClassLoader loader = type.getClassLoader();
            InputStream input = loader != null ? loader.getResourceAsStream(name)
                    : type.getResourceAsStream('/' + name);
            return Optional.ofNullable(input);
        }, null);
    }

    /** Read-only directory-backed pack for development assets and generated test fixtures. */
    public static ModResourcePack forDirectory(String modId, Path root) {
        String checked = validateModId(modId);
        Path requested = java.util.Objects.requireNonNull(root, "root").toAbsolutePath().normalize();
        if (!Files.isDirectory(requested)) {
            throw new IllegalArgumentException("Resource-pack directory does not exist: " + requested);
        }
        final Path realRoot;
        try {
            realRoot = requested.toRealPath();
        } catch (java.io.IOException failure) {
            throw new IllegalArgumentException("Could not resolve resource-pack directory", failure);
        }
        return new ModResourcePack(checked, path -> {
            Path candidate = realRoot.resolve(path.toString()).normalize();
            if (!candidate.startsWith(realRoot) || !Files.isRegularFile(candidate)) {
                return Optional.empty();
            }
            Path real = candidate.toRealPath();
            if (!real.startsWith(realRoot) || !Files.isRegularFile(real)) return Optional.empty();
            return Optional.of(Files.newInputStream(real));
        }, prefix -> {
            LinkedHashSet<String> names = new LinkedHashSet<String>();
            discoverRoot(realRoot, prefix, names);
            return toPaths(names);
        });
    }

    public static Path cacheRoot() {
        return Path.of(System.getProperty("java.io.tmpdir"), "rusted-fabric-loader",
                "resource-cache").toAbsolutePath().normalize();
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
                throw new IllegalArgumentException("Invalid modId character: " + c);
            }
        }
        return value;
    }

    static Path validatePath(String relativePath) {
        if (relativePath == null) throw new NullPointerException("relativePath");
        String value = relativePath.trim().replace('\\', '/');
        if (value.isEmpty() || value.length() > 1024 || value.startsWith("/")) {
            throw new IllegalArgumentException("Resource path must be a non-empty relative path");
        }
        String[] segments = value.split("/", -1);
        for (String segment : segments) {
            if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)) {
                throw new IllegalArgumentException("Invalid resource path segment");
            }
            for (int i = 0; i < segment.length(); i++) {
                char c = segment.charAt(i);
                if (Character.isISOControl(c) || c == ':') {
                    throw new IllegalArgumentException("Unsupported resource path character");
                }
            }
        }
        Path path;
        try {
            path = Path.of(value).normalize();
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid resource path", exception);
        }
        if (path.isAbsolute() || path.startsWith("..")) {
            throw new IllegalArgumentException("Resource path escaped its pack");
        }
        return path;
    }

    private static List<Path> discover(ModContainer container, Path prefix) throws java.io.IOException {
        Set<String> names = new LinkedHashSet<String>();
        for (Path root : container.getRootPaths()) {
            Path normalizedRoot = root.toAbsolutePath().normalize();
            discoverRoot(normalizedRoot, prefix, names);
        }
        return toPaths(names);
    }

    private static void discoverRoot(Path root, Path prefix, Set<String> names)
            throws java.io.IOException {
        Path base = root.resolve(prefix.toString()).normalize();
        if (!base.startsWith(root) || !Files.isDirectory(base)) return;
        try (Stream<Path> paths = Files.walk(base)) {
            paths.filter(path -> Files.isRegularFile(path) && !Files.isSymbolicLink(path))
                    .forEach(path -> names.add(root.relativize(
                            path.toAbsolutePath().normalize()).toString().replace('\\', '/')));
        }
    }

    private static List<Path> toPaths(Set<String> names) {
        ArrayList<String> sorted = new ArrayList<String>(names);
        java.util.Collections.sort(sorted);
        ArrayList<Path> result = new ArrayList<Path>(sorted.size());
        for (String name : sorted) result.add(Path.of(name));
        return result;
    }
}
