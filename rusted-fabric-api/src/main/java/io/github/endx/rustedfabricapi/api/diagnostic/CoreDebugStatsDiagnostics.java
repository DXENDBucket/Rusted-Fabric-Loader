package io.github.endx.rustedfabricapi.api.diagnostic;

import io.github.endx.rustedfabricapi.api.util.RustedReflection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CoreDebugStatsDiagnostics {
    private static final String[] TEAM_CLASSES = {
            "rustedwarfare.game.Team",
            "com.corrodinggames.rts.game.n"
    };
    private static final String[] UNIT_CLASSES = {
            "rustedwarfare.unit.Unit",
            "com.corrodinggames.rts.game.units.am"
    };
    private static final String[] STATS_ENGINE_CLASSES = {
            "rustedwarfare.stats.StatsEngine",
            "com.corrodinggames.rts.gameFramework.bg"
    };
    private static final String[] STATS_TEAM_STATS_CLASSES = {
            "rustedwarfare.stats.TeamStats",
            "com.corrodinggames.rts.gameFramework.bo"
    };
    private static final String[] STATS_HISTORY_CLASSES = {
            "rustedwarfare.stats.StatsHistory",
            "com.corrodinggames.rts.gameFramework.bn"
    };
    private static final String[] STATS_HISTORY_SERIES_CLASSES = {
            "rustedwarfare.stats.StatsHistorySeries",
            "com.corrodinggames.rts.gameFramework.bi"
    };
    private static final String[] STATS_HISTORY_POINT_CLASSES = {
            "rustedwarfare.stats.StatsHistoryPoint",
            "com.corrodinggames.rts.gameFramework.bh"
    };
    private static final String[] STATS_HISTORY_METRIC_CLASSES = {
            "rustedwarfare.stats.StatsHistoryMetric",
            "com.corrodinggames.rts.gameFramework.bj"
    };
    private static final String[] TEAM_STAT_VALUE_SOURCE_CLASSES = {
            "rustedwarfare.stats.TeamStatValueSource",
            "com.corrodinggames.rts.gameFramework.g.f"
    };
    private static final String[] STATS_EVENT_DISPATCHER_CLASSES = {
            "rustedwarfare.stats.StatsEventDispatcher",
            "com.corrodinggames.rts.gameFramework.bl"
    };
    private static final String[] PERFORMANCE_TIMER_CLASSES = {
            "rustedwarfare.debug.PerformanceTimer",
            "com.corrodinggames.rts.gameFramework.bt"
    };
    private static final String[] GAME_PROFILER_CLASSES = {
            "rustedwarfare.debug.GameProfiler",
            "com.corrodinggames.rts.gameFramework.br"
    };
    private static final String[] PROFILER_SECTION_CLASSES = {
            "rustedwarfare.debug.ProfilerSection",
            "com.corrodinggames.rts.gameFramework.bs"
    };
    private static final String[] PROFILER_SECTION_DATA_CLASSES = {
            "rustedwarfare.debug.ProfilerSectionData",
            "com.corrodinggames.rts.gameFramework.bu"
    };
    private static final String[] ANR_WATCHDOG_CLASSES = {
            "rustedwarfare.debug.anr.AnrWatchDog",
            "com.corrodinggames.rts.gameFramework.utility.d"
    };
    private static final String[] ANR_ERROR_CLASSES = {
            "rustedwarfare.debug.anr.AnrError",
            "com.corrodinggames.rts.gameFramework.utility.a"
    };

    private CoreDebugStatsDiagnostics() {
    }

    public static Object currentStatsEngine() {
        return GameEngineDiagnostics.currentStatsEngine();
    }

    public static boolean isStatsEngine(Object value) {
        return isAny(value, STATS_ENGINE_CLASSES);
    }

    public static boolean isStatsTeamStats(Object value) {
        return isAny(value, STATS_TEAM_STATS_CLASSES);
    }

    public static boolean isStatsHistory(Object value) {
        return isAny(value, STATS_HISTORY_CLASSES);
    }

    public static boolean isStatsHistorySeries(Object value) {
        return isAny(value, STATS_HISTORY_SERIES_CLASSES);
    }

    public static boolean isStatsHistoryPoint(Object value) {
        return isAny(value, STATS_HISTORY_POINT_CLASSES);
    }

    public static boolean isStatsHistoryMetric(Object value) {
        return isAny(value, STATS_HISTORY_METRIC_CLASSES);
    }

    public static boolean isTeamStatValueSource(Object value) {
        return isAny(value, TEAM_STAT_VALUE_SOURCE_CLASSES);
    }

    public static boolean isStatsEventDispatcher(Object value) {
        return isAny(value, STATS_EVENT_DISPATCHER_CLASSES);
    }

    public static boolean isPerformanceTimer(Object value) {
        return isAny(value, PERFORMANCE_TIMER_CLASSES);
    }

    public static boolean isGameProfiler(Object value) {
        return isAny(value, GAME_PROFILER_CLASSES);
    }

    public static boolean isProfilerSection(Object value) {
        return isAny(value, PROFILER_SECTION_CLASSES);
    }

    public static boolean isProfilerSectionData(Object value) {
        return isAny(value, PROFILER_SECTION_DATA_CLASSES);
    }

    public static boolean isAnrWatchDog(Object value) {
        return isAny(value, ANR_WATCHDOG_CLASSES);
    }

    public static boolean isAnrError(Object value) {
        return isAny(value, ANR_ERROR_CLASSES);
    }

    public static Map<String, Object> describeCurrentStatsEngine() {
        Object statsEngine = currentStatsEngine();
        return statsEngine != null ? describeStatsEngine(statsEngine) : Collections.<String, Object>emptyMap();
    }

    public static Map<String, Object> describeStatsEngine(Object statsEngine) {
        requireAny(statsEngine, STATS_ENGINE_CLASSES, "StatsEngine");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", statsEngine.getClass().getName());
        putBooleanField(result, statsEngine, "enabled", new String[]{"enabled", "a"});
        putField(result, statsEngine, "neutralStats", new String[]{"neutralStats", "b"});
        Object teamStats = getOptionalField(statsEngine, new String[]{"teamStats", "c"});
        result.put("teamStats", teamStats);
        result.put("teamStatsSize", Integer.valueOf(arrayOrIterableSize(teamStats)));
        putIntField(result, statsEngine, "lastFrame", new String[]{"lastFrame", "d"});
        putBooleanField(result, statsEngine, "historyRecordingEnabled",
                new String[]{"historyRecordingEnabled", "e"});
        putField(result, statsEngine, "unitKillEventDispatcher",
                new String[]{"unitKillEventDispatcher", "f"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeStatsEventDispatcher(Object dispatcher) {
        requireAny(dispatcher, STATS_EVENT_DISPATCHER_CLASSES, "StatsEventDispatcher");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", dispatcher.getClass().getName());
        Object listeners = getOptionalField(dispatcher, new String[]{"listeners", "a"});
        result.put("listeners", listeners);
        result.put("listenerCount", Integer.valueOf(arrayOrIterableSize(listeners)));
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeStatsTeamStats(Object stats) {
        requireAny(stats, STATS_TEAM_STATS_CLASSES, "Stats TeamStats");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", stats.getClass().getName());
        putIntField(result, stats, "unitsKilled", new String[]{"unitsKilled", "c"});
        putIntField(result, stats, "buildingsKilled", new String[]{"buildingsKilled", "d"});
        putIntField(result, stats, "experimentalsKilled", new String[]{"experimentalsKilled", "e"});
        putIntField(result, stats, "unitsLost", new String[]{"unitsLost", "f"});
        putIntField(result, stats, "buildingsLost", new String[]{"buildingsLost", "g"});
        putIntField(result, stats, "experimentalsLost", new String[]{"experimentalsLost", "h"});
        putField(result, stats, "history", new String[]{"history", "l"});
        putByteField(result, stats, "serializationVersion", new String[]{"serializationVersion", "m"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeStatsHistory(Object history) {
        requireAny(history, STATS_HISTORY_CLASSES, "StatsHistory");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", history.getClass().getName());
        putIntField(result, history, "teamIndex", new String[]{"teamIndex", "a"});
        Object metricSeries = getOptionalField(history, new String[]{"metricSeries", "b"});
        result.put("metricSeries", metricSeries);
        result.put("metricSeriesSize", Integer.valueOf(arrayOrIterableSize(metricSeries)));
        result.put("hasHistory", Boolean.valueOf(hasStatsHistory(history)));
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeStatsHistoryPoint(Object point) {
        requireAny(point, STATS_HISTORY_POINT_CLASSES, "StatsHistoryPoint");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", point.getClass().getName());
        putIntField(result, point, "frame", new String[]{"frame", "a"});
        putIntField(result, point, "value", new String[]{"value", "b"});
        return Collections.unmodifiableMap(result);
    }

    public static Object getStatsForTeam(Object statsEngine, Object team) {
        requireAny(statsEngine, STATS_ENGINE_CLASSES, "StatsEngine");
        requireAny(team, TEAM_CLASSES, "Team");
        return RustedReflection.invokeInstance(statsEngine, new String[]{"getStatsForTeam", "a"}, team);
    }

    public static Object getStatsForUnit(Object statsEngine, Object unit) {
        requireAny(statsEngine, STATS_ENGINE_CLASSES, "StatsEngine");
        requireAny(unit, UNIT_CLASSES, "Unit");
        return RustedReflection.invokeInstance(statsEngine, new String[]{"getStatsForUnit", "a"}, unit);
    }

    public static List<Object> teamStatsSnapshot(Object statsEngine) {
        requireAny(statsEngine, STATS_ENGINE_CLASSES, "StatsEngine");
        Object teamStats = RustedReflection.getFieldValue(statsEngine, new String[]{"teamStats", "c"});
        List<Object> snapshot = RustedReflection.snapshotIterable(teamStats);
        List<Object> result = new ArrayList<Object>();
        for (Object item : snapshot) {
            if (item != null) {
                result.add(item);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public static List<Object> statsWithHistorySnapshot(Object statsEngine) {
        requireAny(statsEngine, STATS_ENGINE_CLASSES, "StatsEngine");
        Object value = RustedReflection.invokeInstance(statsEngine, new String[]{"getStatsWithHistory", "d"});
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(value));
    }

    public static int statsHistoryTeamIndex(Object history) {
        requireAny(history, STATS_HISTORY_CLASSES, "StatsHistory");
        Object value = RustedReflection.invokeInstance(history, new String[]{"getTeamIndex", "b"});
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    public static boolean hasStatsHistory(Object history) {
        requireAny(history, STATS_HISTORY_CLASSES, "StatsHistory");
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(history, new String[]{"hasHistory", "c"}));
    }

    public static Object getStatsHistorySeries(Object history, Object metric) {
        requireAny(history, STATS_HISTORY_CLASSES, "StatsHistory");
        requireAny(metric, STATS_HISTORY_METRIC_CLASSES, "StatsHistoryMetric");
        return RustedReflection.invokeInstance(history, new String[]{"getSeries", "a"}, metric);
    }

    public static int getStatsHistoryValueAtFrame(Object history, Object metric, int frame) {
        requireAny(history, STATS_HISTORY_CLASSES, "StatsHistory");
        requireAny(metric, STATS_HISTORY_METRIC_CLASSES, "StatsHistoryMetric");
        Object value = RustedReflection.invokeInstance(history, new String[]{"getValueAtFrame", "a"},
                metric, Integer.valueOf(frame));
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    public static int getStatsHistorySeriesValueAtFrame(Object series, int frame) {
        requireAny(series, STATS_HISTORY_SERIES_CLASSES, "StatsHistorySeries");
        Object value = RustedReflection.invokeInstance(series, new String[]{"getValueAtFrame", "a"},
                Integer.valueOf(frame));
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    public static List<Object> statsHistoryMetrics() {
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(
                RustedReflection.invokeStatic(STATS_HISTORY_METRIC_CLASSES, new String[]{"values"})));
    }

    public static List<String> statsHistoryMetricNames() {
        return enumNames(statsHistoryMetrics());
    }

    public static Object statsHistoryMetric(String name) {
        return RustedReflection.invokeStatic(STATS_HISTORY_METRIC_CLASSES, new String[]{"valueOf"}, name);
    }

    public static Object getStatsHistoryMetricValueSource(Object metric) {
        requireAny(metric, STATS_HISTORY_METRIC_CLASSES, "StatsHistoryMetric");
        return RustedReflection.invokeInstance(metric, new String[]{"getValueSource", "a"});
    }

    public static List<Object> teamStatValueSources() {
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(
                RustedReflection.invokeStatic(TEAM_STAT_VALUE_SOURCE_CLASSES, new String[]{"values"})));
    }

    public static List<String> teamStatValueSourceNames() {
        return enumNames(teamStatValueSources());
    }

    public static Object teamStatValueSource(String name) {
        return RustedReflection.invokeStatic(TEAM_STAT_VALUE_SOURCE_CLASSES, new String[]{"valueOf"}, name);
    }

    public static int getTeamStatValue(Object valueSource, Object team) {
        requireAny(valueSource, TEAM_STAT_VALUE_SOURCE_CLASSES, "TeamStatValueSource");
        requireAny(team, TEAM_CLASSES, "Team");
        Object value = RustedReflection.invokeInstance(valueSource, new String[]{"getValueForTeam", "a"}, team);
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    public static Object newPerformanceTimer(String label) {
        return RustedReflection.newInstance(PERFORMANCE_TIMER_CLASSES, label);
    }

    public static Object newPerformanceTimer(String label, boolean enabled) {
        return RustedReflection.newInstance(PERFORMANCE_TIMER_CLASSES, label, Boolean.valueOf(enabled));
    }

    public static Map<String, Object> describePerformanceTimer(Object timer) {
        requireAny(timer, PERFORMANCE_TIMER_CLASSES, "PerformanceTimer");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", timer.getClass().getName());
        putBooleanField(result, timer, "enabled", new String[]{"enabled", "a"});
        putIntField(result, timer, "sampleCount", new String[]{"sampleCount", "b"});
        putDoubleField(result, timer, "totalMillis", new String[]{"totalMillis", "c"});
        putDoubleField(result, timer, "peakMillis", new String[]{"peakMillis", "d"});
        putLongField(result, timer, "startTimeNanos", new String[]{"startTimeNanos", "e"});
        putStringField(result, timer, "label", new String[]{"label", "f"});
        result.put("summary", formatPerformanceTimerSummary(timer));
        return Collections.unmodifiableMap(result);
    }

    public static void startPerformanceTimer(Object timer) {
        requireAny(timer, PERFORMANCE_TIMER_CLASSES, "PerformanceTimer");
        RustedReflection.invokeInstance(timer, new String[]{"start", "a"});
    }

    public static void stopPerformanceTimer(Object timer) {
        requireAny(timer, PERFORMANCE_TIMER_CLASSES, "PerformanceTimer");
        RustedReflection.invokeInstance(timer, new String[]{"stop", "b"});
    }

    public static String formatPerformanceTimerSummary(Object timer) {
        requireAny(timer, PERFORMANCE_TIMER_CLASSES, "PerformanceTimer");
        Object value = RustedReflection.invokeInstance(timer, new String[]{"formatSummary", "c"});
        return value != null ? value.toString() : "";
    }

    public static void resetPerformanceTimer(Object timer) {
        requireAny(timer, PERFORMANCE_TIMER_CLASSES, "PerformanceTimer");
        RustedReflection.invokeInstance(timer, new String[]{"reset", "f"});
    }

    public static Object newGameProfiler(Object gameEngine) {
        if (!GameEngineDiagnostics.isGameEngine(gameEngine)) {
            throw new IllegalArgumentException("Expected GameEngine, got " + describe(gameEngine));
        }
        return RustedReflection.newInstance(GAME_PROFILER_CLASSES, gameEngine);
    }

    public static Map<String, Object> describeGameProfiler(Object profiler) {
        requireAny(profiler, GAME_PROFILER_CLASSES, "GameProfiler");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", profiler.getClass().getName());
        putField(result, profiler, "gameEngine", new String[]{"gameEngine", "a"});
        putIntField(result, profiler, "maxProfilerSections", new String[]{"maxProfilerSections", "c"});
        putField(result, profiler, "sectionData", new String[]{"sectionData", "e"});
        putField(result, profiler, "paint", new String[]{"paint", "f"});
        putField(result, profiler, "scratchRect", new String[]{"scratchRect", "g"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeProfilerSectionData(Object sectionData) {
        requireAny(sectionData, PROFILER_SECTION_DATA_CLASSES, "ProfilerSectionData");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", sectionData.getClass().getName());
        Object startTimes = getOptionalField(sectionData, new String[]{"sectionStartTimesNanos", "a"});
        Object totalTimes = getOptionalField(sectionData, new String[]{"sectionTotalTimesNanos", "b"});
        Object lastFrameMillis = getOptionalField(sectionData, new String[]{"sectionLastFrameMillis", "c"});
        result.put("sectionStartTimesNanosSize", Integer.valueOf(arrayOrIterableSize(startTimes)));
        result.put("sectionTotalTimesNanosSize", Integer.valueOf(arrayOrIterableSize(totalTimes)));
        result.put("sectionLastFrameMillisSize", Integer.valueOf(arrayOrIterableSize(lastFrameMillis)));
        result.put("activeSectionCount", Integer.valueOf(countNonZero(startTimes)));
        result.put("timedSectionCount", Integer.valueOf(countNonZero(totalTimes)));
        putField(result, sectionData, "profiler", new String[]{"profiler", "f"});
        return Collections.unmodifiableMap(result);
    }

    public static List<Object> profilerSections() {
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(
                RustedReflection.invokeStatic(PROFILER_SECTION_CLASSES, new String[]{"values"})));
    }

    public static List<String> profilerSectionNames() {
        return enumNames(profilerSections());
    }

    public static Object profilerSection(String name) {
        return RustedReflection.invokeStatic(PROFILER_SECTION_CLASSES, new String[]{"valueOf"}, name);
    }

    public static Object newAnrWatchDog() {
        return RustedReflection.newInstance(ANR_WATCHDOG_CLASSES);
    }

    public static Object newAnrWatchDog(int timeoutMillis) {
        return RustedReflection.newInstance(ANR_WATCHDOG_CLASSES, Integer.valueOf(timeoutMillis));
    }

    public static Map<String, Object> describeAnrWatchDog(Object watchDog) {
        requireAny(watchDog, ANR_WATCHDOG_CLASSES, "AnrWatchDog");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", watchDog.getClass().getName());
        putField(result, watchDog, "anrListener", new String[]{"anrListener", "c"});
        putField(result, watchDog, "interruptionListener", new String[]{"interruptionListener", "d"});
        putField(result, watchDog, "mainHandler", new String[]{"mainHandler", "e"});
        putIntField(result, watchDog, "timeoutMillis", new String[]{"timeoutMillis", "f"});
        putStringField(result, watchDog, "threadNamePrefix", new String[]{"threadNamePrefix", "g"});
        putBooleanField(result, watchDog, "includeThreadsWithoutStackTrace",
                new String[]{"includeThreadsWithoutStackTrace", "h"});
        putBooleanField(result, watchDog, "ignoreDebugger", new String[]{"ignoreDebugger", "i"});
        putIntField(result, watchDog, "tick", new String[]{"tick", "j"});
        putField(result, watchDog, "tickerRunnable", new String[]{"tickerRunnable", "k"});
        return Collections.unmodifiableMap(result);
    }

    public static Object newMainOnlyAnrError() {
        return RustedReflection.invokeStatic(ANR_ERROR_CLASSES, new String[]{"newMainOnly", "a"});
    }

    public static Object newAnrErrorWithStackTraces(String threadNamePrefix, boolean includeThreadsWithoutStackTrace) {
        return RustedReflection.invokeStatic(ANR_ERROR_CLASSES, new String[]{"newWithStackTraces", "a"},
                threadNamePrefix, Boolean.valueOf(includeThreadsWithoutStackTrace));
    }

    public static String formatAnrThreadTitle(Thread thread) {
        Object value = RustedReflection.invokeStatic(ANR_ERROR_CLASSES, new String[]{"formatThreadTitle", "a"},
                thread);
        return value != null ? value.toString() : "";
    }

    private static List<String> enumNames(List<Object> values) {
        List<String> result = new ArrayList<String>();
        for (Object value : values) {
            if (value instanceof Enum<?>) {
                result.add(((Enum<?>) value).name());
            } else if (value != null) {
                result.add(value.toString());
            }
        }
        return Collections.unmodifiableList(result);
    }

    private static int arrayOrIterableSize(Object value) {
        if (value == null) {
            return 0;
        }
        return RustedReflection.snapshotIterable(value).size();
    }

    private static int countNonZero(Object value) {
        int count = 0;
        for (Object item : RustedReflection.snapshotIterable(value)) {
            if (item instanceof Number && ((Number) item).doubleValue() != 0.0D) {
                count++;
            }
        }
        return count;
    }

    private static boolean isAny(Object value, String[] classNames) {
        return value != null && RustedReflection.isAnyClass(value.getClass(), classNames);
    }

    private static void requireAny(Object value, String[] classNames, String label) {
        if (value == null || !RustedReflection.isAnyClass(value.getClass(), classNames)) {
            throw new IllegalArgumentException("Expected " + label + ", got " + describe(value));
        }
    }

    private static Object getOptionalField(Object owner, String[] fieldNames) {
        try {
            return RustedReflection.getFieldValue(owner, fieldNames);
        } catch (RuntimeException ignored) {
            return null;
        }
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

    private static void putBooleanField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        try {
            result.put(key, Boolean.valueOf(RustedReflection.getBooleanField(owner, fieldNames)));
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
            result.put(key, Long.valueOf(value instanceof Number ? ((Number) value).longValue() : 0L));
        } catch (RuntimeException ignored) {
        }
    }

    private static void putDoubleField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        try {
            Object value = RustedReflection.getFieldValue(owner, fieldNames);
            result.put(key, Double.valueOf(value instanceof Number ? ((Number) value).doubleValue() : 0.0D));
        } catch (RuntimeException ignored) {
        }
    }

    private static void putByteField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        try {
            Object value = RustedReflection.getFieldValue(owner, fieldNames);
            result.put(key, Byte.valueOf(value instanceof Number ? ((Number) value).byteValue() : 0));
        } catch (RuntimeException ignored) {
        }
    }

    private static String describe(Object value) {
        return value == null ? "null" : value.getClass().getName();
    }
}
