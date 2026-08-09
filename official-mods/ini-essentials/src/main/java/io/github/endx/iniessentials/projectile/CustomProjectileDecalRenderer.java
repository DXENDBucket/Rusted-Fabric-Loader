package io.github.endx.iniessentials.projectile;

import android.graphics.PointF;
import io.github.endx.rustedfabricapi.api.client.render.event.ProjectileRenderEvents;
import io.github.endx.rustedfabricapi.mixin.accessor.DecalBehaviorAccessor;
import rustedwarfare.custom.CustomProjectileTemplate;
import rustedwarfare.custom.CustomUnit;
import rustedwarfare.custom.graphics.DecalBehavior;
import rustedwarfare.custom.graphics.DecalLayer;
import rustedwarfare.game.Projectile;
import rustedwarfare.util.RwArrayList;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/** Draws native Decal sections at a CustomProjectile's live world position. */
public final class CustomProjectileDecalRenderer {
    private static final ThreadLocal<PointF> POINT = ThreadLocal.withInitial(PointF::new);
    private static final Field[] POINT_COORDINATES = pointCoordinateFields();

    private CustomProjectileDecalRenderer() { }

    public static void register() {
        ProjectileRenderEvents.DRAW.register(CustomProjectileDecalRenderer::draw);
    }

    private static void draw(Projectile projectile, float delta,
                             ProjectileRenderEvents.Stage stage) {
        if (!(projectile.projectileTemplate instanceof CustomProjectileTemplate)
                || !(projectile.sourceUnit instanceof CustomUnit)) return;
        DecalBehavior behavior = CustomProjectileDefinitions.decalsFor(
                (CustomProjectileTemplate) projectile.projectileTemplate);
        if (behavior == null) return;

        DecalBehaviorAccessor access = (DecalBehaviorAccessor) (Object) behavior;
        RwArrayList decals;
        DecalLayer layer;
        switch (stage) {
            case SHADOW:
                decals = access.rustedfabricapi$getShadowDecals();
                layer = DecalLayer.SHADOW;
                break;
            case BEFORE_BODY:
                decals = access.rustedfabricapi$getBeforeBodyDecals();
                layer = DecalLayer.BEFORE_BODY;
                break;
            case AFTER_BODY:
                decals = access.rustedfabricapi$getAfterBodyDecals();
                layer = DecalLayer.AFTER_BODY;
                break;
            case ON_TOP:
                decals = access.rustedfabricapi$getOnTopDecals();
                layer = DecalLayer.ON_TOP;
                break;
            case BEFORE_UI:
                decals = access.rustedfabricapi$getBeforeUiDecals();
                layer = DecalLayer.BEFORE_UI;
                break;
            default:
                throw new AssertionError(stage);
        }
        if (decals == null || decals.isEmpty()) return;
        PointF point = POINT.get();
        setPoint(point, projectile.x, projectile.y);
        DecalBehavior.drawLayerAtPoint(
                (CustomUnit) projectile.sourceUnit, delta, layer, decals, point);
    }

    private static Field[] pointCoordinateFields() {
        try {
            PointF probe = new PointF(137.0F, 293.0F);
            Field x = null;
            Field y = null;
            for (Field field : PointF.class.getDeclaredFields()) {
                if (field.getType() != float.class
                        || Modifier.isStatic(field.getModifiers())) continue;
                field.setAccessible(true);
                float value = field.getFloat(probe);
                if (value == 137.0F) x = field;
                if (value == 293.0F) y = field;
            }
            if (x == null || y == null) {
                throw new NoSuchFieldException("PointF coordinate fields");
            }
            return new Field[]{x, y};
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    private static void setPoint(PointF point, float x, float y) {
        try {
            POINT_COORDINATES[0].setFloat(point, x);
            POINT_COORDINATES[1].setFloat(point, y);
        } catch (IllegalAccessException failure) {
            throw new IllegalStateException("cannot update projectile Decal anchor", failure);
        }
    }
}
