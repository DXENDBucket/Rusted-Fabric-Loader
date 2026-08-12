package io.github.endx.rustedfabricloader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/** Safely stages native INI content embedded in a packaged Java mod. */
final class PackagedNativeContentBridge {
    private static final String TARGET_PREFIX = "rfl-java-";
    private static final String MARKER = ".rusted-fabric-managed-native-package";
    private static final String MARKER_VERSION = "1";
    private static final int MAX_FILES = 20_000;
    private static final long MAX_BYTES = 536_870_912L;

    private PackagedNativeContentBridge() { }

    static void sync(String id, Path archive, String declaredRoot, Path unitsRoot)
            throws IOException {
        Path source = archive.toRealPath();
        if (!Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(source)) {
            throw new IOException("Java mod archive is not a normal file: " + archive);
        }
        String archiveRoot = validateRoot(declaredRoot);
        String archiveSha256 = sha256(source);
        Path parent = unitsRoot.toAbsolutePath().normalize();
        Files.createDirectories(parent);
        Path target = parent.resolve(TARGET_PREFIX + id).normalize();
        if (!target.getParent().equals(parent)) {
            throw new IOException("native content target escaped units root for " + id);
        }
        Path marker = target.resolve(MARKER);
        boolean targetExists = Files.exists(target, LinkOption.NOFOLLOW_LINKS);
        if (targetExists && (!Files.isDirectory(target, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(target) || !validOwnership(marker, id))) {
            throw new IOException("refusing to replace unmanaged native mod directory: " + target);
        }
        if (targetExists && markerLines(id, archiveRoot, archiveSha256).equals(
                Files.readAllLines(marker, StandardCharsets.UTF_8))
                && Files.isRegularFile(target.resolve("mod-info.txt"),
                LinkOption.NOFOLLOW_LINKS)) {
            return;
        }

        Path staging = parent.resolve("." + target.getFileName() + ".staging-" + UUID.randomUUID());
        Path backup = parent.resolve("." + target.getFileName() + ".backup-" + UUID.randomUUID());
        boolean backedUp = false;
        try {
            Files.createDirectory(staging);
            extract(source, archiveRoot, staging);
            Files.write(staging.resolve(MARKER), markerLines(id, archiveRoot, archiveSha256),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE);
            if (targetExists) {
                move(target, backup);
                backedUp = true;
            }
            move(staging, target);
            if (backedUp) {
                try {
                    deleteTree(backup);
                } catch (IOException ignored) {
                    // The new managed content is already active. A hidden backup is safer than
                    // rejecting the mod after a successful replacement.
                }
            }
        } catch (IOException failure) {
            if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)
                    && backedUp && Files.exists(backup, LinkOption.NOFOLLOW_LINKS)) {
                try {
                    move(backup, target);
                } catch (IOException restoreFailure) {
                    failure.addSuppressed(restoreFailure);
                }
            }
            try {
                if (Files.exists(staging, LinkOption.NOFOLLOW_LINKS)) deleteTree(staging);
            } catch (IOException cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }
    }

