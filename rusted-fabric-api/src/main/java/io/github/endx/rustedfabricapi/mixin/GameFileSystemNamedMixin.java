package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.FileSystemEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "rustedwarfare.io.GameFileSystem", remap = false)
public abstract class GameFileSystemNamedMixin {
    @Inject(method = "resolveAbstractPath(Ljava/lang/String;)Ljava/lang/String;", at = @At("HEAD"), cancellable = true, require = 1)
    private static void rustedfabricapi$beforeResolveAbstractPath(String path, CallbackInfoReturnable<String> cir) {
        String override = FileSystemEvents.BEFORE_RESOLVE_ABSTRACT_PATH.invoker().beforeResolveAbstractPath(path);
        if (override != null) {
            cir.setReturnValue(override);
        }
    }

    @Inject(method = "resolveAbstractPath(Ljava/lang/String;)Ljava/lang/String;", at = @At("RETURN"), cancellable = true, require = 1)
    private static void rustedfabricapi$afterResolveAbstractPath(String path, CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(FileSystemEvents.AFTER_RESOLVE_ABSTRACT_PATH.invoker()
                .afterResolveAbstractPath(path, cir.getReturnValue()));
    }

    @Inject(method = "toDisplayPath(Ljava/lang/String;)Ljava/lang/String;", at = @At("RETURN"), cancellable = true, require = 1)
    private static void rustedfabricapi$afterToDisplayPath(String path, CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(FileSystemEvents.AFTER_TO_DISPLAY_PATH.invoker()
                .afterToDisplayPath(path, cir.getReturnValue()));
    }
}
