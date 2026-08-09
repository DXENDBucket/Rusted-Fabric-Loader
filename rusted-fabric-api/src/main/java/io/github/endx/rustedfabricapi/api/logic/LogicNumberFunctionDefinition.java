package io.github.endx.rustedfabricapi.api.logic;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** Definition of a deterministic numeric function exposed to native LogicBoolean expressions. */
public final class LogicNumberFunctionDefinition {
    private static final Pattern NAME = Pattern.compile("[a-z][a-z0-9_]*");

    @FunctionalInterface
    public interface Evaluator {
        float evaluate(float[] arguments);
    }

    private final String name;
    private final int argumentCount;
    private final Evaluator evaluator;

    private LogicNumberFunctionDefinition(String name, int argumentCount, Evaluator evaluator) {
        String checkedName = Objects.requireNonNull(name, "name").trim().toLowerCase(Locale.ROOT);
        if (!NAME.matcher(checkedName).matches()) {
            throw new IllegalArgumentException("invalid lowercase function name: " + name);
        }
        if (argumentCount < 0 || argumentCount > 4) {
            throw new IllegalArgumentException("argumentCount must be between 0 and 4");
        }
        this.name = checkedName;
        this.argumentCount = argumentCount;
        this.evaluator = Objects.requireNonNull(evaluator, "evaluator");
    }

    public static LogicNumberFunctionDefinition of(String name, int argumentCount,
                                                    Evaluator evaluator) {
        return new LogicNumberFunctionDefinition(name, argumentCount, evaluator);
    }

    public String name() { return name; }
    public int argumentCount() { return argumentCount; }

    public float evaluate(float[] arguments) {
        Objects.requireNonNull(arguments, "arguments");
        if (arguments.length != argumentCount) {
            throw new IllegalArgumentException(name + " expected " + argumentCount
                    + " arguments, got " + arguments.length);
        }
        return evaluator.evaluate(arguments.clone());
    }
}
