package io.github.endx.rustedfabric.android.bootstrap;

import java.io.ByteArrayInputStream;
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

        AndroidMappingProfile.Selection verified = AndroidMappingProfile.select(
                BootstrapPolicy.OFFICIAL_PACKAGE, "1.15", 176L, AndroidMappingProfile.APK_SHA256);
        require(verified.isVerified() && "VERIFIED".equals(verified.getStatus()),
                "The finalized official APK must select the Android mapping");
        require(!AndroidMappingProfile.select(BootstrapPolicy.OFFICIAL_PACKAGE, "1.15", 176L,
                repeat('0', 64)).isVerified(), "A modified APK hash must be rejected");
        require("VERSION_MISMATCH".equals(AndroidMappingProfile.select(
                BootstrapPolicy.OFFICIAL_PACKAGE, "1.15", 177L,
                AndroidMappingProfile.APK_SHA256).getStatus()), "Version mismatch must be explicit");
        require("PACKAGE_MISMATCH".equals(AndroidMappingProfile.select(
                "community.translation.rts", "1.15", 176L,
                AndroidMappingProfile.APK_SHA256).getStatus()), "Package mismatch must be explicit");
        require("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
                        .equals(Sha256.digest(new ByteArrayInputStream(
                                "abc".getBytes(StandardCharsets.US_ASCII)))),
                "Streaming APK SHA-256 helper is incorrect");

        BootstrapDiagnostics.resetForTests();
        BootstrapDiagnostics.Snapshot first = BootstrapDiagnostics.captureOnce(
                "com.corrodinggames.rts", "com.corrodinggames.rts",
                "example.Application", "example.PathClassLoader");
        BootstrapDiagnostics.Snapshot second = BootstrapDiagnostics.captureOnce(
                "wrong.package", "wrong.process", "wrong.Application", "wrong.Loader");
        require(first == second, "Diagnostics must be captured only once per process");
        require("example.Application".equals(second.getApplicationClassName()),
                "Later attach calls must not replace the first snapshot");
        require("PROFILE_SELECTION_PENDING".equals(second.getMappingProfileStatus()),
                "Runtime profile selection must remain explicit");
        require(BootstrapDiagnostics.class.getDeclaredFields().length == 2,
                "Diagnostics must not grow hidden Context or ClassLoader references");
        verifyXposedMetadata(argument(args, "--xposed-root"));
        verifyMappingProfile(argument(args, "--mapping-profile"));
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
                "Xposed entry must obtain game anchors from the checksum-pinned profile");
        require(source.contains("chain.proceed()"),
                "Application.attach hook must always call the original implementation");
        require(source.contains("AndroidMappingProfile.select"),
                "Installed APK identity must be checked before installing game hooks");
        require(source.contains("installGameEngineInitHook"),
                "The first mapped GameEngine initialization probe is missing");
        require(source.contains("RuntimeLifecycleEvents.BEFORE_ENGINE_INITIALIZATION.dispatch"),
                "Portable before-initialization event is not dispatched");
        require(source.contains("RuntimeLifecycleEvents.AFTER_ENGINE_INITIALIZATION.dispatch"),
                "Portable after-initialization event is not dispatched");
        require(source.contains("RustedFabricRuntime.installContext"),
                "Android API context is not installed for portable mods");
    }

    private static void verifyMappingProfile(Path profilePath) throws Exception {
        Properties profile = new Properties();
        try (java.io.InputStream input = Files.newInputStream(profilePath)) {
            profile.load(input);
        }
        require(AndroidMappingProfile.ID.equals(profile.getProperty("id")),
                "Runtime profile id diverged from profile.properties");
        require("exact".equals(profile.getProperty("matchPolicy")),
                "Final Android runtime profile must remain exact-only");
        require(AndroidMappingProfile.PACKAGE_NAME.equals(profile.getProperty("packageName")),
                "Runtime package diverged from profile.properties");
        require(AndroidMappingProfile.VERSION_NAME.equals(profile.getProperty("versionName")),
                "Runtime version name diverged from profile.properties");
        require(Long.toString(AndroidMappingProfile.VERSION_CODE).equals(profile.getProperty("versionCode")),
                "Runtime version code diverged from profile.properties");
        require(AndroidMappingProfile.APK_SHA256.equals(profile.getProperty("apkSha256")),
                "Runtime APK hash diverged from profile.properties");
        require(AndroidMappingProfile.MAPPING_SHA256.equals(profile.getProperty("mappingFileSha256")),
                "Runtime mapping hash diverged from profile.properties");
        require(AndroidMappingProfile.GAME_ENGINE_INIT_OWNER.equals(
                        profile.getProperty("hook.gameEngineInit.owner")),
                "GameEngine init owner diverged from profile.properties");
        require(AndroidMappingProfile.GAME_ENGINE_INIT_NAME.equals(
                        profile.getProperty("hook.gameEngineInit.name")),
                "GameEngine init name diverged from profile.properties");
        require(AndroidMappingProfile.GAME_ENGINE_INIT_DESCRIPTOR.equals(
                        profile.getProperty("hook.gameEngineInit.descriptor")),
                "GameEngine init descriptor diverged from profile.properties");
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

    private static String repeat(char value, int count) {
        char[] values = new char[count];
        java.util.Arrays.fill(values, value);
        return new String(values);
    }
}
