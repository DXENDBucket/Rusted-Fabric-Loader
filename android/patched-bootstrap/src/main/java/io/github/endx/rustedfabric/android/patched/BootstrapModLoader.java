package io.github.endx.rustedfabric.android.patched;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import dalvik.system.DexClassLoader;
import io.github.endx.rustedfabric.android.mod.DelegatingModParentClassLoader;
import io.github.endx.rustedfabric.android.mod.ModVerificationException;
import io.github.endx.rustedfabric.android.mod.RustedFabricModMetadata;
import io.github.endx.rustedfabric.android.mod.RustedFabricModVerifier;
import io.github.endx.rustedfabric.android.mod.VerifiedModArchive;
import io.github.endx.rustedfabricapi.api.RustedFabricAPIContext;
import io.github.endx.rustedfabricapi.api.RustedFabricModEntrypoint;

/** Pulls verified enabled mods from the separately installed Loader app. */
final class BootstrapModLoader {
    private static final String TAG = "RustedFabric/Local";
    private static final String[] AUTHORITIES = {
            "io.github.endx.rustedfabric.android.xposed.mods",
            "io.github.endx.rustedfabric.android.xposed.debug.mods"
    };
    private static final String[] COLUMNS = {"id", "archive_sha256", "dex_sha256"};
    private static final Pattern SAFE_ID = Pattern.compile("[a-z][a-z0-9_-]{0,63}");
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");
    private static final String SUPPORTED_API_VERSION = "0.1";
    private final RustedFabricModVerifier verifier = new RustedFabricModVerifier();

    Summary loadAll(Context context, ClassLoader gameClassLoader,
                    RustedFabricAPIContext apiContext) {
        Throwable lastFailure = null;
        for (String authority : AUTHORITIES) {
            try {
                return loadAuthority(context, gameClassLoader, apiContext, authority);
            } catch (ThreadDeath | VirtualMachineError critical) {
                throw critical;
            } catch (Throwable unavailable) {
                lastFailure = unavailable;
            }
        }
        return new Summary(0, 0, 0, lastFailure != null ? lastFailure
                : new IOException("No Loader provider is installed"));
    }

    private Summary loadAuthority(Context context, ClassLoader gameClassLoader,
                                  RustedFabricAPIContext apiContext, String authority) {
        int discovered = 0;
        int loaded = 0;
        int failed = 0;
        Uri enabled = Uri.parse("content://" + authority + "/enabled");
        try (Cursor cursor = context.getContentResolver().query(
                enabled, COLUMNS, null, null, null)) {
            if (cursor == null) {
                throw new IllegalStateException("Loader provider returned no cursor");
            }
            int idColumn = cursor.getColumnIndexOrThrow(COLUMNS[0]);
            int archiveColumn = cursor.getColumnIndexOrThrow(COLUMNS[1]);
            int dexColumn = cursor.getColumnIndexOrThrow(COLUMNS[2]);
            while (cursor.moveToNext()) {
                discovered++;
                String id = cursor.getString(idColumn);
                try {
                    loadOne(context, gameClassLoader, apiContext, authority, id,
                            cursor.getString(archiveColumn), cursor.getString(dexColumn));
                    loaded++;
                } catch (ThreadDeath | VirtualMachineError critical) {
                    throw critical;
                } catch (Throwable modFailure) {
                    failed++;
                    Log.e(TAG, "Mod load failed: " + safeId(id), modFailure);
                }
            }
            return new Summary(discovered, loaded, failed, null);
        }
    }

    private void loadOne(Context context, ClassLoader gameClassLoader,
                         RustedFabricAPIContext apiContext, String authority, String id,
                         String archiveSha256, String dexSha256) throws Exception {
        if (id == null || !SAFE_ID.matcher(id).matches()
                || archiveSha256 == null || !SHA256.matcher(archiveSha256).matches()
                || dexSha256 == null || !SHA256.matcher(dexSha256).matches()) {
            throw new IOException("Loader provider returned an invalid mod identity");
        }
        Path root = context.getCodeCacheDir().toPath().resolve("rusted-fabric");
        Path archives = root.resolve("mods");
        Files.createDirectories(archives);
        Path cached = archives.resolve(archiveSha256 + ".rfmod");
        VerifiedModArchive verified = verifyExpected(cached, id, archiveSha256, dexSha256);
        if (verified == null) {
            Files.deleteIfExists(cached);
            copyFromProvider(context, authority, id, archives, cached);
            verified = verifyExpected(cached, id, archiveSha256, dexSha256);
            if (verified == null) {
                Files.deleteIfExists(cached);
                throw new IOException("Copied mod failed Loader identity verification");
            }
        }
        loadVerified(verified, root.resolve("optimized").resolve(archiveSha256).toFile(),
                apiContext, gameClassLoader);
    }

