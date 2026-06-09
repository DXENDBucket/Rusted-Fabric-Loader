package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.SaveSyncEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.corrodinggames.rts.gameFramework.w", remap = false)
public abstract class GameObjectSerializationOfficialMixin {
    @Inject(method = "a(Lcom/corrodinggames/rts/gameFramework/j/as;)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeGameObjectSerialize(@Coerce Object outputStream, CallbackInfo ci) {
        if (SaveSyncEvents.BEFORE_GAME_OBJECT_SERIALIZE.invoker().beforeGameObjectSerialize(this, outputStream)) {
            ci.cancel();
        }
    }

    @Inject(method = "a(Lcom/corrodinggames/rts/gameFramework/j/k;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterGameObjectDeserialize(@Coerce Object inputStream, CallbackInfo ci) {
        SaveSyncEvents.AFTER_GAME_OBJECT_DESERIALIZE.invoker().afterGameObjectDeserialize(this, inputStream);
    }
}
