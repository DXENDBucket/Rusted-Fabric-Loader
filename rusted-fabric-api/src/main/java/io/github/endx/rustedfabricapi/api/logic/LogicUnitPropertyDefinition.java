package io.github.endx.rustedfabricapi.api.logic;

import rustedwarfare.unit.OrderableUnit;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** Definition of a parameterless {@code self.*} property in native LogicBoolean expressions. */
public final class LogicUnitPropertyDefinition {
    private static final Pattern NAME = Pattern.compile("self\\.[A-Za-z][A-Za-z0-9]*");

    public enum ValueType { BOOLEAN, NUMBER, STRING }
    public enum Locality { SYNCHRONIZED, CLIENT_LOCAL }

    @FunctionalInterface
    public interface BooleanEvaluator { boolean evaluate(OrderableUnit unit); }
    @FunctionalInterface
    public interface NumberEvaluator { float evaluate(OrderableUnit unit); }
    @FunctionalInterface
    public interface StringEvaluator { String evaluate(OrderableUnit unit); }

    private final String name;
    private final String canonicalName;
    private final ValueType valueType;
    private final Locality locality;
    private final BooleanEvaluator booleanEvaluator;
    private final NumberEvaluator numberEvaluator;
    private final StringEvaluator stringEvaluator;

    private LogicUnitPropertyDefinition(String name, ValueType valueType, Locality locality,
                                        BooleanEvaluator booleanEvaluator,
                                        NumberEvaluator numberEvaluator,
                                        StringEvaluator stringEvaluator) {
        String checked = Objects.requireNonNull(name, "name").trim();
        if (!NAME.matcher(checked).matches()) {
            throw new IllegalArgumentException("invalid self property name: " + name);
        }
        this.name = checked;
        this.canonicalName = checked.toLowerCase(Locale.ROOT);
        this.valueType = Objects.requireNonNull(valueType, "valueType");
        this.locality = Objects.requireNonNull(locality, "locality");
        this.booleanEvaluator = booleanEvaluator;
        this.numberEvaluator = numberEvaluator;
        this.stringEvaluator = stringEvaluator;
    }

    public static LogicUnitPropertyDefinition booleanProperty(
            String name, Locality locality, BooleanEvaluator evaluator) {
        return new LogicUnitPropertyDefinition(name, ValueType.BOOLEAN, locality,
                Objects.requireNonNull(evaluator, "evaluator"), null, null);
    }

    public static LogicUnitPropertyDefinition numberProperty(
            String name, Locality locality, NumberEvaluator evaluator) {
        return new LogicUnitPropertyDefinition(name, ValueType.NUMBER, locality,
                null, Objects.requireNonNull(evaluator, "evaluator"), null);
    }

    public static LogicUnitPropertyDefinition stringProperty(
            String name, Locality locality, StringEvaluator evaluator) {
        return new LogicUnitPropertyDefinition(name, ValueType.STRING, locality,
                null, null, Objects.requireNonNull(evaluator, "evaluator"));
    }

    public String name() { return name; }
    public String canonicalName() { return canonicalName; }
    public ValueType valueType() { return valueType; }
    public Locality locality() { return locality; }

    public boolean evaluateBoolean(OrderableUnit unit) {
        requireType(ValueType.BOOLEAN);
        return booleanEvaluator.evaluate(unit);
    }

    public float evaluateNumber(OrderableUnit unit) {
        requireType(ValueType.NUMBER);
        return numberEvaluator.evaluate(unit);
    }

    public String evaluateString(OrderableUnit unit) {
        requireType(ValueType.STRING);
        return stringEvaluator.evaluate(unit);
    }

    private void requireType(ValueType expected) {
        if (valueType != expected) {
            throw new IllegalStateException(name + " is " + valueType + ", not " + expected);
        }
    }
}
