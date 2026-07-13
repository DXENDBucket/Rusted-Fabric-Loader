package io.github.endx.rustedfabric.android.bootstrap;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public final class BootstrapContractVerification {
    private BootstrapContractVerification() {
    }

    public static void main(String[] args) throws Exception {
        require(BootstrapPolicy.shouldInstall("com.corrodinggames.rts", true),
                "Official first-package load must be accepted");
        require(!BootstrapPolicy.shouldInstall("com.corrodinggames.rts", false),
                "A secondary package load must not install hooks");
        require(!BootstrapPolicy.shouldInstall("community.translation.rts", true),
                "Package-renamed variants require explicit binding");
        require(!BootstrapPolicy.shouldInstall(null, true), "Null package must be rejected");

        BootstrapDiagnostics.resetForTests();
        BootstrapDiagnostics.Snapshot first = BootstrapDiagnostics.captureOnce(
                "com.corrodinggames.rts", "com.corrodinggames.rts",
                "example.Application", "example.PathClassLoader");
        BootstrapDiagnostics.Snapshot second = BootstrapDiagnostics.captureOnce(
                "wrong.package", "wrong.process", "wrong.Application", "wrong.Loader");
        require(first == second, "Diagnostics must be captured only once per process");
        require("example.Application".equals(second.getApplicationClassName()),
                "Later attach calls must not replace the first snapshot");
        require("PENDING_MAPPING".equals(second.getMappingProfileStatus()),
                "The mapping handoff placeholder must remain explicit");
        require(BootstrapDiagnostics.class.getDeclaredFields().length == 2,
                "Diagnostics must not grow hidden Context or ClassLoader references");
        verifyXposedMetadata(argument(args, "--xposed-root"));
        System.out.println("Android bootstrap target, one-shot, and reference-safety contracts passed");
    }

    private static void verifyXposedMetadata(Path xposedRoot) throws Exception {
        Path metadata = xposedRoot.resolve("module/src/main/resources/META-INF/xposed");
        String entry = read(metadata.resolve("java_init.list"));
        require("io.github.endx.rustedfabric.android.xposed.RustedFabricXposedModule".equals(entry),
                "Modern Xposed Java entry is missing or ambiguous");
        require(BootstrapPolicy.OFFICIAL_PACKAGE.equals(read(metadata.resolve("scope.list"))),
                "Static Xposed scope must contain only the official package");

        Properties properties = new Properties();
        try (java.io.InputStream input = Files.newInputStream(metadata.resolve("module.prop"))) {
            properties.load(input);
        }
        require("101".equals(properties.getProperty("minApiVersion")), "Unexpected minimum Xposed API");
        require("102".equals(properties.getProperty("targetApiVersion")), "Unexpected target Xposed API");
        require("true".equals(properties.getProperty("staticScope")), "Scope must remain static");
        require(!Files.exists(xposedRoot.resolve("module/src/main/assets/xposed_init")),
                "Legacy Xposed entry metadata must not be introduced");

        String source = read(xposedRoot.resolve("module/src/main/java/io/github/endx/rustedfabric/android/xposed/"
                + "RustedFabricXposedModule.java"));
        require(!source.contains("com.corrodinggames.rts.game"),
                "Bootstrap entry must not reference game implementation classes");
        require(source.contains("chain.proceed()"),
                "Application.attach hook must always call the original implementation");
    }

    private static String read(Path path) throws Exception {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8).trim();
    }

    private static Path argument(String[] args, String name) {
        for (int i = 0; i + 1 < args.length; i++) {
            if (name.equals(args[i])) {
                return Paths.get(args[i + 1]);
            }
        }
        throw new IllegalArgumentException("Missing " + name);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
