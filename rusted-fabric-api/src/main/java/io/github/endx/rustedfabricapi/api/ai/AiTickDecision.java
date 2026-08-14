package io.github.endx.rustedfabricapi.api.ai;

/** Determines whether the game's native AI update should still run for this tick. */
public enum AiTickDecision {
    /** Run the original Rusted Warfare AI after the controller returns. */
    PASS,
    /** The controller handled this tick; skip the original AI update. */
    REPLACE_NATIVE
}
