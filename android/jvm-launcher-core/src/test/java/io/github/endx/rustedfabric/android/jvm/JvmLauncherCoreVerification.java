package io.github.endx.rustedfabric.android.jvm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;

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
            DesktopGameLayout.prepareWritableDirectories(game);
            require(Files.isDirectory(game.resolve("mods/maps"))
                            && Files.isDirectory(game.resolve("mods/units"))
                            && Files.isDirectory(game.resolve("replays")),
                    "Writable desktop game directories were not prepared");
            verifyManagedContentLibrary(temporary, game);

            Path sharedContent = Files.createDirectories(temporary.resolve("shared-content"));
            System.setProperty(ManagedContentLibrary.CONTENT_ROOT_PROPERTY,
                    sharedContent.toString());
            ManagedContentLibrary.prepare(game);
            Path sharedMapSource = temporary.resolve("shared-map.tmx");
            Files.write(sharedMapSource, "<map/>".getBytes(
                    java.nio.charset.StandardCharsets.UTF_8));
            ManagedContentLibrary.Item sharedMap = ManagedContentLibrary.importContent(game,
                    ManagedContentLibrary.Kind.MAP, sharedMapSource, "Shared map");
            require(sharedMap.path().startsWith(sharedContent.resolve("maps")),
                    "Configured Android content root was not used directly");

            Path runtime = createRuntime(temporary.resolve("runtime"));
            Path loader = Files.createDirectories(temporary.resolve("loader"));
            createJar(loader.resolve("fabric-loader.jar"),
                    "net/fabricmc/loader/impl/launch/knot/KnotClient.class");
            createJar(loader.resolve("lwjgl-android.jar"), "org/lwjgl/opengl/Display.class");
            createJar(loader.resolve("rusted-fabric-lwjgl2-compat.jar"),
                    "org/lwjgl/Sys.class");
            Path natives = Files.createDirectories(temporary.resolve("natives"));
            Files.write(natives.resolve("libgl4es_114.so"), new byte[]{0});
            JvmBackendCapabilities ready = new JvmBackendCapabilities(true, true, true, true, true);
            JvmLaunchPlan plan = JvmLaunchPlanFactory.create(
                    game, runtime, loader, natives, ready, 1024, 1280, 720);
            require(DesktopGameLayout.FABRIC_MAIN_CLASS.equals(plan.mainClass()),
                    "Launch plan bypassed Fabric Knot");
            require(plan.classpath().size() == 3 && plan.virtualMachineArguments().contains("-Xmx1024M")
                            && plan.virtualMachineArguments().contains("-XX:+UseSerialGC")
                            && plan.virtualMachineArguments().stream()
                            .anyMatch(value -> value.startsWith("-Djava.home="))
                            && plan.virtualMachineArguments().contains("-Djava.io.tmpdir="
                            + game.toAbsolutePath().resolve(".rustedfabricloader/tmp"))
                            && plan.virtualMachineArguments().stream()
                            .anyMatch(value -> value.startsWith("-Drusted.gameDir="))
                            && plan.virtualMachineArguments().stream()
                            .anyMatch(value -> value.startsWith("-Drusted.android.lwjglJar="))
                            && plan.virtualMachineArguments().stream()
                            .anyMatch(value -> value.startsWith("-Drusted.android.lwjglCompatJar="))
                            && plan.virtualMachineArguments().contains("-D"
                            + ManagedContentLibrary.CONTENT_ROOT_PROPERTY + "="
                            + sharedContent.toAbsolutePath().normalize())
                            && plan.virtualMachineArguments().contains("-Drusted.javamodsDir="
                            + sharedContent.toAbsolutePath().normalize().resolve("javamods"))
                            && plan.virtualMachineArguments().stream()
                            .noneMatch(value -> value.startsWith("-Drusted.javamodsDev"))
                            && plan.virtualMachineArguments().stream()
                            .anyMatch(value -> value.startsWith("-Duser.language="))
                            && plan.workingDirectory().equals(game.toAbsolutePath().normalize()),
                    "Launch plan classpath or JVM options changed");
            require(Files.isDirectory(game.resolve(".rustedfabricloader/tmp")),
                    "Launch plan did not create its private Java temporary directory");
            require(plan.gameArguments().contains("1280") && plan.gameArguments().contains("720"),
                    "Launch surface was not forwarded");
            System.clearProperty(ManagedContentLibrary.CONTENT_ROOT_PROPERTY);

            Path smokeJar = temporary.resolve("jvm-smoke.jar");
            createJar(smokeJar, JvmLaunchPlanFactory.SMOKE_MAIN_CLASS.replace('.', '/') + ".class");
            Path smokeWork = Files.createDirectories(temporary.resolve("smoke-work"));
            Path smokeResult = smokeWork.resolve("result.txt");
            JvmLaunchPlan smokePlan = JvmLaunchPlanFactory.createSmokeTest(runtime, smokeJar,
                    smokeWork, natives, smokeResult);
            require(JvmLaunchPlanFactory.SMOKE_MAIN_CLASS.equals(smokePlan.mainClass())
                            && smokePlan.gameArguments().equals(
                            java.util.Collections.singletonList(smokeResult.toString()))
                            && smokePlan.classpath().equals(
                            java.util.Collections.singletonList(smokeJar.toAbsolutePath())),
                    "External-JVM smoke-test plan changed");

            Path lwjglAdapter = temporary.resolve("lwjgl-android.jar");
            createJar(lwjglAdapter, "org/lwjgl/opengl/Display.class");
            JvmLaunchPlan lwjglPlan = JvmLaunchPlanFactory.createLwjglSmokeTest(runtime,
                    smokeJarWithLwjglMain(temporary), lwjglAdapter, smokeWork, natives,
                    smokeWork.resolve("lwjgl-result.txt"));
            require(JvmLaunchPlanFactory.LWJGL_SMOKE_MAIN_CLASS.equals(lwjglPlan.mainClass())
                            && lwjglPlan.classpath().size() == 2
                            && lwjglPlan.virtualMachineArguments().stream()
                            .anyMatch(value -> value.startsWith("-Dorg.lwjgl.opengl.libname=")),
                    "LWJGL2 Android smoke-test plan changed");

            JvmBackendCapabilities probeOnly = new JvmBackendCapabilities(
                    true, true, true, false, false, false);
            require(JvmLaunchPlanFactory.createCompatibilityProbe(game, runtime, loader,
                            natives, probeOnly, 768, 960, 540).classpath().size() == 3,
                    "Compatibility probe did not accept the implemented renderer path");
            JvmLaunchPlan chinesePlan = JvmLaunchPlanFactory.createCompatibilityProbe(
                    game, runtime, loader, natives, probeOnly, 768, 960, 540,
                    Locale.SIMPLIFIED_CHINESE);
            require(chinesePlan.virtualMachineArguments().contains("-Duser.language=zh")
                            && chinesePlan.virtualMachineArguments().contains("-Duser.country=CN"),
                    "Android device locale was not forwarded to the embedded JVM");

            boolean incompleteRejected = false;
            try {
                JvmLaunchPlanFactory.create(game, runtime, loader, natives,
                        JvmBackendCapabilities.unavailable(), 1024, 1280, 720);
            } catch (IllegalStateException expected) {
                incompleteRejected = expected.getMessage().contains("rocket-connector-arm64");
            }
            require(incompleteRejected, "Incomplete platform adapters did not fail closed");

            Path probedRuntime = createRuntime(temporary.resolve("probed-runtime"));
            Path probedNatives = Files.createDirectories(temporary.resolve("probed-natives"));
            for (String nativeName : new String[]{"librustedfabric_jvmhost.so", "liblwjgl.so",
                    "libopenal.so", "librustedfabric_renderbridge.so",
                    "librocketConnector.so"}) {
                Files.write(probedNatives.resolve(nativeName), new byte[]{0});
            }
            require(JvmRuntimeProbe.inspect(probedRuntime, probedNatives).isLaunchReady(),
                    "Complete Java 17 runtime and adapter layout was not detected");
            Path apkMappedNatives = Files.createDirectories(
                    temporary.resolve("apk-mapped-natives"));
            require(JvmRuntimeProbe.inspect(probedRuntime, apkMappedNatives, true).hasJvmHost(),
                    "APK-mapped native JVM host was not detected");
            require(!JvmRuntimeProbe.inspect(probedRuntime, apkMappedNatives, false).hasJvmHost(),
                    "Missing native JVM host was accepted without a successful package load");

            Path darwinRuntime = createRuntime(temporary.resolve("darwin-runtime"));
            Files.write(darwinRuntime.resolve("release"), runtimeRelease("Darwin"));
            Files.write(darwinRuntime.resolve("lib/server/libjvm.so"), machOArm64());
            require(!JvmRuntimeProbe.inspect(darwinRuntime, probedNatives).hasJava17()
                            && JvmRuntimeProbe.runtimeIssue(darwinRuntime).contains("not Linux"),
                    "A Darwin/Mach-O runtime was incorrectly accepted as Android-compatible");

            Path runtimeArchive = temporary.resolve("runtime.zip");
            try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(runtimeArchive))) {
                addArchiveFile(zip, "jre17/release", runtimeRelease("Linux"));
                addArchiveFile(zip, "jre17/lib/server/libjvm.so", aarch64Elf());
                addArchiveFile(zip, "jre17/lib/libjava.so", aarch64Elf());
                addArchiveFile(zip, "jre17/lib/modules", new byte[]{3});
            }
            Path importedRuntime = Files.createDirectories(temporary.resolve("imported-runtime"));
            JvmRuntimeArchiveExtractor.Result runtimeResult =
                    JvmRuntimeArchiveExtractor.extract(runtimeArchive, importedRuntime, null);
            require("jre17".equals(runtimeResult.archiveRoot())
                            && runtimeResult.archiveSha256().length() == 64
                            && JvmRuntimeProbe.inspect(importedRuntime, probedNatives).isLaunchReady(),
                    "Java 17 runtime ZIP import contract failed");

            Path runtimeTarXz = temporary.resolve("runtime.tar.xz");
            try (XZCompressorOutputStream xz = new XZCompressorOutputStream(
                    Files.newOutputStream(runtimeTarXz));
                 TarArchiveOutputStream tar = new TarArchiveOutputStream(xz)) {
                addTarFile(tar, "./jre17/release", runtimeRelease("Linux"));
                addTarFile(tar, "./jre17/lib/server/libjvm.so", aarch64Elf());
                addTarFile(tar, "./jre17/lib/libjava.so", aarch64Elf());
                addTarFile(tar, "./jre17/lib/modules", new byte[]{3});
                TarArchiveEntry legalLink = new TarArchiveEntry(
                        "./jre17/legal/java.xml/LICENSE", TarArchiveEntry.LF_SYMLINK);
                legalLink.setLinkName("../java.base/LICENSE");
                tar.putArchiveEntry(legalLink);
                tar.closeArchiveEntry();
            }
            Path importedTarRuntime = Files.createDirectories(
                    temporary.resolve("imported-tar-runtime"));
            JvmRuntimeArchiveExtractor.Result tarRuntimeResult =
                    JvmRuntimeArchiveExtractor.extract(runtimeTarXz, importedTarRuntime, null);
            require("jre17".equals(tarRuntimeResult.archiveRoot())
                            && tarRuntimeResult.files() == 4
                            && !Files.exists(importedTarRuntime.resolve("legal/java.xml/LICENSE"))
                            && JvmRuntimeProbe.runtimeIssue(importedTarRuntime).isEmpty(),
                    "Java 17 runtime TAR.XZ import contract failed: root="
                            + tarRuntimeResult.archiveRoot() + ", files="
                            + tarRuntimeResult.files() + ", issue="
                            + JvmRuntimeProbe.runtimeIssue(importedTarRuntime));

            Path archive = temporary.resolve("desktop-game.zip");
            createDesktopArchive(archive);
            Path extracted = Files.createDirectories(temporary.resolve("extracted"));
            DesktopGameArchiveExtractor.Result archiveResult =
                    DesktopGameArchiveExtractor.extract(archive, extracted, null);
            require("Rusted Warfare".equals(archiveResult.archiveRoot())
                            && DesktopGameLayout.inspect(extracted).isImportable(),
                    "Wrapped desktop ZIP was not imported");
            require(!Files.exists(extracted.resolve("lwjgl64.dll"))
                            && !Files.exists(extracted.resolve("saves/player.save")),
                    "Non-portable desktop data escaped the ZIP filter");

            Path malicious = temporary.resolve("malicious.zip");
            try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(malicious))) {
                zip.putNextEntry(new ZipEntry("../escaped.txt"));
                zip.write(new byte[]{1});
                zip.closeEntry();
            }
            boolean traversalRejected = false;
            try {
                DesktopGameArchiveExtractor.extract(malicious,
                        Files.createDirectories(temporary.resolve("malicious-out")), null);
            } catch (IOException expected) {
                traversalRejected = expected.getMessage().contains("traversal");
            }
            require(traversalRejected && !Files.exists(temporary.resolve("escaped.txt")),
                    "ZIP path traversal was not rejected");

            Files.delete(game.resolve("libs/lwjgl.jar"));
            require(!DesktopGameLayout.inspect(game).isImportable(),
                    "Incomplete user game files were accepted");
            System.out.println("Android desktop-JVM launcher core contracts passed");
        } finally {
            System.clearProperty(ManagedContentLibrary.CONTENT_ROOT_PROPERTY);
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

    private static Path createRuntime(Path root) throws IOException {
        Files.createDirectories(root.resolve("lib/server"));
        Files.write(root.resolve("release"), runtimeRelease("Linux"));
        Files.write(root.resolve("lib/server/libjvm.so"), aarch64Elf());
        Files.write(root.resolve("lib/libjava.so"), aarch64Elf());
        Files.write(root.resolve("lib/modules"), new byte[]{1});
        return root;
    }

    private static void verifyManagedContentLibrary(Path temporary, Path game) throws Exception {
        ManagedContentLibrary.prepare(game);

        Path map = temporary.resolve("challenge.tmx");
        Files.write(map, "<map/>".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        ManagedContentLibrary.Item importedMap = ManagedContentLibrary.importContent(game,
                ManagedContentLibrary.Kind.MAP, map, "Challenge map");
        require(importedMap.enabled()
                        && ManagedContentLibrary.list(game, ManagedContentLibrary.Kind.MAP).size() == 1,
                "Map import was not listed");
        importedMap = ManagedContentLibrary.setEnabled(game, importedMap, false);
        require(!importedMap.enabled() && Files.isRegularFile(
                        game.resolve("mods-disabled/maps/challenge.tmx")),
                "Map disable did not leave the native scan directory");

        Path iniArchive = temporary.resolve("units.rwmod");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(iniArchive))) {
            addArchiveFile(zip, "wrapped/mod-info.txt", "[mod]".getBytes(
                    java.nio.charset.StandardCharsets.UTF_8));
            addArchiveFile(zip, "wrapped/tank.ini", "[core]".getBytes(
                    java.nio.charset.StandardCharsets.UTF_8));
        }
        ManagedContentLibrary.Item ini = ManagedContentLibrary.importContent(game,
                ManagedContentLibrary.Kind.INI_MOD, iniArchive, "Unit pack");
        require("Unit pack".equals(ini.name()) && Files.isRegularFile(ini.path().resolve("tank.ini")),
                "INI archive import did not flatten and identify its wrapper");

        Path thirdParty = temporary.resolve("third-party.javamod");
        createFabricModJar(thirdParty, "third_party", "Third Party", "1.2.3");
        ManagedContentLibrary.Item javaMod = ManagedContentLibrary.importContent(game,
                ManagedContentLibrary.Kind.JAVA_MOD, thirdParty, "ignored");
        require("third_party".equals(javaMod.id()) && javaMod.enabled(),
                "Java mod metadata import failed");
        javaMod = ManagedContentLibrary.setEnabled(game, javaMod, false);
        require(!javaMod.enabled() && javaMod.path().startsWith(game.resolve("javamods-disabled")),
                "Java mod disable did not leave Fabric discovery");

        Path api = temporary.resolve("api.jar");
        createFabricModJar(api, "rusted_fabric_api", "Rusted Fabric API", "0.1.0");
        ManagedContentLibrary.Item officialApi = ManagedContentLibrary.provisionOfficialJavaMod(
                game, api, "rusted_fabric_api", true);
        require(officialApi.official() && officialApi.enabled(),
                "Bundled API was not provisioned as a manageable official component");

        Path essentials = temporary.resolve("essentials.jar");
        createFabricModJar(essentials, "ini_essentials", "INI Essentials", "0.1.0");
        ManagedContentLibrary.Item officialIni = ManagedContentLibrary.provisionOfficialJavaMod(
                game, essentials, "ini_essentials", false);
        require(officialIni.official() && !officialIni.enabled(),
                "INI Essentials was not installed disabled by default");
        officialIni = ManagedContentLibrary.setEnabled(game, officialIni, true);
        ManagedContentLibrary.Item updatedIni = ManagedContentLibrary.provisionOfficialJavaMod(
                game, essentials, "ini_essentials", false);
        require(updatedIni.enabled(), "Official mod update did not preserve user enable state");
        ManagedContentLibrary.delete(game, updatedIni);
        require(ManagedContentLibrary.list(game, ManagedContentLibrary.Kind.JAVA_MOD).stream()
                        .noneMatch(item -> "ini_essentials".equals(item.id())),
                "Optional bundled Java mod could not be deleted");
        officialApi = ManagedContentLibrary.setEnabled(game, officialApi, false);
        require(!officialApi.enabled(), "Bundled API could not be disabled");
        Path manualApi = temporary.resolve("updated-api.javamod");
        createFabricModJar(manualApi, "rusted_fabric_api", "Rusted Fabric API", "0.2.0");
        officialApi = ManagedContentLibrary.importContent(game,
                ManagedContentLibrary.Kind.JAVA_MOD, manualApi, "updated API");
        require(officialApi.enabled() && "0.2.0".equals(officialApi.version()),
                "Manual official-ID update was not imported");
        ManagedContentLibrary.Item preservedApi = ManagedContentLibrary.provisionOfficialJavaMod(
                game, api, "rusted_fabric_api", true);
        require("0.2.0".equals(preservedApi.version())
                        && !preservedApi.path().getFileName().toString().startsWith("official-"),
                "An older bundled component overwrote a newer manual official-ID update");
        Path nextApi = temporary.resolve("next-api.jar");
        createFabricModJar(nextApi, "rusted_fabric_api", "Rusted Fabric API", "0.3.0");
        ManagedContentLibrary.Item upgradedApi = ManagedContentLibrary.provisionOfficialJavaMod(
                game, nextApi, "rusted_fabric_api", true);
        require("0.3.0".equals(upgradedApi.version()) && upgradedApi.enabled()
                        && upgradedApi.path().getFileName().toString().startsWith("official-"),
                "A newer bundled component did not replace an older official-ID Jar");
        ManagedContentLibrary.delete(game, upgradedApi);
        require(ManagedContentLibrary.list(game, ManagedContentLibrary.Kind.JAVA_MOD).stream()
                        .noneMatch(item -> "rusted_fabric_api".equals(item.id())),
                "Updated API could not be deleted");

        ManagedContentLibrary.delete(game, javaMod);
        require(ManagedContentLibrary.list(game, ManagedContentLibrary.Kind.JAVA_MOD).stream()
                        .noneMatch(item -> "third_party".equals(item.id())),
                "Managed Java mod delete failed");

        Path malicious = temporary.resolve("content-traversal.zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(malicious))) {
            addArchiveFile(zip, "../escaped.tmx", new byte[]{1});
        }
        boolean rejected = false;
        try {
            ManagedContentLibrary.importContent(game, ManagedContentLibrary.Kind.MAP,
                    malicious, "malicious");
        } catch (IOException expected) {
            rejected = expected.getMessage().contains("traversal");
        }
        require(rejected && !Files.exists(game.resolve("mods/escaped.tmx")),
                "Managed-content archive traversal was not rejected");
    }

    private static byte[] runtimeRelease(String osName) {
        return ("JAVA_VERSION=\"17.0.12\"\nOS_ARCH=\"aarch64\"\nOS_NAME=\""
                + osName + "\"\n").getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private static void createJar(Path output, String entryName) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(output))) {
            zip.putNextEntry(new ZipEntry(entryName));
            zip.write(new byte[]{0});
            zip.closeEntry();
        }
    }

    private static void createFabricModJar(Path output, String id, String name, String version)
            throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(output))) {
            zip.putNextEntry(new ZipEntry("fabric.mod.json"));
            String json = "{\"schemaVersion\":1,\"id\":\"" + id + "\",\"name\":\""
                    + name + "\",\"version\":\"" + version + "\"}";
            zip.write(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zip.closeEntry();
        }
    }

    private static Path smokeJarWithLwjglMain(Path temporary) throws IOException {
        Path output = temporary.resolve("lwjgl-smoke.jar");
        createJar(output,
                JvmLaunchPlanFactory.LWJGL_SMOKE_MAIN_CLASS.replace('.', '/') + ".class");
        return output;
    }

    private static void createDesktopArchive(Path output) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(output))) {
            addArchiveJar(zip, "Rusted Warfare/game-lib.jar",
                    "com/corrodinggames/rts/java/Main.class");
            addArchiveJar(zip, "Rusted Warfare/libs/lwjgl.jar", "org/lwjgl/Marker.class");
            addArchiveJar(zip, "Rusted Warfare/libs/slick.jar", "org/newdawn/slick/Marker.class");
            addArchiveJar(zip, "Rusted Warfare/libs/jinput.jar", "net/java/games/input/Marker.class");
            addArchiveFile(zip, "Rusted Warfare/assets/units/core.ini", new byte[]{1});
            addArchiveFile(zip, "Rusted Warfare/res/values/strings.xml", new byte[]{2});
            addArchiveFile(zip, "Rusted Warfare/lwjgl64.dll", new byte[]{3});
            addArchiveFile(zip, "Rusted Warfare/saves/player.save", new byte[]{4});
        }
    }

    private static void addArchiveJar(ZipOutputStream archive, String path, String classEntry)
            throws IOException {
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        try (ZipOutputStream nested = new ZipOutputStream(bytes)) {
            nested.putNextEntry(new ZipEntry(classEntry));
            nested.write(new byte[]{0});
            nested.closeEntry();
        }
        addArchiveFile(archive, path, bytes.toByteArray());
    }

    private static void addArchiveFile(ZipOutputStream archive, String path, byte[] bytes)
            throws IOException {
        archive.putNextEntry(new ZipEntry(path));
        archive.write(bytes);
        archive.closeEntry();
    }

    private static void addTarFile(TarArchiveOutputStream archive, String path, byte[] bytes)
            throws IOException {
        TarArchiveEntry entry = new TarArchiveEntry(path);
        entry.setSize(bytes.length);
        archive.putArchiveEntry(entry);
        archive.write(bytes);
        archive.closeArchiveEntry();
    }

    private static byte[] aarch64Elf() {
        byte[] header = new byte[20];
        header[0] = 0x7f;
        header[1] = 'E';
        header[2] = 'L';
        header[3] = 'F';
        header[4] = 2;
        header[5] = 1;
        header[18] = (byte) 0xb7;
        return header;
    }

    private static byte[] machOArm64() {
        return new byte[]{(byte) 0xcf, (byte) 0xfa, (byte) 0xed, (byte) 0xfe,
                0x0c, 0x00, 0x00, 0x01};
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
