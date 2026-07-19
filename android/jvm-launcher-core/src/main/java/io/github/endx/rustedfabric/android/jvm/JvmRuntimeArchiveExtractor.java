package io.github.endx.rustedfabric.android.jvm;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;

/** Safely imports an ARM64 OpenJDK 17 ZIP or TAR.XZ runtime. */
public final class JvmRuntimeArchiveExtractor {
    private static final long MAX_EXPANDED_BYTES = 805_306_368L;
    private static final int MAX_FILES = 30_000;
    private static final int MAX_DEPTH = 40;

    private JvmRuntimeArchiveExtractor() {
    }

    public static Result extract(Path archive, Path destination, ProgressListener listener)
            throws IOException {
        if (archive == null || !Files.isRegularFile(archive)) {
            throw new IOException("Java runtime ZIP is not available");
        }
        if (destination == null || !Files.isDirectory(destination)) {
            throw new IOException("Private runtime destination is not available");
        }
        String archiveSha256 = sha256(archive);
        if (hasMagic(archive, new byte[]{'P', 'K'})) {
            return extractZip(archive, destination, listener, archiveSha256);
        }
        if (hasMagic(archive, new byte[]{(byte) 0xfd, '7', 'z', 'X', 'Z', 0x00})) {
            return extractTarXz(archive, destination, listener, archiveSha256);
        }
        throw new IOException("Java runtime archive must be ZIP or TAR.XZ");
    }

