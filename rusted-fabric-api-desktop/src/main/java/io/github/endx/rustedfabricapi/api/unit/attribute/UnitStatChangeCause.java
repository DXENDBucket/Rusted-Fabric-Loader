package io.github.endx.rustedfabricapi.api.unit.attribute;

/** Origin of an effective custom-unit stat change. */
public enum UnitStatChangeCause {
    API_SET,
    MODIFIER_ADDED,
    MODIFIER_REMOVED,
    MODIFIERS_CLEARED,
    NATIVE_MUTATION,
    METADATA_APPLY
}
