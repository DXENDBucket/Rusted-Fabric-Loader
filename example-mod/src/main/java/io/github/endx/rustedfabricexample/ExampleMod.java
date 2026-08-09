package io.github.endx.rustedfabricexample;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import rustedwarfare.core.GameEngine;
import rustedwarfare.core.SettingsEngine;
import rustedwarfare.mod.ModManager;
import rustedwarfare.ui.script.RootScript;
import rustedwarfare.ui.script.ScriptEngine;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.github.endx.rustedfabricexample.ExampleDebugOverlay.*;

public final class ExampleMod implements ModInitializer, ClientModInitializer {
    public static final String MOD_ID = "rustedfabricexample";
    private static final AtomicBoolean VISIBLE_SETTINGS_TWEAKS_STARTED = new AtomicBoolean();
    private static final AtomicBoolean MAIN_MENU_POPUP_STARTED = new AtomicBoolean();
    private static final AtomicBoolean MAIN_MENU_POPUP_SHOWN = new AtomicBoolean();

    @Override
    public void onInitialize() {
        log("main entrypoint");
        TypedDesktopApiExample.register();
        logNamedGameTypes("main");
        startVisibleSettingsTweaks("main");
        startMainMenuPopup("main");
        ExampleEventProbes.registerMapEntryMessage("main");
        ExampleEventProbes.registerEventProbeMessages("main");
        registerOverlayRenderer("main");
    }

    @Override
    public void onInitializeClient() {
        log("client entrypoint");
        TypedDesktopApiExample.register();
        logNamedGameTypes("client");
        startVisibleSettingsTweaks("client");
        startMainMenuPopup("client");
        ExampleEventProbes.registerMapEntryMessage("client");
        ExampleEventProbes.registerEventProbeMessages("client");
        registerOverlayRenderer("client");
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

        SettingsEngine settings = engine.settingsEngine;
        ModManager modManager = engine.modManager;
        log(stage + " engine=" + engine.getClass().getName()
                + ", showFps=" + (settings != null && settings.showFps)
                + ", modManager=" + (modManager != null ? modManager.getClass().getName() : "null"));
    }

    static void rememberGameDir(Path gameDir) {
        ExampleDebugOverlay.rememberGameDir(gameDir);
    }

    static void startVisibleSettingsTweaks(String stage) {
        if (!VISIBLE_SETTINGS_TWEAKS_STARTED.compareAndSet(false, true)) {
            return;
        }

        Thread thread = new Thread(() -> waitForSettingsAndApplyTweaks(stage), "Rusted Fabric Example Tweaks");
        thread.setDaemon(true);
        thread.start();
    }

    static void startMainMenuPopup(String stage) {
        if (!MAIN_MENU_POPUP_STARTED.compareAndSet(false, true)) {
            return;
        }

        Thread thread = new Thread(() -> waitForMainMenuAndShowPopup(stage), "Rusted Fabric Example Main Menu Popup");
        thread.setDaemon(true);
        thread.start();
    }

    static void registerOverlayRenderer(String stage) {
        ExampleDebugOverlay.registerRenderer(stage);
    }

    private static void waitForSettingsAndApplyTweaks(String stage) {
        Throwable lastFailure = null;
        boolean settingsApplied = false;
        boolean settingsTimeoutLogged = false;

        for (int attempt = 1; !settingsApplied; attempt++) {
            try {
                GameEngine engine = GameEngine.getInstance();
                if (engine != null && engine.settingsEngine != null) {
                    applyVisibleSettingsTweaks(stage, engine.settingsEngine);
                    settingsApplied = true;
                }

                if (!settingsApplied && !settingsTimeoutLogged && attempt >= 600) {
                    settingsTimeoutLogged = true;
                    if (lastFailure != null) {
                        log("visible settings tweaks still waiting, last failure="
                                + lastFailure.getClass().getName() + ": " + lastFailure.getMessage());
                    } else {
                        log("visible settings tweaks still waiting before SettingsEngine was ready");
                    }
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

    private static void waitForMainMenuAndShowPopup(String stage) {
        Throwable lastFailure = null;
        boolean timeoutLogged = false;

        for (int attempt = 1; !MAIN_MENU_POPUP_SHOWN.get(); attempt++) {
            try {
                ScriptEngine scriptEngine = ScriptEngine.getInstance();
                if (scriptEngine != null) {
                    scriptEngine.addRunnableToQueue(() -> tryShowMainMenuPopup(stage, scriptEngine));
                }

                if (!MAIN_MENU_POPUP_SHOWN.get() && !timeoutLogged && attempt >= 600) {
                    timeoutLogged = true;
                    if (lastFailure != null) {
                        log("main menu popup still waiting, last failure="
                                + lastFailure.getClass().getName() + ": " + lastFailure.getMessage());
                    } else {
                        log("main menu popup still waiting before mainMenu.rml was shown");
                    }
                }
            } catch (Throwable t) {
                lastFailure = t;
            }

            try {
                Thread.sleep(250L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log("main menu popup interrupted before mainMenu.rml was shown");
                return;
            }
        }
    }

    private static void tryShowMainMenuPopup(String stage, ScriptEngine scriptEngine) {
        if (MAIN_MENU_POPUP_SHOWN.get()) {
            return;
        }

        try {
            RootScript root = scriptEngine.getRootNoCheck();
            if (root == null) {
                return;
            }

            String documentPath = root.getCurrentDocumentPath();
            if (!isMainMenuDocument(documentPath)) {
                return;
            }

            if (MAIN_MENU_POPUP_SHOWN.compareAndSet(false, true)) {
                root.showAlert(formatOverlayMessage(stage, "MainMenuPopup"));
                log("showed main menu popup from " + stage);
            }
        } catch (Throwable t) {
            MAIN_MENU_POPUP_SHOWN.set(false);
            log("main menu popup failed: " + t.getClass().getName() + ": " + t.getMessage());
        }
    }

    private static boolean isMainMenuDocument(String documentPath) {
        return documentPath != null
                && documentPath.replace('\\', '/').endsWith("mainMenu.rml");
    }

    static void log(String message) {
        System.out.println("[Rusted Fabric Example] " + message);
    }
}
