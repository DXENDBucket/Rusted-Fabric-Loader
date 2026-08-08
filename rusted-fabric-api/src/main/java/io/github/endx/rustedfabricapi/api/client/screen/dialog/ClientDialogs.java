package io.github.endx.rustedfabricapi.api.client.screen.dialog;

import java.util.Objects;
import java.util.Optional;

import io.github.endx.rustedfabricapi.internal.client.screen.DialogRuntime;

/** Java-callback dialogs backed by the game's native LibRocket popup. */
public final class ClientDialogs {
    private ClientDialogs() {
    }

    /**
     * Attempts to show a dialog without replacing an existing popup.
     *
     * @return a handle, or empty when the game or another mod already owns the popup slot
     */
    public static Optional<DialogHandle> show(DialogSpec spec, DialogCallback callback) {
        long id = DialogRuntime.show(Objects.requireNonNull(spec, "spec"),
                Objects.requireNonNull(callback, "callback"));
        return id > 0L ? Optional.of(new DialogHandle(id)) : Optional.empty();
    }
}
