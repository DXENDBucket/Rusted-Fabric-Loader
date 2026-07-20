package io.github.endx.rustedfabricapi.mixin.accessor;

import java.util.ArrayList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import rustedwarfare.map.MapObjectGroup;

@Mixin(targets = "rustedwarfare.map.MapEngine", remap = false)
public interface MapEngineAccessor {
    @Accessor("objectGroups")
    ArrayList<MapObjectGroup> rustedfabricapi$getObjectGroups();
}
