package io.github.endx.rustedfabricapi.impl.logic;

import io.github.endx.rustedfabricapi.api.logic.LogicNumberFunctionDefinition;
import rustedwarfare.custom.CustomUnitMetadata;
import rustedwarfare.custom.logic.LogicBoolean;
import rustedwarfare.custom.logic.LogicBooleanLoader;
import rustedwarfare.custom.logic.LogicNumberFunction;
import rustedwarfare.unit.OrderableUnit;

import java.util.ArrayList;
import java.util.List;

/** Generic native parser prototype backing public numeric function definitions. */
public final class RegisteredLogicNumberFunction extends LogicNumberFunction {
    private final LogicNumberFunctionDefinition definition;
    private LogicBoolean[] arguments = new LogicBoolean[0];

    public RegisteredLogicNumberFunction(LogicNumberFunctionDefinition definition) {
        this.definition = definition;
    }

    @Override public String getName() { return definition.name(); }

    @Override
    public void setArgumentsRaw(String raw, CustomUnitMetadata metadata, String functionName) {
        List<String> parts = splitArguments(raw != null ? raw.trim() : "");
        if (parts.size() != definition.argumentCount()) {
            throw new IllegalArgumentException(definition.name() + " expected "
                    + definition.argumentCount() + " arguments, got " + parts.size());
        }
        LogicBoolean[] parsed = new LogicBoolean[parts.size()];
        for (int i = 0; i < parsed.length; i++) {
            parsed[i] = LogicBooleanLoader.parseNumberBlock(metadata, parts.get(i));
            if (parsed[i] == null) throw new IllegalArgumentException("argument " + i + " is empty");
        }
        arguments = parsed;
    }

    @Override public float readNumber(OrderableUnit unit) {
        float[] values = new float[definition.argumentCount()];
        for (int i = 0; i < values.length; i++) values[i] = arguments[i].readNumber(unit);
        return definition.evaluate(values);
    }

    private static List<String> splitArguments(String raw) {
        ArrayList<String> result = new ArrayList<String>();
        if (raw.isEmpty()) return result;
        int depth = 0;
        char quote = 0;
        int start = 0;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (quote != 0) {
                if (c == quote && (i == 0 || raw.charAt(i - 1) != '\\')) quote = 0;
                continue;
            }
            if (c == '\'' || c == '"') quote = c;
            else if (c == '(' || c == '[') depth++;
            else if (c == ')' || c == ']') depth--;
            else if (c == ',' && depth == 0) {
                result.add(requirePart(raw.substring(start, i)));
                start = i + 1;
            }
            if (depth < 0) throw new IllegalArgumentException("unbalanced function arguments");
        }
        if (quote != 0 || depth != 0) throw new IllegalArgumentException("unbalanced function arguments");
        result.add(requirePart(raw.substring(start)));
        return result;
    }

    private static String requirePart(String raw) {
        String value = raw.trim();
        if (value.isEmpty()) throw new IllegalArgumentException("empty function argument");
        return value;
    }
}
