package io.github.endx.rustedfabricapi.verification;

import io.github.endx.rustedfabricapi.api.projectile.pattern.ProjectilePatternEmitter;
import io.github.endx.rustedfabricapi.api.projectile.pattern.ProjectilePatternOffset;
import io.github.endx.rustedfabricapi.api.projectile.pattern.ProjectilePatternSpec;

import java.util.List;

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
    }

    private static boolean close(float first, float second) {
        return StrictMath.abs(first - second) < 0.0001F;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
