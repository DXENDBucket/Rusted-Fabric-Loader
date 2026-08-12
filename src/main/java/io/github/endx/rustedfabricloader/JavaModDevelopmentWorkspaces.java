package io.github.endx.rustedfabricloader;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/** Discovers editable exploded Java mods without relying on platform symbolic-link behavior. */
final class JavaModDevelopmentWorkspaces {
    static final String DEV_DIR_PROPERTY = "rusted.javamodsDevDir";
    static final String RESOLVED_DEV_DIR_PROPERTY = "rusted.javamodsDevDir.resolved";
    static final String DEV_ENABLED_PROPERTY = "rusted.javamodsDevEnabled";
    static final String AUTO_RELOAD_PROPERTY = "rusted.javamodsDevAutoReload";
    static final String WORKSPACE_IDS_PROPERTY = "rusted.javamodsDevWorkspaceIds";
    static final String WORKSPACE_PROPERTY_PREFIX = "rusted.javamodsDevWorkspace.";
    private static final String METADATA = "fabric.mod.json";
    private static final String LINK_SUFFIX = ".link";
    private static final int MAX_LINK_BYTES = 16 * 1024;

    private JavaModDevelopmentWorkspaces() { }

    static Selection discover(Path gameDir, Path javaModsDir, boolean androidRuntime,
                              LogCategory logCategory) {
        Path devRoot = resolveDevRoot(gameDir);
        System.setProperty(RESOLVED_DEV_DIR_PROPERTY, devRoot.toString());
        if (System.getProperty(AUTO_RELOAD_PROPERTY) == null) {
            System.setProperty(AUTO_RELOAD_PROPERTY, androidRuntime ? "false" : "true");
        }
        boolean enabled = !"false".equalsIgnoreCase(
                System.getProperty(DEV_ENABLED_PROPERTY, "true").trim());
        if (enabled) {
            try {
                Files.createDirectories(devRoot);
            } catch (IOException failure) {
                Log.warn(logCategory, "Could not create Java mod development directory %s: %s",
                        devRoot, failure.toString());
                enabled = false;
            }
        }

        LinkedHashMap<String, Workspace> workspaces = enabled
                ? discoverWorkspaces(devRoot, logCategory)
                : new LinkedHashMap<String, Workspace>();
        publish(workspaces);
        List<Path> jars = discoverJars(javaModsDir, workspaces.keySet(), logCategory);
        ArrayList<Path> candidates = new ArrayList<Path>(jars.size() + workspaces.size());
        candidates.addAll(jars);
        for (Workspace workspace : workspaces.values()) candidates.add(workspace.root);
        return new Selection(devRoot, candidates, workspaces);
    }

    static Path writeCandidateList(Path gameDir, List<Path> candidates) throws IOException {
        Path state = gameDir.resolve(".rusted-fabric").toAbsolutePath().normalize();
        Files.createDirectories(state);
        Path output = state.resolve("java-mod-candidates.list");
        ArrayList<String> lines = new ArrayList<String>(candidates.size());
        for (Path candidate : candidates) {
            lines.add(candidate.toAbsolutePath().normalize().toString());
        }
        Files.write(output, lines, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
        return output;
    }

    private static Path resolveDevRoot(Path gameDir) {
        String configured = System.getProperty(DEV_DIR_PROPERTY, "").trim();
        Path path = configured.isEmpty() ? gameDir.resolve("javamods-dev")
                : Paths.get(configured);
        if (!path.isAbsolute()) path = gameDir.resolve(path);
        return path.toAbsolutePath().normalize();
    }

    private static LinkedHashMap<String, Workspace> discoverWorkspaces(
            Path devRoot, LogCategory category) {
        ArrayList<Path> entries = new ArrayList<Path>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(devRoot)) {
            for (Path entry : stream) entries.add(entry);
        } catch (IOException failure) {
            Log.warn(category, "Could not scan Java mod development directory %s: %s",
                    devRoot, failure.toString());
            return new LinkedHashMap<String, Workspace>();
        }
        Collections.sort(entries, Comparator.comparing(path ->
                path.getFileName().toString().toLowerCase(Locale.ROOT)));
        LinkedHashMap<String, Workspace> result = new LinkedHashMap<String, Workspace>();
        LinkedHashSet<Path> roots = new LinkedHashSet<Path>();
        for (Path entry : entries) {
            Path root = null;
            boolean linked = false;
            try {
                if (Files.isDirectory(entry, LinkOption.NOFOLLOW_LINKS)
                        && !Files.isSymbolicLink(entry)) {
                    root = entry.toRealPath();
                } else if (Files.isRegularFile(entry, LinkOption.NOFOLLOW_LINKS)
                        && !Files.isSymbolicLink(entry)
                        && entry.getFileName().toString().toLowerCase(Locale.ROOT)
                        .endsWith(LINK_SUFFIX)) {
                    root = readLink(entry);
                    linked = true;
                }
                if (root == null || !roots.add(root)) continue;
                String id = readDirectoryModId(root);
                if (id == null) {
                    Log.warn(category, "Ignoring Java mod workspace without valid %s: %s",
                            METADATA, root);
                    continue;
                }
                Workspace previous = result.putIfAbsent(id, new Workspace(id, root, linked));
                if (previous != null) {
                    Log.warn(category, "Ignoring duplicate Java mod workspace ID %s at %s; using %s",
                            id, root, previous.root);
                }
            } catch (IOException | RuntimeException failure) {
                Log.warn(category, "Ignoring invalid Java mod workspace entry %s: %s",
                        entry, failure.toString());
            }
        }
        return result;
    }

