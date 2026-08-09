package io.github.endx.rustedfabricapi.verification;

import io.github.endx.rustedfabricapi.api.projectile.pattern.ProjectilePatternEmitter;
import io.github.endx.rustedfabricapi.api.projectile.pattern.ProjectilePatternOffset;
import io.github.endx.rustedfabricapi.api.projectile.pattern.ProjectilePatternSpec;
import io.github.endx.rustedfabricapi.api.projectile.pattern.TurretProjectilePatternPlan;
import io.github.endx.rustedfabricapi.api.projectile.spawn.ProjectileAimMode;
import io.github.endx.rustedfabricapi.api.projectile.spawn.ProjectileCollisionSpec;
import io.github.endx.rustedfabricapi.api.projectile.spawn.TerrainKind;
import io.github.endx.rustedfabricapi.api.projectile.spawn.TerrainTransitionSpec;
import io.github.endx.rustedfabricapi.api.projectile.spawn.UnitCollisionFilterSpec;
import io.github.endx.rustedfabricapi.api.projectile.spawn.UnitCollisionLayer;
import rustedwarfare.custom.CustomProjectileTemplate;
import rustedwarfare.unit.MovementType;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

final class ProjectilePatternContractVerification {
    private ProjectilePatternContractVerification() { }

    static void verify() {
        List<ProjectilePatternOffset> fan = ProjectilePatternEmitter.offsets(
                ProjectilePatternSpec.fan(7, 60.0F), 15.0F);
        require(fan.size() == 7, "fan count drifted");
        require(close(fan.get(0).directionOffset(), -30.0F)
                        && close(fan.get(3).directionOffset(), 0.0F)
                        && close(fan.get(6).directionOffset(), 30.0F),
                "fan is not centered with inclusive endpoints");

        List<ProjectilePatternOffset> singleFan = ProjectilePatternEmitter.offsets(
                ProjectilePatternSpec.fan(1, 60.0F), 15.0F);
        require(close(singleFan.get(0).directionOffset(), 0.0F),
                "single-projectile fan must remain centered");

        List<ProjectilePatternOffset> ring = ProjectilePatternEmitter.offsets(
                ProjectilePatternSpec.ring(4, 15.0F), 90.0F);
        require(close(ring.get(0).directionOffset(), 15.0F)
                        && close(ring.get(1).directionOffset(), 105.0F)
                        && close(ring.get(3).directionOffset(), 285.0F),
                "ring must not duplicate its endpoint");

        List<ProjectilePatternOffset> ringArc = ProjectilePatternEmitter.offsets(
                ProjectilePatternSpec.ring(3, -45.0F, 90.0F), 0.0F);
        require(close(ringArc.get(0).directionOffset(), -45.0F)
                        && close(ringArc.get(1).directionOffset(), 0.0F)
                        && close(ringArc.get(2).directionOffset(), 45.0F),
                "partial ring sweep must include both endpoints");

        List<ProjectilePatternOffset> line = ProjectilePatternEmitter.offsets(
                ProjectilePatternSpec.line(3, 10.0F), 0.0F);
        require(close(line.get(0).originOffsetX(), 0.0F)
                        && close(line.get(0).originOffsetY(), -10.0F)
                        && close(line.get(1).originOffsetY(), 0.0F)
                        && close(line.get(2).originOffsetY(), 10.0F),
                "line does not use the unit-local right axis");

        try {
            ProjectilePatternSpec.fan(ProjectilePatternSpec.MAX_PROJECTILES + 1, 30.0F);
            throw new AssertionError("pattern accepted an unsafe projectile count");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }

        CustomProjectileTemplate template = new CustomProjectileTemplate();
        TerrainTransitionSpec shoreImpact = TerrainTransitionSpec.of(
                TerrainKind.LAND, TerrainKind.WATER);
        UnitCollisionFilterSpec airTargets = UnitCollisionFilterSpec.builder()
                .layers(EnumSet.of(UnitCollisionLayer.AIR, UnitCollisionLayer.UNDERWATER))
                .movementTypes(Set.of(MovementType.air))
                .heightRange(-4.0F, 80.0F)
                .includeTransported(true)
                .build();
        TurretProjectilePatternPlan plan = TurretProjectilePatternPlan
                .builder(template, ProjectilePatternSpec.line(3, 8.0F))
                .aimMode(ProjectileAimMode.POINT)
                .centerDirection(45.0F)
                .originOffset(3.0F, -4.0F, 2.0F)
                .collision(ProjectileCollisionSpec.of(
                        true, true, 4.0F, shoreImpact, airTargets))
                .build();
        require(plan.template() == template && plan.pattern().count() == 3
                        && close(plan.centerDirection(), 45.0F)
                        && close(plan.originOffsetX(), 3.0F)
                        && close(plan.originOffsetY(), -4.0F)
                        && plan.collision().collideWithUnits()
                        && plan.collision().collideWithTerrain()
                        && close(plan.collision().contactRadius(), 4.0F)
                        && plan.collision().terrainTransition().from() == TerrainKind.LAND
                        && plan.collision().terrainTransition().to() == TerrainKind.WATER
                        && plan.collision().unitFilter().enabled()
                        && plan.collision().unitFilter().layers().contains(UnitCollisionLayer.AIR)
                        && plan.collision().unitFilter().layers().contains(UnitCollisionLayer.UNDERWATER)
                        && plan.collision().unitFilter().movementTypes().contains(MovementType.air)
                        && close(plan.collision().unitFilter().minHeight(), -4.0F)
                        && close(plan.collision().unitFilter().maxHeight(), 80.0F)
                        && plan.collision().unitFilter().includeTransported(),
                "turret projectile plan lost replacement data");

        try {
            ProjectileCollisionSpec.of(true, false, -1.0F);
            throw new AssertionError("collision accepted a negative contact radius");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }

        try {
            TurretProjectilePatternPlan
                    .builder(template, ProjectilePatternSpec.fan(3, 30.0F))
                    .aimMode(ProjectileAimMode.UNIT)
                    .build();
            throw new AssertionError("angular turret pattern accepted non-direction aim");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }

    private static boolean close(float first, float second) {
        return StrictMath.abs(first - second) < 0.0001F;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
