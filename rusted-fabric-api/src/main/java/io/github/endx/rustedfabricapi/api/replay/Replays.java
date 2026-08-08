package io.github.endx.rustedfabricapi.api.replay;

import io.github.endx.rustedfabricapi.api.client.RustedWarfareClient;
import rustedwarfare.replay.ReplayEngine;

import java.io.File;
import java.util.Locale;

/** High-level access to Rusted Warfare replay recording and playback. */
public final class Replays {
    public static final String EXTENSION = ".replay";

    private Replays() {
    }

    public static ReplayEngine manager() {
        ReplayEngine manager = RustedWarfareClient.requireEngine().replayEngine;
        if (manager == null) throw new IllegalStateException("Replay engine is not initialized");
        return manager;
    }

    public static ReplaySnapshot snapshot() {
        ReplayEngine manager = manager();
        return new ReplaySnapshot(manager.isActive(), manager.isRecording(),
                manager.isReplaying(), manager.loadingReplayInitialSave,
                manager.replaySaveVersion, manager.replayVersionString,
                manager.lastReadReplayCommandFrame, manager.readReplayCommandCount);
    }

    public static boolean isActive() { return manager().isActive(); }

    public static boolean isRecording() { return manager().isRecording(); }

    public static boolean isReplaying() { return manager().isReplaying(); }

    public static String normalizeName(String name) {
        String value = validateLeafName(name);
        if (!value.toLowerCase(Locale.ROOT).endsWith(EXTENSION)) value += EXTENSION;
        return value;
    }

    public static File file(String name) {
        return manager().getReplayFile(normalizeName(name), false);
    }

    public static boolean exists(String name) {
        File file = file(name);
        return file != null && file.isFile();
    }

    /** Starts recording the current match under a path-safe name. */
    public static boolean startRecording(String name) {
        ReplayEngine manager = manager();
        manager.startSavingReplay(normalizeName(name));
        return manager.isRecording();
    }

    /** Loads and begins playback. This resets the current game, matching the native replay menu. */
    public static boolean play(String name) {
        return manager().loadReplayByName(normalizeName(name));
    }

    /** Stops either recording or playback and closes all replay streams. */
    public static void stop() {
        manager().closeReplayStreams();
    }

    public static boolean delete(String name) {
        ReplayEngine manager = manager();
        String normalized = normalizeName(name);
        File file = manager.getReplayFile(normalized, true);
        boolean existed = file != null && file.isFile();
        manager.deleteReplay(normalized);
        return existed && (file == null || !file.exists());
    }

    private static String validateLeafName(String name) {
        if (name == null) throw new NullPointerException("name");
        String value = name.trim();
        if (value.isEmpty()) throw new IllegalArgumentException("Replay name must not be blank");
        if (value.length() > 180) throw new IllegalArgumentException("Replay name is too long");
        if (value.contains("..") || value.indexOf('/') >= 0 || value.indexOf('\\') >= 0
                || value.indexOf(':') >= 0 || value.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("Replay name must be a single safe file name");
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < 32 || "<>\"|?*".indexOf(c) >= 0) {
                throw new IllegalArgumentException("Replay name contains an unsupported character");
            }
        }
        return value;
    }
}
