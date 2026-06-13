package io.github.endx.rustedfabricapi.api.diagnostic;

import io.github.endx.rustedfabricapi.api.util.RustedReflection;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class InputRuntimeDiagnostics {
    private static final String[] INPUT_BINDING_REGISTRY_CLASSES = {
            "rustedwarfare.input.InputBindingRegistry",
            "com.corrodinggames.rts.gameFramework.ac"
    };
    private static final String[] INPUT_ACTION_CLASSES = {
            "rustedwarfare.input.InputAction",
            "com.corrodinggames.rts.gameFramework.ad"
    };
    private static final String[] INPUT_ACTION_CATEGORY_CLASSES = {
            "rustedwarfare.input.InputActionCategory",
            "com.corrodinggames.rts.gameFramework.ae"
    };
    private static final String[] INPUT_BINDING_CLASSES = {
            "rustedwarfare.input.InputBinding",
            "com.corrodinggames.rts.gameFramework.af"
    };
    private static final String[] KEY_INPUT_BINDING_CLASSES = {
            "rustedwarfare.input.KeyInputBinding",
            "com.corrodinggames.rts.gameFramework.ag"
    };
    private static final String[] CONTROLLER_INPUT_BINDING_CLASSES = {
            "rustedwarfare.input.ControllerInputBinding",
            "com.corrodinggames.rts.gameFramework.ah"
    };
    private static final String[] INPUT_DEVICE_PROVIDER_CLASSES = {
            "rustedwarfare.input.InputDeviceProvider",
            "com.corrodinggames.rts.gameFramework.ai"
    };
    private static final String[] DESKTOP_INPUT_DEVICE_PROVIDER_CLASSES = {
            "rustedwarfare.input.DesktopInputDeviceProvider",
            "com.corrodinggames.rts.java.v"
    };
    private static final String[] UNBOUND_INPUT_BINDING_CLASSES = {
            "rustedwarfare.input.UnboundInputBinding",
            "com.corrodinggames.rts.gameFramework.ak"
    };
    private static final String[] SLICK_TO_ANDROID_KEYCODES_CLASSES = {
            "rustedwarfare.input.SlickToAndroidKeycodes",
            "com.corrodinggames.rts.gameFramework.utility.SlickToAndroidKeycodes"
    };
    private static final String[] UNIT_ACTION_CLASSES = {
            "rustedwarfare.unit.action.UnitAction",
            "com.corrodinggames.rts.game.units.a.s"
    };

    private InputRuntimeDiagnostics() {
    }

    public static Object currentInputBindingRegistry() {
        Object engine = GameEngineDiagnostics.currentEngineOrNull();
        if (engine == null) {
            return null;
        }

        try {
            Object value = RustedReflection.getFieldValue(engine, new String[]{"inputBindingRegistry", "bT"});
            if (isInputBindingRegistry(value)) {
                return value;
            }
        } catch (RuntimeException ignored) {
        }

        return firstFieldAssignableTo(engine, INPUT_BINDING_REGISTRY_CLASSES);
    }

    public static Object currentInputDeviceProvider() {
        try {
            return RustedReflection.getStaticFieldValue(INPUT_BINDING_REGISTRY_CLASSES,
                    new String[]{"inputDeviceProvider", "b"});
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    public static Object currentControllerInputHook() {
        try {
            return RustedReflection.getStaticFieldValue(INPUT_BINDING_REGISTRY_CLASSES,
                    new String[]{"controllerInputHook", "a"});
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    public static boolean isInputBindingRegistry(Object value) {
        return isAny(value, INPUT_BINDING_REGISTRY_CLASSES);
    }

    public static boolean isInputAction(Object value) {
        return isAny(value, INPUT_ACTION_CLASSES);
    }

    public static boolean isInputActionCategory(Object value) {
        return isAny(value, INPUT_ACTION_CATEGORY_CLASSES);
    }

    public static boolean isInputBinding(Object value) {
        return isAny(value, INPUT_BINDING_CLASSES);
    }

    public static boolean isKeyInputBinding(Object value) {
        return isAny(value, KEY_INPUT_BINDING_CLASSES);
    }

    public static boolean isControllerInputBinding(Object value) {
        return isAny(value, CONTROLLER_INPUT_BINDING_CLASSES);
    }

    public static boolean isInputDeviceProvider(Object value) {
        return isAny(value, INPUT_DEVICE_PROVIDER_CLASSES);
    }

    public static boolean isDesktopInputDeviceProvider(Object value) {
        return isAny(value, DESKTOP_INPUT_DEVICE_PROVIDER_CLASSES);
    }

    public static boolean isUnboundInputBinding(Object value) {
        return isAny(value, UNBOUND_INPUT_BINDING_CLASSES);
    }

    public static boolean isUnitAction(Object value) {
        return isAny(value, UNIT_ACTION_CLASSES);
    }

    public static Map<String, Object> describeCurrentInputBindingRegistry() {
        Object registry = currentInputBindingRegistry();
        return registry != null ? describeInputBindingRegistry(registry) : Collections.emptyMap();
    }

    public static Map<String, Object> describeInputBindingRegistry(Object registry) {
        requireAny(registry, INPUT_BINDING_REGISTRY_CLASSES, "InputBindingRegistry");
        List<Object> actions = inputActions(registry);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", registry.getClass().getName());
        result.put("controllerInputHook", currentControllerInputHook());
        result.put("inputDeviceProvider", currentInputDeviceProvider());
        result.put("actions", actions);
        result.put("actionsSize", Integer.valueOf(actions.size()));
        result.put("visibleActionCount", Integer.valueOf(countVisibleActions(actions)));
        result.put("categoryCount", Integer.valueOf(countCategories(actions)));
        result.put("boundActionCount", Integer.valueOf(countBoundActions(actions)));
        putField(result, registry, "keyBindingProperties", new String[]{"keyBindingProperties", "am"});
        putIntField(result, registry, "reservedKeyBindingCounter", new String[]{"reservedKeyBindingCounter", "an"});
        putIntField(result, registry, "lastControllerCount", new String[]{"lastControllerCount", "ao"});
        putField(result, registry, "shootAction", new String[]{"shootAction", "c"});
        putField(result, registry, "moveUpAction", new String[]{"moveUpAction", "d"});
        putField(result, registry, "showMenuAction", new String[]{"showMenuAction", "w"});
        putField(result, registry, "pauseGameAction", new String[]{"pauseGameAction", "L"});
        putField(result, registry, "nextMusicTrackAction", new String[]{"nextMusicTrackAction", "H"});
        putField(result, registry, "debugInvincibleUnitsAction", new String[]{"debugInvincibleUnitsAction", "Z"});
        putArrayLengthField(result, registry, "unitActionSlotActionCount", new String[]{"unitActionSlotActions", "ag"});
        putArrayLengthField(result, registry, "selectGroupActionCount", new String[]{"selectGroupActions", "ai"});
        putArrayLengthField(result, registry, "addGroupToSelectionActionCount", new String[]{"addGroupToSelectionActions", "aj"});
        putArrayLengthField(result, registry, "createGroupActionCount", new String[]{"createGroupActions", "ak"});
        return Collections.unmodifiableMap(result);
    }

    public static List<Object> inputActions(Object registry) {
        requireAny(registry, INPUT_BINDING_REGISTRY_CLASSES, "InputBindingRegistry");
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(
                RustedReflection.getFieldValue(registry, new String[]{"actions", "al"})));
    }

    public static Map<String, Object> describeInputAction(Object action) {
        requireAny(action, INPUT_ACTION_CLASSES, "InputAction");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        List<Object> bindings = actionBindings(action);
        result.put("className", action.getClass().getName());
        putStringField(result, action, "displayName", new String[]{"displayName", "a"});
        putBooleanField(result, action, "visibleInKeyBindingMenu", new String[]{"visibleInKeyBindingMenu", "b"});
        result.put("bindings", bindings);
        result.put("bindingCount", Integer.valueOf(bindings.size()));
        putCollectionSizeField(result, action, "reservedDefaultBindingCount",
                new String[]{"reservedDefaultBindings", "d"});
        result.put("category", Boolean.valueOf(isActionCategory(action)));
        result.put("configKey", invokeStringOrEmpty(action, new String[]{"getConfigKey", "e"}));
        result.put("primaryBindingDisplay", invokeStringOrEmpty(action,
                new String[]{"getPrimaryBindingDisplay", "c"}));
        result.put("slot0BindingDisplay", invokeStringOrEmpty(action,
                new String[]{"getBindingDisplay", "b"}, Integer.valueOf(0)));
        return Collections.unmodifiableMap(result);
    }

    public static List<Object> actionBindings(Object action) {
        requireAny(action, INPUT_ACTION_CLASSES, "InputAction");
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(
                RustedReflection.getFieldValue(action, new String[]{"bindings", "c"})));
    }

    public static Object getActionBinding(Object action, int slot) {
        requireAny(action, INPUT_ACTION_CLASSES, "InputAction");
        return RustedReflection.invokeInstance(action, new String[]{"getBinding", "a"}, Integer.valueOf(slot));
    }

    public static String getBindingDisplay(Object action, int slot) {
        requireAny(action, INPUT_ACTION_CLASSES, "InputAction");
        Object value = RustedReflection.invokeInstance(action, new String[]{"getBindingDisplay", "b"},
                Integer.valueOf(slot));
        return value != null ? value.toString() : "";
    }

    public static String serializeActionBindings(Object registry, Object action) {
        requireAny(registry, INPUT_BINDING_REGISTRY_CLASSES, "InputBindingRegistry");
        requireAny(action, INPUT_ACTION_CLASSES, "InputAction");
        Object value = RustedReflection.invokeInstance(registry, new String[]{"serializeActionBindings", "a"}, action);
        return value != null ? value.toString() : "";
    }

    public static boolean hasBindingConflict(Object registry, Object action, int slot) {
        requireAny(registry, INPUT_BINDING_REGISTRY_CLASSES, "InputBindingRegistry");
        requireAny(action, INPUT_ACTION_CLASSES, "InputAction");
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(registry,
                new String[]{"hasBindingConflict", "a"}, action, Integer.valueOf(slot)));
    }

    public static boolean isActionCategory(Object action) {
        requireAny(action, INPUT_ACTION_CLASSES, "InputAction");
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(action, new String[]{"isCategory", "d"}));
    }

    public static Object getInputActionForUnitAction(Object unitAction) {
        requireAny(unitAction, UNIT_ACTION_CLASSES, "UnitAction");
        return RustedReflection.invokeInstance(unitAction, new String[]{"getInputAction", "M"});
    }

    public static Map<String, Object> describeUnitActionInputBridge(Object unitAction) {
        requireAny(unitAction, UNIT_ACTION_CLASSES, "UnitAction");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", unitAction.getClass().getName());
        result.put("description", invokeStringOrEmpty(unitAction, new String[]{"getDescription", "a"}));
        result.put("text", invokeStringOrEmpty(unitAction, new String[]{"getText", "b"}));
        result.put("displayTextWithCount", invokeStringOrEmpty(unitAction,
                new String[]{"getDisplayTextWithCount", "d"}));
        result.put("actionCommandType", RustedReflection.invokeInstance(unitAction,
                new String[]{"getActionCommandType", "e"}));
        result.put("displayType", RustedReflection.invokeInstance(unitAction,
                new String[]{"getDisplayType", "f"}));
        Object inputAction = getInputActionForUnitAction(unitAction);
        result.put("inputAction", inputAction);
        result.put("hasExplicitInputAction", Boolean.valueOf(inputAction != null && isInputAction(inputAction)));
        if (inputAction != null && isInputAction(inputAction)) {
            Map<String, Object> inputActionDetails = describeInputAction(inputAction);
            result.put("inputActionDisplayName", inputActionDetails.get("displayName"));
            result.put("inputActionConfigKey", inputActionDetails.get("configKey"));
            result.put("inputActionPrimaryBindingDisplay", inputActionDetails.get("primaryBindingDisplay"));
        }
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeInputBinding(Object binding) {
        requireAny(binding, INPUT_BINDING_CLASSES, "InputBinding");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", binding.getClass().getName());
        putIntField(result, binding, "sourceId", new String[]{"sourceId", "a"});
        putIntField(result, binding, "modifierMask", new String[]{"modifierMask", "b"});
        putBooleanField(result, binding, "wasPressed", new String[]{"wasPressed", "c"});
        putBooleanField(result, binding, "userDefined", new String[]{"userDefined", "d"});
        result.put("displayName", invokeStringOrEmpty(binding, new String[]{"getDisplayName", "c"}));
        result.put("cleared", Boolean.valueOf(invokeBooleanOrFalse(binding, new String[]{"isCleared", "d"})));
        if (isKeyInputBinding(binding)) {
            putIntField(result, binding, "keyCode", new String[]{"keyCode", "e"});
        } else if (isControllerInputBinding(binding)) {
            putIntField(result, binding, "controllerIndex", new String[]{"controllerIndex", "e"});
            putIntField(result, binding, "axisIndex", new String[]{"axisIndex", "f"});
            putBooleanField(result, binding, "negativeAxisDirection", new String[]{"negativeAxisDirection", "g"});
            putIntField(result, binding, "buttonIndex", new String[]{"buttonIndex", "h"});
            putFloatField(result, binding, "neutralAxisValue", new String[]{"neutralAxisValue", "i"});
            putBooleanField(result, binding, "axisMovedFromNeutral", new String[]{"axisMovedFromNeutral", "j"});
        }
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeInputDeviceProvider(Object provider) {
        requireAny(provider, INPUT_DEVICE_PROVIDER_CLASSES, "InputDeviceProvider");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", provider.getClass().getName());
        result.put("desktopProvider", Boolean.valueOf(isDesktopInputDeviceProvider(provider)));
        result.put("controllerCount", Integer.valueOf(getControllerCount(provider)));
        return Collections.unmodifiableMap(result);
    }

    public static int getControllerCount(Object provider) {
        requireAny(provider, INPUT_DEVICE_PROVIDER_CLASSES, "InputDeviceProvider");
        Object value = RustedReflection.invokeInstance(provider, new String[]{"getControllerCount", "a"});
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    public static String getKeyDisplayName(Object provider, int keyCode, int modifierMask) {
        requireAny(provider, INPUT_DEVICE_PROVIDER_CLASSES, "InputDeviceProvider");
        Object value = RustedReflection.invokeInstance(provider, new String[]{"getKeyDisplayName", "c"},
                Integer.valueOf(keyCode), Integer.valueOf(modifierMask));
        return value != null ? value.toString() : "";
    }

    public static Map<String, Object> describeKeycodeBridge() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", className(SLICK_TO_ANDROID_KEYCODES_CLASSES));
        putStaticMapSize(result, "slickToAndroidCodesSize", new String[]{"slickToAndroidCodes", "a"});
        putStaticMapSize(result, "gdxToAndroidCodesSize", new String[]{"gdxToAndroidCodes", "b"});
        putStaticMapSize(result, "gdxToSlickCodesSize", new String[]{"gdxToSlickCodes", "c"});
        putStaticMapSize(result, "slickCodesByNameSize", new String[]{"slickCodesByName", "d"});
        putStaticMapSize(result, "androidCodesByNameSize", new String[]{"androidCodesByName", "e"});
        putStaticMapSize(result, "gdxCodesByNameSize", new String[]{"gdxCodesByName", "f"});
        putStaticMapSize(result, "androidNamesByCodeSize", new String[]{"androidNamesByCode", "g"});
        int enterCode = getAndroidKeyCodeOrZero("ENTER");
        result.put("enterAndroidCode", Integer.valueOf(enterCode));
        result.put("enterAndroidName", enterCode != 0 ? getAndroidKeyName(enterCode) : "");
        return Collections.unmodifiableMap(result);
    }

    public static int parseKeyCode(Object registry, String keyName) {
        requireAny(registry, INPUT_BINDING_REGISTRY_CLASSES, "InputBindingRegistry");
        Object value = RustedReflection.invokeInstance(registry, new String[]{"parseKeyCode", "d"}, keyName);
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    public static int getAndroidKeyCode(String keyName) {
        Object value = RustedReflection.invokeStatic(SLICK_TO_ANDROID_KEYCODES_CLASSES,
                new String[]{"getAndroidKeyCode", "a"}, keyName);
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    public static int getAndroidKeyCodeOrZero(String keyName) {
        try {
            return getAndroidKeyCode(keyName);
        } catch (RuntimeException ignored) {
            return 0;
        }
    }

    public static String getAndroidKeyName(int keyCode) {
        Object value = RustedReflection.invokeStatic(SLICK_TO_ANDROID_KEYCODES_CLASSES,
                new String[]{"getAndroidKeyName", "a"}, Integer.valueOf(keyCode));
        return value != null ? value.toString() : "";
    }

    public static int slickToAndroidKeyCode(int slickKeyCode) {
        Object value = RustedReflection.invokeStatic(SLICK_TO_ANDROID_KEYCODES_CLASSES,
                new String[]{"slickToAndroidKeyCode", "b"}, Integer.valueOf(slickKeyCode));
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    public static Integer slickToLibrocketKeyCode(int slickKeyCode) {
        Object value = RustedReflection.invokeStatic(SLICK_TO_ANDROID_KEYCODES_CLASSES,
                new String[]{"slickToLibrocketKeyCode", "c"}, Integer.valueOf(slickKeyCode));
        return value instanceof Integer ? (Integer) value : null;
    }

    private static int countVisibleActions(List<Object> actions) {
        int count = 0;
        for (Object action : actions) {
            try {
                if (RustedReflection.getBooleanField(action, new String[]{"visibleInKeyBindingMenu", "b"})) {
                    count++;
                }
            } catch (RuntimeException ignored) {
            }
        }
        return count;
    }

    private static int countCategories(List<Object> actions) {
        int count = 0;
        for (Object action : actions) {
            try {
                if (isActionCategory(action)) {
                    count++;
                }
            } catch (RuntimeException ignored) {
            }
        }
        return count;
    }

    private static int countBoundActions(List<Object> actions) {
        int count = 0;
        for (Object action : actions) {
            try {
                if (!actionBindings(action).isEmpty()) {
                    count++;
                }
            } catch (RuntimeException ignored) {
            }
        }
        return count;
    }

    private static Object firstFieldAssignableTo(Object owner, String[] classNames) {
        Class<?> expected;
        try {
            expected = RustedReflection.findClass(classNames);
        } catch (RuntimeException e) {
            return null;
        }

        Class<?> current = owner.getClass();
        while (current != null) {
            Field[] fields = current.getDeclaredFields();
            for (Field field : fields) {
                if (!expected.isAssignableFrom(field.getType())) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    return field.get(owner);
                } catch (IllegalAccessException ignored) {
                }
            }
            current = current.getSuperclass();
        }
        return null;
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

    private static String className(String[] classNames) {
        try {
            return RustedReflection.findClass(classNames).getName();
        } catch (RuntimeException e) {
            return "";
        }
    }

    private static String invokeStringOrEmpty(Object owner, String[] methodNames, Object... args) {
        try {
            Object value = RustedReflection.invokeInstance(owner, methodNames, args);
            return value != null ? value.toString() : "";
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static boolean invokeBooleanOrFalse(Object owner, String[] methodNames, Object... args) {
        try {
            return Boolean.TRUE.equals(RustedReflection.invokeInstance(owner, methodNames, args));
        } catch (RuntimeException ignored) {
            return false;
        }
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

    private static int arrayLength(Object array) {
        return array != null && array.getClass().isArray() ? java.lang.reflect.Array.getLength(array) : 0;
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

    private static void putFloatField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        try {
            result.put(key, Float.valueOf(RustedReflection.getFloatField(owner, fieldNames)));
        } catch (RuntimeException ignored) {
        }
    }

    private static void putBooleanField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        try {
            result.put(key, Boolean.valueOf(RustedReflection.getBooleanField(owner, fieldNames)));
        } catch (RuntimeException ignored) {
        }
    }

    private static void putArrayLengthField(Map<String, Object> result, Object owner, String key,
                                            String[] fieldNames) {
        try {
            result.put(key, Integer.valueOf(arrayLength(RustedReflection.getFieldValue(owner, fieldNames))));
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

    private static void putStaticMapSize(Map<String, Object> result, String key, String[] fieldNames) {
        try {
            result.put(key, Integer.valueOf(collectionSize(RustedReflection.getStaticFieldValue(
                    SLICK_TO_ANDROID_KEYCODES_CLASSES, fieldNames))));
        } catch (RuntimeException ignored) {
        }
    }
}
