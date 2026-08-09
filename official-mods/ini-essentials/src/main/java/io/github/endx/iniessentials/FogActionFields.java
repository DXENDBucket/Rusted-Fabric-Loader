package io.github.endx.iniessentials;

import io.github.endx.rustedfabricapi.api.fog.FogMode;
import io.github.endx.rustedfabricapi.api.fog.FogOfWar;
import io.github.endx.rustedfabricapi.api.fog.FogOperation;
import io.github.endx.rustedfabricapi.api.fog.FogSources;
import io.github.endx.rustedfabricapi.api.fog.FogState;
import io.github.endx.rustedfabricapi.api.geometry.GeometryMask;
import io.github.endx.rustedfabricapi.api.geometry.GeometryMasks;
import io.github.endx.rustedfabricapi.api.ini.IniFieldDefinition;
import io.github.endx.rustedfabricapi.api.ini.IniFieldDocumentation;
import io.github.endx.rustedfabricapi.api.ini.IniExtensions;
import io.github.endx.rustedfabricapi.api.ini.IniMultiplayerImpact;
import io.github.endx.rustedfabricapi.api.ini.IniSectionSelector;
import io.github.endx.rustedfabricapi.api.ini.action.IniActionEffectDefinition;
import io.github.endx.rustedfabricapi.api.ini.action.IniActionEffects;
import io.github.endx.rustedfabricapi.api.ini.action.IniActionExecutionContext;
import io.github.endx.rustedfabricapi.api.world.WorldPoint;
import rustedwarfare.game.Team;
import rustedwarfare.util.UnitConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

final class FogActionFields {
    private static final String PREFIX = "fog_";
    private static final Map<Object, Map<String, Template>> BY_METADATA =
            Collections.synchronizedMap(new WeakHashMap<Object, Map<String, Template>>());

    private FogActionFields() { }

    static void register() {
        IniExtensions.register(IniFieldDefinition
                .<String>builder(IniEssentials.MOD_ID, "fog_definition",
                        IniSectionSelector.prefix(PREFIX), "operation")
                .decoder(context -> context.rawValue().trim())
                .validator((context, value) -> parseOperation(value))
                .applier(field -> {
                    IniEssentials.activateSynchronizedRequirement();
                    parseAndStore(field.metadata(), (UnitConfig) field.unitConfig(),
                            field.source().section());
                })
                .documentation(new IniFieldDocumentation(
                        "reveal|explore|conceal|shroud",
                        "Declares a reusable geometry-backed fog operation.",
                        "声明一个可复用、由几何遮罩驱动的战争迷雾操作。",
                        "[fog_revealFront]\noperation: reveal\ngeometry: front\nteam: own",
                        IniMultiplayerImpact.GAMEPLAY_SYNCED))
                .build());

        IniActionEffects.register(IniActionEffectDefinition
                .<List<Template>>builder(IniEssentials.MOD_ID, "apply_fog", "applyFog")
                .decoder(context -> {
                    IniEssentials.activateSynchronizedRequirement();
                    ArrayList<Template> result = new ArrayList<Template>();
                    for (String raw : context.rawValue().split(",")) {
                        result.add(require(context.metadata(), raw));
                    }
                    if (result.isEmpty()) throw new IllegalArgumentException("applyFog requires a fog name");
                    return Collections.unmodifiableList(result);
                })
                .handler((context, templates) -> {
                    for (Template template : templates) template.execute(context);
                })
                .documentation(new IniFieldDocumentation(
                        "fog name[,fog name...]",
                        "Applies one or more [fog_NAME] definitions from an action.",
                        "从动作中执行一个或多个 [fog_NAME] 定义。",
                        "applyFog: revealFront",
                        IniMultiplayerImpact.GAMEPLAY_SYNCED))
                .build());

        IniActionEffects.register(IniActionEffectDefinition
                .<FogMode>builder(IniEssentials.MOD_ID, "set_fog_mode", "setFogMode")
                .priority(100)
                .decoder(context -> {
                    IniEssentials.activateSynchronizedRequirement();
                    return parseMode(context.rawValue());
                })
                .handler((context, mode) -> FogOfWar.setMode(mode, FogState.UNEXPLORED))
                .documentation(new IniFieldDocumentation(
                        "off|basic|los",
                        "Changes the global match fog mode; newly created fog maps start unexplored.",
                        "修改整局的迷雾模式；新建的队伍雾图会从完全未探索开始。",
                        "setFogMode: los",
                        IniMultiplayerImpact.GAMEPLAY_SYNCED))
                .build());
    }

    private static void parseAndStore(Object metadata, UnitConfig config, String section) {
        String name = section.substring(PREFIX.length());
        Template template = new Template(metadata, name,
                required(config, section, "geometry"),
                parseOperation(required(config, section, "operation")),
                TeamSelector.parse(optional(config, section, "team", "own")),
                ActionPositionReference.compile(metadata,
                        optional(config, section, "anchor", "self")),
                NumericExpression.compile(metadata, optional(config, section, "duration", "0")),
                BooleanExpression.compile(metadata,
                        optional(config, section, "follow", "true")));
        Map<String, Template> definitions = BY_METADATA.get(metadata);
        if (definitions == null) {
            definitions = new LinkedHashMap<String, Template>();
            BY_METADATA.put(metadata, definitions);
        }
        String key = normalize(name);
        if (definitions.put(key, template) != null) {
            throw new IllegalArgumentException("duplicate fog definition: " + name);
        }
    }

