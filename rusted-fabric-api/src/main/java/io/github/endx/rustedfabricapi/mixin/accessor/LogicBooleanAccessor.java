package io.github.endx.rustedfabricapi.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import rustedwarfare.custom.logic.LogicBoolean;
import rustedwarfare.custom.logic.LogicEventContext;

@Mixin(value = LogicBoolean.class, remap = false)
public interface LogicBooleanAccessor {
    @Accessor("activeEvent")
    static LogicEventContext rustedfabricapi$getActiveEvent() {
        throw new AssertionError();
    }
}