    private static Result extractZip(Path archive, Path destination, ProgressListener listener,
                                     String archiveSha256) throws IOException {
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            List<Item> items = inventory(zip);
            String prefix = selectRuntimeRoot(items);
            Set<String> outputs = new HashSet<>();
            long bytes = 0L;
            int files = 0;
            byte[] buffer = new byte[64 * 1024];
            for (Item item : items) {
                String relative = relative(item.path, prefix);
                if (relative == null || relative.isEmpty()) continue;
                Path output = checkedResolve(destination, relative);
                if (item.directory) {
                    Files.createDirectories(output);
                    continue;
                }
                if (!outputs.add(relative)) {
                    throw new IOException("Java runtime ZIP contains a duplicate entry: " + relative);
                }
                if (++files > MAX_FILES) throw new IOException("Java runtime ZIP contains too many files");
                if (item.size > MAX_EXPANDED_BYTES) {
                    throw new IOException("Java runtime ZIP contains an oversized entry: " + relative);
                }
                if (output.getParent() != null) Files.createDirectories(output.getParent());
                try (InputStream input = zip.getInputStream(item.entry);
                     OutputStream target = Files.newOutputStream(output)) {
                    int read;
                    while ((read = input.read(buffer)) >= 0) {
                        bytes += read;
                        if (bytes > MAX_EXPANDED_BYTES) {
                            throw new IOException("Expanded Java runtime exceeds the import limit");
                        }
                        target.write(buffer, 0, read);
                    }
                }
                if (listener != null) listener.onProgress(files, bytes, relative);
            }
            if (!JvmRuntimeProbe.inspect(destination, null).hasJava17()) {
                throw new IOException("Imported runtime is not usable: "
                        + JvmRuntimeProbe.runtimeIssue(destination));
            }
            return new Result(prefix, files, bytes, archiveSha256);
        }
    }

    private static Result extractTarXz(Path archive, Path destination, ProgressListener listener,
                                       String archiveSha256) throws IOException {
        List<TarItem> items = inventoryTarXz(archive);
        Set<String> regularFiles = new HashSet<>();
        for (TarItem item : items) if (item.regularFile) regularFiles.add(item.path);
        String prefix = selectRuntimeRoot(regularFiles);
        Set<String> outputs = new HashSet<>();
        long bytes = 0L;
        int files = 0;
        byte[] buffer = new byte[64 * 1024];
        try (TarArchiveInputStream tar = openTarXz(archive)) {
            TarArchiveEntry entry;
            while ((entry = tar.getNextEntry()) != null) {
                String path = archivePath(entry.getName());
                if (path == null) continue;
                String relative = relative(path, prefix);
                if (relative == null || relative.isEmpty()) continue;
                Path output = checkedResolve(destination, relative);
                if (entry.isDirectory()) {
                    Files.createDirectories(output);
                    continue;
                }
                if (!isRegularTarFile(entry)) continue;
                if (!outputs.add(relative)) {
                    throw new IOException("Java runtime TAR.XZ contains a duplicate entry: "
                            + relative);
                }
                if (++files > MAX_FILES) {
                    throw new IOException("Java runtime TAR.XZ contains too many files");
                }
                if (entry.getSize() < 0L || entry.getSize() > MAX_EXPANDED_BYTES) {
                    throw new IOException("Java runtime TAR.XZ contains an oversized entry: "
                            + relative);
                }
                if (output.getParent() != null) Files.createDirectories(output.getParent());
                try (OutputStream target = Files.newOutputStream(output)) {
                    long entryBytes = 0L;
                    int read;
                    while ((read = tar.read(buffer)) >= 0) {
                        entryBytes += read;
                        bytes += read;
                        if (entryBytes > entry.getSize() || bytes > MAX_EXPANDED_BYTES) {
                            throw new IOException("Expanded Java runtime exceeds the import limit");
                        }
                        target.write(buffer, 0, read);
                    }
                    if (entryBytes != entry.getSize()) {
                        throw new IOException("Truncated Java runtime entry: " + relative);
                    }
                }
                if (listener != null) listener.onProgress(files, bytes, relative);
            }
        }
        if (!JvmRuntimeProbe.inspect(destination, null).hasJava17()) {
            throw new IOException("Imported runtime is not usable: "
                    + JvmRuntimeProbe.runtimeIssue(destination));
        }
        return new Result(prefix, files, bytes, archiveSha256);
    }

    private static List<TarItem> inventoryTarXz(Path archive) throws IOException {
        List<TarItem> items = new ArrayList<>();
        try (TarArchiveInputStream tar = openTarXz(archive)) {
            TarArchiveEntry entry;
            while ((entry = tar.getNextEntry()) != null) {
                String path = archivePath(entry.getName());
                if (path == null) continue;
                items.add(new TarItem(path, isRegularTarFile(entry)));
                if (items.size() > MAX_FILES + 10_000) {
                    throw new IOException("Java runtime TAR.XZ contains too many entries");
                }
            }
        }
        return items;
    }

    private static TarArchiveInputStream openTarXz(Path archive) throws IOException {
        BufferedInputStream compressed = new BufferedInputStream(
                Files.newInputStream(archive), 64 * 1024);
        try {
            XZCompressorInputStream xz = XZCompressorInputStream.builder()
                    .setInputStream(compressed)
                    .setDecompressConcatenated(true)
                    .setMemoryLimitKiB(262_144)
                    .get();
            return new TarArchiveInputStream(xz);
        } catch (IOException | RuntimeException failure) {
            compressed.close();
            throw failure;
        }
    }

    private static boolean isRegularTarFile(TarArchiveEntry entry) {
        return entry.isFile() && !entry.isSymbolicLink() && !entry.isLink();
    }

    private static List<Item> inventory(ZipFile zip) throws IOException {
        List<Item> items = new ArrayList<>();
        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            items.add(new Item(entry, safePath(entry.getName()), entry.isDirectory(), entry.getSize()));
        }
        return items;
    }

    private static String selectRuntimeRoot(List<Item> items) throws IOException {
        Set<String> files = new HashSet<>();
        for (Item item : items) {
            if (!item.directory) files.add(item.path);
        }
        return selectRuntimeRoot(files);
    }

    private static String selectRuntimeRoot(Set<String> files) throws IOException {
        Set<String> candidates = new LinkedHashSet<>();
        for (String path : files) {
            if ("release".equals(path) || path.endsWith("/release")) {
                int separator = path.lastIndexOf('/');
                candidates.add(separator < 0 ? "" : path.substring(0, separator));
            }
        }
        List<String> viable = new ArrayList<>();
        for (String candidate : candidates) {
            String base = candidate.isEmpty() ? "" : candidate + "/";
            if (files.contains(base + "release")
                    && files.contains(base + "lib/server/libjvm.so")
                    && files.contains(base + "lib/libjava.so")
                    && files.contains(base + "lib/modules")) {
                viable.add(candidate);
            }
        }
        if (viable.size() != 1) {
            throw new IOException(viable.isEmpty()
                    ? "ZIP does not contain one complete Java 17 runtime"
                    : "ZIP contains multiple Java runtime roots");
        }
        return viable.get(0);
    }

    private static String relative(String path, String prefix) {
        if (prefix.isEmpty()) return path;
        String expected = prefix + "/";
        return path.startsWith(expected) ? path.substring(expected.length()) : null;
    }

    private static String safePath(String raw) throws IOException {
        if (raw == null || raw.isEmpty() || raw.indexOf('\\') >= 0 || raw.indexOf('\0') >= 0
                || raw.startsWith("/") || raw.matches("^[A-Za-z]:.*")) {
            throw new IOException("Java runtime ZIP contains an unsafe entry name");
        }
        String value = raw.endsWith("/") ? raw.substring(0, raw.length() - 1) : raw;
        String[] segments = value.split("/", -1);
        if (segments.length > MAX_DEPTH) throw new IOException("Java runtime ZIP nesting is too deep");
        for (String segment : segments) {
            if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)) {
                throw new IOException("Java runtime ZIP contains path traversal");
            }
        }
        return value;
    }

    private static String archivePath(String raw) throws IOException {
        if (".".equals(raw) || "./".equals(raw)) return null;
        while (raw != null && raw.startsWith("./")) raw = raw.substring(2);
        if (raw == null || raw.isEmpty()) return null;
        return safePath(raw);
    }

    private static Path checkedResolve(Path root, String relative) throws IOException {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path result = normalizedRoot.resolve(relative).normalize();
        if (!result.startsWith(normalizedRoot)) {
            throw new IOException("Java runtime entry escaped the private directory");
        }
        return result;
    }

    private static String sha256(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(path)) {
                byte[] buffer = new byte[64 * 1024];
                int read;
                while ((read = input.read(buffer)) >= 0) digest.update(buffer, 0, read);
            }
            StringBuilder value = new StringBuilder(64);
            for (byte item : digest.digest()) value.append(String.format("%02x", item & 0xff));
            return value.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    private static boolean hasMagic(Path archive, byte[] expected) throws IOException {
        try (InputStream input = Files.newInputStream(archive)) {
            for (byte value : expected) {
                int actual = input.read();
                if (actual < 0 || actual != (value & 0xff)) return false;
            }
            return true;
        }
    }

    public interface ProgressListener {
        void onProgress(int files, long bytes, String currentPath);
    }

    public static final class Result {
        private final String archiveRoot;
        private final int files;
        private final long bytes;
        private final String archiveSha256;

        Result(String archiveRoot, int files, long bytes, String archiveSha256) {
            this.archiveRoot = archiveRoot;
            this.files = files;
            this.bytes = bytes;
            this.archiveSha256 = archiveSha256;
        }

        public String archiveRoot() { return archiveRoot; }
        public int files() { return files; }
        public long bytes() { return bytes; }
        public String archiveSha256() { return archiveSha256; }
    }

    private static final class Item {
        private final ZipEntry entry;
        private final String path;
        private final boolean directory;
        private final long size;

        private Item(ZipEntry entry, String path, boolean directory, long size) {
            this.entry = entry;
            this.path = path;
            this.directory = directory;
            this.size = size;
        }
    }

    private static final class TarItem {
        private final String path;
        private final boolean regularFile;

        private TarItem(String path, boolean regularFile) {
            this.path = path;
            this.regularFile = regularFile;
        }
    }
}
