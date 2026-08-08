package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.MapSpawnEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.w3c.dom.Element;
import rustedwarfare.custom.CustomUnitMetadata;
import rustedwarfare.game.Team;
import rustedwarfare.unit.Unit;

import java.util.Properties;

@Mixin(targets = "rustedwarfare.map.MapObject", remap = false)
public abstract class MapObjectSpawnNamedMixin {
    @Unique
    private boolean rustedfabricapi$skipMapObjectSpawnUnit;

    @Redirect(
            method = "<init>(Lorg/w3c/dom/Element;Lrustedwarfare/map/MapEngine;Lrustedwarfare/map/MapObjectGroup;)V",
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
            method = "<init>(Lorg/w3c/dom/Element;Lrustedwarfare/map/MapEngine;Lrustedwarfare/map/MapObjectGroup;)V",
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
            method = "<init>(Lorg/w3c/dom/Element;Lrustedwarfare/map/MapEngine;Lrustedwarfare/map/MapObjectGroup;)V",
            at = @At(value = "INVOKE", target = "Lrustedwarfare/custom/CustomUnitMetadata;findByNameOrAlias(Ljava/lang/String;)Lrustedwarfare/custom/CustomUnitMetadata;"),
            require = 1
    )
    private CustomUnitMetadata rustedfabricapi$mapObjectCustomUnitResolveCallback(String customUnitName, Element element, @Coerce Object mapEngine, @Coerce Object objectGroup) {
        CustomUnitMetadata metadata = CustomUnitMetadata.findByNameOrAlias(customUnitName);
        Object resolved = MapSpawnEvents.MAP_OBJECT_CUSTOM_UNIT_RESOLVE.invoker()
                .mapObjectCustomUnitResolve(this, mapEngine, objectGroup, customUnitName, metadata);
        if (resolved instanceof CustomUnitMetadata) {
            return (CustomUnitMetadata) resolved;
        }
        return metadata;
    }

    @Redirect(
            method = "<init>(Lorg/w3c/dom/Element;Lrustedwarfare/map/MapEngine;Lrustedwarfare/map/MapObjectGroup;)V",
            at = @At(value = "INVOKE", target = "Lrustedwarfare/game/Team;registerUnit(Lrustedwarfare/unit/Unit;)V"),
            require = 1
    )
    private void rustedfabricapi$afterMapObjectSpawnUnit(Unit unit, Element element, @Coerce Object mapEngine, @Coerce Object objectGroup) {
        Team.registerUnit(unit);
        MapSpawnEvents.AFTER_MAP_OBJECT_SPAWN_UNIT.invoker()
                .afterMapObjectSpawnUnit(this, mapEngine, objectGroup, unit, rustedfabricapi$getProperties());
    }

    @Unique
    private Properties rustedfabricapi$getProperties() {
        try {
            java.lang.reflect.Field field = this.getClass().getDeclaredField("properties");
            field.setAccessible(true);
            return (Properties) field.get(this);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
