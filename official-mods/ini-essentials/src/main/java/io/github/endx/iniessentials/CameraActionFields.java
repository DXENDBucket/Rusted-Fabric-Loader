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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class CameraActionFields {
    private static final String POSITION_GROUP = "camera_position";

    private CameraActionFields() { }

    static void register() {
        IniActionEffects.register(IniActionEffectDefinition
                .<DynamicPoint>builder(IniEssentials.MOD_ID, "camera_center_at", "cameraCenterAt")
                .exclusiveGroup(POSITION_GROUP)
                .decoder(context -> synchronizedValue(parsePoint(
                        context.metadata(), context.rawValue())))
                .handler((context, point) -> {
                    if (context.isActorOwnedByLocalPlayer()) {
                        Camera.centerAt(point.evaluate(context));
                    }
                })
                .documentation(documentation(
                        "runtimeNumberX,runtimeNumberY",
                        "Centers the local owning player's camera at runtime absolute coordinates.",
                        "将本机玩家视角移动到运行时求值的绝对坐标。",
                        "cameraCenterAt: memory.homeX,self.resource.cameraY"))
                .build());

        IniActionEffects.register(IniActionEffectDefinition
                .<DynamicPoint>builder(IniEssentials.MOD_ID, "camera_center_by", "cameraCenterBy")
                .exclusiveGroup(POSITION_GROUP)
                .decoder(context -> synchronizedValue(parsePoint(
                        context.metadata(), context.rawValue())))
                .handler((context, offset) -> {
                    if (context.isActorOwnedByLocalPlayer()) {
                        WorldPoint value = offset.evaluate(context);
                        Camera.moveCenterBy(value.x(), value.y());
                    }
                })
                .documentation(documentation(
                        "runtimeDeltaX,runtimeDeltaY",
                        "Moves the local owning player's camera by a runtime world-space offset.",
                        "按运行时求值的世界坐标偏移量移动本机玩家视角。",
                        "cameraCenterBy: memory.panX,memory.panY"))
                .build());

        IniActionEffects.register(IniActionEffectDefinition
                .<ContextTarget>builder(IniEssentials.MOD_ID, "camera_center_on", "cameraCenterOn")
                .exclusiveGroup(POSITION_GROUP)
                .decoder(context -> synchronizedValue(parseContextTarget(
                        context.metadata(), context.rawValue())))
                .handler(CameraActionFields::centerOnContext)
                .documentation(documentation(
                        "self|target|actionTarget[,runtimeOffsetX,runtimeOffsetY]",
                        "Centers on a contextual target with optional runtime offsets.",
                        "将视角居中到上下文目标，并可附加运行时坐标偏移。",
                        "cameraCenterOn: actionTarget,memory.offsetX,memory.offsetY"))
                .build());

        IniActionEffects.register(IniActionEffectDefinition
                .<NumericExpression>builder(IniEssentials.MOD_ID, "camera_target_zoom", "cameraTargetZoom")
                .decoder(context -> synchronizedValue(NumericExpression.compile(
                        context.metadata(), context.rawValue())))
                .handler((context, zoomExpression) -> {
                    if (context.isActorOwnedByLocalPlayer()) {
                        float zoom = zoomExpression.evaluate(context.actor());
                        if (!(zoom > 0.0F)) {
                            throw new IllegalArgumentException(
                                    "cameraTargetZoom must evaluate to a positive number");
                        }
                        Camera.setTargetZoom(zoom);
                    }
                })
                .documentation(documentation(
                        "positive runtime number",
                        "Sets the native smoothed camera zoom target from a runtime expression.",
                        "根据运行时表达式设置原版平滑视角缩放目标。",
                        "cameraTargetZoom: clamp(memory.zoom,0.5,3)"))
                .build());

        IniActionEffects.register(IniActionEffectDefinition
                .<BooleanExpression>builder(IniEssentials.MOD_ID, "camera_stop_movement", "cameraStopMovement")
                .decoder(context -> synchronizedValue(BooleanExpression.compile(
                        context.metadata(), context.rawValue())))
                .handler((context, enabled) -> {
                    if (context.isActorOwnedByLocalPlayer()
                            && enabled.evaluate(context.actor())) Camera.stopMovement();
                })
                .documentation(documentation(
                        "runtime LogicBoolean",
                        "Clears native camera momentum when the runtime condition is true.",
                        "当运行时条件为真时清除原版摄像机惯性。",
                        "cameraStopMovement: memory.lockCamera"))
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
        base.ifPresent(point -> {
            WorldPoint offset = target.offset != null
                    ? target.offset.evaluate(context) : new WorldPoint(0.0F, 0.0F);
            Camera.centerAt(point.x() + offset.x(), point.y() + offset.y());
        });
    }

    private static WorldPoint position(Unit unit) {
        GameObject object = unit;
        return new WorldPoint(object.x, object.y);
    }

    private static DynamicPoint parsePoint(Object metadata, String raw) {
        List<String> parts = splitTopLevel(raw);
        if (parts.size() != 2) throw new IllegalArgumentException("expected x,y");
        return new DynamicPoint(NumericExpression.compile(metadata, parts.get(0)),
                NumericExpression.compile(metadata, parts.get(1)));
    }

    private static ContextTarget parseContextTarget(Object metadata, String raw) {
        List<String> parts = splitTopLevel(raw);
        if (parts.size() != 1 && parts.size() != 3) {
            throw new IllegalArgumentException(
                    "expected self|target|actionTarget[,offsetX,offsetY]");
        }
        String name = parts.get(0).trim().toLowerCase(Locale.ROOT);
        Anchor anchor;
        if ("self".equals(name)) {
            anchor = Anchor.SELF;
        } else if ("target".equals(name)) {
            anchor = Anchor.TARGET;
        } else if ("actiontarget".equals(name)) {
            anchor = Anchor.ACTION_TARGET;
        } else {
            throw new IllegalArgumentException("unknown camera anchor: " + parts.get(0).trim());
        }
        DynamicPoint offset = parts.size() == 3
                ? new DynamicPoint(NumericExpression.compile(metadata, parts.get(1)),
                        NumericExpression.compile(metadata, parts.get(2)))
                : null;
        return new ContextTarget(anchor, offset);
    }

    private static List<String> splitTopLevel(String raw) {
        ArrayList<String> result = new ArrayList<String>();
        int depth = 0;
        int start = 0;
        for (int index = 0; index < raw.length(); index++) {
            char value = raw.charAt(index);
            if (value == '(' || value == '[') depth++;
            else if (value == ')' || value == ']') depth--;
            else if (value == ',' && depth == 0) {
                result.add(requiredPart(raw.substring(start, index)));
                start = index + 1;
            }
            if (depth < 0) throw new IllegalArgumentException("unbalanced camera expression");
        }
        if (depth != 0) throw new IllegalArgumentException("unbalanced camera expression");
        result.add(requiredPart(raw.substring(start)));
        return result;
    }

    private static String requiredPart(String raw) {
        String value = raw.trim();
        if (value.isEmpty()) throw new IllegalArgumentException("empty camera expression");
        return value;
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

    private static final class DynamicPoint {
        private final NumericExpression x;
        private final NumericExpression y;

        private DynamicPoint(NumericExpression x, NumericExpression y) {
            this.x = x;
            this.y = y;
        }

        private WorldPoint evaluate(IniActionExecutionContext context) {
            return new WorldPoint(x.evaluate(context.actor()), y.evaluate(context.actor()));
        }
    }

    private static final class ContextTarget {
        private final Anchor anchor;
        private final DynamicPoint offset;

        private ContextTarget(Anchor anchor, DynamicPoint offset) {
            this.anchor = anchor;
            this.offset = offset;
        }
    }
}
