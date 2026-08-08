package io.github.endx.rustedfabric.android.xposed.mod;

import android.content.Context;
import android.database.Cursor;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import java.util.regex.Pattern;

import io.github.endx.rustedfabric.android.mod.ModVerificationException;
import io.github.endx.rustedfabric.android.mod.RustedFabricModVerifier;
import io.github.endx.rustedfabric.android.mod.VerifiedModArchive;
import io.github.endx.rustedfabric.android.xposed.storage.ModContentProvider;
import io.github.endx.rustedfabricapi.api.RustedFabricAPIContext;

/** Discovers enabled private mods through the provider and copies them into game code cache. */
public final class EnabledModClient {
    private static final Pattern SAFE_ID = Pattern.compile("[a-z][a-z0-9_-]{0,63}");
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");
    private final RustedFabricModVerifier verifier = new RustedFabricModVerifier();
    private final AndroidDexModLoader loader = new AndroidDexModLoader();

    public LoadSummary loadAll(Context gameContext, ClassLoader gameClassLoader,
                               RustedFabricAPIContext apiContext, FailureSink failures) {
        int discovered = 0;
        int loaded = 0;
        int failed = 0;
        try (Cursor cursor = gameContext.getContentResolver().query(
                ModContentProvider.ENABLED_MODS_URI,
                new String[]{ModContentProvider.COLUMN_ID,
                        ModContentProvider.COLUMN_ARCHIVE_SHA256,
                        ModContentProvider.COLUMN_DEX_SHA256},
                null, null, null)) {
            if (cursor == null) {
                return new LoadSummary(0, 0, 0,
                        new IOException("Enabled mod provider returned no cursor"));
            }
            int idColumn = cursor.getColumnIndexOrThrow(ModContentProvider.COLUMN_ID);
            int archiveColumn = cursor.getColumnIndexOrThrow(
                    ModContentProvider.COLUMN_ARCHIVE_SHA256);
            int dexColumn = cursor.getColumnIndexOrThrow(ModContentProvider.COLUMN_DEX_SHA256);
            while (cursor.moveToNext()) {
                discovered++;
                String id = cursor.getString(idColumn);
                String archiveSha256 = cursor.getString(archiveColumn);
                String dexSha256 = cursor.getString(dexColumn);
                try {
                    loadOne(gameContext, gameClassLoader, apiContext, id,
                            archiveSha256, dexSha256);
                    loaded++;
                } catch (ThreadDeath | VirtualMachineError critical) {
                    throw critical;
                } catch (Throwable failure) {
                    failed++;
                    failures.onFailure(safeId(id), failure);
                }
            }
            return new LoadSummary(discovered, loaded, failed, null);
        } catch (ThreadDeath | VirtualMachineError critical) {
            throw critical;
        } catch (Throwable discoveryFailure) {
            return new LoadSummary(discovered, loaded, failed, discoveryFailure);
        }
    }

    private LoadedAndroidMod loadOne(Context context, ClassLoader gameClassLoader,
                                     RustedFabricAPIContext apiContext, String id,
                                     String archiveSha256, String dexSha256)
            throws IOException, ModVerificationException, AndroidModLoadException {
        if (id == null || !SAFE_ID.matcher(id).matches()
                || archiveSha256 == null || !SHA256.matcher(archiveSha256).matches()
                || dexSha256 == null || !SHA256.matcher(dexSha256).matches()) {
            throw new IOException("Provider returned invalid mod identity");
        }
        Path cacheRoot = context.getCodeCacheDir().toPath().resolve("rusted-fabric");
        Path archives = cacheRoot.resolve("mods");
        Files.createDirectories(archives);
        Path cached = archives.resolve(archiveSha256 + ".javamod");
        VerifiedModArchive verified = verifyExpected(cached, id, archiveSha256, dexSha256);
        if (verified == null) {
            Files.deleteIfExists(cached);
            copyFromProvider(context, id, archives, cached);
            verified = verifyExpected(cached, id, archiveSha256, dexSha256);
            if (verified == null) {
                Files.deleteIfExists(cached);
                throw new IOException("Copied mod failed provider identity verification");
            }
        }
        File optimized = cacheRoot.resolve("optimized").resolve(archiveSha256).toFile();
        return loader.loadVerified(verified, optimized, apiContext, gameClassLoader);
    }

    private VerifiedModArchive verifyExpected(Path archive, String id, String archiveSha256,
                                              String dexSha256) {
        if (!Files.isRegularFile(archive)) {
            return null;
        }
        try {
            VerifiedModArchive verified = verifier.verify(archive);
            return id.equals(verified.getMetadata().getId())
                    && archiveSha256.equals(verified.getArchiveSha256())
                    && dexSha256.equals(verified.getDexSha256()) ? verified : null;
        } catch (ModVerificationException invalid) {
            return null;
        }
    }

    private static void copyFromProvider(Context context, String id, Path directory, Path target)
            throws IOException, ModVerificationException {
        Path temporary = directory.resolve(".provider-" + UUID.randomUUID() + ".tmp");
        try (ParcelFileDescriptor descriptor = context.getContentResolver()
                .openFileDescriptor(ModContentProvider.archiveUri(id), "r")) {
            if (descriptor == null) {
                throw new IOException("Enabled mod provider returned no file");
            }
            try (InputStream input = new FileInputStream(descriptor.getFileDescriptor());
                 OutputStream output = Files.newOutputStream(temporary,
                         StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                byte[] buffer = new byte[32 * 1024];
                long total = 0;
                for (int count = input.read(buffer); count >= 0; count = input.read(buffer)) {
                    if (count == 0) {
                        continue;
                    }
                    total += count;
                    if (total > RustedFabricModVerifier.MAX_ARCHIVE_BYTES) {
                        throw new ModVerificationException(
                                ModVerificationException.Reason.LIMIT_EXCEEDED,
                                "Provider mod exceeds the archive size limit");
                    }
                    output.write(buffer, 0, count);
                }
            }
            atomicMove(temporary, target);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void atomicMove(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException unsupported) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String safeId(String id) {
        return id != null && SAFE_ID.matcher(id).matches() ? id : "unknown";
    }

    @FunctionalInterface
    public interface FailureSink {
        void onFailure(String modId, Throwable failure);
    }

    public static final class LoadSummary {
        private final int discovered;
        private final int loaded;
        private final int failed;
        private final Throwable discoveryFailure;

        private LoadSummary(int discovered, int loaded, int failed, Throwable discoveryFailure) {
            this.discovered = discovered;
            this.loaded = loaded;
            this.failed = failed;
            this.discoveryFailure = discoveryFailure;
        }

        public int getDiscovered() { return discovered; }
        public int getLoaded() { return loaded; }
        public int getFailed() { return failed; }
        public Throwable getDiscoveryFailure() { return discoveryFailure; }
    }
}