    private static Path readLink(Path link) throws IOException {
        long size = Files.size(link);
        if (size <= 0L || size > MAX_LINK_BYTES) {
            throw new IOException("workspace link must contain at most " + MAX_LINK_BYTES + " bytes");
        }
        String selected = null;
        try (BufferedReader reader = Files.newBufferedReader(link, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String value = line.trim();
                if (value.startsWith("\ufeff")) value = value.substring(1).trim();
                if (value.isEmpty() || value.startsWith("#")) continue;
                if (selected != null) throw new IOException("workspace link contains multiple paths");
                selected = value;
            }
        }
        if (selected == null) throw new IOException("workspace link is empty");
        Path target = Paths.get(selected);
        if (!target.isAbsolute()) target = link.getParent().resolve(target);
        target = target.toAbsolutePath().normalize();
        if (!Files.isDirectory(target)) throw new IOException("workspace target is not a directory");
        return target.toRealPath();
    }

    private static List<Path> discoverJars(Path javaModsDir, Set<String> overriddenIds,
                                            LogCategory category) {
        if (javaModsDir == null || !Files.isDirectory(javaModsDir)) {
            return Collections.emptyList();
        }
        ArrayList<Path> result = new ArrayList<Path>();
        try (Stream<Path> paths = Files.walk(javaModsDir)) {
            paths.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
                            && !Files.isSymbolicLink(path)
                            && path.getFileName().toString().toLowerCase(Locale.ROOT)
                            .endsWith(".jar"))
                    .forEach(path -> {
                        String id = readJarModId(path, category);
                        if (id == null || !overriddenIds.contains(id)) {
                            result.add(path.toAbsolutePath().normalize());
                        } else {
                            Log.info(category, "Development workspace %s overrides %s", id, path);
                        }
                    });
        } catch (IOException failure) {
            Log.warn(category, "Could not scan Java mods from %s: %s",
                    javaModsDir, failure.toString());
        }
        Collections.sort(result, Comparator.comparing(Path::toString,
                String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    private static String readDirectoryModId(Path root) throws IOException {
        Path metadata = root.resolve(METADATA).normalize();
        if (!metadata.startsWith(root) || !Files.isRegularFile(metadata, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(metadata)) return null;
        try (BufferedReader reader = Files.newBufferedReader(metadata, StandardCharsets.UTF_8)) {
            return metadataId(new JsonParser().parse(reader));
        }
    }

    private static String readJarModId(Path jarPath, LogCategory category) {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            JarEntry entry = jar.getJarEntry(METADATA);
            if (entry == null) return null;
            try (InputStreamReader reader = new InputStreamReader(
                    jar.getInputStream(entry), StandardCharsets.UTF_8)) {
                return metadataId(new JsonParser().parse(reader));
            }
        } catch (IOException | RuntimeException failure) {
            Log.warn(category, "Could not read Java mod metadata from %s: %s",
                    jarPath, failure.toString());
            return null;
        }
    }

    private static String metadataId(JsonElement parsed) {
        if (parsed == null || !parsed.isJsonObject()) return null;
        JsonObject object = parsed.getAsJsonObject();
        JsonElement raw = object.get("id");
        if (raw == null || !raw.isJsonPrimitive()) return null;
        String id = raw.getAsString().trim().toLowerCase(Locale.ROOT);
        return id.matches("[a-z][a-z0-9_-]{1,63}") ? id : null;
    }

    private static void publish(Map<String, Workspace> workspaces) {
        String oldIds = System.getProperty(WORKSPACE_IDS_PROPERTY, "");
        for (String id : oldIds.split(",")) {
            if (!id.isEmpty()) System.clearProperty(WORKSPACE_PROPERTY_PREFIX + id);
        }
        StringBuilder ids = new StringBuilder();
        for (Workspace workspace : workspaces.values()) {
            if (ids.length() > 0) ids.append(',');
            ids.append(workspace.id);
            System.setProperty(WORKSPACE_PROPERTY_PREFIX + workspace.id,
                    workspace.root.toString());
        }
        System.setProperty(WORKSPACE_IDS_PROPERTY, ids.toString());
    }

    static final class Workspace {
        final String id;
        final Path root;
        final boolean linked;

        Workspace(String id, Path root, boolean linked) {
            this.id = id;
            this.root = root;
            this.linked = linked;
        }
    }

    static final class Selection {
        final Path devRoot;
        final List<Path> candidates;
        final Map<String, Workspace> workspaces;

        Selection(Path devRoot, List<Path> candidates, Map<String, Workspace> workspaces) {
            this.devRoot = devRoot;
            this.candidates = Collections.unmodifiableList(new ArrayList<Path>(candidates));
            this.workspaces = Collections.unmodifiableMap(
                    new LinkedHashMap<String, Workspace>(workspaces));
        }
    }
}
