package io.github.endx.rustedfabricapi.api.unit.attribute;

/** Fixed modifier stages, evaluated in declaration order. */
public enum UnitStatOperation {
    /** Adds the amount directly. */
    ADD_VALUE,
    /** Adds {@code baseline * amount}. */
    ADD_MULTIPLIED_BASE,
    /** Multiplies the accumulated value by {@code 1 + amount}. */
    MULTIPLY_TOTAL
}
