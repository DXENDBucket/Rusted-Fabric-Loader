package io.github.endx.iniessentials;

import io.github.endx.rustedfabricapi.api.client.Camera;
import io.github.endx.rustedfabricapi.api.ini.IniFieldDocumentation;
import io.github.endx.rustedfabricapi.api.ini.IniMultiplayerImpact;
import io.github.endx.rustedfabricapi.api.ini.action.IniActionEffectDefinition;
import io.github.endx.rustedfabricapi.api.ini.action.IniActionEffects;
import io.github.endx.rustedfabricapi.api.ini.action.IniActionExecutionContext;
import io.github.endx.rustedfabricapi.api.world.WorldPoint;
import rustedwarfare.unit.Unit;
import rustedwarfare.framework.GameObject;

import java.util.Locale;
import java.util.Optional;

final class CameraActionFields {
    private static final String POSITION_GROUP = "camera_position";

    private CameraActionFields() { }

    static void register() {
        IniActionEffects.register(IniActionEffectDefinition
                .<WorldPoint>builder(IniEssentials.MOD_ID, "camera_center_at", "cameraCenterAt")
                .exclusiveGroup(POSITION_GROUP)
                .decoder(context -> synchronizedValue(parsePoint(context.rawValue())))
                .handler((context, point) -> {
                    if (context.isActorOwnedByLocalPlayer()) Camera.centerAt(point);
                })
                .documentation(documentation(
                        "worldX,worldY",
                        "Centers the local owning player's camera at an absolute world coordinate.",
                        "将本机单位所属玩家的视角中心移动到绝对世界坐标。",
                        "cameraCenterAt: 400,600"))
                .build());

        IniActionEffects.register(IniActionEffectDefinition
                .<WorldPoint>builder(IniEssentials.MOD_ID, "camera_center_by", "cameraCenterBy")
                .exclusiveGroup(POSITION_GROUP)
                .decoder(context -> synchronizedValue(parsePoint(context.rawValue())))
                .handler((context, offset) -> {
                    if (context.isActorOwnedByLocalPlayer()) {
                        Camera.moveCenterBy(offset.x(), offset.y());
                    }
                })
                .documentation(documentation(
                        "deltaX,deltaY",
                        "Moves the local owning player's camera center by a world-space offset.",
                        "按照世界坐标偏移量移动本机单位所属玩家的视角中心。",
                        "cameraCenterBy: 80,-40"))
                .build());

        IniActionEffects.register(IniActionEffectDefinition
                .<ContextTarget>builder(IniEssentials.MOD_ID, "camera_center_on", "cameraCenterOn")
                .exclusiveGroup(POSITION_GROUP)
                .decoder(context -> synchronizedValue(parseContextTarget(context.rawValue())))
                .handler(CameraActionFields::centerOnContext)
                .documentation(documentation(
                        "self|target|actionTarget[,offsetX,offsetY]",
                        "Centers the camera on the acting unit, unit target, or action target point with an optional offset.",
                        "将视角居中到动作单位、目标单位或动作目标点，并可附加坐标偏移。",
                        "cameraCenterOn: actionTarget,0,-60"))
                .build());

        IniActionEffects.register(IniActionEffectDefinition
                .<Float>builder(IniEssentials.MOD_ID, "camera_target_zoom", "cameraTargetZoom")
                .decoder(context -> synchronizedValue(parsePositiveFloat(context.rawValue())))
                .handler((context, zoom) -> {
                    if (context.isActorOwnedByLocalPlayer()) Camera.setTargetZoom(zoom);
                })
                .documentation(documentation(
                        "positive float",
                        "Sets the local owning player's native smoothed camera zoom target.",
                        "设置本机单位所属玩家的原版平滑视角缩放目标。",
                        "cameraTargetZoom: 1.25"))
                .build());

        IniActionEffects.register(IniActionEffectDefinition
                .<Boolean>builder(IniEssentials.MOD_ID, "camera_stop_movement", "cameraStopMovement")
                .decoder(context -> synchronizedValue(parseBoolean(context.rawValue())))
                .handler((context, enabled) -> {
                    if (enabled && context.isActorOwnedByLocalPlayer()) Camera.stopMovement();
                })
                .documentation(documentation(
                        "boolean",
                        "Clears native camera scroll momentum when the action executes.",
                        "在动作执行时清除原版摄像机的滚动惯性。",
                        "cameraStopMovement: true"))
                .build());
    }

