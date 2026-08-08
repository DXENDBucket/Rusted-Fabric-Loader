package io.github.endx.rustedfabricapi.api.audio;

import io.github.endx.rustedfabricapi.api.client.RustedWarfareClient;
import rustedwarfare.client.audio.GameSound;
import rustedwarfare.client.audio.SoundEngine;
import rustedwarfare.core.GameEngine;
import rustedwarfare.io.NamedInputStream;

import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;

/** Lookup, custom loading, and high-level playback through the game's sound mixer. */
public final class Sounds {
    private Sounds() {
    }

    public static SoundEngine manager() {
        GameEngine engine = RustedWarfareClient.requireEngine();
        if (engine.soundEngine == null) {
            throw new IllegalStateException("The game sound engine is not initialized");
        }
        return engine.soundEngine;
    }

    public static Optional<GameSound> findBuiltin(String name) {
        return Optional.ofNullable(manager().getBuiltinSound(requireText(name, "name")));
    }

    public static GameSound requireBuiltin(String name) {
        return findBuiltin(name).orElseThrow(() ->
                new IllegalArgumentException("Unknown built-in sound: " + name));
    }

    /**
     * Loads a mod sound from a stream using the active desktop backend. Loading is synchronous;
     * the caller retains responsibility for closing the supplied stream. When
     * {@code registerForLookup} is true, the backend also indexes it by its normalized name.
     */
    public static GameSound load(String name, InputStream stream, boolean registerForLookup) {
        String checkedName = requireText(name, "name");
        Objects.requireNonNull(stream, "stream");
        GameSound sound = manager().loadSoundFromStream(checkedName,
                new NamedInputStream(stream, checkedName), registerForLookup);
        if (sound == null) throw new IllegalStateException("Could not load sound: " + checkedName);
        return sound;
    }

    public static void playInterface(GameSound sound, float volume) {
        requireVolume(volume);
        manager().playInterfaceSound(Objects.requireNonNull(sound, "sound"), volume);
    }

    public static void playGlobal(GameSound sound, float volume) {
        requireVolume(volume);
        manager().playGameSoundGlobal(Objects.requireNonNull(sound, "sound"), volume);
    }

    public static void playAt(GameSound sound, float volume, float worldX, float worldY) {
        requireVolume(volume);
        requireFinite(worldX, "worldX");
        requireFinite(worldY, "worldY");
        manager().playGameSoundAt(Objects.requireNonNull(sound, "sound"), volume, worldX, worldY);
    }

    public static void playAt(GameSound sound, float volume, float worldX, float worldY,
            float pitch) {
        requireVolume(volume);
        requireFinite(worldX, "worldX");
        requireFinite(worldY, "worldY");
        if (!Float.isFinite(pitch) || !(pitch > 0.0F)) {
            throw new IllegalArgumentException("pitch must be positive and finite");
        }
        manager().playGameSoundAtWithPitch(Objects.requireNonNull(sound, "sound"),
                volume, worldX, worldY, pitch);
    }

    public static boolean isMuted() {
        return manager().soundsMuted;
    }

    public static void setMuted(boolean muted) {
        manager().soundsMuted = muted;
    }

    public static boolean canPlay() {
        return manager().canPlayGameSounds();
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        String checked = value.trim();
        if (checked.isEmpty()) throw new IllegalArgumentException(name + " must not be blank");
        return checked;
    }

    private static void requireVolume(float volume) {
        if (!Float.isFinite(volume) || volume < 0.0F) {
            throw new IllegalArgumentException("volume must be non-negative and finite");
        }
    }

    private static void requireFinite(float value, String name) {
        if (!Float.isFinite(value)) throw new IllegalArgumentException(name + " must be finite");
    }
}
