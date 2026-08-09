package io.github.endx.rustedfabricapi.api.logic;

import io.github.endx.rustedfabricapi.api.util.RustedReflection;
import io.github.endx.rustedfabricapi.impl.logic.RegisteredLogicNumberFunction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Registry for numeric functions parsed by the game's runtime LogicBoolean language. */
public final class LogicNumberFunctions {
    private static final String[] LOGIC_BOOLEAN_CLASSES = {
            "rustedwarfare.custom.logic.LogicBoolean",
            "com.corrodinggames.rts.game.units.custom.logicBooleans.LogicBoolean"
    };
    private static final Object LOCK = new Object();
    private static final Map<String, LogicNumberFunctionDefinition> DEFINITIONS =
            new LinkedHashMap<String, LogicNumberFunctionDefinition>();

    private LogicNumberFunctions() { }

    public static void register(LogicNumberFunctionDefinition definition) {
        LogicNumberFunctionDefinition checked = Objects.requireNonNull(definition, "definition");
        synchronized (LOCK) {
            if (DEFINITIONS.containsKey(checked.name())) {
                throw new IllegalArgumentException("duplicate LogicBoolean function: " + checked.name());
            }
            RegisteredLogicNumberFunction prototype = new RegisteredLogicNumberFunction(checked);
            RustedReflection.invokeStatic(LOGIC_BOOLEAN_CLASSES, new String[]{"addBooleanType"},
                    prototype, new String[]{checked.name()});
            DEFINITIONS.put(checked.name(), checked);
        }
    }

    public static boolean isRegistered(String name) {
        if (name == null) return false;
        synchronized (LOCK) { return DEFINITIONS.containsKey(name.trim().toLowerCase(java.util.Locale.ROOT)); }
    }

    public static List<LogicNumberFunctionDefinition> definitions() {
        synchronized (LOCK) {
            return Collections.unmodifiableList(
                    new ArrayList<LogicNumberFunctionDefinition>(DEFINITIONS.values()));
        }
    }
}