    private static void centerOnContext(IniActionExecutionContext context, ContextTarget target) {
        if (!context.isActorOwnedByLocalPlayer()) return;
        Optional<WorldPoint> base;
        switch (target.anchor) {
            case SELF:
                base = Optional.of(context.actorPosition());
                break;
            case TARGET:
                base = context.targetUnit().map(CameraActionFields::position);
                break;
            case ACTION_TARGET:
                base = context.actionTargetPosition();
                break;
            default:
                throw new AssertionError(target.anchor);
        }
        base.ifPresent(point -> Camera.centerAt(
                point.x() + target.offset.x(), point.y() + target.offset.y()));
    }

    private static WorldPoint position(Unit unit) {
        GameObject object = unit;
        return new WorldPoint(object.x, object.y);
    }

    private static WorldPoint parsePoint(String raw) {
        String[] parts = raw.split(",", -1);
        if (parts.length != 2) throw new IllegalArgumentException("expected x,y");
        return new WorldPoint(parseFinite(parts[0]), parseFinite(parts[1]));
    }

    private static ContextTarget parseContextTarget(String raw) {
        String[] parts = raw.split(",", -1);
        if (parts.length != 1 && parts.length != 3) {
            throw new IllegalArgumentException(
                    "expected self|target|actionTarget[,offsetX,offsetY]");
        }
        String name = parts[0].trim().toLowerCase(Locale.ROOT);
        Anchor anchor;
        if ("self".equals(name)) {
            anchor = Anchor.SELF;
        } else if ("target".equals(name)) {
            anchor = Anchor.TARGET;
        } else if ("actiontarget".equals(name)) {
            anchor = Anchor.ACTION_TARGET;
        } else {
            throw new IllegalArgumentException("unknown camera anchor: " + parts[0].trim());
        }
        WorldPoint offset = parts.length == 3
                ? new WorldPoint(parseFinite(parts[1]), parseFinite(parts[2]))
                : new WorldPoint(0.0F, 0.0F);
        return new ContextTarget(anchor, offset);
    }

    private static float parsePositiveFloat(String raw) {
        float value = parseFinite(raw);
        if (!(value > 0.0F)) throw new IllegalArgumentException("expected a positive number");
        return value;
    }

    private static float parseFinite(String raw) {
        float value = Float.parseFloat(raw.trim());
        if (!Float.isFinite(value)) throw new IllegalArgumentException("number must be finite");
        return value;
    }

    private static Boolean parseBoolean(String raw) {
        String value = raw.trim();
        if ("true".equalsIgnoreCase(value)) return Boolean.TRUE;
        if ("false".equalsIgnoreCase(value)) return Boolean.FALSE;
        throw new IllegalArgumentException("expected true or false, got: " + raw);
    }

    private static <T> T synchronizedValue(T value) {
        IniEssentials.activateSynchronizedRequirement();
        return value;
    }

    private static IniFieldDocumentation documentation(String type, String english,
                                                       String chinese, String example) {
        return new IniFieldDocumentation(type, english, chinese, example,
                IniMultiplayerImpact.CLIENT_ONLY);
    }

    private enum Anchor { SELF, TARGET, ACTION_TARGET }

    private static final class ContextTarget {
        private final Anchor anchor;
        private final WorldPoint offset;

        private ContextTarget(Anchor anchor, WorldPoint offset) {
            this.anchor = anchor;
            this.offset = offset;
        }
    }
}
