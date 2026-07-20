package io.github.endx.rustedfabricapi.api.unit.action;

/** Native input mode used after a Java unit action button is pressed. */
public enum JavaUnitActionTargeting {
    /** Executes immediately without a target. */
    IMMEDIATE,
    /** Enters the native map targeting mode and supplies a synchronized world point. */
    WORLD_POINT
}
