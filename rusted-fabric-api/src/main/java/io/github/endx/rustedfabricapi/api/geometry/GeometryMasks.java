package io.github.endx.rustedfabricapi.api.geometry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Factory and boolean-composition operations for finite geometry masks. */
public final class GeometryMasks {
    private GeometryMasks() { }

    public static GeometryMask circle(float radius) {
        return ellipse(radius, radius);
    }

    public static GeometryMask ellipse(float radiusX, float radiusY) {
        positive(radiusX, "radiusX");
        positive(radiusY, "radiusY");
        return new EllipseMask(radiusX, radiusY);
    }

    public static GeometryMask rectangle(float width, float height) {
        positive(width, "width");
        positive(height, "height");
        return new RectangleMask(width * 0.5F, height * 0.5F);
    }

    public static GeometryMask ring(float innerRadius, float outerRadius) {
        nonNegative(innerRadius, "innerRadius");
        positive(outerRadius, "outerRadius");
        if (innerRadius >= outerRadius) {
            throw new IllegalArgumentException("innerRadius must be smaller than outerRadius");
        }
        return new RadialMask(innerRadius, outerRadius, 0.0F, 360.0F);
    }

    public static GeometryMask sector(float innerRadius, float outerRadius,
                                      float startAngle, float sweepAngle) {
        nonNegative(innerRadius, "innerRadius");
        positive(outerRadius, "outerRadius");
        finite(startAngle, "startAngle");
        finite(sweepAngle, "sweepAngle");
        if (innerRadius >= outerRadius) {
            throw new IllegalArgumentException("innerRadius must be smaller than outerRadius");
        }
        if (sweepAngle == 0.0F) throw new IllegalArgumentException("sweepAngle must not be zero");
        return new RadialMask(innerRadius, outerRadius, startAngle, sweepAngle);
    }

    public static GeometryMask arc(float radius, float thickness,
                                   float startAngle, float sweepAngle) {
        positive(radius, "radius");
        positive(thickness, "thickness");
        float half = thickness * 0.5F;
        return sector(Math.max(0.0F, radius - half), radius + half, startAngle, sweepAngle);
    }

    public static GeometryMask line(float length, float thickness) {
        positive(length, "length");
        positive(thickness, "thickness");
        return rectangle(length, thickness);
    }

    public static GeometryMask polygon(List<GeometryPoint> points) {
        Objects.requireNonNull(points, "points");
        if (points.size() < 3) throw new IllegalArgumentException("polygon requires at least 3 points");
        return new PolygonMask(points);
    }

    public static GeometryMask transform(GeometryMask mask, float offsetX,
                                         float offsetY, float rotationDegrees) {
        Objects.requireNonNull(mask, "mask");
        finite(offsetX, "offsetX");
        finite(offsetY, "offsetY");
        finite(rotationDegrees, "rotationDegrees");
        if (offsetX == 0.0F && offsetY == 0.0F && rotationDegrees == 0.0F) return mask;
        return new TransformMask(mask, offsetX, offsetY, rotationDegrees);
    }

    public static GeometryMask union(GeometryMask... masks) {
        return composite(CompositeKind.UNION, Arrays.asList(masks));
    }

    public static GeometryMask intersection(GeometryMask... masks) {
        return composite(CompositeKind.INTERSECTION, Arrays.asList(masks));
    }

    public static GeometryMask difference(GeometryMask base, GeometryMask... subtract) {
        ArrayList<GeometryMask> masks = new ArrayList<GeometryMask>();
        masks.add(Objects.requireNonNull(base, "base"));
        masks.addAll(Arrays.asList(subtract));
        return composite(CompositeKind.DIFFERENCE, masks);
    }

    private static GeometryMask composite(CompositeKind kind, List<GeometryMask> masks) {
        if (masks == null || masks.isEmpty()) throw new IllegalArgumentException("at least one mask is required");
        ArrayList<GeometryMask> checked = new ArrayList<GeometryMask>(masks.size());
        for (GeometryMask mask : masks) checked.add(Objects.requireNonNull(mask, "mask"));
        return new CompositeMask(kind, checked);
    }

    private static final class EllipseMask implements GeometryMask {
        private final float radiusX;
        private final float radiusY;
        private final GeometryBounds bounds;
        private EllipseMask(float radiusX, float radiusY) {
            this.radiusX = radiusX;
            this.radiusY = radiusY;
            bounds = new GeometryBounds(-radiusX, -radiusY, radiusX, radiusY);
        }
        @Override public GeometryBounds bounds() { return bounds; }
        @Override public boolean contains(float x, float y) {
            double nx = x / radiusX;
            double ny = y / radiusY;
            return nx * nx + ny * ny <= 1.0D;
        }
    }

    private static final class RectangleMask implements GeometryMask {
        private final float halfWidth;
        private final float halfHeight;
        private final GeometryBounds bounds;
        private RectangleMask(float halfWidth, float halfHeight) {
            this.halfWidth = halfWidth;
            this.halfHeight = halfHeight;
            bounds = new GeometryBounds(-halfWidth, -halfHeight, halfWidth, halfHeight);
        }
        @Override public GeometryBounds bounds() { return bounds; }
        @Override public boolean contains(float x, float y) {
            return Math.abs(x) <= halfWidth && Math.abs(y) <= halfHeight;
        }
    }

