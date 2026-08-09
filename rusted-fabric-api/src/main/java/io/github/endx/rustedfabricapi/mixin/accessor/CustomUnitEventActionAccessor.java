package io.github.endx.rustedfabricapi.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import rustedwarfare.custom.event.CustomUnitEventAction;
import rustedwarfare.custom.event.CustomUnitEventType;

@Mixin(value = CustomUnitEventAction.class, remap = false)
public interface CustomUnitEventActionAccessor {
    @Accessor("eventType")
    CustomUnitEventType rustedfabricapi$getEventType();
}
