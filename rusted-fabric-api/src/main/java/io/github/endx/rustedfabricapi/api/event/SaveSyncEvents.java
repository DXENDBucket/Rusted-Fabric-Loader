package io.github.endx.rustedfabricapi.api.event;

public final class SaveSyncEvents {
    public static final RustedFabricEvent<BeforeSaveGameToFile> BEFORE_SAVE_GAME_TO_FILE =
            RustedFabricEvent.create(listeners -> (gameSaver, saveName, autoSave) -> {
                boolean cancelled = false;
                for (BeforeSaveGameToFile listener : listeners) {
                    cancelled |= listener.beforeSaveGameToFile(gameSaver, saveName, autoSave);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<BeforeWriteSaveStream> BEFORE_WRITE_SAVE_STREAM =
            RustedFabricEvent.create(listeners -> (gameSaver, outputStream) -> {
                boolean cancelled = false;
                for (BeforeWriteSaveStream listener : listeners) {
                    cancelled |= listener.beforeWriteSaveStream(gameSaver, outputStream);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterWriteSaveStream> AFTER_WRITE_SAVE_STREAM =
            RustedFabricEvent.create(listeners -> (gameSaver, outputStream) -> {
                for (AfterWriteSaveStream listener : listeners) {
                    listener.afterWriteSaveStream(gameSaver, outputStream);
                }
            });

    public static final RustedFabricEvent<BeforeReadSaveStream> BEFORE_READ_SAVE_STREAM =
            RustedFabricEvent.create(listeners -> (gameSaver, inputStream, optionA, optionB, optionC) -> {
                boolean cancelled = false;
                for (BeforeReadSaveStream listener : listeners) {
                    cancelled |= listener.beforeReadSaveStream(gameSaver, inputStream, optionA, optionB, optionC);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterReadSaveStream> AFTER_READ_SAVE_STREAM =
            RustedFabricEvent.create(listeners -> (gameSaver, inputStream, optionA, optionB, optionC, result) -> {
                for (AfterReadSaveStream listener : listeners) {
                    listener.afterReadSaveStream(gameSaver, inputStream, optionA, optionB, optionC, result);
                }
            });

    public static final RustedFabricEvent<BeforeNetworkResyncSave> BEFORE_NETWORK_RESYNC_SAVE =
            RustedFabricEvent.create(listeners -> (networkEngine, connection, saveBytes, optionA, optionB, reloadCreatedSave, operation) -> {
                boolean cancelled = false;
                for (BeforeNetworkResyncSave listener : listeners) {
                    cancelled |= listener.beforeNetworkResyncSave(networkEngine, connection, saveBytes, optionA, optionB, reloadCreatedSave, operation);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterNetworkResyncPacketCreated> AFTER_NETWORK_RESYNC_PACKET_CREATED =
            RustedFabricEvent.create(listeners -> (networkEngine, connection, packet, saveBytes, optionA, optionB, reloadCreatedSave, operation) -> {
                for (AfterNetworkResyncPacketCreated listener : listeners) {
                    listener.afterNetworkResyncPacketCreated(networkEngine, connection, packet, saveBytes, optionA, optionB, reloadCreatedSave, operation);
                }
            });

    public static final RustedFabricEvent<BeforeReplayRecordCommand> BEFORE_REPLAY_RECORD_COMMAND =
            RustedFabricEvent.create(listeners -> (replayEngine, command, frame) -> {
                boolean cancelled = false;
                for (BeforeReplayRecordCommand listener : listeners) {
                    cancelled |= listener.beforeReplayRecordCommand(replayEngine, command, frame);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<BeforeReplayPlaybackBlock> BEFORE_REPLAY_PLAYBACK_BLOCK =
            RustedFabricEvent.create(listeners -> replayEngine -> {
                boolean cancelled = false;
                for (BeforeReplayPlaybackBlock listener : listeners) {
                    cancelled |= listener.beforeReplayPlaybackBlock(replayEngine);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<BeforeChecksumSend> BEFORE_CHECKSUM_SEND =
            RustedFabricEvent.create(listeners -> (networkEngine, packet, checksum, delta) -> {
                boolean cancelled = false;
                for (BeforeChecksumSend listener : listeners) {
                    cancelled |= listener.beforeChecksumSend(networkEngine, packet, checksum, delta);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<BeforeGameObjectSerialize> BEFORE_GAME_OBJECT_SERIALIZE =
            RustedFabricEvent.create(listeners -> (gameObject, outputStream) -> {
                boolean cancelled = false;
                for (BeforeGameObjectSerialize listener : listeners) {
                    cancelled |= listener.beforeGameObjectSerialize(gameObject, outputStream);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterGameObjectDeserialize> AFTER_GAME_OBJECT_DESERIALIZE =
            RustedFabricEvent.create(listeners -> (gameObject, inputStream) -> {
                for (AfterGameObjectDeserialize listener : listeners) {
                    listener.afterGameObjectDeserialize(gameObject, inputStream);
                }
            });

    private SaveSyncEvents() {
    }

    @FunctionalInterface
    public interface BeforeSaveGameToFile {
        boolean beforeSaveGameToFile(Object gameSaver, String saveName, boolean autoSave);
    }

    @FunctionalInterface
    public interface BeforeWriteSaveStream {
        boolean beforeWriteSaveStream(Object gameSaver, Object outputStream);
    }

    @FunctionalInterface
    public interface AfterWriteSaveStream {
        void afterWriteSaveStream(Object gameSaver, Object outputStream);
    }

    @FunctionalInterface
    public interface BeforeReadSaveStream {
        boolean beforeReadSaveStream(Object gameSaver, Object inputStream, boolean optionA, boolean optionB, boolean optionC);
    }

    @FunctionalInterface
    public interface AfterReadSaveStream {
        void afterReadSaveStream(Object gameSaver, Object inputStream, boolean optionA, boolean optionB, boolean optionC, boolean result);
    }

    @FunctionalInterface
    public interface BeforeNetworkResyncSave {
        boolean beforeNetworkResyncSave(Object networkEngine, Object connection, byte[] saveBytes, boolean optionA, boolean optionB, boolean reloadCreatedSave, String operation);
    }

    @FunctionalInterface
    public interface AfterNetworkResyncPacketCreated {
        void afterNetworkResyncPacketCreated(Object networkEngine, Object connection, Object packet, byte[] saveBytes, boolean optionA, boolean optionB, boolean reloadCreatedSave, String operation);
    }

    @FunctionalInterface
    public interface BeforeReplayRecordCommand {
        boolean beforeReplayRecordCommand(Object replayEngine, Object command, int frame);
    }

    @FunctionalInterface
    public interface BeforeReplayPlaybackBlock {
        boolean beforeReplayPlaybackBlock(Object replayEngine);
    }

    @FunctionalInterface
    public interface BeforeChecksumSend {
        boolean beforeChecksumSend(Object networkEngine, Object packet, Object checksum, float delta);
    }

    @FunctionalInterface
    public interface BeforeGameObjectSerialize {
        boolean beforeGameObjectSerialize(Object gameObject, Object outputStream);
    }

    @FunctionalInterface
    public interface AfterGameObjectDeserialize {
        void afterGameObjectDeserialize(Object gameObject, Object inputStream);
    }
}
