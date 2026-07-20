package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.MapMissionEvents;
import io.github.endx.rustedfabricapi.api.map.MapObjects;
import io.github.endx.rustedfabricapi.api.map.event.MapObjectEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.InputStream;

@Mixin(targets = "rustedwarfare.map.MapEngine", remap = false)
public abstract class MapEngineLifecycleNamedMixin {
    @Inject(method = "openMapInputStream(Ljava/lang/String;)Ljava/io/InputStream;", at = @At("HEAD"), cancellable = true, require = 1)
    private static void rustedfabricapi$beforeMapStreamOpen(String mapPath, CallbackInfoReturnable<InputStream> cir) {
        boolean cancelled = MapMissionEvents.BEFORE_MAP_STREAM_OPEN.invoker().beforeMapStreamOpen(mapPath);
        cancelled |= io.github.endx.rustedfabricapi.api.map.event.MapLifecycleEvents.BEFORE_MAP_STREAM_OPEN.invoker()
                .beforeMapStreamOpen(mapPath);
        if (cancelled) {
            cir.setReturnValue(null);
        }
    }

    @Inject(
            method = "loadMapFromStream(Ljava/io/InputStream;Z)V",
            at = @At(value = "INVOKE", target = "Ljavax/xml/parsers/DocumentBuilder;parse(Ljava/io/InputStream;)Lorg/w3c/dom/Document;"),
            cancellable = true,
            require = 1
    )
    private void rustedfabricapi$beforeTmxDocumentParse(InputStream inputStream, boolean newGame, CallbackInfo ci) {
        boolean cancelled = MapMissionEvents.BEFORE_TMX_DOCUMENT_PARSE.invoker()
                .beforeTmxDocumentParse(this, inputStream, newGame);
        cancelled |= io.github.endx.rustedfabricapi.api.map.event.MapLifecycleEvents.BEFORE_TMX_DOCUMENT_PARSE.invoker()
                .beforeDocumentParse((rustedwarfare.map.MapEngine) (Object) this,
                        inputStream, newGame);
        if (cancelled) {
            ci.cancel();
        }
    }

    @Inject(
            method = "loadMapFromStream(Ljava/io/InputStream;Z)V",
            at = @At(value = "INVOKE", target = "Lorg/w3c/dom/Element;getElementsByTagName(Ljava/lang/String;)Lorg/w3c/dom/NodeList;", ordinal = 0),
            require = 1
    )
    private void rustedfabricapi$afterMapAttributesRead(InputStream inputStream, boolean newGame, CallbackInfo ci) {
        MapMissionEvents.AFTER_MAP_ATTRIBUTES_READ.invoker().afterMapAttributesRead(this, inputStream, newGame);
        io.github.endx.rustedfabricapi.api.map.event.MapLifecycleEvents.AFTER_MAP_ATTRIBUTES_READ.invoker()
                .afterPhase((rustedwarfare.map.MapEngine) (Object) this, inputStream, newGame);
    }

    @Inject(
            method = "loadMapFromStream(Ljava/io/InputStream;Z)V",
            at = @At(value = "INVOKE", target = "Lrustedwarfare/map/MapLayer;<init>(Lrustedwarfare/map/MapEngine;Lorg/w3c/dom/Element;)V", ordinal = 0),
            require = 1
    )
    private void rustedfabricapi$afterTilesetsLoaded(InputStream inputStream, boolean newGame, CallbackInfo ci) {
        MapMissionEvents.AFTER_TILESETS_LOADED.invoker().afterTilesetsLoaded(this, inputStream, newGame);
        io.github.endx.rustedfabricapi.api.map.event.MapLifecycleEvents.AFTER_TILESETS_LOADED.invoker()
                .afterPhase((rustedwarfare.map.MapEngine) (Object) this, inputStream, newGame);
    }

    @Inject(
            method = "loadMapFromStream(Ljava/io/InputStream;Z)V",
            at = @At(value = "INVOKE", target = "Lrustedwarfare/map/MapObjectGroup;<init>(Lorg/w3c/dom/Element;Lrustedwarfare/map/MapEngine;)V", ordinal = 0),
            require = 1
    )
    private void rustedfabricapi$afterMapLayersLoaded(InputStream inputStream, boolean newGame, CallbackInfo ci) {
        MapMissionEvents.AFTER_MAP_LAYERS_LOADED.invoker().afterMapLayersLoaded(this, inputStream, newGame);
        io.github.endx.rustedfabricapi.api.map.event.MapLifecycleEvents.AFTER_MAP_LAYERS_LOADED.invoker()
                .afterPhase((rustedwarfare.map.MapEngine) (Object) this, inputStream, newGame);
    }

    @Inject(
            method = "loadMapFromStream(Ljava/io/InputStream;Z)V",
            at = @At(value = "INVOKE", target = "Lrustedwarfare/map/Tileset;a()V"),
            require = 1
    )
    private void rustedfabricapi$afterMapObjectGroupsLoaded(InputStream inputStream, boolean newGame, CallbackInfo ci) {
        MapMissionEvents.AFTER_MAP_OBJECT_GROUPS_LOADED.invoker().afterMapObjectGroupsLoaded(this);
        io.github.endx.rustedfabricapi.api.map.event.MapLifecycleEvents.AFTER_MAP_OBJECT_GROUPS_LOADED.invoker()
                .afterObjectGroupsLoaded((rustedwarfare.map.MapEngine) (Object) this);
        MapObjectEvents.AFTER_LOAD.invoker().afterLoad(
                MapObjects.snapshot((rustedwarfare.map.MapEngine) (Object) this));
    }
}
