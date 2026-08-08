package io.github.endx.rustedfabric.android.xposed.storage;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Binder;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import io.github.endx.rustedfabric.android.mod.ModRegistry;
import io.github.endx.rustedfabric.android.mod.ModVerificationException;
import io.github.endx.rustedfabric.android.mod.RustedFabricModVerifier;
import io.github.endx.rustedfabric.android.mod.VerifiedModArchive;
import io.github.endx.rustedfabric.android.xposed.BuildConfig;

/** Read-only bridge from Loader-private storage to the verified game process. */
public final class ModContentProvider extends ContentProvider {
    public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".mods";
    public static final Uri ENABLED_MODS_URI = Uri.parse("content://" + AUTHORITY + "/enabled");
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_VERSION = "version";
    public static final String COLUMN_ARCHIVE_SHA256 = "archive_sha256";
    public static final String COLUMN_DEX_SHA256 = "dex_sha256";
    public static final String COLUMN_API_VERSION = "api_version";
    public static final String COLUMN_MAPPING_PROFILES = "mapping_profiles";
    public static final String COLUMN_MULTIPLAYER_MODE = "multiplayer_mode";
    public static final String COLUMN_MULTIPLAYER_PROTOCOL = "multiplayer_protocol";
    public static final String COLUMN_MULTIPLAYER_SYNC_HASH = "multiplayer_sync_hash";

    private static final String[] COLUMNS = {COLUMN_ID, COLUMN_NAME, COLUMN_VERSION,
            COLUMN_ARCHIVE_SHA256, COLUMN_DEX_SHA256, COLUMN_API_VERSION,
            COLUMN_MAPPING_PROFILES, COLUMN_MULTIPLAYER_MODE,
            COLUMN_MULTIPLAYER_PROTOCOL, COLUMN_MULTIPLAYER_SYNC_HASH};

    private ModRegistry registry;

    @Override
    public boolean onCreate() {
        if (getContext() == null) {
            return false;
        }
        registry = ModStorage.registry(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        enforceCaller();
        if (!isEnabledUri(uri)) {
            throw new IllegalArgumentException("Unknown mod provider URI");
        }
        MatrixCursor cursor = new MatrixCursor(COLUMNS);
        try {
            List<ModRegistry.Record> records = registry.list();
            for (ModRegistry.Record record : records) {
                if (record.isEnabled()) {
                    cursor.addRow(new Object[]{record.getId(), record.getName(), record.getVersion(),
                            record.getArchiveSha256(), record.getDexSha256(),
                            record.getApiVersion(), String.join(",", record.getMappingProfiles()),
                            record.getMultiplayer().mode().wireName(),
                            record.getMultiplayer().protocol(),
                            record.getMultiplayer().syncHash()});
                }
            }
            cursor.setNotificationUri(getContext().getContentResolver(), ENABLED_MODS_URI);
            return cursor;
        } catch (IOException failure) {
            cursor.close();
            throw new IllegalStateException("Private mod registry cannot be read", failure);
        }
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        enforceCaller();
        if (!"r".equals(mode)) {
            throw new SecurityException("Mod provider is read-only");
        }
        String id = modId(uri);
        try {
            Optional<ModRegistry.Record> found = registry.find(id);
            if (!found.isPresent() || !found.get().isEnabled()) {
                throw new FileNotFoundException("Enabled mod is unavailable");
            }
            ModRegistry.Record record = found.get();
            File archive = registry.archivePath(record).toFile();
            VerifiedModArchive verified = new RustedFabricModVerifier().verify(archive.toPath());
            if (!record.getId().equals(verified.getMetadata().getId())
                    || !record.getArchiveSha256().equals(verified.getArchiveSha256())) {
                throw new FileNotFoundException("Enabled mod failed integrity verification");
            }
            return ParcelFileDescriptor.open(archive, ParcelFileDescriptor.MODE_READ_ONLY);
        } catch (IOException | ModVerificationException failure) {
            FileNotFoundException unavailable = new FileNotFoundException(
                    "Enabled mod cannot be opened");
            unavailable.initCause(failure);
            throw unavailable;
        }
    }

    @Override
    public String getType(Uri uri) {
        if (isEnabledUri(uri)) {
            return "vnd.android.cursor.dir/vnd.rustedfabric.mod";
        }
        if (uri.getPathSegments().size() == 2 && "mod".equals(uri.getPathSegments().get(0))) {
            return "application/vnd.rustedfabric.mod";
        }
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new SecurityException("Mod provider is read-only");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new SecurityException("Mod provider is read-only");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        throw new SecurityException("Mod provider is read-only");
    }

    public static Uri archiveUri(String id) {
        return new Uri.Builder().scheme("content").authority(AUTHORITY)
                .appendPath("mod").appendPath(id).build();
    }

    private void enforceCaller() {
        if (getContext() == null) {
            throw new SecurityException("Provider context is unavailable");
        }
        GameCallerAuthorizer.enforce(getContext(), Binder.getCallingUid());
    }

    private static boolean isEnabledUri(Uri uri) {
        return uri != null && AUTHORITY.equals(uri.getAuthority())
                && uri.getPathSegments().size() == 1
                && "enabled".equals(uri.getPathSegments().get(0));
    }

    private static String modId(Uri uri) throws FileNotFoundException {
        if (uri == null || !AUTHORITY.equals(uri.getAuthority())
                || uri.getPathSegments().size() != 2
                || !"mod".equals(uri.getPathSegments().get(0))) {
            throw new FileNotFoundException("Unknown mod URI");
        }
        return uri.getPathSegments().get(1);
    }
}
