package io.github.endx.rustedfabricapi.api.ai;

/** Suggested strategic treatment of one resource site; controllers remain free to ignore it. */
public enum AiResourceObjectiveKind {
    CAPTURE,
    LOCK_DOWN,
    DENY,
    DEFEND,
    SUPPORT
}
