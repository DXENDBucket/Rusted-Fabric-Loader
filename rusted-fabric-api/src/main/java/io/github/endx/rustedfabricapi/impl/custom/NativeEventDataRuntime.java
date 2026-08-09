package io.github.endx.rustedfabricapi.impl.custom;

import android.graphics.PointF;
import io.github.endx.rustedfabricapi.api.custom.event.CustomUnitEventData;
import io.github.endx.rustedfabricapi.api.custom.event.NativeEventData;
import io.github.endx.rustedfabricapi.api.util.RustedReflection;
import rustedwarfare.custom.CustomTagList;
import rustedwarfare.custom.CustomUnit;
import rustedwarfare.custom.attachment.AttachmentSlot;
import rustedwarfare.custom.event.CustomUnitEventType;
import rustedwarfare.custom.logic.VariableScope;
import rustedwarfare.game.Team;
import rustedwarfare.unit.OrderableUnit;
import rustedwarfare.unit.Unit;
import rustedwarfare.unit.UnitOrder;
import rustedwarfare.unit.UnitType;
import rustedwarfare.unit.action.UnitAction;
import rustedwarfare.unit.action.UnitActionId;
import rustedwarfare.unit.build.BuildQueueItem;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/** Captures native call-site values until their matching custom-unit event is queued. */
public final class NativeEventDataRuntime {
    private static final Set<String> FIELD_NAMES = Collections.unmodifiableSet(
            new LinkedHashSet<String>(Arrays.asList(
                    NativeEventData.QueueItem.ACTION_ID,
                    NativeEventData.QueueItem.QUANTITY,
                    NativeEventData.QueueItem.PRODUCES_UNIT,
                    NativeEventData.QueueItem.PRODUCED_UNIT_TYPE,
                    NativeEventData.QueueItem.SIZE_BEFORE,
                    NativeEventData.QueueItem.SIZE_AFTER,
                    NativeEventData.QueueItem.HAS_TARGET_POINT,
                    NativeEventData.QueueItem.TARGET_X,
                    NativeEventData.QueueItem.TARGET_Y,
                    NativeEventData.QueueItem.TARGET_UNIT,
                    NativeEventData.Waypoint.ORDER_TYPE,
                    NativeEventData.Waypoint.TARGET_X,
                    NativeEventData.Waypoint.TARGET_Y,
                    NativeEventData.Waypoint.TARGET_UNIT,
                    NativeEventData.Waypoint.QUEUED_BY_PLAYER,
                    NativeEventData.Waypoint.BUILD_UNIT_TYPE,
                    NativeEventData.Waypoint.ACTION_ID,
                    NativeEventData.TeamChange.OLD_TEAM_ID,
                    NativeEventData.TeamChange.NEW_TEAM_ID,
                    NativeEventData.TeamChange.OLD_ALLIANCE_GROUP,
                    NativeEventData.TeamChange.NEW_ALLIANCE_GROUP,
                    NativeEventData.Teleport.FROM_X,
                    NativeEventData.Teleport.FROM_Y,
                    NativeEventData.Teleport.FROM_HEIGHT,
                    NativeEventData.Teleport.FROM_DIRECTION,
                    NativeEventData.Teleport.TO_X,
                    NativeEventData.Teleport.TO_Y,
                    NativeEventData.Teleport.TO_HEIGHT,
                    NativeEventData.Teleport.TO_DIRECTION,
                    NativeEventData.AttachmentRemoval.REMOVED_UNIT,
                    NativeEventData.AttachmentRemoval.SLOT_NAME,
                    NativeEventData.AttachmentRemoval.SLOT_INDEX,
                    NativeEventData.AttachmentRemoval.WAS_TRANSPORTED,
                    NativeEventData.KilledUnit.UNIT,
                    NativeEventData.KilledUnit.UNIT_TYPE,
                    NativeEventData.KilledUnit.TEAM_ID,
                    NativeEventData.KilledUnit.X,
                    NativeEventData.KilledUnit.Y,
                    NativeEventData.KilledUnit.HP,
                    NativeEventData.KilledUnit.MAX_HP,
                    NativeEventData.KilledUnit.WAS_BUILDING,
                    NativeEventData.FinishedQueueUnit.UNIT,
                    NativeEventData.FinishedQueueUnit.UNIT_TYPE,
                    NativeEventData.FinishedQueueUnit.TEAM_ID,
                    NativeEventData.FinishedQueueUnit.X,
                    NativeEventData.FinishedQueueUnit.Y,
                    NativeEventData.FinishedQueueUnit.ACTION_ID,
                    NativeEventData.FinishedQueueUnit.QUEUE_QUANTITY,
                    NativeEventData.TouchedUnit.UNIT,
                    NativeEventData.TouchedUnit.UNIT_TYPE,
                    NativeEventData.TouchedUnit.TEAM_ID,
                    NativeEventData.TouchedUnit.X,
                    NativeEventData.TouchedUnit.Y,
                    NativeEventData.TransportedUnit.UNIT,
                    NativeEventData.TransportedUnit.UNIT_TYPE,
                    NativeEventData.TransportedUnit.TEAM_ID,
                    NativeEventData.TransportedUnit.X,
                    NativeEventData.TransportedUnit.Y,
                    NativeEventData.TransportedUnit.USED_SLOTS,
                    NativeEventData.TransportedUnit.MAX_SLOTS,
                    NativeEventData.Carrier.UNIT,
                    NativeEventData.Carrier.UNIT_TYPE,
                    NativeEventData.Carrier.TEAM_ID,
                    NativeEventData.Carrier.X,
                    NativeEventData.Carrier.Y,
                    NativeEventData.Carrier.USED_SLOTS,
                    NativeEventData.Carrier.MAX_SLOTS,
                    NativeEventData.Message.SENDER,
                    NativeEventData.Message.SENDER_UNIT_TYPE,
                    NativeEventData.Message.SENDER_TEAM_ID,
                    NativeEventData.Message.SENDER_X,
                    NativeEventData.Message.SENDER_Y,
                    NativeEventData.Message.HAS_TAGS,
                    NativeEventData.Message.HAS_DATA)));
    private static final Set<String> NORMALIZED_FIELD_NAMES;
    private static final CopyOnWriteArrayList<Runnable> USAGE_CALLBACKS =
            new CopyOnWriteArrayList<Runnable>();
    private static final ThreadLocal<Deque<QueueFrame>> QUEUE =
            ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<Deque<WaypointFrame>> WAYPOINT =
            ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<Deque<TeamFrame>> TEAM =
            ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<Deque<TeleportFrame>> TELEPORT =
            ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<Deque<AttachmentFrame>> ATTACHMENT =
            ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<Deque<FinishedQueueFrame>> FINISHED_QUEUE =
            ThreadLocal.withInitial(ArrayDeque::new);

