package io.github.endx.rustedfabricapi.api.client;

import io.github.endx.rustedfabricapi.api.world.WorldPoint;
import rustedwarfare.core.GameEngine;
import rustedwarfare.framework.GameObject;

import java.util.Objects;

/** Typed world-camera queries and update-thread controls. */
public final class Camera {
    private Camera() {
    }

    public static CameraSnapshot snapshot() {
        GameEngine engine = RustedWarfareClient.requireEngine();
        return new CameraSnapshot(engine.viewpointX, engine.viewpointY,
                engine.viewpointWidth, engine.visibleWorldHeight, engine.zoom, engine.targetZoom);
    }

    public static WorldPoint center() {
        return snapshot().center();
    }

    public static WorldPoint topLeft() {
        return snapshot().topLeft();
    }

    /** Moves the top-left corner and clamps it to the current map. */
    public static void moveTopLeftTo(float worldX, float worldY) {
        requireFinite(worldX, "worldX");
        requireFinite(worldY, "worldY");
        GameEngine engine = RustedWarfareClient.requireEngine();
        engine.setCameraPosition(worldX, worldY);
        engine.clampCameraPosition();
    }

    /** Centers and clamps the camera around a world position. */
    public static void centerAt(float worldX, float worldY) {
        requireFinite(worldX, "worldX");
        requireFinite(worldY, "worldY");
        GameEngine engine = RustedWarfareClient.requireEngine();
        engine.centerCameraAt(worldX, worldY);
        engine.clampCameraPosition();
    }

    public static void centerAt(WorldPoint point) {
        Objects.requireNonNull(point, "point");
        centerAt(point.x(), point.y());
    }

    public static void centerAt(GameObject object) {
        Objects.requireNonNull(object, "object");
        centerAt(object.x, object.y);
    }

    /** Centers on a game object plus a world-space offset. */
    public static void centerAt(GameObject object, float offsetX, float offsetY) {
        Objects.requireNonNull(object, "object");
        requireFinite(offsetX, "offsetX");
        requireFinite(offsetY, "offsetY");
        centerAt(object.x + offsetX, object.y + offsetY);
    }

    /** Moves the camera center relative to its current world position. */
    public static void moveCenterBy(float deltaX, float deltaY) {
        requireFinite(deltaX, "deltaX");
        requireFinite(deltaY, "deltaY");
        WorldPoint center = center();
        centerAt(center.x() + deltaX, center.y() + deltaY);
    }

    /** Moves the top-left corner relative to its current world position. */
    public static void moveTopLeftBy(float deltaX, float deltaY) {
        requireFinite(deltaX, "deltaX");
        requireFinite(deltaY, "deltaY");
        CameraSnapshot snapshot = snapshot();
        moveTopLeftTo(snapshot.left() + deltaX, snapshot.top() + deltaY);
    }

    /** Clears native keyboard/edge-scroll momentum without changing the current position. */
    public static void stopMovement() {
        GameEngine engine = RustedWarfareClient.requireEngine();
        engine.cameraMovementX = 0.0F;
        engine.cameraMovementY = 0.0F;
    }

    /** Sets the zoom target consumed by the game's normal smoothing and limit logic. */
    public static void setTargetZoom(float zoom) {
        requirePositiveFinite(zoom, "zoom");
        RustedWarfareClient.requireEngine().targetZoom = zoom;
    }

    public static boolean isVisible(float worldX, float worldY, float radius) {
        requireFinite(worldX, "worldX");
        requireFinite(worldY, "worldY");
        requireNonNegativeFinite(radius, "radius");
        return RustedWarfareClient.requireEngine().isCircleVisibleInCamera(worldX, worldY, radius);
    }

    public static boolean isVisible(GameObject object) {
        Objects.requireNonNull(object, "object");
        return object.isVisibleInCamera(RustedWarfareClient.requireEngine());
    }

    public static WorldPoint mouseWorldPosition() {
        GameEngine engine = RustedWarfareClient.requireEngine();
        float zoom = engine.zoom;
        if (!(zoom > 0.0F) || !Float.isFinite(zoom)) {
            throw new IllegalStateException("Camera zoom is not initialized");
        }
        return new WorldPoint(engine.viewpointXSnapped + engine.getPrimaryTouchX() / zoom,
                engine.viewpointYSnapped + engine.getPrimaryTouchY() / zoom);
    }

    private static void requireFinite(float value, String name) {
        if (!Float.isFinite(value)) throw new IllegalArgumentException(name + " must be finite");
    }

    private static void requirePositiveFinite(float value, String name) {
        requireFinite(value, name);
        if (!(value > 0.0F)) throw new IllegalArgumentException(name + " must be positive");
    }

    private static void requireNonNegativeFinite(float value, String name) {
        requireFinite(value, name);
        if (value < 0.0F) throw new IllegalArgumentException(name + " must be non-negative");
    }
}
