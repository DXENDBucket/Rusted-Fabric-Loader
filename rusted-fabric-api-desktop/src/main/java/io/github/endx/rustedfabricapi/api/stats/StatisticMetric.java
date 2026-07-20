package io.github.endx.rustedfabricapi.api.stats;

import rustedwarfare.stats.StatsHistoryMetric;

/** Stable names for the metrics tracked by the native post-game history graph. */
public enum StatisticMetric {
    INCOME(StatsHistoryMetric.INCOME),
    ARMY_VALUE(StatsHistoryMetric.ARMY_VALUE),
    BUILDING_VALUE(StatsHistoryMetric.BUILDING_VALUE),
    TOTAL_VALUE(StatsHistoryMetric.TOTAL_VALUE);

    private final StatsHistoryMetric nativeMetric;

    StatisticMetric(StatsHistoryMetric nativeMetric) {
        this.nativeMetric = nativeMetric;
    }

    StatsHistoryMetric nativeMetric() {
        return nativeMetric;
    }
}
