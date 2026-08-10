package io.github.endx.iniessentials.decal;

import io.github.endx.iniessentials.BooleanExpression;
import io.github.endx.iniessentials.GeometryDefinitions;
import io.github.endx.iniessentials.IniEssentials;
import io.github.endx.iniessentials.NumericExpression;

import android.graphics.Rect;
import io.github.endx.rustedfabricapi.api.client.render.AlphaMask;
import io.github.endx.rustedfabricapi.api.client.render.AlphaMaskOptions;
import io.github.endx.rustedfabricapi.api.client.render.AlphaMasks;
import io.github.endx.rustedfabricapi.api.client.render.BarDirection;
import io.github.endx.rustedfabricapi.api.client.render.BarStyle;
import io.github.endx.rustedfabricapi.api.client.render.ClientImage;
import io.github.endx.rustedfabricapi.api.client.render.ClientImages;
import io.github.endx.rustedfabricapi.api.client.render.Decals;
import io.github.endx.rustedfabricapi.api.client.render.MaskAlphaMode;
import io.github.endx.rustedfabricapi.api.client.render.MaskThresholdMode;
import io.github.endx.rustedfabricapi.api.client.render.event.DecalRenderEvents;
import io.github.endx.rustedfabricapi.api.geometry.GeometryMask;
import io.github.endx.rustedfabricapi.api.ini.IniApplicationPhase;
import io.github.endx.rustedfabricapi.api.ini.IniExtensions;
import io.github.endx.rustedfabricapi.api.ini.IniFieldDefinition;
import io.github.endx.rustedfabricapi.api.ini.IniFieldDocumentation;
import io.github.endx.rustedfabricapi.api.ini.IniMultiplayerImpact;
import io.github.endx.rustedfabricapi.api.ini.IniSectionSelector;
import rustedwarfare.client.render.GameImage;
import rustedwarfare.custom.CustomUnit;
import rustedwarfare.custom.CustomUnitMetadata;
import rustedwarfare.custom.graphics.DecalBehavior;
import rustedwarfare.custom.graphics.DecalImageFrame;
import rustedwarfare.custom.graphics.DecalTemplate;
import rustedwarfare.custom.logic.LogicBoolean;
import rustedwarfare.unit.Unit;
import rustedwarfare.util.UnitConfig;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** Optional bar and alpha-mask images layered into native Decal placement/render rules. */
public final class DecalMaskDefinitions {
    private static final String PREFIX = "decal_";
    private static final Set<String> KEYS = new java.util.HashSet<String>(java.util.Arrays.asList(
            "mask", "maskgeometry", "maskrender", "maskalphathreshold", "maskinvert",
            "maskthresholdmode", "maskalphamode", "maskusessourcealpha"));
    private static final Set<String> BAR_KEYS = new java.util.HashSet<String>(java.util.Arrays.asList(
            "barvalue", "barmaxvalue", "barcolor", "barbackgroundcolor", "barbordercolor",
            "barborderwidth", "bardirection"));
    private static final Map<DecalTemplate, MaskConfig> MASKS =
            Collections.synchronizedMap(new WeakHashMap<DecalTemplate, MaskConfig>());
    private static final Map<DecalTemplate, Boolean> MASK_SOURCES =
            Collections.synchronizedMap(new WeakHashMap<DecalTemplate, Boolean>());
    private static final Map<DecalTemplate, Boolean> SOURCE_RENDER =
            Collections.synchronizedMap(new WeakHashMap<DecalTemplate, Boolean>());
    private static final Map<DecalTemplate, BarConfig> BARS =
            Collections.synchronizedMap(new WeakHashMap<DecalTemplate, BarConfig>());
    private static final ThreadLocal<Deque<LayerState>> STATES =
            ThreadLocal.withInitial(ArrayDeque::new);

    private DecalMaskDefinitions() { }

