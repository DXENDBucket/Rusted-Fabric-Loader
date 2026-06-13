package io.github.endx.rustedfabricapi.api.diagnostic;

import io.github.endx.rustedfabricapi.api.util.RustedReflection;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FileSystemDiagnostics {
    private static final String[] GAME_FILE_SYSTEM_CLASSES = {
            "rustedwarfare.io.GameFileSystem",
            "com.corrodinggames.rts.gameFramework.e.a"
    };
    private static final String[] STORAGE_BACKEND_CAPABILITIES_CLASSES = {
            "rustedwarfare.io.StorageBackendCapabilities",
            "com.corrodinggames.rts.gameFramework.e.b"
    };
    private static final String[] FILE_SYSTEM_BACKEND_CLASSES = {
            "rustedwarfare.io.FileSystemBackend",
            "com.corrodinggames.rts.gameFramework.e.c"
    };
    private static final String[] ROOTED_FILE_SYSTEM_BACKEND_CLASSES = {
            "rustedwarfare.io.RootedFileSystemBackend",
            "com.corrodinggames.rts.gameFramework.e.d"
    };
    private static final String[] COMPOSITE_FILE_SYSTEM_BACKEND_CLASSES = {
            "rustedwarfare.io.CompositeFileSystemBackend",
            "com.corrodinggames.rts.gameFramework.e.e"
    };
    private static final String[] NULL_FILE_SYSTEM_BACKEND_CLASSES = {
            "rustedwarfare.io.NullFileSystemBackend",
            "com.corrodinggames.rts.gameFramework.e.f"
    };
    private static final String[] ASSET_CACHE_STORE_CLASSES = {
            "rustedwarfare.io.AssetCacheStore",
            "com.corrodinggames.rts.gameFramework.e.g"
    };
    private static final String[] CACHED_INPUT_STREAM_HANDLE_CLASSES = {
            "rustedwarfare.io.CachedInputStreamHandle",
            "com.corrodinggames.rts.gameFramework.e.h"
    };

    private FileSystemDiagnostics() {
    }

    public static Object defaultBackend() {
        return RustedReflection.getStaticFieldValue(GAME_FILE_SYSTEM_CLASSES, new String[]{"defaultBackend", "a"});
    }

    public static Object activeBackend() {
        return RustedReflection.getStaticFieldValue(GAME_FILE_SYSTEM_CLASSES, new String[]{"activeBackend", "b"});
    }

    public static Map<String, Object> describeGameFileSystemState() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        Object defaultBackend = defaultBackend();
        Object activeBackend = activeBackend();
        result.put("defaultBackend", defaultBackend);
        result.put("activeBackend", activeBackend);
        result.put("defaultBackendDescription", defaultBackend != null
                ? describeFileSystemBackend(defaultBackend)
                : Collections.emptyMap());
        result.put("activeBackendDescription", activeBackend != null
                ? describeFileSystemBackend(activeBackend)
                : Collections.emptyMap());
        putOptionalStaticField(result, "cachedStorageCapabilityOverride",
                GAME_FILE_SYSTEM_CLASSES, new String[]{"cachedStorageCapabilityOverride", "c"});
        putOptionalStaticField(result, "overriddenExternalPath",
                GAME_FILE_SYSTEM_CLASSES, new String[]{"overriddenExternalPath", "d"});
        putOptionalStaticField(result, "storageSelectionWarning",
                GAME_FILE_SYSTEM_CLASSES, new String[]{"storageSelectionWarning", "e"});
        return Collections.unmodifiableMap(result);
    }

    public static void initializeFileSystemBackend() {
        RustedReflection.invokeStatic(GAME_FILE_SYSTEM_CLASSES, new String[]{"initializeFileSystemBackend", "b"});
    }

    public static String getInternalAppPath() {
        return stringValue(RustedReflection.invokeStatic(GAME_FILE_SYSTEM_CLASSES, new String[]{"getInternalAppPath", "a"}));
    }

    public static Object getStorageBackendCapabilities(boolean includeOverride) {
        return RustedReflection.invokeStatic(GAME_FILE_SYSTEM_CLASSES,
                new String[]{"getStorageBackendCapabilities", "a"}, Boolean.valueOf(includeOverride));
    }

    public static Map<String, Object> describeStorageBackendCapabilities(Object capabilities) {
        requireAny(capabilities, STORAGE_BACKEND_CAPABILITIES_CLASSES, "StorageBackendCapabilities");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putBooleanField(result, capabilities, "directExternalFileAccess",
                new String[]{"directExternalFileAccess", "a"});
        putBooleanField(result, capabilities, "newStorageFrameworkSupported",
                new String[]{"newStorageFrameworkSupported", "b"});
        putBooleanField(result, capabilities, "directFileAccessAvailable",
                new String[]{"directFileAccessAvailable", "c"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeStorageBackendCapabilities(boolean includeOverride) {
        return describeStorageBackendCapabilities(getStorageBackendCapabilities(includeOverride));
    }

    public static Object createBackendForStorageType(int storageType) {
        return RustedReflection.invokeStatic(GAME_FILE_SYSTEM_CLASSES,
                new String[]{"createBackendForStorageType", "a"}, Integer.valueOf(storageType));
    }

    public static Object newRootedBackend(String rootPath, String storageName) {
        return RustedReflection.newInstance(ROOTED_FILE_SYSTEM_BACKEND_CLASSES, rootPath, storageName);
    }

    public static Object newCompositeBackend(Object primaryBackend, String primaryPathPrefix,
                                             Object secondaryBackend, String secondaryPathPrefix) {
        requireFileSystemBackend(primaryBackend);
        requireFileSystemBackend(secondaryBackend);
        return RustedReflection.newInstance(COMPOSITE_FILE_SYSTEM_BACKEND_CLASSES,
                primaryBackend, primaryPathPrefix, secondaryBackend, secondaryPathPrefix);
    }

    public static Object newNullBackend() {
        return RustedReflection.newInstance(NULL_FILE_SYSTEM_BACKEND_CLASSES);
    }

    public static boolean isFileSystemBackend(Object value) {
        return value != null && RustedReflection.isAnyClass(value.getClass(), FILE_SYSTEM_BACKEND_CLASSES);
    }

    public static boolean isRootedFileSystemBackend(Object value) {
        return value != null && RustedReflection.isAnyClass(value.getClass(), ROOTED_FILE_SYSTEM_BACKEND_CLASSES);
    }

    public static boolean isCompositeFileSystemBackend(Object value) {
        return value != null && RustedReflection.isAnyClass(value.getClass(), COMPOSITE_FILE_SYSTEM_BACKEND_CLASSES);
    }

    public static boolean isNullFileSystemBackend(Object value) {
        return value != null && RustedReflection.isAnyClass(value.getClass(), NULL_FILE_SYSTEM_BACKEND_CLASSES);
    }

    public static Map<String, Object> describeFileSystemBackend(Object backend) {
        requireFileSystemBackend(backend);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", backend.getClass().getName());
        result.put("rooted", Boolean.valueOf(isRootedFileSystemBackend(backend)));
        result.put("composite", Boolean.valueOf(isCompositeFileSystemBackend(backend)));
        result.put("nullBackend", Boolean.valueOf(isNullFileSystemBackend(backend)));
        putStringField(result, backend, "logPrefix", new String[]{"logPrefix", "a"});
        putBooleanField(result, backend, "verboseLogging", new String[]{"verboseLogging", "b"});
        putBooleanField(result, backend, "useAbstractPaths", new String[]{"useAbstractPaths", "c"});
        putBooleanField(result, backend, "allowDirectAccess", new String[]{"allowDirectAccess", "d"});
        putStringField(result, backend, "pendingErrorMessage", new String[]{"pendingErrorMessage", "e"});
        putStringField(result, backend, "cachedRootPath", new String[]{"cachedRootPath", "f"});
        result.put("backendName", backendStringOrEmpty(backend, new String[]{"getBackendName", "d"}));
        result.put("externalStoragePath", backendStringOrEmpty(backend, new String[]{"getExternalStoragePath", "b"}));
        result.put("documentsPath", backendStringOrEmpty(backend, new String[]{"getDocumentsPath", "c"}));
        result.put("supportsDirectPathAccess", Boolean.valueOf(backendBooleanOrFalse(backend,
                new String[]{"supportsDirectPathAccess", "e"})));
        putOptionalField(result, backend, "rootPath", new String[]{"rootPath", "g"});
        putOptionalField(result, backend, "storageName", new String[]{"storageName", "h"});
        putOptionalField(result, backend, "displayPrefix", new String[]{"displayPrefix", "i"});
        putOptionalField(result, backend, "primaryBackend", new String[]{"primaryBackend", "g"});
        putOptionalField(result, backend, "secondaryBackend", new String[]{"secondaryBackend", "h"});
        putOptionalField(result, backend, "primaryPathPrefix", new String[]{"primaryPathPrefix", "i"});
        putOptionalField(result, backend, "secondaryPathPrefix", new String[]{"secondaryPathPrefix", "j"});
        return Collections.unmodifiableMap(result);
    }

    public static String consumePendingErrorMessage() {
        return stringValue(RustedReflection.invokeStatic(GAME_FILE_SYSTEM_CLASSES,
                new String[]{"consumePendingErrorMessage", "c"}));
    }

    public static void setPendingErrorMessage(String message) {
        RustedReflection.invokeStatic(GAME_FILE_SYSTEM_CLASSES,
                new String[]{"setPendingErrorMessage", "b"}, message);
    }

    public static String consumeBackendPendingErrorMessage(Object backend) {
        requireFileSystemBackend(backend);
        return stringValue(RustedReflection.invokeInstance(backend, new String[]{"consumePendingErrorMessage", "a"}));
    }

    public static void setBackendPendingErrorMessage(Object backend, String message) {
        requireFileSystemBackend(backend);
        RustedReflection.invokeInstance(backend, new String[]{"setPendingErrorMessage", "a"}, message);
    }

    public static String findFileWithExtension(String path, String extension) {
        return stringValue(RustedReflection.invokeStatic(GAME_FILE_SYSTEM_CLASSES,
                new String[]{"findFileWithExtension", "a"}, path, extension));
    }

    public static String backendFindFileWithExtension(Object backend, String path, String extension) {
        requireFileSystemBackend(backend);
        return stringValue(RustedReflection.invokeInstance(backend,
                new String[]{"findFileWithExtension", "a"}, path, extension));
    }

    public static boolean isPathRootedOrSpecial(String path) {
        return Boolean.TRUE.equals(RustedReflection.invokeStatic(GAME_FILE_SYSTEM_CLASSES,
                new String[]{"isPathRootedOrSpecial", "c"}, path));
    }

    public static boolean backendIsPathRootedOrSpecial(Object backend, String path) {
        requireFileSystemBackend(backend);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(backend,
                new String[]{"isPathRootedOrSpecial", "b"}, path));
    }

    public static boolean backendIsPathAllowed(Object backend, String path) {
        requireFileSystemBackend(backend);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(backend,
                new String[]{"isPathAllowed", "c"}, path));
    }

    public static String toDisplayPath(String path) {
        return stringValue(RustedReflection.invokeStatic(GAME_FILE_SYSTEM_CLASSES,
                new String[]{"toDisplayPath", "d"}, path));
    }

    public static String backendToDisplayPath(Object backend, String path) {
        requireFileSystemBackend(backend);
        return stringValue(RustedReflection.invokeInstance(backend, new String[]{"toDisplayPath", "e"}, path));
    }

    public static String resolveAbstractPath(String path) {
        return stringValue(RustedReflection.invokeStatic(GAME_FILE_SYSTEM_CLASSES,
                new String[]{"resolveAbstractPath", "e"}, path));
    }

    public static String backendResolveAbstractPath(Object backend, String path) {
        requireFileSystemBackend(backend);
        return stringValue(RustedReflection.invokeInstance(backend, new String[]{"resolveAbstractPath", "f"}, path));
    }

    public static String backendNormalizeModPath(Object backend, String path) {
        requireFileSystemBackend(backend);
        return stringValue(RustedReflection.invokeInstance(backend, new String[]{"normalizeModPath", "d"}, path));
    }

    public static String backendReplaceStorageTokens(Object backend, String path) {
        requireFileSystemBackend(backend);
        return stringValue(RustedReflection.invokeInstance(backend, new String[]{"replaceStorageTokens", "n"}, path));
    }

    public static String normalizePath(String path) {
        return stringValue(RustedReflection.invokeStatic(GAME_FILE_SYSTEM_CLASSES, new String[]{"normalizePath", "o"}, path));
    }

    public static String backendNormalizePath(Object backend, String path) {
        requireFileSystemBackend(backend);
        return stringValue(RustedReflection.invokeInstance(backend, new String[]{"normalizePath", "o"}, path));
    }

    public static String resolveAssetPath(String path) {
        return stringValue(RustedReflection.invokeStatic(GAME_FILE_SYSTEM_CLASSES,
                new String[]{"resolveAssetPath", "p"}, path));
    }

    public static String getStorageDisplayName(String path) {
        return stringValue(RustedReflection.invokeStatic(GAME_FILE_SYSTEM_CLASSES,
                new String[]{"getStorageDisplayName", "n"}, path));
    }

    public static String backendGetStorageDisplayName(Object backend, String path) {
        requireFileSystemBackend(backend);
        return stringValue(RustedReflection.invokeInstance(backend,
                new String[]{"getStorageDisplayName", "m"}, path));
    }

    public static boolean isAssetFile(String path) {
        return Boolean.TRUE.equals(RustedReflection.invokeStatic(GAME_FILE_SYSTEM_CLASSES,
                new String[]{"isAssetFile", "a"}, path));
    }

    public static boolean backendIsAssetFile(Object backend, String path) {
        requireFileSystemBackend(backend);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(backend, new String[]{"isAssetFile", "p"}, path));
    }

    public static boolean exists(String path) {
        return Boolean.TRUE.equals(RustedReflection.invokeStatic(GAME_FILE_SYSTEM_CLASSES,
                new String[]{"exists", "i"}, path));
    }

    public static boolean backendExists(Object backend, String path) {
        requireFileSystemBackend(backend);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(backend, new String[]{"exists", "g"}, path));
    }

    public static boolean isDirectory(String path) {
        return Boolean.TRUE.equals(RustedReflection.invokeStatic(GAME_FILE_SYSTEM_CLASSES,
                new String[]{"isDirectory", "f"}, path));
    }

    public static boolean isDirectoryStrict(String path) {
        return Boolean.TRUE.equals(RustedReflection.invokeStatic(GAME_FILE_SYSTEM_CLASSES,
                new String[]{"isDirectoryStrict", "g"}, path));
    }

    public static boolean backendIsDirectory(Object backend, String path, boolean strict) {
        requireFileSystemBackend(backend);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(backend,
                new String[]{"isDirectory", "a"}, path, Boolean.valueOf(strict)));
    }

    public static List<String> listDirectory(String path) {
        return stringArrayList(RustedReflection.invokeStatic(GAME_FILE_SYSTEM_CLASSES,
                new String[]{"listDirectory", "h"}, path));
    }

    public static List<String> listDirectoryFiltered(String path, boolean includeDirectories) {
        return stringArrayList(RustedReflection.invokeStatic(GAME_FILE_SYSTEM_CLASSES,
                new String[]{"listDirectoryFiltered", "a"}, path, Boolean.valueOf(includeDirectories)));
    }

    public static List<String> backendListDirectory(Object backend, String path, boolean includeDirectories) {
        requireFileSystemBackend(backend);
        return stringArrayList(RustedReflection.invokeInstance(backend,
                new String[]{"listDirectory", "b"}, path, Boolean.valueOf(includeDirectories)));
    }

    public static Object backendToFile(Object backend, String path) {
        requireFileSystemBackend(backend);
        return RustedReflection.invokeInstance(backend, new String[]{"toFile", "h"}, path);
    }

    public static Object openInputStream(String path) {
        return RustedReflection.invokeStatic(GAME_FILE_SYSTEM_CLASSES, new String[]{"openInputStream", "k"}, path);
    }

    public static Object backendOpenInputStream(Object backend, String path) {
        requireFileSystemBackend(backend);
        return RustedReflection.invokeInstance(backend, new String[]{"openInputStream", "j"}, path);
    }

    public static Object openAssetInputStream(String path) {
        return RustedReflection.invokeStatic(GAME_FILE_SYSTEM_CLASSES, new String[]{"openAssetInputStream", "j"}, path);
    }

    public static Object backendOpenAssetInputStream(Object backend, String path) {
        requireFileSystemBackend(backend);
        return RustedReflection.invokeInstance(backend, new String[]{"openAssetInputStream", "i"}, path);
    }

    public static Object openInputStreamFromFile(File file) {
        return RustedReflection.invokeStatic(GAME_FILE_SYSTEM_CLASSES,
                new String[]{"openInputStreamFromFile", "a"}, file);
    }

    public static OutputStream openOutputStreamFromFile(File file, boolean append) {
        return (OutputStream) RustedReflection.invokeStatic(GAME_FILE_SYSTEM_CLASSES,
                new String[]{"openOutputStreamFromFile", "a"}, file, Boolean.valueOf(append));
    }

    public static OutputStream openOutputStream(String path, boolean append) {
        return (OutputStream) RustedReflection.invokeStatic(GAME_FILE_SYSTEM_CLASSES,
                new String[]{"openOutputStream", "b"}, path, Boolean.valueOf(append));
    }

    public static OutputStream backendOpenOutputStream(Object backend, String path, boolean append) {
        requireFileSystemBackend(backend);
        return (OutputStream) RustedReflection.invokeInstance(backend,
                new String[]{"openOutputStream", "c"}, path, Boolean.valueOf(append));
    }

    public static boolean createDirectories(String path) {
        return Boolean.TRUE.equals(RustedReflection.invokeStatic(GAME_FILE_SYSTEM_CLASSES,
                new String[]{"createDirectories", "l"}, path));
    }

    public static boolean backendCreateDirectories(Object backend, String path) {
        requireFileSystemBackend(backend);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(backend,
                new String[]{"createDirectories", "k"}, path));
    }

    public static void ensureParentDirectoryExists(File file) {
        RustedReflection.invokeStatic(GAME_FILE_SYSTEM_CLASSES,
                new String[]{"ensureParentDirectoryExists", "c"}, file);
    }

    public static void backendEnsureDirectoryExists(Object backend, File directory) {
        requireFileSystemBackend(backend);
        RustedReflection.invokeInstance(backend, new String[]{"ensureDirectoryExists", "a"}, directory);
    }

    public static String getExternalStoragePath() {
        return stringValue(RustedReflection.invokeStatic(GAME_FILE_SYSTEM_CLASSES,
                new String[]{"getExternalStoragePath", "d"}));
    }

    public static String backendGetExternalStoragePath(Object backend) {
        requireFileSystemBackend(backend);
        return stringValue(RustedReflection.invokeInstance(backend, new String[]{"getExternalStoragePath", "b"}));
    }

    public static String getDocumentsPath() {
        return stringValue(RustedReflection.invokeStatic(GAME_FILE_SYSTEM_CLASSES, new String[]{"getDocumentsPath", "e"}));
    }

    public static String backendGetDocumentsPath(Object backend) {
        requireFileSystemBackend(backend);
        return stringValue(RustedReflection.invokeInstance(backend, new String[]{"getDocumentsPath", "c"}));
    }

    public static long getLastModified(String path) {
        Object value = RustedReflection.invokeStatic(GAME_FILE_SYSTEM_CLASSES, new String[]{"getLastModified", "m"}, path);
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    public static long backendGetLastModified(Object backend, String path) {
        requireFileSystemBackend(backend);
        Object value = RustedReflection.invokeInstance(backend, new String[]{"getLastModified", "l"}, path);
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    public static File createTempFile(String prefix, String suffix, boolean preferExternal) {
        return (File) RustedReflection.invokeStatic(GAME_FILE_SYSTEM_CLASSES,
                new String[]{"createTempFile", "a"}, prefix, suffix, Boolean.valueOf(preferExternal));
    }

    public static File backendCreateTempFile(Object backend, String prefix, String suffix, boolean preferExternal) {
        requireFileSystemBackend(backend);
        return (File) RustedReflection.invokeInstance(backend,
                new String[]{"createTempFile", "a"}, prefix, suffix, Boolean.valueOf(preferExternal));
    }

    public static boolean renameFile(File from, File to) {
        return Boolean.TRUE.equals(RustedReflection.invokeStatic(GAME_FILE_SYSTEM_CLASSES,
                new String[]{"renameFile", "a"}, from, to));
    }

    public static boolean backendRenameFile(Object backend, File from, File to) {
        requireFileSystemBackend(backend);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(backend, new String[]{"renameFile", "a"}, from, to));
    }

    public static boolean copyFile(File from, File to) {
        return Boolean.TRUE.equals(RustedReflection.invokeStatic(GAME_FILE_SYSTEM_CLASSES,
                new String[]{"copyFile", "b"}, from, to));
    }

    public static boolean deleteFile(File file) {
        return Boolean.TRUE.equals(RustedReflection.invokeStatic(GAME_FILE_SYSTEM_CLASSES,
                new String[]{"deleteFile", "b"}, file));
    }

    public static boolean backendDeleteFile(Object backend, File file) {
        requireFileSystemBackend(backend);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(backend, new String[]{"deleteFile", "b"}, file));
    }

    public static boolean supportsDirectPathAccess() {
        return Boolean.TRUE.equals(RustedReflection.invokeStatic(GAME_FILE_SYSTEM_CLASSES,
                new String[]{"supportsDirectPathAccess", "f"}));
    }

    public static boolean backendSupportsDirectPathAccess(Object backend) {
        requireFileSystemBackend(backend);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(backend,
                new String[]{"supportsDirectPathAccess", "e"}));
    }

    public static Object getExternalFilesDirCompat(Object androidContext, String type, String label) {
        return RustedReflection.invokeStatic(GAME_FILE_SYSTEM_CLASSES,
                new String[]{"getExternalFilesDirCompat", "a"}, androidContext, type, label);
    }

    public static boolean isAssetCacheStoreAvailable() {
        return RustedReflection.tryFindClass(ASSET_CACHE_STORE_CLASSES[0]) != null
                || RustedReflection.tryFindClass(ASSET_CACHE_STORE_CLASSES[1]) != null;
    }

    public static boolean assetCacheEnabled() {
        try {
            Object value = RustedReflection.getStaticFieldValue(ASSET_CACHE_STORE_CLASSES, new String[]{"enabled", "a"});
            return Boolean.TRUE.equals(value);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    public static Map<String, Object> describeAssetCacheStore() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("available", Boolean.valueOf(isAssetCacheStoreAvailable()));
        result.put("enabled", Boolean.valueOf(assetCacheEnabled()));
        return Collections.unmodifiableMap(result);
    }

    public static String encodePathChar(char value) {
        return stringValue(RustedReflection.invokeStatic(ASSET_CACHE_STORE_CLASSES,
                new String[]{"encodePathChar", "a"}, Character.valueOf(value)));
    }

    public static String replaceCharForCacheKey(String path, char value) {
        return stringValue(RustedReflection.invokeStatic(ASSET_CACHE_STORE_CLASSES,
                new String[]{"replaceCharForCacheKey", "a"}, path, Character.valueOf(value)));
    }

    public static String encodeCacheKey(String path) {
        return stringValue(RustedReflection.invokeStatic(ASSET_CACHE_STORE_CLASSES,
                new String[]{"encodeCacheKey", "a"}, path));
    }

    public static String getCacheFilePath(String source, String key, boolean dataFile) {
        return stringValue(RustedReflection.invokeStatic(ASSET_CACHE_STORE_CLASSES,
                new String[]{"getCacheFilePath", "a"}, source, key, Boolean.valueOf(dataFile)));
    }

    public static String getCacheMetadataPath(String source, String key) {
        return stringValue(RustedReflection.invokeStatic(ASSET_CACHE_STORE_CLASSES,
                new String[]{"getCacheMetadataPath", "b"}, source, key));
    }

    public static boolean writeCacheMetadata(String source, String key, String metadata) {
        return Boolean.TRUE.equals(RustedReflection.invokeStatic(ASSET_CACHE_STORE_CLASSES,
                new String[]{"writeCacheMetadata", "a"}, source, key, metadata));
    }

    public static boolean writeAssetCacheData(String source, String key, InputStream inputStream) {
        return Boolean.TRUE.equals(RustedReflection.invokeStatic(ASSET_CACHE_STORE_CLASSES,
                new String[]{"writeAssetCacheData", "a"}, source, key, inputStream));
    }

    public static Object openCacheFileInputStream(String source, String key) {
        return RustedReflection.invokeStatic(ASSET_CACHE_STORE_CLASSES,
                new String[]{"openCacheFileInputStream", "a"}, source, key);
    }

    public static void deleteCacheEntry(String source, String key) {
        RustedReflection.invokeStatic(ASSET_CACHE_STORE_CLASSES, new String[]{"deleteCacheEntry", "c"}, source, key);
    }

    public static Object openCachedInputStreamHandle(String source, String key, String metadata) {
        return RustedReflection.invokeStatic(ASSET_CACHE_STORE_CLASSES,
                new String[]{"openCachedInputStreamHandle", "b"}, source, key, metadata);
    }

    public static List<String> listCachedAssetDirectory(String source, String key) {
        return stringArrayList(RustedReflection.invokeStatic(ASSET_CACHE_STORE_CLASSES,
                new String[]{"listCachedAssetDirectory", "d"}, source, key));
    }

    public static InputStream openAssetCached(String source, String key) {
        return (InputStream) RustedReflection.invokeStatic(ASSET_CACHE_STORE_CLASSES,
                new String[]{"openAssetCached", "e"}, source, key);
    }

    public static boolean isCachedAssetAvailable(String source, String key) {
        return Boolean.TRUE.equals(RustedReflection.invokeStatic(ASSET_CACHE_STORE_CLASSES,
                new String[]{"isCachedAssetAvailable", "f"}, source, key));
    }

    public static Object newCachedInputStreamHandle(InputStream inputStream) {
        return RustedReflection.newInstance(CACHED_INPUT_STREAM_HANDLE_CLASSES, inputStream);
    }

    public static boolean isCachedInputStreamHandle(Object value) {
        return value != null && RustedReflection.isAnyClass(value.getClass(), CACHED_INPUT_STREAM_HANDLE_CLASSES);
    }

    public static Map<String, Object> describeCachedInputStreamHandle(Object handle) {
        requireAny(handle, CACHED_INPUT_STREAM_HANDLE_CLASSES, "CachedInputStreamHandle");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putField(result, handle, "inputStream", new String[]{"inputStream", "a"});
        return Collections.unmodifiableMap(result);
    }

    public static void closeCachedInputStreamHandle(Object handle) {
        requireAny(handle, CACHED_INPUT_STREAM_HANDLE_CLASSES, "CachedInputStreamHandle");
        RustedReflection.invokeInstance(handle, new String[]{"close", "a"});
    }

    private static String backendStringOrEmpty(Object backend, String[] methodNames) {
        try {
            return stringValue(RustedReflection.invokeInstance(backend, methodNames));
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static boolean backendBooleanOrFalse(Object backend, String[] methodNames) {
        try {
            return Boolean.TRUE.equals(RustedReflection.invokeInstance(backend, methodNames));
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static List<String> stringArrayList(Object value) {
        if (value == null || !value.getClass().isArray()) {
            return Collections.emptyList();
        }
        int length = Array.getLength(value);
        List<String> result = new ArrayList<String>(length);
        for (int i = 0; i < length; i++) {
            Object item = Array.get(value, i);
            result.add(stringValue(item));
        }
        return Collections.unmodifiableList(result);
    }

    private static String stringValue(Object value) {
        return value != null ? value.toString() : null;
    }

    private static void requireFileSystemBackend(Object value) {
        requireAny(value, FILE_SYSTEM_BACKEND_CLASSES, "FileSystemBackend");
    }

    private static void requireAny(Object value, String[] classNames, String label) {
        if (value == null || !RustedReflection.isAnyClass(value.getClass(), classNames)) {
            throw new IllegalArgumentException("Expected " + label + ", got " + describe(value));
        }
    }

    private static String describe(Object value) {
        return value == null ? "null" : value.getClass().getName();
    }

    private static void putField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        result.put(key, RustedReflection.getFieldValue(owner, fieldNames));
    }

    private static void putOptionalField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        try {
            result.put(key, RustedReflection.getFieldValue(owner, fieldNames));
        } catch (RuntimeException ignored) {
        }
    }

    private static void putOptionalStaticField(Map<String, Object> result, String key,
                                               String[] classNames, String[] fieldNames) {
        try {
            result.put(key, RustedReflection.getStaticFieldValue(classNames, fieldNames));
        } catch (RuntimeException ignored) {
        }
    }

    private static void putStringField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        result.put(key, RustedReflection.getStringField(owner, fieldNames));
    }

    private static void putBooleanField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        result.put(key, Boolean.valueOf(RustedReflection.getBooleanField(owner, fieldNames)));
    }
}
