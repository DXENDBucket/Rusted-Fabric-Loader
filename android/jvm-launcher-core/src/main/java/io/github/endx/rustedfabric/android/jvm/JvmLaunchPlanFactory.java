package io.github.endx.rustedfabric.android.jvm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipFile;

/** Builds launch requests for the imported desktop game without packaging any game payload. */
public final class JvmLaunchPlanFactory {
    public static final String SMOKE_MAIN_CLASS =
            "io.github.endx.rustedfabric.android.jvm.JvmHostSmokeMain";
    public static final String LWJGL_SMOKE_MAIN_CLASS =
            "io.github.endx.rustedfabric.android.jvm.JvmLwjglSmokeMain";

    private JvmLaunchPlanFactory() {
    }

    public static JvmLaunchPlan create(Path gameRoot, Path runtimeHome, Path loaderClasspath,
                                       Path nativeLibraryDirectory,
                                       JvmBackendCapabilities capabilities,
                                       int maximumHeapMiB, int width, int height) throws IOException {
        if (capabilities == null || !capabilities.isLaunchReady()) {
            throw new IllegalStateException("JVM backend is incomplete: "
                    + (capabilities == null ? "capabilities unavailable" : capabilities.missing()));
        }
        return createGamePlan(gameRoot, runtimeHome, loaderClasspath, nativeLibraryDirectory,
                maximumHeapMiB, width, height);
    }

    /**
     * Runs the real Fabric/game entrypoint once Java, the JVM host and rendering are present.
     * Missing audio/input/Steam adapters are reported by the game as the next compatibility
     * failure instead of preventing development from reaching that code path.
     */
    public static JvmLaunchPlan createCompatibilityProbe(
            Path gameRoot, Path runtimeHome, Path loaderClasspath,
            Path nativeLibraryDirectory, JvmBackendCapabilities capabilities,
            int maximumHeapMiB, int width, int height) throws IOException {
        if (capabilities == null || !capabilities.hasJava17() || !capabilities.hasJvmHost()
                || !capabilities.hasLwjgl2()) {
            throw new IllegalStateException("JVM game probe requires Java, JVM host, and LWJGL2");
        }
        return createGamePlan(gameRoot, runtimeHome, loaderClasspath, nativeLibraryDirectory,
                maximumHeapMiB, width, height);
    }

