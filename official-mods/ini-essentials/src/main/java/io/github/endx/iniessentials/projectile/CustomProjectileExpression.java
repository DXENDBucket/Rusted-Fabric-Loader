package io.github.endx.iniessentials.projectile;

import io.github.endx.iniessentials.BooleanExpression;
import io.github.endx.iniessentials.NumericExpression;
import io.github.endx.rustedfabricapi.api.logic.LogicNumberFunctionDefinition;
import io.github.endx.rustedfabricapi.api.logic.LogicNumberFunctions;
import rustedwarfare.unit.OrderableUnit;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Bridges projectile-local state into the native LogicBoolean parser. */
final class CustomProjectileExpression {
    private static final ThreadLocal<CustomProjectileState> CURRENT =
            new ThreadLocal<CustomProjectileState>();
    private static final Pattern LOCAL_MEMORY = Pattern.compile(
            "(?i)(?<![a-z0-9_])memory\\.([a-z_][a-z0-9_]*)");
    private static final Map<String, String> PROJECTILE_VALUES = new LinkedHashMap<String, String>();

    static {
        PROJECTILE_VALUES.put("projectile.age", "cpstate_age");
        PROJECTILE_VALUES.put("projectile.x", "cpstate_x");
        PROJECTILE_VALUES.put("projectile.y", "cpstate_y");
        PROJECTILE_VALUES.put("projectile.height", "cpstate_height");
        PROJECTILE_VALUES.put("projectile.direction", "cpstate_direction");
        PROJECTILE_VALUES.put("projectile.speed", "cpstate_speed");
        PROJECTILE_VALUES.put("projectile.dx", "cpstate_dx");
        PROJECTILE_VALUES.put("projectile.dy", "cpstate_dy");
        PROJECTILE_VALUES.put("projectile.offsetx", "cpstate_offsetx");
        PROJECTILE_VALUES.put("projectile.offsety", "cpstate_offsety");
    }

    private CustomProjectileExpression() { }

    static MemorySchema schema(String definitionId, Map<String, MemoryType> declarations) {
        registerProjectileFunctions();
        String prefix = "cpmem_" + digest(definitionId).substring(0, 12) + "_";
        LinkedHashMap<String, MemoryVariable> variables = new LinkedHashMap<String, MemoryVariable>();
        for (Map.Entry<String, MemoryType> entry : declarations.entrySet()) {
            String name = entry.getKey().toLowerCase(Locale.ROOT);
            String function = prefix + name;
            if (!LogicNumberFunctions.isRegistered(function)) {
                LogicNumberFunctions.register(LogicNumberFunctionDefinition.of(
                        function, 0, ignored -> currentValue(name)));
            }
            variables.put(name, new MemoryVariable(entry.getValue(), function));
        }
        return new MemorySchema(variables);
    }

    static Numeric compileNumber(Object metadata, MemorySchema schema, String source) {
        return new Numeric(NumericExpression.compile(metadata, rewrite(source, schema, false)));
    }

    static Condition compileBoolean(Object metadata, MemorySchema schema, String source) {
        return new Condition(BooleanExpression.compile(metadata, rewrite(source, schema, true)));
    }

    private static String rewrite(String source, MemorySchema schema, boolean booleanContext) {
        String result = source != null ? source.trim() : "";
        Matcher matcher = LOCAL_MEMORY.matcher(result);
        StringBuffer replaced = new StringBuffer();
        while (matcher.find()) {
            String name = matcher.group(1).toLowerCase(Locale.ROOT);
            MemoryVariable variable = schema.variables.get(name);
            if (variable == null) continue;
            String value = variable.function + "()";
            if (booleanContext && variable.type == MemoryType.BOOLEAN) value = "(" + value + ">0)";
            matcher.appendReplacement(replaced, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(replaced);
        result = replaced.toString();
        for (Map.Entry<String, String> entry : PROJECTILE_VALUES.entrySet()) {
            result = result.replaceAll("(?i)(?<![a-z0-9_])"
                    + Pattern.quote(entry.getKey()) + "(?![a-z0-9_])", entry.getValue() + "()");
        }
        return result;
    }

    private static void registerProjectileFunctions() {
        registerStateFunction("cpstate_age", state -> state.projectile.ageTimer);
        registerStateFunction("cpstate_x", state -> state.projectile.x);
        registerStateFunction("cpstate_y", state -> state.projectile.y);
        registerStateFunction("cpstate_height", state -> state.projectile.height);
        registerStateFunction("cpstate_direction", state -> state.projectile.direction);
        registerStateFunction("cpstate_speed", state -> state.projectile.speed);
        registerStateFunction("cpstate_dx", state -> state.projectile.initialUnguidedSpeedX);
        registerStateFunction("cpstate_dy", state -> state.projectile.initialUnguidedSpeedY);
        registerStateFunction("cpstate_offsetx", state -> state.projectile.x - state.originX);
        registerStateFunction("cpstate_offsety", state -> state.projectile.y - state.originY);
    }

    private static void registerStateFunction(String name, StateReader reader) {
        if (LogicNumberFunctions.isRegistered(name)) return;
        LogicNumberFunctions.register(LogicNumberFunctionDefinition.of(name, 0, ignored -> {
            CustomProjectileState state = CURRENT.get();
            return state != null ? reader.read(state) : 0.0F;
        }));
    }

    private static float currentValue(String name) {
        CustomProjectileState state = CURRENT.get();
        return state != null ? state.memory(name) : 0.0F;
    }

    private static <T> T withState(CustomProjectileState state, Evaluation<T> evaluation) {
        CustomProjectileState previous = CURRENT.get();
        CURRENT.set(state);
        try {
            return evaluation.evaluate();
        } finally {
            if (previous != null) CURRENT.set(previous); else CURRENT.remove();
        }
    }

    private static String digest(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte item : bytes) result.append(String.format(Locale.ROOT, "%02x", item & 255));
            return result.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    enum MemoryType { NUMBER, BOOLEAN }

    static final class MemorySchema {
        private final Map<String, MemoryVariable> variables;

        private MemorySchema(Map<String, MemoryVariable> variables) {
            this.variables = variables;
        }

        boolean contains(String name) { return variables.containsKey(name.toLowerCase(Locale.ROOT)); }

        MemoryType typeOf(String name) {
            MemoryVariable variable = variables.get(name.toLowerCase(Locale.ROOT));
            if (variable == null) throw new IllegalArgumentException("undeclared @memory: " + name);
            return variable.type;
        }
    }

    private static final class MemoryVariable {
        private final MemoryType type;
        private final String function;

        private MemoryVariable(MemoryType type, String function) {
            this.type = type;
            this.function = function;
        }
    }

    static final class Numeric {
        private final NumericExpression expression;

        private Numeric(NumericExpression expression) { this.expression = expression; }

        float evaluate(CustomProjectileState state) {
            return withState(state, () -> expression.evaluate((OrderableUnit) state.source));
        }
    }

    static final class Condition {
        private final BooleanExpression expression;

        private Condition(BooleanExpression expression) { this.expression = expression; }

        boolean evaluate(CustomProjectileState state) {
            return withState(state, () -> expression.evaluate((OrderableUnit) state.source));
        }
    }

    private interface Evaluation<T> { T evaluate(); }
    private interface StateReader { float read(CustomProjectileState state); }
}
