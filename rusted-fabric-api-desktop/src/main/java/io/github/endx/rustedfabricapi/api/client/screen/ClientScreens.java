package io.github.endx.rustedfabricapi.api.client.screen;

import java.util.Optional;

import io.github.endx.rustedfabricapi.internal.client.screen.ScreenRuntime;

/** Read-only screen-stack queries and safe native navigation operations. */
public final class ClientScreens {
    private ClientScreens() {
    }

    public static boolean isUiOpen() { return ScreenRuntime.isUiOpen(); }
    public static Optional<UiDocumentSnapshot> activePage() {
        return ScreenRuntime.activePage();
    }
    public static Optional<UiDocumentSnapshot> popup() { return ScreenRuntime.popup(); }
    public static Optional<UiDocumentSnapshot> alert() { return ScreenRuntime.alert(); }
    public static Optional<UiDocumentSnapshot> topmost() { return ScreenRuntime.topmost(); }

    /** Closes the alert or popup currently above the active page. */
    public static boolean closeTopmostOverlay() { return ScreenRuntime.closeTopmostOverlay(); }

    /** Uses the game's normal history behavior, including closing the UI when history is empty. */
    public static void back() { ScreenRuntime.back(); }

    /** Reloads the active page with its current native metadata. */
    public static void reloadActivePage() { ScreenRuntime.reloadActivePage(); }

    /** Clears page navigation history without closing the current page. */
    public static void clearHistory() { ScreenRuntime.clearHistory(); }
}
