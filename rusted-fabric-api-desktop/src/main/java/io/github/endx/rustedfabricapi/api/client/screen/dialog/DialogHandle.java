package io.github.endx.rustedfabricapi.api.client.screen.dialog;

import io.github.endx.rustedfabricapi.internal.client.screen.DialogRuntime;

/** Identity and dismissal control for one API-owned dialog. */
public final class DialogHandle {
    private final long id;

    DialogHandle(long id) {
        if (id <= 0L) throw new IllegalArgumentException("id must be positive");
        this.id = id;
    }

    public long id() { return id; }
    public boolean isOpen() { return DialogRuntime.isOpen(id); }

    /** Dismisses this dialog if it is still current, completing it as {@code DISMISSED}. */
    public boolean dismiss() { return DialogRuntime.dismiss(id); }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof DialogHandle && id == ((DialogHandle) other).id;
    }

    @Override
    public int hashCode() { return Long.hashCode(id); }

    @Override
    public String toString() { return "DialogHandle{" + id + '}'; }
}
