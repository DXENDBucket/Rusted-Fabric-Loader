package io.github.endx.rustedfabric.android.jvm;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Safe private-storage content library shared by the Android launcher UI and tests. */
public final class ManagedContentLibrary {
    private static final String MARKER = ".rusted-fabric-managed.properties";
    private static final int MAX_ARCHIVE_FILES = 20_000;
    private static final int MAX_ARCHIVE_DEPTH = 24;
    private static final long MAX_ARCHIVE_BYTES = 536_870_912L;
    private static final long MAX_JAVA_MOD_BYTES = 268_435_456L;
    private static final Set<String> OFFICIAL_JAVA_IDS;

    static {
        Set<String> ids = new HashSet<>();
        ids.add("rusted_fabric_api");
        ids.add("java_mod_menu");
        ids.add("ini_essentials");
        OFFICIAL_JAVA_IDS = Collections.unmodifiableSet(ids);
    }

    private ManagedContentLibrary() {
    }

    public enum Kind {
        INI_MOD,
        MAP,
        JAVA_MOD
    }

    public static void prepare(Path gameRoot) throws IOException {
        requireGameRoot(gameRoot);
        for (Kind kind : Kind.values()) Files.createDirectories(enabledRoot(gameRoot, kind));
        Files.createDirectories(disabledRoot(gameRoot, Kind.MAP));
        Files.createDirectories(disabledRoot(gameRoot, Kind.JAVA_MOD));
    }

