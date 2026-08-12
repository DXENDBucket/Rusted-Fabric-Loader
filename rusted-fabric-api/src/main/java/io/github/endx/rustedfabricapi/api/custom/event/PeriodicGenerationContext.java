package io.github.endx.rustedfabricapi.api.custom.event;

import io.github.endx.rustedfabricapi.api.custom.CustomUnitHandle;
import rustedwarfare.custom.CustomUnit;
import rustedwarfare.custom.resource.ResourceAmount;

import java.util.Objects;

/** One sparse native {@code generation_delay} settlement for a custom unit. */
public final class PeriodicGenerationContext {
    private final CustomUnitHandle unit;
    private final int delayFrames;
    private final int baseCredits;
    private final Object nativeAmount;

    public PeriodicGenerationContext(CustomUnit unit, ResourceAmount amount) {
        CustomUnit checkedUnit = Objects.requireNonNull(unit, "unit");
        ResourceAmount checkedAmount = Objects.requireNonNull(amount, "amount");
        this.unit = CustomUnitHandle.of(checkedUnit);
        this.delayFrames = checkedUnit.unitMetadata.generationDelay;
        this.baseCredits = checkedAmount.credits;
        this.nativeAmount = checkedAmount;
    }

    public CustomUnitHandle unit() { return unit; }
    public int delayFrames() { return delayFrames; }
    public int baseCredits() { return baseCredits; }

    /** Opaque native amount for advanced integrations not yet covered by the stable view. */
    public Object nativeAmount() { return nativeAmount; }
}
