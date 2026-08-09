package io.github.endx.iniessentials;

import io.github.endx.rustedfabricapi.api.client.Camera;
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
import io.github.endx.rustedfabricapi.api.world.WorldPoint;
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
        HudRenderEvents.BEFORE_HUD.register((gameInterface, context) ->
                render(context, Layer.BEFORE_HUD));
        HudRenderEvents.AFTER_HUD.register((gameInterface, context) ->
                render(context, Layer.AFTER_HUD));
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
        int totalFrames = positiveInt(config, section, "total_frames", 1);
        Integer configuredFrameWidth = optionalPositiveInt(config, section, "frame_width");
        Integer configuredFrameHeight = optionalPositiveInt(config, section, "frame_height");
        boolean verticalFrames = bool(config, section, "frame_verticalOrdering", false);
        if (type != Type.IMAGE && (config.hasKey(section, "frame")
                || config.hasKey(section, "total_frames")
                || config.hasKey(section, "frame_width")
                || config.hasKey(section, "frame_height")
                || config.hasKey(section, "frame_verticalOrdering"))) {
            throw new IllegalArgumentException("[" + section + "] frame fields require type: image");
        }
        FrameLayout frameLayout = FrameLayout.create(type, image, totalFrames,
                configuredFrameWidth, configuredFrameHeight, verticalFrames, section);

        Template template = new Template(name, type,
                parseEnum(Anchor.class, optional(config, section, "anchor", "topCenter"), "anchor"),
                parseEnum(Layer.class, optional(config, section, "layer", "afterHud"), "layer"),
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
                NumericExpression.compile(metadata, optional(config, section, "scale"), "1"),
                NumericExpression.compile(metadata, optional(config, section, "scaleX"), "1"),
                NumericExpression.compile(metadata, optional(config, section, "scaleY"), "1"),
                NumericExpression.compile(metadata, optional(config, section, "rotation"), "0"),
                NumericExpression.compile(metadata, optional(config, section, "alpha"), "1"),
                NumericExpression.compile(metadata, optional(config, section, "order"), "0"),
                image, frameLayout,
                NumericExpression.compile(metadata, optional(config, section, "frame"), "0"),
                color(config, section, "color", type == Type.BAR ? 0xff43a047 : 0xffffffff),
                color(config, section, "backgroundColor", 0xb0000000),
                color(config, section, "borderColor", 0xffffffff),
                NumericExpression.compile(metadata, optional(config, section, "borderWidth"), "1"),
                NumericExpression.compile(metadata, optional(config, section, "value"), "self.hp"),
                NumericExpression.compile(metadata, optional(config, section, "maxValue"), "self.maxHp"),
                text,
                NumericExpression.compile(metadata, optional(config, section, "textSize"), "18"),
                color(config, section, "textColor", 0xffffffff),
                parseEnum(TextAlign.class, optional(config, section, "textAlign", "center"), "textAlign"),
                parseEnum(BarDirection.class, optional(config, section, "barDirection", "leftToRight"),
                        "barDirection"));

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

    private static void render(HudDrawContext context, Layer layer) {
        List<CustomUnit> units = customUnits();
        if (units.isEmpty()) return;
        Team player = Teams.player().orElse(null);
        ArrayList<RenderEntry> entries = new ArrayList<RenderEntry>();
        synchronized (BY_METADATA) {
            for (Map.Entry<Object, Map<String, Template>> metadataEntry : BY_METADATA.entrySet()) {
                CustomUnitMetadata metadata = (CustomUnitMetadata) metadataEntry.getKey();
                boolean hasLayer = false;
                for (Template template : metadataEntry.getValue().values()) {
                    if (template.layer == layer) {
                        hasLayer = true;
                        break;
                    }
                }
                if (!hasLayer) continue;
                ArrayList<CustomUnit> matching = new ArrayList<CustomUnit>();
                for (CustomUnit unit : units) if (unit.unitMetadata == metadata) matching.add(unit);
                if (matching.isEmpty()) continue;
                for (Template template : metadataEntry.getValue().values()) {
                    if (template.layer != layer) continue;
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

    private static Integer optionalPositiveInt(UnitConfig config, String section, String key) {
        Integer value = config.getInteger(section, key, null);
        if (value != null && value.intValue() <= 0) {
            throw new IllegalArgumentException(key + " must be positive");
        }
        return value;
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
    enum Layer { BEFORE_HUD, AFTER_HUD }
    enum InstanceMode { ALL, FIRST, LAST, HIGHEST_PRIORITY, LOWEST_PRIORITY,
        NEAREST_TO_CAMERA, FARTHEST_FROM_CAMERA }
    enum IndexMode { COMPACT, STABLE, EXPLICIT }
    enum TeamFilter { ANY, OWN, ALLY, ENEMY, NEUTRAL }
    enum FogVisibility { VISIBLE, EXPLORED, ALWAYS }
    enum TextAlign { LEFT, CENTER, RIGHT }
    enum BarDirection { LEFT_TO_RIGHT, RIGHT_TO_LEFT, TOP_TO_BOTTOM, BOTTOM_TO_TOP }

    private static final class FrameLayout {
        final int totalFrames;
        final int frameWidth;
        final int frameHeight;
        final int columns;
        final int rows;
        final boolean verticalOrdering;

        private FrameLayout(int totalFrames, int frameWidth, int frameHeight,
                            int columns, int rows, boolean verticalOrdering) {
            this.totalFrames = totalFrames; this.frameWidth = frameWidth;
            this.frameHeight = frameHeight; this.columns = columns; this.rows = rows;
            this.verticalOrdering = verticalOrdering;
        }

        static FrameLayout create(Type type, ClientImage image, int totalFrames,
                                  Integer configuredWidth, Integer configuredHeight,
                                  boolean verticalOrdering, String section) {
            if (type != Type.IMAGE) return new FrameLayout(1, 0, 0, 1, 1, false);
            int imageWidth = image.width();
            int imageHeight = image.height();
            int frameWidth;
            int frameHeight;
            if (configuredWidth == null && configuredHeight == null && totalFrames > 1) {
                frameWidth = verticalOrdering ? imageWidth : imageWidth / totalFrames;
                frameHeight = verticalOrdering ? imageHeight / totalFrames : imageHeight;
            } else {
                frameWidth = configuredWidth != null ? configuredWidth.intValue() : imageWidth;
                frameHeight = configuredHeight != null ? configuredHeight.intValue() : imageHeight;
            }
            if (frameWidth <= 0 || frameHeight <= 0
                    || frameWidth > imageWidth || frameHeight > imageHeight) {
                throw new IllegalArgumentException("[" + section + "] invalid image frame size");
            }
            int columns = imageWidth / frameWidth;
            int rows = imageHeight / frameHeight;
            if (columns <= 0 || rows <= 0 || totalFrames > columns * rows) {
                throw new IllegalArgumentException("[" + section
                        + "] total_frames exceeds the image frame grid");
            }
            return new FrameLayout(totalFrames, frameWidth, frameHeight,
                    columns, rows, verticalOrdering);
        }

        int sourceX(int index) {
            int column = verticalOrdering ? index / rows : index % columns;
            return column * frameWidth;
        }

        int sourceY(int index) {
            int row = verticalOrdering ? index % rows : index / columns;
            return row * frameHeight;
        }

        int clampFrame(float value) {
            return Math.max(0, Math.min(totalFrames - 1, Math.round(value)));
        }
    }

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
        final Layer layer;
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
        final NumericExpression scale, scaleX, scaleY, rotation;
        final NumericExpression alpha, order;
        final ClientImage image;
        final FrameLayout frameLayout;
        final NumericExpression frame;
        final int color, backgroundColor, borderColor;
        final NumericExpression borderWidth, value, maxValue;
        final LocalizedString text;
        final NumericExpression textSize;
        final int textColor;
        final TextAlign textAlign;
        final BarDirection barDirection;
        final Map<CustomUnit, Integer> stableIndices = new WeakHashMap<CustomUnit, Integer>();
        int nextStableIndex;

        Template(String name, Type type, Anchor anchor, Layer layer, InstanceMode instanceMode,
                 IndexMode indexMode, TeamFilter team, FogVisibility fogVisibility,
                 BooleanExpression visible, BooleanExpression instanceCondition,
                 NumericExpression priority, NumericExpression slot, int maxInstances,
                 int columns, NumericExpression offsetX, NumericExpression offsetY,
                 NumericExpression spacingX, NumericExpression spacingY,
                 NumericExpression width, NumericExpression height, NumericExpression scale,
                 NumericExpression scaleX, NumericExpression scaleY, NumericExpression rotation,
                 NumericExpression alpha, NumericExpression order, ClientImage image,
                 FrameLayout frameLayout, NumericExpression frame, int color, int backgroundColor,
                 int borderColor, NumericExpression borderWidth, NumericExpression value,
                 NumericExpression maxValue, LocalizedString text, NumericExpression textSize,
                 int textColor, TextAlign textAlign, BarDirection barDirection) {
            this.name = name; this.type = type; this.anchor = anchor;
            this.layer = layer;
            this.instanceMode = instanceMode; this.indexMode = indexMode;
            this.team = team; this.fogVisibility = fogVisibility; this.visible = visible;
            this.instanceCondition = instanceCondition; this.priority = priority;
            this.slot = slot; this.maxInstances = maxInstances; this.columns = columns;
            this.offsetX = offsetX; this.offsetY = offsetY; this.spacingX = spacingX;
            this.spacingY = spacingY; this.width = width; this.height = height;
            this.scale = scale; this.scaleX = scaleX; this.scaleY = scaleY;
            this.rotation = rotation; this.alpha = alpha; this.order = order;
            this.image = image; this.frameLayout = frameLayout; this.frame = frame;
            this.color = color;
            this.backgroundColor = backgroundColor; this.borderColor = borderColor;
            this.borderWidth = borderWidth; this.value = value; this.maxValue = maxValue;
            this.text = text; this.textSize = textSize; this.textAlign = textAlign;
            this.textColor = textColor;
            this.barDirection = barDirection;
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
                case NEAREST_TO_CAMERA: {
                    WorldPoint center = Camera.center();
                    selected = Collections.min(candidates, Comparator
                            .comparingDouble((Candidate candidate) ->
                                    distanceSquared(candidate.unit, center))
                            .thenComparingLong(candidate -> candidate.unit.id));
                    break;
                }
                case FARTHEST_FROM_CAMERA: {
                    WorldPoint center = Camera.center();
                    selected = Collections.max(candidates, Comparator
                            .comparingDouble((Candidate candidate) ->
                                    distanceSquared(candidate.unit, center))
                            .thenComparingLong(candidate -> -candidate.unit.id));
                    break;
                }
                case FIRST:
                default:
                    selected = candidates.get(0);
                    break;
            }
            candidates.clear();
            candidates.add(selected);
        }

        private static float distanceSquared(CustomUnit unit, WorldPoint point) {
            GameObject object = unit;
            float dx = object.x - point.x();
            float dy = object.y - point.y();
            return dx * dx + dy * dy;
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
                        : template.image != null ? template.frameLayout.frameWidth : 0.0F;
                float height = template.height != null ? template.height.evaluate(unit)
                        : template.image != null ? template.frameLayout.frameHeight : 0.0F;
                final float resolvedWidth = Math.max(0.0F, width);
                final float resolvedHeight = Math.max(0.0F, height);
                float uniformScale = template.scale.evaluate(unit);
                float scaleX = uniformScale * template.scaleX.evaluate(unit);
                float scaleY = uniformScale * template.scaleY.evaluate(unit);
                if (scaleX == 0.0F || scaleY == 0.0F
                        || resolvedWidth == 0.0F || resolvedHeight == 0.0F) return;
                float drawnWidth = resolvedWidth * Math.abs(scaleX);
                float drawnHeight = resolvedHeight * Math.abs(scaleY);
                float x = anchorX(template.anchor, context.width(), drawnWidth)
                        + template.offsetX.evaluate(unit)
                        + state.column * (drawnWidth + template.spacingX.evaluate(unit));
                float y = anchorY(template.anchor, context.height(), drawnHeight)
                        + template.offsetY.evaluate(unit)
                        + state.row * (drawnHeight + template.spacingY.evaluate(unit));
                float alpha = template.alpha.evaluate(unit);
                float rotation = template.rotation.evaluate(unit);
                context.transformed(x + drawnWidth * 0.5F, y + drawnHeight * 0.5F,
                        scaleX, scaleY, rotation, transformed ->
                                drawPrimitive(transformed, -resolvedWidth * 0.5F,
                                        -resolvedHeight * 0.5F,
                                        resolvedWidth, resolvedHeight, alpha));
            });
        }

        private void drawPrimitive(HudDrawContext context, float x, float y, float width,
                                   float height, float alpha) {
            switch (template.type) {
                case IMAGE:
                    int frame = template.frameLayout.clampFrame(template.frame.evaluate(unit));
                    context.drawImageRegion(template.image,
                            template.frameLayout.sourceX(frame),
                            template.frameLayout.sourceY(frame),
                            template.frameLayout.frameWidth,
                            template.frameLayout.frameHeight,
                            x, y, width, height,
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
        }

        private void drawBar(HudDrawContext context, float x, float y, float width,
                             float height, float alpha) {
            context.fillRect(x, y, width, height, alphaColor(template.backgroundColor, alpha));
            float maximum = template.maxValue.evaluate(unit);
            float ratio = maximum > 0.0F ? template.value.evaluate(unit) / maximum : 0.0F;
            ratio = Math.max(0.0F, Math.min(1.0F, ratio));
            int fillColor = alphaColor(template.color, alpha);
            switch (template.barDirection) {
                case RIGHT_TO_LEFT:
                    context.fillRect(x + width * (1.0F - ratio), y,
                            width * ratio, height, fillColor);
                    break;
                case TOP_TO_BOTTOM:
                    context.fillRect(x, y, width, height * ratio, fillColor);
                    break;
                case BOTTOM_TO_TOP:
                    context.fillRect(x, y + height * (1.0F - ratio),
                            width, height * ratio, fillColor);
                    break;
                case LEFT_TO_RIGHT:
                default:
                    context.fillRect(x, y, width * ratio, height, fillColor);
                    break;
            }
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
