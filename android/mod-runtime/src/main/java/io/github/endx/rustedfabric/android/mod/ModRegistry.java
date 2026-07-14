package io.github.endx.rustedfabric.android.mod;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Pattern;

import io.github.endx.rustedfabricapi.api.multiplayer.MultiplayerMod;

/** Atomic app-private registry for imported Android mod archives. */
public final class ModRegistry {
    private static final Object PROCESS_LOCK = new Object();
    private static final Pattern MOD_ID = Pattern.compile("[a-z][a-z0-9_-]{0,63}");
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");

    private final Path root;
    private final Path archives;
    private final Path records;
    private final RustedFabricModVerifier verifier = new RustedFabricModVerifier();

    public ModRegistry(Path root) {
        this.root = root.toAbsolutePath().normalize();
        this.archives = this.root.resolve("archives");
        this.records = this.root.resolve("registry");
    }

    public Record install(VerifiedModArchive verified) throws IOException, ModVerificationException {
        synchronized (PROCESS_LOCK) {
            ensureDirectories();
            RustedFabricModMetadata metadata = verified.getMetadata();
            Path destination = archives.resolve(verified.getArchiveSha256() + ".rfmod");
            if (!isMatchingArchive(destination, verified.getArchiveSha256())) {
                Path temporary = archives.resolve(".import-" + UUID.randomUUID() + ".tmp");
                try {
                    Files.copy(verified.getArchivePath(), temporary,
                            StandardCopyOption.REPLACE_EXISTING);
                    VerifiedModArchive copied = verifier.verify(temporary);
                    if (!verified.getArchiveSha256().equals(copied.getArchiveSha256())) {
                        throw new IOException("Imported mod changed while it was being copied");
                    }
                    try (FileChannel channel = FileChannel.open(temporary,
                            StandardOpenOption.WRITE)) {
                        channel.force(true);
                    }
                    atomicMove(temporary, destination);
                } finally {
                    Files.deleteIfExists(temporary);
                }
            }

            Record previous = readRecord(recordPath(metadata.getId()));
            Record record = new Record(metadata.getId(), metadata.getName(), metadata.getVersion(),
                    metadata.getEntrypoint(), metadata.getApiVersion(),
                    metadata.getMappingProfiles(), metadata.getCapabilities(),
                    metadata.getMultiplayer(),
                    verified.getArchiveSha256(), verified.getDexSha256(),
                    previous != null && previous.isEnabled(), System.currentTimeMillis());
            writeRecord(record);
            if (previous != null && !previous.getArchiveSha256().equals(record.getArchiveSha256())) {
                removeArchiveIfUnused(previous.getArchiveSha256());
            }
            return record;
        }
    }

    public List<Record> list() throws IOException {
        synchronized (PROCESS_LOCK) {
            ensureDirectories();
            List<Record> result = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(records, "*.properties")) {
                for (Path path : stream) {
                    Record record = readRecord(path);
                    if (record != null) {
                        result.add(record);
                    }
                }
            }
            result.sort(Comparator.comparing(Record::getName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(Record::getId));
            return Collections.unmodifiableList(result);
        }
    }

    public Optional<Record> find(String id) throws IOException {
        synchronized (PROCESS_LOCK) {
            if (!validId(id)) {
                return Optional.empty();
            }
            ensureDirectories();
            return Optional.ofNullable(readRecord(recordPath(id)));
        }
    }

    public Record setEnabled(String id, boolean enabled) throws IOException {
        synchronized (PROCESS_LOCK) {
            ensureDirectories();
            Record current = requireRecord(id);
            Record updated = current.withEnabled(enabled);
            writeRecord(updated);
            return updated;
        }
    }

    public boolean remove(String id) throws IOException {
        synchronized (PROCESS_LOCK) {
            ensureDirectories();
            if (!validId(id)) {
                return false;
            }
            Record current = readRecord(recordPath(id));
            if (current == null) {
                return false;
            }
            Files.deleteIfExists(recordPath(id));
            removeArchiveIfUnused(current.getArchiveSha256());
            return true;
        }
    }

    public Path archivePath(Record record) {
        return archives.resolve(record.getArchiveSha256() + ".rfmod").normalize();
    }

    private void ensureDirectories() throws IOException {
        Files.createDirectories(archives);
        Files.createDirectories(records);
    }

    private boolean isMatchingArchive(Path path, String expectedSha256) {
        if (!Files.isRegularFile(path)) {
            return false;
        }
        try {
            return expectedSha256.equals(verifier.verify(path).getArchiveSha256());
        } catch (ModVerificationException invalid) {
            return false;
        }
    }

    private Record requireRecord(String id) throws IOException {
        if (!validId(id)) {
            throw new IOException("Invalid mod id");
        }
        Record record = readRecord(recordPath(id));
        if (record == null) {
            throw new IOException("Mod is not installed: " + id);
        }
        return record;
    }

    private Path recordPath(String id) {
        return records.resolve(id + ".properties");
    }