    private static Template require(Object metadata, String name) {
        Map<String, Template> definitions = BY_METADATA.get(metadata);
        Template result = definitions != null ? definitions.get(normalize(name)) : null;
        if (result == null) throw new IllegalArgumentException("unknown fog definition: " + name);
        return result;
    }

    private static FogOperation parseOperation(String raw) {
        try {
            return FogOperation.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException("unknown fog operation: " + raw);
        }
    }

    private static FogMode parseMode(String raw) {
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if ("off".equals(value)) return FogMode.OFF;
        if ("basic".equals(value)) return FogMode.BASIC;
        if ("los".equals(value) || "line_of_sight".equals(value)) return FogMode.LINE_OF_SIGHT;
        throw new IllegalArgumentException("unknown fog mode: " + raw);
    }

    private static String required(UnitConfig config, String section, String key) {
        String value = config.getString(section, key, null);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("[" + section + "] requires " + key);
        }
        return value.trim();
    }

    private static String optional(UnitConfig config, String section, String key, String fallback) {
        String value = config.getString(section, key, null);
        return value != null ? value.trim() : fallback;
    }

    private static String normalize(String raw) {
        String value = raw != null ? raw.trim().toLowerCase(Locale.ROOT) : "";
        if (value.isEmpty()) throw new IllegalArgumentException("definition name must not be empty");
        return value;
    }

    private static final class Template {
        private final Object metadata;
        private final String name;
        private final String geometry;
        private final FogOperation operation;
        private final TeamSelector teamSelector;
        private final ActionPositionReference anchor;
        private final NumericExpression duration;
        private final BooleanExpression follow;

        private Template(Object metadata, String name, String geometry,
                         FogOperation operation, TeamSelector teamSelector,
                         ActionPositionReference anchor, NumericExpression duration,
                         BooleanExpression follow) {
            this.metadata = metadata; this.name = name; this.geometry = geometry;
            this.operation = operation; this.teamSelector = teamSelector;
            this.anchor = anchor; this.duration = duration; this.follow = follow;
        }

        private void execute(IniActionExecutionContext context) {
            if (FogOfWar.mode() == FogMode.OFF) return;
            float seconds = duration.evaluate(context.actor());
            List<Team> teams = teamSelector.resolve(context.actor().team);
            GeometryMask initial = worldMask(context);
            for (Team team : teams) {
                if (team.fogOfWarMap == null) continue;
                FogOfWar.apply(team, initial, operation);
                if (seconds == 0.0F) continue;
                final GeometryMask snapshot = initial;
                FogSources.add(team, operation,
                        seconds < 0.0F ? FogSources.PERMANENT : seconds * 60.0F,
                        !follow.isStaticFalse(),
                        () -> follow.evaluate(context.actor())
                                ? worldMask(context) : snapshot);
            }
        }

        private GeometryMask worldMask(IniActionExecutionContext context) {
            GeometryMask local = GeometryDefinitions.require(metadata, geometry)
                    .resolve(metadata, context.actor());
            WorldPoint origin = anchor.require(context, "fog anchor");
            return GeometryMasks.transform(local, origin.x(), origin.y(), 0.0F);
        }

        @Override public String toString() { return "FogTemplate{" + name + '}'; }
    }

    private static final class TeamSelector {
        private enum Kind { OWN, ALLIES, OWN_AND_ALLIES, ENEMIES, ALL, NEUTRAL, ID }
        private final Kind kind;
        private final int id;
        private TeamSelector(Kind kind, int id) { this.kind = kind; this.id = id; }
        private static TeamSelector parse(String raw) {
            String value = raw.trim().toLowerCase(Locale.ROOT).replace("_", "");
            if (value.startsWith("team:")) {
                return new TeamSelector(Kind.ID, Integer.parseInt(value.substring(5)));
            }
            if ("own".equals(value)) return new TeamSelector(Kind.OWN, 0);
            if ("allies".equals(value)) return new TeamSelector(Kind.ALLIES, 0);
            if ("ownandallies".equals(value)) return new TeamSelector(Kind.OWN_AND_ALLIES, 0);
            if ("enemies".equals(value)) return new TeamSelector(Kind.ENEMIES, 0);
            if ("all".equals(value)) return new TeamSelector(Kind.ALL, 0);
            if ("neutral".equals(value)) return new TeamSelector(Kind.NEUTRAL, 0);
            throw new IllegalArgumentException("unknown fog team selector: " + raw);
        }
        private List<Team> resolve(Team owner) {
            if (kind == Kind.OWN) return Collections.singletonList(owner);
            if (kind == Kind.NEUTRAL) return Collections.singletonList(Team.i);
            if (kind == Kind.ID) {
                Team team = Team.getTeamById(id);
                if (team == null) throw new IllegalArgumentException("fog team does not exist: " + id);
                return Collections.singletonList(team);
            }
            ArrayList<Team> result = new ArrayList<Team>();
            for (Object value : Team.b(false)) {
                if (!(value instanceof Team)) continue;
                Team team = (Team) value;
                if (kind == Kind.ALL
                        || (kind == Kind.ALLIES && team != owner && owner.isAlly(team))
                        || (kind == Kind.OWN_AND_ALLIES && (team == owner || owner.isAlly(team)))
                        || (kind == Kind.ENEMIES && owner.isEnemy(team))) result.add(team);
            }
            return Collections.unmodifiableList(result);
        }
    }
}