    public static void register() {
        IniExtensions.register(IniFieldDefinition
                .<String>builder(IniEssentials.MOD_ID, "decal_mask_fields",
                        IniSectionSelector.prefix(PREFIX), "mask")
                .applicationPhase(IniApplicationPhase.AFTER_METADATA_PARSED)
                .matchKeyPrefix()
                .activatesWhen(context -> KEYS.contains(
                        context.key().toLowerCase(Locale.ROOT)))
                .decoder(context -> context.rawValue().trim())
                .applier(field -> applyField((CustomUnitMetadata) field.metadata(),
                        (UnitConfig) field.unitConfig(), field.source().section(),
                        field.source().key()))
                .documentation(new IniFieldDocumentation(
                        "decal name or geometry name plus alpha options",
                        "Masks a native Decal image with another Decal image or reusable geometry.",
                        "使用另一张原版 Decal 图片或可复用 geometry 遮罩原版 Decal 图片。",
                        "mask: hullShape\nmaskAlphaThreshold: 0.1",
                        IniMultiplayerImpact.CLIENT_ONLY))
                .build());
        IniExtensions.register(IniFieldDefinition
                .<String>builder(IniEssentials.MOD_ID, "decal_bar_fields",
                        IniSectionSelector.prefix(PREFIX), "bar")
                .applicationPhase(IniApplicationPhase.AFTER_METADATA_PARSED)
                .matchKeyPrefix()
                .activatesWhen(context -> BAR_KEYS.contains(
                        context.key().toLowerCase(Locale.ROOT)))
                .decoder(context -> context.rawValue().trim())
                .applier(field -> applyBarField((CustomUnitMetadata) field.metadata(),
                        (UnitConfig) field.unitConfig(), field.source().section(),
                        field.source().key()))
                .documentation(new IniFieldDocumentation(
                        "runtime bar value plus optional bar style fields",
                        "Turns a native Decal image frame into a dynamic bar while retaining native placement and visibility rules.",
                        "把原版 Decal 图片帧变为动态条形图，同时保留原版的位置与可见性规则。",
                        "barValue: self.hp\nbarMaxValue: self.maxHp",
                        IniMultiplayerImpact.CLIENT_ONLY))
                .build());
        DecalRenderEvents.BEFORE_LAYER.register(DecalMaskDefinitions::beforeLayer);
        DecalRenderEvents.AFTER_LAYER.register(DecalMaskDefinitions::afterLayer);
    }

    private static void applyBarField(CustomUnitMetadata metadata, UnitConfig config,
                                      String section, String key) {
        if (!config.hasKey(section, "barValue")) {
            throw new IllegalArgumentException("[" + section + "] " + key
                    + " requires barValue");
        }
        if (!key.equalsIgnoreCase("barValue")) return;
        DecalTemplate template = Decals.require(metadata,
                section.substring(PREFIX.length()));
        if (template.imageFrame == null || template.imageStackFrames != null) {
            throw new IllegalArgumentException("[" + section
                    + "] bar Decal requires image and does not support imageStack");
        }
        BarConfig parsed = new BarConfig(template,
                NumericExpression.compile(metadata,
                        config.getString(section, "barValue", null), "self.hp"),
                NumericExpression.compile(metadata,
                        config.getString(section, "barMaxValue", null), "self.maxHp"),
                config.getColor(section, "barColor", Integer.valueOf(0xff43a047)).intValue(),
                config.getColor(section, "barBackgroundColor", Integer.valueOf(0xb0000000)).intValue(),
                config.getColor(section, "barBorderColor", Integer.valueOf(0xffffffff)).intValue(),
                NumericExpression.compile(metadata,
                        config.getString(section, "barBorderWidth", null), "1"),
                parseEnum(BarDirection.class,
                        config.getString(section, "barDirection", "leftToRight"),
                        "barDirection"));
        BarConfig previous = BARS.put(template, parsed);
        if (previous != null) previous.close();
    }

