package io.github.endx.rustedfabricapi.api.event;

import java.io.InputStream;

public final class FileSystemEvents {
    private FileSystemEvents() {
    }

    public static final RustedFabricEvent<BeforeResolveAbstractPath> BEFORE_RESOLVE_ABSTRACT_PATH =
            RustedFabricEvent.create(listeners -> path -> {
                String override = null;
                for (BeforeResolveAbstractPath listener : listeners) {
                    String value = listener.beforeResolveAbstractPath(path);
                    if (value != null) {
                        override = value;
                    }
                }
                return override;
            });

    public static final RustedFabricEvent<AfterResolveAbstractPath> AFTER_RESOLVE_ABSTRACT_PATH =
            RustedFabricEvent.create(listeners -> (path, resolvedPath) -> {
                String result = resolvedPath;
                for (AfterResolveAbstractPath listener : listeners) {
                    String value = listener.afterResolveAbstractPath(path, result);
                    if (value != null) {
                        result = value;
                    }
                }
                return result;
            });

    public static final RustedFabricEvent<AfterToDisplayPath> AFTER_TO_DISPLAY_PATH =
            RustedFabricEvent.create(listeners -> (path, displayPath) -> {
                String result = displayPath;
                for (AfterToDisplayPath listener : listeners) {
                    String value = listener.afterToDisplayPath(path, result);
                    if (value != null) {
                        result = value;
                    }
                }
                return result;
            });

    public static final RustedFabricEvent<BeforeOpenAssetCached> BEFORE_OPEN_ASSET_CACHED =
            RustedFabricEvent.create(listeners -> (source, key) -> {
                InputStream override = null;
                for (BeforeOpenAssetCached listener : listeners) {
                    InputStream value = listener.beforeOpenAssetCached(source, key);
                    if (value != null) {
                        override = value;
                    }
                }
                return override;
            });

    public static final RustedFabricEvent<AfterOpenAssetCached> AFTER_OPEN_ASSET_CACHED =
            RustedFabricEvent.create(listeners -> (source, key, inputStream) -> {
                InputStream result = inputStream;
                for (AfterOpenAssetCached listener : listeners) {
                    InputStream value = listener.afterOpenAssetCached(source, key, result);
                    if (value != null) {
                        result = value;
                    }
                }
                return result;
            });

    public static final RustedFabricEvent<BeforeListCachedAssetDirectory> BEFORE_LIST_CACHED_ASSET_DIRECTORY =
            RustedFabricEvent.create(listeners -> (source, key) -> {
                String[] override = null;
                for (BeforeListCachedAssetDirectory listener : listeners) {
                    String[] value = listener.beforeListCachedAssetDirectory(source, key);
                    if (value != null) {
                        override = value;
                    }
                }
                return override;
            });

    public static final RustedFabricEvent<AfterListCachedAssetDirectory> AFTER_LIST_CACHED_ASSET_DIRECTORY =
            RustedFabricEvent.create(listeners -> (source, key, entries) -> {
                String[] result = entries;
                for (AfterListCachedAssetDirectory listener : listeners) {
                    String[] value = listener.afterListCachedAssetDirectory(source, key, result);
                    if (value != null) {
                        result = value;
                    }
                }
                return result;
            });

    @FunctionalInterface
    public interface BeforeResolveAbstractPath {
        String beforeResolveAbstractPath(String path);
    }

    @FunctionalInterface
    public interface AfterResolveAbstractPath {
        String afterResolveAbstractPath(String path, String resolvedPath);
    }

    @FunctionalInterface
    public interface AfterToDisplayPath {
        String afterToDisplayPath(String path, String displayPath);
    }

    @FunctionalInterface
    public interface BeforeOpenAssetCached {
        InputStream beforeOpenAssetCached(String source, String key);
    }

    @FunctionalInterface
    public interface AfterOpenAssetCached {
        InputStream afterOpenAssetCached(String source, String key, InputStream inputStream);
    }

    @FunctionalInterface
    public interface BeforeListCachedAssetDirectory {
        String[] beforeListCachedAssetDirectory(String source, String key);
    }

    @FunctionalInterface
    public interface AfterListCachedAssetDirectory {
        String[] afterListCachedAssetDirectory(String source, String key, String[] entries);
    }
}
