package io.github.endx.iniessentials;

import io.github.endx.rustedfabricapi.api.ini.IniApplicationPhase;
import io.github.endx.rustedfabricapi.api.ini.IniExtensions;
import io.github.endx.rustedfabricapi.api.ini.IniFieldDefinition;
import io.github.endx.rustedfabricapi.api.ini.IniFieldDocumentation;
import io.github.endx.rustedfabricapi.api.ini.IniMultiplayerImpact;
import io.github.endx.rustedfabricapi.api.ini.IniSectionSelector;
import io.github.endx.rustedfabricapi.api.projectile.pattern.TurretProjectilePatternEvents;
import io.github.endx.rustedfabricapi.api.projectile.pattern.TurretProjectilePatternPlan;
import rustedwarfare.custom.CustomUnit;
import rustedwarfare.custom.TurretTemplate;
import rustedwarfare.unit.combat.TurretRuntimeState;
import rustedwarfare.util.CommonUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

/** Exact turret integration for independent CustomProjectile patterns. */
final class CustomProjectileTurretFields {
    private static final String FIELD = "projectilePattern";
    private static final Map<Object, Map<String, CustomProjectileDefinitions.Reference>> BY_METADATA =
            Collections.synchronizedMap(
                    new WeakHashMap<Object, Map<String, CustomProjectileDefinitions.Reference>>());

    private CustomProjectileTurretFields() { }

    static void register() {
        IniExtensions.register(IniFieldDefinition
                .<CustomProjectileDefinitions.Reference>builder(
                        IniEssentials.MOD_ID, "turret_projectile_pattern",
                        IniSectionSelector.prefix("turret_"), FIELD)
                .applicationPhase(IniApplicationPhase.BEFORE_STATIC_VARIABLES)
                .decoder(context -> {
                    CustomProjectileDefinitions.Reference reference =
                            CustomProjectileDefinitions.Reference.parse(context.rawValue());
                    CustomProjectileDefinitions.noteReference(reference);
                    return reference;
                })
                .applier(field -> {
                    synchronized (BY_METADATA) {
                        BY_METADATA.computeIfAbsent(field.metadata(), ignored ->
                                new LinkedHashMap<String, CustomProjectileDefinitions.Reference>())
                                .put(field.source().section(), field.value());
                    }
                    IniEssentials.activateSynchronizedRequirement();
                })
                .documentation(new IniFieldDocumentation(
                        "namespace:path/pattern",
                        "Replaces only this turret's native projectile creation with a CustomProjectile pattern while preserving native firing side effects.",
                        "仅把此炮塔的原版弹体创建替换为 CustomProjectile 弹幕，同时保留原版开火副作用。",
                        "projectilePattern: example:plasma_fan/main",
                        IniMultiplayerImpact.GAMEPLAY_SYNCED))
                .build());

        TurretProjectilePatternEvents.PLAN.register(request -> {
            CustomUnit shooter = request.shooter();
            Map<String, CustomProjectileDefinitions.Reference> fields =
                    BY_METADATA.get(shooter.unitMetadata);
            if (fields == null || shooter.unitMetadata.turretTemplates == null
                    || request.turretIndex() >= shooter.unitMetadata.turretTemplates.length) return;
            TurretTemplate turret = shooter.unitMetadata.turretTemplates[request.turretIndex()];
            CustomProjectileDefinitions.Reference reference = fields.get(turret.sectionName);
            if (reference == null) reference = fields.get("turret_" + turret.name);
            if (reference == null) return;

            CustomProjectileDefinitions.Definition definition = reference.definition();
            CustomProjectileDefinitions.CompiledPattern pattern = definition
                    .requirePattern(reference.patternName()).compileFor(shooter);
            float nativeDirection = nativeTurretDirection(shooter, request.turretIndex());
            float direction = pattern.centerDirection(shooter, nativeDirection);
            float localX = pattern.originOffsetX.evaluate(shooter);
            float localY = pattern.originOffsetY.evaluate(shooter);
            float sin = CommonUtils.fastSin(shooter.direction);
            float cos = CommonUtils.fastCos(shooter.direction);
            float worldX = cos * localY - sin * localX;
            float worldY = sin * localY + cos * localX;

            request.replace(TurretProjectilePatternPlan
                    .builder(definition.projectile(), pattern.resolve(shooter))
                    .aimMode(pattern.aimMode)
                    .centerDirection(direction)
                    .originOffset(worldX, worldY,
                            pattern.originOffsetHeight.evaluate(shooter))
                    .directionDistance(pattern.directionDistance.evaluate(shooter))
                    .build());
        });
    }

    private static float nativeTurretDirection(CustomUnit shooter, int turretIndex) {
        if (shooter.turretStates != null && turretIndex < shooter.turretStates.length) {
            TurretRuntimeState state = shooter.turretStates[turretIndex];
            if (state != null) return state.aimAngle;
        }
        return shooter.direction;
    }
}
