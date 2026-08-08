package io.github.endx.rustedfabric.android.xposed.jvm;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.endx.rustedfabric.android.jvm.DesktopGameArchiveExtractor;
import io.github.endx.rustedfabric.android.jvm.DesktopGameInspection;
import io.github.endx.rustedfabric.android.jvm.DesktopGameLayout;

/** Imports only portable desktop game data from a user-selected document tree. */
public final class DesktopGameImportService {
    private static final long MAX_BYTES = 1_610_612_736L;
    private static final long MAX_ARCHIVE_BYTES = 2_147_483_648L;
    private static final int MAX_FILES = 50_000;
    private static final int MAX_DEPTH = 32;

    private DesktopGameImportService() {
    }

    public static Result importTree(Context context, Uri tree, ProgressListener listener)
            throws IOException {
        if (tree == null || !DocumentsContract.isTreeUri(tree)) {
            throw new IOException("The selected location is not a document tree");
        }
        File backend = backendRoot(context);
        File staging = prepareStaging(backend);

        Counter counter = new Counter();
        ContentResolver resolver = context.getContentResolver();
        String rootDocument = DocumentsContract.getTreeDocumentId(tree);
        Set<String> accepted = new HashSet<>(DesktopGameLayout.importRoots());
        try {
            for (Document child : children(resolver, tree, rootDocument)) {
                if (!accepted.contains(child.name)) continue;
                File destination = checkedChild(staging, child.name);
                copyDocument(resolver, tree, child, destination, counter, listener, 0,
                        "libs".equals(child.name));
            }
            DesktopGameInspection inspection = DesktopGameLayout.inspect(staging.toPath());
            if (!inspection.isImportable()) {
                throw new IOException("Selected desktop game is incomplete: " + inspection.errors());
            }
            DesktopGameLayout.prepareWritableDirectories(staging.toPath());
            return activate(backend, staging, counter.files, counter.bytes, inspection.warnings());
        } catch (IOException | RuntimeException failure) {
            deleteTree(staging, backend);
            throw failure;
        }
    }

    public static Result importArchive(Context context, Uri archiveUri,
                                       ProgressListener listener) throws IOException {
        if (archiveUri == null) throw new IOException("No desktop game ZIP was selected");
        File temporaryArchive = File.createTempFile("rusted-fabric-desktop-", ".zip",
                context.getCacheDir());
        File backend = backendRoot(context);
        File staging = null;
        try {
            copyArchive(context.getContentResolver(), archiveUri, temporaryArchive);
            staging = prepareStaging(backend);
            DesktopGameArchiveExtractor.Result extracted = DesktopGameArchiveExtractor.extract(
                    temporaryArchive.toPath(), staging.toPath(), listener == null ? null
                            : listener::onProgress);
            DesktopGameLayout.prepareWritableDirectories(staging.toPath());
            return activate(backend, staging, extracted.files(), extracted.bytes(),
                    extracted.warnings());
        } catch (IOException | RuntimeException failure) {
            if (staging != null) deleteTree(staging, backend);
            throw failure;
        } finally {
            if (temporaryArchive.exists() && !temporaryArchive.delete()) {
                temporaryArchive.deleteOnExit();
            }
        }
    }

    public static File importedRoot(Context context) {
        return new File(backendRoot(context), "game");
    }

    private static void copyArchive(ContentResolver resolver, Uri source, File target)
            throws IOException {
        try (InputStream input = resolver.openInputStream(source);
             FileOutputStream output = new FileOutputStream(target)) {
            if (input == null) throw new IOException("Cannot open the selected desktop game ZIP");
            byte[] buffer = new byte[64 * 1024];
            long bytes = 0L;
            int read;
            while ((read = input.read(buffer)) >= 0) {
                bytes += read;
                if (bytes > MAX_ARCHIVE_BYTES) {
                    throw new IOException("Desktop game ZIP exceeds the archive size limit");
                }
                output.write(buffer, 0, read);
            }
        } catch (SecurityException denied) {
            throw new IOException("Android denied access to the selected desktop game ZIP", denied);
        }
    }

    private static File backendRoot(Context context) {
        return new File(context.getFilesDir(), "desktop-jvm");
    }

    private static File prepareStaging(File backend) throws IOException {
        if (!backend.isDirectory() && !backend.mkdirs()) {
            throw new IOException("Cannot create the private JVM backend directory");
        }
        File staging = checkedChild(backend, "game.importing");
        deleteTree(staging, backend);
        if (!staging.mkdirs()) throw new IOException("Cannot create private import directory");
        return staging;
    }

