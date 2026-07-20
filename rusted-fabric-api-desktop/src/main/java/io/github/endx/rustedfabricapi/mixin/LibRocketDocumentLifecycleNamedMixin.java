package io.github.endx.rustedfabricapi.mixin;

import com.ElementDocument;
import io.github.endx.rustedfabricapi.internal.client.screen.ScreenRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.LibRocket", remap = false)
public abstract class LibRocketDocumentLifecycleNamedMixin {
    @Inject(method = "closeDocument(Lcom/ElementDocument;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterDocumentClosed(ElementDocument document, CallbackInfo ci) {
        ScreenRuntime.onDocumentClosed(this, document);
    }
}
