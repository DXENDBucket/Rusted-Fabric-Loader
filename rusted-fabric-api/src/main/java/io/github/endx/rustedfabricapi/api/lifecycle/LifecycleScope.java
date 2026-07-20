package io.github.endx.rustedfabricapi.api.lifecycle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Owns event subscriptions, service registrations, and other resources for one mod feature.
 * Resources close in reverse registration order, and one failure never skips later cleanup.
 */
public final class LifecycleScope implements AutoCloseable {
    private enum State { OPEN, CLOSING, CLOSED }

    private final Object lock = new Object();
    private final String name;
    private final ArrayList<OwnedResource> resources = new ArrayList<OwnedResource>();
    private State state = State.OPEN;
    private Thread closingThread;
    private LifecycleCloseReport closeReport;

    private LifecycleScope(String name) {
        String checked = Objects.requireNonNull(name, "name").trim();
        if (checked.isEmpty()) throw new IllegalArgumentException("Scope name must not be empty");
        this.name = checked;
    }

    public static LifecycleScope create(String name) {
        return new LifecycleScope(name);
    }

    public String name() { return name; }

    public <T extends AutoCloseable> T own(T resource) {
        T checked = Objects.requireNonNull(resource, "resource");
        return own(checked.getClass().getName(), checked);
    }

    public <T extends AutoCloseable> T own(String label, T resource) {
        T checked = Objects.requireNonNull(resource, "resource");
        add(new OwnedResource(validateLabel(label), checked));
        return checked;
    }

    /** Adds an idempotent cleanup action and returns a handle that may also run it early. */
    public AutoCloseable onClose(String label, Runnable cleanup) {
        CleanupAction action = new CleanupAction(Objects.requireNonNull(cleanup, "cleanup"));
        own(label, action);
        return action;
    }

    /** Creates a child whose resources are closed before earlier parent resources. */
    public LifecycleScope child(String childName) {
        LifecycleScope child = create(name + '/' + validateLabel(childName));
        return own("child:" + childName, child);
    }

    /** Stops this scope from managing one exact resource without closing that resource. */
    public boolean forget(AutoCloseable resource) {
        if (resource == null) return false;
        synchronized (lock) {
            if (state != State.OPEN) return false;
            for (int i = resources.size() - 1; i >= 0; i--) {
                if (resources.get(i).resource == resource) {
                    resources.remove(i);
                    return true;
                }
            }
            return false;
        }
    }

    public int ownedCount() {
        synchronized (lock) { return resources.size(); }
    }

    public boolean isOpen() {
        synchronized (lock) { return state == State.OPEN; }
    }

    public boolean isClosed() {
        synchronized (lock) { return state == State.CLOSED; }
    }

    /**
     * Closes all resources and returns every failure instead of throwing it. Repeated calls return
     * the same report. Concurrent callers wait for the first close to finish.
     */
    public LifecycleCloseReport closeReport() {
        List<OwnedResource> snapshot;
        synchronized (lock) {
            while (state == State.CLOSING) {
                if (closingThread == Thread.currentThread()) {
                    throw new IllegalStateException("Recursive close of lifecycle scope " + name);
                }
                try {
                    lock.wait();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(
                            "Interrupted while waiting for lifecycle scope " + name, exception);
                }
            }
            if (state == State.CLOSED) return closeReport;
            state = State.CLOSING;
            closingThread = Thread.currentThread();
            snapshot = new ArrayList<OwnedResource>(resources);
            Collections.reverse(snapshot);
            resources.clear();
        }

        ArrayList<LifecycleCloseFailure> failures =
                new ArrayList<LifecycleCloseFailure>();
        for (OwnedResource owned : snapshot) {
            try {
                owned.resource.close();
            } catch (Throwable failure) {
                failures.add(new LifecycleCloseFailure(owned.label, failure));
            }
        }
        LifecycleCloseReport result = new LifecycleCloseReport(name, snapshot.size(), failures);
        synchronized (lock) {
            closeReport = result;
            closingThread = null;
            state = State.CLOSED;
            lock.notifyAll();
        }
        return result;
    }

    @Override
    public void close() {
        LifecycleCloseReport report = closeReport();
        if (!report.successful()) throw new LifecycleCloseException(report);
    }

    private void add(OwnedResource resource) {
        synchronized (lock) {
            if (state != State.OPEN) {
                throw new IllegalStateException("Lifecycle scope is not open: " + name);
            }
            resources.add(resource);
        }
    }

    private static String validateLabel(String label) {
        String checked = Objects.requireNonNull(label, "label").trim();
        if (checked.isEmpty()) throw new IllegalArgumentException("Resource label must not be empty");
        return checked;
    }

    private static final class OwnedResource {
        final String label;
        final AutoCloseable resource;

        OwnedResource(String label, AutoCloseable resource) {
            this.label = label;
            this.resource = resource;
        }
    }

    private static final class CleanupAction implements AutoCloseable {
        private final Runnable cleanup;
        private final AtomicBoolean active = new AtomicBoolean(true);

        CleanupAction(Runnable cleanup) { this.cleanup = cleanup; }

        @Override
        public void close() {
            if (active.compareAndSet(true, false)) cleanup.run();
        }
    }
}
