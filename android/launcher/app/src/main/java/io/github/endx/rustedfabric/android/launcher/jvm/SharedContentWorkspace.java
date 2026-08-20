package io.github.endx.rustedfabric.android.launcher.jvm;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.github.endx.rustedfabric.android.jvm.ManagedContentLibrary;

/** Public, file-manager-accessible content directories used directly by the desktop JVM. */
public final class SharedContentWorkspace {
    public static final String ROOT_NAME = "rustedWarfare";

    private SharedContentWorkspace() {
    }

    public static boolean hasStorageAccess(Context context) {
        if (Build.VERSION.SDK_INT >= 30) return Environment.isExternalStorageManager();
        if (context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) return false;
        // Apps targeting a modern SDK can hold WRITE_EXTERNAL_STORAGE on Android 10 while still
        // being confined by scoped storage. The manifest opts into Android 10's legacy mode; make
        // sure the OS actually honored it before claiming the ordinary public folders are usable.
        return Build.VERSION.SDK_INT < 29 || Environment.isExternalStorageLegacy();
    }

    public static Path root() {
        return Environment.getExternalStorageDirectory().toPath().resolve(ROOT_NAME);
    }

    public static Path directory(ManagedContentLibrary.Kind kind) {
        switch (kind) {
            case INI_MOD: return root().resolve("units");
            case MAP: return root().resolve("maps");
            case JAVA_MOD: return root().resolve("javamods");
            default: throw new IllegalArgumentException("Unknown content kind");
        }
    }

    public static String documentId(ManagedContentLibrary.Kind kind) {
        return "primary:" + ROOT_NAME + "/" + directory(kind).getFileName();
    }

    /**
     * Prepares public content and removes the obsolete private-to-public symlinks.
     *
     * Android's emulated-storage layer does not permit an app to traverse a symlink from its
     * private data directory into /storage/emulated/0, even with all-files access. The embedded
     * desktop JVM therefore receives the public root explicitly and never follows these paths.
     */
    public static void ensureReady(Context context) throws IOException {
        if (!hasStorageAccess(context)) {
            throw new IOException("Shared storage permission is required");
        }
        Path gameRoot = DesktopGameImportService.importedRoot(context).toPath();
        if (!Files.isDirectory(gameRoot)) throw new IOException("Desktop game is not imported");
        configureManagedContent();
        prepareWritableDirectory(root(), "shared content root");
        for (Link link : links(gameRoot)) {
            prepareWritableDirectory(link.shared,
                    "shared " + link.shared.getFileName() + " directory");
            try {
                migrateAndDetach(link.privatePath, link.shared);
            } catch (IOException failure) {
                throw new IOException("Could not migrate " + link.privatePath + " to "
                        + link.shared + ": " + failureDetail(failure), failure);
            }
        }
    }

    public static boolean isReady(Context context) {
        if (!hasStorageAccess(context)) return false;
        configureManagedContent();
        Path gameRoot = DesktopGameImportService.importedRoot(context).toPath();
        if (!Files.isDirectory(gameRoot)) return false;
        try {
            for (Link link : links(gameRoot)) {
                if (!Files.isDirectory(link.shared) || !Files.isReadable(link.shared)
                        || !Files.isWritable(link.shared)
                        || Files.isSymbolicLink(link.privatePath)
                        || !Files.isDirectory(link.privatePath, LinkOption.NOFOLLOW_LINKS)) {
                    return false;
                }
                // Opening the public directory catches FUSE/AppOps failures that simple metadata
                // checks can otherwise hide until the game is already starting.
                try (DirectoryStream<Path> ignored = Files.newDirectoryStream(link.shared)) {
                    // Successful open is the readiness check.
                }
            }
            return true;
        } catch (IOException failure) {
            return false;
        }
    }

    public static void configureManagedContent() {
        System.setProperty(ManagedContentLibrary.CONTENT_ROOT_PROPERTY,
                root().toAbsolutePath().normalize().toString());
    }

    private static List<Link> links(Path gameRoot) {
        List<Link> result = new ArrayList<>();
        result.add(new Link(gameRoot.resolve("mods/units"), root().resolve("units")));
        result.add(new Link(gameRoot.resolve("mods/maps"), root().resolve("maps")));
        result.add(new Link(gameRoot.resolve("javamods"), root().resolve("javamods")));
        result.add(new Link(gameRoot.resolve("mods-disabled/maps"),
                root().resolve(".rusted-fabric-disabled/maps")));
        result.add(new Link(gameRoot.resolve("javamods-disabled"),
                root().resolve(".rusted-fabric-disabled/javamods")));
        return result;
    }

