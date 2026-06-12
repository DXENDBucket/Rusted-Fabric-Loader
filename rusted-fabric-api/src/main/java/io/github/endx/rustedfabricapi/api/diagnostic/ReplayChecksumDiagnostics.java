package io.github.endx.rustedfabricapi.api.diagnostic;

import io.github.endx.rustedfabricapi.api.util.RustedReflection;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class ReplayChecksumDiagnostics {
    private static final String[] REPLAY_ENGINE_CLASSES = {
            "rustedwarfare.replay.ReplayEngine",
            "com.corrodinggames.rts.gameFramework.ba"
    };
    private static final String[] REPLAY_WRITER_THREAD_CLASSES = {
            "rustedwarfare.replay.ReplayWriterThread",
            "com.corrodinggames.rts.gameFramework.bb"
    };
    private static final String[] REPLAY_RECORD_ENTRY_CLASSES = {
            "rustedwarfare.replay.ReplayRecordEntry",
            "com.corrodinggames.rts.gameFramework.bd"
    };
    private static final String[] REPLAY_CHAT_MESSAGE_CLASSES = {
            "rustedwarfare.replay.ReplayChatMessage",
            "com.corrodinggames.rts.gameFramework.bc"
    };
    private static final String[] NETWORK_CHECKSUM_CLASSES = {
            "rustedwarfare.network.NetworkChecksum",
            "com.corrodinggames.rts.gameFramework.j.ak"
    };
    private static final String[] NETWORK_CHECKSUM_ENTRY_CLASSES = {
            "rustedwarfare.network.NetworkChecksumEntry",
            "com.corrodinggames.rts.gameFramework.j.al"
    };

    private ReplayChecksumDiagnostics() {
    }

    public static Map<String, Object> describeReplayEngine(Object replayEngine) {
        requireReplayEngine(replayEngine);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putField(result, replayEngine, "inputStream", new String[]{"inputStream", "F"});
        putField(result, replayEngine, "outputStream", new String[]{"outputStream", "J"});
        putField(result, replayEngine, "writerThread", new String[]{"writerThread", "K"});
        putField(result, replayEngine, "writerLock", new String[]{"writerLock", "M"});
        putBooleanField(result, replayEngine, "allowRecording", new String[]{"allowRecording", "N"});
        putBooleanField(result, replayEngine, "allowPlayback", new String[]{"allowPlayback", "O"});
        putField(result, replayEngine, "currentReplayName", new String[]{"currentReplayName", "a"});
        putField(result, replayEngine, "networkChecksum", new String[]{"networkChecksum", "g"});
        putBooleanField(result, replayEngine, "legacyRecordingFlag", new String[]{"recording", "h"});
        putBooleanField(result, replayEngine, "legacyReplayingFlag", new String[]{"replaying", "n"});
        putField(result, replayEngine, "currentEntry", new String[]{"currentEntry", "w"});
        putField(result, replayEngine, "nextEntry", new String[]{"nextEntry", "x"});
        putBooleanField(result, replayEngine, "active", new String[]{"active", "P"});
        putBooleanField(result, replayEngine, "playbackMode", new String[]{"playbackMode", "u"});
        putIntField(result, replayEngine, "recordedCommandCount", new String[]{"recordedCommandCount", "A"});
        putIntField(result, replayEngine, "recordedResyncCount", new String[]{"recordedResyncCount", "B"});
        putIntField(result, replayEngine, "issuedReplayCommandCount", new String[]{"issuedReplayCommandCount", "z"});
        putIntField(result, replayEngine, "replaySaveVersion", new String[]{"replaySaveVersion", "q"});
        putField(result, replayEngine, "replayVersionString", new String[]{"replayVersionString", "r"});
        putOptional(result, "isActive", new Supplier<Object>() {
            @Override
            public Object get() {
                return Boolean.valueOf(isActive(replayEngine));
            }
        });
        putOptional(result, "isReplaying", new Supplier<Object>() {
            @Override
            public Object get() {
                return Boolean.valueOf(isReplaying(replayEngine));
            }
        });
        putOptional(result, "isRecording", new Supplier<Object>() {
            @Override
            public Object get() {
                return Boolean.valueOf(isRecording(replayEngine));
            }
        });
        return Collections.unmodifiableMap(result);
    }

    public static Object getNetworkChecksum(Object replayEngine) {
        requireReplayEngine(replayEngine);
        return RustedReflection.getFieldValue(replayEngine, new String[]{"networkChecksum", "g"});
    }

    public static Object getReplayWriterThread(Object replayEngine) {
        requireReplayEngine(replayEngine);
        return RustedReflection.getFieldValue(replayEngine, new String[]{"writerThread", "K"});
    }

    public static Object getCurrentEntry(Object replayEngine) {
        requireReplayEngine(replayEngine);
        return RustedReflection.getFieldValue(replayEngine, new String[]{"currentEntry", "w"});
    }

    public static Object getNextEntry(Object replayEngine) {
        requireReplayEngine(replayEngine);
        return RustedReflection.getFieldValue(replayEngine, new String[]{"nextEntry", "x"});
    }

    public static boolean isActive(Object replayEngine) {
        requireReplayEngine(replayEngine);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(replayEngine,
                new String[]{"isActive", "isRecording", "i"}));
    }

    public static boolean isReplaying(Object replayEngine) {
        requireReplayEngine(replayEngine);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(replayEngine,
                new String[]{"isReplaying", "j"}));
    }

    public static boolean isRecording(Object replayEngine) {
        requireReplayEngine(replayEngine);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(replayEngine,
                new String[]{"isRecording", "isActive", "k"}));
    }

    public static long calculateQuickUnitChecksum(Object replayEngine) {
        requireReplayEngine(replayEngine);
        Object value = RustedReflection.invokeInstance(replayEngine,
                new String[]{"calculateQuickUnitChecksum", "getReplayLength", "f"});
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    public static Map<String, Object> describeReplayWriterThread(Object writerThread) {
        requireReplayWriterThread(writerThread);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putBooleanField(result, writerThread, "running", new String[]{"running", "a"});
        putCollectionField(result, writerThread, "queue", new String[]{"queue", "i"});
        putLongField(result, writerThread, "lastFlushTime", new String[]{"lastFlushTime", "j"});
        putField(result, writerThread, "replayEngine", new String[]{"replayEngine", "k"});
        putIntField(result, writerThread, "stoppedFrame", new String[]{"stoppedFrame", "b"});
        putIntField(result, writerThread, "stoppedGameTime", new String[]{"stoppedGameTime", "c"});
        putIntField(result, writerThread, "commandCountAtStop", new String[]{"commandCountAtStop", "d"});
        putIntField(result, writerThread, "resyncCountAtStop", new String[]{"resyncCountAtStop", "e"});
        putIntField(result, writerThread, "lastQueuedFrame", new String[]{"lastQueuedFrame", "f"});
        putIntField(result, writerThread, "lastWrittenCommandFrame",
                new String[]{"lastWrittenCommandFrame", "g"});
        putBooleanField(result, writerThread, "finishedWriting", new String[]{"finishedWriting", "h"});
        return Collections.unmodifiableMap(result);
    }

    public static List<Object> replayWriterQueueSnapshot(Object writerThread) {
        requireReplayWriterThread(writerThread);
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(
                RustedReflection.getFieldValue(writerThread, new String[]{"queue", "i"})));
    }

    public static Map<String, Object> describeReplayRecordEntry(Object entry) {
        requireReplayRecordEntry(entry);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putIntField(result, entry, "frame", new String[]{"frame", "a"});
        putBooleanField(result, entry, "waitMarker", new String[]{"waitMarker", "isChecksum", "b"});
        putField(result, entry, "checksum", new String[]{"checksum", "c"});
        putField(result, entry, "extendedChecksumBytes", new String[]{"extendedChecksumBytes", "checksumDetails", "d"});
        result.put("extendedChecksumBytesLength", Integer.valueOf(arrayLength(
                RustedReflection.getFieldValue(entry, new String[]{"extendedChecksumBytes", "checksumDetails", "d"}))));
        putField(result, entry, "command", new String[]{"command", "e"});
        putField(result, entry, "resyncSaveBytes", new String[]{"resyncSaveBytes", "f"});
        result.put("resyncSaveBytesLength", Integer.valueOf(arrayLength(
                RustedReflection.getFieldValue(entry, new String[]{"resyncSaveBytes", "f"}))));
        putField(result, entry, "chatMessage", new String[]{"chatMessage", "g"});
        putIntField(result, entry, "resyncFrame", new String[]{"resyncFrame", "h"});
        putIntField(result, entry, "resyncTick", new String[]{"resyncTick", "i"});
        putFloatField(result, entry, "resyncX", new String[]{"resyncX", "j"});
        putFloatField(result, entry, "resyncY", new String[]{"resyncY", "k"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeReplayChatMessage(Object chatMessage) {
        requireReplayChatMessage(chatMessage);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putIntField(result, chatMessage, "frame", new String[]{"frame", "a"});
        putField(result, chatMessage, "sender", new String[]{"sender", "b"});
        putField(result, chatMessage, "message", new String[]{"message", "c"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeNetworkChecksum(Object checksum) {
        requireNetworkChecksum(checksum);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putLongField(result, checksum, "totalChecksum", new String[]{"totalChecksum", "a"});
        putCollectionField(result, checksum, "entries", new String[]{"entries", "b"});
        result.put("buckets", checksumBuckets(checksum));
        return Collections.unmodifiableMap(result);
    }

    public static List<Object> checksumEntriesSnapshot(Object checksum) {
        requireNetworkChecksum(checksum);
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(
                RustedReflection.getFieldValue(checksum, new String[]{"entries", "b"})));
    }

    public static Map<String, Object> checksumBuckets(Object checksum) {
        requireNetworkChecksum(checksum);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putBucket(result, checksum, "unitPositionChecksum", new String[]{"unitPositionChecksum", "unitChecksum", "c"});
        putBucket(result, checksum, "unitDirectionChecksum",
                new String[]{"unitDirectionChecksum", "unitPosChecksum", "d"});
        putBucket(result, checksum, "unitHpChecksum", new String[]{"unitHpChecksum", "unitDirChecksum", "e"});
        putBucket(result, checksum, "unitIdChecksum", new String[]{"unitIdChecksum", "unitHpChecksum", "f"});
        putBucket(result, checksum, "waypointsChecksum", new String[]{"waypointsChecksum", "unitIdChecksum", "g"});
        putBucket(result, checksum, "waypointPositionChecksum",
                new String[]{"waypointPositionChecksum", "waypointChecksum", "h"});
        putBucket(result, checksum, "teamCreditsChecksum",
                new String[]{"teamCreditsChecksum", "waypointPosChecksum", "i"});
        putBucket(result, checksum, "unitPathsChecksum", new String[]{"unitPathsChecksum", "teamCreditsChecksum", "j"});
        putBucket(result, checksum, "unitCountChecksum", new String[]{"unitCountChecksum", "k"});
        putBucket(result, checksum, "teamInfoChecksum", new String[]{"teamInfoChecksum", "customUnitChecksum", "l"});
        putBucket(result, checksum, "team1CreditsChecksum", new String[]{"team1CreditsChecksum", "mapChecksum", "m"});
        putBucket(result, checksum, "team2CreditsChecksum", new String[]{"team2CreditsChecksum", "commandChecksum", "n"});
        putBucket(result, checksum, "team3CreditsChecksum", new String[]{"team3CreditsChecksum", "randomChecksum", "o"});
        putBucket(result, checksum, "commandCenter2Checksum", new String[]{"commandCenter2Checksum", "pathChecksum", "p"});
        putBucket(result, checksum, "commandCenter3Checksum", new String[]{"commandCenter3Checksum", "miscChecksum", "q"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeNetworkChecksumEntry(Object entry) {
        requireNetworkChecksumEntry(entry);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putField(result, entry, "name", new String[]{"name", "a"});
        putLongField(result, entry, "value", new String[]{"value", "b"});
        putBooleanField(result, entry, "includedInTotal", new String[]{"includedInTotal", "enabled", "c"});
        putField(result, entry, "owner", new String[]{"owner", "d"});
        return Collections.unmodifiableMap(result);
    }

    private static void putBucket(Map<String, Object> result, Object checksum, String key, String[] fieldNames) {
        Object entry = RustedReflection.getFieldValue(checksum, fieldNames);
        result.put(key, entry != null ? describeNetworkChecksumEntry(entry) : null);
    }

    private static void requireReplayEngine(Object replayEngine) {
        requireAny(replayEngine, REPLAY_ENGINE_CLASSES, "ReplayEngine");
    }

    private static void requireReplayWriterThread(Object writerThread) {
        requireAny(writerThread, REPLAY_WRITER_THREAD_CLASSES, "ReplayWriterThread");
    }

    private static void requireReplayRecordEntry(Object entry) {
        requireAny(entry, REPLAY_RECORD_ENTRY_CLASSES, "ReplayRecordEntry");
    }

    private static void requireReplayChatMessage(Object chatMessage) {
        requireAny(chatMessage, REPLAY_CHAT_MESSAGE_CLASSES, "ReplayChatMessage");
    }

    private static void requireNetworkChecksum(Object checksum) {
        requireAny(checksum, NETWORK_CHECKSUM_CLASSES, "NetworkChecksum");
    }

    private static void requireNetworkChecksumEntry(Object entry) {
        requireAny(entry, NETWORK_CHECKSUM_ENTRY_CLASSES, "NetworkChecksumEntry");
    }

    private static void requireAny(Object value, String[] classNames, String label) {
        if (value == null) {
            throw new IllegalArgumentException(label + " must not be null");
        }
        if (!RustedReflection.isAnyClass(value.getClass(), classNames)) {
            throw new IllegalArgumentException("Expected " + label + ", got " + value.getClass().getName());
        }
    }

    private static void putField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        result.put(key, RustedReflection.getFieldValue(owner, fieldNames));
    }

    private static void putCollectionField(Map<String, Object> result, Object owner, String key,
                                           String[] fieldNames) {
        Object value = RustedReflection.getFieldValue(owner, fieldNames);
        result.put(key, value);
        result.put(key + "Size", Integer.valueOf(RustedReflection.snapshotIterable(value).size()));
    }

    private static void putIntField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        result.put(key, Integer.valueOf(RustedReflection.getIntField(owner, fieldNames)));
    }

    private static void putLongField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        Object value = RustedReflection.getFieldValue(owner, fieldNames);
        result.put(key, Long.valueOf(value instanceof Number ? ((Number) value).longValue() : 0L));
    }

    private static void putFloatField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        result.put(key, Float.valueOf(RustedReflection.getFloatField(owner, fieldNames)));
    }

    private static void putBooleanField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        result.put(key, Boolean.valueOf(RustedReflection.getBooleanField(owner, fieldNames)));
    }

    private static void putOptional(Map<String, Object> result, String key, Supplier<Object> valueSupplier) {
        try {
            result.put(key, valueSupplier.get());
        } catch (RuntimeException e) {
            result.put(key + "Error", e.getClass().getName() + ": " + e.getMessage());
        }
    }

    private static int arrayLength(Object array) {
        return array != null && array.getClass().isArray() ? Array.getLength(array) : 0;
    }
}
