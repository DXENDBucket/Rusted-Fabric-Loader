package io.github.endx.rustedfabric.android.mod;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import io.github.endx.rustedfabricapi.api.multiplayer.MultiplayerMod;

public final class ModRuntimeContractVerification {
    private static final String ENTRYPOINT = "example.mod.PortableEntrypoint";
    private static final String PROFILE = "rw-android-1.15-code176-v1.0";

    private ModRuntimeContractVerification() {
    }

    public static void main(String[] args) throws Exception {
        Path temporary = Files.createTempDirectory("rusted-fabric-mod-runtime-");
        try {
            verifyValidArchive(temporary);
            verifyReservedDefinitionsAreRejected(temporary);
            verifyEntrypointMustBeDefined(temporary);
            verifyForbiddenEntriesAreRejected(temporary);
            verifyTraversalIsRejected(temporary);
            verifyClassLoaderRouting();
            verifyPrivateRegistry(temporary);
            System.out.println("Android mod runtime contracts passed");
        } finally {
            deleteTree(temporary);
        }
    }

    private static void verifyValidArchive(Path temporary) throws Exception {
        Path archive = createArchive(temporary.resolve("valid.rfmod"),
                metadata(ENTRYPOINT), dex(ENTRYPOINT), null);
        VerifiedModArchive verified = new RustedFabricModVerifier().verify(archive);
        require("portable_probe".equals(verified.getMetadata().getId()), "mod id missing");
        require(verified.getMetadata().supportsMappingProfile(PROFILE), "profile missing");
        require(verified.getMetadata().getMultiplayer().mode()
                        == MultiplayerMod.Mode.CLIENT_ONLY,
                "multiplayer declaration missing");
        require(verified.getDefinedClasses().contains(ENTRYPOINT), "DEX definition missing");
        require(verified.getArchiveSha256().length() == 64, "archive hash is invalid");
        require(verified.getDexSha256().length() == 64, "DEX hash is invalid");
        require(verified.getArchiveSha256().equals(
                        new RustedFabricModVerifier().verify(archive).getArchiveSha256()),
                "verification must be deterministic");
    }

    private static void verifyReservedDefinitionsAreRejected(Path temporary) throws Exception {
        assertReason(createArchive(temporary.resolve("game-class.rfmod"),
                        metadata("com.corrodinggames.rts.gameFramework.k"),
                        dex("com.corrodinggames.rts.gameFramework.k"), null),
                ModVerificationException.Reason.FORBIDDEN_CLASS_DEFINITION);
        assertReason(createArchive(temporary.resolve("api-class.rfmod"),
                        metadata("io.github.endx.rustedfabricapi.api.Shadow"),
                        dex("io.github.endx.rustedfabricapi.api.Shadow"), null),
                ModVerificationException.Reason.FORBIDDEN_CLASS_DEFINITION);
    }

    private static void verifyEntrypointMustBeDefined(Path temporary) throws Exception {
        assertReason(createArchive(temporary.resolve("missing-entrypoint.rfmod"),
                        metadata(ENTRYPOINT), dex("example.mod.AnotherClass"), null),
                ModVerificationException.Reason.ENTRYPOINT_NOT_DEFINED);
    }

    private static void verifyForbiddenEntriesAreRejected(Path temporary) throws Exception {
        assertReason(createArchive(temporary.resolve("class-file.rfmod"),
                        metadata(ENTRYPOINT), dex(ENTRYPOINT), "example/mod/PortableEntrypoint.class"),
                ModVerificationException.Reason.FORBIDDEN_ENTRY);
    }

    private static void verifyTraversalIsRejected(Path temporary) throws Exception {
        assertReason(createArchive(temporary.resolve("traversal.rfmod"),
                        metadata(ENTRYPOINT), dex(ENTRYPOINT), "assets/../escape.txt"),
                ModVerificationException.Reason.INVALID_ARCHIVE);
    }

    private static void verifyClassLoaderRouting() {
        RecordingClassLoader api = new RecordingClassLoader();
        RecordingClassLoader game = new RecordingClassLoader();
        ClassLoader bridge = new DelegatingModParentClassLoader(api, game);

        expectClassNotFound(bridge, "io.github.endx.rustedfabricapi.api.RustedFabricAPIContext");
        require(api.requests.equals(Arrays.asList(
                        "io.github.endx.rustedfabricapi.api.RustedFabricAPIContext")),
                "common API must resolve through the module API loader");
        require(game.requests.isEmpty(), "common API must not reach the game loader");

        expectClassNotFound(bridge, "com.corrodinggames.rts.gameFramework.k");
        require(game.requests.equals(Arrays.asList("com.corrodinggames.rts.gameFramework.k")),
                "game classes must resolve through the game loader");

        int apiCalls = api.requests.size();
        int gameCalls = game.requests.size();
        expectClassNotFound(bridge, "io.github.endx.rustedfabric.android.mod.RustedFabricModVerifier");
        require(api.requests.size() == apiCalls && game.requests.size() == gameCalls,
                "loader implementation classes must be hidden from mods");
    }

