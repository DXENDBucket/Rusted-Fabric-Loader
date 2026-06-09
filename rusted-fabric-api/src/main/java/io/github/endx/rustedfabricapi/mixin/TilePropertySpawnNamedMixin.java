package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.MapSpawnEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.w3c.dom.Element;

import java.util.Properties;

@Mixin(targets = "rustedwarfare.map.Tileset", remap = false)
public abstract class TilePropertySpawnNamedMixin {
    @Redirect(
            method = "parseTileProperties(Lorg/w3c/dom/Element;)V",
            at = @At(value = "INVOKE", target = "Ljava/util/Properties;setProperty(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;"),
            require = 1
    )
    private Object rustedfabricapi$tilePropertySpawnUnit(Properties properties, String propertyName, String propertyValue, Element tilesetElement) {
        if (!rustedfabricapi$isSpawnUnitProperty(propertyName)) {
            return properties.setProperty(propertyName, propertyValue);
        }
        if (MapSpawnEvents.BEFORE_TILE_PROPERTY_SPAWN_UNIT.invoker()
                .beforeTilePropertySpawnUnit(this, properties, propertyName, propertyValue)) {
            return null;
        }

        Object previous = properties.setProperty(propertyName, propertyValue);
        MapSpawnEvents.AFTER_TILE_PROPERTY_SPAWN_UNIT.invoker()
                .afterTilePropertySpawnUnit(this, properties, propertyName, propertyValue);
        return previous;
    }

    private static boolean rustedfabricapi$isSpawnUnitProperty(String propertyName) {
        return "unit".equalsIgnoreCase(propertyName) || "customUnit".equalsIgnoreCase(propertyName);
    }
}
