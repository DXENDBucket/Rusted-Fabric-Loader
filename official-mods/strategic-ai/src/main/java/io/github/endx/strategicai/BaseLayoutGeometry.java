package io.github.endx.strategicai;

import io.github.endx.rustedfabricapi.api.world.WorldPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Stable, front-oriented building districts around one base anchor. */
final class BaseLayoutGeometry {
    enum District {
        PRODUCTION,
        DEFENSE,
        SUPPORT
    }

    private static final float[][] PRODUCTION = {
            {-95.0F, -68.0F}, {-95.0F, 68.0F},
            {-150.0F, 0.0F}, {-165.0F, -125.0F}, {-165.0F, 125.0F},
            {-230.0F, -65.0F}, {-230.0F, 65.0F},
            {-285.0F, -135.0F}, {-285.0F, 135.0F}
    };
    private static final float[][] DEFENSE = {
            {190.0F, 0.0F}, {165.0F, -135.0F}, {165.0F, 135.0F},
            {75.0F, -220.0F}, {75.0F, 220.0F},
            {-75.0F, -245.0F}, {-75.0F, 245.0F},
            {-190.0F, -180.0F}, {-190.0F, 180.0F}
    };
    private static final float[][] SUPPORT = {
            {-90.0F, 0.0F}, {-90.0F, -170.0F}, {-90.0F, 170.0F},
            {-210.0F, 0.0F}, {-210.0F, -250.0F}, {-210.0F, 250.0F}
    };

    private BaseLayoutGeometry() {
    }

    static List<WorldPoint> slots(WorldPoint anchor, WorldPoint front, District district) {
        float dx = front.x() - anchor.x();
        float dy = front.y() - anchor.y();
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (!Float.isFinite(length) || length < 0.001F) {
            dx = 1.0F;
            dy = 0.0F;
        } else {
            dx /= length;
            dy /= length;
        }
        float rightX = -dy;
        float rightY = dx;
        float[][] offsets = district == District.PRODUCTION ? PRODUCTION
                : district == District.DEFENSE ? DEFENSE : SUPPORT;
        ArrayList<WorldPoint> result = new ArrayList<WorldPoint>(offsets.length);
        for (float[] offset : offsets) {
            float forward = offset[0];
            float right = offset[1];
            result.add(new WorldPoint(anchor.x() + dx * forward + rightX * right,
                    anchor.y() + dy * forward + rightY * right));
        }
        return Collections.unmodifiableList(result);
    }
}
