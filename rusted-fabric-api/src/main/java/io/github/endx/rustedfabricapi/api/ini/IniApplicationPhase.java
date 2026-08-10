package io.github.endx.rustedfabricapi.api.ini;

/** Selects when a decoded extension is applied to custom-unit metadata. */
public enum IniApplicationPhase {
    BEFORE_STATIC_VARIABLES,
    AFTER_STATIC_VARIABLES,
    /** After the native unit parser has registered resources, memory, actions, and projectiles. */
    AFTER_METADATA_PARSED
}
