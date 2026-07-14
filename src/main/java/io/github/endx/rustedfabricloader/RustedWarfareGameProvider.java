package io.github.endx.rustedfabricloader;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.fabricmc.loader.impl.game.GameProvider;
import net.fabricmc.loader.impl.game.patch.GameTransformer;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.metadata.BuiltinModMetadata;
import net.fabricmc.loader.impl.util.Arguments;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

/**
 * Fabric GameProvider for Rusted Warfare.
 */
public class RustedWarfareGameProvider implements GameProvider {

    private static final LogCategory LOG_CATEGORY = LogCategory.create("GameProvider", "RustedWarfare");

    private static final String FABRIC_ADD_MODS = "fabric.addMods";
    private static final String FABRIC_RUNTIME_MAPPING_NAMESPACE = "fabric.runtimeMappingNamespace";
    private static final String GAME_VERSION_PROPERTY = "rusted.gameVersion";
    private static final String DEFAULT_GAME_VERSION = "1.15";
    private static final String OFFICIAL_NAMESPACE = "official";
    private static final String NAMED_NAMESPACE = "named";

    // Optional: -Drusted.devNamed=true
    private static final String DEV_NAMED_PROPERTY = "rusted.devNamed";

    // Optional: -Drusted.gameJar=...
    private static final String GAME_JAR_PROPERTY = "rusted.gameJar";

    // Optional: -Drusted.namedGameJar=...
    private static final String NAMED_GAME_JAR_PROPERTY = "rusted.namedGameJar";

    // Optional: -Drusted.javamodsDir=...
    private static final String JAVA_MODS_DIR_PROPERTY = "rusted.javamodsDir";
    private static final String DEFAULT_JAVA_MODS_DIR_NAME = "javamods";

    // Optional: -Drusted.gameDir=...
    private static final String GAME_DIR_PROPERTY = "rusted.gameDir";

    private static final String GAME_LIB_JAR_NAME = "game-lib.jar";
    private static final String NAMED_GAME_LIB_JAR_NAME = "game-lib-named.jar";
    private static final String LIBS_DIR_NAME = "libs";
    private static final String ANDROID_JAR_NAME = "android.jar";

    private static final String OFFICIAL_ENTRYPOINT = "com.corrodinggames.rts.java.Main";
    private static final String NAMED_ENTRYPOINT = "rustedwarfare.client.RustedWarfareMain";

    // Desktop filter: remove Android SDK stubs that shadow JRE modules (java.xml)
    private static final String[] ANDROID_JAR_EXCLUDE_PREFIXES = new String[] {
            "javax/xml/",
            "org/w3c/",
            "org/xml/"
    };

    private final GameTransformer transformer = new GameTransformer() {
        @Override
        public byte[] transform(String className) {
            return null;
        }
    };

    private Arguments loaderArgs;
    private String[] gameArgs;

    private Path gameDir;
    private Path gameLibJar;
    private Path libsDir;

    @Override
    public String getGameId() {
        return "rusted_warfare";
    }

    @Override
    public String getGameName() {
        return "Rusted Warfare";
    }

    @Override
    public String getRawGameVersion() {
        return System.getProperty(GAME_VERSION_PROPERTY,
                BUILD_PROPERTIES.getProperty("gameVersion", DEFAULT_GAME_VERSION));
    }

    @Override
    public String getNormalizedGameVersion() {
        return getRawGameVersion();
    }

    @Override
    public String getRuntimeNamespace(String defaultNamespace) {
        return getRequestedRuntimeNamespace();
    }

    @Override
    public String getDefaultModDistributionNamespace(String defaultNamespace) {
        return getRequestedRuntimeNamespace();
    }

    @Override
    public Collection<BuiltinMod> getBuiltinMods() {
        if (gameLibJar == null) {
            return Collections.emptyList();
        }

        BuiltinModMetadata.Builder metadata = new BuiltinModMetadata.Builder(getGameId(), getNormalizedGameVersion())
                .setName(getGameName())
                .setDescription("Rusted Warfare game runtime provided by Rusted Fabric Loader");
        return Collections.singletonList(new BuiltinMod(
                Collections.singletonList(gameLibJar),
                metadata.build()));
    }

    @Override
    public String getEntrypoint() {
        return isNamedRuntimeRequested() ? NAMED_ENTRYPOINT : OFFICIAL_ENTRYPOINT;
    }

    @Override
    public Path getLaunchDirectory() {
        return gameDir;
    }

