package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.game.Projectiles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "rustedwarfare.game.Projectile", remap = false)
public abstract class ProjectileLifecycleNamedMixin {
    @Redirect(
            method = "update(F)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lrustedwarfare/game/BaseProjectileTemplate;applyDamageModifiers(Lrustedwarfare/unit/Unit;FZ)F"
            ),
            require = 2
    )
    private float rustedfabricapi$modifyDirectProjectileDamage(
            rustedwarfare.game.BaseProjectileTemplate template,
            rustedwarfare.unit.Unit target, float damage, boolean areaHit) {
        float nativeDamage = template.applyDamageModifiers(target, damage, areaHit);
        return io.github.endx.rustedfabricapi.api.projectile.event.ProjectileCombatEvents
                .MODIFY_DAMAGE.invoker().modify(
                        (rustedwarfare.game.Projectile) (Object) this,
                        target, damage, nativeDamage, nativeDamage, areaHit);
    }

    @Redirect(
            method = "applyAreaDamageToUnit(FLrustedwarfare/unit/Unit;F)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lrustedwarfare/game/BaseProjectileTemplate;applyDamageModifiers(Lrustedwarfare/unit/Unit;FZ)F"
            ),
            require = 1
    )
    private float rustedfabricapi$modifyAreaProjectileDamage(
            rustedwarfare.game.BaseProjectileTemplate template,
            rustedwarfare.unit.Unit target, float damage, boolean areaHit) {
        float nativeDamage = template.applyDamageModifiers(target, damage, areaHit);
        return io.github.endx.rustedfabricapi.api.projectile.event.ProjectileCombatEvents
                .MODIFY_DAMAGE.invoker().modify(
                        (rustedwarfare.game.Projectile) (Object) this,
                        target, damage, nativeDamage, nativeDamage, areaHit);
    }

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
            io.github.endx.rustedfabricapi.api.projectile.event.ProjectileEvents.AFTER_CREATED.invoker()
                    .afterCreated((rustedwarfare.game.Projectile) projectile,
                            (rustedwarfare.unit.Unit) sourceUnit);
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
            io.github.endx.rustedfabricapi.api.projectile.event.ProjectileEvents.AFTER_CREATED.invoker()
                    .afterCreated((rustedwarfare.game.Projectile) projectile,
                            (rustedwarfare.unit.Unit) sourceUnit);
        }
    }

    @Inject(method = "update(F)V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeProjectileUpdate(float delta, CallbackInfo ci) {
        io.github.endx.rustedfabricapi.api.projectile.event.ProjectileEvents.BEFORE_UPDATE.invoker()
                .onUpdate((rustedwarfare.game.Projectile) (Object) this, delta);
    }

    @Inject(method = "update(F)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterProjectileUpdate(float delta, CallbackInfo ci) {
        io.github.endx.rustedfabricapi.api.projectile.event.ProjectileEvents.AFTER_UPDATE.invoker()
                .onUpdate((rustedwarfare.game.Projectile) (Object) this, delta);
    }

    @Inject(method = "explodeAndRemove()V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeProjectileExplosion(CallbackInfo ci) {
        io.github.endx.rustedfabricapi.api.game.ProjectileImpactSnapshot impact = Projectiles.impactSnapshot(this);
        io.github.endx.rustedfabricapi.api.projectile.event.ProjectileEvents.BEFORE_IMPACT.invoker()
                .onImpact((rustedwarfare.game.Projectile) (Object) this, impact);
        io.github.endx.rustedfabricapi.api.projectile.event.ProjectileEvents.BEFORE_EXPLOSION.invoker()
                .onExplosion((rustedwarfare.game.Projectile) (Object) this);
    }

    @Inject(method = "explodeAndRemove()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterProjectileExplosion(CallbackInfo ci) {
        io.github.endx.rustedfabricapi.api.projectile.event.ProjectileEvents.AFTER_EXPLOSION.invoker()
                .onExplosion((rustedwarfare.game.Projectile) (Object) this);
        io.github.endx.rustedfabricapi.api.game.ProjectileImpactSnapshot impact = Projectiles.impactSnapshot(this);
        io.github.endx.rustedfabricapi.api.projectile.event.ProjectileEvents.AFTER_IMPACT.invoker()
                .onImpact((rustedwarfare.game.Projectile) (Object) this, impact);
    }

    @Inject(method = "requestRemoval()V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeProjectileRemovalRequested(CallbackInfo ci) {
        io.github.endx.rustedfabricapi.api.projectile.event.ProjectileEvents.BEFORE_REMOVAL.invoker().onRemoval(
                (rustedwarfare.game.Projectile) (Object) this,
                io.github.endx.rustedfabricapi.api.projectile.event.ProjectileEvents.RemovalReason.REQUESTED);
    }

    @Inject(method = "requestRemoval()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterProjectileRemovalRequested(CallbackInfo ci) {
        io.github.endx.rustedfabricapi.api.projectile.event.ProjectileEvents.AFTER_REMOVAL.invoker().onRemoval(
                (rustedwarfare.game.Projectile) (Object) this,
                io.github.endx.rustedfabricapi.api.projectile.event.ProjectileEvents.RemovalReason.REQUESTED);
    }

    @Inject(method = "removeFromGame()V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeProjectileRemovedFromGame(CallbackInfo ci) {
        io.github.endx.rustedfabricapi.api.projectile.event.ProjectileEvents.BEFORE_REMOVAL.invoker().onRemoval(
                (rustedwarfare.game.Projectile) (Object) this,
                io.github.endx.rustedfabricapi.api.projectile.event.ProjectileEvents.RemovalReason.REMOVED_FROM_GAME);
    }

    @Inject(method = "removeFromGame()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterProjectileRemovedFromGame(CallbackInfo ci) {
        io.github.endx.rustedfabricapi.api.projectile.event.ProjectileEvents.AFTER_REMOVAL.invoker().onRemoval(
                (rustedwarfare.game.Projectile) (Object) this,
                io.github.endx.rustedfabricapi.api.projectile.event.ProjectileEvents.RemovalReason.REMOVED_FROM_GAME);
    }
}
