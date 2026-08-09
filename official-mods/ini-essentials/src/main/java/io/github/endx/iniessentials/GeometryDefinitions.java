package io.github.endx.iniessentials;

import io.github.endx.rustedfabricapi.api.geometry.GeometryMask;
import io.github.endx.rustedfabricapi.api.geometry.GeometryMasks;
import io.github.endx.rustedfabricapi.api.geometry.GeometryPoint;
import io.github.endx.rustedfabricapi.api.ini.IniFieldDefinition;
import io.github.endx.rustedfabricapi.api.ini.IniFieldDocumentation;
import io.github.endx.rustedfabricapi.api.ini.IniExtensions;
import io.github.endx.rustedfabricapi.api.ini.IniMultiplayerImpact;
import io.github.endx.rustedfabricapi.api.ini.IniSectionSelector;
import rustedwarfare.unit.OrderableUnit;
import rustedwarfare.util.UnitConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

final class GeometryDefinitions {
    private static final String PREFIX = "geometry_";
    private static final Map<Object, Map<String, Template>> BY_METADATA =
            Collections.synchronizedMap(new WeakHashMap<Object, Map<String, Template>>());

    private GeometryDefinitions() { }

    static void register() {
        IniExtensions.register(IniFieldDefinition
                .<String>builder(IniEssentials.MOD_ID, "geometry_definition",
                        IniSectionSelector.prefix(PREFIX), "type")
                .decoder(context -> context.rawValue().trim())
                .validator((context, value) -> parseType(value))
                .applier(field -> {
                    IniEssentials.activateSynchronizedRequirement();
                    parseAndStore(field.metadata(), (UnitConfig) field.unitConfig(),
                            field.source().section());
                })
                .documentation(new IniFieldDocumentation(
                        "circle|ellipse|rectangle|sector|ring|arc|line|polygon|union|intersection|difference",
                        "Declares a reusable runtime-evaluated geometry mask.",
                        "声明一个可复用、运行时求值的几何遮罩。",
                        "[geometry_front]\ntype: sector\nradius: 240\nsweepAngle: 180",
                        IniMultiplayerImpact.GAMEPLAY_SYNCED))
                .build());
    }

    static Template require(Object metadata, String name) {
        String key = normalizeName(name);
        Map<String, Template> definitions = BY_METADATA.get(metadata);
        Template result = definitions != null ? definitions.get(key) : null;
        if (result == null) throw new IllegalArgumentException("unknown geometry: " + name);
        return result;
    }

    private static void parseAndStore(Object metadata, UnitConfig config, String section) {
        String name = section.substring(PREFIX.length());
        if (name.trim().isEmpty()) throw new IllegalArgumentException("geometry section name must not be empty");
        Type type = parseType(required(config, section, "type"));
        Template template = new Template(name, type,
                expression(metadata, config, section, "radius", "1"),
                expression(metadata, config, section, "radiusX", "1"),
                expression(metadata, config, section, "radiusY", "1"),
                expression(metadata, config, section, "width", "1"),
                expression(metadata, config, section, "height", "1"),
                expression(metadata, config, section, "innerRadius", "0"),
                expression(metadata, config, section, "startAngle", "0"),
                expression(metadata, config, section, "sweepAngle", "360"),
                expression(metadata, config, section, "rotation", "0"),
                expression(metadata, config, section, "offsetX", "0"),
                expression(metadata, config, section, "offsetY", "0"),
                expression(metadata, config, section, "thickness", "1"),
                parsePoints(metadata, optional(config, section, "points")),
                parseNames(optional(config, section, "components")));
        Map<String, Template> definitions = BY_METADATA.get(metadata);
        if (definitions == null) {
            definitions = new LinkedHashMap<String, Template>();
            BY_METADATA.put(metadata, definitions);
        }
        String key = normalizeName(name);
        if (definitions.put(key, template) != null) {
            throw new IllegalArgumentException("duplicate geometry definition: " + name);
        }
    }

    private static NumericExpression expression(Object metadata, UnitConfig config,
                                                String section, String key, String fallback) {
        return NumericExpression.compile(metadata, optional(config, section, key), fallback);
    }

    private static List<PointExpression> parsePoints(Object metadata, String raw) {
        if (raw == null || raw.trim().isEmpty()) return Collections.emptyList();
        ArrayList<PointExpression> points = new ArrayList<PointExpression>();
        for (String pair : splitTopLevel(raw, ';')) {
            List<String> coordinates = splitTopLevel(pair, ',');
            if (coordinates.size() != 2) {
                throw new IllegalArgumentException("polygon points must use x,y; x,y syntax");
            }
            points.add(new PointExpression(NumericExpression.compile(metadata, coordinates.get(0)),
                    NumericExpression.compile(metadata, coordinates.get(1))));
        }
        return Collections.unmodifiableList(points);
    }

    private static List<String> parseNames(String raw) {
        if (raw == null || raw.trim().isEmpty()) return Collections.emptyList();
        ArrayList<String> result = new ArrayList<String>();
        for (String part : raw.split(",")) result.add(normalizeName(part));
        return Collections.unmodifiableList(result);
    }

