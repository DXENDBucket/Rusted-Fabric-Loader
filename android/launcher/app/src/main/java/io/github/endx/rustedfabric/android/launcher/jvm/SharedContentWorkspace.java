package io.github.endx.rustedfabric.android.launcher.jvm;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.system.ErrnoException;
import android.system.Os;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import io.github.endx.rustedfabric.android.jvm.ManagedContentLibrary;

/** Public, file-manager-accessible content directories linked into the private game root. */
public final class SharedContentWorkspace {
    public static final String ROOT_NAME = "rustedWarfare";

    private SharedContentWorkspace() {
    }

    public static boolean hasStorageAccess(Context context) {
        if (Build.VERSION.SDK_INT >= 30) return Environment.isExternalStorageManager();
        return context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
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

    public static void ensureLinked(Context context) throws IOException {
        if (!hasStorageAccess(context)) {
            throw new IOException("Shared storage permission is required");
        }
        Path gameRoot = DesktopGameImportService.importedRoot(context).toPath();
        if (!Files.isDirectory(gameRoot)) throw new IOException("Desktop game is not imported");
        Files.createDirectories(root());
        for (Link link : links(gameRoot)) {
            Files.createDirectories(link.shared);
            migrateAndLink(link.privatePath, link.shared);
        }
    }

    public static boolean isReady(Context context) {
        if (!hasStorageAccess(context)) return false;
        Path gameRoot = DesktopGameImportService.importedRoot(context).toPath();
        if (!Files.isDirectory(gameRoot)) return false;
        try {
            for (Link link : links(gameRoot)) {
                if (!Files.isDirectory(link.shared) || !pointsTo(link.privatePath, link.shared)) {
                    return false;
                }
            }
            return true;
        } catch (IOException failure) {
            return false;
        }
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

    private static void migrateAndLink(Path privatePath, Path shared) throws IOException {
        Files.createDirectories(privatePath.getParent());
        if (Files.isSymbolicLink(privatePath)) {
            if (pointsTo(privatePath, shared)) return;
            Files.delete(privatePath);
        }
        if (Files.exists(privatePath, LinkOption.NOFOLLOW_LINKS)) {
            if (!Files.isDirectory(privatePath, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("Managed content path is not a directory: " + privatePath);
            }
            mergeDirectory(privatePath, shared);
            Files.delete(privatePath);
        }
        createLink(privatePath, shared);
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
        try {
            Files.move(source, target);
        } catch (IOException crossDevice) {
            if (Files.isDirectory(source, LinkOption.NOFOLLOW_LINKS)) {
                Files.createDirectories(target);
                mergeDirectory(source, target);
                Files.delete(source);
            } else {
                Files.copy(source, target);
                Files.delete(source);
            }
        }
    }

    private static void createLink(Path link, Path target) throws IOException {
        try {
            Files.createSymbolicLink(link, target.toAbsolutePath());
        } catch (UnsupportedOperationException | IOException javaFailure) {
            try {
                Os.symlink(target.toAbsolutePath().toString(), link.toString());
            } catch (ErrnoException nativeFailure) {
                javaFailure.addSuppressed(nativeFailure);
                throw new IOException("Cannot link the shared content directory: " + link,
                        javaFailure);
            }
        }
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