    private static void applyField(CustomUnitMetadata metadata, UnitConfig config,
                                   String section, String key) {
        DecalTemplate template = Decals.require(metadata,
                section.substring(PREFIX.length()));
        if (key.equalsIgnoreCase("maskRender")) {
            SOURCE_RENDER.put(template, Boolean.valueOf(bool(config, section, key, false)));
            return;
        }
        if (!key.equalsIgnoreCase("mask") && !key.equalsIgnoreCase("maskGeometry")) return;
        String maskName = optionalName(config.getString(section, "mask", null));
        String geometryName = optionalName(config.getString(section, "maskGeometry", null));
        if (maskName != null && geometryName != null) {
            throw new IllegalArgumentException("[" + section
                    + "] mask and maskGeometry are mutually exclusive");
        }
        if (maskName == null && geometryName == null) return;
        if (template.imageFrame == null || template.imageStackFrames != null) {
            throw new IllegalArgumentException("[" + section
                    + "] masked Decal requires one image and does not support imageStack");
        }
        DecalTemplate source = null;
        if (maskName != null) {
            source = Decals.require(metadata, maskName);
            if (source == template) throw new IllegalArgumentException("Decal cannot mask itself");
            if (source.imageFrame == null || source.imageStackFrames != null) {
                throw new IllegalArgumentException("Decal mask source requires one image: " + maskName);
            }
            requireCompatibleAnchor(template, source, section);
            MASK_SOURCES.put(source, Boolean.TRUE);
        }
        MaskConfig parsed = new MaskConfig(metadata, template, source, geometryName,
                NumericExpression.compile(metadata,
                        config.getString(section, "maskAlphaThreshold", null), "0"),
                BooleanExpression.compile(metadata,
                        config.getString(section, "maskInvert", null), "false"),
                parseEnum(MaskThresholdMode.class,
                        config.getString(section, "maskThresholdMode", "keep"),
                        "maskThresholdMode"),
                parseEnum(MaskAlphaMode.class,
                        config.getString(section, "maskAlphaMode", "multiply"),
                        "maskAlphaMode"),
                bool(config, section, "maskUsesSourceAlpha", true));
        MaskConfig previous = MASKS.put(template, parsed);
        if (previous != null) previous.close();
    }

    private static void requireCompatibleAnchor(DecalTemplate content, DecalTemplate source,
                                                String section) {
        if (content.basePosition != source.basePosition
                || content.basePositionFromLegEnd != source.basePositionFromLegEnd
                || content.basePositionFromTurret != source.basePositionFromTurret
                || content.alwaysStartDirAtZero != source.alwaysStartDirAtZero
                || content.alwaysStartHeightAtZero != source.alwaysStartHeightAtZero
                || content.basePositionFromLegEnd != -1
                || content.basePositionFromTurret != -1) {
            throw new IllegalArgumentException("[" + section + "] Decal mask source must use the "
                    + "same unit/basePosition; leg and turret anchors are not yet supported");
        }
    }

    private static void beforeLayer(CustomUnit unit, float delta, Object layer,
                                    List<DecalTemplate> decals) {
        LayerState state = new LayerState();
        STATES.get().push(state);
        try {
            for (DecalTemplate decal : decals) {
                if (MASK_SOURCES.containsKey(decal)
                        && !Boolean.TRUE.equals(SOURCE_RENDER.get(decal))) {
                    state.visibility.add(new VisibilitySwap(decal, decal.isVisible));
                    decal.isVisible = LogicBoolean.falseBoolean;
                }
                BarConfig bar = BARS.get(decal);
                if (bar != null) {
                    ImageSwap swap = bar.prepare(unit);
                    if (swap != null) {
                        state.images.add(swap);
                        swap.apply();
                    }
                }
                MaskConfig config = MASKS.get(decal);
                if (config == null) continue;
                ImageSwap swap = config.prepare(unit);
                if (swap != null) {
                    state.images.add(swap);
                    swap.apply();
                }
            }
        } catch (RuntimeException failure) {
            state.restore();
            STATES.get().pop();
            throw failure;
        }
    }

    private static final class BarConfig {
        final DecalTemplate template;
        final NumericExpression value;
        final NumericExpression maxValue;
        final int fillColor, backgroundColor, borderColor;
        final NumericExpression borderWidth;
        final BarDirection direction;
        final LinkedHashMap<String, ClientImage> cache =
                new LinkedHashMap<String, ClientImage>(32, 0.75F, true);

        BarConfig(DecalTemplate template, NumericExpression value, NumericExpression maxValue,
                  int fillColor, int backgroundColor, int borderColor,
                  NumericExpression borderWidth, BarDirection direction) {
            this.template = template; this.value = value; this.maxValue = maxValue;
            this.fillColor = fillColor; this.backgroundColor = backgroundColor;
            this.borderColor = borderColor; this.borderWidth = borderWidth;
            this.direction = direction;
        }

