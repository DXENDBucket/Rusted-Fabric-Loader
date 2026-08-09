package io.github.endx.rustedfabricapi.api.custom.event;

import io.github.endx.rustedfabricapi.impl.custom.NativeEventDataRuntime;

import java.util.Set;

/** Extra typed values supplied to native custom-unit events with sparse context. */
public final class NativeEventData {
    private NativeEventData() { }

    public static final class QueueItem {
        public static final String ACTION_ID = "actionId";
        public static final String QUANTITY = "queueQuantity";
        public static final String PRODUCES_UNIT = "producesUnit";
        public static final String PRODUCED_UNIT_TYPE = "producedUnitType";
        public static final String SIZE_BEFORE = "queueSizeBefore";
        public static final String SIZE_AFTER = "queueSizeAfter";
        public static final String HAS_TARGET_POINT = "hasActionTargetPoint";
        public static final String TARGET_X = "actionTargetX";
        public static final String TARGET_Y = "actionTargetY";
        public static final String TARGET_UNIT = "actionTargetUnit";

        private QueueItem() { }
    }

    public static final class Waypoint {
        public static final String ORDER_TYPE = "orderType";
        public static final String TARGET_X = "waypointTargetX";
        public static final String TARGET_Y = "waypointTargetY";
        public static final String TARGET_UNIT = "waypointTargetUnit";
        public static final String QUEUED_BY_PLAYER = "waypointQueuedByPlayer";
        public static final String BUILD_UNIT_TYPE = "waypointBuildUnitType";
        public static final String ACTION_ID = "waypointActionId";

        private Waypoint() { }
    }

    public static final class TeamChange {
        public static final String OLD_TEAM_ID = "oldTeamId";
        public static final String NEW_TEAM_ID = "newTeamId";
        public static final String OLD_ALLIANCE_GROUP = "oldAllianceGroup";
        public static final String NEW_ALLIANCE_GROUP = "newAllianceGroup";

        private TeamChange() { }
    }

    public static final class Teleport {
        public static final String FROM_X = "teleportFromX";
        public static final String FROM_Y = "teleportFromY";
        public static final String FROM_HEIGHT = "teleportFromHeight";
        public static final String FROM_DIRECTION = "teleportFromDirection";
        public static final String TO_X = "teleportToX";
        public static final String TO_Y = "teleportToY";
        public static final String TO_HEIGHT = "teleportToHeight";
        public static final String TO_DIRECTION = "teleportToDirection";

        private Teleport() { }
    }

    public static final class AttachmentRemoval {
        public static final String REMOVED_UNIT = "removedUnit";
        public static final String SLOT_NAME = "attachmentSlot";
        public static final String SLOT_INDEX = "attachmentSlotIndex";
        public static final String WAS_TRANSPORTED = "attachmentWasTransported";

        private AttachmentRemoval() { }
    }

    public static final class KilledUnit {
        public static final String UNIT = "killedUnit";
        public static final String UNIT_TYPE = "killedUnitType";
        public static final String TEAM_ID = "killedUnitTeamId";
        public static final String X = "killedUnitX";
        public static final String Y = "killedUnitY";
        public static final String HP = "killedUnitHp";
        public static final String MAX_HP = "killedUnitMaxHp";
        public static final String WAS_BUILDING = "killedUnitWasBuilding";

        private KilledUnit() { }
    }

    public static final class FinishedQueueUnit {
        public static final String UNIT = "finishedUnit";
        public static final String UNIT_TYPE = "finishedUnitType";
        public static final String TEAM_ID = "finishedUnitTeamId";
        public static final String X = "finishedUnitX";
        public static final String Y = "finishedUnitY";
        public static final String ACTION_ID = "finishedActionId";
        public static final String QUEUE_QUANTITY = "finishedQueueQuantity";

        private FinishedQueueUnit() { }
    }

    public static final class TouchedUnit {
        public static final String UNIT = "touchedUnit";
        public static final String UNIT_TYPE = "touchedUnitType";
        public static final String TEAM_ID = "touchedUnitTeamId";
        public static final String X = "touchedUnitX";
        public static final String Y = "touchedUnitY";

        private TouchedUnit() { }
    }

    /** Fields visible on the carrier's transporting/unloaded events. */
    public static final class TransportedUnit {
        public static final String UNIT = "transportedUnit";
        public static final String UNIT_TYPE = "transportedUnitType";
        public static final String TEAM_ID = "transportedUnitTeamId";
        public static final String X = "transportedUnitX";
        public static final String Y = "transportedUnitY";
        public static final String USED_SLOTS = "transportUsedSlots";
        public static final String MAX_SLOTS = "transportMaxSlots";

        private TransportedUnit() { }
    }

    /** Fields visible on the passenger's entered/left transport events. */
    public static final class Carrier {
        public static final String UNIT = "transportUnit";
        public static final String UNIT_TYPE = "transportUnitType";
        public static final String TEAM_ID = "transportUnitTeamId";
        public static final String X = "transportUnitX";
        public static final String Y = "transportUnitY";
        public static final String USED_SLOTS = "carrierUsedSlots";
        public static final String MAX_SLOTS = "carrierMaxSlots";

        private Carrier() { }
    }

    public static final class Message {
        public static final String SENDER = "messageSender";
        public static final String SENDER_UNIT_TYPE = "messageSenderUnitType";
        public static final String SENDER_TEAM_ID = "messageSenderTeamId";
        public static final String SENDER_X = "messageSenderX";
        public static final String SENDER_Y = "messageSenderY";
        public static final String HAS_TAGS = "messageHasTags";
        public static final String HAS_DATA = "messageHasData";

        private Message() { }
    }

    /** Enables enrichment and reports when any field in this catalog is parsed by an INI. */
    public static Registration enable(Runnable onFieldUsed) {
        return new Registration(NativeEventDataRuntime.enable(onFieldUsed));
    }

    public static Set<String> fieldNames() {
        return NativeEventDataRuntime.fieldNames();
    }

    public static final class Registration implements AutoCloseable {
        private final AutoCloseable delegate;
        private boolean closed;

        private Registration(AutoCloseable delegate) { this.delegate = delegate; }

        @Override
        public synchronized void close() {
            if (closed) return;
            closed = true;
            try {
                delegate.close();
            } catch (RuntimeException failure) {
                throw failure;
            } catch (Exception failure) {
                throw new IllegalStateException("Failed to disable native event data", failure);
            }
        }
    }
}
