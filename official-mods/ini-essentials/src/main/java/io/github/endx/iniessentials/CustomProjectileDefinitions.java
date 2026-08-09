package io.github.endx.iniessentials;

import io.github.endx.rustedfabricapi.api.event.RustedIniEvents;
import io.github.endx.rustedfabricapi.api.projectile.pattern.ProjectilePatternSpec;
import io.github.endx.rustedfabricapi.api.projectile.pattern.ProjectilePatternType;
import io.github.endx.rustedfabricapi.api.projectile.spawn.ProjectileAimMode;
import io.github.endx.rustedfabricapi.api.util.Identifier;
import rustedwarfare.custom.CustomProjectileTemplate;
import rustedwarfare.custom.CustomUnit;
import rustedwarfare.custom.CustomUnitMetadata;
import rustedwarfare.mod.ModInfo;
import rustedwarfare.util.UnitConfig;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

/** Loader and registry for independent {@code class: CustomProjectile} INI assets. */
final class CustomProjectileDefinitions {
    private static final int PROBE_BYTES = 65536;
    private static final Map<Identifier, Definition> DEFINITIONS =
            new LinkedHashMap<Identifier, Definition>();
    private static final List<Reference> REFERENCES = new ArrayList<Reference>();

    private CustomProjectileDefinitions() { }

    static void register() {
        RustedIniEvents.BEFORE_PARSE_STREAM.register(CustomProjectileDefinitions::inspectStream);
    }

    static synchronized void beginReload() {
        DEFINITIONS.clear();
        REFERENCES.clear();
    }

    static synchronized void noteReference(Reference reference) {
        REFERENCES.add(reference);
    }

    static synchronized void validateReferences() {
        for (Reference reference : REFERENCES) {
            Definition definition = DEFINITIONS.get(reference.definitionId);
            if (definition == null) {
                throw new IllegalArgumentException(
                        "unknown CustomProjectile: " + reference.definitionId);
            }
            definition.requirePattern(reference.patternName);
        }
    }

    static synchronized Definition require(Identifier id) {
        Definition result = DEFINITIONS.get(id);
        if (result == null) throw new IllegalArgumentException("unknown CustomProjectile: " + id);
        return result;
    }

    private static void inspectStream(RustedIniEvents.ParseStreamContext context) {
        InputStream input = context.inputStream();
        if (input == null || !input.markSupported()) return;
        try {
            input.mark(PROBE_BYTES + 1);
            byte[] probe = input.readNBytes(PROBE_BYTES);
            input.reset();
            if (!isCustomProjectile(probe)) return;

            byte[] bytes = input.readAllBytes();
            UnitConfig config = new UnitConfig(new ByteArrayInputStream(bytes), context.unitId());
            Definition definition = parse(context, config);
            synchronized (CustomProjectileDefinitions.class) {
                if (DEFINITIONS.put(definition.id, definition) != null) {
                    throw new IllegalArgumentException(
                            "duplicate CustomProjectile ID: " + definition.id);
                }
            }
            IniEssentials.activateSynchronizedRequirement();
            context.cancelWith(null);
        } catch (IOException failure) {
            throw new IllegalStateException("failed to read CustomProjectile " + context.unitId(), failure);
        }
    }

    private static boolean isCustomProjectile(byte[] probe) {
        String text = new String(probe, StandardCharsets.UTF_8);
        boolean inCore = false;
        for (String rawLine : text.split("\\r?\\n")) {
            String line = rawLine.trim().toLowerCase(Locale.ROOT);
            if (line.startsWith("\ufeff")) line = line.substring(1).trim();
            if (line.startsWith("[") && line.endsWith("]")) {
                inCore = "[core]".equals(line);
                continue;
            }
            if (!inCore) continue;
            int separator = line.indexOf(':');
            if (separator < 0) continue;
            String key = line.substring(0, separator).trim();
            String value = line.substring(separator + 1).trim();
            int comment = value.indexOf('#');
            if (comment >= 0) value = value.substring(0, comment).trim();
            if ("class".equals(key) && "customprojectile".equals(value)) return true;
        }
        return false;
    }

