package io.github.endx.rustedfabricapi.api.replay;

/** Immutable snapshot of the native replay engine's public playback state. */
public final class ReplaySnapshot {
    private final boolean active;
    private final boolean recording;
    private final boolean replaying;
    private final boolean loadingInitialSave;
    private final int saveVersion;
    private final String gameVersion;
    private final int lastCommandFrame;
    private final int commandsRead;

    ReplaySnapshot(boolean active, boolean recording, boolean replaying,
            boolean loadingInitialSave, int saveVersion, String gameVersion,
            int lastCommandFrame, int commandsRead) {
        this.active = active;
        this.recording = recording;
        this.replaying = replaying;
        this.loadingInitialSave = loadingInitialSave;
        this.saveVersion = saveVersion;
        this.gameVersion = gameVersion;
        this.lastCommandFrame = lastCommandFrame;
        this.commandsRead = commandsRead;
    }

    public boolean isActive() { return active; }

    public boolean isRecording() { return recording; }

    public boolean isReplaying() { return replaying; }

    public boolean isLoadingInitialSave() { return loadingInitialSave; }

    /** Replay format/save version, or a negative value before a replay header is read. */
    public int getSaveVersion() { return saveVersion; }

    /** Game version stored in the replay header; may be {@code null}. */
    public String getGameVersion() { return gameVersion; }

    public int getLastCommandFrame() { return lastCommandFrame; }

    public int getCommandsRead() { return commandsRead; }

    @Override
    public String toString() {
        return "ReplaySnapshot{active=" + active + ", recording=" + recording
                + ", replaying=" + replaying + ", saveVersion=" + saveVersion
                + ", commandsRead=" + commandsRead + '}';
    }
}
