package io.github.endx.rustedfabricapi.api.client.screen.dialog;

/** Completion callback for an API-owned client dialog. */
@FunctionalInterface
public interface DialogCallback {
    void onComplete(DialogResult result);
}