    private static Result activate(File backend, File staging, int files, long bytes,
                                   List<String> warnings) throws IOException {
        File target = checkedChild(backend, "game");
        File previous = checkedChild(backend, "game.previous");
        deleteTree(previous, backend);
        if (target.exists() && !target.renameTo(previous)) {
            throw new IOException("Cannot replace the previous private game import");
        }
        if (!staging.renameTo(target)) {
            if (previous.exists()) previous.renameTo(target);
            throw new IOException("Cannot activate the imported desktop game");
        }
        deleteTree(previous, backend);
        return new Result(target, files, bytes, warnings);
    }

    private static void copyDocument(ContentResolver resolver, Uri tree, Document source,
                                     File destination, Counter counter,
                                     ProgressListener listener, int depth,
                                     boolean jarOnly) throws IOException {
        if (depth > MAX_DEPTH) throw new IOException("Selected game tree is too deeply nested");
        if (source.directory) {
            if (!destination.isDirectory() && !destination.mkdirs()) {
                throw new IOException("Cannot create imported directory: " + source.name);
            }
            for (Document child : children(resolver, tree, source.documentId)) {
                boolean childJarOnly = jarOnly;
                if (childJarOnly && !child.directory
                        && !child.name.toLowerCase(java.util.Locale.ROOT).endsWith(".jar")) {
                    continue;
                }
                copyDocument(resolver, tree, child, checkedChild(destination, child.name),
                        counter, listener, depth + 1, childJarOnly);
            }
            return;
        }
        if (++counter.files > MAX_FILES) throw new IOException("Selected game contains too many files");
        Uri sourceUri = DocumentsContract.buildDocumentUriUsingTree(tree, source.documentId);
        try (InputStream input = resolver.openInputStream(sourceUri);
             FileOutputStream output = new FileOutputStream(destination)) {
            if (input == null) throw new IOException("Cannot open selected file: " + source.name);
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                counter.bytes += read;
                if (counter.bytes > MAX_BYTES) {
                    throw new IOException("Portable desktop game data exceeds the import limit");
                }
                output.write(buffer, 0, read);
            }
        }
        if (listener != null) listener.onProgress(counter.files, counter.bytes, source.name);
    }

    private static List<Document> children(ContentResolver resolver, Uri tree, String parentId)
            throws IOException {
        Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentId);
        java.util.ArrayList<Document> result = new java.util.ArrayList<>();
        String[] projection = {
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
        };
        try (Cursor cursor = resolver.query(children, projection, null, null, null)) {
            if (cursor == null) throw new IOException("Cannot list the selected game directory");
            while (cursor.moveToNext()) {
                String id = cursor.getString(0);
                String name = safeName(cursor.getString(1));
                String mime = cursor.getString(2);
                result.add(new Document(id, name,
                        DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)));
            }
        } catch (SecurityException denied) {
            throw new IOException("Android denied access to the selected game directory", denied);
        }
        return result;
    }

    private static String safeName(String value) throws IOException {
        if (value == null || value.isEmpty() || ".".equals(value) || "..".equals(value)
                || value.indexOf('/') >= 0 || value.indexOf('\\') >= 0
                || value.indexOf('\0') >= 0) {
            throw new IOException("Selected tree contains an unsafe file name");
        }
        return value;
    }

    private static File checkedChild(File parent, String name) throws IOException {
        File child = new File(parent, safeName(name));
        String parentPath = parent.getCanonicalPath() + File.separator;
        if (!child.getCanonicalPath().startsWith(parentPath)) {
            throw new IOException("Import path escaped the private backend directory");
        }
        return child;
    }

    private static void deleteTree(File target, File backendRoot) throws IOException {
        if (!target.exists()) return;
        String root = backendRoot.getCanonicalPath() + File.separator;
        if (!target.getCanonicalPath().startsWith(root)) {
            throw new IOException("Refusing to delete outside the private backend directory");
        }
        File[] children = target.listFiles();
        if (children != null) {
            for (File child : children) deleteTree(child, backendRoot);
        }
        if (!target.delete()) throw new IOException("Cannot clean private import path: " + target);
    }

    public interface ProgressListener {
        void onProgress(int files, long bytes, String currentName);
    }

    public static final class Result {
        private final File root;
        private final int files;
        private final long bytes;
        private final List<String> warnings;

        Result(File root, int files, long bytes, List<String> warnings) {
            this.root = root;
            this.files = files;
            this.bytes = bytes;
            this.warnings = java.util.Collections.unmodifiableList(
                    new java.util.ArrayList<>(warnings));
        }

        public File root() { return root; }
        public int files() { return files; }
        public long bytes() { return bytes; }
        public List<String> warnings() { return warnings; }
    }

    private static final class Counter {
        private int files;
        private long bytes;
    }

    private static final class Document {
        private final String documentId;
        private final String name;
        private final boolean directory;

        private Document(String documentId, String name, boolean directory) {
            this.documentId = documentId;
            this.name = name;
            this.directory = directory;
        }
    }
}
