package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.MapMissionEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.InputStream;

@Mixin(targets = "com.corrodinggames.rts.game.b.b", remap = false)
public abstract class MapEngineLifecycleOfficialMixin {
    @Inject(method = "b(Ljava/lang/String;)Ljava/io/InputStream;", at = @At("HEAD"), cancellable = true, require = 1)
    private static void rustedfabricapi$beforeMapStreamOpen(String mapPath, CallbackInfoReturnable<InputStream> cir) {
        if (MapMissionEvents.BEFORE_MAP_STREAM_OPEN.invoker().beforeMapStreamOpen(mapPath)) {
            cir.setReturnValue(null);
        }
    }

    @Inject(
            method = "a(Ljava/io/InputStream;Z)V",
            at = @At(value = "INVOKE", target = "Ljavax/xml/parsers/DocumentBuilder;parse(Ljava/io/InputStream;)Lorg/w3c/dom/Document;"),
            cancellable = true,
            require = 1
    )
    private void rustedfabricapi$beforeTmxDocumentParse(InputStream inputStream, boolean newGame, CallbackInfo ci) {
        if (MapMissionEvents.BEFORE_TMX_DOCUMENT_PARSE.invoker().beforeTmxDocumentParse(this, inputStream, newGame)) {
            ci.cancel();
        }
    }

    @Inject(
            method = "a(Ljava/io/InputStream;Z)V",
            at = @At(value = "INVOKE", target = "Lorg/w3c/dom/Element;getElementsByTagName(Ljava/lang/String;)Lorg/w3c/dom/NodeList;", ordinal = 0),
            require = 1
    )
    private void rustedfabricapi$afterMapAttributesRead(InputStream inputStream, boolean newGame, CallbackInfo ci) {
        MapMissionEvents.AFTER_MAP_ATTRIBUTES_READ.invoker().afterMapAttributesRead(this, inputStream, newGame);
    }

    @Inject(
            method = "a(Ljava/io/InputStream;Z)V",
            at = @At(value = "INVOKE", target = "Lcom/corrodinggames/rts/game/b/e;<init>(Lcom/corrodinggames/rts/game/b/b;Lorg/w3c/dom/Element;)V", ordinal = 0),
            require = 1
    )
    private void rustedfabricapi$afterTilesetsLoaded(InputStream inputStream, boolean newGame, CallbackInfo ci) {
        MapMissionEvents.AFTER_TILESETS_LOADED.invoker().afterTilesetsLoaded(this, inputStream, newGame);
    }

    @Inject(
            method = "a(Ljava/io/InputStream;Z)V",
            at = @At(value = "INVOKE", target = "Lcom/corrodinggames/rts/game/b/i;<init>(Lorg/w3c/dom/Element;Lcom/corrodinggames/rts/game/b/b;)V", ordinal = 0),
            require = 1
    )
    private void rustedfabricapi$afterMapLayersLoaded(InputStream inputStream, boolean newGame, CallbackInfo ci) {
        MapMissionEvents.AFTER_MAP_LAYERS_LOADED.invoker().afterMapLayersLoaded(this, inputStream, newGame);
    }

    @Inject(
            method = "a(Ljava/io/InputStream;Z)V",
            at = @At(value = "INVOKE", target = "Lcom/corrodinggames/rts/game/b/j;a()V"),
            require = 1
    )
    private void rustedfabricapi$afterMapObjectGroupsLoaded(InputStream inputStream, boolean newGame, CallbackInfo ci) {
        MapMissionEvents.AFTER_MAP_OBJECT_GROUPS_LOADED.invoker().afterMapObjectGroupsLoaded(this);
    }
}
