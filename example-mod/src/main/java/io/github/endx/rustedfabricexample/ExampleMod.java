package io.github.endx.rustedfabricexample;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import rustedwarfare.core.GameEngine;
import rustedwarfare.core.SettingsEngine;
import rustedwarfare.mod.ModManager;

import java.util.concurrent.atomic.AtomicBoolean;

public final class ExampleMod implements ModInitializer, ClientModInitializer {
    public static final String MOD_ID = "rusted_fabric_example";
    private static final AtomicBoolean VISIBLE_SETTINGS_TWEAKS_STARTED = new AtomicBoolean();

    @Override
    public void onInitialize() {
        log("main entrypoint");
        logNamedGameTypes("main");
        startVisibleSettingsTweaks("main");
    }

    @Override
    public void onInitializeClient() {
        log("client entrypoint");
        logNamedGameTypes("client");
        startVisibleSettingsTweaks("client");
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

    static void startVisibleSettingsTweaks(String stage) {
        if (!VISIBLE_SETTINGS_TWEAKS_STARTED.compareAndSet(false, true)) {
            return;
        }

        Thread thread = new Thread(() -> waitForSettingsAndApplyTweaks(stage), "Rusted Fabric Example Tweaks");
        thread.setDaemon(true);
        thread.start();
    }

    private static void waitForSettingsAndApplyTweaks(String stage) {
        Throwable lastFailure = null;

        for (int attempt = 1; attempt <= 600; attempt++) {
            try {
                GameEngine engine = GameEngine.getInstance();
                if (engine != null && engine.settings != null) {
                    applyVisibleSettingsTweaks(stage, engine.settings);
                    return;
                }
            } catch (Throwable t) {
                lastFailure = t;
            }

            try {
                Thread.sleep(250L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log("visible settings tweaks interrupted before GameEngine was ready");
                return;
            }
        }

        if (lastFailure != null) {
            log("visible settings tweaks timed out, last failure="
                    + lastFailure.getClass().getName() + ": " + lastFailure.getMessage());
        } else {
            log("visible settings tweaks timed out before SettingsEngine was ready");
        }
    }

    private static void applyVisibleSettingsTweaks(String stage, SettingsEngine settings) {
        boolean oldShowFps = settings.showFps;
        boolean oldShowHp = settings.showHp;
        boolean oldShowHpChanges = settings.showHpChanges;
        boolean oldShowUnitIcons = settings.showUnitIcons;
        boolean oldShowWarLogOnScreen = settings.showWarLogOnScreen;

        settings.showFps = true;
        settings.showHp = true;
        settings.showHpChanges = true;
        settings.showUnitIcons = true;
        settings.showWarLogOnScreen = true;

        log("visible settings tweaks applied from " + stage
                + ": showFps " + oldShowFps + "->" + settings.showFps
                + ", showHp " + oldShowHp + "->" + settings.showHp
                + ", showHpChanges " + oldShowHpChanges + "->" + settings.showHpChanges
                + ", showUnitIcons " + oldShowUnitIcons + "->" + settings.showUnitIcons
                + ", showWarLogOnScreen " + oldShowWarLogOnScreen + "->" + settings.showWarLogOnScreen);
    }

    static void log(String message) {
        System.out.println("[Rusted Fabric Example] " + message);
    }
}
