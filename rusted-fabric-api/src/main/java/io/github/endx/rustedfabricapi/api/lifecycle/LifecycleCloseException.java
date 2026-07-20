package io.github.endx.rustedfabricapi.api.lifecycle;

/** Aggregated failure thrown by {@link LifecycleScope#close()}. */
public final class LifecycleCloseException extends RuntimeException {
    private final LifecycleCloseReport report;

    LifecycleCloseException(LifecycleCloseReport report) {
        super("Failed to close " + report.failures().size() + " of " + report.attempted()
                + " resources in lifecycle scope " + report.scopeName());
        this.report = report;
        for (LifecycleCloseFailure failure : report.failures()) {
            addSuppressed(failure.cause());
        }
    }

    public LifecycleCloseReport report() { return report; }
}
