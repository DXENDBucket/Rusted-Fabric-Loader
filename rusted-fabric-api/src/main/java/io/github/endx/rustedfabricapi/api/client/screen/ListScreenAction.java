package io.github.endx.rustedfabricapi.api.client.screen;

import java.util.Objects;

/** One safe callback button displayed in a native list screen's bottom action row. */
public final class ListScreenAction {
    private final String label;
    private final Runnable callback;

    public ListScreenAction(String label, Runnable callback) {
        this.label = ListScreenEntry.checked(label, "label", 128, false);
        this.callback = Objects.requireNonNull(callback, "callback");
    }

    public static ListScreenAction of(String label, Runnable callback) {
        return new ListScreenAction(label, callback);
    }

    public String label() { return label; }

    public void invoke() { callback.run(); }
}
