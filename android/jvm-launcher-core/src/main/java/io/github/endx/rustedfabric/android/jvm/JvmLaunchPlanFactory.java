package io.github.endx.rustedfabric.android.jvm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipFile;

/** Builds a launch request only after game files and every platform adapter are verified. */
public final class JvmLaunchPlanFactory {
    public static final String SMOKE_MAIN_CLASS =
            "io.github.endx.rustedfabric.android.jvm.JvmHostSmokeMain";

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
        classpath.addAll(DesktopGameLayout.desktopClasspath(gameRoot));
        List<String> vmArguments = Arrays.asList(
                "-Xmx" + maximumHeapMiB + "M",
                "-Dfile.encoding=UTF-8",
                "-Djava.home=" + runtimeHome.toAbsolutePath().normalize(),
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