    public static List<Item> list(Path gameRoot, Kind kind) throws IOException {
        requireKind(kind);
        prepare(gameRoot);
        List<Item> result = new ArrayList<>();
        scanRoot(enabledRoot(gameRoot, kind), kind, true, result);
        if (kind != Kind.INI_MOD) {
            scanRoot(disabledRoot(gameRoot, kind), kind, false, result);
        }
        result.sort(Comparator.comparing(Item::official).reversed()
                .thenComparing(Item::name, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(item -> item.path().toString()));
        return Collections.unmodifiableList(result);
    }

    public static Item importContent(Path gameRoot, Kind kind, Path source,
                                     String displayName) throws IOException {
        requireKind(kind);
        prepare(gameRoot);
        Path checkedSource = requireRegularFile(source, "import source");
        if (kind == Kind.JAVA_MOD) {
            JavaMetadata metadata = readJavaMetadata(checkedSource);
            if (OFFICIAL_JAVA_IDS.contains(metadata.id)) {
                throw new IOException("The bundled official mod cannot be replaced by an import: "
                        + metadata.id);
            }
            Path root = enabledRoot(gameRoot, kind);
            Path target = root.resolve(
                    safeFilePart(metadata.id) + "-" + safeFilePart(metadata.version) + ".jar");
            Path candidate = stageCopy(checkedSource, root, MAX_JAVA_MOD_BYTES);
            try {
                removeJavaModById(gameRoot, metadata.id);
                Files.move(candidate, target, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(candidate);
            }
            return javaItem(target, true);
        }

        String chosenName = cleanDisplayName(displayName, checkedSource.getFileName().toString());
        Path root = enabledRoot(gameRoot, kind);
        if (isZip(checkedSource)) {
            Path target = uniqueChild(root, safeFilePart(stripArchiveSuffix(chosenName)));
            Path staging = root.resolve(".importing-" + UUID.randomUUID());
            Files.createDirectories(staging);
            boolean success = false;
            try {
                extractZip(checkedSource, staging);
                flattenSingleWrapper(staging);
                if (!containsUsableContent(staging, kind)) {
                    throw new IOException(kind == Kind.MAP
                            ? "The archive contains no .tmx map"
                            : "The archive contains no INI mod files");
                }
                writeMarker(staging, chosenName, kind);
                Files.move(staging, target);
                success = true;
                return contentItem(target, kind, true);
            } finally {
                if (!success) deleteRecursively(staging, root);
            }
        }

        String lower = checkedSource.getFileName().toString().toLowerCase(Locale.ROOT);
        if (kind == Kind.MAP && !lower.endsWith(".tmx")) {
            throw new IOException("A map import must be a .tmx file or ZIP archive");
        }
        if (kind == Kind.INI_MOD && !lower.endsWith(".ini")) {
            throw new IOException("An INI mod import must be an .ini file, .rwmod, or ZIP archive");
        }
        Path target = uniqueChild(root, safeFilePart(checkedSource.getFileName().toString()));
        copyAtomically(checkedSource, target, MAX_ARCHIVE_BYTES);
        return contentItem(target, kind, true);
    }

    /** Installs or updates one APK-owned official Java mod while preserving its current state. */
    public static Item provisionOfficialJavaMod(Path gameRoot, Path source, String expectedId,
                                                boolean defaultEnabled, boolean locked)
            throws IOException {
        prepare(gameRoot);
        JavaMetadata metadata = readJavaMetadata(source);
        if (!metadata.id.equals(expectedId) || !OFFICIAL_JAVA_IDS.contains(expectedId)) {
            throw new IOException("Unexpected bundled official mod: " + metadata.id);
        }
        List<Item> current = findJavaModsById(gameRoot, expectedId);
        boolean enabled = locked || (current.isEmpty() ? defaultEnabled
                : current.stream().anyMatch(Item::enabled));
        Path root = enabled ? enabledRoot(gameRoot, Kind.JAVA_MOD)
                : disabledRoot(gameRoot, Kind.JAVA_MOD);
        Path target = root.resolve("official-" + expectedId + ".jar");
        Path candidate = stageCopy(source, root, MAX_JAVA_MOD_BYTES);
        try {
            for (Item item : current) Files.deleteIfExists(item.path());
            Files.move(candidate, target, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(candidate);
        }
        return javaItem(target, enabled);
    }

    public static Item setEnabled(Path gameRoot, Item item, boolean enabled) throws IOException {
        requireOwnedItem(gameRoot, item);
        if (item.kind() == Kind.INI_MOD) {
            throw new IOException("INI mods are enabled through the in-game mod menu");
        }
        if (item.locked()) throw new IOException("This core component must remain enabled");
        if (item.enabled() == enabled) return item;
        Path destinationRoot = enabled ? enabledRoot(gameRoot, item.kind())
                : disabledRoot(gameRoot, item.kind());
        Files.createDirectories(destinationRoot);
        Path target = destinationRoot.resolve(item.path().getFileName());
        if (Files.exists(target)) throw new IOException("A content item with this name already exists");
        Files.move(item.path(), target);
        return item.kind() == Kind.JAVA_MOD ? javaItem(target, enabled)
                : contentItem(target, item.kind(), enabled);
    }

    public static void delete(Path gameRoot, Item item) throws IOException {
        requireOwnedItem(gameRoot, item);
        if (item.official()) throw new IOException("Bundled official mods cannot be removed");
        deleteRecursively(item.path(), item.path().getParent());
    }

    public static JavaMetadata readJavaMetadata(Path jar) throws IOException {
        Path checked = requireRegularFile(jar, "Java mod");
        if (Files.size(checked) > MAX_JAVA_MOD_BYTES) throw new IOException("Java mod is too large");
        try (JarFile archive = new JarFile(checked.toFile())) {
            JarEntry entry = archive.getJarEntry("fabric.mod.json");
            if (entry == null || entry.isDirectory()) {
                throw new IOException("Java mod does not contain fabric.mod.json");
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    archive.getInputStream(entry), StandardCharsets.UTF_8))) {
                JsonElement parsed = JsonParser.parseReader(reader);
                if (!parsed.isJsonObject()) throw new IOException("fabric.mod.json is not an object");
                JsonObject json = parsed.getAsJsonObject();
                String id = requiredString(json, "id");
                if (!id.matches("[a-z][a-z0-9_-]{0,63}")) {
                    throw new IOException("Invalid Java mod id: " + id);
                }
                String version = requiredString(json, "version");
                String name = optionalString(json, "name", id);
                return new JavaMetadata(id, name, version);
            }
        } catch (RuntimeException malformed) {
            throw new IOException("Could not parse fabric.mod.json", malformed);
        }
    }

    private static void scanRoot(Path root, Kind kind, boolean enabled, List<Item> result)
            throws IOException {
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(root)) {
            for (Path path : entries) {
                String fileName = path.getFileName().toString();
                if (fileName.startsWith(".")) continue;
                if (kind == Kind.JAVA_MOD) {
                    if (Files.isRegularFile(path)
                            && fileName.toLowerCase(Locale.ROOT).endsWith(".jar")) {
                        try {
                            result.add(javaItem(path, enabled));
                        } catch (IOException malformed) {
                            result.add(new Item(kind, fileName, malformed.getMessage(), "", "",
                                    enabled, false, false, path));
                        }
                    }
                } else if (Files.isDirectory(path) || Files.isRegularFile(path)
                        && acceptedContentFile(kind, fileName)) {
                    result.add(contentItem(path, kind, enabled));
                }
            }
        }
    }

