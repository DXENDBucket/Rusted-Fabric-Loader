package io.github.endx.rustedfabricexample;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import rustedwarfare.core.GameEngine;
import rustedwarfare.core.SettingsEngine;
import rustedwarfare.mod.ModManager;

public final class ExampleMod implements ModInitializer, ClientModInitializer {
    public static final String MOD_ID = "rusted_fabric_example";

    @Override
    public void onInitialize() {
        log("main entrypoint");
        logNamedGameTypes("main");
    }

    @Override
    public void onInitializeClient() {
        log("client entrypoint");
        logNamedGameTypes("client");
    }

    static void logNamedGameTypes(String stage) {
        log(stage + " GameEngine class=" + GameEngine.class.getName());

        GameEngine engine = null;
        try {
            engine = GameEngine.getInstance();
        } catch (Throwable t) {
            log(stage + " GameEngine.getInstance() failed: " + t.getClass().getName() + ": " + t.getMessage());
        }

        if (engine == null) {
            log(stage + " GameEngine is not initialized yet");
            return;
        }

        SettingsEngine settings = engine.settings;
        ModManager modManager = engine.modManager;
        log(stage + " engine=" + engine.getClass().getName()
                + ", showFps=" + (settings != null && settings.showFps)
                + ", modManager=" + (modManager != null ? modManager.getClass().getName() : "null"));
    }

    static void log(String message) {
        System.out.println("[Rusted Fabric Example] " + message);
    }
}
