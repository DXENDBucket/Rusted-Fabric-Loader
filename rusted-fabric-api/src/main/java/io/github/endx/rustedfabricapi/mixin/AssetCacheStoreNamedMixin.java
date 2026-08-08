package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.FileSystemEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.InputStream;

@Mixin(targets = "rustedwarfare.io.AssetCacheStore", remap = false)
public abstract class AssetCacheStoreNamedMixin {
    @Inject(method = "openAssetCached(Ljava/lang/String;Ljava/lang/String;)Ljava/io/InputStream;", at = @At("HEAD"), cancellable = true, require = 1)
    private static void rustedfabricapi$beforeOpenAssetCached(String source, String key,
                                                              CallbackInfoReturnable<InputStream> cir) {
        InputStream override = FileSystemEvents.BEFORE_OPEN_ASSET_CACHED.invoker()
                .beforeOpenAssetCached(source, key);
        if (override != null) {
            cir.setReturnValue(override);
        }
    }

    @Inject(method = "openAssetCached(Ljava/lang/String;Ljava/lang/String;)Ljava/io/InputStream;", at = @At("RETURN"), cancellable = true, require = 1)
    private static void rustedfabricapi$afterOpenAssetCached(String source, String key,
                                                             CallbackInfoReturnable<InputStream> cir) {
        cir.setReturnValue(FileSystemEvents.AFTER_OPEN_ASSET_CACHED.invoker()
                .afterOpenAssetCached(source, key, cir.getReturnValue()));
    }

    @Inject(method = "listCachedAssetDirectory(Ljava/lang/String;Ljava/lang/String;)[Ljava/lang/String;", at = @At("HEAD"), cancellable = true, require = 1)
    private static void rustedfabricapi$beforeListCachedAssetDirectory(String source, String key,
                                                                       CallbackInfoReturnable<String[]> cir) {
        String[] override = FileSystemEvents.BEFORE_LIST_CACHED_ASSET_DIRECTORY.invoker()
                .beforeListCachedAssetDirectory(source, key);
        if (override != null) {
            cir.setReturnValue(override);
        }
    }

    @Inject(method = "listCachedAssetDirectory(Ljava/lang/String;Ljava/lang/String;)[Ljava/lang/String;", at = @At("RETURN"), cancellable = true, require = 1)
    private static void rustedfabricapi$afterListCachedAssetDirectory(String source, String key,
                                                                      CallbackInfoReturnable<String[]> cir) {
        cir.setReturnValue(FileSystemEvents.AFTER_LIST_CACHED_ASSET_DIRECTORY.invoker()
                .afterListCachedAssetDirectory(source, key, cir.getReturnValue()));
    }
}