    private VerifiedModArchive verifyExpected(Path archive, String id, String archiveSha256,
                                              String dexSha256) {
        if (!Files.isRegularFile(archive)) return null;
        try {
            VerifiedModArchive verified = verifier.verify(archive);
            return id.equals(verified.getMetadata().getId())
                    && archiveSha256.equals(verified.getArchiveSha256())
                    && dexSha256.equals(verified.getDexSha256()) ? verified : null;
        } catch (ModVerificationException invalid) {
            return null;
        }
    }

    private static void copyFromProvider(Context context, String authority, String id,
                                         Path directory, Path target) throws Exception {
        Path temporary = directory.resolve(".provider-" + UUID.randomUUID() + ".tmp");
        Uri uri = new Uri.Builder().scheme("content").authority(authority)
                .appendPath("mod").appendPath(id).build();
        try (ParcelFileDescriptor descriptor = context.getContentResolver()
                .openFileDescriptor(uri, "r")) {
            if (descriptor == null) throw new IOException("Loader provider returned no file");
            try (InputStream input = new FileInputStream(descriptor.getFileDescriptor());
                 OutputStream output = Files.newOutputStream(temporary,
                         StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                byte[] buffer = new byte[32 * 1024];
                long total = 0;
                for (int count = input.read(buffer); count >= 0; count = input.read(buffer)) {
                    if (count == 0) continue;
                    total += count;
                    if (total > RustedFabricModVerifier.MAX_ARCHIVE_BYTES) {
                        throw new IOException("Provider mod exceeds the archive size limit");
                    }
                    output.write(buffer, 0, count);
                }
            }
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void loadVerified(VerifiedModArchive verified, File optimizedDirectory,
                                     RustedFabricAPIContext context, ClassLoader gameClassLoader)
            throws Exception {
        RustedFabricModMetadata metadata = verified.getMetadata();
        if (!SUPPORTED_API_VERSION.equals(metadata.getApiVersion())
                || !metadata.supportsMappingProfile(context.mappingProfileId())) {
            throw new IOException("Mod is incompatible with this Android mapping/API profile");
        }
        for (String capability : metadata.getCapabilities()) {
            if (!context.hasCapability(capability)) {
                throw new IOException("Required capability is unavailable: " + capability);
            }
        }
        if (!optimizedDirectory.isDirectory() && !optimizedDirectory.mkdirs()) {
            throw new IOException("Private DEX cache directory cannot be created");
        }
        ClassLoader apiLoader = RustedFabricAPIContext.class.getClassLoader();
        if (apiLoader == null) throw new IOException("Common API ClassLoader is unavailable");
        ClassLoader parent = new DelegatingModParentClassLoader(apiLoader, gameClassLoader);
        OwnedDexClassLoader modLoader = new OwnedDexClassLoader(
                verified.getArchivePath().toString(), optimizedDirectory.getAbsolutePath(), parent,
                verified.getDefinedClasses());
        Class<?> type = modLoader.loadClass(metadata.getEntrypoint());
        if (type.getClassLoader() != modLoader
                || !RustedFabricModEntrypoint.class.isAssignableFrom(type)
                || !Modifier.isPublic(type.getModifiers()) || Modifier.isAbstract(type.getModifiers())) {
            throw new IOException("Mod entrypoint is not a public concrete Loader entrypoint");
        }
        Constructor<?> constructor = type.getConstructor();
        RustedFabricModEntrypoint entrypoint = (RustedFabricModEntrypoint) constructor.newInstance();
        entrypoint.onInitialize(context);
    }

    private static String safeId(String id) {
        return id != null && SAFE_ID.matcher(id).matches() ? id : "unknown";
    }

    static final class Summary {
        final int discovered;
        final int loaded;
        final int failed;
        final Throwable discoveryFailure;

        Summary(int discovered, int loaded, int failed, Throwable discoveryFailure) {
            this.discovered = discovered;
            this.loaded = loaded;
            this.failed = failed;
            this.discoveryFailure = discoveryFailure;
        }
    }

    private static final class OwnedDexClassLoader extends DexClassLoader {
        private final Set<String> ownedClasses;

        OwnedDexClassLoader(String dexPath, String optimizedDirectory, ClassLoader parent,
                            Set<String> ownedClasses) {
            super(dexPath, optimizedDirectory, null, parent);
            this.ownedClasses = Collections.unmodifiableSet(new LinkedHashSet<>(ownedClasses));
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (!ownedClasses.contains(name)) return super.loadClass(name, resolve);
            synchronized (this) {
                Class<?> loaded = findLoadedClass(name);
                if (loaded == null) loaded = findClass(name);
                if (resolve) resolveClass(loaded);
                return loaded;
            }
        }
    }
}
