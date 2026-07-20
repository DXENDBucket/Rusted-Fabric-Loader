package io.github.endx.rustedfabricapi.api.asset.reload;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import io.github.endx.rustedfabricapi.api.util.Identifier;

/** Immutable result of one complete prepare/apply resource reload. */
public final class ResourceReloadReport {
    private final ResourceReloadReason reason;
    private final List<Result> results;
    private final Map<Identifier, Result> byId;
    private final long durationNanos;

    ResourceReloadReport(ResourceReloadReason reason, List<Result> results, long durationNanos) {
        this.reason = Objects.requireNonNull(reason, "reason");
        this.results = Collections.unmodifiableList(new ArrayList<Result>(results));
        LinkedHashMap<Identifier, Result> indexed = new LinkedHashMap<Identifier, Result>();
        for (Result result : results) indexed.put(result.id(), result);
        this.byId = Collections.unmodifiableMap(indexed);
        this.durationNanos = Math.max(0L, durationNanos);
    }

    public ResourceReloadReason reason() { return reason; }

    public List<Result> results() { return results; }

    public Optional<Result> result(Identifier id) {
        return Optional.ofNullable(byId.get(Objects.requireNonNull(id, "id")));
    }

    public int listenerCount() { return results.size(); }

    public int failureCount() {
        int count = 0;
        for (Result result : results) if (result.status() != ResourceReloadStatus.APPLIED) count++;
        return count;
    }

    public boolean successful() { return failureCount() == 0; }

    public long durationNanos() { return durationNanos; }

    public static final class Result {
        private final Identifier id;
        private final ResourceReloadStatus status;
        private final String detail;
        private final Exception failure;

        Result(Identifier id, ResourceReloadStatus status, String detail, Exception failure) {
            this.id = Objects.requireNonNull(id, "id");
            this.status = Objects.requireNonNull(status, "status");
            this.detail = detail != null ? detail : "";
            this.failure = failure;
        }

        public Identifier id() { return id; }

        public ResourceReloadStatus status() { return status; }

        public String detail() { return detail; }

        public Optional<Exception> failure() { return Optional.ofNullable(failure); }

        @Override public String toString() {
            return "Result{" + id + '=' + status
                    + (detail.isEmpty() ? "" : ", " + detail) + '}';
        }
    }
}