    static {
        LinkedHashSet<String> normalized = new LinkedHashSet<String>();
        for (String name : FIELD_NAMES) normalized.add(normalize(name));
        NORMALIZED_FIELD_NAMES = Collections.unmodifiableSet(normalized);
    }

    private NativeEventDataRuntime() { }

    public static AutoCloseable enable(Runnable callback) {
        Runnable value = Objects.requireNonNull(callback, "callback");
        USAGE_CALLBACKS.add(value);
        return () -> USAGE_CALLBACKS.remove(value);
    }

    public static Set<String> fieldNames() { return FIELD_NAMES; }

    public static void onEventDataNameParsed(String name) {
        if (!NORMALIZED_FIELD_NAMES.contains(normalize(name))) return;
        for (Runnable callback : USAGE_CALLBACKS) callback.run();
    }

    public static void beginQueueAction(CustomUnit unit, UnitAction action,
                                        boolean cancellation, PointF targetPoint,
                                        Unit targetUnit) {
        if (USAGE_CALLBACKS.isEmpty() || action == null || !action.isQueuedAction()) return;
        QUEUE.get().push(new QueueFrame(
                unit, action, cancellation,
                targetPoint != null, targetPoint != null ? targetPoint.a : 0.0F,
                targetPoint != null ? targetPoint.b : 0.0F, targetUnit,
                queueSize(unit)));
    }

