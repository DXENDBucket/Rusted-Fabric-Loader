package io.github.endx.rustedfabric.android.xposed.storage;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

import io.github.endx.rustedfabric.android.mod.ModRegistry;
import io.github.endx.rustedfabric.android.mod.ModVerificationException;
import io.github.endx.rustedfabric.android.mod.RustedFabricModVerifier;
import io.github.endx.rustedfabric.android.mod.VerifiedModArchive;

public final class ModImportService {
    private ModImportService() {
    }

    public static ModRegistry.Record importUri(Context context, Uri source)
            throws IOException, ModVerificationException {
        if (source == null) {
            throw new IOException("No mod document was selected");
        }
        Path incoming = context.getCacheDir().toPath().resolve("rusted-fabric-imports");
        Files.createDirectories(incoming);
        Path temporary = incoming.resolve(UUID.randomUUID() + ".javamod");
        try {
            copyBounded(context.getContentResolver(), source, temporary,
                    RustedFabricModVerifier.MAX_ARCHIVE_BYTES);
            VerifiedModArchive verified = new RustedFabricModVerifier().verify(temporary);
            return ModStorage.registry(context).install(verified);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void copyBounded(ContentResolver resolver, Uri source, Path target, long limit)
            throws IOException, ModVerificationException {
        try (InputStream input = resolver.openInputStream(source);
             OutputStream output = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW,
                     StandardOpenOption.WRITE)) {
            if (input == null) {
                throw new IOException("The selected document cannot be opened");
            }
            byte[] buffer = new byte[32 * 1024];
            long total = 0;
            for (int count = input.read(buffer); count >= 0; count = input.read(buffer)) {
                if (count == 0) {
                    continue;
                }
                total += count;
                if (total > limit) {
                    throw new ModVerificationException(
                            ModVerificationException.Reason.LIMIT_EXCEEDED,
                            "Selected mod exceeds the archive size limit");
                }
                output.write(buffer, 0, count);
            }
        }
    }
}
