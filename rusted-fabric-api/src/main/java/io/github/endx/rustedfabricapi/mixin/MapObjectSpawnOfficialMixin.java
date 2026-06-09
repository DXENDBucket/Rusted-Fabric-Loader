package io.github.endx.rustedfabricapi.mixin;

import com.corrodinggames.rts.game.n;
import com.corrodinggames.rts.game.units.am;
import com.corrodinggames.rts.game.units.custom.l;
import io.github.endx.rustedfabricapi.api.event.MapSpawnEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.w3c.dom.Element;

import java.util.Properties;

@Mixin(targets = "com.corrodinggames.rts.game.b.a", remap = false)
public abstract class MapObjectSpawnOfficialMixin {
    @Unique
    private boolean rustedfabricapi$skipMapObjectSpawnUnit;

    @Redirect(
            method = "<init>(Lorg/w3c/dom/Element;Lcom/corrodinggames/rts/game/b/b;Lcom/corrodinggames/rts/game/b/i;)V",
            at = @At(value = "INVOKE", target = "Ljava/util/Properties;getProperty(Ljava/lang/String;)Ljava/lang/String;", ordinal = 0),
            require = 1
    )
    private String rustedfabricapi$beforeMapObjectSpawnUnit(Properties properties, String key, Element element, @Coerce Object mapEngine, @Coerce Object objectGroup) {
        String unitName = properties.getProperty(key);
        String customUnitName = properties.getProperty("customUnit");
        if (unitName == null && customUnitName == null) {
            return unitName;
        }

        String teamName = properties.getProperty("team");
        rustedfabricapi$skipMapObjectSpawnUnit = MapSpawnEvents.BEFORE_MAP_OBJECT_SPAWN_UNIT.invoker()
                .beforeMapObjectSpawnUnit(this, mapEngine, objectGroup, properties, unitName, customUnitName, teamName);
        return rustedfabricapi$skipMapObjectSpawnUnit ? null : unitName;
    }

    @Redirect(
            method = "<init>(Lorg/w3c/dom/Element;Lcom/corrodinggames/rts/game/b/b;Lcom/corrodinggames/rts/game/b/i;)V",
            at = @At(value = "INVOKE", target = "Ljava/util/Properties;getProperty(Ljava/lang/String;)Ljava/lang/String;", ordinal = 1),
            require = 1
    )
    private String rustedfabricapi$skipCancelledMapObjectCustomUnit(Properties properties, String key, Element element, @Coerce Object mapEngine, @Coerce Object objectGroup) {
        if (rustedfabricapi$skipMapObjectSpawnUnit) {
            return null;
        }
        return properties.getProperty(key);
    }

    @Redirect(
            method = "<init>(Lorg/w3c/dom/Element;Lcom/corrodinggames/rts/game/b/b;Lcom/corrodinggames/rts/game/b/i;)V",
            at = @At(value = "INVOKE", target = "Lcom/corrodinggames/rts/game/units/custom/l;n(Ljava/lang/String;)Lcom/corrodinggames/rts/game/units/custom/l;"),
            require = 1
    )
    private l rustedfabricapi$mapObjectCustomUnitResolveCallback(String customUnitName, Element element, @Coerce Object mapEngine, @Coerce Object objectGroup) {
        l metadata = l.n(customUnitName);
        Object resolved = MapSpawnEvents.MAP_OBJECT_CUSTOM_UNIT_RESOLVE.invoker()
                .mapObjectCustomUnitResolve(this, mapEngine, objectGroup, customUnitName, metadata);
        if (resolved instanceof l) {
            return (l) resolved;
        }
        return metadata;
    }

    @Redirect(
            method = "<init>(Lorg/w3c/dom/Element;Lcom/corrodinggames/rts/game/b/b;Lcom/corrodinggames/rts/game/b/i;)V",
            at = @At(value = "INVOKE", target = "Lcom/corrodinggames/rts/game/n;c(Lcom/corrodinggames/rts/game/units/am;)V"),
            require = 1
    )
    private void rustedfabricapi$afterMapObjectSpawnUnit(am unit, Element element, @Coerce Object mapEngine, @Coerce Object objectGroup) {
        n.c(unit);
        MapSpawnEvents.AFTER_MAP_OBJECT_SPAWN_UNIT.invoker()
                .afterMapObjectSpawnUnit(this, mapEngine, objectGroup, unit, rustedfabricapi$getProperties());
    }

    @Unique
    private Properties rustedfabricapi$getProperties() {
        try {
            java.lang.reflect.Field field = this.getClass().getDeclaredField("n");
            field.setAccessible(true);
            return (Properties) field.get(this);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
