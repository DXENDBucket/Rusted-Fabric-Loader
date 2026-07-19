package io.github.endx.rustedfabric.android.jvm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class JvmLauncherCoreVerification {
    private JvmLauncherCoreVerification() {
    }

    public static void main(String[] args) throws Exception {
        Path temporary = Files.createTempDirectory("rusted-fabric-jvm-core-");
        try {
            Path game = createGame(temporary.resolve("game"));
            DesktopGameInspection inspection = DesktopGameLayout.inspect(game);
            require(inspection.isImportable(), "Valid desktop layout was rejected: " + inspection.errors());
            require(DesktopGameLayout.desktopClasspath(game).size() == 4,
                    "Desktop classpath did not contain the game and required libraries");

            Path runtime = Files.createDirectories(temporary.resolve("runtime"));
            Path loader = Files.createDirectories(temporary.resolve("loader"));
            createJar(loader.resolve("fabric-loader.jar"), "net/fabricmc/loader/Marker.class");
            Path natives = Files.createDirectories(temporary.resolve("natives"));
            JvmBackendCapabilities ready = new JvmBackendCapabilities(true, true, true, true, true);
            JvmLaunchPlan plan = JvmLaunchPlanFactory.create(
                    game, runtime, loader, natives, ready, 1024, 1280, 720);
            require(DesktopGameLayout.FABRIC_MAIN_CLASS.equals(plan.mainClass()),
                    "Launch plan bypassed Fabric Knot");
            require(plan.classpath().size() == 5 && plan.virtualMachineArguments().contains("-Xmx1024M"),
                    "Launch plan classpath or JVM options changed");
            require(plan.gameArguments().contains("1280") && plan.gameArguments().contains("720"),
                    "Launch surface was not forwarded");

            boolean incompleteRejected = false;
            try {
                JvmLaunchPlanFactory.create(game, runtime, loader, natives,
                        JvmBackendCapabilities.unavailable(), 1024, 1280, 720);
            } catch (IllegalStateException expected) {
                incompleteRejected = expected.getMessage().contains("rocket-connector-arm64");
            }
            require(incompleteRejected, "Incomplete platform adapters did not fail closed");

            Files.delete(game.resolve("libs/lwjgl.jar"));
            require(!DesktopGameLayout.inspect(game).isImportable(),
                    "Incomplete user game files were accepted");
            System.out.println("Android desktop-JVM launcher core contracts passed");
        } finally {
            deleteRecursively(temporary);
        }
    }

    private static Path createGame(Path root) throws IOException {
        Files.createDirectories(root.resolve("assets"));
        Files.createDirectories(root.resolve("res"));
        Files.createDirectories(root.resolve("libs"));
        createJar(root.resolve("game-lib.jar"), "com/corrodinggames/rts/java/Main.class");
        createJar(root.resolve("libs/lwjgl.jar"), "org/lwjgl/Marker.class");
        createJar(root.resolve("libs/slick.jar"), "org/newdawn/slick/Marker.class");
        createJar(root.resolve("libs/jinput.jar"), "net/java/games/input/Marker.class");
        return root;
    }

    private static void createJar(Path output, String entryName) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(output))) {
            zip.putNextEntry(new ZipEntry(entryName));
            zip.write(new byte[]{0});
            zip.closeEntry();
        }
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) return;
        try (java.util.stream.Stream<Path> entries = Files.walk(root)) {
            entries.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException failure) {
                    throw new RuntimeException(failure);
                }
            });
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