    public static void captureQueueItem(UnitAction action, BuildQueueItem item) {
        if (item == null) return;
        for (QueueFrame frame : QUEUE.get()) {
            if (frame.action == action) {
                frame.item = item;
                return;
            }
        }
    }

    public static void endQueueAction(CustomUnit unit, UnitAction action) {
        pop(QUEUE, frame -> frame.unit == unit && frame.action == action);
    }

    public static void beginWaypoint(CustomUnit unit, UnitOrder order) {
        if (USAGE_CALLBACKS.isEmpty()) return;
        WAYPOINT.get().push(new WaypointFrame(unit, order));
    }

    public static void endWaypoint(CustomUnit unit) {
        pop(WAYPOINT, frame -> frame.unit == unit);
    }

    public static void beginTeamChange(CustomUnit unit, Team newTeam) {
        if (USAGE_CALLBACKS.isEmpty()) return;
        TEAM.get().push(new TeamFrame(unit, unit.team, newTeam));
    }

    public static void endTeamChange(CustomUnit unit) {
        pop(TEAM, frame -> frame.unit == unit);
    }

    public static void beginTeleport(CustomUnit unit) {
        if (USAGE_CALLBACKS.isEmpty()) return;
        TELEPORT.get().push(new TeleportFrame(
                unit, ((Unit) unit).x, ((Unit) unit).y, unit.height, unit.direction));
    }

    public static void endTeleport(CustomUnit unit) {
        pop(TELEPORT, frame -> frame.unit == unit);
    }

    public static void beginAttachmentRemoval(CustomUnit parent, OrderableUnit child) {
        if (USAGE_CALLBACKS.isEmpty()) return;
        AttachmentSlot slot = child != null ? child.getAttachmentSlot() : null;
        boolean transported = child != null && parent.getTransportedUnits().contains(child);
        ATTACHMENT.get().push(new AttachmentFrame(parent, child, slot, transported));
    }

    public static void endAttachmentRemoval(CustomUnit parent, OrderableUnit child) {
        pop(ATTACHMENT, frame -> frame.parent == parent && frame.child == child);
    }

    public static void beginFinishedQueueItem(CustomUnit unit, BuildQueueItem item) {
        if (USAGE_CALLBACKS.isEmpty()) return;
        FINISHED_QUEUE.get().push(new FinishedQueueFrame(unit, item));
    }

    public static void endFinishedQueueItem(CustomUnit unit, BuildQueueItem item) {
        pop(FINISHED_QUEUE, frame -> frame.unit == unit && frame.item == item);
    }

