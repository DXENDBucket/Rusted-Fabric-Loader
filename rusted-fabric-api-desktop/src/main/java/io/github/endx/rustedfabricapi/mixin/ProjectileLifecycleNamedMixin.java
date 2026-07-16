package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.ProjectileEvents;
import io.github.endx.rustedfabricapi.api.game.Projectiles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "rustedwarfare.game.Projectile", remap = false)
public abstract class ProjectileLifecycleNamedMixin {
    @Inject(
            method = "createProjectile(Lrustedwarfare/unit/Unit;FF)Lrustedwarfare/game/Projectile;",
            at = @At("RETURN"),
            require = 1
    )
    private static void rustedfabricapi$afterProjectileCreated(@Coerce Object sourceUnit,
                                                               float x, float y,
                                                               CallbackInfoReturnable<Object> cir) {
        Object projectile = cir.getReturnValue();
        if (projectile != null) {
            ProjectileEvents.AFTER_PROJECTILE_CREATED.invoker()
                    .afterProjectileCreated(projectile, sourceUnit);
        }
    }

    @Inject(
            method = "createProjectileWithHeightAndTurret(Lrustedwarfare/unit/Unit;FFFI)Lrustedwarfare/game/Projectile;",
            at = @At("RETURN"),
            require = 1
    )
    private static void rustedfabricapi$afterProjectileCreatedWithHeight(
            @Coerce Object sourceUnit, float x, float y, float height, int turretIndex,
            CallbackInfoReturnable<Object> cir) {
        Object projectile = cir.getReturnValue();
        if (projectile != null) {
            ProjectileEvents.AFTER_PROJECTILE_CREATED.invoker()
                    .afterProjectileCreated(projectile, sourceUnit);
        }
    }

    @Inject(method = "update(F)V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeProjectileUpdate(float delta, CallbackInfo ci) {
        ProjectileEvents.BEFORE_PROJECTILE_UPDATE.invoker()
                .beforeProjectileUpdate(this, delta);
    }

    @Inject(method = "update(F)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterProjectileUpdate(float delta, CallbackInfo ci) {
        ProjectileEvents.AFTER_PROJECTILE_UPDATE.invoker()
                .afterProjectileUpdate(this, delta);
    }

    @Inject(method = "explodeAndRemove()V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeProjectileExplosion(CallbackInfo ci) {
        ProjectileEvents.BEFORE_PROJECTILE_IMPACT.invoker()
                .onProjectileImpact(this, Projectiles.impactSnapshot(this));
        ProjectileEvents.BEFORE_PROJECTILE_EXPLOSION.invoker()
                .onProjectileExplosion(this);
    }

    @Inject(method = "explodeAndRemove()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterProjectileExplosion(CallbackInfo ci) {
        ProjectileEvents.AFTER_PROJECTILE_EXPLOSION.invoker()
                .onProjectileExplosion(this);
        ProjectileEvents.AFTER_PROJECTILE_IMPACT.invoker()
                .onProjectileImpact(this, Projectiles.impactSnapshot(this));
    }

    @Inject(method = "requestRemoval()V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeProjectileRemovalRequested(CallbackInfo ci) {
        ProjectileEvents.BEFORE_PROJECTILE_REMOVAL.invoker().onProjectileRemoval(
                this, ProjectileEvents.RemovalReason.REQUESTED);
    }

    @Inject(method = "requestRemoval()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterProjectileRemovalRequested(CallbackInfo ci) {
        ProjectileEvents.AFTER_PROJECTILE_REMOVAL.invoker().onProjectileRemoval(
                this, ProjectileEvents.RemovalReason.REQUESTED);
    }

    @Inject(method = "removeFromGame()V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeProjectileRemovedFromGame(CallbackInfo ci) {
        ProjectileEvents.BEFORE_PROJECTILE_REMOVAL.invoker().onProjectileRemoval(
                this, ProjectileEvents.RemovalReason.REMOVED_FROM_GAME);
    }

    @Inject(method = "removeFromGame()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterProjectileRemovedFromGame(CallbackInfo ci) {
        ProjectileEvents.AFTER_PROJECTILE_REMOVAL.invoker().onProjectileRemoval(
                this, ProjectileEvents.RemovalReason.REMOVED_FROM_GAME);
    }
}