    private static void migrateAndDetach(Path privatePath, Path shared) throws IOException {
        Files.createDirectories(privatePath.getParent());
        if (Files.isSymbolicLink(privatePath)) {
            if (!pointsTo(privatePath, shared)) {
                throw new IOException("Managed content link points to an unexpected location: "
                        + privatePath);
            }
            Files.delete(privatePath);
        }
        if (Files.exists(privatePath, LinkOption.NOFOLLOW_LINKS)) {
            if (!Files.isDirectory(privatePath, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("Managed content path is not a directory: " + privatePath);
            }
            mergeDirectory(privatePath, shared);
        } else {
            Files.createDirectories(privatePath);
        }
    }

    /**
     * Verifies real directory access instead of trusting Android's permission bit alone. Some
     * Android 10 vendor builds report WRITE_EXTERNAL_STORAGE as granted while scoped storage still
     * rejects java.nio operations under /storage/emulated/0.
     */
    private static void prepareWritableDirectory(Path directory, String label) throws IOException {
        try {
            Files.createDirectories(directory);
            if (!Files.isDirectory(directory) || !Files.isReadable(directory)
                    || !Files.isWritable(directory)) {
                throw new IOException("directory is not readable and writable");
            }
            try (DirectoryStream<Path> ignored = Files.newDirectoryStream(directory)) {
                // Opening the directory verifies traversal through Android's emulated-storage
                // layer; metadata checks alone can return a false positive.
            }
            Path probe = directory.resolve(".rusted-fabric-write-probe-" + UUID.randomUUID());
            try {
                Files.write(probe, new byte[]{0}, StandardOpenOption.CREATE_NEW,
                        StandardOpenOption.WRITE);
            } finally {
                Files.deleteIfExists(probe);
            }
        } catch (IOException failure) {
            throw new IOException("Cannot access " + label + " " + directory + ": "
                    + failureDetail(failure), failure);
        }
    }

    private static void mergeDirectory(Path source, Path target) throws IOException {
        Files.createDirectories(target);
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(source)) {
            for (Path entry : entries) {
                if (Files.isSymbolicLink(entry)) {
                    throw new IOException("A symbolic link was found in managed content: " + entry);
                }
                Path destination = target.resolve(entry.getFileName());
                if (Files.exists(destination, LinkOption.NOFOLLOW_LINKS)) {
                    if (Files.isDirectory(entry, LinkOption.NOFOLLOW_LINKS)
                            && Files.isDirectory(destination, LinkOption.NOFOLLOW_LINKS)) {
                        mergeDirectory(entry, destination);
                        Files.delete(entry);
                        continue;
                    }
                    destination = uniqueMigratedPath(target, entry.getFileName().toString());
                }
                moveAcrossStorage(entry, destination);
            }
        }
    }

    private static Path uniqueMigratedPath(Path parent, String name) {
        int suffix = 2;
        Path candidate = parent.resolve(name + "-migrated");
        while (Files.exists(candidate, LinkOption.NOFOLLOW_LINKS)) {
            candidate = parent.resolve(name + "-migrated-" + suffix++);
        }
        return candidate;
    }

    private static void moveAcrossStorage(Path source, Path target) throws IOException {
        // The imported game is app-private while the editable content root is emulated public
        // storage. Files.move across those providers can block for a long time on some EMUI/FUSE
        // implementations before eventually reporting EXDEV. Copy deliberately and only delete a
        // source after its destination has been committed.
        if (Files.isDirectory(source, LinkOption.NOFOLLOW_LINKS)) {
            Files.createDirectories(target);
            mergeDirectory(source, target);
            Files.delete(source);
            return;
        }
        Path staging = target.resolveSibling("." + target.getFileName()
                + ".rusted-fabric-migrating-" + UUID.randomUUID());
        try {
            Files.copy(source, staging);
            try {
                Files.move(staging, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(staging, target);
            }
            try {
                Files.delete(source);
            } catch (IOException sourceDeleteFailure) {
                // Preserve retry safety: if the source could not be retired, remove the complete
                // destination so a later preparation attempt cannot create a duplicate copy.
                try {
                    Files.deleteIfExists(target);
                } catch (IOException rollbackFailure) {
                    sourceDeleteFailure.addSuppressed(rollbackFailure);
                }
                throw sourceDeleteFailure;
            }
        } finally {
            Files.deleteIfExists(staging);
        }
    }

    private static String failureDetail(Throwable failure) {
        String message = failure.getMessage();
        return failure.getClass().getSimpleName()
                + (message == null || message.trim().isEmpty() ? "" : ": " + message);
    }

    private static boolean pointsTo(Path link, Path target) throws IOException {
        if (!Files.isSymbolicLink(link)) return false;
        Path destination = Files.readSymbolicLink(link);
        if (!destination.isAbsolute()) destination = link.getParent().resolve(destination);
        return destination.normalize().equals(target.toAbsolutePath().normalize());
    }

    private static final class Link {
        final Path privatePath;
        final Path shared;

        Link(Path privatePath, Path shared) {
            this.privatePath = privatePath;
            this.shared = shared;
        }
    }
}
