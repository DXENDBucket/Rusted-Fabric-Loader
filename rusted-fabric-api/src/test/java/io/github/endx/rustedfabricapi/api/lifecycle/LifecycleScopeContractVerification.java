package io.github.endx.rustedfabricapi.api.lifecycle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class LifecycleScopeContractVerification {
    private LifecycleScopeContractVerification() {
    }

    public static void verify() {
        verifyReverseOrderAndChildren();
        verifyFailureAggregationAndIdempotence();
        verifyEarlyCleanupAndForget();
    }

    private static void verifyReverseOrderAndChildren() {
        List<String> closed = new ArrayList<String>();
        LifecycleScope parent = LifecycleScope.create("contract");
        parent.onClose("first", () -> closed.add("first"));
        LifecycleScope child = parent.child("feature");
        child.onClose("child-first", () -> closed.add("child-first"));
        child.onClose("child-last", () -> closed.add("child-last"));
        parent.onClose("parent-last", () -> closed.add("parent-last"));

        LifecycleCloseReport report = parent.closeReport();
        require(report.successful() && report.attempted() == 3 && report.succeeded() == 3,
                "successful lifecycle close report was incorrect");
        require(closed.equals(Arrays.asList(
                        "parent-last", "child-last", "child-first", "first")),
                "lifecycle resources or children did not close in reverse order");
        require(parent.isClosed() && child.isClosed(),
                "closed lifecycle scope state was not retained");
        require(parent.closeReport() == report,
                "repeated lifecycle close did not return the original report");
    }

    private static void verifyFailureAggregationAndIdempotence() {
        List<String> closed = new ArrayList<String>();
        LifecycleScope scope = LifecycleScope.create("failures");
        scope.onClose("last", () -> closed.add("last"));
        scope.onClose("broken", () -> {
            closed.add("broken");
            throw new IllegalStateException("synthetic");
        });
        scope.onClose("first", () -> closed.add("first"));

        LifecycleCloseException thrown = null;
        try {
            scope.close();
        } catch (LifecycleCloseException expected) {
            thrown = expected;
        }
        require(thrown != null, "lifecycle close did not report a resource failure");
        require(closed.equals(Arrays.asList("first", "broken", "last")),
                "one lifecycle failure skipped remaining cleanup");
        require(thrown.report().attempted() == 3
                        && thrown.report().succeeded() == 2
                        && thrown.report().failures().size() == 1
                        && thrown.getSuppressed().length == 1
                        && "broken".equals(thrown.report().failures().get(0).label()),
                "lifecycle failure details were incomplete");

        boolean repeatedThrows = false;
        try {
            scope.close();
        } catch (LifecycleCloseException expected) {
            repeatedThrows = expected.report() == thrown.report();
        }
        require(repeatedThrows && closed.size() == 3,
                "repeated close reran cleanup or changed its report");
    }

    private static void verifyEarlyCleanupAndForget() {
        List<String> closed = new ArrayList<String>();
        LifecycleScope scope = LifecycleScope.create("ownership");
        AutoCloseable early = scope.onClose("early", () -> closed.add("early"));
        AutoCloseable forgotten = () -> closed.add("forgotten");
        scope.own("forgotten", forgotten);
        require(scope.ownedCount() == 2 && scope.forget(forgotten),
                "lifecycle scope could not forget an owned resource");
        try {
            early.close();
            early.close();
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
        scope.close();
        require(closed.equals(Arrays.asList("early")),
                "early cleanup was not idempotent or forgotten resource was closed");

        AutoCloseable late = () -> closed.add("late");
        boolean lateRejected = false;
        try {
            scope.own("late", late);
        } catch (IllegalStateException expected) {
            lateRejected = true;
        }
        require(lateRejected && closed.equals(Arrays.asList("early")),
                "closed lifecycle scope accepted or closed a caller-owned late resource");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
