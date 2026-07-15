package io.github.endx.rustedfabricapi.api.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.endx.rustedfabricapi.api.util.RustedReflection;

/** Stable accessors for the mapped Projectile runtime without compile-time game classes. */
public final class Projectiles {
    private static final String[] TYPES = {
            "rustedwarfare.game.Projectile",
            "com.corrodinggames.rts.game.f"
    };

    private Projectiles() {
    }

    public static List<Object> activeObjects() {
        Object value = RustedReflection.getStaticFieldValue(TYPES,
                new String[]{"activeProjectiles", "a"});
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(value));
    }

    public static List<ProjectileSnapshot> activeSnapshots() {
        List<ProjectileSnapshot> snapshots = new ArrayList<ProjectileSnapshot>();
        for (Object projectile : activeObjects()) {
            if (projectile != null) snapshots.add(ProjectileSnapshot.capture(projectile));
        }
        return Collections.unmodifiableList(snapshots);
    }

    public static ProjectileSnapshot snapshot(Object projectile) {
        return ProjectileSnapshot.capture(projectile);
    }

    public static void requestRemoval(Object projectile) {
        if (projectile == null) throw new IllegalArgumentException("projectile must not be null");
        if (hasTypeInHierarchy(projectile, "com.corrodinggames.rts.gameFramework.ah")) {
            // Android 1.15 inlines the PC requestRemoval() body; its d() method is getDrawPaint().
            RustedReflection.setFieldValue(projectile,
                    new String[]{"removalRequested", "aS"}, Boolean.TRUE);
            return;
        }
        RustedReflection.invokeInstance(projectile, new String[]{"requestRemoval", "d"});
    }

    private static boolean hasTypeInHierarchy(Object owner, String typeName) {
        Class<?> current = owner.getClass();
        while (current != null) {
            if (typeName.equals(current.getName())) return true;
            current = current.getSuperclass();
        }
        return false;
    }
}
