package io.github.endx.rustedfabricapi.api.event;

public final class AudioRuntimeEvents {
    private AudioRuntimeEvents() {
    }

    public static final RustedFabricEvent<BeforeSoundFactoryInit> BEFORE_SOUND_FACTORY_INIT =
            RustedFabricEvent.create(listeners -> (factory, androidContext) -> {
                boolean cancelled = false;
                for (BeforeSoundFactoryInit listener : listeners) {
                    cancelled |= listener.beforeSoundFactoryInit(factory, androidContext);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterSoundFactoryInit> AFTER_SOUND_FACTORY_INIT =
            RustedFabricEvent.create(listeners -> (factory, androidContext) -> {
                for (AfterSoundFactoryInit listener : listeners) {
                    listener.afterSoundFactoryInit(factory, androidContext);
                }
            });

    public static final RustedFabricEvent<BeforeLoadBuiltinSound> BEFORE_LOAD_BUILTIN_SOUND =
            RustedFabricEvent.create(listeners -> (factory, resourceId) -> {
                Object override = null;
                for (BeforeLoadBuiltinSound listener : listeners) {
                    Object value = listener.beforeLoadBuiltinSound(factory, resourceId);
                    if (value != null) {
                        override = value;
                    }
                }
                return override;
            });

    public static final RustedFabricEvent<AfterLoadBuiltinSound> AFTER_LOAD_BUILTIN_SOUND =
            RustedFabricEvent.create(listeners -> (factory, resourceId, sound) -> {
                Object result = sound;
                for (AfterLoadBuiltinSound listener : listeners) {
                    result = listener.afterLoadBuiltinSound(factory, resourceId, result);
                }
                return result;
            });

    public static final RustedFabricEvent<BeforeLoadSoundFromStream> BEFORE_LOAD_SOUND_FROM_STREAM =
            RustedFabricEvent.create(listeners -> (factory, name, inputStream, strict) -> {
                Object override = null;
                for (BeforeLoadSoundFromStream listener : listeners) {
                    Object value = listener.beforeLoadSoundFromStream(factory, name, inputStream, strict);
                    if (value != null) {
                        override = value;
                    }
                }
                return override;
            });

    public static final RustedFabricEvent<AfterLoadSoundFromStream> AFTER_LOAD_SOUND_FROM_STREAM =
            RustedFabricEvent.create(listeners -> (factory, name, inputStream, strict, sound) -> {
                Object result = sound;
                for (AfterLoadSoundFromStream listener : listeners) {
                    result = listener.afterLoadSoundFromStream(factory, name, inputStream, strict, result);
                }
                return result;
            });

    public static final RustedFabricEvent<BeforeGameSoundPlay> BEFORE_GAME_SOUND_PLAY =
            RustedFabricEvent.create(listeners -> (gameSound, leftVolume, rightVolume, priority, loop, pitch) -> {
                boolean cancelled = false;
                for (BeforeGameSoundPlay listener : listeners) {
                    cancelled |= listener.beforeGameSoundPlay(gameSound, leftVolume, rightVolume, priority, loop, pitch);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterGameSoundPlay> AFTER_GAME_SOUND_PLAY =
            RustedFabricEvent.create(listeners -> (gameSound, leftVolume, rightVolume, priority, loop, pitch) -> {
                for (AfterGameSoundPlay listener : listeners) {
                    listener.afterGameSoundPlay(gameSound, leftVolume, rightVolume, priority, loop, pitch);
                }
            });

    public static final RustedFabricEvent<BeforeGameSoundPlayNow> BEFORE_GAME_SOUND_PLAY_NOW =
            RustedFabricEvent.create(listeners -> (gameSound, leftVolume, rightVolume, priority, loop, pitch) -> {
                boolean cancelled = false;
                for (BeforeGameSoundPlayNow listener : listeners) {
                    cancelled |= listener.beforeGameSoundPlayNow(gameSound, leftVolume, rightVolume, priority, loop, pitch);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterGameSoundPlayNow> AFTER_GAME_SOUND_PLAY_NOW =
            RustedFabricEvent.create(listeners -> (gameSound, leftVolume, rightVolume, priority, loop, pitch) -> {
                for (AfterGameSoundPlayNow listener : listeners) {
                    listener.afterGameSoundPlayNow(gameSound, leftVolume, rightVolume, priority, loop, pitch);
                }
            });

    public static final RustedFabricEvent<BeforeSoundPlayTaskRun> BEFORE_SOUND_PLAY_TASK_RUN =
            RustedFabricEvent.create(listeners -> playTask -> {
                boolean cancelled = false;
                for (BeforeSoundPlayTaskRun listener : listeners) {
                    cancelled |= listener.beforeSoundPlayTaskRun(playTask);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterSoundPlayTaskRun> AFTER_SOUND_PLAY_TASK_RUN =
            RustedFabricEvent.create(listeners -> playTask -> {
                for (AfterSoundPlayTaskRun listener : listeners) {
                    listener.afterSoundPlayTaskRun(playTask);
                }
            });

    public static final RustedFabricEvent<BeforeOpenALNewSound> BEFORE_OPENAL_NEW_SOUND =
            RustedFabricEvent.create(listeners -> (audio, fileHandle) -> {
                Object override = null;
                for (BeforeOpenALNewSound listener : listeners) {
                    Object value = listener.beforeOpenALNewSound(audio, fileHandle);
                    if (value != null) {
                        override = value;
                    }
                }
                return override;
            });

    public static final RustedFabricEvent<AfterOpenALNewSound> AFTER_OPENAL_NEW_SOUND =
            RustedFabricEvent.create(listeners -> (audio, fileHandle, sound) -> {
                Object result = sound;
                for (AfterOpenALNewSound listener : listeners) {
                    result = listener.afterOpenALNewSound(audio, fileHandle, result);
                }
                return result;
            });

    public static final RustedFabricEvent<BeforeOpenALNewMusic> BEFORE_OPENAL_NEW_MUSIC =
            RustedFabricEvent.create(listeners -> (audio, fileHandle) -> {
                Object override = null;
                for (BeforeOpenALNewMusic listener : listeners) {
                    Object value = listener.beforeOpenALNewMusic(audio, fileHandle);
                    if (value != null) {
                        override = value;
                    }
                }
                return override;
            });

    public static final RustedFabricEvent<AfterOpenALNewMusic> AFTER_OPENAL_NEW_MUSIC =
            RustedFabricEvent.create(listeners -> (audio, fileHandle, music) -> {
                Object result = music;
                for (AfterOpenALNewMusic listener : listeners) {
                    result = listener.afterOpenALNewMusic(audio, fileHandle, result);
                }
                return result;
            });

    public static final RustedFabricEvent<BeforeMusicTrackLoad> BEFORE_MUSIC_TRACK_LOAD =
            RustedFabricEvent.create(listeners -> (factory, path) -> {
                Object override = null;
                for (BeforeMusicTrackLoad listener : listeners) {
                    Object value = listener.beforeMusicTrackLoad(factory, path);
                    if (value != null) {
                        override = value;
                    }
                }
                return override;
            });

    public static final RustedFabricEvent<AfterMusicTrackLoad> AFTER_MUSIC_TRACK_LOAD =
            RustedFabricEvent.create(listeners -> (factory, path, track) -> {
                Object result = track;
                for (AfterMusicTrackLoad listener : listeners) {
                    result = listener.afterMusicTrackLoad(factory, path, result);
                }
                return result;
            });

    public static final RustedFabricEvent<AfterNewMusicPlayer> AFTER_NEW_MUSIC_PLAYER =
            RustedFabricEvent.create(listeners -> (factory, player) -> {
                Object result = player;
                for (AfterNewMusicPlayer listener : listeners) {
                    result = listener.afterNewMusicPlayer(factory, result);
                }
                return result;
            });

    public static final RustedFabricEvent<BeforeMusicPlayerSetTrack> BEFORE_MUSIC_PLAYER_SET_TRACK =
            RustedFabricEvent.create(listeners -> (player, track) -> {
                boolean cancelled = false;
                for (BeforeMusicPlayerSetTrack listener : listeners) {
                    cancelled |= listener.beforeMusicPlayerSetTrack(player, track);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterMusicPlayerSetTrack> AFTER_MUSIC_PLAYER_SET_TRACK =
            RustedFabricEvent.create(listeners -> (player, track) -> {
                for (AfterMusicPlayerSetTrack listener : listeners) {
                    listener.afterMusicPlayerSetTrack(player, track);
                }
            });

    public static final RustedFabricEvent<BeforeMusicPlayerQueuePlay> BEFORE_MUSIC_PLAYER_QUEUE_PLAY =
            RustedFabricEvent.create(listeners -> (player, loop) -> {
                boolean cancelled = false;
                for (BeforeMusicPlayerQueuePlay listener : listeners) {
                    cancelled |= listener.beforeMusicPlayerQueuePlay(player, loop);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterMusicPlayerQueuePlay> AFTER_MUSIC_PLAYER_QUEUE_PLAY =
            RustedFabricEvent.create(listeners -> (player, loop) -> {
                for (AfterMusicPlayerQueuePlay listener : listeners) {
                    listener.afterMusicPlayerQueuePlay(player, loop);
                }
            });

    public static final RustedFabricEvent<BeforeMusicPlayerControl> BEFORE_MUSIC_PLAYER_CONTROL =
            RustedFabricEvent.create(listeners -> (player, operation) -> {
                boolean cancelled = false;
                for (BeforeMusicPlayerControl listener : listeners) {
                    cancelled |= listener.beforeMusicPlayerControl(player, operation);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterMusicPlayerControl> AFTER_MUSIC_PLAYER_CONTROL =
            RustedFabricEvent.create(listeners -> (player, operation) -> {
                for (AfterMusicPlayerControl listener : listeners) {
                    listener.afterMusicPlayerControl(player, operation);
                }
            });

    @FunctionalInterface
    public interface BeforeSoundFactoryInit {
        boolean beforeSoundFactoryInit(Object factory, Object androidContext);
    }

    @FunctionalInterface
    public interface AfterSoundFactoryInit {
        void afterSoundFactoryInit(Object factory, Object androidContext);
    }

    @FunctionalInterface
    public interface BeforeLoadBuiltinSound {
        Object beforeLoadBuiltinSound(Object factory, int resourceId);
    }

    @FunctionalInterface
    public interface AfterLoadBuiltinSound {
        Object afterLoadBuiltinSound(Object factory, int resourceId, Object sound);
    }

    @FunctionalInterface
    public interface BeforeLoadSoundFromStream {
        Object beforeLoadSoundFromStream(Object factory, String name, Object inputStream, boolean strict);
    }

    @FunctionalInterface
    public interface AfterLoadSoundFromStream {
        Object afterLoadSoundFromStream(Object factory, String name, Object inputStream, boolean strict, Object sound);
    }

    @FunctionalInterface
    public interface BeforeGameSoundPlay {
        boolean beforeGameSoundPlay(Object gameSound, float leftVolume, float rightVolume, int priority, int loop, float pitch);
    }

    @FunctionalInterface
    public interface AfterGameSoundPlay {
        void afterGameSoundPlay(Object gameSound, float leftVolume, float rightVolume, int priority, int loop, float pitch);
    }

    @FunctionalInterface
    public interface BeforeGameSoundPlayNow {
        boolean beforeGameSoundPlayNow(Object gameSound, float leftVolume, float rightVolume, int priority, int loop, float pitch);
    }

    @FunctionalInterface
    public interface AfterGameSoundPlayNow {
        void afterGameSoundPlayNow(Object gameSound, float leftVolume, float rightVolume, int priority, int loop, float pitch);
    }

    @FunctionalInterface
    public interface BeforeSoundPlayTaskRun {
        boolean beforeSoundPlayTaskRun(Object playTask);
    }

    @FunctionalInterface
    public interface AfterSoundPlayTaskRun {
        void afterSoundPlayTaskRun(Object playTask);
    }

    @FunctionalInterface
    public interface BeforeOpenALNewSound {
        Object beforeOpenALNewSound(Object audio, Object fileHandle);
    }

    @FunctionalInterface
    public interface AfterOpenALNewSound {
        Object afterOpenALNewSound(Object audio, Object fileHandle, Object sound);
    }

    @FunctionalInterface
    public interface BeforeOpenALNewMusic {
        Object beforeOpenALNewMusic(Object audio, Object fileHandle);
    }

    @FunctionalInterface
    public interface AfterOpenALNewMusic {
        Object afterOpenALNewMusic(Object audio, Object fileHandle, Object music);
    }

    @FunctionalInterface
    public interface BeforeMusicTrackLoad {
        Object beforeMusicTrackLoad(Object factory, String path);
    }

    @FunctionalInterface
    public interface AfterMusicTrackLoad {
        Object afterMusicTrackLoad(Object factory, String path, Object track);
    }

    @FunctionalInterface
    public interface AfterNewMusicPlayer {
        Object afterNewMusicPlayer(Object factory, Object player);
    }

    @FunctionalInterface
    public interface BeforeMusicPlayerSetTrack {
        boolean beforeMusicPlayerSetTrack(Object player, Object track);
    }

    @FunctionalInterface
    public interface AfterMusicPlayerSetTrack {
        void afterMusicPlayerSetTrack(Object player, Object track);
    }

    @FunctionalInterface
    public interface BeforeMusicPlayerQueuePlay {
        boolean beforeMusicPlayerQueuePlay(Object player, boolean loop);
    }

    @FunctionalInterface
    public interface AfterMusicPlayerQueuePlay {
        void afterMusicPlayerQueuePlay(Object player, boolean loop);
    }

    @FunctionalInterface
    public interface BeforeMusicPlayerControl {
        boolean beforeMusicPlayerControl(Object player, String operation);
    }

    @FunctionalInterface
    public interface AfterMusicPlayerControl {
        void afterMusicPlayerControl(Object player, String operation);
    }
}
