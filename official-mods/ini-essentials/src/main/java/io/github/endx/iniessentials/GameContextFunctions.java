package io.github.endx.iniessentials;

import io.github.endx.rustedfabricapi.api.lobby.LobbyGameSetup;
import io.github.endx.rustedfabricapi.api.logic.LogicNumberFunctionDefinition;
import io.github.endx.rustedfabricapi.api.logic.LogicNumberFunctions;

import java.util.concurrent.atomic.AtomicBoolean;

/** Synchronized game-rule values available to native runtime number expressions. */
final class GameContextFunctions {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();

    private GameContextFunctions() { }

    static void register() {
        if (!REGISTERED.compareAndSet(false, true)) return;
        LogicNumberFunctions.register(LogicNumberFunctionDefinition.of(
                "incomemultiplier", 0, ignored -> currentIncomeMultiplier()));
    }

    static float currentIncomeMultiplier() {
        try {
            return LobbyGameSetup.currentIncomeMultiplier();
        } catch (IllegalStateException noActiveSetup) {
            // Metadata validation can evaluate expressions before a game setup exists.
            return 1.0F;
        }
    }
}