    private static Definition parse(RustedIniEvents.ParseStreamContext context, UnitConfig config) {
        String className = required(config, "core", "class");
        if (!"customprojectile".equalsIgnoreCase(className)) {
            throw new IllegalArgumentException("expected class: CustomProjectile");
        }
        int schemaVersion = config.getInteger("core", "schemaVersion", Integer.valueOf(1));
        if (schemaVersion != 1) {
            throw new IllegalArgumentException("unsupported CustomProjectile schemaVersion: "
                    + schemaVersion);
        }
        Identifier id = Identifier.parse(required(config, "core", "name").toLowerCase(Locale.ROOT));
        if (!config.hasSection("projectile")) {
            throw new IllegalArgumentException(id + " requires [projectile]");
        }
        rejectDeferredNativeLinks(config, id);

        CustomUnitMetadata metadata = new CustomUnitMetadata();
        metadata.configPath = context.unitId();
        metadata.resolvedConfigPath = context.unitId();
        metadata.resourceLoadPath = context.unitId();
        metadata.modInfo = context.modInfo() instanceof ModInfo ? (ModInfo) context.modInfo() : null;
        metadata.modRootPath = context.resourceRoot();
        metadata.templateRootPath = context.templateRoot();
        metadata.internalName = id.toString();

        CustomProjectileTemplate projectile = new CustomProjectileTemplate();
        projectile.name = id.path();
        projectile.projectileIndex = 0;
        projectile.unitMetadata = metadata;
        CustomProjectileTemplate.parseFromConfig(projectile, metadata, config, "projectile");
        metadata.projectileTemplates = new CustomProjectileTemplate[]{projectile};

        LinkedHashMap<String, PatternTemplate> patterns = new LinkedHashMap<String, PatternTemplate>();
        for (Object rawSection : config.getNonMetaSectionsWithPrefix("pattern_")) {
            String section = String.valueOf(rawSection);
            String patternName = normalizePatternName(section.substring("pattern_".length()));
            PatternTemplate pattern = PatternTemplate.parse(config, section);
            if (patterns.put(patternName, pattern) != null) {
                throw new IllegalArgumentException(id + " has duplicate pattern: " + patternName);
            }
        }
        if (patterns.isEmpty()) {
            patterns.put("main", PatternTemplate.defaultSingle());
        }
        return new Definition(id, projectile, Collections.unmodifiableMap(patterns));
    }

    private static void rejectDeferredNativeLinks(UnitConfig config, Identifier id) {
        List<String> unsupported = new ArrayList<String>();
        for (String key : new String[]{"spawnProjectilesOnCreate", "spawnProjectilesOnExplode",
                "spawnProjectilesOnEndOfLife"}) {
            if (config.hasKey("projectile", key)) unsupported.add(key);
        }
        if (!unsupported.isEmpty()) {
            throw new IllegalArgumentException(id + " cannot yet use deferred projectile links: "
                    + unsupported);
        }
    }

    private static String required(UnitConfig config, String section, String key) {
        String result = config.getString(section, key, null);
        if (result == null || result.trim().isEmpty()) {
            throw new IllegalArgumentException("[" + section + "] requires " + key);
        }
        return result.trim();
    }

    private static String optional(UnitConfig config, String section, String key,
                                   String fallback) {
        String result = config.getString(section, key, null);
        return result != null ? result.trim() : fallback;
    }

    private static String optionalNullable(UnitConfig config, String section, String key) {
        String result = config.getString(section, key, null);
        return result != null ? result.trim() : null;
    }

    private static String normalizePatternName(String raw) {
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (!value.matches("[a-z0-9_.-]+")) {
            throw new IllegalArgumentException("invalid pattern name: " + raw);
        }
        return value;
    }

    static final class Definition {
        private final Identifier id;
        private final CustomProjectileTemplate projectile;
        private final Map<String, PatternTemplate> patterns;

        private Definition(Identifier id, CustomProjectileTemplate projectile,
                           Map<String, PatternTemplate> patterns) {
            this.id = id;
            this.projectile = projectile;
            this.patterns = patterns;
        }

        CustomProjectileTemplate projectile() { return projectile; }

        PatternTemplate requirePattern(String name) {
            PatternTemplate result = patterns.get(normalizePatternName(name));
            if (result == null) {
                throw new IllegalArgumentException(id + " has no pattern: " + name);
            }
            return result;
        }
    }

    static final class Reference {
        private final Identifier definitionId;
        private final String patternName;

        private Reference(Identifier definitionId, String patternName) {
            this.definitionId = definitionId;
            this.patternName = patternName;
        }

        static Reference parse(String raw) {
            String value = raw != null ? raw.trim().toLowerCase(Locale.ROOT) : "";
            int slash = value.lastIndexOf('/');
            if (slash <= value.indexOf(':') || slash == value.length() - 1) {
                throw new IllegalArgumentException(
                        "CustomProjectile reference must use namespace:path/pattern: " + raw);
            }
            return new Reference(Identifier.parse(value.substring(0, slash)),
                    normalizePatternName(value.substring(slash + 1)));
        }

        Definition definition() { return require(definitionId); }
        String patternName() { return patternName; }
    }

    static final class PatternTemplate {
        private final ProjectilePatternType type;
        private final ProjectileAimMode aimMode;
        private final String count;
        private final String centerDirection;
        private final String startAngle;
        private final String sweepAngle;
        private final String originSpacing;
        private final String lineAngleOffset;
        private final String originOffsetX;
        private final String originOffsetY;
        private final String originOffsetHeight;
        private final String directionDistance;
        private final Map<Object, CompiledPattern> compiled =
                Collections.synchronizedMap(new WeakHashMap<Object, CompiledPattern>());

