package io.github.endx.rustedfabricapi.api.diagnostic;

import io.github.endx.rustedfabricapi.api.util.RustedReflection;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class AudioRuntimeDiagnostics {
    private static final String[] AUDIO_FILE_HANDLE_CLASSES = {
            "rustedwarfare.audio.util.AudioFileHandle",
            "com.corrodinggames.rts.java.audio.a.a"
    };
    private static final String[] OPENAL_AUDIO_CLASSES = {
            "rustedwarfare.audio.openal.OpenALAudio",
            "com.corrodinggames.rts.java.audio.lwjgl.OpenALAudio"
    };
    private static final String[] OPENAL_SOUND_CLASSES = {
            "rustedwarfare.audio.openal.OpenALSound",
            "com.corrodinggames.rts.java.audio.lwjgl.OpenALSound"
    };
    private static final String[] OPENAL_MUSIC_CLASSES = {
            "rustedwarfare.audio.openal.OpenALMusic",
            "com.corrodinggames.rts.java.audio.lwjgl.OpenALMusic"
    };
    private static final String[] OPENAL_SOUND_FACTORY_CLASSES = {
            "rustedwarfare.audio.OpenALSoundFactory",
            "com.corrodinggames.rts.java.o"
    };
    private static final String[] OPENAL_GAME_SOUND_CLASSES = {
            "rustedwarfare.audio.OpenALGameSound",
            "com.corrodinggames.rts.java.q"
    };
    private static final String[] OPENAL_SOUND_PLAY_TASK_CLASSES = {
            "rustedwarfare.audio.OpenALSoundPlayTask",
            "com.corrodinggames.rts.java.p"
    };
    private static final String[] MUSIC_FACTORY_CLASSES = {
            "rustedwarfare.audio.MusicFactory",
            "com.corrodinggames.rts.gameFramework.aq"
    };
    private static final String[] OPENAL_MUSIC_FACTORY_CLASSES = {
            "rustedwarfare.audio.OpenALMusicFactory",
            "com.corrodinggames.rts.java.l"
    };
    private static final String[] MUSIC_TRACK_CLASSES = {
            "rustedwarfare.audio.MusicTrack",
            "com.corrodinggames.rts.gameFramework.ar"
    };
    private static final String[] OPENAL_MUSIC_TRACK_CLASSES = {
            "rustedwarfare.audio.OpenALMusicTrack",
            "com.corrodinggames.rts.java.m"
    };
    private static final String[] MUSIC_PLAYER_CLASSES = {
            "rustedwarfare.audio.MusicPlayer",
            "com.corrodinggames.rts.gameFramework.as"
    };
    private static final String[] OPENAL_MUSIC_PLAYER_CLASSES = {
            "rustedwarfare.audio.OpenALMusicPlayer",
            "com.corrodinggames.rts.java.n"
    };

    private AudioRuntimeDiagnostics() {
    }

    public static boolean isAudioFileHandle(Object value) {
        return isAny(value, AUDIO_FILE_HANDLE_CLASSES);
    }

    public static boolean isOpenALAudio(Object value) {
        return isAny(value, OPENAL_AUDIO_CLASSES);
    }

    public static boolean isOpenALSound(Object value) {
        return isAny(value, OPENAL_SOUND_CLASSES);
    }

    public static boolean isOpenALMusic(Object value) {
        return isAny(value, OPENAL_MUSIC_CLASSES);
    }

    public static boolean isOpenALSoundFactory(Object value) {
        return isAny(value, OPENAL_SOUND_FACTORY_CLASSES);
    }

    public static boolean isOpenALGameSound(Object value) {
        return isAny(value, OPENAL_GAME_SOUND_CLASSES);
    }

    public static boolean isOpenALSoundPlayTask(Object value) {
        return isAny(value, OPENAL_SOUND_PLAY_TASK_CLASSES);
    }

    public static boolean isMusicFactory(Object value) {
        return isAny(value, MUSIC_FACTORY_CLASSES);
    }

    public static boolean isOpenALMusicFactory(Object value) {
        return isAny(value, OPENAL_MUSIC_FACTORY_CLASSES);
    }

    public static boolean isMusicTrack(Object value) {
        return isAny(value, MUSIC_TRACK_CLASSES);
    }

    public static boolean isOpenALMusicTrack(Object value) {
        return isAny(value, OPENAL_MUSIC_TRACK_CLASSES);
    }

    public static boolean isMusicPlayer(Object value) {
        return isAny(value, MUSIC_PLAYER_CLASSES);
    }

    public static boolean isOpenALMusicPlayer(Object value) {
        return isAny(value, OPENAL_MUSIC_PLAYER_CLASSES);
    }

    public static Object newAudioFileHandle(String path) {
        return RustedReflection.newInstance(AUDIO_FILE_HANDLE_CLASSES, path);
    }

    public static Object newAudioFileHandle(InputStream inputStream, String path) {
        return RustedReflection.newInstance(AUDIO_FILE_HANDLE_CLASSES, inputStream, path);
    }

    public static Map<String, Object> describeAudioFileHandle(Object fileHandle) {
        requireAny(fileHandle, AUDIO_FILE_HANDLE_CLASSES, "AudioFileHandle");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", fileHandle.getClass().getName());
        putField(result, fileHandle, "inputStream", new String[]{"inputStream", "a"});
        putField(result, fileHandle, "file", new String[]{"file", "b"});
        putStringField(result, fileHandle, "path", new String[]{"path", "c"});
        result.put("extension", invokeStringOrEmpty(fileHandle, new String[]{"extension", "b"}));
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeOpenALAudio(Object audio) {
        requireAny(audio, OPENAL_AUDIO_CLASSES, "OpenALAudio");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", audio.getClass().getName());
        putIntField(result, audio, "deviceBufferSize", new String[]{"deviceBufferSize", "a"});
        putIntField(result, audio, "deviceBufferCount", new String[]{"deviceBufferCount", "b"});
        putField(result, audio, "idleSources", new String[]{"idleSources", "c"});
        putSizedField(result, audio, "idleSourcesSize", new String[]{"idleSources", "c"}, new String[]{"size", "b"});
        putField(result, audio, "allSources", new String[]{"allSources", "d"});
        putSizedField(result, audio, "allSourcesSize", new String[]{"allSources", "d"}, new String[]{"size", "b"});
        putField(result, audio, "soundIdToSource", new String[]{"soundIdToSource", "e"});
        putSizedField(result, audio, "soundIdToSourceSize", new String[]{"soundIdToSource", "e"}, new String[]{"size", "a"});
        putField(result, audio, "sourceToSoundId", new String[]{"sourceToSoundId", "f"});
        putSizedField(result, audio, "sourceToSoundIdSize", new String[]{"sourceToSoundId", "f"}, new String[]{"size", "a"});
        putLongField(result, audio, "nextSoundId", new String[]{"nextSoundId", "g"});
        putField(result, audio, "extensionToSoundClass", new String[]{"extensionToSoundClass", "h"});
        putSizedField(result, audio, "extensionToSoundClassSize", new String[]{"extensionToSoundClass", "h"}, new String[]{"size", "a"});
        putField(result, audio, "extensionToMusicClass", new String[]{"extensionToMusicClass", "i"});
        putSizedField(result, audio, "extensionToMusicClassSize", new String[]{"extensionToMusicClass", "i"}, new String[]{"size", "a"});
        putField(result, audio, "recentSounds", new String[]{"recentSounds", "j"});
        putArrayLengthField(result, audio, "recentSoundsLength", new String[]{"recentSounds", "j"});
        putIntField(result, audio, "mostRecentSound", new String[]{"mostRecentSound", "k"});
        putField(result, audio, "music", new String[]{"music", "l"});
        putCollectionSizeField(result, audio, "musicSize", new String[]{"music", "l"});
        putBooleanField(result, audio, "noDevice", new String[]{"noDevice", "m"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeOpenALSound(Object sound) {
        requireAny(sound, OPENAL_SOUND_CLASSES, "OpenALSound");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", sound.getClass().getName());
        putIntField(result, sound, "bufferID", new String[]{"bufferID", "a"});
        putField(result, sound, "audio", new String[]{"audio", "b"});
        putFloatField(result, sound, "duration", new String[]{"duration", "c"});
        putIntField(result, sound, "bytesUsed", new String[]{"bytesUsed", "d"});
        result.put("bytesUsedMethod", Integer.valueOf(invokeIntOrZero(sound, new String[]{"getBytesUsed", "a"})));
        result.put("durationMethod", Float.valueOf(invokeFloatOrZero(sound, new String[]{"duration"})));
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeOpenALMusic(Object music) {
        requireAny(music, OPENAL_MUSIC_CLASSES, "OpenALMusic");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", music.getClass().getName());
        putIntField(result, music, "bufferSize", new String[]{"bufferSize"});
        putIntField(result, music, "bufferCount", new String[]{"bufferCount"});
        putIntField(result, music, "bytesPerSample", new String[]{"bytesPerSample"});
        putField(result, music, "audio", new String[]{"audio"});
        putField(result, music, "buffers", new String[]{"buffers"});
        putIntField(result, music, "sourceID", new String[]{"sourceID"});
        putIntField(result, music, "format", new String[]{"format"});
        putIntField(result, music, "sampleRate", new String[]{"sampleRate"});
        putBooleanField(result, music, "isLooping", new String[]{"isLooping"});
        putBooleanField(result, music, "isPlaying", new String[]{"isPlaying"});
        putFloatField(result, music, "volume", new String[]{"volume"});
        putFloatField(result, music, "pan", new String[]{"pan"});
        putFloatField(result, music, "renderedSeconds", new String[]{"renderedSeconds"});
        putFloatField(result, music, "maxSecondsPerBuffer", new String[]{"maxSecondsPerBuffer"});
        putField(result, music, "file", new String[]{"file"});
        putIntField(result, music, "bufferOverhead", new String[]{"bufferOverhead"});
        putField(result, music, "onCompletionListener", new String[]{"onCompletionListener"});
        result.put("position", Float.valueOf(invokeFloatOrZero(music, new String[]{"getPosition"})));
        result.put("sourceIdMethod", Integer.valueOf(invokeIntOrZero(music, new String[]{"getSourceId"})));
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeOpenALSoundFactory(Object factory) {
        requireAny(factory, OPENAL_SOUND_FACTORY_CLASSES, "OpenALSoundFactory");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", factory.getClass().getName());
        putIntField(result, factory, "soundPoolSize", new String[]{"soundPoolSize", "a"});
        putField(result, factory, "playQueue", new String[]{"playQueue", "b"});
        putCollectionSizeField(result, factory, "playQueueSize", new String[]{"playQueue", "b"});
        putField(result, factory, "playTaskPool", new String[]{"playTaskPool", "c"});
        putField(result, factory, "soundThread", new String[]{"soundThread", "d"});
        putField(result, factory, "context", new String[]{"context", "e"});
        putField(result, factory, "openALAudio", new String[]{"openALAudio", "f"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeOpenALGameSound(Object gameSound) {
        requireAny(gameSound, OPENAL_GAME_SOUND_CLASSES, "OpenALGameSound");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", gameSound.getClass().getName());
        putField(result, gameSound, "sound", new String[]{"sound", "a"});
        putField(result, gameSound, "soundFactory", new String[]{"soundFactory", "b"});
        result.put("bytesUsed", Integer.valueOf(invokeIntOrZero(gameSound, new String[]{"getBytesUsed", "a"})));
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeOpenALSoundPlayTask(Object playTask) {
        requireAny(playTask, OPENAL_SOUND_PLAY_TASK_CLASSES, "OpenALSoundPlayTask");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", playTask.getClass().getName());
        putField(result, playTask, "gameSound", new String[]{"gameSound", "a"});
        putFloatField(result, playTask, "leftVolume", new String[]{"leftVolume", "b"});
        putFloatField(result, playTask, "rightVolume", new String[]{"rightVolume", "c"});
        putIntField(result, playTask, "priority", new String[]{"priority", "d"});
        putIntField(result, playTask, "loop", new String[]{"loop", "e"});
        putFloatField(result, playTask, "pitch", new String[]{"pitch", "f"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeMusicFactory(Object factory) {
        requireAny(factory, MUSIC_FACTORY_CLASSES, "MusicFactory");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", factory.getClass().getName());
        putField(result, factory, "musicController", new String[]{"musicController", "e"});
        result.put("available", Boolean.valueOf(invokeBooleanOrFalse(factory, new String[]{"isAvailable", "c"})));
        result.put("enabled", Boolean.valueOf(invokeBooleanOrFalse(factory, new String[]{"isEnabled", "d"})));
        result.put("maxMusicVolumePercent", Integer.valueOf(invokeIntOrZero(factory,
                new String[]{"getMaxMusicVolumePercent", "e"})));
        if (isOpenALMusicFactory(factory)) {
            result.putAll(describeOpenALMusicFactory(factory));
        }
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeOpenALMusicFactory(Object factory) {
        requireAny(factory, OPENAL_MUSIC_FACTORY_CLASSES, "OpenALMusicFactory");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", factory.getClass().getName());
        putBooleanField(result, factory, "shutdownRequested", new String[]{"shutdownRequested", "a"});
        putField(result, factory, "openALAudio", new String[]{"openALAudio", "b"});
        putBooleanField(result, factory, "musicThreadStarted", new String[]{"musicThreadStarted", "c"});
        result.put("available", Boolean.valueOf(invokeBooleanOrFalse(factory, new String[]{"isAvailable", "c"})));
        result.put("enabled", Boolean.valueOf(invokeBooleanOrFalse(factory, new String[]{"isEnabled", "d"})));
        result.put("maxMusicVolumePercent", Integer.valueOf(invokeIntOrZero(factory,
                new String[]{"getMaxMusicVolumePercent", "e"})));
        result.put("audioLock", invokeOrNull(factory, new String[]{"getAudioLock", "f"}));
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeMusicTrack(Object track) {
        requireAny(track, MUSIC_TRACK_CLASSES, "MusicTrack");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", track.getClass().getName());
        putStringField(result, track, "trackPath", new String[]{"trackPath", "b"});
        if (isOpenALMusicTrack(track)) {
            putField(result, track, "musicFactory", new String[]{"musicFactory", "a"});
            putField(result, track, "music", new String[]{"music", "c"});
        }
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeMusicPlayer(Object player) {
        requireAny(player, MUSIC_PLAYER_CLASSES, "MusicPlayer");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", player.getClass().getName());
        result.put("playing", Boolean.valueOf(invokeBooleanOrFalse(player, new String[]{"isPlaying", "c"})));
        if (isOpenALMusicPlayer(player)) {
            putField(result, player, "track", new String[]{"track", "a"});
            putField(result, player, "musicFactory", new String[]{"musicFactory", "b"});
            putField(result, player, "music", new String[]{"music", "c"});
            putBooleanField(result, player, "playQueued", new String[]{"playQueued", "d"});
            putBooleanField(result, player, "loopQueued", new String[]{"loopQueued", "e"});
            putBooleanField(result, player, "hasStartedPlayback", new String[]{"hasStartedPlayback", "f"});
        }
        return Collections.unmodifiableMap(result);
    }

    private static boolean isAny(Object value, String[] classNames) {
        return value != null && RustedReflection.isAnyClass(value.getClass(), classNames);
    }

    private static void requireAny(Object value, String[] classNames, String label) {
        if (value == null || !RustedReflection.isAnyClass(value.getClass(), classNames)) {
            throw new IllegalArgumentException("Expected " + label + ", got " + describe(value));
        }
    }

    private static String describe(Object value) {
        return value == null ? "null" : value.getClass().getName();
    }

    private static Object invokeOrNull(Object owner, String[] methodNames, Object... args) {
        try {
            return RustedReflection.invokeInstance(owner, methodNames, args);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String invokeStringOrEmpty(Object owner, String[] methodNames, Object... args) {
        Object value = invokeOrNull(owner, methodNames, args);
        return value != null ? value.toString() : "";
    }

    private static int invokeIntOrZero(Object owner, String[] methodNames, Object... args) {
        Object value = invokeOrNull(owner, methodNames, args);
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    private static float invokeFloatOrZero(Object owner, String[] methodNames, Object... args) {
        Object value = invokeOrNull(owner, methodNames, args);
        return value instanceof Number ? ((Number) value).floatValue() : 0.0F;
    }

    private static boolean invokeBooleanOrFalse(Object owner, String[] methodNames, Object... args) {
        return Boolean.TRUE.equals(invokeOrNull(owner, methodNames, args));
    }

    private static int arrayLength(Object array) {
        return array != null && array.getClass().isArray() ? java.lang.reflect.Array.getLength(array) : 0;
    }

    private static int collectionSize(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Collection<?>) {
            return ((Collection<?>) value).size();
        }
        if (value instanceof Map<?, ?>) {
            return ((Map<?, ?>) value).size();
        }
        if (value.getClass().isArray()) {
            return arrayLength(value);
        }
        return 1;
    }

    private static void putField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        try {
            result.put(key, RustedReflection.getFieldValue(owner, fieldNames));
        } catch (RuntimeException ignored) {
        }
    }

    private static void putStringField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        try {
            result.put(key, RustedReflection.getStringField(owner, fieldNames));
        } catch (RuntimeException ignored) {
        }
    }

    private static void putIntField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        try {
            result.put(key, Integer.valueOf(RustedReflection.getIntField(owner, fieldNames)));
        } catch (RuntimeException ignored) {
        }
    }

    private static void putLongField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        try {
            Object value = RustedReflection.getFieldValue(owner, fieldNames);
            result.put(key, value instanceof Number ? Long.valueOf(((Number) value).longValue()) : Long.valueOf(0L));
        } catch (RuntimeException ignored) {
        }
    }

    private static void putFloatField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        try {
            result.put(key, Float.valueOf(RustedReflection.getFloatField(owner, fieldNames)));
        } catch (RuntimeException ignored) {
        }
    }

    private static void putBooleanField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        try {
            result.put(key, Boolean.valueOf(RustedReflection.getBooleanField(owner, fieldNames)));
        } catch (RuntimeException ignored) {
        }
    }

    private static void putCollectionSizeField(Map<String, Object> result, Object owner, String key,
                                               String[] fieldNames) {
        try {
            result.put(key, Integer.valueOf(collectionSize(RustedReflection.getFieldValue(owner, fieldNames))));
        } catch (RuntimeException ignored) {
        }
    }

    private static void putArrayLengthField(Map<String, Object> result, Object owner, String key,
                                            String[] fieldNames) {
        try {
            result.put(key, Integer.valueOf(arrayLength(RustedReflection.getFieldValue(owner, fieldNames))));
        } catch (RuntimeException ignored) {
        }
    }

    private static void putSizedField(Map<String, Object> result, Object owner, String key,
                                      String[] fieldNames, String[] sizeFieldNames) {
        try {
            Object value = RustedReflection.getFieldValue(owner, fieldNames);
            int size = value != null ? RustedReflection.getIntField(value, sizeFieldNames) : 0;
            result.put(key, Integer.valueOf(size));
        } catch (RuntimeException ignored) {
        }
    }
}
