package io.github.endx.rustedfabricloader;

import net.fabricmc.loader.impl.util.log.LogCategory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/** Exploded discovery, explicit links, deterministic override, and candidate-list checks. */
public final class JavaModDevelopmentWorkspacesVerification {
    private JavaModDevelopmentWorkspacesVerification() { }

    public static void main(String[] arguments) throws Exception {
        Path temporary = Files.createTempDirectory("rfl-java-mod-dev-");
        try {
            Path game = Files.createDirectories(temporary.resolve("game"));
            Path jars = Files.createDirectories(game.resolve("javamods"));
            Path dev = Files.createDirectories(game.resolve("javamods-dev"));
            Path packagedOverride = jars.resolve("test-mod.jar");
            Path packagedOther = jars.resolve("other-mod.jar");
            createJar(packagedOverride, metadata("test_mod", "1.0.0"));
            createJar(packagedOther, metadata("other_mod", "1.0.0"));

            Path workspace = Files.createDirectories(dev.resolve("test_mod"));
            Files.write(workspace.resolve("fabric.mod.json"),
                    metadata("test_mod", "2.0.0").getBytes(StandardCharsets.UTF_8));
            Path linked = Files.createDirectories(temporary.resolve("linked-workspace"));
            Files.write(linked.resolve("fabric.mod.json"),
                    metadata("linked_mod", "1.0.0").getBytes(StandardCharsets.UTF_8));
            Files.write(dev.resolve("linked_mod.link"),
                    linked.toString().getBytes(StandardCharsets.UTF_8));

            System.setProperty(JavaModDevelopmentWorkspaces.DEV_DIR_PROPERTY, dev.toString());
            JavaModDevelopmentWorkspaces.Selection selected =
                    JavaModDevelopmentWorkspaces.discover(game, jars, false,
                            LogCategory.create("Test", "JavaModDevelopment"));
            require(selected.workspaces.size() == 2
                            && selected.workspaces.get("test_mod").root.equals(workspace.toRealPath())
                            && selected.workspaces.get("linked_mod").linked,
                    "exploded or linked workspace was not discovered");
            require(!selected.candidates.contains(packagedOverride.toAbsolutePath().normalize())
                            && selected.candidates.contains(packagedOther.toAbsolutePath().normalize())
                            && selected.candidates.contains(workspace.toRealPath())
                            && "linked_mod,test_mod".equals(System.getProperty(
                            JavaModDevelopmentWorkspaces.WORKSPACE_IDS_PROPERTY)),
                    "workspace did not deterministically override its packaged mod");
            Path list = JavaModDevelopmentWorkspaces.writeCandidateList(game,
                    selected.candidates);
            require(Files.readAllLines(list, StandardCharsets.UTF_8).size()
                            == selected.candidates.size(),
                    "Fabric candidate list lost selected mods");
        } finally {
            System.clearProperty(JavaModDevelopmentWorkspaces.DEV_DIR_PROPERTY);
            System.clearProperty(JavaModDevelopmentWorkspaces.RESOLVED_DEV_DIR_PROPERTY);
            System.clearProperty(JavaModDevelopmentWorkspaces.AUTO_RELOAD_PROPERTY);
            String ids = System.getProperty(
                    JavaModDevelopmentWorkspaces.WORKSPACE_IDS_PROPERTY, "");
            for (String id : ids.split(",")) {
                System.clearProperty(JavaModDevelopmentWorkspaces.WORKSPACE_PROPERTY_PREFIX + id);
            }
            System.clearProperty(JavaModDevelopmentWorkspaces.WORKSPACE_IDS_PROPERTY);
            try (java.util.stream.Stream<Path> paths = Files.walk(temporary)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try { Files.deleteIfExists(path); } catch (IOException ignored) { }
                });
            }
        }
        System.out.println("Java mod development workspace discovery passed");
    }

    private static void createJar(Path path, String metadata) throws IOException {
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(path))) {
            output.putNextEntry(new JarEntry("fabric.mod.json"));
            output.write(metadata.getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
        }
    }

    private static String metadata(String id, String version) {
        return "{\"schemaVersion\":1,\"id\":\"" + id
                + "\",\"version\":\"" + version + "\"}";
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
