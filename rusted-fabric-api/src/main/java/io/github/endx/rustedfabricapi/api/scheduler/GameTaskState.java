package io.github.endx.rustedfabricapi.api.scheduler;

/** Observable lifecycle state of a scheduled task. */
public enum GameTaskState {
    PENDING,
    RUNNING,
    COMPLETED,
    CANCELLED,
    FAILED
}
