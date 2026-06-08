package io.github.endx.rustedfabricexample;

import io.github.endx.rustedfabricapi.api.event.GameLifecycleEvents;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import rustedwarfare.core.GameEngine;
import rustedwarfare.core.SettingsEngine;
import rustedwarfare.mod.ModManager;
import rustedwarfare.ui.MessageLine;
import rustedwarfare.ui.script.RootScript;
import rustedwarfare.ui.script.ScriptEngine;

import java.util.concurrent.atomic.AtomicBoolean;

public final class ExampleMod implements ModInitializer, ClientModInitializer {
    public static final String MOD_ID = "rusted_fabric_example";
    private static final AtomicBoolean VISIBLE_SETTINGS_TWEAKS_STARTED = new AtomicBoolean();
    private static final AtomicBoolean MAIN_MENU_POPUP_STARTED = new AtomicBoolean();
    private static final AtomicBoolean MAIN_MENU_POPUP_SHOWN = new AtomicBoolean();
    private static final AtomicBoolean MAP_ENTRY_MESSAGE_REGISTERED = new AtomicBoolean();
    private static final Object MAP_ENTRY_MESSAGE_LOCK = new Object();
    private static final int MAP_ENTRY_MESSAGE_COLOR = 0xFF80D8FF;
    private static String lastMapEntryMessageMapPath;
    private static long lastMapEntryMessageMillis;

    @Override
    public void onInitialize() {
        log("main entrypoint");
        logNamedGameTypes("main");
        startVisibleSettingsTweaks("main");
        startMainMenuPopup("main");
        registerMapEntryMessage("main");
    }

    @Override
    public void onInitializeClient() {
        log("client entrypoint");
        logNamedGameTypes("client");
        startVisibleSettingsTweaks("client");
        startMainMenuPopup("client");
        registerMapEntryMessage("client");
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

    static void startMainMenuPopup(String stage) {
        if (!MAIN_MENU_POPUP_STARTED.compareAndSet(false, true)) {
            return;
        }

        Thread thread = new Thread(() -> waitForMainMenuAndShowPopup(stage), "Rusted Fabric Example Main Menu Popup");
        thread.setDaemon(true);
        thread.start();
    }

    static void registerMapEntryMessage(String stage) {
        if (!MAP_ENTRY_MESSAGE_REGISTERED.compareAndSet(false, true)) {
            return;
        }

        GameLifecycleEvents.AFTER_MAP_SETUP.register((minimap, map, fogEnabled) -> showMapEntryMessage(stage, map));
        log("registered map entry message hook from " + stage);
    }

    private static void waitForSettingsAndApplyTweaks(String stage) {
        Throwable lastFailure = null;
        boolean settingsApplied = false;
        boolean settingsTimeoutLogged = false;

        for (int attempt = 1; !settingsApplied; attempt++) {
            try {
                GameEngine engine = GameEngine.getInstance();
                if (engine != null && engine.settings != null) {
                    applyVisibleSettingsTweaks(stage, engine.settings);
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
                root.showAlert("Example Text");
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

    private static void showMapEntryMessage(String stage, Object map) {
        if (tryShowMapEntryMessage(stage, map)) {
            return;
        }

        Thread thread = new Thread(() -> waitForMessageInterfaceAndShowMapEntryMessage(stage, map),
                "Rusted Fabric Example Map Entry Message");
        thread.setDaemon(true);
        thread.start();
    }

    private static void waitForMessageInterfaceAndShowMapEntryMessage(String stage, Object map) {
        for (int attempt = 1; attempt <= 40; attempt++) {
            if (tryShowMapEntryMessage(stage, map)) {
                return;
            }

            try {
                Thread.sleep(250L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log("map entry message retry interrupted before MessageInterface was ready");
                return;
            }
        }

        log("map entry message skipped because MessageInterface was not ready");
    }

    private static boolean tryShowMapEntryMessage(String stage, Object map) {
        if (!isMessageInterfaceReady()) {
            return false;
        }

        String mapPath = getCurrentMapPathForLog();
        if (isDuplicateMapEntryMessage(mapPath)) {
            return true;
        }

        try {
            GameEngine engine = GameEngine.getInstance();
            if (engine == null || engine.interfaceEngine == null) {
                return false;
            }

            boolean colored = false;
            if (engine.interfaceEngine.messageInterface != null) {
                MessageLine line = engine.interfaceEngine.messageInterface.addMessage(null, "Example Text");
                if (line != null) {
                    line.textColor = MAP_ENTRY_MESSAGE_COLOR;
                    colored = true;
                }
            } else if (engine.interfaceEngine.warLogInterface != null) {
                engine.interfaceEngine.warLogInterface.addTextEntry("Example Text");
            }
            log("showed map entry message from " + stage
                    + ", mapPath=" + (mapPath != null ? mapPath : "unknown")
                    + ", messageInterface=" + (engine.interfaceEngine.messageInterface != null)
                    + ", warLogInterface=" + (engine.interfaceEngine.warLogInterface != null)
                    + ", colored=" + colored
                    + ", map=" + (map != null ? map.getClass().getName() : "null"));
            return true;
        } catch (Throwable t) {
            clearDuplicateMapEntryMessage(mapPath);
            log("map entry message failed: " + t.getClass().getName() + ": " + t.getMessage());
            return false;
        }
    }

    private static boolean isMessageInterfaceReady() {
        try {
            GameEngine engine = GameEngine.getInstance();
            return engine != null
                    && engine.interfaceEngine != null
                    && (engine.interfaceEngine.messageInterface != null
                    || engine.interfaceEngine.warLogInterface != null);
        } catch (Throwable t) {
            return false;
        }
    }

    private static String getCurrentMapPathForLog() {
        try {
            GameEngine engine = GameEngine.getInstance();
            if (engine == null) {
                return null;
            }

            String mapPath = engine.getCurrentMapPath();
            if (mapPath == null || mapPath.trim().isEmpty()) {
                mapPath = engine.currentMapPath;
            }
            return mapPath;
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean isDuplicateMapEntryMessage(String mapPath) {
        long now = System.currentTimeMillis();
        String key = mapPath != null ? mapPath : "<unknown>";
        synchronized (MAP_ENTRY_MESSAGE_LOCK) {
            if (key.equals(lastMapEntryMessageMapPath) && now - lastMapEntryMessageMillis < 1500L) {
                return true;
            }

            lastMapEntryMessageMapPath = key;
            lastMapEntryMessageMillis = now;
            return false;
        }
    }

    private static void clearDuplicateMapEntryMessage(String mapPath) {
        String key = mapPath != null ? mapPath : "<unknown>";
        synchronized (MAP_ENTRY_MESSAGE_LOCK) {
            if (key.equals(lastMapEntryMessageMapPath)) {
                lastMapEntryMessageMapPath = null;
                lastMapEntryMessageMillis = 0L;
            }
        }
    }

    static void log(String message) {
        System.out.println("[Rusted Fabric Example] " + message);
    }
}
