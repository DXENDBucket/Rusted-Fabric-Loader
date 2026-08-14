package io.github.endx.rustedfabricapi.api.ai;

import io.github.endx.rustedfabricapi.api.ai.event.AiControlEvents;
import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import io.github.endx.rustedfabricapi.api.util.Identifier;
import rustedwarfare.ai.AiTeam;
import rustedwarfare.framework.GameObject;
import rustedwarfare.game.Team;
import rustedwarfare.unit.Unit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class AiControlContractVerification {
    private AiControlContractVerification() {
    }

    public static void verify() {
        AiControllers.clearAssignments();
        Unit.allUnits.clear();
        AiTeam blue = new AiTeam(31);
        Team red = new Team(32);
        Team neutral = new Team(33);
        blue.enemy = red;
        red.enemy = blue;
        Unit redUnit = unit(9L, red);
        Unit blueUnit = unit(3L, blue);
        Unit neutralUnit = unit(6L, neutral);
        Unit deadUnit = unit(1L, red);
        deadUnit.dead = true;
        Unit.allUnits.addAll(Arrays.asList(redUnit, blueUnit, neutralUnit, deadUnit));

        AiTickContext context = AiTickContext.capture(blue, 1.0F);
        AiWorldSnapshot world = context.world();
        require(world.omniscient(), "AI world was not marked omniscient");
        require(ids(world.all()).equals(Arrays.asList(3L, 6L, 9L)),
                "AI world was not alive-only and stable-ID sorted");
        require(ids(world.own()).equals(Arrays.asList(3L))
                        && ids(world.enemies()).equals(Arrays.asList(9L))
                        && ids(world.neutral()).equals(Arrays.asList(6L)),
                "AI world relation groups were incorrect");

        List<String> events = new ArrayList<String>();
        RustedFabricEvent.Registration before = AiControlEvents.BEFORE_TICK.subscribe(
                value -> events.add("before:" + value.team().hashCode()));
        RustedFabricEvent.Registration after = AiControlEvents.AFTER_TICK.subscribe(
                (value, outcome) -> events.add("after:" + outcome.name()));
        AiControllers.Handle pass = AiControllers.assign(blue,
                Identifier.of("contract", "pass"), value -> AiTickDecision.PASS);
        require(!AiControllers.beforeNativeTick(blue, 1.0F),
                "PASS controller incorrectly cancelled native AI");
        AiControllers.afterNativeTick(blue, 1.0F);
        require(events.get(0).startsWith("before:")
                        && "after:NATIVE".equals(events.get(1)),
                "native AI event order/outcome was incorrect");
        require(pass.unregister() && !pass.unregister(),
                "AI controller handle cleanup was not idempotent");

        events.clear();
        AiControllers.Handle replacement = AiControllers.assign(blue,
                Identifier.of("contract", "replacement"), value -> {
                    require(value.rawTeam() == blue && value.delta() == 2.0F,
                            "controller context lost team or delta");
                    return AiTickDecision.REPLACE_NATIVE;
                });
        require(AiControllers.beforeNativeTick(blue, 2.0F),
                "replacement controller did not cancel native AI");
        require(events.get(0).startsWith("before:")
                        && "after:CUSTOM".equals(events.get(1)),
                "custom AI event order/outcome was incorrect");

        boolean collisionRejected = false;
        try {
            AiControllers.assign(blue, Identifier.of("other", "controller"),
                    value -> AiTickDecision.PASS);
        } catch (IllegalStateException expected) {
            collisionRejected = true;
        }
        require(collisionRejected, "AI controller ownership collision was not rejected");
        replacement.close();
        before.close();
        after.close();
        AiControllers.clearAssignments();
        Unit.allUnits.clear();
    }

    private static Unit unit(long id, Team team) {
        Unit unit = new Unit();
        ((GameObject) unit).id = id;
        unit.team = team;
        unit.hp = 100.0F;
        unit.maxHp = 100.0F;
        return unit;
    }

    private static List<Long> ids(List<io.github.endx.rustedfabricapi.api.game.UnitView> units) {
        List<Long> result = new ArrayList<Long>();
        for (io.github.endx.rustedfabricapi.api.game.UnitView unit : units) {
            result.add(Long.valueOf(unit.id()));
        }
        return result;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
