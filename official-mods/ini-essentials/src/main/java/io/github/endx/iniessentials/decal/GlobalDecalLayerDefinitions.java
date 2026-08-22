package io.github.endx.iniessentials.decal;

import io.github.endx.iniessentials.IniEssentials;
import io.github.endx.rustedfabricapi.api.client.event.WorldRenderEvents;
import io.github.endx.rustedfabricapi.api.client.render.Decals;
import io.github.endx.rustedfabricapi.api.client.render.WorldLayerDrawContext;
import io.github.endx.rustedfabricapi.api.ini.IniApplicationPhase;
import io.github.endx.rustedfabricapi.api.ini.IniExtensionKind;
import io.github.endx.rustedfabricapi.api.ini.IniExtensions;
import io.github.endx.rustedfabricapi.api.ini.IniFieldDefinition;
import io.github.endx.rustedfabricapi.api.ini.IniFieldDocumentation;
import io.github.endx.rustedfabricapi.api.ini.IniMultiplayerImpact;
import io.github.endx.rustedfabricapi.api.ini.IniSectionSelector;
import rustedwarfare.custom.CustomUnit;
import rustedwarfare.custom.CustomUnitMetadata;
import rustedwarfare.custom.graphics.DecalBehavior;
import rustedwarfare.custom.graphics.DecalLayer;
import rustedwarfare.custom.graphics.DecalTemplate;
import rustedwarfare.framework.GameObject;
import rustedwarfare.util.RwArrayList;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.WeakHashMap;

/** Adds world-global draw stages to the native per-unit Decal layer field. */
public final class GlobalDecalLayerDefinitions {
    private static final String PREFIX = "decal_";
    private static final Map<CustomUnitMetadata, LayerLists> BY_METADATA =
            Collections.synchronizedMap(new WeakHashMap<CustomUnitMetadata, LayerLists>());

    private GlobalDecalLayerDefinitions() { }

    public static void register() {
        IniExtensions.register(IniFieldDefinition
                .<GlobalLayer>builder(IniEssentials.MOD_ID, "global_decal_layer",
                        IniSectionSelector.prefix(PREFIX), "layer")
                .kind(IniExtensionKind.EXTENDED_VALUE)
                .applicationPhase(IniApplicationPhase.AFTER_METADATA_PARSED)
                .activatesWhen(context -> GlobalLayer.parse(context.rawValue()) != null)
                .decoder(context -> GlobalLayer.require(context.rawValue()))
                // Keep the native parser and all native Decal fields intact, but prevent its
                // per-unit render behavior from drawing this template a second time.
                .nativeFallback((context, value) -> "inactive")
                .applier(field -> add((CustomUnitMetadata) field.metadata(),
                        field.source().section(), field.value()))
                .documentation(new IniFieldDocumentation(
                        "native Decal layer or underAllUnits|overAllUnits",
                        "Draws this unit Decal in a global world pass below or above every unit, instead of only ordering it around its own unit body.",
                        "把这个单位贴画放进全局世界绘制批次，绘制在所有单位下方或上方；它不再只围绕自己绑定的单位排序。",
                        "layer: underAllUnits",
                        IniMultiplayerImpact.CLIENT_ONLY))
                .build());
        WorldRenderEvents.BEFORE_UNITS.register(context ->
                draw(context, GlobalLayer.UNDER_ALL_UNITS));
        WorldRenderEvents.AFTER_UNITS.register(context ->
                draw(context, GlobalLayer.OVER_ALL_UNITS));
    }

    private static void add(CustomUnitMetadata metadata, String section, GlobalLayer layer) {
        if (metadata == null) {
            throw new IllegalArgumentException("global Decal layer requires custom-unit metadata");
        }
        DecalTemplate template = Decals.require(metadata,
                section.substring(PREFIX.length()));
        LayerLists lists = BY_METADATA.get(metadata);
        if (lists == null) {
            lists = new LayerLists();
            BY_METADATA.put(metadata, lists);
        }
        RwArrayList decals = lists.get(layer);
        decals.add(template);
        Collections.sort(decals);
    }

    private static void draw(WorldLayerDrawContext context, GlobalLayer layer) {
        int count = context.visibleObjectCount();
        for (int index = 0; index < count; index++) {
            GameObject object = context.visibleObject(index);
            if (!(object instanceof CustomUnit)) continue;
            CustomUnit unit = (CustomUnit) object;
            LayerLists lists = BY_METADATA.get(unit.unitMetadata);
            if (lists == null) continue;
            RwArrayList decals = lists.get(layer);
            if (decals.isEmpty()) continue;
            // INACTIVE prevents native per-unit drawing. Here it intentionally means a normal
            // non-shadow image while the surrounding world event supplies the global ordering.
            DecalBehavior.drawLayerAtPoint(unit, context.delta(), DecalLayer.INACTIVE,
                    decals, null);
        }
    }

    private enum GlobalLayer {
        UNDER_ALL_UNITS("underAllUnits"),
        OVER_ALL_UNITS("overAllUnits");

        final String iniName;

        GlobalLayer(String iniName) {
            this.iniName = iniName;
        }

        static GlobalLayer parse(String source) {
            if (source == null) return null;
            String value = source.trim();
            for (GlobalLayer layer : values()) {
                if (layer.iniName.equalsIgnoreCase(value)) return layer;
            }
            return null;
        }

        static GlobalLayer require(String source) {
            GlobalLayer result = parse(source);
            if (result == null) {
                throw new IllegalArgumentException(
                        "layer must be underAllUnits or overAllUnits: " + source);
            }
            return result;
        }
    }

    private static final class LayerLists {
        private final EnumMap<GlobalLayer, RwArrayList> values =
                new EnumMap<GlobalLayer, RwArrayList>(GlobalLayer.class);

        LayerLists() {
            for (GlobalLayer layer : GlobalLayer.values()) {
                values.put(layer, new RwArrayList());
            }
        }

        RwArrayList get(GlobalLayer layer) {
            return values.get(layer);
        }
    }
}