        private PatternTemplate(ProjectilePatternType type, ProjectileAimMode aimMode,
                                String count, String centerDirection, String startAngle,
                                String sweepAngle, String originSpacing, String lineAngleOffset,
                                String originOffsetX, String originOffsetY,
                                String originOffsetHeight, String directionDistance) {
            this.type = type;
            this.aimMode = aimMode;
            this.count = count;
            this.centerDirection = centerDirection;
            this.startAngle = startAngle;
            this.sweepAngle = sweepAngle;
            this.originSpacing = originSpacing;
            this.lineAngleOffset = lineAngleOffset;
            this.originOffsetX = originOffsetX;
            this.originOffsetY = originOffsetY;
            this.originOffsetHeight = originOffsetHeight;
            this.directionDistance = directionDistance;
        }

        static PatternTemplate parse(UnitConfig config, String section) {
            ProjectilePatternType type = enumValue(ProjectilePatternType.class,
                    optional(config, section, "type", "single"), "pattern type");
            return new PatternTemplate(
                    type,
                    enumValue(ProjectileAimMode.class,
                            optional(config, section, "aimMode", "direction"), "aimMode"),
                    optional(config, section, "count", "1"),
                    optionalNullable(config, section, "centerDirection"),
                    optional(config, section, "startAngle", "0"),
                    optional(config, section, "sweepAngle",
                            type == ProjectilePatternType.RING ? "360" : "0"),
                    optional(config, section, "originSpacing", "0"),
                    optional(config, section, "lineAngleOffset", "90"),
                    optional(config, section, "originOffsetX", "0"),
                    optional(config, section, "originOffsetY", "0"),
                    optional(config, section, "originOffsetHeight", "0"),
                    optional(config, section, "directionDistance", "100000"));
        }

        static PatternTemplate defaultSingle() {
            return new PatternTemplate(ProjectilePatternType.SINGLE, ProjectileAimMode.DIRECTION,
                    "1", null, "0", "0", "0", "90",
                    "0", "0", "0", "100000");
        }

        CompiledPattern compileFor(CustomUnit actor) {
            Object metadata = actor.unitMetadata;
            CompiledPattern result = compiled.get(metadata);
            if (result != null) return result;
            result = new CompiledPattern(type, aimMode,
                    NumericExpression.compile(metadata, count),
                    centerDirection != null
                            ? NumericExpression.compile(metadata, centerDirection) : null,
                    NumericExpression.compile(metadata, startAngle),
                    NumericExpression.compile(metadata, sweepAngle),
                    NumericExpression.compile(metadata, originSpacing),
                    NumericExpression.compile(metadata, lineAngleOffset),
                    NumericExpression.compile(metadata, originOffsetX),
                    NumericExpression.compile(metadata, originOffsetY),
                    NumericExpression.compile(metadata, originOffsetHeight),
                    NumericExpression.compile(metadata, directionDistance));
            compiled.put(metadata, result);
            return result;
        }

        private static <E extends Enum<E>> E enumValue(Class<E> type, String raw, String label) {
            try {
                return Enum.valueOf(type, raw.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException failure) {
                throw new IllegalArgumentException("unknown " + label + ": " + raw);
            }
        }
    }

    static final class CompiledPattern {
        final ProjectilePatternType type;
        final ProjectileAimMode aimMode;
        final NumericExpression count, centerDirection, startAngle, sweepAngle;
        final NumericExpression originSpacing, lineAngleOffset;
        final NumericExpression originOffsetX, originOffsetY, originOffsetHeight;
        final NumericExpression directionDistance;

        private CompiledPattern(ProjectilePatternType type, ProjectileAimMode aimMode,
                                NumericExpression count, NumericExpression centerDirection,
                                NumericExpression startAngle, NumericExpression sweepAngle,
                                NumericExpression originSpacing, NumericExpression lineAngleOffset,
                                NumericExpression originOffsetX, NumericExpression originOffsetY,
                                NumericExpression originOffsetHeight,
                                NumericExpression directionDistance) {
            this.type = type; this.aimMode = aimMode; this.count = count;
            this.centerDirection = centerDirection; this.startAngle = startAngle;
            this.sweepAngle = sweepAngle; this.originSpacing = originSpacing;
            this.lineAngleOffset = lineAngleOffset; this.originOffsetX = originOffsetX;
            this.originOffsetY = originOffsetY; this.originOffsetHeight = originOffsetHeight;
            this.directionDistance = directionDistance;
        }

        ProjectilePatternSpec resolve(CustomUnit actor) {
            int resolvedCount = Math.round(count.evaluate(actor));
            return ProjectilePatternSpec.builder(type)
                    .count(resolvedCount)
                    .startAngle(startAngle.evaluate(actor))
                    .sweepAngle(sweepAngle.evaluate(actor))
                    .originSpacing(originSpacing.evaluate(actor))
                    .lineAngleOffset(lineAngleOffset.evaluate(actor))
                    .build();
        }

        float centerDirection(CustomUnit actor, float fallback) {
            return centerDirection != null ? centerDirection.evaluate(actor) : fallback;
        }
    }
}
