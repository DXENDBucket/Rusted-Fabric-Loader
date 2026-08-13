package io.github.endx.rustedfabricapi.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import rustedwarfare.custom.CustomUnit;
import rustedwarfare.unit.build.FactoryQueueManager;

/** Internal access used by the namespace-stable custom-unit queue controls. */
@Mixin(value = CustomUnit.class, remap = false)
public interface CustomUnitBuildQueueAccessor {
    @Accessor("buildQueue")
    FactoryQueueManager rustedfabricapi$getBuildQueue();
}