    public static VariableScope enrichQueuedEvent(CustomUnit unit,
                                                   CustomUnitEventType eventType,
                                                   Unit source, CustomTagList eventTags,
                                                   VariableScope original) {
        if (USAGE_CALLBACKS.isEmpty()) return original;
        VariableScope result = original;
        if (eventType == CustomUnitEventType.QUEUE_ITEM_ADDED
                || eventType == CustomUnitEventType.QUEUE_ITEM_CANCELLED) {
            QueueFrame frame = find(QUEUE.get(), value -> value.unit == unit);
            if (frame != null) result = enrichQueue(result, frame);
        } else if (eventType == CustomUnitEventType.NEW_WAYPOINT_GIVEN_BY_PLAYER) {
            WaypointFrame frame = find(WAYPOINT.get(), value -> value.unit == unit);
            if (frame != null) result = enrichWaypoint(result, frame);
        } else if (eventType == CustomUnitEventType.TEAM_CHANGED) {
            TeamFrame frame = find(TEAM.get(), value -> value.unit == unit);
            if (frame != null) result = enrichTeam(result, frame);
        } else if (eventType == CustomUnitEventType.TELEPORTED) {
            TeleportFrame frame = find(TELEPORT.get(), value -> value.unit == unit);
            if (frame != null) result = enrichTeleport(result, frame);
        } else if (eventType == CustomUnitEventType.ATTACHMENT_REMOVED) {
            AttachmentFrame frame = find(
                    ATTACHMENT.get(), value -> value.parent == unit);
            if (frame != null) result = enrichAttachment(result, frame);
        } else if (eventType == CustomUnitEventType.KILLED_ANY_UNIT) {
            result = enrichKilledUnit(result, source);
        } else if (eventType == CustomUnitEventType.QUEUED_UNIT_FINISHED) {
            FinishedQueueFrame frame = find(
                    FINISHED_QUEUE.get(), value -> value.unit == unit);
            result = enrichFinishedQueueUnit(result, source, frame);
        } else if (eventType == CustomUnitEventType.TOUCH_TARGET_SUCCESS) {
            result = enrichTouchedUnit(result, source);
        } else if (eventType == CustomUnitEventType.TRANSPORTING_NEW_UNIT
                || eventType == CustomUnitEventType.TRANSPORT_UNLOADED_OR_REMOVED_UNIT) {
            result = enrichTransportedUnit(result, unit, source);
        } else if (eventType == CustomUnitEventType.ENTERED_TRANSPORT
                || eventType == CustomUnitEventType.LEFT_TRANSPORT) {
            result = enrichCarrier(result, source);
        } else if (eventType == CustomUnitEventType.NEW_MESSAGE) {
            result = enrichMessage(result, source, eventTags, original);
        }
        return result;
    }

    private static VariableScope enrichQueue(VariableScope original, QueueFrame frame) {
        CustomUnitEventData data = data(original);
        data.putString(NativeEventData.QueueItem.ACTION_ID,
                        frame.action.getActionIdString())
                .putNumber(NativeEventData.QueueItem.SIZE_BEFORE, frame.sizeBefore)
                .putNumber(NativeEventData.QueueItem.SIZE_AFTER, queueSize(frame.unit))
                .putBoolean(NativeEventData.QueueItem.HAS_TARGET_POINT, frame.hasTargetPoint);
        if (frame.hasTargetPoint) {
            data.putNumber(NativeEventData.QueueItem.TARGET_X, frame.targetX)
                    .putNumber(NativeEventData.QueueItem.TARGET_Y, frame.targetY);
        }
        if (frame.targetUnit != null) {
            data.putUnit(NativeEventData.QueueItem.TARGET_UNIT, frame.targetUnit);
        }
        if (frame.item != null) {
            data.putNumber(NativeEventData.QueueItem.QUANTITY, frame.item.quantity)
                    .putBoolean(NativeEventData.QueueItem.PRODUCES_UNIT,
                            frame.item.producesUnit);
            if (frame.item.producedUnitType != null) {
                data.putString(NativeEventData.QueueItem.PRODUCED_UNIT_TYPE,
                        frame.item.producedUnitType.getInternalName());
            }
        }
        return data.nativeScope();
    }

    private static VariableScope enrichWaypoint(VariableScope original, WaypointFrame frame) {
        CustomUnitEventData data = data(original);
        UnitOrder order = frame.order;
        data.putString(NativeEventData.Waypoint.ORDER_TYPE,
                        order.getOrderType().name())
                .putNumber(NativeEventData.Waypoint.TARGET_X, order.getTargetX())
                .putNumber(NativeEventData.Waypoint.TARGET_Y, order.getTargetY())
                .putBoolean(NativeEventData.Waypoint.QUEUED_BY_PLAYER,
                        order.queueByPlayer);
        Unit target = order.getTargetUnit();
        if (target != null) data.putUnit(NativeEventData.Waypoint.TARGET_UNIT, target);
        UnitType buildType = order.getBuildUnitType();
        if (buildType != null) {
            data.putString(NativeEventData.Waypoint.BUILD_UNIT_TYPE,
                    buildType.getInternalName());
        }
        Object actionId = RustedReflection.getFieldValue(
                order, new String[]{"actionId", "c"});
        if (actionId instanceof UnitActionId) {
            data.putString(NativeEventData.Waypoint.ACTION_ID,
                    ((UnitActionId) actionId).asString());
        }
        return data.nativeScope();
    }

