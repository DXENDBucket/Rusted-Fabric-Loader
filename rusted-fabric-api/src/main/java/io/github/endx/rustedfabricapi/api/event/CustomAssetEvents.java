package io.github.endx.rustedfabricapi.api.event;

public final class CustomAssetEvents {
    private CustomAssetEvents() {
    }

    public static final RustedFabricEvent<BeforeLoadImage> BEFORE_LOAD_IMAGE =
            RustedFabricEvent.create(listeners -> (path, basePath, smooth, metadata, section, key) -> {
                boolean cancelled = false;
                for (BeforeLoadImage listener : listeners) {
                    cancelled |= listener.beforeLoadImage(path, basePath, smooth, metadata, section, key);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterLoadImage> AFTER_LOAD_IMAGE =
            RustedFabricEvent.create(listeners -> (path, basePath, smooth, metadata, section, key, image) -> {
                Object result = image;
                for (AfterLoadImage listener : listeners) {
                    result = listener.afterLoadImage(path, basePath, smooth, metadata, section, key, result);
                }
                return result;
            });

    public static final RustedFabricEvent<BeforeCreateTeamColorImages> BEFORE_CREATE_TEAM_COLOR_IMAGES =
            RustedFabricEvent.create(listeners -> (metadata, sourceImage, teamColoringMode) -> {
                for (BeforeCreateTeamColorImages listener : listeners) {
                    listener.beforeCreateTeamColorImages(metadata, sourceImage, teamColoringMode);
                }
            });

    public static final RustedFabricEvent<AfterCreateTeamColorImages> AFTER_CREATE_TEAM_COLOR_IMAGES =
            RustedFabricEvent.create(listeners -> (metadata, sourceImage, teamColoringMode, images) -> {
                Object result = images;
                for (AfterCreateTeamColorImages listener : listeners) {
                    result = listener.afterCreateTeamColorImages(metadata, sourceImage, teamColoringMode, result);
                }
                return result;
            });

    public static final RustedFabricEvent<BeforeLoadSound> BEFORE_LOAD_SOUND =
            RustedFabricEvent.create(listeners -> (basePath, soundPath, metadata) -> {
                boolean cancelled = false;
                for (BeforeLoadSound listener : listeners) {
                    cancelled |= listener.beforeLoadSound(basePath, soundPath, metadata);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterLoadSound> AFTER_LOAD_SOUND =
            RustedFabricEvent.create(listeners -> (basePath, soundPath, metadata, sound) -> {
                Object result = sound;
                for (AfterLoadSound listener : listeners) {
                    result = listener.afterLoadSound(basePath, soundPath, metadata, result);
                }
                return result;
            });

    public static final RustedFabricEvent<BeforeParseSoundList> BEFORE_PARSE_SOUND_LIST =
            RustedFabricEvent.create(listeners -> (metadata, rawSoundList) -> {
                boolean cancelled = false;
                for (BeforeParseSoundList listener : listeners) {
                    cancelled |= listener.beforeParseSoundList(metadata, rawSoundList);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterParseSoundList> AFTER_PARSE_SOUND_LIST =
            RustedFabricEvent.create(listeners -> (metadata, rawSoundList, soundList) -> {
                Object result = soundList;
                for (AfterParseSoundList listener : listeners) {
                    result = listener.afterParseSoundList(metadata, rawSoundList, result);
                }
                return result;
            });

    public static final RustedFabricEvent<BeforeParseProjectileSpawnList> BEFORE_PARSE_PROJECTILE_SPAWN_LIST =
            RustedFabricEvent.create(listeners -> (metadata, rawList, section, key, requireSingle) -> {
                boolean cancelled = false;
                for (BeforeParseProjectileSpawnList listener : listeners) {
                    cancelled |= listener.beforeParseProjectileSpawnList(metadata, rawList, section, key, requireSingle);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterParseProjectileSpawnList> AFTER_PARSE_PROJECTILE_SPAWN_LIST =
            RustedFabricEvent.create(listeners -> (metadata, rawList, section, key, requireSingle, projectileSpawnList) -> {
                Object result = projectileSpawnList;
                for (AfterParseProjectileSpawnList listener : listeners) {
                    result = listener.afterParseProjectileSpawnList(metadata, rawList, section, key, requireSingle, result);
                }
                return result;
            });

    @FunctionalInterface
    public interface BeforeLoadImage {
        boolean beforeLoadImage(String path, String basePath, boolean smooth, Object metadata, String section, String key);
    }

    @FunctionalInterface
    public interface AfterLoadImage {
        Object afterLoadImage(String path, String basePath, boolean smooth, Object metadata, String section, String key, Object image);
    }

    @FunctionalInterface
    public interface BeforeCreateTeamColorImages {
        void beforeCreateTeamColorImages(Object metadata, Object sourceImage, Object teamColoringMode);
    }

    @FunctionalInterface
    public interface AfterCreateTeamColorImages {
        Object afterCreateTeamColorImages(Object metadata, Object sourceImage, Object teamColoringMode, Object images);
    }

    @FunctionalInterface
    public interface BeforeLoadSound {
        boolean beforeLoadSound(String basePath, String soundPath, Object metadata);
    }

    @FunctionalInterface
    public interface AfterLoadSound {
        Object afterLoadSound(String basePath, String soundPath, Object metadata, Object sound);
    }

    @FunctionalInterface
    public interface BeforeParseSoundList {
        boolean beforeParseSoundList(Object metadata, String rawSoundList);
    }

    @FunctionalInterface
    public interface AfterParseSoundList {
        Object afterParseSoundList(Object metadata, String rawSoundList, Object soundList);
    }

    @FunctionalInterface
    public interface BeforeParseProjectileSpawnList {
        boolean beforeParseProjectileSpawnList(Object metadata, String rawList, String section, String key, boolean requireSingle);
    }

    @FunctionalInterface
    public interface AfterParseProjectileSpawnList {
        Object afterParseProjectileSpawnList(Object metadata, String rawList, String section, String key, boolean requireSingle, Object projectileSpawnList);
    }
}
