package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.MapDiscoveryEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

@Mixin(targets = "com.corrodinggames.librocket.scripts.Multiplayer", remap = false)
public abstract class MultiplayerMapDropdownOfficialMixin {
    @Inject(method = "updateMapDropdown(Lcom/Element;Ljava/lang/String;Ljava/lang/String;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterMultiplayerMapDropdownBuilt(@Coerce Object rootElement, String mapsElementId, String typeElementId, CallbackInfo ci) {
        MapDiscoveryEvents.AFTER_MULTIPLAYER_MAP_DROPDOWN_BUILT.invoker()
                .afterMultiplayerMapDropdownBuilt(this, rootElement, mapsElementId, typeElementId, rustedfabricapi$getStringArrayField("currentDropdownRawArray"));
    }

    private String[] rustedfabricapi$getStringArrayField(String fieldName) {
        try {
            Field field = this.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (String[]) field.get(this);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
