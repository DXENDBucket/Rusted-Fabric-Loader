package io.github.endx.rustedfabric.android.jvm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Validates game files supplied by the user without accepting an Android APK. */
public final class DesktopGameLayout {
    public static final String GAME_JAR = "game-lib.jar";
    public static final String DESKTOP_MAIN_CLASS = "com.corrodinggames.rts.java.Main";
    public static final String FABRIC_MAIN_CLASS = "net.fabricmc.loader.impl.launch.knot.KnotClient";
    private static final String DESKTOP_MAIN_ENTRY = "com/corrodinggames/rts/java/Main.class";
    private static final List<String> REQUIRED_DIRECTORIES =
            Collections.unmodifiableList(Arrays.asList("assets", "res", "libs"));
    private static final List<String> REQUIRED_LIBRARY_NAMES =
            Collections.unmodifiableList(Arrays.asList("lwjgl.jar", "slick.jar", "jinput.jar"));

    private DesktopGameLayout() {
    }

    public static DesktopGameInspection inspect(Path selectedRoot) {
        if (selectedRoot == null) throw new IllegalArgumentException("selectedRoot must not be null");
        Path root = selectedRoot.toAbsolutePath().normalize();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        if (!Files.isDirectory(root)) {
            errors.add("Selected desktop game root is not a directory");
            return new DesktopGameInspection(root, errors, warnings);
        }
        Path gameJar = root.resolve(GAME_JAR);
        if (!Files.isRegularFile(gameJar)) {
            errors.add("Missing " + GAME_JAR);
        } else {
            verifyGameJar(gameJar, errors);
        }
        for (String directory : REQUIRED_DIRECTORIES) {
            if (!Files.isDirectory(root.resolve(directory))) {
                errors.add("Missing directory: " + directory);
            }
        }
        Path libraries = root.resolve("libs");
        if (Files.isDirectory(libraries)) {
            for (String library : REQUIRED_LIBRARY_NAMES) {
                if (!Files.isRegularFile(libraries.resolve(library))) {
                    errors.add("Missing desktop library: libs/" + library);
                }
            }
        }
        if (containsWindowsNative(root)) {
            warnings.add("Windows native libraries are source artifacts only and will not be loaded on Android");
        }
        return new DesktopGameInspection(root, errors, warnings);
    }

    public static List<Path> desktopClasspath(Path root) throws IOException {
        DesktopGameInspection inspection = inspect(root);
        if (!inspection.isImportable()) {
            throw new IOException("Desktop game layout is incomplete: " + inspection.errors());
        }
        List<Path> result = new ArrayList<>();
        result.add(inspection.root().resolve(GAME_JAR));
        try (Stream<Path> entries = Files.list(inspection.root().resolve("libs"))) {
            result.addAll(entries.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)
                            .endsWith(".jar"))
                    .sorted()
                    .collect(Collectors.toList()));
        }
        return Collections.unmodifiableList(result);
    }

    public static List<String> importRoots() {
        return Collections.unmodifiableList(Arrays.asList(GAME_JAR, "assets", "res", "libs", "font"));
    }

    private static void verifyGameJar(Path gameJar, List<String> errors) {
        try (ZipFile zip = new ZipFile(gameJar.toFile())) {
            ZipEntry main = zip.getEntry(DESKTOP_MAIN_ENTRY);
            if (main == null || main.isDirectory()) {
                errors.add(GAME_JAR + " does not contain the desktop main class");
            }
        } catch (IOException invalid) {
            errors.add(GAME_JAR + " is not a readable JAR: " + invalid.getMessage());
        }
    }

    private static boolean containsWindowsNative(Path root) {
        try (Stream<Path> entries = Files.list(root)) {
            return entries.anyMatch(path -> Files.isRegularFile(path)
                    && path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".dll"));
        } catch (IOException ignored) {
            return false;
        }
    }
}
