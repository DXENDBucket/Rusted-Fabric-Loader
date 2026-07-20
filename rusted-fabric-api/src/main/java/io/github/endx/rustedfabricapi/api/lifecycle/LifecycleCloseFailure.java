package io.github.endx.rustedfabricapi.api.lifecycle;

import java.util.Objects;

/** One resource failure captured while closing a {@link LifecycleScope}. */
public final class LifecycleCloseFailure {
    private final String label;
    private final Throwable cause;

    LifecycleCloseFailure(String label, Throwable cause) {
        this.label = Objects.requireNonNull(label, "label");
        this.cause = Objects.requireNonNull(cause, "cause");
    }

    public String label() { return label; }

    public Throwable cause() { return cause; }

    @Override
    public String toString() {
        return label + ": " + cause.getClass().getName()
                + (cause.getMessage() == null ? "" : ": " + cause.getMessage());
    }
}