    private static VariableScope enrichTeam(VariableScope original, TeamFrame frame) {
        CustomUnitEventData data = data(original);
        data.putNumber(NativeEventData.TeamChange.OLD_TEAM_ID, teamId(frame.oldTeam))
                .putNumber(NativeEventData.TeamChange.NEW_TEAM_ID, teamId(frame.newTeam))
                .putNumber(NativeEventData.TeamChange.OLD_ALLIANCE_GROUP,
                        allianceGroup(frame.oldTeam))
                .putNumber(NativeEventData.TeamChange.NEW_ALLIANCE_GROUP,
                        allianceGroup(frame.newTeam));
        return data.nativeScope();
    }

    private static VariableScope enrichTeleport(VariableScope original, TeleportFrame frame) {
        CustomUnitEventData data = data(original);
        data.putNumber(NativeEventData.Teleport.FROM_X, frame.fromX)
                .putNumber(NativeEventData.Teleport.FROM_Y, frame.fromY)
                .putNumber(NativeEventData.Teleport.FROM_HEIGHT, frame.fromHeight)
                .putNumber(NativeEventData.Teleport.FROM_DIRECTION, frame.fromDirection)
                .putNumber(NativeEventData.Teleport.TO_X, ((Unit) frame.unit).x)
                .putNumber(NativeEventData.Teleport.TO_Y, ((Unit) frame.unit).y)
                .putNumber(NativeEventData.Teleport.TO_HEIGHT, frame.unit.height)
                .putNumber(NativeEventData.Teleport.TO_DIRECTION, frame.unit.direction);
        return data.nativeScope();
    }

    private static VariableScope enrichAttachment(VariableScope original,
                                                   AttachmentFrame frame) {
        CustomUnitEventData data = data(original);
        if (frame.child != null) {
            data.putUnit(NativeEventData.AttachmentRemoval.REMOVED_UNIT, frame.child);
        }
        if (frame.slot != null) {
            data.putString(NativeEventData.AttachmentRemoval.SLOT_NAME,
                            frame.slot.getName())
                    .putNumber(NativeEventData.AttachmentRemoval.SLOT_INDEX,
                            frame.slot.getIndex());
        }
        data.putBoolean(NativeEventData.AttachmentRemoval.WAS_TRANSPORTED,
                frame.wasTransported);
        return data.nativeScope();
    }

    private static VariableScope enrichKilledUnit(VariableScope original, Unit killed) {
        if (killed == null) return original;
        CustomUnitEventData data = data(original);
        putUnitSnapshot(data, killed,
                NativeEventData.KilledUnit.UNIT,
                NativeEventData.KilledUnit.UNIT_TYPE,
                NativeEventData.KilledUnit.TEAM_ID,
                NativeEventData.KilledUnit.X,
                NativeEventData.KilledUnit.Y);
        data.putNumber(NativeEventData.KilledUnit.HP, killed.hp)
                .putNumber(NativeEventData.KilledUnit.MAX_HP, killed.maxHp)
                .putBoolean(NativeEventData.KilledUnit.WAS_BUILDING, killed.isBuilding());
        return data.nativeScope();
    }

