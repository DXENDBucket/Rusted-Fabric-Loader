package io.github.endx.rustedfabric.android.launcher.jvm;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import io.github.endx.rustedfabric.android.jvm.ManagedContentLibrary;

/** Copies one SAF document into a bounded temporary file before managed content validation. */
public final class ManagedContentImportService {
    private static final long MAX_SOURCE_BYTES = 536_870_912L;

    private ManagedContentImportService() {
    }

    public static ManagedContentLibrary.Item importDocument(
            Context context, Uri source, ManagedContentLibrary.Kind kind,
            ProgressListener listener) throws IOException {
        if (source == null) throw new IOException("No content document was selected");
        String name = displayName(context.getContentResolver(), source);
        File temporary = File.createTempFile("rusted-fabric-content-", suffix(name),
                context.getCacheDir());
        try {
            copy(context.getContentResolver(), source, temporary, listener);
            return ManagedContentLibrary.importContent(
                    DesktopGameImportService.importedRoot(context).toPath(), kind,
                    temporary.toPath(), name);
        } finally {
            if (temporary.exists() && !temporary.delete()) temporary.deleteOnExit();
        }
    }

    private static void copy(ContentResolver resolver, Uri source, File target,
                             ProgressListener listener) throws IOException {
        try (InputStream input = resolver.openInputStream(source);
             FileOutputStream output = new FileOutputStream(target)) {
            if (input == null) throw new IOException("Cannot open the selected document");
            byte[] buffer = new byte[64 * 1024];
            long bytes = 0L;
            int read;
            while ((read = input.read(buffer)) >= 0) {
                bytes += read;
                if (bytes > MAX_SOURCE_BYTES) throw new IOException("Selected content is too large");
                output.write(buffer, 0, read);
                if (listener != null) listener.onProgress(bytes);
            }
        } catch (SecurityException denied) {
            throw new IOException("Android denied access to the selected document", denied);
        }
    }

    private static String displayName(ContentResolver resolver, Uri source) {
        try (Cursor cursor = resolver.query(source,
                new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                String value = cursor.getString(0);
                if (value != null && !value.trim().isEmpty()) return value.trim();
            }
        } catch (RuntimeException ignored) {
            // URI path fallback below.
        }
        String segment = source.getLastPathSegment();
        return segment == null || segment.trim().isEmpty() ? "imported-content" : segment;
    }

    private static String suffix(String name) {
        int dot = name.lastIndexOf('.');
        String suffix = dot >= 0 ? name.substring(dot) : ".bin";
        return suffix.length() <= 12 ? suffix : ".bin";
    }

    public interface ProgressListener {
        void onProgress(long bytes);
    }
}
