package io.github.endx.rustedfabricapi.api.audio;

import rustedwarfare.client.audio.GameSound;

import java.util.Objects;

/** Immutable high-level sound request observed before or after the game sound engine. */
public final class SoundPlayback {
    public enum Scope {
        INTERFACE,
        GLOBAL,
        POSITIONAL
    }

    private final GameSound sound;
    private final Scope scope;
    private final float volume;
    private final float worldX;
    private final float worldY;
    private final float pitch;

    private SoundPlayback(GameSound sound, Scope scope, float volume,
            float worldX, float worldY, float pitch) {
        this.sound = Objects.requireNonNull(sound, "sound");
        this.scope = Objects.requireNonNull(scope, "scope");
        this.volume = volume;
        this.worldX = worldX;
        this.worldY = worldY;
        this.pitch = pitch;
    }

    public static SoundPlayback nonPositional(GameSound sound, Scope scope, float volume) {
        if (scope == Scope.POSITIONAL) {
            throw new IllegalArgumentException("positional scope requires coordinates");
        }
        return new SoundPlayback(sound, scope, volume, Float.NaN, Float.NaN, 1.0F);
    }

    public static SoundPlayback positional(GameSound sound, float volume,
            float worldX, float worldY, float pitch) {
        return new SoundPlayback(sound, Scope.POSITIONAL, volume, worldX, worldY, pitch);
    }

    public GameSound sound() {
        return sound;
    }

    public Scope scope() {
        return scope;
    }

    public float volume() {
        return volume;
    }

    public boolean isPositional() {
        return scope == Scope.POSITIONAL;
    }

    public float worldX() {
        if (!isPositional()) throw new IllegalStateException("Sound is not positional");
        return worldX;
    }

    public float worldY() {
        if (!isPositional()) throw new IllegalStateException("Sound is not positional");
        return worldY;
    }

    public float pitch() {
        return pitch;
    }

}