    private void writeRecord(Record record) throws IOException {
        Properties values = new Properties();
        values.setProperty("schemaVersion", "1");
        values.setProperty("id", record.getId());
        values.setProperty("name", record.getName());
        values.setProperty("version", record.getVersion());
        values.setProperty("entrypoint", record.getEntrypoint());
        values.setProperty("apiVersion", record.getApiVersion());
        values.setProperty("mappingProfiles", String.join(",", record.getMappingProfiles()));
        values.setProperty("capabilities", String.join(",", record.getCapabilities()));
        values.setProperty("multiplayerMode", record.getMultiplayer().mode().wireName());
        if (record.getMultiplayer().mode() == MultiplayerMod.Mode.REQUIRED) {
            values.setProperty("multiplayerProtocol", record.getMultiplayer().protocol());
            values.setProperty("multiplayerSyncHash", record.getMultiplayer().syncHash());
        }
        values.setProperty("archiveSha256", record.getArchiveSha256());
        values.setProperty("dexSha256", record.getDexSha256());
        values.setProperty("enabled", Boolean.toString(record.isEnabled()));
        values.setProperty("installedAt", Long.toString(record.getInstalledAt()));

        Path temporary = records.resolve("." + record.getId() + "-" + UUID.randomUUID() + ".tmp");
        try {
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE)) {
                OutputStream output = Channels.newOutputStream(channel);
                values.store(output, "Rusted Fabric private mod registry");
                output.flush();
                channel.force(true);
            }
            atomicMove(temporary, recordPath(record.getId()));
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private Record readRecord(Path path) throws IOException {
        if (!Files.isRegularFile(path)) {
            return null;
        }
        try {
            Properties values = new Properties();
            try (InputStream input = Files.newInputStream(path)) {
                values.load(input);
            }
            if (!"1".equals(values.getProperty("schemaVersion"))) {
                return null;
            }
            String id = required(values, "id");
            String archiveSha = required(values, "archiveSha256").toLowerCase(Locale.ROOT);
            String dexSha = required(values, "dexSha256").toLowerCase(Locale.ROOT);
            if (!validId(id) || !SHA256.matcher(archiveSha).matches()
                    || !SHA256.matcher(dexSha).matches()) {
                return null;
            }
            String version = required(values, "version");
            MultiplayerMod multiplayer = new MultiplayerMod(id, version,
                    MultiplayerMod.Mode.parse(values.getProperty("multiplayerMode", "unsafe")),
                    values.getProperty("multiplayerProtocol", ""),
                    values.getProperty("multiplayerSyncHash", ""));
            return new Record(id, required(values, "name"), version,
                    required(values, "entrypoint"), required(values, "apiVersion"),
                    csv(required(values, "mappingProfiles")),
                    csv(values.getProperty("capabilities", "")), multiplayer, archiveSha, dexSha,
                    Boolean.parseBoolean(values.getProperty("enabled", "false")),
                    Long.parseLong(values.getProperty("installedAt", "0")));
        } catch (IllegalArgumentException malformed) {
            return null;
        }
    }

    private void removeArchiveIfUnused(String sha256) throws IOException {
        for (Record record : list()) {
            if (sha256.equals(record.getArchiveSha256())) {
                return;
            }
        }
        Files.deleteIfExists(archives.resolve(sha256 + ".rfmod"));
    }

    private static void atomicMove(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException unsupported) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String required(Properties values, String key) {
        String value = values.getProperty(key);
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Missing registry value");
        }
        return value;
    }

    private static List<String> csv(String value) {
        if (value == null || value.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(Arrays.asList(value.split(",", -1)));
    }

    private static boolean validId(String id) {
        return id != null && MOD_ID.matcher(id).matches();
    }

    public static final class Record {
        private final String id;
        private final String name;
        private final String version;
        private final String entrypoint;
        private final String apiVersion;
        private final List<String> mappingProfiles;
        private final List<String> capabilities;
        private final MultiplayerMod multiplayer;
        private final String archiveSha256;
        private final String dexSha256;
        private final boolean enabled;
        private final long installedAt;

        private Record(String id, String name, String version, String entrypoint,
                       String apiVersion, List<String> mappingProfiles,
                       List<String> capabilities, MultiplayerMod multiplayer,
                       String archiveSha256, String dexSha256,
                       boolean enabled, long installedAt) {
            this.id = id;
            this.name = name;
            this.version = version;
            this.entrypoint = entrypoint;
            this.apiVersion = apiVersion;
            this.mappingProfiles = immutable(mappingProfiles);
            this.capabilities = immutable(capabilities);
            this.multiplayer = multiplayer;
            this.archiveSha256 = archiveSha256;
            this.dexSha256 = dexSha256;
            this.enabled = enabled;
            this.installedAt = installedAt;
        }

        private Record withEnabled(boolean value) {
            return new Record(id, name, version, entrypoint, apiVersion, mappingProfiles,
                    capabilities, multiplayer, archiveSha256, dexSha256, value, installedAt);
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getVersion() { return version; }
        public String getEntrypoint() { return entrypoint; }
        public String getApiVersion() { return apiVersion; }
        public List<String> getMappingProfiles() { return mappingProfiles; }
        public List<String> getCapabilities() { return capabilities; }
        public MultiplayerMod getMultiplayer() { return multiplayer; }
        public String getArchiveSha256() { return archiveSha256; }
        public String getDexSha256() { return dexSha256; }
        public boolean isEnabled() { return enabled; }
        public long getInstalledAt() { return installedAt; }

        private static List<String> immutable(List<String> values) {
            return Collections.unmodifiableList(new ArrayList<>(values));
        }
    }
}
