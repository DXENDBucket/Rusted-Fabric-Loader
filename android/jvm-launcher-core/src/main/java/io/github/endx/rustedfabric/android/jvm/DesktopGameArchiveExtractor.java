package io.github.endx.rustedfabric.android.jvm;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Selectively extracts a user-owned desktop installation from a ZIP archive. */
public final class DesktopGameArchiveExtractor {
    private static final long MAX_EXPANDED_BYTES = 1_610_612_736L;
    private static final int MAX_FILES = 50_000;
    private static final int MAX_DEPTH = 32;
    private static final long RATIO_CHECK_FLOOR = 64L * 1024L * 1024L;
    private static final long MAX_EXPANSION_RATIO = 50L;

    private DesktopGameArchiveExtractor() {
    }

    public static Result extract(Path archive, Path destination, ProgressListener listener)
            throws IOException {
        if (archive == null || !Files.isRegularFile(archive)) {
            throw new IOException("Desktop game ZIP is not available");
        }
        if (destination == null || !Files.isDirectory(destination)) {
            throw new IOException("Private import destination is not available");
        }
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            List<EntryPath> entries = inventory(zip);
            String prefix = selectGameRoot(entries);
            Set<String> destinations = new HashSet<>();
            long expandedBytes = 0L;
            int files = 0;
            byte[] buffer = new byte[64 * 1024];
            for (EntryPath item : entries) {
                String relative = relativeToPrefix(item.path, prefix);
                if (relative == null || relative.isEmpty() || !portablePath(relative, item.directory)) {
                    continue;
                }
                Path output = checkedResolve(destination, relative);
                if (item.directory) {
                    Files.createDirectories(output);
                    continue;
                }
                if (!destinations.add(relative)) {
                    throw new IOException("Desktop game ZIP contains a duplicate entry: " + relative);
                }
                if (++files > MAX_FILES) throw new IOException("Desktop game ZIP contains too many files");
                if (item.declaredSize > MAX_EXPANDED_BYTES) {
                    throw new IOException("Desktop game ZIP contains an oversized entry: " + relative);
                }
                Path parent = output.getParent();
                if (parent != null) Files.createDirectories(parent);
                try (InputStream input = zip.getInputStream(item.entry);
                     OutputStream target = Files.newOutputStream(output)) {
                    int read;
                    while ((read = input.read(buffer)) >= 0) {
                        expandedBytes += read;
                        if (expandedBytes > MAX_EXPANDED_BYTES) {
                            throw new IOException("Expanded desktop game exceeds the import limit");
                        }
                        target.write(buffer, 0, read);
                    }
                }
                if (listener != null) listener.onProgress(files, expandedBytes, relative);
            }
            long archiveBytes = Files.size(archive);
            if (expandedBytes > RATIO_CHECK_FLOOR && archiveBytes > 0
                    && expandedBytes / archiveBytes > MAX_EXPANSION_RATIO) {
                throw new IOException("Desktop game ZIP has an unsafe expansion ratio");
            }
            DesktopGameInspection inspection = DesktopGameLayout.inspect(destination);
            if (!inspection.isImportable()) {
                throw new IOException("Desktop game ZIP is incomplete: " + inspection.errors());
            }
            return new Result(prefix, files, expandedBytes, inspection.warnings());
        }
    }

    private static List<EntryPath> inventory(ZipFile zip) throws IOException {
        List<EntryPath> entries = new ArrayList<>();
        Enumeration<? extends ZipEntry> source = zip.entries();
        while (source.hasMoreElements()) {
            ZipEntry entry = source.nextElement();
            String path = safeArchivePath(entry.getName());
            entries.add(new EntryPath(entry, path, entry.isDirectory(), entry.getSize()));
        }
        return entries;
    }

    private static String selectGameRoot(List<EntryPath> entries) throws IOException {
        Set<String> candidates = new LinkedHashSet<>();
        Set<String> files = new HashSet<>();
        for (EntryPath entry : entries) {
            if (!entry.directory) files.add(entry.path);
            if (!entry.directory && (DesktopGameLayout.GAME_JAR.equals(entry.path)
                    || entry.path.endsWith("/" + DesktopGameLayout.GAME_JAR))) {
                int separator = entry.path.lastIndexOf('/');
                candidates.add(separator < 0 ? "" : entry.path.substring(0, separator));
            }
        }
        List<String> viable = new ArrayList<>();
        for (String candidate : candidates) {
            String base = candidate.isEmpty() ? "" : candidate + "/";
            if (files.contains(base + DesktopGameLayout.GAME_JAR)
                    && files.contains(base + "libs/lwjgl.jar")
                    && files.contains(base + "libs/slick.jar")
                    && files.contains(base + "libs/jinput.jar")
                    && hasChild(entries, base + "assets/")
                    && hasChild(entries, base + "res/")) {
                viable.add(candidate);
            }
        }
        if (viable.size() != 1) {
            throw new IOException(viable.isEmpty()
                    ? "ZIP does not contain one complete Rusted Warfare desktop directory"
                    : "ZIP contains multiple complete desktop game directories");
        }
        return viable.get(0);
    }

    private static boolean hasChild(List<EntryPath> entries, String prefix) {
        for (EntryPath entry : entries) {
            if (entry.path.startsWith(prefix) && entry.path.length() > prefix.length()) return true;
        }
        return false;
    }

    private static String relativeToPrefix(String path, String prefix) {
        if (prefix.isEmpty()) return path;
        String expected = prefix + "/";
        return path.startsWith(expected) ? path.substring(expected.length()) : null;
    }

    private static boolean portablePath(String relative, boolean directory) {
        int separator = relative.indexOf('/');
        String root = separator < 0 ? relative : relative.substring(0, separator);
        if (DesktopGameLayout.GAME_JAR.equals(root)) {
            return !directory && separator < 0;
        }
        if ("libs".equals(root)) {
            return directory || relative.toLowerCase(Locale.ROOT).endsWith(".jar");
        }
        return "assets".equals(root) || "res".equals(root) || "font".equals(root);
    }

    private static String safeArchivePath(String raw) throws IOException {
        if (raw == null || raw.isEmpty() || raw.indexOf('\\') >= 0 || raw.indexOf('\0') >= 0
                || raw.startsWith("/") || raw.matches("^[A-Za-z]:.*")) {
            throw new IOException("ZIP contains an unsafe entry name");
        }
        boolean directory = raw.endsWith("/");
        String trimmed = directory ? raw.substring(0, raw.length() - 1) : raw;
        String[] segments = trimmed.split("/", -1);
        if (segments.length > MAX_DEPTH) throw new IOException("ZIP entry nesting is too deep");
        for (String segment : segments) {
            if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)) {
                throw new IOException("ZIP contains a path traversal or ambiguous entry");
            }
        }
        return trimmed;
    }

    private static Path checkedResolve(Path root, String relative) throws IOException {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path result = normalizedRoot.resolve(relative).normalize();
        if (!result.startsWith(normalizedRoot)) {
            throw new IOException("ZIP entry escaped the private import directory");
        }
        return result;
    }

    public interface ProgressListener {
        void onProgress(int files, long expandedBytes, String currentPath);
    }

    public static final class Result {
        private final String archiveRoot;
        private final int files;
        private final long bytes;
        private final List<String> warnings;

        Result(String archiveRoot, int files, long bytes, List<String> warnings) {
            this.archiveRoot = archiveRoot;
            this.files = files;
            this.bytes = bytes;
            this.warnings = Collections.unmodifiableList(new ArrayList<>(warnings));
        }

        public String archiveRoot() { return archiveRoot; }
        public int files() { return files; }
        public long bytes() { return bytes; }
        public List<String> warnings() { return warnings; }
    }

    private static final class EntryPath {
        private final ZipEntry entry;
        private final String path;
        private final boolean directory;
        private final long declaredSize;

        private EntryPath(ZipEntry entry, String path, boolean directory, long declaredSize) {
            this.entry = entry;
            this.path = path;
            this.directory = directory;
            this.declaredSize = declaredSize;
        }
    }
}