        ImageSwap prepare(CustomUnit unit) {
            Frame frame = Frame.resolve(template, unit);
            if (frame == null) return null;
            float maximum = maxValue.evaluate(unit);
            float ratio = maximum > 0.0F ? clamp(value.evaluate(unit) / maximum) : 0.0F;
            int axis = direction.isHorizontal() ? frame.width() : frame.height();
            int filled = Math.round(axis * ratio);
            int border = Math.max(0, Math.round(borderWidth.evaluate(unit)));
            String key = frame.width() + "x" + frame.height() + ':' + filled + ':' + border;
            ClientImage image = cache.get(key);
            if (image == null || image.isClosed()) {
                float quantizedRatio = axis > 0 ? (float) filled / axis : 0.0F;
                image = ClientImages.createBar(frame.width(), frame.height(), quantizedRatio,
                        new BarStyle(fillColor, backgroundColor, borderColor, border, direction));
                cache(key, image);
            }
            return ImageSwap.create(template, image);
        }

        private void cache(String key, ClientImage image) {
            ClientImage previous = cache.put(key, image);
            if (previous != null && previous != image) previous.close();
            while (cache.size() > 128) {
                Map.Entry<String, ClientImage> eldest = cache.entrySet().iterator().next();
                cache.remove(eldest.getKey());
                eldest.getValue().close();
            }
        }

        void close() {
            for (ClientImage image : cache.values()) image.close();
            cache.clear();
        }
    }

    private static void afterLayer(CustomUnit unit, float delta, Object layer,
                                   List<DecalTemplate> decals) {
        Deque<LayerState> states = STATES.get();
        if (states.isEmpty()) return;
        states.pop().restore();
        if (states.isEmpty()) STATES.remove();
    }

    private static final class MaskConfig {
        final CustomUnitMetadata metadata;
        final DecalTemplate content;
        final DecalTemplate source;
        final String geometry;
        final NumericExpression threshold;
        final BooleanExpression invert;
        final MaskThresholdMode thresholdMode;
        final MaskAlphaMode alphaMode;
        final boolean usesSourceAlpha;
        final LinkedHashMap<String, ClientImage> cache =
                new LinkedHashMap<String, ClientImage>(16, 0.75F, true);

        MaskConfig(CustomUnitMetadata metadata, DecalTemplate content, DecalTemplate source,
                   String geometry, NumericExpression threshold, BooleanExpression invert,
                   MaskThresholdMode thresholdMode, MaskAlphaMode alphaMode,
                   boolean usesSourceAlpha) {
            this.metadata = metadata; this.content = content; this.source = source;
            this.geometry = geometry; this.threshold = threshold; this.invert = invert;
            this.thresholdMode = thresholdMode; this.alphaMode = alphaMode;
            this.usesSourceAlpha = usesSourceAlpha;
        }

        ImageSwap prepare(CustomUnit unit) {
            Frame contentFrame = Frame.resolve(content, unit);
            if (contentFrame == null) return null;
            float checkedThreshold = clamp(threshold.evaluate(unit));
            boolean checkedInvert = invert.evaluate(unit);
            AlphaMaskOptions options = new AlphaMaskOptions(checkedThreshold, checkedInvert,
                    thresholdMode, alphaMode);
            AlphaMask mask;
            String key = null;
            boolean temporary = true;
            if (geometry != null) {
                GeometryMask resolved = GeometryDefinitions.require(metadata, geometry)
                        .resolve(metadata, unit);
                mask = AlphaMasks.geometry(resolved);
            } else {
                Frame maskFrame = Frame.resolve(source, unit);
                if (maskFrame == null) return null;
                Transform contentTransform = Transform.resolve(content, unit);
                Transform maskTransform = Transform.resolve(source, unit);
                float[] affine = Transform.relative(contentTransform, maskTransform);
                float sourceAlpha = usesSourceAlpha ? sourceAlpha(source, unit) : 1.0F;
                mask = AlphaMasks.imageAffine(ClientImage.borrowed(maskFrame.image),
                        maskFrame.x, maskFrame.y, maskFrame.width(), maskFrame.height(),
                        affine[0], affine[1], affine[2], affine[3], affine[4], affine[5],
                        sourceAlpha);
                key = unit.id + ":" + contentFrame.key() + ':' + maskFrame.key() + ':'
                        + contentTransform.key() + ':' + maskTransform.key() + ':'
                        + Float.floatToIntBits(sourceAlpha) + ':'
                        + Float.floatToIntBits(checkedThreshold) + ':' + checkedInvert + ':'
                        + thresholdMode + ':' + alphaMode;
                ClientImage cached = cache.get(key);
                if (cached != null && !cached.isClosed()) return ImageSwap.create(content, cached);
                temporary = false;
            }
            ClientImage result = ClientImages.applyAlphaMask(
                    ClientImage.borrowed(contentFrame.image),
                    contentFrame.x, contentFrame.y, contentFrame.width(), contentFrame.height(),
                    mask, options);
            if (!temporary) cache(key, result);
            return ImageSwap.create(content, result, temporary);
        }

