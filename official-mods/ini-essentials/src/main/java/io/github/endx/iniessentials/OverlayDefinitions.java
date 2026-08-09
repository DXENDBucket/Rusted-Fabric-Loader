package io.github.endx.iniessentials;

import io.github.endx.rustedfabricapi.api.client.event.HudRenderEvents;
import io.github.endx.rustedfabricapi.api.client.render.ArgbColor;
import io.github.endx.rustedfabricapi.api.client.render.ClientImage;
import io.github.endx.rustedfabricapi.api.client.render.DrawStyle;
import io.github.endx.rustedfabricapi.api.client.render.HudDrawContext;
import io.github.endx.rustedfabricapi.api.client.render.TextAlignment;
import io.github.endx.rustedfabricapi.api.ini.IniExtensions;
import io.github.endx.rustedfabricapi.api.ini.IniFieldDefinition;
import io.github.endx.rustedfabricapi.api.ini.IniFieldDocumentation;
import io.github.endx.rustedfabricapi.api.ini.IniMultiplayerImpact;
import io.github.endx.rustedfabricapi.api.ini.IniSectionSelector;
import io.github.endx.rustedfabricapi.api.map.Maps;
import io.github.endx.rustedfabricapi.api.unit.Teams;
import io.github.endx.rustedfabricapi.api.unit.Units;
import rustedwarfare.client.render.GameImage;
import rustedwarfare.custom.CustomUnit;
import rustedwarfare.custom.CustomUnitMetadata;
import rustedwarfare.custom.LocalizedString;
import rustedwarfare.game.Team;
import rustedwarfare.framework.GameObject;
import rustedwarfare.unit.Unit;
import rustedwarfare.util.UnitConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

/** Screen-space overlays declared by custom-unit INI sections. */
final class OverlayDefinitions {
    private static final String PREFIX = "overlay_";
    private static final Map<Object, Map<String, Template>> BY_METADATA =
            Collections.synchronizedMap(new WeakHashMap<Object, Map<String, Template>>());

    private OverlayDefinitions() { }

    static void register() {
        OverlayEvaluationContext.registerFunctions();
        IniExtensions.register(IniFieldDefinition
                .<String>builder(IniEssentials.MOD_ID, "overlay_definition",
                        IniSectionSelector.prefix(PREFIX), "type")
                .decoder(context -> context.rawValue().trim())
                .validator((context, value) -> parseEnum(Type.class, value, "overlay type"))
                .applier(field -> parseAndStore((CustomUnitMetadata) field.metadata(),
                        (UnitConfig) field.unitConfig(), field.source().section()))
                .documentation(new IniFieldDocumentation(
                        "bar|text|image",
                        "Declares a screen-space HUD overlay backed by live instances of this custom unit.",
                        "声明一个由此自定义单位的存活实例提供上下文的屏幕 HUD 覆盖层。",
                        "[overlay_boss]\ntype: bar\nanchor: topCenter\nvalue: self.hp\nmaxValue: self.maxHp",
                        IniMultiplayerImpact.CLIENT_ONLY))
                .build());
        HudRenderEvents.AFTER_HUD.register((gameInterface, context) -> render(context));
    }

