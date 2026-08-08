package io.github.endx.rustedfabricapi.api.asset.condition;

import java.util.Optional;
import java.util.OptionalInt;

import io.github.endx.rustedfabricapi.api.util.Identifier;

/** Immutable top-level condition-list result, including the first rejecting condition. */
public final class ResourceConditionEvaluation {
    private final boolean load;
    private final int evaluatedCount;
    private final int failedIndex;
    private final Identifier failedCondition;

    ResourceConditionEvaluation(boolean load, int evaluatedCount,
            int failedIndex, Identifier failedCondition) {
        this.load = load;
        this.evaluatedCount = evaluatedCount;
        this.failedIndex = failedIndex;
        this.failedCondition = failedCondition;
    }

    public boolean shouldLoad() { return load; }

    public int evaluatedCount() { return evaluatedCount; }

    public OptionalInt failedIndex() {
        return failedIndex >= 0 ? OptionalInt.of(failedIndex) : OptionalInt.empty();
    }

    public Optional<Identifier> failedCondition() {
        return Optional.ofNullable(failedCondition);
    }

    @Override public String toString() {
        return load ? "ResourceConditionEvaluation{load, evaluated=" + evaluatedCount + '}'
                : "ResourceConditionEvaluation{rejected=" + failedCondition
                + ", index=" + failedIndex + '}';
    }
}
