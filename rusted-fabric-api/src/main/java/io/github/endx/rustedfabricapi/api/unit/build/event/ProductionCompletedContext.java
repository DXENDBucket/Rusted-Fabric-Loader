package io.github.endx.rustedfabricapi.api.unit.build.event;

import io.github.endx.rustedfabricapi.api.game.UnitView;

import java.util.Objects;

/** Stable high-level context emitted after a factory has created one unit. */
public final class ProductionCompletedContext {
    private final UnitView producer;
    private final UnitView producedUnit;

    public ProductionCompletedContext(UnitView producer, UnitView producedUnit) {
        this.producer = Objects.requireNonNull(producer, "producer");
        this.producedUnit = Objects.requireNonNull(producedUnit, "producedUnit");
    }

    public UnitView producer() { return producer; }
    public UnitView producedUnit() { return producedUnit; }
}