    private static void parseAndStore(CustomUnitMetadata metadata, UnitConfig config,
                                      String section) {
        String name = section.substring(PREFIX.length()).trim();
        if (name.isEmpty()) throw new IllegalArgumentException("overlay section name must not be empty");
        Type type = parseEnum(Type.class, required(config, section, "type"), "overlay type");
        ClientImage image = null;
        if (type == Type.IMAGE) {
            if (!config.hasKey(section, "image")) {
                throw new IllegalArgumentException("[" + section + "] image overlay requires image");
            }
            boolean smoothing = bool(config, section, "imageSmoothing", true);
            GameImage nativeImage = metadata.loadImageFromConfigWithSmoothing(
                    config, section, "image", smoothing);
            if (nativeImage == null) {
                throw new IllegalArgumentException("[" + section + "] could not load image");
            }
            image = ClientImage.borrowed(nativeImage);
        } else {
            optional(config, section, "image");
            optional(config, section, "imageSmoothing");
        }

        LocalizedString text = null;
        if (config.hasKey(section, "text")) {
            text = config.getLocalizedString(metadata, section, "text", null);
        }
        NumericExpression width = optionalExpression(metadata, config, section, "width");
        NumericExpression height = optionalExpression(metadata, config, section, "height");
        if (type == Type.BAR) {
            if (width == null) width = NumericExpression.compile(metadata, "320");
            if (height == null) height = NumericExpression.compile(metadata, "24");
        } else if (type == Type.TEXT) {
            if (width == null) width = NumericExpression.compile(metadata, "320");
            if (height == null) height = NumericExpression.compile(metadata, "28");
        }
        if (type == Type.TEXT && text == null) {
            throw new IllegalArgumentException("[" + section + "] text overlay requires text");
        }

        Template template = new Template(name, type,
                parseEnum(Anchor.class, optional(config, section, "anchor", "topCenter"), "anchor"),
                parseEnum(InstanceMode.class, optional(config, section, "instanceMode", "all"), "instanceMode"),
                parseEnum(IndexMode.class, optional(config, section, "indexMode", "compact"), "indexMode"),
                parseEnum(TeamFilter.class, optional(config, section, "team", "any"), "team"),
                parseEnum(FogVisibility.class, optional(config, section, "fogVisibility", "visible"), "fogVisibility"),
                BooleanExpression.compile(metadata, optional(config, section, "isVisible"), "true"),
                BooleanExpression.compile(metadata, optional(config, section, "instanceCondition"), "true"),
                NumericExpression.compile(metadata, optional(config, section, "priority"), "0"),
                optionalExpression(metadata, config, section, "slot"),
                positiveInt(config, section, "maxInstances", Integer.MAX_VALUE),
                positiveInt(config, section, "columns", 1),
                NumericExpression.compile(metadata, optional(config, section, "offsetX"), "0"),
                NumericExpression.compile(metadata, optional(config, section, "offsetY"), "0"),
                NumericExpression.compile(metadata, optional(config, section, "spacingX"), "0"),
                NumericExpression.compile(metadata, optional(config, section, "spacingY"), "8"),
                width, height,
                NumericExpression.compile(metadata, optional(config, section, "alpha"), "1"),
                NumericExpression.compile(metadata, optional(config, section, "order"), "0"),
                image,
                color(config, section, "color", type == Type.BAR ? 0xff43a047 : 0xffffffff),
                color(config, section, "backgroundColor", 0xb0000000),
                color(config, section, "borderColor", 0xffffffff),
                NumericExpression.compile(metadata, optional(config, section, "borderWidth"), "1"),
                NumericExpression.compile(metadata, optional(config, section, "value"), "self.hp"),
                NumericExpression.compile(metadata, optional(config, section, "maxValue"), "self.maxHp"),
                text,
                NumericExpression.compile(metadata, optional(config, section, "textSize"), "18"),
                color(config, section, "textColor", 0xffffffff),
                parseEnum(TextAlign.class, optional(config, section, "textAlign", "center"), "textAlign"));

        Map<String, Template> definitions = BY_METADATA.get(metadata);
        if (definitions == null) {
            definitions = new LinkedHashMap<String, Template>();
            BY_METADATA.put(metadata, definitions);
        }
        String key = name.toLowerCase(Locale.ROOT);
        if (definitions.put(key, template) != null) {
            throw new IllegalArgumentException("duplicate overlay definition: " + name);
        }
        IniEssentials.activateSynchronizedRequirement();
    }

    private static void render(HudDrawContext context) {
        List<CustomUnit> units = customUnits();
        if (units.isEmpty()) return;
        Team player = Teams.player().orElse(null);
        ArrayList<RenderEntry> entries = new ArrayList<RenderEntry>();
        synchronized (BY_METADATA) {
            for (Map.Entry<Object, Map<String, Template>> metadataEntry : BY_METADATA.entrySet()) {
                CustomUnitMetadata metadata = (CustomUnitMetadata) metadataEntry.getKey();
                ArrayList<CustomUnit> matching = new ArrayList<CustomUnit>();
                for (CustomUnit unit : units) if (unit.unitMetadata == metadata) matching.add(unit);
                if (matching.isEmpty()) continue;
                for (Template template : metadataEntry.getValue().values()) {
                    template.collect(matching, player, context, entries);
                }
            }
        }
        entries.sort(Comparator.comparingDouble((RenderEntry entry) -> entry.order)
                .thenComparing(entry -> entry.template.name)
                .thenComparingLong(entry -> entry.unit.id));
        for (RenderEntry entry : entries) entry.draw(context);
    }

    private static List<CustomUnit> customUnits() {
        ArrayList<CustomUnit> result = new ArrayList<CustomUnit>();
        for (Unit unit : Units.alive()) if (unit instanceof CustomUnit) result.add((CustomUnit) unit);
        return result;
    }

