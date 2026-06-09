package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.MapDiscoveryEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "rustedwarfare.io.GameFileSystem", remap = false)
public abstract class GameFileSystemMapDiscoveryNamedMixin {
    @Inject(method = "listDirectoryFiltered(Ljava/lang/String;Z)[Ljava/lang/String;", at = @At("HEAD"), cancellable = true, require = 1)
    private static void rustedfabricapi$beforeMapListDirectoryScan(String path, boolean includeDirectories, CallbackInfoReturnable<String[]> cir) {
        if (MapDiscoveryEvents.BEFORE_MAP_LIST_DIRECTORY_SCAN.invoker().beforeMapListDirectoryScan(path, includeDirectories)) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "listDirectoryFiltered(Ljava/lang/String;Z)[Ljava/lang/String;", at = @At("RETURN"), cancellable = true, require = 1)
    private static void rustedfabricapi$afterMapListDirectoryScan(String path, boolean includeDirectories, CallbackInfoReturnable<String[]> cir) {
        cir.setReturnValue(MapDiscoveryEvents.AFTER_MAP_LIST_DIRECTORY_SCAN.invoker()
                .afterMapListDirectoryScan(path, includeDirectories, cir.getReturnValue()));
    }
}
