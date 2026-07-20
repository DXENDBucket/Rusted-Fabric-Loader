package io.github.endx.rustedfabricapi.api.unit.attribute;

import io.github.endx.rustedfabricapi.api.util.Identifier;

import java.util.Objects;

/** A namespaced, replaceable custom-unit stat modifier. */
public final class UnitStatModifier implements Comparable<UnitStatModifier> {
    private final Identifier id;
    private final UnitStatOperation operation;
    private final double amount;

    public UnitStatModifier(Identifier id, UnitStatOperation operation, double amount) {
        this.id = Objects.requireNonNull(id, "id");
        this.operation = Objects.requireNonNull(operation, "operation");
        if (!Double.isFinite(amount)) {
            throw new IllegalArgumentException("amount must be finite");
        }
        this.amount = amount;
    }

    public static UnitStatModifier of(String id, UnitStatOperation operation, double amount) {
        return new UnitStatModifier(Identifier.parse(id), operation, amount);
    }

    public Identifier id() { return id; }
    public UnitStatOperation operation() { return operation; }
    public double amount() { return amount; }

    @Override
    public int compareTo(UnitStatModifier other) {
        return id.compareTo(Objects.requireNonNull(other, "other").id);
    }
}
