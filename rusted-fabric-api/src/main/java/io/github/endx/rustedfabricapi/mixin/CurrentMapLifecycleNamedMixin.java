package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.MapMissionEvents;
import io.github.endx.rustedfabricapi.api.data.PersistentData;
import io.github.endx.rustedfabricapi.api.unit.attribute.CustomUnitStats;
import io.github.endx.rustedfabricapi.api.path.Pathfinding;
import io.github.endx.rustedfabricapi.api.scheduler.GameTickScheduler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

@Mixin(targets = "rustedwarfare.core.RustedWarfareGameEngine", remap = false)
public abstract class CurrentMapLifecycleNamedMixin {
    @Inject(method = "loadLevel(ZZLcom/corrodinggames/rts/gameFramework/s;)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeCurrentMapLoad(boolean optionA, boolean optionB, @Coerce Object mode, CallbackInfo ci) {
        boolean cancelled = MapMissionEvents.BEFORE_CURRENT_MAP_LOAD.invoker()
                .beforeCurrentMapLoad(this, optionA, optionB, mode);
        cancelled |= io.github.endx.rustedfabricapi.api.map.event.MapLifecycleEvents.BEFORE_CURRENT_MAP_LOAD.invoker()
                .beforeCurrentMapLoad((rustedwarfare.core.GameEngine) (Object) this,
                        optionA, optionB, (rustedwarfare.game.GameLoadMode) mode);
        if (cancelled) {
            ci.cancel();
        } else {
            PersistentData.clearRuntime();
            CustomUnitStats.clearRuntime();
            Pathfinding.clearRuntime();
        }
    }

    @Inject(
            method = "loadLevel(ZZLcom/corrodinggames/rts/gameFramework/s;)V",
            at = @At(value = "FIELD", target = "Lrustedwarfare/map/MapEngine;revealedMap:Z", ordinal = 0),
            require = 1
    )
    private void rustedfabricapi$afterCurrentMapLoadedBeforeStartingUnits(boolean optionA, boolean optionB, @Coerce Object mode, CallbackInfo ci) {
        Object map = rustedfabricapi$getFieldValue("tileMap", "bL");
        GameTickScheduler.beginMap();
        MapMissionEvents.AFTER_CURRENT_MAP_LOADED_BEFORE_STARTING_UNITS.invoker()
                .afterCurrentMapLoadedBeforeStartingUnits(this, map, optionA, optionB, mode);
        io.github.endx.rustedfabricapi.api.map.event.MapLifecycleEvents.AFTER_CURRENT_MAP_LOADED_BEFORE_STARTING_UNITS.invoker()
                .afterMapLoaded((rustedwarfare.core.GameEngine) (Object) this,
                        (rustedwarfare.map.MapEngine) map, optionA, optionB,
                        (rustedwarfare.game.GameLoadMode) mode);
    }

    @Inject(method = "loadLevel(ZZLcom/corrodinggames/rts/gameFramework/s;)V", at = @At(value = "RETURN", ordinal = 2), require = 1)
    private void rustedfabricapi$afterCurrentMapStarted(boolean optionA, boolean optionB, @Coerce Object mode, CallbackInfo ci) {
        MapMissionEvents.AFTER_CURRENT_MAP_STARTED.invoker().afterCurrentMapStarted(this, optionA, optionB, mode);
        io.github.endx.rustedfabricapi.api.map.event.MapLifecycleEvents.AFTER_CURRENT_MAP_STARTED.invoker()
                .afterMapStarted((rustedwarfare.core.GameEngine) (Object) this,
                        optionA, optionB, (rustedwarfare.game.GameLoadMode) mode);
    }

    private Object rustedfabricapi$getFieldValue(String namedField, String officialField) {
        Class<?> type = this.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(namedField);
                field.setAccessible(true);
                return field.get(this);
            } catch (ReflectiveOperationException e) {
                try {
                    Field field = type.getDeclaredField(officialField);
                    field.setAccessible(true);
                    return field.get(this);
                } catch (ReflectiveOperationException ignored) {
                    type = type.getSuperclass();
                }
            }
        }
        return null;
    }
}