    private static final class RadialMask implements GeometryMask {
        private final float innerSquared;
        private final float outerRadius;
        private final float outerSquared;
        private final float start;
        private final float sweep;
        private final GeometryBounds bounds;
        private RadialMask(float innerRadius, float outerRadius, float start, float sweep) {
            innerSquared = innerRadius * innerRadius;
            this.outerRadius = outerRadius;
            outerSquared = outerRadius * outerRadius;
            this.start = normalize(start);
            this.sweep = sweep;
            bounds = new GeometryBounds(-outerRadius, -outerRadius, outerRadius, outerRadius);
        }
        @Override public GeometryBounds bounds() { return bounds; }
        @Override public boolean contains(float x, float y) {
            float squared = x * x + y * y;
            if (squared < innerSquared || squared > outerSquared) return false;
            if (Math.abs(sweep) >= 360.0F) return true;
            float angle = normalize((float) Math.toDegrees(StrictMath.atan2(y, x)));
            if (sweep > 0.0F) return normalize(angle - start) <= sweep;
            return normalize(start - angle) <= -sweep;
        }
    }

    private static final class PolygonMask implements GeometryMask {
        private final List<GeometryPoint> points;
        private final GeometryBounds bounds;
        private PolygonMask(List<GeometryPoint> source) {
            points = Collections.unmodifiableList(new ArrayList<GeometryPoint>(source));
            float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY;
            float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;
            for (GeometryPoint point : points) {
                Objects.requireNonNull(point, "point");
                minX = Math.min(minX, point.x()); minY = Math.min(minY, point.y());
                maxX = Math.max(maxX, point.x()); maxY = Math.max(maxY, point.y());
            }
            bounds = new GeometryBounds(minX, minY, maxX, maxY);
        }
        @Override public GeometryBounds bounds() { return bounds; }
        @Override public boolean contains(float x, float y) {
            boolean inside = false;
            for (int i = 0, j = points.size() - 1; i < points.size(); j = i++) {
                GeometryPoint a = points.get(i);
                GeometryPoint b = points.get(j);
                boolean crosses = (a.y() > y) != (b.y() > y)
                        && x < (b.x() - a.x()) * (y - a.y()) / (b.y() - a.y()) + a.x();
                if (crosses) inside = !inside;
            }
            return inside;
        }
    }

    private static final class TransformMask implements GeometryMask {
        private final GeometryMask delegate;
        private final float offsetX;
        private final float offsetY;
        private final double cos;
        private final double sin;
        private final GeometryBounds bounds;
        private TransformMask(GeometryMask delegate, float offsetX, float offsetY, float degrees) {
            this.delegate = delegate;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            double radians = StrictMath.toRadians(degrees);
            cos = StrictMath.cos(radians);
            sin = StrictMath.sin(radians);
            GeometryBounds source = delegate.bounds();
            float[] xs = {source.minX(), source.maxX(), source.maxX(), source.minX()};
            float[] ys = {source.minY(), source.minY(), source.maxY(), source.maxY()};
            float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY;
            float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;
            for (int i = 0; i < 4; i++) {
                float x = (float) (xs[i] * cos - ys[i] * sin) + offsetX;
                float y = (float) (xs[i] * sin + ys[i] * cos) + offsetY;
                minX = Math.min(minX, x); minY = Math.min(minY, y);
                maxX = Math.max(maxX, x); maxY = Math.max(maxY, y);
            }
            bounds = new GeometryBounds(minX, minY, maxX, maxY);
        }
        @Override public GeometryBounds bounds() { return bounds; }
        @Override public boolean contains(float x, float y) {
            double dx = x - offsetX;
            double dy = y - offsetY;
            float localX = (float) (dx * cos + dy * sin);
            float localY = (float) (-dx * sin + dy * cos);
            return delegate.contains(localX, localY);
        }
    }

    private enum CompositeKind { UNION, INTERSECTION, DIFFERENCE }

    private static final class CompositeMask implements GeometryMask {
        private final CompositeKind kind;
        private final List<GeometryMask> masks;
        private final GeometryBounds bounds;
        private CompositeMask(CompositeKind kind, List<GeometryMask> masks) {
            this.kind = kind;
            this.masks = Collections.unmodifiableList(new ArrayList<GeometryMask>(masks));
            GeometryBounds result = masks.get(0).bounds();
            if (kind == CompositeKind.UNION) {
                for (int i = 1; i < masks.size(); i++) result = result.union(masks.get(i).bounds());
            } else if (kind == CompositeKind.INTERSECTION) {
                for (int i = 1; i < masks.size(); i++) result = result.intersect(masks.get(i).bounds());
            }
            bounds = result;
        }
        @Override public GeometryBounds bounds() { return bounds; }
        @Override public boolean contains(float x, float y) {
            if (kind == CompositeKind.UNION) {
                for (GeometryMask mask : masks) if (mask.contains(x, y)) return true;
                return false;
            }
            if (kind == CompositeKind.INTERSECTION) {
                for (GeometryMask mask : masks) if (!mask.contains(x, y)) return false;
                return true;
            }
            if (!masks.get(0).contains(x, y)) return false;
            for (int i = 1; i < masks.size(); i++) if (masks.get(i).contains(x, y)) return false;
            return true;
        }
    }

    private static float normalize(float degrees) {
        float result = degrees % 360.0F;
        return result < 0.0F ? result + 360.0F : result;
    }

    private static void positive(float value, String name) {
        finite(value, name);
        if (value <= 0.0F) throw new IllegalArgumentException(name + " must be positive");
    }
    private static void nonNegative(float value, String name) {
        finite(value, name);
        if (value < 0.0F) throw new IllegalArgumentException(name + " must not be negative");
    }
    private static void finite(float value, String name) {
        if (!Float.isFinite(value)) throw new IllegalArgumentException(name + " must be finite");
    }
}
