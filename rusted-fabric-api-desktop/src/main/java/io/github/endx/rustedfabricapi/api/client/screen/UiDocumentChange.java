package io.github.endx.rustedfabricapi.api.client.screen;

import java.util.Optional;

/** Immutable previous/next document pair for active-page and topmost changes. */
public final class UiDocumentChange {
    private final UiDocumentSnapshot previous;
    private final UiDocumentSnapshot next;

    public UiDocumentChange(UiDocumentSnapshot previous, UiDocumentSnapshot next) {
        if (previous == null && next == null) {
            throw new IllegalArgumentException("at least one document must be present");
        }
        this.previous = previous;
        this.next = next;
    }

    public Optional<UiDocumentSnapshot> previous() { return Optional.ofNullable(previous); }
    public Optional<UiDocumentSnapshot> next() { return Optional.ofNullable(next); }
    public boolean opened() { return previous == null && next != null; }
    public boolean closed() { return previous != null && next == null; }
    public boolean replaced() { return previous != null && next != null; }

    @Override
    public String toString() {
        return "UiDocumentChange{" + previous + " -> " + next + '}';
    }
}