    private static Item javaItem(Path path, boolean enabled) throws IOException {
        JavaMetadata metadata = readJavaMetadata(path);
        boolean official = OFFICIAL_JAVA_IDS.contains(metadata.id);
        boolean locked = "rusted_fabric_api".equals(metadata.id);
        return new Item(Kind.JAVA_MOD, metadata.name,
                "ID: " + metadata.id + " · " + metadata.version,
                metadata.id, metadata.version, enabled, official, locked, path);
    }

    private static Item contentItem(Path path, Kind kind, boolean enabled) throws IOException {
        String name = readMarkerName(path);
        if (name.isEmpty()) name = path.getFileName().toString();
        String detail = Files.isDirectory(path) ? "folder" : path.getFileName().toString();
        return new Item(kind, name, detail, "", "", enabled, false, false, path);
    }

    private static List<Item> findJavaModsById(Path gameRoot, String id) throws IOException {
        List<Item> result = new ArrayList<>();
        for (Item item : list(gameRoot, Kind.JAVA_MOD)) {
            if (id.equals(item.id())) result.add(item);
        }
        return result;
    }

    private static void removeJavaModById(Path gameRoot, String id) throws IOException {
        List<Item> existing = findJavaModsById(gameRoot, id);
        if (existing.stream().anyMatch(Item::official)) {
            throw new IOException("Official Java mod id is reserved: " + id);
        }
        for (Item item : existing) Files.deleteIfExists(item.path());
    }

    private static void copyAtomically(Path source, Path target, long maximumBytes)
            throws IOException {
        if (Files.size(source) > maximumBytes) throw new IOException("Imported file is too large");
        Files.createDirectories(target.getParent());
        Path staging = target.getParent().resolve(".importing-" + UUID.randomUUID());
        boolean success = false;
        try {
            Files.copy(source, staging, StandardCopyOption.REPLACE_EXISTING);
            Files.move(staging, target, StandardCopyOption.REPLACE_EXISTING);
            success = true;
        } finally {
            if (!success) Files.deleteIfExists(staging);
        }
    }

    private static Path stageCopy(Path source, Path root, long maximumBytes) throws IOException {
        Path candidate = root.resolve(".candidate-" + UUID.randomUUID() + ".jar");
        copyAtomically(source, candidate, maximumBytes);
        return candidate;
    }

    private static void extractZip(Path archive, Path destination) throws IOException {
        int files = 0;
        long bytes = 0L;
        try (InputStream raw = Files.newInputStream(archive);
             ZipInputStream zip = new ZipInputStream(new BufferedInputStream(raw))) {
            ZipEntry entry;
            byte[] buffer = new byte[64 * 1024];
            while ((entry = zip.getNextEntry()) != null) {
                String rawName = entry.getName().replace('\\', '/');
                if (rawName.isEmpty()) continue;
                Path target = destination.resolve(rawName).normalize();
                if (!target.startsWith(destination) || target.equals(destination)) {
                    throw new IOException("Archive path traversal was rejected");
                }
                if (destination.relativize(target).getNameCount() > MAX_ARCHIVE_DEPTH) {
                    throw new IOException("Archive is too deeply nested");
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                    continue;
                }
                if (++files > MAX_ARCHIVE_FILES) throw new IOException("Archive has too many files");
                Files.createDirectories(target.getParent());
                try (java.io.OutputStream output = Files.newOutputStream(target)) {
                    int read;
                    while ((read = zip.read(buffer)) >= 0) {
                        bytes += read;
                        if (bytes > MAX_ARCHIVE_BYTES) {
                            throw new IOException("Expanded archive is too large");
                        }
                        output.write(buffer, 0, read);
                    }
                }
            }
        }
    }

