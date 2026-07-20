package io.github.endx.rustedfabricapi.api.unit;

/** Identifies the mutation boundary reported with a team credit change. */
public enum TeamCreditChangeSource {
    /** A direct, validated assignment made through {@link Teams#setCredits}. */
    API_SET,
    /** The game's normal credit-addition path, including recorded income. */
    NATIVE_RECORDED_INCOME
}
