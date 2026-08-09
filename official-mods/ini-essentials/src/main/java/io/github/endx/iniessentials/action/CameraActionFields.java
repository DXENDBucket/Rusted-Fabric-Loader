package io.github.endx.iniessentials.action;

import io.github.endx.iniessentials.ActionPositionReference;
import io.github.endx.iniessentials.BooleanExpression;
import io.github.endx.iniessentials.IniEssentials;
import io.github.endx.iniessentials.NumericExpression;

import io.github.endx.rustedfabricapi.api.client.Camera;
import io.github.endx.rustedfabricapi.api.ini.IniFieldDocumentation;
import io.github.endx.rustedfabricapi.api.ini.IniMultiplayerImpact;
import io.github.endx.rustedfabricapi.api.ini.action.IniActionEffectDefinition;
import io.github.endx.rustedfabricapi.api.ini.action.IniActionEffects;
import io.github.endx.rustedfabricapi.api.ini.action.IniActionExecutionContext;
import io.github.endx.rustedfabricapi.api.world.WorldPoint;
import java.util.ArrayList;
import java.util.List;

public final class CameraActionFields {
    private static final String POSITION_GROUP = "camera_position";

    private CameraActionFields() { }

    public static void register() {
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
                        "(UnitReference|actionTarget)[,runtimeWorldOffsetX,runtimeWorldOffsetY]",
                        "Centers on a native unit reference or contextual target with optional world-axis offsets.",
                        "将视角居中到原版 UnitReference 或上下文目标；尾部世界轴偏移整体选填且默认均为零。",
                        "cameraCenterOn: self.getOffsetRelative(y=100)"))
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
        target.position.resolve(context).ifPresent(point -> {
            WorldPoint offset = target.offset != null
                    ? target.offset.evaluate(context) : new WorldPoint(0.0F, 0.0F);
            Camera.centerAt(point.x() + offset.x(), point.y() + offset.y());
        });
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
                    "expected (UnitReference|actionTarget)[,worldOffsetX,worldOffsetY]");
        }
        ActionPositionReference position = ActionPositionReference.compile(metadata, parts.get(0));
        DynamicPoint offset = parts.size() == 3
                ? new DynamicPoint(NumericExpression.compile(metadata, parts.get(1)),
                        NumericExpression.compile(metadata, parts.get(2)))
                : null;
        return new ContextTarget(position, offset);
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
        private final ActionPositionReference position;
        private final DynamicPoint offset;

        private ContextTarget(ActionPositionReference position, DynamicPoint offset) {
            this.position = position;
            this.offset = offset;
        }
    }
}
