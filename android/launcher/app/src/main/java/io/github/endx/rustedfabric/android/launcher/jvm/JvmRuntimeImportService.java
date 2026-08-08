package io.github.endx.rustedfabric.android.launcher.jvm;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import io.github.endx.rustedfabric.android.jvm.JvmRuntimeArchiveExtractor;

/** Installs a user-selected ARM64 Java 17 ZIP or TAR.XZ into Loader-private storage. */
public final class JvmRuntimeImportService {
    private static final long MAX_ARCHIVE_BYTES = 1_073_741_824L;

    private JvmRuntimeImportService() {
    }

    public static Result importArchive(Context context, Uri source,
                                       JvmRuntimeArchiveExtractor.ProgressListener listener)
            throws IOException {
        if (source == null) throw new IOException("No Java runtime archive was selected");
        File backend = new File(context.getFilesDir(), "desktop-jvm");
        if (!backend.isDirectory() && !backend.mkdirs()) {
            throw new IOException("Cannot create private JVM backend directory");
        }
        File staging = child(backend, "runtime.importing");
        File target = child(backend, "runtime");
        File previous = child(backend, "runtime.previous");
        deleteTree(staging, backend);
        if (!staging.mkdirs()) throw new IOException("Cannot create private runtime staging directory");
        File archive = File.createTempFile("rusted-fabric-runtime-", ".archive",
                context.getCacheDir());
        try {
            copy(context.getContentResolver(), source, archive);
            JvmRuntimeArchiveExtractor.Result extracted = JvmRuntimeArchiveExtractor.extract(
                    archive.toPath(), staging.toPath(), listener);
            deleteTree(previous, backend);
            if (target.exists() && !target.renameTo(previous)) {
                throw new IOException("Cannot replace the previous Java runtime");
            }
            if (!staging.renameTo(target)) {
                if (previous.exists()) previous.renameTo(target);
                throw new IOException("Cannot activate the imported Java runtime");
            }
            deleteTree(previous, backend);
            return new Result(target, extracted.files(), extracted.bytes(),
                    extracted.archiveSha256());
        } catch (IOException | RuntimeException failure) {
            deleteTree(staging, backend);
            throw failure;
        } finally {
            if (archive.exists()) archive.delete();
        }
    }

    private static void copy(ContentResolver resolver, Uri source, File destination)
            throws IOException {
        try (InputStream input = resolver.openInputStream(source);
             FileOutputStream output = new FileOutputStream(destination)) {
            if (input == null) throw new IOException("Cannot open the selected Java runtime archive");
            byte[] buffer = new byte[64 * 1024];
            long bytes = 0L;
            int read;
            while ((read = input.read(buffer)) >= 0) {
                bytes += read;
                if (bytes > MAX_ARCHIVE_BYTES) {
                    throw new IOException("Java runtime archive exceeds the size limit");
                }
                output.write(buffer, 0, read);
            }
        } catch (SecurityException denied) {
            throw new IOException("Android denied access to the Java runtime archive", denied);
        }
    }

    private static File child(File parent, String name) throws IOException {
        File value = new File(parent, name);
        String root = parent.getCanonicalPath() + File.separator;
        if (!value.getCanonicalPath().startsWith(root)) {
            throw new IOException("Runtime path escaped private storage");
        }
        return value;
    }

    private static void deleteTree(File target, File backend) throws IOException {
        if (!target.exists()) return;
        String root = backend.getCanonicalPath() + File.separator;
        if (!target.getCanonicalPath().startsWith(root)) {
            throw new IOException("Refusing to delete outside private runtime storage");
        }
        File[] children = target.listFiles();
        if (children != null) for (File child : children) deleteTree(child, backend);
        if (!target.delete()) throw new IOException("Cannot clean private runtime path");
    }

    public static final class Result {
        private final File root;
        private final int files;
        private final long bytes;
        private final String archiveSha256;

        Result(File root, int files, long bytes, String archiveSha256) {
            this.root = root;
            this.files = files;
            this.bytes = bytes;
            this.archiveSha256 = archiveSha256;
        }

        public File root() { return root; }
        public int files() { return files; }
        public long bytes() { return bytes; }
        public String archiveSha256() { return archiveSha256; }
    }
}
