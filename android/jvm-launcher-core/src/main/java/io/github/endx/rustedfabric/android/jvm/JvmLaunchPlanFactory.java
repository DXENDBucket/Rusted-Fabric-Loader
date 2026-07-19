package io.github.endx.rustedfabric.android.jvm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Builds a launch request only after game files and every platform adapter are verified. */
public final class JvmLaunchPlanFactory {
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
        if (maximumHeapMiB < 256 || maximumHeapMiB > 8192) {
            throw new IllegalArgumentException("maximumHeapMiB must be between 256 and 8192");
        }
        if (width < 320 || height < 240) {
            throw new IllegalArgumentException("launch surface is too small");
        }
        requireDirectory(runtimeHome, "runtimeHome");
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
        classpath.addAll(DesktopGameLayout.desktopClasspath(gameRoot));
        List<String> vmArguments = Arrays.asList(
                "-Xmx" + maximumHeapMiB + "M",
                "-Dfile.encoding=UTF-8",
                "-Djava.library.path=" + nativeLibraryDirectory.toAbsolutePath().normalize(),
                "-Drustedfabric.platform=android-jvm",
                "-Drustedfabric.gameDir=" + gameRoot.toAbsolutePath().normalize());
        List<String> gameArguments = Arrays.asList(
                "-width", Integer.toString(width), "-height", Integer.toString(height));
        return new JvmLaunchPlan(gameRoot.toAbsolutePath().normalize(),
                runtimeHome.toAbsolutePath().normalize(),
                nativeLibraryDirectory.toAbsolutePath().normalize(), classpath, vmArguments,
                DesktopGameLayout.FABRIC_MAIN_CLASS, gameArguments);
    }

    private static void requireDirectory(Path value, String name) throws IOException {
        if (value == null || !Files.isDirectory(value)) {
            throw new IOException(name + " is not an available directory");
        }
    }
}