    private static VariableScope enrichFinishedQueueUnit(
            VariableScope original, Unit finished, FinishedQueueFrame frame) {
        CustomUnitEventData data = data(original);
        if (finished != null) {
            putUnitSnapshot(data, finished,
                    NativeEventData.FinishedQueueUnit.UNIT,
                    NativeEventData.FinishedQueueUnit.UNIT_TYPE,
                    NativeEventData.FinishedQueueUnit.TEAM_ID,
                    NativeEventData.FinishedQueueUnit.X,
                    NativeEventData.FinishedQueueUnit.Y);
        }
        if (frame != null && frame.item != null) {
            if (frame.item.actionId != null) {
                data.putString(NativeEventData.FinishedQueueUnit.ACTION_ID,
                        frame.item.actionId.asString());
            }
            data.putNumber(NativeEventData.FinishedQueueUnit.QUEUE_QUANTITY,
                    frame.item.quantity);
        }
        return data.nativeScope();
    }

    private static VariableScope enrichTouchedUnit(VariableScope original, Unit touched) {
        if (touched == null) return original;
        CustomUnitEventData data = data(original);
        putUnitSnapshot(data, touched,
                NativeEventData.TouchedUnit.UNIT,
                NativeEventData.TouchedUnit.UNIT_TYPE,
                NativeEventData.TouchedUnit.TEAM_ID,
                NativeEventData.TouchedUnit.X,
                NativeEventData.TouchedUnit.Y);
        return data.nativeScope();
    }

    private static VariableScope enrichTransportedUnit(
            VariableScope original, CustomUnit carrier, Unit transported) {
        CustomUnitEventData data = data(original);
        if (transported != null) {
            putUnitSnapshot(data, transported,
                    NativeEventData.TransportedUnit.UNIT,
                    NativeEventData.TransportedUnit.UNIT_TYPE,
                    NativeEventData.TransportedUnit.TEAM_ID,
                    NativeEventData.TransportedUnit.X,
                    NativeEventData.TransportedUnit.Y);
        }
        data.putNumber(NativeEventData.TransportedUnit.USED_SLOTS,
                        carrier.getTransportBarUsedSlots())
                .putNumber(NativeEventData.TransportedUnit.MAX_SLOTS,
                        carrier.getTransportBarMaxSlots());
        return data.nativeScope();
    }

    private static VariableScope enrichCarrier(VariableScope original, Unit carrier) {
        if (carrier == null) return original;
        CustomUnitEventData data = data(original);
        putUnitSnapshot(data, carrier,
                NativeEventData.Carrier.UNIT,
                NativeEventData.Carrier.UNIT_TYPE,
                NativeEventData.Carrier.TEAM_ID,
                NativeEventData.Carrier.X,
                NativeEventData.Carrier.Y);
        data.putNumber(NativeEventData.Carrier.USED_SLOTS,
                        carrier.getTransportBarUsedSlots())
                .putNumber(NativeEventData.Carrier.MAX_SLOTS,
                        carrier.getTransportBarMaxSlots());
        return data.nativeScope();
    }

    private static VariableScope enrichMessage(
            VariableScope original, Unit sender, CustomTagList tags,
            VariableScope originalEventData) {
        CustomUnitEventData data = data(original);
        if (sender != null) {
            putUnitSnapshot(data, sender,
                    NativeEventData.Message.SENDER,
                    NativeEventData.Message.SENDER_UNIT_TYPE,
                    NativeEventData.Message.SENDER_TEAM_ID,
                    NativeEventData.Message.SENDER_X,
                    NativeEventData.Message.SENDER_Y);
        }
        data.putBoolean(NativeEventData.Message.HAS_TAGS, tags != null)
                .putBoolean(NativeEventData.Message.HAS_DATA, originalEventData != null);
        return data.nativeScope();
    }

    private static void putUnitSnapshot(CustomUnitEventData data, Unit unit,
                                        String unitField, String typeField,
                                        String teamField, String xField,
                                        String yField) {
        data.putUnit(unitField, unit)
                .putString(typeField, unitTypeName(unit))
                .putNumber(teamField, teamId(unit.team))
                .putNumber(xField, unit.x)
                .putNumber(yField, unit.y);
    }

    private static String unitTypeName(Unit unit) {
        Object type = RustedReflection.invokeInstance(
                unit, new String[]{"getUnitMetadata", "getUnitType", "r"});
        return type instanceof UnitType
                ? ((UnitType) type).getInternalName()
                : type != null ? type.toString() : "";
    }