        private void cache(String key, ClientImage image) {
            ClientImage previous = cache.put(key, image);
            if (previous != null && previous != image) previous.close();
            while (cache.size() > 64) {
                Map.Entry<String, ClientImage> eldest = cache.entrySet().iterator().next();
                cache.remove(eldest.getKey());
                eldest.getValue().close();
            }
        }

        void close() {
            for (ClientImage image : cache.values()) image.close();
            cache.clear();
        }
    }

    private static final class Frame {
        final GameImage image;
        final int x, y, right, bottom;

        private Frame(GameImage image, int x, int y, int right, int bottom) {
            this.image = image; this.x = x; this.y = y; this.right = right; this.bottom = bottom;
        }

        static Frame resolve(DecalTemplate template, CustomUnit unit) {
            DecalImageFrame frame = template.imageFrame;
            if (frame == null) return null;
            GameImage image = frame.image;
            if (frame.teamColorImages != null && unit.team != null) {
                int colorIndex = unit.team.R();
                if (colorIndex >= 0 && colorIndex < frame.teamColorImages.length) {
                    image = frame.teamColorImages[colorIndex];
                }
            }
            if (image == null) return null;
            int index = template.frame != null ? (int) template.frame.readNumber(unit) : 0;
            index += unit.currentFrame * template.addBodyFrameMultipliedBy;
            if (!template.hasCustomFrameGrid) {
                return new Frame(image, 0, 0, image.getWidth(), image.getHeight());
            }
            Rect rect = DecalBehavior.getSourceRect(template, frame, image, index);
            return new Frame(image, rect.a, rect.b, rect.c, rect.d);
        }

        int width() { return right - x; }
        int height() { return bottom - y; }
        String key() {
            return System.identityHashCode(image) + "," + x + ',' + y + ',' + right + ',' + bottom;
        }
    }

    private static final class Transform {
        final float centerX, centerY, scaleX, scaleY, rotation;

        private Transform(float centerX, float centerY, float scaleX, float scaleY,
                          float rotation) {
            this.centerX = centerX; this.centerY = centerY;
            this.scaleX = scaleX; this.scaleY = scaleY; this.rotation = rotation;
        }

        static Transform resolve(DecalTemplate template, CustomUnit unit) {
            Unit base = template.basePosition != null
                    ? template.basePosition.readUnit(unit) : unit;
            if (base == null) return new Transform(0, 0, 1, 1, 0);
            float pivot = base.direction + template.pivotOffset;
            float cosine = cosine(pivot);
            float sine = sine(pivot);
            float x = template.xOffsetAbsoluteFallback;
            float y = template.yOffsetAbsoluteFallback - template.hOffset;
            if (template.xOffsetAbsolute != null) x += template.xOffsetAbsolute.readNumber(unit);
            if (template.yOffsetAbsolute != null) y += template.yOffsetAbsolute.readNumber(unit);
            x += cosine * template.yOffsetRelative - sine * template.xOffsetRelative;
            y += sine * template.yOffsetRelative + cosine * template.xOffsetRelative;
            float scaleX = template.imageScale;
            float scaleY = template.imageScale;
            if (template.usesSplitImageScale) {
                if (template.imageScaleX != null) {
                    float common = template.imageScaleX.readNumber(unit);
                    scaleX = common; scaleY = common;
                }
                if (template.imageScaleY != null) scaleX *= template.imageScaleY.readNumber(unit);
                if (template.imageScaleLogic != null) scaleY *= template.imageScaleLogic.readNumber(unit);
            }
            float rotation = (template.alwaysStartDirAtZero ? 0.0F : base.direction + 90.0F)
                    + template.dirOffset;
            return new Transform(x, y, scaleX, scaleY, rotation);
        }