    private static boolean fogVisible(CustomUnit unit, FogVisibility visibility, Team player) {
        if (visibility == FogVisibility.ALWAYS) return true;
        if (player == null || !Maps.isLoaded()) return false;
        GameObject object = unit;
        if (visibility == FogVisibility.EXPLORED) {
            int x = Maps.worldToTileX(object.x);
            int y = Maps.worldToTileY(object.y);
            return Maps.isExplored(x, y, player);
        }
        return Maps.isVisible(object.x, object.y, player);
    }

    private static boolean teamMatches(CustomUnit unit, TeamFilter filter, Team player) {
        if (filter == TeamFilter.ANY) return true;
        if (unit.team == null) return false;
        switch (filter) {
            case OWN: return unit.team == player;
            case ALLY: return player != null && player.isAlly(unit.team);
            case ENEMY: return player != null && player.isEnemy(unit.team);
            case NEUTRAL: return unit.team == Teams.neutral();
            default: return true;
        }
    }

    private static String required(UnitConfig config, String section, String key) {
        String value = config.getString(section, key, null);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("[" + section + "] requires " + key);
        }
        return value;
    }

    private static String optional(UnitConfig config, String section, String key) {
        return config.getString(section, key, null);
    }

    private static String optional(UnitConfig config, String section, String key, String fallback) {
        String value = optional(config, section, key);
        return value != null ? value : fallback;
    }

    private static NumericExpression optionalExpression(CustomUnitMetadata metadata,
                                                        UnitConfig config, String section,
                                                        String key) {
        String value = optional(config, section, key);
        return value != null ? NumericExpression.compile(metadata, value) : null;
    }

    private static boolean bool(UnitConfig config, String section, String key, boolean fallback) {
        Boolean value = config.getBoolean(section, key, Boolean.valueOf(fallback));
        return value != null ? value.booleanValue() : fallback;
    }

    private static int positiveInt(UnitConfig config, String section, String key, int fallback) {
        Integer value = config.getInteger(section, key, Integer.valueOf(fallback));
        int result = value != null ? value.intValue() : fallback;
        if (result <= 0) throw new IllegalArgumentException(key + " must be positive");
        return result;
    }

    private static int color(UnitConfig config, String section, String key, int fallback) {
        Integer value = config.getColor(section, key, Integer.valueOf(fallback));
        return value != null ? value.intValue() : fallback;
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String raw, String label) {
        String normalized = raw.trim().replace("_", "").replace("-", "").toUpperCase(Locale.ROOT);
        for (E value : type.getEnumConstants()) {
            if (value.name().replace("_", "").equals(normalized)) return value;
        }
        throw new IllegalArgumentException("unknown " + label + ": " + raw);
    }

    private static int alphaColor(int color, float alpha) {
        float checked = Math.max(0.0F, Math.min(1.0F, alpha));
        int original = (color >>> 24) & 0xff;
        return ArgbColor.withAlpha(color, Math.round(original * checked));
    }

    enum Type { BAR, TEXT, IMAGE }
    enum Anchor { TOP_LEFT, TOP_CENTER, TOP_RIGHT, CENTER_LEFT, CENTER, CENTER_RIGHT,
        BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT }
    enum InstanceMode { ALL, FIRST, LAST, HIGHEST_PRIORITY, LOWEST_PRIORITY }
    enum IndexMode { COMPACT, STABLE, EXPLICIT }
    enum TeamFilter { ANY, OWN, ALLY, ENEMY, NEUTRAL }
    enum FogVisibility { VISIBLE, EXPLORED, ALWAYS }
    enum TextAlign { LEFT, CENTER, RIGHT }

    private static final class Candidate {
        final CustomUnit unit;
        final float priority;
        int compactIndex;
        int stableIndex;
        int slot;

        Candidate(CustomUnit unit, float priority) {
            this.unit = unit;
            this.priority = priority;
        }
    }

    private static final class Template {
        final String name;
        final Type type;
        final Anchor anchor;
        final InstanceMode instanceMode;
        final IndexMode indexMode;
        final TeamFilter team;
        final FogVisibility fogVisibility;
        final BooleanExpression visible;
        final BooleanExpression instanceCondition;
        final NumericExpression priority;
        final NumericExpression slot;
        final int maxInstances;
        final int columns;
        final NumericExpression offsetX, offsetY, spacingX, spacingY, width, height;
        final NumericExpression alpha, order;
        final ClientImage image;
        final int color, backgroundColor, borderColor;
        final NumericExpression borderWidth, value, maxValue;
        final LocalizedString text;
        final NumericExpression textSize;
        final int textColor;
        final TextAlign textAlign;
        final Map<CustomUnit, Integer> stableIndices = new WeakHashMap<CustomUnit, Integer>();
        int nextStableIndex;

        Template(String name, Type type, Anchor anchor, InstanceMode instanceMode,
                 IndexMode indexMode, TeamFilter team, FogVisibility fogVisibility,
                 BooleanExpression visible, BooleanExpression instanceCondition,
                 NumericExpression priority, NumericExpression slot, int maxInstances,
                 int columns, NumericExpression offsetX, NumericExpression offsetY,
                 NumericExpression spacingX, NumericExpression spacingY,
                 NumericExpression width, NumericExpression height, NumericExpression alpha,
                 NumericExpression order, ClientImage image, int color, int backgroundColor,
                 int borderColor, NumericExpression borderWidth, NumericExpression value,
                 NumericExpression maxValue, LocalizedString text, NumericExpression textSize,
                 int textColor, TextAlign textAlign) {
            this.name = name; this.type = type; this.anchor = anchor;
            this.instanceMode = instanceMode; this.indexMode = indexMode;
            this.team = team; this.fogVisibility = fogVisibility; this.visible = visible;
            this.instanceCondition = instanceCondition; this.priority = priority;
            this.slot = slot; this.maxInstances = maxInstances; this.columns = columns;
            this.offsetX = offsetX; this.offsetY = offsetY; this.spacingX = spacingX;
            this.spacingY = spacingY; this.width = width; this.height = height;
            this.alpha = alpha; this.order = order; this.image = image; this.color = color;
            this.backgroundColor = backgroundColor; this.borderColor = borderColor;
            this.borderWidth = borderWidth; this.value = value; this.maxValue = maxValue;
            this.text = text; this.textSize = textSize; this.textAlign = textAlign;
            this.textColor = textColor;
        }

        void collect(List<CustomUnit> units, Team player, HudDrawContext context,
                     List<RenderEntry> output) {
            ArrayList<Candidate> candidates = new ArrayList<Candidate>();
            for (CustomUnit unit : units) {
                if (!teamMatches(unit, team, player) || !fogVisible(unit, fogVisibility, player)) continue;
                if (!instanceCondition.evaluate(unit)) continue;
                candidates.add(new Candidate(unit, priority.evaluate(unit)));
            }
            candidates.sort(Comparator.comparingLong(candidate -> candidate.unit.id));
            select(candidates);
            if (candidates.size() > maxInstances) {
                candidates.subList(maxInstances, candidates.size()).clear();
            }
            int count = candidates.size();
            for (int i = 0; i < count; i++) {
                Candidate candidate = candidates.get(i);
                candidate.compactIndex = i;
                candidate.stableIndex = stableIndex(candidate.unit);
                candidate.slot = resolveSlot(candidate, count, context);
                int layoutIndex = indexMode == IndexMode.COMPACT ? i
                        : indexMode == IndexMode.STABLE ? candidate.stableIndex : candidate.slot;
                int row = Math.floorDiv(layoutIndex, columns);
                int column = Math.floorMod(layoutIndex, columns);
                OverlayEvaluationContext.State state = state(candidate, count, row, column,
                        context);
                if (!OverlayEvaluationContext.with(state, () -> visible.evaluate(candidate.unit))) continue;
                float resolvedOrder = OverlayEvaluationContext.with(state,
                        () -> order.evaluate(candidate.unit));
                output.add(new RenderEntry(this, candidate.unit, state, resolvedOrder));
            }
        }

        private void select(ArrayList<Candidate> candidates) {
            if (candidates.isEmpty() || instanceMode == InstanceMode.ALL) return;
            Candidate selected;
            switch (instanceMode) {
                case LAST:
                    selected = candidates.get(candidates.size() - 1);
                    break;
                case HIGHEST_PRIORITY:
                    selected = Collections.max(candidates, Comparator
                            .comparingDouble((Candidate candidate) -> candidate.priority)
                            .thenComparingLong(candidate -> -candidate.unit.id));
                    break;
                case LOWEST_PRIORITY:
                    selected = Collections.min(candidates, Comparator
                            .comparingDouble((Candidate candidate) -> candidate.priority)
                            .thenComparingLong(candidate -> candidate.unit.id));
                    break;
                case FIRST:
                default:
                    selected = candidates.get(0);
                    break;
            }
            candidates.clear();
            candidates.add(selected);
        }

        private int stableIndex(CustomUnit unit) {
            Integer existing = stableIndices.get(unit);
            if (existing != null) return existing.intValue();
            int assigned = nextStableIndex++;
            stableIndices.put(unit, Integer.valueOf(assigned));
            return assigned;
        }

        private int resolveSlot(Candidate candidate, int count, HudDrawContext context) {
            if (slot == null) return candidate.compactIndex;
            OverlayEvaluationContext.State initial = state(candidate, count, 0, 0, context);
            return Math.round(OverlayEvaluationContext.with(initial,
                    () -> slot.evaluate(candidate.unit)));
        }

        private OverlayEvaluationContext.State state(Candidate candidate, int count, int row,
                                                     int column, HudDrawContext context) {
            return new OverlayEvaluationContext.State(candidate.compactIndex,
                    candidate.stableIndex, count, row, column, candidate.slot,
                    candidate.unit.id, context.width(), context.height(), context.uiScale());
        }
    }

    private static final class RenderEntry {
        final Template template;
        final CustomUnit unit;
        final OverlayEvaluationContext.State state;
        final float order;

        RenderEntry(Template template, CustomUnit unit,
                    OverlayEvaluationContext.State state, float order) {
            this.template = template; this.unit = unit; this.state = state; this.order = order;
        }

        void draw(HudDrawContext context) {
            OverlayEvaluationContext.with(state, () -> {
                float width = template.width != null ? template.width.evaluate(unit)
                        : template.image != null ? template.image.width() : 0.0F;
                float height = template.height != null ? template.height.evaluate(unit)
                        : template.image != null ? template.image.height() : 0.0F;
                width = Math.max(0.0F, width);
                height = Math.max(0.0F, height);
                float x = anchorX(template.anchor, context.width(), width)
                        + template.offsetX.evaluate(unit)
                        + state.column * (width + template.spacingX.evaluate(unit));
                float y = anchorY(template.anchor, context.height(), height)
                        + template.offsetY.evaluate(unit)
                        + state.row * (height + template.spacingY.evaluate(unit));
                float alpha = template.alpha.evaluate(unit);
                switch (template.type) {
                    case IMAGE:
                        context.drawImageScaled(template.image, x, y, width, height,
                                DrawStyle.fill(alphaColor(template.color, alpha)));
                        break;
                    case BAR:
                        drawBar(context, x, y, width, height, alpha);
                        break;
                    case TEXT:
                        drawText(context, x, y, width, height, alpha, true);
                        break;
                    default:
                        break;
                }
            });
        }

        private void drawBar(HudDrawContext context, float x, float y, float width,
                             float height, float alpha) {
            context.fillRect(x, y, width, height, alphaColor(template.backgroundColor, alpha));
            float maximum = template.maxValue.evaluate(unit);
            float ratio = maximum > 0.0F ? template.value.evaluate(unit) / maximum : 0.0F;
            ratio = Math.max(0.0F, Math.min(1.0F, ratio));
            context.fillRect(x, y, width * ratio, height, alphaColor(template.color, alpha));
            float border = template.borderWidth.evaluate(unit);
            if (border > 0.0F) {
                context.strokeRect(x, y, width, height,
                        alphaColor(template.borderColor, alpha), border);
            }
            if (template.text != null) drawText(context, x, y, width, height, alpha, false);
        }

        private void drawText(HudDrawContext context, float x, float y, float width,
                              float height, float alpha, boolean freeText) {
            String value = template.text.resolveForUnit(unit);
            float size = Math.max(1.0F, template.textSize.evaluate(unit));
            TextAlignment alignment = template.textAlign == TextAlign.LEFT ? TextAlignment.LEFT
                    : template.textAlign == TextAlign.RIGHT ? TextAlignment.RIGHT : TextAlignment.CENTER;
            int baseColor = template.type == Type.TEXT ? template.color : template.textColor;
            DrawStyle style = DrawStyle.text(alphaColor(baseColor, alpha), size)
                    .withTextAlignment(alignment);
            float drawX = template.textAlign == TextAlign.LEFT ? x
                    : template.textAlign == TextAlign.RIGHT ? x + width : x + width * 0.5F;
            int textHeight = context.textHeight(value, style);
            float drawY = freeText && height == 0.0F ? y : y + (height + textHeight) * 0.5F;
            context.drawText(value, drawX, drawY, style);
        }
    }

    private static float anchorX(Anchor anchor, float screenWidth, float width) {
        switch (anchor) {
            case TOP_CENTER: case CENTER: case BOTTOM_CENTER: return (screenWidth - width) * 0.5F;
            case TOP_RIGHT: case CENTER_RIGHT: case BOTTOM_RIGHT: return screenWidth - width;
            default: return 0.0F;
        }
    }

    private static float anchorY(Anchor anchor, float screenHeight, float height) {
        switch (anchor) {
            case CENTER_LEFT: case CENTER: case CENTER_RIGHT: return (screenHeight - height) * 0.5F;
            case BOTTOM_LEFT: case BOTTOM_CENTER: case BOTTOM_RIGHT: return screenHeight - height;
            default: return 0.0F;
        }
    }
}