    static void removeOrphans(Path unitsRoot, Set<String> activeIds) throws IOException {
        if (!Files.isDirectory(unitsRoot, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(unitsRoot)) return;
        try (java.nio.file.DirectoryStream<Path> entries = Files.newDirectoryStream(
                unitsRoot, TARGET_PREFIX + "*")) {
            for (Path entry : entries) {
                String name = entry.getFileName().toString();
                String id = name.substring(TARGET_PREFIX.length());
                if (activeIds.contains(id)) continue;
                if (Files.isDirectory(entry, LinkOption.NOFOLLOW_LINKS)
                        && !Files.isSymbolicLink(entry)
                        && validOwnership(entry.resolve(MARKER), id)) {
                    deleteTree(entry);
                }
            }
        }
    }

    private static void extract(Path archive, String root, Path staging) throws IOException {
        String prefix = root + "/";
        Set<Path> extracted = new HashSet<Path>();
        int fileCount = 0;
        long totalBytes = 0L;
        byte[] buffer = new byte[64 * 1024];
        try (JarFile jar = new JarFile(archive.toFile())) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!name.startsWith(prefix)) continue;
                if (name.indexOf('\\') >= 0 || name.startsWith("/")) {
                    throw new IOException("invalid native content archive path: " + name);
                }
                String relativeName = name.substring(prefix.length());
                if (relativeName.isEmpty()) continue;
                final Path relative;
                try {
                    relative = Paths.get(relativeName).normalize();
                } catch (RuntimeException invalid) {
                    throw new IOException("invalid native content archive path: " + name, invalid);
                }
                if (relative.isAbsolute() || relative.getNameCount() == 0
                        || "..".equals(relative.getName(0).toString())
                        || MARKER.equals(relative.toString())) {
                    throw new IOException("native content archive path escaped its root: " + name);
                }
                Path destination = staging.resolve(relative).normalize();
                if (!destination.startsWith(staging)) {
                    throw new IOException("native content archive path escaped staging: " + name);
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(destination);
                    continue;
                }
                if (!extracted.add(relative)) {
                    throw new IOException("duplicate native content archive path: " + name);
                }
                if (++fileCount > MAX_FILES) {
                    throw new IOException("native content contains more than " + MAX_FILES + " files");
                }
                Files.createDirectories(destination.getParent());
                try (InputStream input = jar.getInputStream(entry);
                     OutputStream output = Files.newOutputStream(destination,
                             StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                    int read;
                    while ((read = input.read(buffer)) >= 0) {
                        if (read == 0) continue;
                        totalBytes += read;
                        if (totalBytes > MAX_BYTES) {
                            throw new IOException("native content expands beyond " + MAX_BYTES
                                    + " bytes");
                        }
                        output.write(buffer, 0, read);
                    }
                }
            }
        }
        if (!Files.isRegularFile(staging.resolve("mod-info.txt"),
                LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("packaged native content is missing mod-info.txt below " + root);
        }
    }

    private static String validateRoot(String value) throws IOException {
        String root = value == null ? "" : value.trim();
        while (root.endsWith("/")) root = root.substring(0, root.length() - 1);
        if (root.isEmpty() || root.startsWith("/") || root.indexOf('\\') >= 0) {
            throw new IOException("native content root must be a relative archive directory");
        }
        for (String segment : root.split("/", -1)) {
            if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)) {
                throw new IOException("invalid native content root: " + value);
            }
        }
        return root;
    }

    private static String sha256(Path file) throws IOException {
        final MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IOException("SHA-256 is unavailable", impossible);
        }
        byte[] buffer = new byte[64 * 1024];
        try (InputStream input = Files.newInputStream(file)) {
            int read;
            while ((read = input.read(buffer)) >= 0) {
                if (read > 0) digest.update(buffer, 0, read);
            }
        }
        StringBuilder result = new StringBuilder(64);
        for (byte value : digest.digest()) result.append(String.format("%02x", value & 0xff));
        return result.toString();
    }

    private static boolean validOwnership(Path marker, String id) throws IOException {
        if (!Files.isRegularFile(marker, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(marker)) return false;
        List<String> lines = Files.readAllLines(marker, StandardCharsets.UTF_8);
        return lines.contains("version=" + MARKER_VERSION) && lines.contains("id=" + id);
    }

    private static List<String> markerLines(String id, String root, String hash) {
        ArrayList<String> lines = new ArrayList<String>();
        lines.add("version=" + MARKER_VERSION);
        lines.add("id=" + id);
        lines.add("root=" + root);
        lines.add("archiveSha256=" + hash);
        return lines;
    }

    private static void move(Path source, Path destination) throws IOException {
        try {
            Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException unsupported) {
            Files.move(source, destination);
        }
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root, LinkOption.NOFOLLOW_LINKS)) return;
        ArrayList<Path> paths = new ArrayList<Path>();
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override public FileVisitResult visitFile(
                    Path file, BasicFileAttributes attributes) {
                paths.add(file);
                return FileVisitResult.CONTINUE;
            }

            @Override public FileVisitResult postVisitDirectory(Path directory, IOException error)
                    throws IOException {
                if (error != null) throw error;
                paths.add(directory);
                return FileVisitResult.CONTINUE;
            }
        });
        Collections.sort(paths, Comparator.comparingInt(Path::getNameCount).reversed());
        for (Path path : paths) Files.deleteIfExists(path);
    }
}
