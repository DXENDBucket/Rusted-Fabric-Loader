package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.client.event.CustomActionIconEvents;
import io.github.endx.rustedfabricapi.api.client.render.ClientImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rustedwarfare.client.render.GameImage;
import rustedwarfare.custom.action.CustomAction;
import rustedwarfare.custom.action.CustomActionConfig;
import rustedwarfare.unit.action.UnitAction;

/** Keeps animated or stateful action-icon presentation outside synchronized action parsing. */
@Mixin(CustomAction.class)
public abstract class CustomActionIconNamedMixin {
    @Shadow public CustomActionConfig config;

    @Inject(method = "getIconImage()Lrustedwarfare/client/render/GameImage;",
            at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$overrideConfiguredActionIcon(
            CallbackInfoReturnable<GameImage> cir) {
        String actionId = ((UnitAction) (Object) this).getActionIdString();
        String actionName = config != null ? config.actionName : null;
        ClientImage replacement = CustomActionIconEvents.OVERRIDE.invoker()
                .icon(actionId, actionName);
        if (replacement == null || replacement.isClosed()) return;
        try {
            cir.setReturnValue(replacement.nativeImage());
        } catch (IllegalStateException closedDuringLookup) {
            // Retain the native icon when an owner releases an image during this lookup.
        }
    }
}
