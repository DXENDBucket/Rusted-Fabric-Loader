package io.github.endx.rustedfabricapi.api.diagnostic;

import io.github.endx.rustedfabricapi.api.util.RustedReflection;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LibRocketUiDiagnostics {
    private static final String[] LIBROCKET_UI_CONTROLLER_CLASSES = {
            "rustedwarfare.ui.LibRocketUiController",
            "com.corrodinggames.librocket.a"
    };
    private static final String[] LIBROCKET_UI_ENGINE_CLASSES = {
            "rustedwarfare.ui.LibRocketUiEngine",
            "com.corrodinggames.librocket.b"
    };
    private static final String[] POPUP_DOCUMENT_DATA_CLASSES = {
            "rustedwarfare.ui.PopupDocumentData",
            "com.corrodinggames.librocket.d"
    };
    private static final String[] POPUP_BUTTON_CLASSES = {
            "rustedwarfare.ui.PopupButton",
            "com.corrodinggames.librocket.e"
    };
    private static final String[] SCRIPT_ENGINE_CLASSES = {
            "rustedwarfare.ui.script.ScriptEngine",
            "com.corrodinggames.librocket.scripts.ScriptEngine"
    };
    private static final String[] ROOT_SCRIPT_CLASSES = {
            "rustedwarfare.ui.script.RootScript",
            "com.corrodinggames.librocket.scripts.Root"
    };
    private static final String[] MODS_SCRIPT_CLASSES = {
            "rustedwarfare.ui.script.ModsScript",
            "com.corrodinggames.librocket.scripts.Mods"
    };
    private static final String[] MULTIPLAYER_SCRIPT_CLASSES = {
            "rustedwarfare.ui.script.MultiplayerScript",
            "com.corrodinggames.librocket.scripts.Multiplayer"
    };
    private static final String[] DEBUG_SCRIPT_CLASSES = {
            "rustedwarfare.ui.script.DebugScript",
            "com.corrodinggames.librocket.scripts.Debug"
    };
    private static final String[] SCRIPT_ACTION_CLASSES = {
            "rustedwarfare.ui.script.ScriptEngine$Action",
            "com.corrodinggames.librocket.scripts.ScriptEngine$Action"
    };

    private LibRocketUiDiagnostics() {
    }

    public static Object currentUiController() {
        try {
            return RustedReflection.getStaticFieldValue(LIBROCKET_UI_CONTROLLER_CLASSES,
                    new String[]{"instance", "a"});
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    public static Object currentUiEngine() {
        Object controller = currentUiController();
        if (controller == null) {
            return null;
        }

        try {
            Object value = RustedReflection.getFieldValue(controller, new String[]{"libRocket", "b"});
            return isLibRocketUiEngine(value) ? value : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    public static Object currentScriptEngine() {
        Object uiEngine = currentUiEngine();
        if (uiEngine == null) {
            return null;
        }

        try {
            Object value = RustedReflection.getFieldValue(uiEngine, new String[]{"scriptEngine", "c"});
            return isScriptEngine(value) ? value : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    public static Object currentRootScript() {
        Object scriptEngine = currentScriptEngine();
        return scriptEngine != null ? rootScript(scriptEngine) : null;
    }

    public static Object currentModsScript() {
        Object root = currentRootScript();
        return root != null ? modsScript(root) : null;
    }

    public static Object currentMultiplayerScript() {
        Object root = currentRootScript();
        return root != null ? multiplayerScript(root) : null;
    }

    public static Object currentDebugScript() {
        Object scriptEngine = currentScriptEngine();
        return scriptEngine != null ? debugScript(scriptEngine) : null;
    }

    public static boolean isLibRocketUiController(Object value) {
        return isAny(value, LIBROCKET_UI_CONTROLLER_CLASSES);
    }

    public static boolean isLibRocketUiEngine(Object value) {
        return isAny(value, LIBROCKET_UI_ENGINE_CLASSES);
    }

    public static boolean isPopupDocumentData(Object value) {
        return isAny(value, POPUP_DOCUMENT_DATA_CLASSES);
    }

    public static boolean isPopupButton(Object value) {
        return isAny(value, POPUP_BUTTON_CLASSES);
    }

    public static boolean isScriptEngine(Object value) {
        return isAny(value, SCRIPT_ENGINE_CLASSES);
    }

    public static boolean isRootScript(Object value) {
        return isAny(value, ROOT_SCRIPT_CLASSES);
    }

    public static boolean isModsScript(Object value) {
        return isAny(value, MODS_SCRIPT_CLASSES);
    }

    public static boolean isMultiplayerScript(Object value) {
        return isAny(value, MULTIPLAYER_SCRIPT_CLASSES);
    }

    public static boolean isDebugScript(Object value) {
        return isAny(value, DEBUG_SCRIPT_CLASSES);
    }

    public static boolean isScriptAction(Object value) {
        return isAny(value, SCRIPT_ACTION_CLASSES);
    }

    public static Map<String, Object> describeCurrentUiController() {
        Object controller = currentUiController();
        return controller != null ? describeUiController(controller) : Collections.emptyMap();
    }

    public static Map<String, Object> describeUiController(Object controller) {
        requireAny(controller, LIBROCKET_UI_CONTROLLER_CLASSES, "LibRocketUiController");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", controller.getClass().getName());
        putField(result, controller, "libRocket", new String[]{"libRocket", "b"});
        putField(result, controller, "appFramework", new String[]{"appFramework", "c"});
        putBooleanField(result, controller, "resumeGameFlag", new String[]{"resumeGameFlag", "e"});
        result.put("uiOpen", invokeBooleanOrFalse(controller, new String[]{"isUiOpen", "j"}));
        result.put("keyModifierState", Integer.valueOf(invokeIntOrZero(controller,
                new String[]{"getKeyModifierState", "i"})));
        result.put("gameLogLineCount", Integer.valueOf(collectionSize(invokeOrNull(controller,
                new String[]{"getGameLogLines", "k"}))));
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeCurrentUiEngine() {
        Object uiEngine = currentUiEngine();
        return uiEngine != null ? describeUiEngine(uiEngine) : Collections.emptyMap();
    }

    public static Map<String, Object> describeUiEngine(Object uiEngine) {
        requireAny(uiEngine, LIBROCKET_UI_ENGINE_CLASSES, "LibRocketUiEngine");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", uiEngine.getClass().getName());
        putStringField(result, uiEngine, "guiBasePath", new String[]{"guiBasePath", "b"});
        putField(result, uiEngine, "scriptEngine", new String[]{"scriptEngine", "c"});
        putIntField(result, uiEngine, "frameTextureCounter", new String[]{"frameTextureCounter", "d"});
        putBooleanField(result, uiEngine, "insideEvent", new String[]{"insideEvent", "e"});
        putBooleanField(result, uiEngine, "scissorEnabled", new String[]{"scissorEnabled", "h"});
        putField(result, uiEngine, "alertDocumentData", new String[]{"alertDocumentData", "j"});
        putField(result, uiEngine, "popupDocumentData", new String[]{"popupDocumentData", "k"});
        result.put("alertDocument", invokeOrNull(uiEngine, new String[]{"getAlertDocument", "d"}));
        result.put("popupDocument", invokeOrNull(uiEngine, new String[]{"getPopupDocument", "c"}));
        result.put("topmostDocument", invokeOrNull(uiEngine, new String[]{"getTopmostDocument", "g"}));
        result.put("noDocumentOrPopupActive", Boolean.valueOf(invokeBooleanOrFalse(uiEngine,
                new String[]{"isNoDocumentOrPopupActive", "b"})));
        return Collections.unmodifiableMap(result);
    }

    public static void loadCharsetIfNeededOnChildren(Object uiEngine, Object element, boolean includeOptions) {
        requireAny(uiEngine, LIBROCKET_UI_ENGINE_CLASSES, "LibRocketUiEngine");
        if (element == null) {
            throw new IllegalArgumentException("element must not be null");
        }
        RustedReflection.invokeInstance(uiEngine, new String[]{"loadCharsetIfNeededOnChildren", "a"},
                element, Boolean.valueOf(includeOptions));
    }

    public static Map<String, Object> describePopupDocumentData(Object data) {
        requireAny(data, POPUP_DOCUMENT_DATA_CLASSES, "PopupDocumentData");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", data.getClass().getName());
        putField(result, data, "document", new String[]{"document", "a"});
        putStringField(result, data, "title", new String[]{"title", "b"});
        putStringField(result, data, "message", new String[]{"message", "c"});
        putStringField(result, data, "inputDefaultValue", new String[]{"inputDefaultValue", "d"});
        putField(result, data, "button1", new String[]{"button1", "e"});
        putField(result, data, "button2", new String[]{"button2", "f"});
        putBooleanField(result, data, "showImmediately", new String[]{"showImmediately", "g"});
        putBooleanField(result, data, "showBackButton", new String[]{"showBackButton", "h"});
        putField(result, data, "onClose", new String[]{"onClose", "i"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describePopupButton(Object button) {
        requireAny(button, POPUP_BUTTON_CLASSES, "PopupButton");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", button.getClass().getName());
        putStringField(result, button, "label", new String[]{"label", "a"});
        putField(result, button, "runnable", new String[]{"runnable", "b"});
        putBooleanField(result, button, "bindEnterKey", new String[]{"bindEnterKey", "c"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeCurrentScriptEngine() {
        Object scriptEngine = currentScriptEngine();
        return scriptEngine != null ? describeScriptEngine(scriptEngine) : Collections.emptyMap();
    }

    public static Map<String, Object> describeScriptEngine(Object scriptEngine) {
        requireAny(scriptEngine, SCRIPT_ENGINE_CLASSES, "ScriptEngine");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", scriptEngine.getClass().getName());
        putMapSizeField(result, scriptEngine, "globalsSize", new String[]{"globals", "globals"});
        putCollectionSizeField(result, scriptEngine, "queuedScriptsSize", new String[]{"queuedScripts", "queuedScripts"});
        putCollectionSizeField(result, scriptEngine, "runningScriptsSize", new String[]{"runningScripts", "runningScripts"});
        putBooleanField(result, scriptEngine, "inDebugScript", new String[]{"inDebugScript", "inDebugScript"});
        putBooleanField(result, scriptEngine, "mainScriptThreadMarked",
                new String[]{"mainScriptThreadMarked", "mainScriptThreadMarked"});
        putField(result, scriptEngine, "root", new String[]{"root", "root"});
        putField(result, scriptEngine, "scriptError", new String[]{"scriptError", "scriptError"});
        putStringField(result, scriptEngine, "scriptErrorMessage",
                new String[]{"scriptErrorMessage", "scriptErrorMessage"});
        putField(result, scriptEngine, "slickLibRocket", new String[]{"slickLibRocket", "slickLibRocket"});
        result.put("strict", Boolean.valueOf(invokeBooleanOrFalse(scriptEngine, new String[]{"isStrict", "isStrict"})));
        Object debug = debugScript(scriptEngine);
        result.put("debugScript", debug);
        return Collections.unmodifiableMap(result);
    }

    public static Object rootScript(Object scriptEngine) {
        requireAny(scriptEngine, SCRIPT_ENGINE_CLASSES, "ScriptEngine");
        return fieldValueOrNull(scriptEngine, new String[]{"root", "root"});
    }

    public static Object debugScript(Object scriptEngine) {
        requireAny(scriptEngine, SCRIPT_ENGINE_CLASSES, "ScriptEngine");
        Object value = scriptGlobal(scriptEngine, "debug");
        if (isDebugScript(value)) {
            return value;
        }
        value = scriptGlobal(scriptEngine, "Debug");
        return isDebugScript(value) ? value : null;
    }

    public static Object scriptGlobal(Object scriptEngine, String key) {
        requireAny(scriptEngine, SCRIPT_ENGINE_CLASSES, "ScriptEngine");
        Object globals = fieldValueOrNull(scriptEngine, new String[]{"globals", "globals"});
        if (globals instanceof Map<?, ?>) {
            return ((Map<?, ?>) globals).get(key);
        }
        return null;
    }

    public static Map<String, Object> describeCurrentRootScript() {
        Object root = currentRootScript();
        return root != null ? describeRootScript(root) : Collections.emptyMap();
    }

    public static Map<String, Object> describeRootScript(Object root) {
        requireAny(root, ROOT_SCRIPT_CLASSES, "RootScript");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", root.getClass().getName());
        putField(result, root, "mods", new String[]{"mods", "mods"});
        putField(result, root, "multiplayer", new String[]{"multiplayer", "multiplayer"});
        putField(result, root, "lastConnectingPopup", new String[]{"lastConnectingPopup", "lastConnectingPopup"});
        putField(result, root, "threadedGameConnector", new String[]{"threadedGameConnector", "threadedGameConnector"});
        putCollectionSizeField(result, root, "lastSortedDiscoveredServersSize",
                new String[]{"lastSortedDiscoveredServers", "lastSortedDiscoveredServers"});
        result.put("currentDocumentPath", invokeStringOrEmpty(root,
                new String[]{"getCurrentDocumentPath", "getCurrentDocumentPath"}));
        result.put("currentPopupPath", invokeStringOrEmpty(root,
                new String[]{"getCurrentPopupPath", "getCurrentPopupPath"}));
        result.put("versionName", invokeStringOrEmpty(root, new String[]{"getVersionName", "getVersionName"}));
        result.put("desktop", Boolean.valueOf(invokeBooleanOrFalse(root, new String[]{"isDesktop", "isDesktop"})));
        result.put("mobile", Boolean.valueOf(invokeBooleanOrFalse(root, new String[]{"isMobile", "isMobile"})));
        result.put("modSupport", Boolean.valueOf(invokeBooleanOrFalse(root,
                new String[]{"hasModSupport", "hasModSupport"})));
        result.put("workshopSupport", Boolean.valueOf(invokeBooleanOrFalse(root,
                new String[]{"hasWorkshopSupport", "hasWorkshopSupport"})));
        return Collections.unmodifiableMap(result);
    }

    public static Object modsScript(Object root) {
        requireAny(root, ROOT_SCRIPT_CLASSES, "RootScript");
        return fieldValueOrNull(root, new String[]{"mods", "mods"});
    }

    public static Object multiplayerScript(Object root) {
        requireAny(root, ROOT_SCRIPT_CLASSES, "RootScript");
        return fieldValueOrNull(root, new String[]{"multiplayer", "multiplayer"});
    }

    public static Map<String, Object> describeModsScript(Object mods) {
        requireAny(mods, MODS_SCRIPT_CLASSES, "ModsScript");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", mods.getClass().getName());
        putIntField(result, mods, "checkWorkshopSkip", new String[]{"checkWorkshopSkip", "checkWorkshopSkip"});
        putField(result, mods, "root", new String[]{"root", "root"});
        putField(result, mods, "updateModsRunnable", new String[]{"updateModsRunnable", "updateModsRunnable"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeMultiplayerScript(Object multiplayer) {
        requireAny(multiplayer, MULTIPLAYER_SCRIPT_CLASSES, "MultiplayerScript");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", multiplayer.getClass().getName());
        putArrayLengthField(result, multiplayer, "currentDropdownRawArrayLength",
                new String[]{"currentDropdownRawArray", "currentDropdownRawArray"});
        putField(result, multiplayer, "lastPlayerTable", new String[]{"lastPlayerTable", "lastPlayerTable"});
        putField(result, multiplayer, "root", new String[]{"root", "root"});
        putBooleanField(result, multiplayer, "useMapDropdown", new String[]{"useMapDropdown", "useMapDropdown"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeCurrentDebugScript() {
        Object debug = currentDebugScript();
        return debug != null ? describeDebugScript(debug) : Collections.emptyMap();
    }

    public static Map<String, Object> describeDebugScript(Object debug) {
        requireAny(debug, DEBUG_SCRIPT_CLASSES, "DebugScript");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", debug.getClass().getName());
        putField(result, debug, "root", new String[]{"root", "root"});
        putBooleanField(result, debug, "allFeatures", new String[]{"allFeatures", "allFeatures"});
        putCollectionSizeField(result, debug, "backgroundClientConnectionsSize",
                new String[]{"backgroundClientConnections", "backgroundClientConnections"});
        putField(result, debug, "backgroundConnectionThread",
                new String[]{"backgroundConnectionThread", "backgroundConnectionThread"});
        putField(result, debug, "backgroundConnectionRunnable",
                new String[]{"backgroundConnectionRunnable", "backgroundConnectionRunnable"});
        putBooleanField(result, debug, "forceNonThreaded", new String[]{"forceNonThreaded", "forceNonThreaded"});
        result.put("currentPid", Integer.valueOf(invokeIntOrZero(debug, new String[]{"currentPid", "currentPid"})));
        result.put("networkGameActive", Boolean.valueOf(invokeBooleanOrFalse(debug,
                new String[]{"isNetworkGameActive", "isNetworkGameActive"})));
        result.put("localPlayerId", Integer.valueOf(invokeIntOrZero(debug,
                new String[]{"getLocalPlayerId", "getLocalPlayerId"})));
        result.put("humanPlayers", Integer.valueOf(invokeIntOrZero(debug,
                new String[]{"numberOfHumanPlayers", "numberOfHumanPlayers"})));
        result.put("playersPlusAi", Integer.valueOf(invokeIntOrZero(debug,
                new String[]{"numberOfPlayersPlusAI", "numberOfPlayersPlusAI"})));
        result.put("playerConnections", Integer.valueOf(invokeIntOrZero(debug,
                new String[]{"numberOfPlayerConnections", "numberOfPlayerConnections"})));
        result.put("desyncErrors", Integer.valueOf(invokeIntOrZero(debug,
                new String[]{"getNumberOfDesyncErrors", "getNumberOfDesyncErrors"})));
        result.put("desyncPasses", Integer.valueOf(invokeIntOrZero(debug,
                new String[]{"getNumberOfDesyncPasses", "getNumberOfDesyncPasses"})));
        result.put("resyncSendsOrRecv", Integer.valueOf(invokeIntOrZero(debug,
                new String[]{"getNumberOfResyncSendsOrRecv", "getNumberOfResyncSendsOrRecv"})));
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeScriptAction(Object action) {
        requireAny(action, SCRIPT_ACTION_CLASSES, "ScriptEngine.Action");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", action.getClass().getName());
        putStringField(result, action, "script", new String[]{"script", "script"});
        putStringField(result, action, "caughtCrash", new String[]{"caughtCrash", "caughtCrash"});
        putBooleanField(result, action, "completed", new String[]{"completed", "completed"});
        putBooleanField(result, action, "tryToCatchCrash", new String[]{"tryToCatchCrash", "tryToCatchCrash"});
        putIntField(result, action, "framesDelay", new String[]{"framesDelay", "framesDelay"});
        return Collections.unmodifiableMap(result);
    }

    private static boolean isAny(Object value, String[] classNames) {
        return value != null && RustedReflection.isAnyClass(value.getClass(), classNames);
    }

    private static void requireAny(Object value, String[] classNames, String label) {
        if (value == null || !RustedReflection.isAnyClass(value.getClass(), classNames)) {
            throw new IllegalArgumentException("Expected " + label + ", got " + describe(value));
        }
    }

    private static String describe(Object value) {
        return value == null ? "null" : value.getClass().getName();
    }

    private static Object fieldValueOrNull(Object owner, String[] fieldNames) {
        try {
            return RustedReflection.getFieldValue(owner, fieldNames);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static Object invokeOrNull(Object owner, String[] methodNames, Object... args) {
        try {
            return RustedReflection.invokeInstance(owner, methodNames, args);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String invokeStringOrEmpty(Object owner, String[] methodNames, Object... args) {
        Object value = invokeOrNull(owner, methodNames, args);
        return value != null ? value.toString() : "";
    }

    private static int invokeIntOrZero(Object owner, String[] methodNames, Object... args) {
        Object value = invokeOrNull(owner, methodNames, args);
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    private static boolean invokeBooleanOrFalse(Object owner, String[] methodNames, Object... args) {
        return Boolean.TRUE.equals(invokeOrNull(owner, methodNames, args));
    }

    private static int collectionSize(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof java.util.Collection<?>) {
            return ((java.util.Collection<?>) value).size();
        }
        if (value instanceof Map<?, ?>) {
            return ((Map<?, ?>) value).size();
        }
        if (value.getClass().isArray()) {
            return java.lang.reflect.Array.getLength(value);
        }
        return 1;
    }

    private static void putField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        try {
            result.put(key, RustedReflection.getFieldValue(owner, fieldNames));
        } catch (RuntimeException ignored) {
        }
    }

    private static void putStringField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        try {
            result.put(key, RustedReflection.getStringField(owner, fieldNames));
        } catch (RuntimeException ignored) {
        }
    }

    private static void putIntField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        try {
            result.put(key, Integer.valueOf(RustedReflection.getIntField(owner, fieldNames)));
        } catch (RuntimeException ignored) {
        }
    }

    private static void putBooleanField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        try {
            result.put(key, Boolean.valueOf(RustedReflection.getBooleanField(owner, fieldNames)));
        } catch (RuntimeException ignored) {
        }
    }

    private static void putCollectionSizeField(Map<String, Object> result, Object owner, String key,
                                               String[] fieldNames) {
        try {
            result.put(key, Integer.valueOf(collectionSize(RustedReflection.getFieldValue(owner, fieldNames))));
        } catch (RuntimeException ignored) {
        }
    }

    private static void putMapSizeField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        try {
            Object value = RustedReflection.getFieldValue(owner, fieldNames);
            result.put(key, Integer.valueOf(value instanceof Map<?, ?> ? ((Map<?, ?>) value).size() : 0));
        } catch (RuntimeException ignored) {
        }
    }

    private static void putArrayLengthField(Map<String, Object> result, Object owner, String key,
                                            String[] fieldNames) {
        try {
            Object array = RustedReflection.getFieldValue(owner, fieldNames);
            result.put(key, Integer.valueOf(array != null && array.getClass().isArray()
                    ? java.lang.reflect.Array.getLength(array) : 0));
        } catch (RuntimeException ignored) {
        }
    }
}
