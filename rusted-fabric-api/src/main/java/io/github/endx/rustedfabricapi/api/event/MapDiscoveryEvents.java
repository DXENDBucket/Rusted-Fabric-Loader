package io.github.endx.rustedfabricapi.api.event;

public final class MapDiscoveryEvents {
    public static final RustedFabricEvent<BeforeExtraMapsForPath> BEFORE_EXTRA_MAPS_FOR_PATH =
            RustedFabricEvent.create(listeners -> (modManager, originalMaps, mapPath) -> {
                boolean cancelled = false;
                for (BeforeExtraMapsForPath listener : listeners) {
                    cancelled |= listener.beforeExtraMapsForPath(modManager, originalMaps, mapPath);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterExtraMapsForPath> AFTER_EXTRA_MAPS_FOR_PATH =
            RustedFabricEvent.create(listeners -> (modManager, originalMaps, mapPath, currentResult) -> {
                String[] result = currentResult;
                for (AfterExtraMapsForPath listener : listeners) {
                    result = listener.afterExtraMapsForPath(modManager, originalMaps, mapPath, result);
                }
                return result;
            });

    public static final RustedFabricEvent<BeforeMapListDirectoryScan> BEFORE_MAP_LIST_DIRECTORY_SCAN =
            RustedFabricEvent.create(listeners -> (path, includeDirectories) -> {
                boolean cancelled = false;
                for (BeforeMapListDirectoryScan listener : listeners) {
                    cancelled |= listener.beforeMapListDirectoryScan(path, includeDirectories);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterMapListDirectoryScan> AFTER_MAP_LIST_DIRECTORY_SCAN =
            RustedFabricEvent.create(listeners -> (path, includeDirectories, currentResult) -> {
                String[] result = currentResult;
                for (AfterMapListDirectoryScan listener : listeners) {
                    result = listener.afterMapListDirectoryScan(path, includeDirectories, result);
                }
                return result;
            });

    public static final RustedFabricEvent<AfterExtraMapRecordAdded> AFTER_EXTRA_MAP_RECORD_ADDED =
            RustedFabricEvent.create(listeners -> (modManager, originalPath, modInfo, extraMapRecord) -> {
                for (AfterExtraMapRecordAdded listener : listeners) {
                    listener.afterExtraMapRecordAdded(modManager, originalPath, modInfo, extraMapRecord);
                }
            });

    public static final RustedFabricEvent<AfterMultiplayerMapDropdownBuilt> AFTER_MULTIPLAYER_MAP_DROPDOWN_BUILT =
            RustedFabricEvent.create(listeners -> (multiplayerScript, rootElement, mapsElementId, typeElementId, rawMaps) -> {
                for (AfterMultiplayerMapDropdownBuilt listener : listeners) {
                    listener.afterMultiplayerMapDropdownBuilt(multiplayerScript, rootElement, mapsElementId, typeElementId, rawMaps);
                }
            });

    public static final RustedFabricEvent<BeforeMapStartFromAndroidUi> BEFORE_MAP_START_FROM_ANDROID_UI =
            RustedFabricEvent.create(listeners -> (mapPath, customMap, playerCount, aiDifficulty, fog, revealedMap) -> {
                boolean cancelled = false;
                for (BeforeMapStartFromAndroidUi listener : listeners) {
                    cancelled |= listener.beforeMapStartFromAndroidUi(mapPath, customMap, playerCount, aiDifficulty, fog, revealedMap);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<BeforeNetworkMapPathResolve> BEFORE_NETWORK_MAP_PATH_RESOLVE =
            RustedFabricEvent.create(listeners -> (networkEngine, gameSetup, mapPath, mapType) -> {
                String result = null;
                for (BeforeNetworkMapPathResolve listener : listeners) {
                    String override = listener.beforeNetworkMapPathResolve(networkEngine, gameSetup, mapPath, mapType);
                    if (override != null) {
                        result = override;
                    }
                }
                return result;
            });

    private MapDiscoveryEvents() {
    }

    @FunctionalInterface
    public interface BeforeExtraMapsForPath {
        boolean beforeExtraMapsForPath(Object modManager, String[] originalMaps, String mapPath);
    }

    @FunctionalInterface
    public interface AfterExtraMapsForPath {
        String[] afterExtraMapsForPath(Object modManager, String[] originalMaps, String mapPath, String[] currentResult);
    }

    @FunctionalInterface
    public interface BeforeMapListDirectoryScan {
        boolean beforeMapListDirectoryScan(String path, boolean includeDirectories);
    }

    @FunctionalInterface
    public interface AfterMapListDirectoryScan {
        String[] afterMapListDirectoryScan(String path, boolean includeDirectories, String[] currentResult);
    }

    @FunctionalInterface
    public interface AfterExtraMapRecordAdded {
        void afterExtraMapRecordAdded(Object modManager, String originalPath, Object modInfo, Object extraMapRecord);
    }

    @FunctionalInterface
    public interface AfterMultiplayerMapDropdownBuilt {
        void afterMultiplayerMapDropdownBuilt(Object multiplayerScript, Object rootElement, String mapsElementId, String typeElementId, String[] rawMaps);
    }

    @FunctionalInterface
    public interface BeforeMapStartFromAndroidUi {
        boolean beforeMapStartFromAndroidUi(String mapPath, boolean customMap, int playerCount, int aiDifficulty, boolean fog, boolean revealedMap);
    }

    @FunctionalInterface
    public interface BeforeNetworkMapPathResolve {
        String beforeNetworkMapPathResolve(Object networkEngine, Object gameSetup, String mapPath, Object mapType);
    }
}
