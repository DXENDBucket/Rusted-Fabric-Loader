package io.github.endx.rustedfabricloader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Stages editable native INI content declared by exploded Java-mod workspaces.
 * The public no-argument entry point is intentionally reflection-friendly for the API reload path.
 */
public final class NativeContentDevelopmentBridge {
    private static final String IDS_PROPERTY = "rusted.javamodsDevWorkspaceIds";
    private static final String SOURCE_PREFIX = "rusted.javamodsDevNativeContent.";
    private static final String TARGET_PREFIX = "rusted.javamodsDevNativeContentTarget.";
    private static final String MARKER = ".rusted-fabric-managed-native-content";
    private static final String MARKER_VERSION = "1";

    private NativeContentDevelopmentBridge() { }

    /** Synchronizes all native content roots selected for this process. */
    public static void syncAll() throws IOException {
        for (String candidate : System.getProperty(IDS_PROPERTY, "").split(",")) {
            String id = candidate.trim();
            if (id.isEmpty()) continue;
            String sourceValue = System.getProperty(SOURCE_PREFIX + id, "").trim();
            String targetValue = System.getProperty(TARGET_PREFIX + id, "").trim();
            if (sourceValue.isEmpty() && targetValue.isEmpty()) continue;
            if (sourceValue.isEmpty() || targetValue.isEmpty()) {
                throw new IOException("incomplete native content workspace properties for " + id);
            }
            sync(id, Paths.get(sourceValue), Paths.get(targetValue));
        }
    }

    static void sync(String id, Path source, Path target) throws IOException {
        Path realSource = source.toRealPath();
        if (!Files.isDirectory(realSource, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(realSource)) {
            throw new IOException("native content source is not a normal directory: " + source);
        }
        Path checkedTarget = target.toAbsolutePath().normalize();
        Files.createDirectories(checkedTarget.getParent());
        Path marker = checkedTarget.resolve(MARKER);
        if (Files.exists(checkedTarget, LinkOption.NOFOLLOW_LINKS)) {
            if (!Files.isDirectory(checkedTarget, LinkOption.NOFOLLOW_LINKS)
                    || Files.isSymbolicLink(checkedTarget)
                    || !validMarker(marker, id)) {
                throw new IOException("refusing to replace unmanaged native mod directory: "
                        + checkedTarget);
            }
        } else {
            Files.createDirectory(checkedTarget);
            writeMarker(marker, id);
        }

        final Set<Path> retained = new LinkedHashSet<Path>();
        retained.add(checkedTarget.relativize(marker));
        Files.walkFileTree(realSource, new SimpleFileVisitor<Path>() {
            @Override public FileVisitResult preVisitDirectory(
                    Path directory, BasicFileAttributes attributes) throws IOException {
                if (Files.isSymbolicLink(directory)) {
                    throw new IOException("symbolic links are not allowed in native content: "
                            + directory);
                }
                Path relative = realSource.relativize(directory);
                Path destination = checkedTarget.resolve(relative).normalize();
                if (!destination.startsWith(checkedTarget)) {
                    throw new IOException("native content path escaped target: " + directory);
                }
                if (Files.isSymbolicLink(destination)) {
                    throw new IOException("symbolic link found in managed native content target: "
                            + destination);
                }
                retained.add(relative);
                Files.createDirectories(destination);
                return FileVisitResult.CONTINUE;
            }

            @Override public FileVisitResult visitFile(
                    Path file, BasicFileAttributes attributes) throws IOException {
                if (!attributes.isRegularFile() || Files.isSymbolicLink(file)) {
                    throw new IOException("only regular files are allowed in native content: "
                            + file);
                }
                Path relative = realSource.relativize(file);
                if (MARKER.equals(relative.toString())) {
                    throw new IOException("native content uses reserved file name " + MARKER);
                }
                Path destination = checkedTarget.resolve(relative).normalize();
                if (!destination.startsWith(checkedTarget)) {
                    throw new IOException("native content path escaped target: " + file);
                }
                if (Files.isSymbolicLink(destination)) {
                    throw new IOException("symbolic link found in managed native content target: "
                            + destination);
                }
                retained.add(relative);
                Files.copy(file, destination, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
        removeStale(checkedTarget, retained);
        writeMarker(marker, id);
    }

    static void removeOrphans(Path unitsRoot, Set<String> activeWorkspaceIds)
            throws IOException {
        if (!Files.isDirectory(unitsRoot, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(unitsRoot)) return;
        try (java.nio.file.DirectoryStream<Path> entries = Files.newDirectoryStream(
                unitsRoot, "rfl-dev-*")) {
            for (Path entry : entries) {
                String name = entry.getFileName().toString();
                String id = name.substring("rfl-dev-".length());
                if (activeWorkspaceIds.contains(id)) continue;
                Path marker = entry.resolve(MARKER);
                if (Files.isDirectory(entry, LinkOption.NOFOLLOW_LINKS)
                        && !Files.isSymbolicLink(entry) && validMarker(marker, id)) {
                    deleteManagedTree(entry);
                }
            }
        }
    }

    private static void deleteManagedTree(Path root) throws IOException {
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

    private static void removeStale(Path target, Set<Path> retained) throws IOException {
        ArrayList<Path> paths = new ArrayList<Path>();
        Files.walkFileTree(target, new SimpleFileVisitor<Path>() {
            @Override public FileVisitResult visitFile(
                    Path file, BasicFileAttributes attributes) {
                paths.add(file);
                return FileVisitResult.CONTINUE;
            }

            @Override public FileVisitResult postVisitDirectory(Path directory, IOException error)
                    throws IOException {
                if (error != null) throw error;
                if (!directory.equals(target)) paths.add(directory);
                return FileVisitResult.CONTINUE;
            }
        });
        Collections.sort(paths, Comparator.comparingInt(Path::getNameCount).reversed());
        for (Path path : paths) {
            Path relative = target.relativize(path);
            if (!retained.contains(relative)) Files.deleteIfExists(path);
        }
    }

    private static boolean validMarker(Path marker, String id) throws IOException {
        if (!Files.isRegularFile(marker, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(marker)) return false;
        return Files.readAllLines(marker, StandardCharsets.UTF_8).equals(
                markerLines(id));
    }

    private static void writeMarker(Path marker, String id) throws IOException {
        Files.write(marker, markerLines(id), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
    }

    private static java.util.List<String> markerLines(String id) {
        ArrayList<String> lines = new ArrayList<String>();
        lines.add("version=" + MARKER_VERSION);
        lines.add("id=" + id);
        return lines;
    }
}