        static float[] relative(Transform content, Transform mask) {
            float[] center = content.screenToLocal(
                    mask.centerX - content.centerX, mask.centerY - content.centerY);
            float maskCos = cosine(mask.rotation);
            float maskSin = sine(mask.rotation);
            float[] xAxis = content.screenToLocal(maskCos * mask.scaleX,
                    maskSin * mask.scaleX);
            float[] yAxis = content.screenToLocal(-maskSin * mask.scaleY,
                    maskCos * mask.scaleY);
            return new float[] { center[0], center[1], xAxis[0], xAxis[1],
                    yAxis[0], yAxis[1] };
        }

        private float[] screenToLocal(float x, float y) {
            float cosine = cosine(rotation);
            float sine = sine(rotation);
            return new float[] { (cosine * x + sine * y) / scaleX,
                    (-sine * x + cosine * y) / scaleY };
        }

        String key() {
            return Float.floatToIntBits(centerX) + "," + Float.floatToIntBits(centerY) + ","
                    + Float.floatToIntBits(scaleX) + "," + Float.floatToIntBits(scaleY) + ","
                    + Float.floatToIntBits(rotation);
        }
    }

    private static final class ImageSwap {
        final DecalTemplate template;
        final DecalImageFrame frame;
        final ClientImage replacement;
        final boolean temporary;
        final GameImage image;
        final GameImage[] teamImages;
        final boolean customGrid;
        final LogicBoolean frameExpression;
        final int bodyFrameMultiplier;

        private ImageSwap(DecalTemplate template, ClientImage replacement, boolean temporary) {
            this.template = template; this.frame = template.imageFrame;
            this.replacement = replacement; this.temporary = temporary;
            this.image = frame.image; this.teamImages = frame.teamColorImages;
            this.customGrid = template.hasCustomFrameGrid;
            this.frameExpression = template.frame;
            this.bodyFrameMultiplier = template.addBodyFrameMultipliedBy;
        }

        static ImageSwap create(DecalTemplate template, ClientImage replacement) {
            return new ImageSwap(template, replacement, false);
        }

        static ImageSwap create(DecalTemplate template, ClientImage replacement,
                                boolean temporary) {
            return new ImageSwap(template, replacement, temporary);
        }

        void apply() {
            frame.image = replacement.nativeImage();
            frame.teamColorImages = null;
            template.hasCustomFrameGrid = false;
            template.frame = null;
            template.addBodyFrameMultipliedBy = 0;
        }

        void restore() {
            frame.image = image;
            frame.teamColorImages = teamImages;
            template.hasCustomFrameGrid = customGrid;
            template.frame = frameExpression;
            template.addBodyFrameMultipliedBy = bodyFrameMultiplier;
            if (temporary) replacement.close();
        }
    }

    private static final class VisibilitySwap {
        final DecalTemplate template;
        final LogicBoolean visible;
        VisibilitySwap(DecalTemplate template, LogicBoolean visible) {
            this.template = template; this.visible = visible;
        }
        void restore() { template.isVisible = visible; }
    }

    private static final class LayerState {
        final List<ImageSwap> images = new ArrayList<ImageSwap>();
        final List<VisibilitySwap> visibility = new ArrayList<VisibilitySwap>();
        void restore() {
            for (int i = images.size() - 1; i >= 0; i--) images.get(i).restore();
            for (int i = visibility.size() - 1; i >= 0; i--) visibility.get(i).restore();
        }
    }

    private static float sourceAlpha(DecalTemplate source, CustomUnit unit) {
        float alpha = source.alpha != null ? source.alpha.readNumber(unit) : 1.0F;
        return clamp(alpha) * ((source.color >>> 24) & 0xff) / 255.0F;
    }

    private static String optionalName(String raw) {
        if (raw == null) return null;
        String value = raw.trim();
        return value.isEmpty() ? null : value;
    }

    private static boolean bool(UnitConfig config, String section, String key,
                                boolean fallback) {
        Boolean value = config.getBoolean(section, key, Boolean.valueOf(fallback));
        return value != null ? value.booleanValue() : fallback;
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String raw, String label) {
        String normalized = raw.trim().replace("_", "").replace("-", "")
                .toUpperCase(Locale.ROOT);
        for (E value : type.getEnumConstants()) {
            if (value.name().replace("_", "").equals(normalized)) return value;
        }
        throw new IllegalArgumentException("unknown " + label + ": " + raw);
    }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static float cosine(float degrees) {
        return (float) Math.cos(Math.toRadians(degrees));
    }

    private static float sine(float degrees) {
        return (float) Math.sin(Math.toRadians(degrees));
    }
}
