package io.github.endx.rustedfabricapi.api.unit.action;

/** Deterministic handler for a registered Java unit action. */
@FunctionalInterface
public interface JavaUnitActionHandler {
    void execute(JavaUnitActionContext context);
}
