package io.github.endx.rustedfabricapi.api.path;

import rustedwarfare.path.PathRequest;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/** Handle returned immediately while the game's native path solvers work asynchronously. */
public final class PathRequestHandle {
    private final PathQuery query;
    private final PathRequest request;
    private final CompletableFuture<PathResult> future = new CompletableFuture<PathResult>();

    PathRequestHandle(PathQuery query, PathRequest request) {
        this.query = Objects.requireNonNull(query, "query");
        this.request = Objects.requireNonNull(request, "request");
    }

    public PathQuery query() { return query; }
    public PathRequest nativeRequest() { return request; }
    public CompletableFuture<PathResult> future() { return future; }
    public boolean done() { return future.isDone(); }
    public Optional<PathResult> resultNow() {
        return future.isDone() && !future.isCompletedExceptionally()
                ? Optional.ofNullable(future.getNow(null)) : Optional.empty();
    }

    void complete(PathResult result) {
        future.complete(result);
    }
}
