package io.github.endx.rustedfabricapi.api.mission;

import android.graphics.PointF;
import io.github.endx.rustedfabricapi.api.client.RustedWarfareClient;
import io.github.endx.rustedfabricapi.api.world.WorldPoint;
import rustedwarfare.core.GameEngine;
import rustedwarfare.mission.MissionEngine;
import rustedwarfare.mission.MissionTrigger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Read-only mission state plus bookkeeping-aware trigger activation. */
public final class Missions {
    private Missions() {
    }

    public static MissionEngine currentOrNull() {
        GameEngine engine = RustedWarfareClient.getEngine();
        return engine != null ? engine.missionEngine : null;
    }

    public static Optional<MissionEngine> current() {
        return Optional.ofNullable(currentOrNull());
    }

    public static MissionEngine requireCurrent() {
        MissionEngine mission = currentOrNull();
        if (mission == null) throw new IllegalStateException("No mission engine is active");
        return mission;
    }

    public static MissionSnapshot snapshot() {
        GameEngine engine = RustedWarfareClient.requireEngine();
        MissionEngine mission = engine.missionEngine;
        if (mission == null) {
            return new MissionSnapshot(false, false, false, false,
                    0, 0, engine.hasWonGame, engine.hasLostGame);
        }
        int triggerCount = mission.triggers != null ? mission.triggers.size() : 0;
        return new MissionSnapshot(mission.hasMissionData(), mission.isMissionActive(),
                mission.isMissionOver(), mission.hasActiveObjectives(), mission.survivalWave,
                triggerCount, engine.hasWonGame, engine.hasLostGame);
    }

    public static List<MissionTrigger> triggers() {
        MissionEngine mission = requireCurrent();
        if (mission.triggers == null || mission.triggers.isEmpty()) return Collections.emptyList();
        List<MissionTrigger> result = new ArrayList<MissionTrigger>(mission.triggers.size());
        for (Object value : mission.triggers) {
            if (value instanceof MissionTrigger) result.add((MissionTrigger) value);
        }
        return Collections.unmodifiableList(result);
    }

    public static Optional<MissionTrigger> triggerById(String id) {
        return Optional.ofNullable(requireCurrent().getTriggerById(requireText(id, "id")));
    }

    public static Optional<MissionTrigger> triggerByName(String name) {
        return Optional.ofNullable(requireCurrent().getTriggerByName(requireText(name, "name")));
    }

    public static Optional<WorldPoint> mapPoint(String objectName) {
        PointF point = requireCurrent().getPointFromMapObject(requireText(objectName, "objectName"));
        return point != null ? Optional.of(new WorldPoint(point.a, point.b)) : Optional.empty();
    }

    /** Activates a parsed mission trigger through its normal native path. */
    public static void activate(MissionTrigger trigger) {
        requireCurrent().activateTrigger(Objects.requireNonNull(trigger, "trigger"));
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        String checked = value.trim();
        if (checked.isEmpty()) throw new IllegalArgumentException(name + " must not be blank");
        return checked;
    }
}