    private static List<String> splitTopLevel(String raw, char delimiter) {
        ArrayList<String> result = new ArrayList<String>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '(' || c == '[') depth++;
            else if (c == ')' || c == ']') depth--;
            else if (c == delimiter && depth == 0) {
                result.add(requiredPart(raw.substring(start, i)));
                start = i + 1;
            }
            if (depth < 0) throw new IllegalArgumentException("unbalanced geometry expression");
        }
        if (depth != 0) throw new IllegalArgumentException("unbalanced geometry expression");
        result.add(requiredPart(raw.substring(start)));
        return result;
    }

    private static String requiredPart(String raw) {
        String value = raw.trim();
        if (value.isEmpty()) throw new IllegalArgumentException("empty geometry value");
        return value;
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

    private static Type parseType(String raw) {
        try {
            return Type.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException("unknown geometry type: " + raw);
        }
    }

    private static String normalizeName(String raw) {
        String value = raw != null ? raw.trim().toLowerCase(Locale.ROOT) : "";
        if (value.isEmpty()) throw new IllegalArgumentException("geometry name must not be empty");
        return value;
    }

    enum Type { CIRCLE, ELLIPSE, RECTANGLE, SECTOR, RING, ARC, LINE, POLYGON,
        UNION, INTERSECTION, DIFFERENCE }

    static final class Template {
        private final String name;
        private final Type type;
        private final NumericExpression radius, radiusX, radiusY, width, height;
        private final NumericExpression innerRadius, startAngle, sweepAngle;
        private final NumericExpression rotation, offsetX, offsetY, thickness;
        private final List<PointExpression> points;
        private final List<String> components;

        private Template(String name, Type type, NumericExpression radius,
                         NumericExpression radiusX, NumericExpression radiusY,
                         NumericExpression width, NumericExpression height,
                         NumericExpression innerRadius, NumericExpression startAngle,
                         NumericExpression sweepAngle, NumericExpression rotation,
                         NumericExpression offsetX, NumericExpression offsetY,
                         NumericExpression thickness, List<PointExpression> points,
                         List<String> components) {
            this.name = name; this.type = type; this.radius = radius;
            this.radiusX = radiusX; this.radiusY = radiusY; this.width = width;
            this.height = height; this.innerRadius = innerRadius;
            this.startAngle = startAngle; this.sweepAngle = sweepAngle;
            this.rotation = rotation; this.offsetX = offsetX; this.offsetY = offsetY;
            this.thickness = thickness; this.points = points; this.components = components;
        }

        GeometryMask resolve(Object metadata, OrderableUnit unit) {
            return resolve(metadata, unit, new HashSet<String>(), 0);
        }

        private GeometryMask resolve(Object metadata, OrderableUnit unit,
                                     Set<String> stack, int depth) {
            if (depth > 32 || !stack.add(normalizeName(name))) {
                throw new IllegalArgumentException("cyclic or too-deep geometry composition at " + name);
            }
            GeometryMask mask;
            switch (type) {
                case CIRCLE: mask = GeometryMasks.circle(radius.evaluate(unit)); break;
                case ELLIPSE: mask = GeometryMasks.ellipse(radiusX.evaluate(unit), radiusY.evaluate(unit)); break;
                case RECTANGLE: mask = GeometryMasks.rectangle(width.evaluate(unit), height.evaluate(unit)); break;
                case SECTOR: mask = GeometryMasks.sector(innerRadius.evaluate(unit), radius.evaluate(unit),
                        startAngle.evaluate(unit), sweepAngle.evaluate(unit)); break;
                case RING: mask = GeometryMasks.ring(innerRadius.evaluate(unit), radius.evaluate(unit)); break;
                case ARC: mask = GeometryMasks.arc(radius.evaluate(unit), thickness.evaluate(unit),
                        startAngle.evaluate(unit), sweepAngle.evaluate(unit)); break;
                case LINE: mask = GeometryMasks.line(width.evaluate(unit), thickness.evaluate(unit)); break;
                case POLYGON:
                    ArrayList<GeometryPoint> resolved = new ArrayList<GeometryPoint>();
                    for (PointExpression point : points) resolved.add(point.evaluate(unit));
                    mask = GeometryMasks.polygon(resolved);
                    break;
                case UNION:
                case INTERSECTION:
                case DIFFERENCE:
                    if (components.isEmpty()) throw new IllegalArgumentException(type + " geometry requires components");
                    ArrayList<GeometryMask> children = new ArrayList<GeometryMask>();
                    for (String child : components) {
                        children.add(require(metadata, child).resolve(metadata, unit,
                                new HashSet<String>(stack), depth + 1));
                    }
                    if (type == Type.UNION) mask = GeometryMasks.union(children.toArray(new GeometryMask[0]));
                    else if (type == Type.INTERSECTION) mask = GeometryMasks.intersection(children.toArray(new GeometryMask[0]));
                    else mask = GeometryMasks.difference(children.get(0),
                            children.subList(1, children.size()).toArray(new GeometryMask[0]));
                    break;
                default: throw new AssertionError(type);
            }
            stack.remove(normalizeName(name));
            return GeometryMasks.transform(mask, offsetX.evaluate(unit), offsetY.evaluate(unit),
                    rotation.evaluate(unit));
        }
    }

    private static final class PointExpression {
        private final NumericExpression x;
        private final NumericExpression y;
        private PointExpression(NumericExpression x, NumericExpression y) { this.x = x; this.y = y; }
        private GeometryPoint evaluate(OrderableUnit unit) {
            return new GeometryPoint(x.evaluate(unit), y.evaluate(unit));
        }
    }
}
