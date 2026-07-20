package io.github.endx.rustedfabricapi.api.scheduler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable report for one distinct native simulation tick. */
public final class TickExecutionReport {
    private final int nativeTick;
    private final long advancedTicks;
    private final List<Long> executedTaskSequences;
    private final List<Long> failedTaskSequences;
    private final int activeTaskCount;

    TickExecutionReport(int nativeTick, long advancedTicks, List<Long> executed,
            List<Long> failed, int activeTaskCount) {
        this.nativeTick = nativeTick;
        this.advancedTicks = advancedTicks;
        this.executedTaskSequences = immutable(executed);
        this.failedTaskSequences = immutable(failed);
        this.activeTaskCount = activeTaskCount;
    }

    public int nativeTick() { return nativeTick; }

    /** Native-tick distance consumed; zero means the duplicate/paused tick was ignored. */
    public long advancedTicks() { return advancedTicks; }

    public List<Long> executedTaskSequences() { return executedTaskSequences; }

    public List<Long> failedTaskSequences() { return failedTaskSequences; }

    public int executedCount() { return executedTaskSequences.size(); }

    public int failedCount() { return failedTaskSequences.size(); }

    public int succeededCount() { return executedCount() - failedCount(); }

    public int activeTaskCount() { return activeTaskCount; }

    private static List<Long> immutable(List<Long> values) {
        return Collections.unmodifiableList(new ArrayList<Long>(values));
    }

    @Override public String toString() {
        return "TickExecutionReport{tick=" + nativeTick + ", advanced=" + advancedTicks
                + ", executed=" + executedCount() + ", failed=" + failedCount()
                + ", active=" + activeTaskCount + '}';
    }
}
