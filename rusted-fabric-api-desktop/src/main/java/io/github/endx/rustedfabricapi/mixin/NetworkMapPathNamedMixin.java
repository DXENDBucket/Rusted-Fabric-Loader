package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.MapDiscoveryEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;

@Mixin(targets = "rustedwarfare.network.NetworkEngine", remap = false)
public abstract class NetworkMapPathNamedMixin {
    @Inject(method = "getNetworkMapPath()Ljava/lang/String;", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeNetworkMapPathResolve(CallbackInfoReturnable<String> cir) {
        Object gameSetup = rustedfabricapi$getFieldValue(this, "gameSetup", "ay");
        String override = MapDiscoveryEvents.BEFORE_NETWORK_MAP_PATH_RESOLVE.invoker()
                .beforeNetworkMapPathResolve(this, gameSetup, (String) rustedfabricapi$getFieldValue(gameSetup, "mapPath", "b"), rustedfabricapi$getFieldValue(gameSetup, "mapType", "a"));
        if (override != null) {
            cir.setReturnValue(override);
        }
    }

    private Object rustedfabricapi$getFieldValue(Object owner, String namedField, String officialField) {
        if (owner == null) {
            return null;
        }
        Class<?> type = owner.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(namedField);
                field.setAccessible(true);
                return field.get(owner);
            } catch (ReflectiveOperationException e) {
                try {
                    Field field = type.getDeclaredField(officialField);
                    field.setAccessible(true);
                    return field.get(owner);
                } catch (ReflectiveOperationException ignored) {
                    type = type.getSuperclass();
                }
            }
        }
        return null;
    }
}