    private static void verifyPrivateRegistry(Path temporary) throws Exception {
        Path archive = createArchive(temporary.resolve("registry-source.rfmod"),
                metadata(ENTRYPOINT), dex(ENTRYPOINT), null);
        VerifiedModArchive verified = new RustedFabricModVerifier().verify(archive);
        ModRegistry registry = new ModRegistry(temporary.resolve("private-registry"));
        ModRegistry.Record installed = registry.install(verified);
        require(!installed.isEnabled(), "new mods must default to disabled");
        require(Files.isRegularFile(registry.archivePath(installed)),
                "private archive was not installed");
        require(registry.list().size() == 1, "installed mod is missing from registry");
        require(registry.setEnabled(installed.getId(), true).isEnabled(),
                "enabled state was not persisted");
        ModRegistry reopened = new ModRegistry(temporary.resolve("private-registry"));
        require(reopened.find(installed.getId()).orElseThrow().isEnabled(),
                "registry did not survive reopen");
        require(reopened.find(installed.getId()).orElseThrow().getMultiplayer().mode()
                        == MultiplayerMod.Mode.CLIENT_ONLY,
                "multiplayer declaration did not survive registry reopen");
        require(reopened.remove(installed.getId()), "mod removal failed");
        require(reopened.list().isEmpty(), "removed mod remains registered");
        require(!Files.exists(registry.archivePath(installed)), "orphan archive remains");
    }

    private static Path createArchive(Path target, String metadata, byte[] dex,
                                      String extraEntry) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(target))) {
            put(zip, RustedFabricModVerifier.METADATA_PATH,
                    metadata.getBytes(StandardCharsets.UTF_8));
            put(zip, RustedFabricModVerifier.DEX_PATH, dex);
            if (extraEntry != null) {
                put(zip, extraEntry, new byte[]{1});
            }
        }
        return target;
    }

    private static void put(ZipOutputStream zip, String name, byte[] value) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        entry.setTime(0L);
        zip.putNextEntry(entry);
        zip.write(value);
        zip.closeEntry();
    }

    private static String metadata(String entrypoint) {
        return "schemaVersion=1\n"
                + "id=portable_probe\n"
                + "version=1.0.0\n"
                + "name=Portable Probe\n"
                + "entrypoint=" + entrypoint + "\n"
                + "apiVersion=0.1\n"
                + "mappingProfiles=" + PROFILE + "\n"
                + "capabilities=event.engine.init\n"
                + "multiplayerMode=client_only\n"
                + "platform=android\n"
                + "dex=classes.dex\n";
    }

    private static byte[] dex(String... binaryNames) throws IOException {
        List<byte[]> descriptors = new ArrayList<>();
        int stringDataSize = 0;
        for (String name : binaryNames) {
            byte[] value = ("L" + name.replace('.', '/') + ";").getBytes(StandardCharsets.UTF_8);
            require(value.length < 128, "synthetic descriptor is too long");
            descriptors.add(value);
            stringDataSize += 1 + value.length + 1;
        }
        int count = descriptors.size();
        int stringIdsOffset = 112;
        int typeIdsOffset = stringIdsOffset + count * 4;
        int classDefsOffset = typeIdsOffset + count * 4;
        int dataOffset = classDefsOffset + count * 32;
        int fileSize = dataOffset + stringDataSize;
        ByteBuffer buffer = ByteBuffer.allocate(fileSize).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(new byte[]{'d', 'e', 'x', '\n', '0', '3', '5', 0});
        buffer.position(32);
        buffer.putInt(fileSize);
        buffer.putInt(112);
        buffer.putInt(0x12345678);
        buffer.position(56);
        buffer.putInt(count).putInt(stringIdsOffset);
        buffer.putInt(count).putInt(typeIdsOffset);
        buffer.position(96);
        buffer.putInt(count).putInt(classDefsOffset);

        int cursor = dataOffset;
        for (int index = 0; index < count; index++) {
            buffer.putInt(stringIdsOffset + index * 4, cursor);
            buffer.putInt(typeIdsOffset + index * 4, index);
            buffer.putInt(classDefsOffset + index * 32, index);
            byte[] descriptor = descriptors.get(index);
            buffer.put(cursor, (byte) descriptor.length);
            cursor++;
            buffer.position(cursor);
            buffer.put(descriptor);
            cursor += descriptor.length;
            buffer.put(cursor++, (byte) 0);
        }
        return buffer.array();
    }

    private static void assertReason(Path archive, ModVerificationException.Reason reason)
            throws Exception {
        try {
            new RustedFabricModVerifier().verify(archive);
            throw new AssertionError("Expected verification reason " + reason);
        } catch (ModVerificationException expected) {
            require(expected.getReason() == reason,
                    "expected " + reason + " but got " + expected.getReason());
        }
    }

    private static void expectClassNotFound(ClassLoader loader, String name) {
        try {
            loader.loadClass(name);
            throw new AssertionError("Expected ClassNotFoundException for " + name);
        } catch (ClassNotFoundException expected) {
            // Expected: the recording loaders deliberately resolve no classes.
        }
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (java.util.stream.Stream<Path> paths = Files.walk(root)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException failure) {
                    throw new DeleteFailure(failure);
                }
            });
        } catch (DeleteFailure failure) {
            throw failure.cause;
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class RecordingClassLoader extends ClassLoader {
        private final List<String> requests = new ArrayList<>();

        private RecordingClassLoader() {
            super(null);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            requests.add(name);
            throw new ClassNotFoundException(name);
        }
    }

    private static final class DeleteFailure extends RuntimeException {
        private final IOException cause;

        private DeleteFailure(IOException cause) {
            super(cause);
            this.cause = cause;
        }
    }
}