    private static boolean containsUsableContent(Path root, Kind kind) throws IOException {
        String suffix = kind == Kind.MAP ? ".tmx" : ".ini";
        try (java.util.stream.Stream<Path> paths = Files.walk(root)) {
            return paths.anyMatch(path -> Files.isRegularFile(path)
                    && path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(suffix));
        }
    }

    private static void flattenSingleWrapper(Path root) throws IOException {
        List<Path> children = new ArrayList<>();
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(root)) {
            for (Path entry : entries) children.add(entry);
        }
        if (children.size() != 1 || !Files.isDirectory(children.get(0))) return;
        Path wrapper = children.get(0);
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(wrapper)) {
            for (Path entry : entries) Files.move(entry, root.resolve(entry.getFileName()));
        }
        Files.delete(wrapper);
    }

    private static void writeMarker(Path directory, String name, Kind kind) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("name", name);
        properties.setProperty("kind", kind.name());
        try (java.io.OutputStream output = Files.newOutputStream(directory.resolve(MARKER))) {
            properties.store(output, "Rusted Fabric managed content");
        }
    }

    private static String readMarkerName(Path path) {
        if (!Files.isDirectory(path)) return "";
        Path marker = path.resolve(MARKER);
        if (!Files.isRegularFile(marker)) return "";
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(marker)) {
            properties.load(input);
            return properties.getProperty("name", "").trim();
        } catch (IOException ignored) {
            return "";
        }
    }

    private static boolean isZip(Path source) throws IOException {
        try (InputStream input = new BufferedInputStream(Files.newInputStream(source))) {
            int first = input.read();
            int second = input.read();
            return first == 'P' && second == 'K';
        }
    }

    private static boolean acceptedContentFile(Kind kind, String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (kind == Kind.MAP) return lower.endsWith(".tmx") || lower.endsWith(".rwmap");
        return lower.endsWith(".ini") || lower.endsWith(".rwmod");
    }

    private static Path uniqueChild(Path root, String requested) throws IOException {
        String safe = requested.isEmpty() ? "imported-content" : requested;
        Path candidate = root.resolve(safe);
        int suffix = 2;
        while (Files.exists(candidate)) candidate = root.resolve(safe + "-" + suffix++);
        return candidate;
    }

    private static String cleanDisplayName(String displayName, String fallback) {
        String value = displayName == null ? "" : displayName.trim();
        return value.isEmpty() ? fallback : value;
    }

    private static String stripArchiveSuffix(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        for (String suffix : new String[]{".zip", ".rwmod", ".rwmap"}) {
            if (lower.endsWith(suffix)) return name.substring(0, name.length() - suffix.length());
        }
        return name;
    }

    private static String safeFilePart(String raw) {
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < raw.length(); index++) {
            char character = raw.charAt(index);
            if (Character.isLetterOrDigit(character) || character == '-' || character == '_'
                    || character == '.') result.append(character);
            else if (result.length() == 0 || result.charAt(result.length() - 1) != '-') result.append('-');
        }
        String value = result.toString();
        while (value.startsWith(".")) value = value.substring(1);
        return value.isEmpty() ? "content" : value;
    }

    private static Path enabledRoot(Path gameRoot, Kind kind) {
        switch (kind) {
            case INI_MOD: return gameRoot.resolve("mods/units");
            case MAP: return gameRoot.resolve("mods/maps");
            case JAVA_MOD: return gameRoot.resolve("javamods");
            default: throw new IllegalArgumentException("Unknown content kind");
        }
    }

    private static Path disabledRoot(Path gameRoot, Kind kind) {
        switch (kind) {
            case MAP: return gameRoot.resolve("mods-disabled/maps");
            case JAVA_MOD: return gameRoot.resolve("javamods-disabled");
            default: throw new IllegalArgumentException("INI mods use the in-game mod menu");
        }
    }

    private static void requireOwnedItem(Path gameRoot, Item item) throws IOException {
        if (item == null) throw new IllegalArgumentException("item must not be null");
        Path root = gameRoot.toAbsolutePath().normalize();
        Path path = item.path().toAbsolutePath().normalize();
        boolean allowed = path.getParent() != null && (path.getParent().equals(
                enabledRoot(root, item.kind()).toAbsolutePath().normalize())
                || (item.kind() != Kind.INI_MOD && path.getParent().equals(
                disabledRoot(root, item.kind()).toAbsolutePath().normalize())));
        if (!allowed || !Files.exists(path)) throw new IOException("Content item is no longer available");
    }

    private static void requireGameRoot(Path gameRoot) throws IOException {
        if (gameRoot == null || !Files.isDirectory(gameRoot.toAbsolutePath().normalize())) {
            throw new IOException("Desktop game root is not available");
        }
    }

    private static Path requireRegularFile(Path path, String label) throws IOException {
        if (path == null || !Files.isRegularFile(path.toAbsolutePath().normalize())) {
            throw new IOException(label + " is not a readable file");
        }
        return path.toAbsolutePath().normalize();
    }

    private static void requireKind(Kind kind) {
        if (kind == null) throw new IllegalArgumentException("kind must not be null");
    }

    private static String requiredString(JsonObject json, String key) throws IOException {
        String value = optionalString(json, key, "").trim();
        if (value.isEmpty()) throw new IOException("fabric.mod.json is missing " + key);
        return value;
    }

    private static String optionalString(JsonObject json, String key, String fallback) {
        JsonElement value = json.get(key);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                ? value.getAsString() : fallback;
    }

    private static void deleteRecursively(Path target, Path boundary) throws IOException {
        if (!Files.exists(target)) return;
        Path checkedBoundary = boundary.toAbsolutePath().normalize();
        Path checkedTarget = target.toAbsolutePath().normalize();
        if (!checkedTarget.startsWith(checkedBoundary) || checkedTarget.equals(checkedBoundary)) {
            throw new IOException("Refusing to delete outside the managed content directory");
        }
        try (java.util.stream.Stream<Path> entries = Files.walk(checkedTarget)) {
            try {
                entries.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException failure) {
                        throw new DeleteFailure(failure);
                    }
                });
            } catch (DeleteFailure failure) {
                throw failure.cause;
            }
        }
    }

    public static final class JavaMetadata {
        private final String id;
        private final String name;
        private final String version;

        JavaMetadata(String id, String name, String version) {
            this.id = id;
            this.name = name;
            this.version = version;
        }

        public String id() { return id; }
        public String name() { return name; }
        public String version() { return version; }
    }

    public static final class Item {
        private final Kind kind;
        private final String name;
        private final String detail;
        private final String id;
        private final String version;
        private final boolean enabled;
        private final boolean official;
        private final boolean locked;
        private final Path path;

        Item(Kind kind, String name, String detail, String id, String version,
             boolean enabled, boolean official, boolean locked, Path path) {
            this.kind = kind;
            this.name = name;
            this.detail = detail;
            this.id = id;
            this.version = version;
            this.enabled = enabled;
            this.official = official;
            this.locked = locked;
            this.path = path;
        }

        public Kind kind() { return kind; }
        public String name() { return name; }
        public String detail() { return detail; }
        public String id() { return id; }
        public String version() { return version; }
        public boolean enabled() { return enabled; }
        public boolean official() { return official; }
        public boolean locked() { return locked; }
        public Path path() { return path; }
    }

    private static final class DeleteFailure extends RuntimeException {
        final IOException cause;
        DeleteFailure(IOException cause) { super(cause); this.cause = cause; }
    }
}
