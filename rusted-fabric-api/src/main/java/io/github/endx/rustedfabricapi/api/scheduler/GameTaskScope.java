package io.github.endx.rustedfabricapi.api.scheduler;

/** Lifetime boundary of a scheduled update-thread task. */
public enum GameTaskScope {
    /** Cancelled after a new map has loaded successfully and when the session ends. */
    MAP,
    /** Survives map changes but is cancelled when the current game session ends. */
    SESSION,
    /** Survives map/session transitions until explicitly cancelled or the process exits. */
    GLOBAL
}
