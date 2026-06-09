package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.MapMissionEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

@Mixin(targets = "rustedwarfare.core.RustedWarfareGameEngine", remap = false)
public abstract class CurrentMapLifecycleNamedMixin {
    @Inject(method = "loadCurrentMapAndStartGame(ZZLcom/corrodinggames/rts/gameFramework/s;)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeCurrentMapLoad(boolean optionA, boolean optionB, @Coerce Object mode, CallbackInfo ci) {
        if (MapMissionEvents.BEFORE_CURRENT_MAP_LOAD.invoker().beforeCurrentMapLoad(this, optionA, optionB, mode)) {
            ci.cancel();
        }
    }

    @Inject(
            method = "loadCurrentMapAndStartGame(ZZLcom/corrodinggames/rts/gameFramework/s;)V",
            at = @At(value = "FIELD", target = "Lrustedwarfare/map/MapEngine;revealedMap:Z", ordinal = 0),
            require = 1
    )
    private void rustedfabricapi$afterCurrentMapLoadedBeforeStartingUnits(boolean optionA, boolean optionB, @Coerce Object mode, CallbackInfo ci) {
        MapMissionEvents.AFTER_CURRENT_MAP_LOADED_BEFORE_STARTING_UNITS.invoker()
                .afterCurrentMapLoadedBeforeStartingUnits(this, rustedfabricapi$getFieldValue("mapEngine", "bL"), optionA, optionB, mode);
    }

    @Inject(method = "loadCurrentMapAndStartGame(ZZLcom/corrodinggames/rts/gameFramework/s;)V", at = @At(value = "RETURN", ordinal = 2), require = 1)
    private void rustedfabricapi$afterCurrentMapStarted(boolean optionA, boolean optionB, @Coerce Object mode, CallbackInfo ci) {
        MapMissionEvents.AFTER_CURRENT_MAP_STARTED.invoker().afterCurrentMapStarted(this, optionA, optionB, mode);
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
