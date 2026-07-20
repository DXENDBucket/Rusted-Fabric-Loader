package io.github.endx.rustedfabricapi.api.lifecycle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable result of closing every resource owned by a lifecycle scope. */
public final class LifecycleCloseReport {
    private final String scopeName;
    private final int attempted;
    private final List<LifecycleCloseFailure> failures;

    LifecycleCloseReport(String scopeName, int attempted,
            List<LifecycleCloseFailure> failures) {
        this.scopeName = scopeName;
        this.attempted = attempted;
        this.failures = Collections.unmodifiableList(
                new ArrayList<LifecycleCloseFailure>(failures));
    }

    public String scopeName() { return scopeName; }

    public int attempted() { return attempted; }

    public int succeeded() { return attempted - failures.size(); }

    public boolean successful() { return failures.isEmpty(); }

    public List<LifecycleCloseFailure> failures() { return failures; }

    @Override
    public String toString() {
        return "LifecycleCloseReport{" + scopeName + ", attempted=" + attempted
                + ", succeeded=" + succeeded() + ", failures=" + failures.size() + '}';
    }
}