    private static CustomUnitEventData data(VariableScope scope) {
        return scope == null ? CustomUnitEventData.create() : CustomUnitEventData.wrap(scope);
    }

    private static int queueSize(CustomUnit unit) {
        return unit.getBuildQueueItems() != null ? unit.getBuildQueueItems().size() : 0;
    }

    private static int teamId(Team team) { return team != null ? team.teamId : -1; }

    private static int allianceGroup(Team team) {
        return team != null ? team.allianceGroup : -1;
    }

    private static String normalize(String name) {
        return Objects.requireNonNull(name, "name").trim().toLowerCase(Locale.ROOT);
    }

    private static <T> T find(Deque<T> frames, Predicate<T> predicate) {
        for (T frame : frames) if (predicate.test(frame)) return frame;
        return null;
    }

    private static <T> void pop(ThreadLocal<Deque<T>> local, Predicate<T> predicate) {
        Deque<T> frames = local.get();
        if (!frames.isEmpty() && predicate.test(frames.peek())) {
            frames.pop();
        } else {
            for (Iterator<T> iterator = frames.iterator(); iterator.hasNext(); ) {
                if (predicate.test(iterator.next())) {
                    iterator.remove();
                    break;
                }
            }
        }
        if (frames.isEmpty()) local.remove();
    }

    private static final class QueueFrame {
        private final CustomUnit unit;
        private final UnitAction action;
        @SuppressWarnings("unused") private final boolean cancellation;
        private final boolean hasTargetPoint;
        private final float targetX;
        private final float targetY;
        private final Unit targetUnit;
        private final int sizeBefore;
        private BuildQueueItem item;

        private QueueFrame(CustomUnit unit, UnitAction action, boolean cancellation,
                           boolean hasTargetPoint, float targetX, float targetY,
                           Unit targetUnit, int sizeBefore) {
            this.unit = unit;
            this.action = action;
            this.cancellation = cancellation;
            this.hasTargetPoint = hasTargetPoint;
            this.targetX = targetX;
            this.targetY = targetY;
            this.targetUnit = targetUnit;
            this.sizeBefore = sizeBefore;
        }
    }

    private static final class WaypointFrame {
        private final CustomUnit unit;
        private final UnitOrder order;

        private WaypointFrame(CustomUnit unit, UnitOrder order) {
            this.unit = unit;
            this.order = order;
        }
    }

    private static final class TeamFrame {
        private final CustomUnit unit;
        private final Team oldTeam;
        private final Team newTeam;

        private TeamFrame(CustomUnit unit, Team oldTeam, Team newTeam) {
            this.unit = unit;
            this.oldTeam = oldTeam;
            this.newTeam = newTeam;
        }
    }

    private static final class TeleportFrame {
        private final CustomUnit unit;
        private final float fromX;
        private final float fromY;
        private final float fromHeight;
        private final float fromDirection;

        private TeleportFrame(CustomUnit unit, float fromX, float fromY,
                              float fromHeight, float fromDirection) {
            this.unit = unit;
            this.fromX = fromX;
            this.fromY = fromY;
            this.fromHeight = fromHeight;
            this.fromDirection = fromDirection;
        }
    }

    private static final class AttachmentFrame {
        private final CustomUnit parent;
        private final OrderableUnit child;
        private final AttachmentSlot slot;
        private final boolean wasTransported;

        private AttachmentFrame(CustomUnit parent, OrderableUnit child,
                                AttachmentSlot slot, boolean wasTransported) {
            this.parent = parent;
            this.child = child;
            this.slot = slot;
            this.wasTransported = wasTransported;
        }
    }

    private static final class FinishedQueueFrame {
        private final CustomUnit unit;
        private final BuildQueueItem item;

        private FinishedQueueFrame(CustomUnit unit, BuildQueueItem item) {
            this.unit = unit;
            this.item = item;
        }
    }
}
