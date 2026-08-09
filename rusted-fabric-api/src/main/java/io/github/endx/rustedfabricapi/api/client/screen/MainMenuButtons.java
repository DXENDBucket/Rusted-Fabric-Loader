package io.github.endx.rustedfabricapi.api.client.screen;

import java.util.List;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import io.github.endx.rustedfabricapi.internal.client.screen.MainMenuRuntime;

/** Registration point for buttons inserted below the game's built-in Mods button. */
public final class MainMenuButtons {
    private MainMenuButtons() {
    }

    public static RustedFabricEvent.Registration register(MainMenuButton button) {
        return MainMenuRuntime.register(button);
    }

    /** Deterministic registration-order snapshot. */
    public static List<MainMenuButton> registered() {
        return MainMenuRuntime.registered();
    }
}