    private static JvmLaunchPlan createGamePlan(
            Path gameRoot, Path runtimeHome, Path loaderClasspath,
            Path nativeLibraryDirectory, int maximumHeapMiB, int width, int height)
            throws IOException {
        if (maximumHeapMiB < 256 || maximumHeapMiB > 8192) {
            throw new IllegalArgumentException("maximumHeapMiB must be between 256 and 8192");
        }
        if (width < 320 || height < 240) {
            throw new IllegalArgumentException("launch surface is too small");
        }
        requireDirectory(runtimeHome, "runtimeHome");
        String runtimeIssue = JvmRuntimeProbe.runtimeIssue(runtimeHome);
        if (!runtimeIssue.isEmpty()) {
            throw new IOException("runtimeHome is not usable: " + runtimeIssue);
        }
        requireDirectory(loaderClasspath, "loaderClasspath");
        requireDirectory(nativeLibraryDirectory, "nativeLibraryDirectory");
        List<Path> classpath = new ArrayList<>();
        try (java.util.stream.Stream<Path> loaderEntries = Files.list(loaderClasspath)) {
            loaderEntries.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".jar"))
                    .sorted().forEach(classpath::add);
        }
        if (classpath.isEmpty()) {
            throw new IOException("Loader classpath contains no JAR files");
        }
        DesktopGameInspection game = DesktopGameLayout.inspect(gameRoot);
        if (!game.isImportable()) {
            throw new IOException("Desktop game layout is incomplete: " + game.errors());
        }
        Path launcherJar = findJarEntry(classpath,
                "net/fabricmc/loader/impl/launch/knot/KnotClient.class");
        if (launcherJar == null) {
            throw new IOException("Loader classpath does not contain Fabric KnotClient");
        }
        Path lwjglAdapter = findJarEntry(classpath, "org/lwjgl/opengl/Display.class");
        if (lwjglAdapter == null) {
            throw new IOException("Loader classpath does not contain the Android LWJGL adapter");
        }
        Path lwjglCompat = findNamedJar(classpath, "lwjgl2-compat");
        if (lwjglCompat == null) {
            throw new IOException("Loader classpath does not contain the LWJGL2 compatibility layer");
        }
        requireJarEntry(lwjglCompat, "org/lwjgl/Sys.class", "LWJGL2 compatibility layer");
        Path nativeDirectory = nativeLibraryDirectory.toAbsolutePath().normalize();
        Path temporaryDirectory = gameRoot.toAbsolutePath().normalize()
                .resolve(".rustedfabricloader").resolve("tmp");
        Files.createDirectories(temporaryDirectory);
        Path gl4es = nativeDirectory.resolve("libgl4es_114.so");
        if (!Files.isRegularFile(gl4es)) {
            throw new IOException("GL4ES is not packaged for this ABI");
        }
        List<String> vmArguments = Arrays.asList(
                "-XX:+UseSerialGC",
                "-Xmx" + maximumHeapMiB + "M",
                "-Dfile.encoding=UTF-8",
                "-Djava.io.tmpdir=" + temporaryDirectory,
                "-Djava.home=" + runtimeHome.toAbsolutePath().normalize(),
                "-Djava.library.path=" + nativeDirectory,
                "-Dorg.lwjgl.librarypath=" + nativeDirectory,
                "-Dorg.lwjgl.opengl.libname=" + gl4es,
                "-Dorg.lwjgl.util.Debug=true",
                "-Dorg.lwjgl.util.DebugLoader=true",
                "-Drustedfabric.platform=android-jvm",
                "-Drusted.gameDir=" + gameRoot.toAbsolutePath().normalize(),
                "-Drusted.android.lwjglJar=" + lwjglAdapter.toAbsolutePath().normalize(),
                "-Drusted.android.lwjglCompatJar="
                        + lwjglCompat.toAbsolutePath().normalize(),
                "--add-opens=java.base/java.lang=ALL-UNNAMED",
                "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
                "--add-opens=java.base/java.net=ALL-UNNAMED",
                "--add-opens=java.base/java.nio=ALL-UNNAMED",
                "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED");
        List<String> gameArguments = Arrays.asList(
                "-width", Integer.toString(width), "-height", Integer.toString(height));
        return new JvmLaunchPlan(gameRoot.toAbsolutePath().normalize(),
                runtimeHome.toAbsolutePath().normalize(),
                nativeLibraryDirectory.toAbsolutePath().normalize(), classpath, vmArguments,
                DesktopGameLayout.FABRIC_MAIN_CLASS, gameArguments);
    }

    private static Path findJarEntry(List<Path> jars, String entry) throws IOException {
        for (Path path : jars) {
            try (ZipFile jar = new ZipFile(path.toFile())) {
                if (jar.getEntry(entry) != null) return path;
            }
        }
        return null;
    }

    private static Path findNamedJar(List<Path> jars, String namePart) {
        for (Path path : jars) {
            if (path.getFileName().toString().toLowerCase(java.util.Locale.ROOT)
                    .contains(namePart.toLowerCase(java.util.Locale.ROOT))) {
                return path;
            }
        }
        return null;
    }

    public static JvmLaunchPlan createSmokeTest(Path runtimeHome, Path payloadJar,
                                                Path workingDirectory,
                                                Path nativeLibraryDirectory, Path resultFile)
            throws IOException {
        requireUsableRuntime(runtimeHome);
        requireDirectory(workingDirectory, "workingDirectory");
        requireDirectory(nativeLibraryDirectory, "nativeLibraryDirectory");
        if (payloadJar == null || !Files.isRegularFile(payloadJar)) {
            throw new IOException("JVM smoke-test payload JAR is unavailable");
        }
        try (ZipFile jar = new ZipFile(payloadJar.toFile())) {
            if (jar.getEntry(SMOKE_MAIN_CLASS.replace('.', '/') + ".class") == null) {
                throw new IOException("JVM smoke-test payload does not contain its main class");
            }
        }
        if (resultFile == null || resultFile.getParent() == null
                || !resultFile.toAbsolutePath().normalize().getParent()
                .equals(workingDirectory.toAbsolutePath().normalize())) {
            throw new IOException("JVM smoke-test result must remain in its working directory");
        }
        List<String> vmArguments = Arrays.asList(
                "-Xms16M", "-Xmx64M",
                "-Dfile.encoding=UTF-8",
                "-Djava.home=" + runtimeHome.toAbsolutePath().normalize(),
                "-Djava.library.path=" + nativeLibraryDirectory.toAbsolutePath().normalize(),
                "-Drustedfabric.platform=android-jvm-smoke");
        return new JvmLaunchPlan(workingDirectory.toAbsolutePath().normalize(),
                runtimeHome.toAbsolutePath().normalize(),
                nativeLibraryDirectory.toAbsolutePath().normalize(),
                Collections.singletonList(payloadJar.toAbsolutePath().normalize()), vmArguments,
                SMOKE_MAIN_CLASS,
                Collections.singletonList(resultFile.toAbsolutePath().normalize().toString()));
    }

    public static JvmLaunchPlan createLwjglSmokeTest(Path runtimeHome, Path payloadJar,
                                                     Path lwjglAdapterJar,
                                                     Path workingDirectory,
                                                     Path nativeLibraryDirectory,
                                                     Path resultFile) throws IOException {
        requireUsableRuntime(runtimeHome);
        requireDirectory(workingDirectory, "workingDirectory");
        requireDirectory(nativeLibraryDirectory, "nativeLibraryDirectory");
        requireJarEntry(payloadJar, LWJGL_SMOKE_MAIN_CLASS.replace('.', '/') + ".class",
                "LWJGL smoke-test payload");
        requireJarEntry(lwjglAdapterJar, "org/lwjgl/opengl/Display.class",
                "LWJGL Android adapter");
        if (resultFile == null || resultFile.getParent() == null
                || !resultFile.toAbsolutePath().normalize().getParent()
                .equals(workingDirectory.toAbsolutePath().normalize())) {
            throw new IOException("LWJGL smoke-test result must remain in its working directory");
        }
        Path nativeDirectory = nativeLibraryDirectory.toAbsolutePath().normalize();
        Path gl4es = nativeDirectory.resolve("libgl4es_114.so");
        if (!Files.isRegularFile(gl4es)) {
            throw new IOException("GL4ES is not packaged for this ABI");
        }
        List<String> vmArguments = Arrays.asList(
                "-Xms32M", "-Xmx128M",
                "-Dfile.encoding=UTF-8",
                "-Djava.home=" + runtimeHome.toAbsolutePath().normalize(),
                "-Djava.library.path=" + nativeDirectory,
                "-Dorg.lwjgl.librarypath=" + nativeDirectory,
                "-Dorg.lwjgl.opengl.libname=" + gl4es,
                "-Drustedfabric.platform=android-jvm-lwjgl2-smoke");
        return new JvmLaunchPlan(workingDirectory.toAbsolutePath().normalize(),
                runtimeHome.toAbsolutePath().normalize(), nativeDirectory,
                Arrays.asList(payloadJar.toAbsolutePath().normalize(),
                        lwjglAdapterJar.toAbsolutePath().normalize()),
                vmArguments, LWJGL_SMOKE_MAIN_CLASS,
                Collections.singletonList(resultFile.toAbsolutePath().normalize().toString()));
    }

    private static void requireJarEntry(Path jarPath, String entry, String label)
            throws IOException {
        if (jarPath == null || !Files.isRegularFile(jarPath)) {
            throw new IOException(label + " JAR is unavailable");
        }
        try (ZipFile jar = new ZipFile(jarPath.toFile())) {
            if (jar.getEntry(entry) == null) {
                throw new IOException(label + " does not contain " + entry);
            }
        }
    }

    private static void requireUsableRuntime(Path runtimeHome) throws IOException {
        requireDirectory(runtimeHome, "runtimeHome");
        String runtimeIssue = JvmRuntimeProbe.runtimeIssue(runtimeHome);
        if (!runtimeIssue.isEmpty()) {
            throw new IOException("runtimeHome is not usable: " + runtimeIssue);
        }
    }

    private static void requireDirectory(Path value, String name) throws IOException {
        if (value == null || !Files.isDirectory(value)) {
            throw new IOException(name + " is not an available directory");
        }
    }
}