    @Override
    public boolean requiresUrlClassLoader() {
        return false;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public Set<BuiltinTransform> getBuiltinTransforms(String className) {
        return Collections.emptySet();
    }

    @Override
    public boolean locateGame(FabricLauncher launcher, String[] args) {
        this.loaderArgs = new Arguments();
        this.loaderArgs.parse(args != null ? args : new String[0]);
        this.gameArgs = (args != null) ? args.clone() : new String[0];

        // Resolve gameDir
        String override = System.getProperty(GAME_DIR_PROPERTY);
        if (override != null && !override.isEmpty()) {
            Path p = Paths.get(override);
            this.gameDir = (p.isAbsolute() ? p : Paths.get(".").resolve(p)).toAbsolutePath().normalize();
        } else {
            this.gameDir = Paths.get(".").toAbsolutePath().normalize();
        }

        this.gameLibJar = resolveGameLibJar();
        this.libsDir = gameDir.resolve(LIBS_DIR_NAME).toAbsolutePath().normalize();

        configureFabricModDirs();
        return true;
    }

    @Override
    public void initialize(FabricLauncher launcher) {
        // no-op
    }

    private Path resolveGameLibJar() {
        if (isNamedRuntimeRequested()) {
            Path override = resolvePathProperty(NAMED_GAME_JAR_PROPERTY, gameDir);
            if (override != null) {
                return override;
            }

            Path[] candidates = new Path[] {
                    gameDir.resolve(NAMED_GAME_LIB_JAR_NAME),
                    gameDir.resolve(LIBS_DIR_NAME).resolve(NAMED_GAME_LIB_JAR_NAME),
                    gameDir.resolve(".rustedfabricloader").resolve("dev").resolve(NAMED_GAME_LIB_JAR_NAME),
                    Paths.get("build").resolve("rusted-dev").resolve(NAMED_GAME_LIB_JAR_NAME)
            };

            return firstExistingOrFirst(candidates);
        }

        Path override = resolvePathProperty(GAME_JAR_PROPERTY, gameDir);
        if (override != null) {
            return override;
        }

        return firstExistingOrFirst(new Path[] {
                gameDir.resolve(GAME_LIB_JAR_NAME),
                gameDir.resolve(LIBS_DIR_NAME).resolve(GAME_LIB_JAR_NAME)
        });
    }

    private static Path firstExistingOrFirst(Path[] candidates) {
        Path first = null;

        for (Path candidate : candidates) {
            if (candidate == null) {
                continue;
            }

            Path normalized = candidate.toAbsolutePath().normalize();
            if (first == null) {
                first = normalized;
            }
            if (Files.isRegularFile(normalized)) {
                return normalized;
            }
        }

        return first;
    }

    private static Path resolvePathProperty(String propertyName, Path baseDir) {
        String value = System.getProperty(propertyName);
        if (value == null || value.isEmpty()) {
            return null;
        }

        Path path = Paths.get(value);
        if (!path.isAbsolute() && baseDir != null) {
            path = baseDir.resolve(path);
        }
        return path.toAbsolutePath().normalize();
    }

    @Override
    public GameTransformer getEntrypointTransformer() {
        return transformer;
    }
    @Override
    public void unlockClassPath(FabricLauncher launcher) {
        try {
            List<Path> ordered = buildOrderedGameClasspath();

            int count = 0;

            for (Path p : ordered) {
                launcher.addToClassPath(p);
                count++;
            }
            Log.info(LOG_CATEGORY, "Injected " + count + " classpath entries.");
        } catch (IOException e) {
            throw new RuntimeException("Failed to build Rusted Warfare classpath", e);
        }

        /*
        net.fabricmc.loader.api.FabricLoader.getInstance()
                .invokeEntrypoints("rustedfabricloader:classpath_ready", Runnable.class, Runnable::run);

         */

    }

    private List<Path> buildOrderedGameClasspath() throws IOException {
        List<Path> out = new ArrayList<Path>();
        HashSet<Path> seen = new HashSet<Path>();

        // 1) game-lib.jar first
        addIfExists(out, seen, gameLibJar);
        if (gameLibJar == null || !Files.isRegularFile(gameLibJar)) {
            Log.error(LOG_CATEGORY, "Missing game jar: " + gameLibJar);
        }

        // 2) libs/*.jar (sorted)
        List<Path> libs = new ArrayList<Path>();
        if (libsDir == null || !Files.isDirectory(libsDir)) {
            Log.error(LOG_CATEGORY, "Missing libs directory: " + libsDir);
        } else {

            List<Path> tmp = new ArrayList<Path>();

            java.util.stream.Stream<Path> stream = null;
            try {
                stream = Files.list(libsDir);
                for (Path p : (Iterable<Path>) stream::iterator) {
                    String name = p.getFileName().toString().toLowerCase(Locale.ROOT);
                    if (name.equalsIgnoreCase(GAME_LIB_JAR_NAME) || name.equalsIgnoreCase(NAMED_GAME_LIB_JAR_NAME)) {
                        continue;
                    }
                    if (name.endsWith(".jar")) {
                        tmp.add(p.toAbsolutePath().normalize());
                    }
                }
            } finally {
                if (stream != null) {
                    try { stream.close(); } catch (Throwable ignored) {}
                }
            }

            tmp.sort(new Comparator<Path>() {
                @Override
                public int compare(Path a, Path b) {
                    return a.getFileName().toString().compareToIgnoreCase(b.getFileName().toString());
                }
            });

            libs = tmp;
        }

        Path androidJar = null;
        for (Path p : libs) {
            String name = p.getFileName().toString();
            if (name.equalsIgnoreCase(ANDROID_JAR_NAME)) {
                androidJar = p;
            } else {
                addIfExists(out, seen, p);
            }
        }

        // 3) android.jar last (desktop only)
        if (isAndroidRuntime()) {
            Log.info(LOG_CATEGORY, "Android runtime detected; skipping desktop android.jar handling.");
        } else {
            Path desktopAndroid = prepareDesktopAndroidJar(androidJar);
            addIfExists(out, seen, desktopAndroid);
        }

        return out;
    }

    private static void addIfExists(List<Path> out, Set<Path> seen, Path p) {
        if (p == null) return;
        if (!Files.exists(p)) return;
        if (seen.add(p)) out.add(p);
    }

    /**
     * Desktop: generate a filtered copy of android.jar under .rustedfabricloader/cache/
     * so that javax.xml / org.w3c / org.xml stubs won't shadow JRE's java.xml module.
     *
     * Does NOT modify the original android.jar, so you can still distribute it.
     */
    private Path prepareDesktopAndroidJar(Path originalAndroidJar) throws IOException {
        if (originalAndroidJar == null) return null;
        if (!Files.isRegularFile(originalAndroidJar)) return originalAndroidJar;

        if (!jarContainsAnyPrefix(originalAndroidJar, ANDROID_JAR_EXCLUDE_PREFIXES)) {
            // No conflicting stubs found; use as-is.
            return originalAndroidJar;
        }

        Path cacheDir = gameDir.resolve(".rustedfabricloader").resolve("cache").toAbsolutePath().normalize();
        Files.createDirectories(cacheDir);

        long size = Files.size(originalAndroidJar);
        long mtime = Files.getLastModifiedTime(originalAndroidJar).toMillis();
        String outName = "android-desktop-" + size + "-" + mtime + ".jar";
        Path outJar = cacheDir.resolve(outName);

        if (Files.isRegularFile(outJar)) {
            Log.info(LOG_CATEGORY, "Using cached filtered android.jar: " + outJar);
            return outJar;
        }

        try {
            // Build filtered jar
            JarFile jf = null;
            JarOutputStream jos = null;
            OutputStream fout = null;

            HashSet<String> writtenDirs = new HashSet<String>();
            byte[] buf = new byte[8192];

            try {
                jf = new JarFile(originalAndroidJar.toFile(), false);

                fout = new BufferedOutputStream(Files.newOutputStream(outJar));
                jos = new JarOutputStream(fout);

                Enumeration<JarEntry> en = jf.entries();
                while (en.hasMoreElements()) {
                    JarEntry e = en.nextElement();
                    String name = e.getName();

                    if (name == null || name.isEmpty()) continue;
                    if (e.isDirectory()) continue;

                    if (isSignatureFile(name)) continue;

                    if (shouldExcludeFromDesktopAndroidJar(name)) continue;

                    int slash = name.lastIndexOf('/');
                    if (slash > 0) {
                        String dir = name.substring(0, slash + 1);
                        ensureDirEntry(jos, writtenDirs, dir);
                    }

                    JarEntry ne = new JarEntry(name);
                    ne.setTime(e.getTime());
                    jos.putNextEntry(ne);

                    InputStream is = null;
                    try {
                        is = new BufferedInputStream(jf.getInputStream(e));
                        copyStream(is, jos, buf);
                    } finally {
                        if (is != null) try { is.close(); } catch (IOException ignored) {}
                    }

                    jos.closeEntry();
                }
            } finally {
                if (jos != null) try { jos.close(); } catch (IOException ignored) {}
                if (fout != null) try { fout.close(); } catch (IOException ignored) {}
                if (jf != null) try { jf.close(); } catch (IOException ignored) {}
            }

        } catch (Throwable t) {
            // Fallback to original android.jar if filtering fails for any reason.
            try { Files.deleteIfExists(outJar); } catch (Throwable ignored) {}
            Log.warn(LOG_CATEGORY, "Failed to create filtered android.jar, using original: " + originalAndroidJar, t);
            // cause logged above
            return originalAndroidJar;
        }
        Log.info(LOG_CATEGORY, "Created filtered android.jar: " + outJar);
        return outJar;
    }

    private static void ensureDirEntry(JarOutputStream jos, HashSet<String> writtenDirs, String dirName) throws IOException {
        if (dirName == null || dirName.isEmpty()) return;
        String n = dirName.endsWith("/") ? dirName : (dirName + "/");
        if (writtenDirs.add(n)) {
            JarEntry de = new JarEntry(n);
            jos.putNextEntry(de);
            jos.closeEntry();
        }
    }

    private static boolean jarContainsAnyPrefix(Path jar, String[] prefixes) {
        JarFile jf = null;
        try {
            jf = new JarFile(jar.toFile(), false);
            Enumeration<JarEntry> en = jf.entries();
            while (en.hasMoreElements()) {
                JarEntry e = en.nextElement();
                String name = e.getName();
                if (name == null) continue;
                for (int i = 0; i < prefixes.length; i++) {
                    if (name.startsWith(prefixes[i])) return true;
                }
            }
        } catch (Throwable t) {
            // ignore, fall back to filtering
            return true;
        } finally {
            if (jf != null) try { jf.close(); } catch (IOException ignored) {}
        }
        return false;
    }

    private static boolean shouldExcludeFromDesktopAndroidJar(String entryName) {
        for (int i = 0; i < ANDROID_JAR_EXCLUDE_PREFIXES.length; i++) {
            if (entryName.startsWith(ANDROID_JAR_EXCLUDE_PREFIXES[i])) return true;
        }
        return false;
    }

    private static boolean isSignatureFile(String entryName) {
        String upper = entryName.toUpperCase(Locale.ROOT);
        if (!upper.startsWith("META-INF/")) return false;
        return upper.endsWith(".SF") || upper.endsWith(".RSA") || upper.endsWith(".DSA");
    }

    private static void copyStream(InputStream in, OutputStream out, byte[] buf) throws IOException {
        int r;
        while ((r = in.read(buf)) >= 0) {
            if (r == 0) continue;
            out.write(buf, 0, r);
        }
    }

    private static boolean isAndroidRuntime() {
        String vmName = String.valueOf(System.getProperty("java.vm.name", "")).toLowerCase(Locale.ROOT);
        String runtimeName = String.valueOf(System.getProperty("java.runtime.name", "")).toLowerCase(Locale.ROOT);
        return vmName.contains("dalvik") || runtimeName.contains("android");
    }

    private static final Properties BUILD_PROPERTIES = loadBuildProperties();

    private static Properties loadBuildProperties() {
        Properties properties = new Properties();
        try (InputStream input = RustedWarfareGameProvider.class.getResourceAsStream("/rusted-fabric-loader.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException e) {
            Log.warn(LOG_CATEGORY, "Unable to read Rusted Fabric Loader build metadata", e);
        }
        return properties;
    }

    private static String getRequestedRuntimeNamespace() {
        return isNamedRuntimeRequested() ? NAMED_NAMESPACE : OFFICIAL_NAMESPACE;
    }

    private static boolean isNamedRuntimeRequested() {
        String fabricRuntimeNamespace = System.getProperty(FABRIC_RUNTIME_MAPPING_NAMESPACE);
        if (NAMED_NAMESPACE.equals(fabricRuntimeNamespace)) {
            return true;
        }

        if (Boolean.getBoolean(DEV_NAMED_PROPERTY)) {
            return true;
        }

        String namedGameJar = System.getProperty(NAMED_GAME_JAR_PROPERTY);
        return namedGameJar != null && !namedGameJar.isEmpty();
    }

    @Override
    public void launch(ClassLoader loader) {
        try {
            Thread.currentThread().setContextClassLoader(loader);



            // entrypoints
            FabricLoader fl = net.fabricmc.loader.api.FabricLoader.getInstance();

            // fl.invokeEntrypoints("rustedfabricloader:before_game", Runnable.class, Runnable::run);

            runRustedFabricAPIStage("rustedfabricloader:classpath_ready");

            fl.invokeEntrypoints("main", net.fabricmc.api.ModInitializer.class, net.fabricmc.api.ModInitializer::onInitialize);
            fl.invokeEntrypoints("client", net.fabricmc.api.ClientModInitializer.class, net.fabricmc.api.ClientModInitializer::onInitializeClient);

            runRustedFabricAPIStage("rustedfabricloader:before_game");

            // launch
            Class<?> mainClass = loader.loadClass(getEntrypoint());
            Method main = mainClass.getMethod("main", String[].class);
            String[] launchArgs = (gameArgs != null) ? gameArgs : new String[0];
            main.invoke(null, (Object) launchArgs);
        } catch (Exception e) {
            throw new RuntimeException("Failed to launch Rusted Warfare via Fabric Loader", e);
        }
    }


    @Override
    public Arguments getArguments() {
        return loaderArgs;
    }

    @Override
    public String[] getLaunchArguments(boolean sanitize) {
        return (gameArgs != null) ? gameArgs.clone() : new String[0];
    }

    /**
     * Calculate javamods dir:
     * - -Drusted.javamodsDir=... overrides
     * - else <gameDir>/javamods
     */
    private Path resolveJavaModsDir() {
        String override = System.getProperty(JAVA_MODS_DIR_PROPERTY);

        if (override != null && !override.isEmpty()) {
            Path path = Paths.get(override);
            if (path.isAbsolute()) {
                return path.normalize();
            } else if (gameDir != null) {
                return gameDir.resolve(path).toAbsolutePath().normalize();
            } else {
                return path.toAbsolutePath().normalize();
            }
        }

        Path base = (gameDir != null) ? gameDir.resolve(DEFAULT_JAVA_MODS_DIR_NAME) : Paths.get(DEFAULT_JAVA_MODS_DIR_NAME);
        return base.toAbsolutePath().normalize();
    }

    /**
     * Add javamods dir to fabric.addMods so Fabric can discover mods there.
     */
    private void configureFabricModDirs() {
        Path javaModsDir = resolveJavaModsDir();
        if (javaModsDir == null || !Files.isDirectory(javaModsDir)) return;

        String existing = System.getProperty(FABRIC_ADD_MODS);
        String sep = File.pathSeparator;
        String newEntry = javaModsDir.toString();

        if (existing == null || existing.isEmpty()) {
            System.setProperty(FABRIC_ADD_MODS, newEntry);
        } else {
            if (!existing.contains(newEntry)) {
                System.setProperty(FABRIC_ADD_MODS, existing + sep + newEntry);
            }
        }
        Log.info(LOG_CATEGORY, "Extra Fabric mods from: " + javaModsDir);
    }

    @SuppressWarnings({"rawtypes","unchecked"})
    private void runRustedFabricAPIStage(String key) {
        Map<String, Object> ctx = new HashMap<>();

        ctx.put("rustedfabricapi.ctxVersion", 5);
        ctx.put("rustedfabricapi.loaderVersion", BUILD_PROPERTIES.getProperty("loaderVersion", ""));
        ctx.put("rustedfabricapi.gameVersion", getRawGameVersion());
        ctx.put("rustedfabricapi.mappingsVersion", BUILD_PROPERTIES.getProperty("mappingsVersion", ""));
        ctx.put("rustedfabricapi.mappingProfileId", "rw-pc-1.15-v1.1");
        ctx.put("rustedfabricapi.platform", isAndroidRuntime() ? "android" : "windows");
        ctx.put("rustedfabricapi.capabilities", Arrays.asList(
                "event.engine.init", "event.runtime.ready", "mapping.named",
                "session.v1", "multiplayer.compat.v1", "multiplayer.handshake.rfh1",
                "platform.windows.fabric"));
        ctx.put("rustedfabricapi.processName", "rusted-warfare-client");

        ctx.put("gameDir", gameDir);
        ctx.put("gameJar", gameLibJar);
        ctx.put("gameArgs", getLaunchArguments(false));
        ctx.put("androidRuntime", isAndroidRuntime());
        ctx.put("runtimeNamespace", getRequestedRuntimeNamespace());
        ctx.put("entrypointKey", key);
        ctx.put("rustedfabricapi.multiplayerManifest", buildMultiplayerManifest());

        FabricLoader.getInstance().invokeEntrypoints(
                key,
                Consumer.class,
                ep -> {
                    try {
                        ((Consumer<Map<String, Object>>) ep).accept(Collections.unmodifiableMap(ctx));
                    } catch (Throwable t) {
                        throw new RuntimeException("Entrypoint failed for key=" + key + " ep=" + ep.getClass().getName(), t);
                    }
                }
        );

    }

    /** Reads only mod metadata; platform binaries are deliberately excluded from the sync hash. */
    private String buildMultiplayerManifest() {
        Path directory = resolveJavaModsDir();
        if (directory == null || !Files.isDirectory(directory)) return "RFM1\twindows\n";
        SortedMap<String, MultiplayerRow> rows = new TreeMap<>();
        try (java.nio.file.DirectoryStream<Path> jars = Files.newDirectoryStream(directory, "*.jar")) {
            for (Path path : jars) {
                try (JarFile jar = new JarFile(path.toFile())) {
                    JarEntry entry = jar.getJarEntry("fabric.mod.json");
                    if (entry == null) continue;
                    JsonObject metadata;
                    try (InputStreamReader reader = new InputStreamReader(
                            jar.getInputStream(entry), StandardCharsets.UTF_8)) {
                        metadata = new JsonParser().parse(reader).getAsJsonObject();
                    }
                    String id = jsonString(metadata, "id");
                    String version = jsonString(metadata, "version");
                    if ("rustedfabricapi".equals(id)
                            || !id.matches("[a-z][a-z0-9_-]{0,63}")
                            || !safeToken(version, 64)) continue;
                    MultiplayerRow row = readMultiplayerRow(id, version, metadata);
                    rows.put(id, row);
                } catch (RuntimeException | IOException malformed) {
                    Log.warn(LOG_CATEGORY, "Could not read multiplayer metadata from %s", path);
                }
            }
        } catch (IOException unavailable) {
            Log.warn(LOG_CATEGORY, "Could not scan Java mods for multiplayer metadata");
        }
        StringBuilder result = new StringBuilder("RFM1\twindows\n");
        for (MultiplayerRow row : rows.values()) {
            result.append(row.id).append('\t').append(row.version).append('\t')
                    .append(row.mode).append('\t').append(row.protocol).append('\t')
                    .append(row.hash).append('\n');
        }
        return result.toString();
    }

    private static MultiplayerRow readMultiplayerRow(String id, String version,
                                                      JsonObject metadata) {
        JsonObject declaration = null;
        JsonElement custom = metadata.get("custom");
        if (custom != null && custom.isJsonObject()) {
            JsonElement value = custom.getAsJsonObject().get("rustedfabric:multiplayer");
            if (value != null && value.isJsonObject()) declaration = value.getAsJsonObject();
        }
        if (declaration == null) return new MultiplayerRow(id, version, "unsafe", "-", "-");
        String mode = jsonString(declaration, "mode");
        if ("client_only".equals(mode) || "server_only".equals(mode)
                || "optional".equals(mode)) {
            return new MultiplayerRow(id, version, mode, "-", "-");
        }
        if ("required".equals(mode)) {
            String protocol = jsonString(declaration, "protocol");
            String hash = jsonString(declaration, "syncHash").toLowerCase(Locale.ROOT);
            if (safeToken(protocol, 64) && hash.matches("[0-9a-f]{64}")) {
                return new MultiplayerRow(id, version, mode, protocol, hash);
            }
        }
        return new MultiplayerRow(id, version, "unsafe", "-", "-");
    }

    private static String jsonString(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                ? value.getAsString().trim() : "";
    }

    private static boolean safeToken(String value, int maximum) {
        return value != null && !value.isEmpty() && value.length() <= maximum
                && value.matches("[0-9A-Za-z][0-9A-Za-z._+\\-]*");
    }

    private static final class MultiplayerRow {
        final String id;
        final String version;
        final String mode;
        final String protocol;
        final String hash;

        MultiplayerRow(String id, String version, String mode, String protocol, String hash) {
            this.id = id;
            this.version = version;
            this.mode = mode;
            this.protocol = protocol;
            this.hash = hash;
        }
    }
}
