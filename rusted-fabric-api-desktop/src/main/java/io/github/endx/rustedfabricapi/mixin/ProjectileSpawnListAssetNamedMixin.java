package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.CustomAssetEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "rustedwarfare.custom.spawn.ProjectileSpawnList", remap = false)
public abstract class ProjectileSpawnListAssetNamedMixin {
    @Inject(method = "parseStringInternal(Lrustedwarfare/custom/CustomUnitMetadata;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Z)Lrustedwarfare/custom/spawn/ProjectileSpawnList;", at = @At("HEAD"), cancellable = true, require = 1)
    private static void rustedfabricapi$beforeParseProjectileSpawnList(@Coerce Object metadata, String rawList, String section, String key, boolean requireSingle, CallbackInfoReturnable<Object> cir) {
        if (CustomAssetEvents.BEFORE_PARSE_PROJECTILE_SPAWN_LIST.invoker()
                .beforeParseProjectileSpawnList(metadata, rawList, section, key, requireSingle)) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "parseStringInternal(Lrustedwarfare/custom/CustomUnitMetadata;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Z)Lrustedwarfare/custom/spawn/ProjectileSpawnList;", at = @At("RETURN"), cancellable = true, require = 1)
    private static void rustedfabricapi$afterParseProjectileSpawnList(@Coerce Object metadata, String rawList, String section, String key, boolean requireSingle, CallbackInfoReturnable<Object> cir) {
        cir.setReturnValue(CustomAssetEvents.AFTER_PARSE_PROJECTILE_SPAWN_LIST.invoker()
                .afterParseProjectileSpawnList(metadata, rawList, section, key, requireSingle, cir.getReturnValue()));
    }
}
