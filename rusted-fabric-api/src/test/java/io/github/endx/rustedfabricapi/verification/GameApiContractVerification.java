package io.github.endx.rustedfabricapi.verification;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import io.github.endx.rustedfabricapi.api.game.TeamView;
import io.github.endx.rustedfabricapi.api.game.Teams;
import io.github.endx.rustedfabricapi.api.game.UnitView;
import io.github.endx.rustedfabricapi.api.game.Units;
import io.github.endx.rustedfabricapi.api.unit.event.UnitEvents;
import rustedwarfare.game.Team;
import rustedwarfare.framework.GameObject;
import rustedwarfare.unit.Unit;

import java.util.List;

public final class GameApiContractVerification {
    private GameApiContractVerification() {
    }

    public static void main(String[] args) {
        Unit.allUnits.clear();
        Team blue = new Team(1);
        Team red = new Team(2);
        blue.credits = 1200.5D;
        blue.totalUnits = 2;
        blue.nonBuildings = 1;
        blue.maximumUnits = 200;
        blue.income = 7;
        blue.enemy = red;
        red.enemy = blue;

        Unit tank = unit(11L, blue, 10.0F, 20.0F, 80.0F, 100.0F);
        Unit builder = unit(12L, blue, 16.0F, 22.0F, 40.0F, 40.0F);
        builder.building = true;
        Unit enemy = unit(13L, red, 100.0F, 100.0F, 60.0F, 60.0F);
        ShadowedCoordinateUnit shadowed = new ShadowedCoordinateUnit();
        ((GameObject) shadowed).id = 14L;
        ((GameObject) shadowed).x = 300.0F;
        ((GameObject) shadowed).y = 400.0F;
        shadowed.x = 10.0F;
        shadowed.y = 20.0F;
        shadowed.team = red;
        shadowed.hp = 50.0F;
        shadowed.maxHp = 50.0F;
        Unit.allUnits.add(tank);
        Unit.allUnits.add(builder);
        Unit.allUnits.add(enemy);
        Unit.allUnits.add(shadowed);

        UnitView view = Units.view(tank);
        require(view.id() == 11L && view.x() == 10.0F && view.y() == 20.0F,
                "unit identity or position was not exposed");
        require(view.health() == 80.0F && view.maxHealth() == 100.0F
                        && view.healthFraction() == 0.8F,
                "unit health view is incorrect");
        require("land".equals(view.movementType()) && view.alive(),
                "unit state view is incorrect");
        require(view.team().orElseThrow().sameTeam(Teams.view(blue)),
                "unit team view lost object identity");

        TeamView blueView = Teams.byId(1);
        require(blueView != null && blueView.credits() == 1200.5D
                        && blueView.totalUnitCountIncludingQueued() == 2
                        && blueView.enemyOf(Teams.view(red)),
                "team view is incorrect");
        require(Units.forTeam(blueView).size() == 2,
                "team query did not return the expected units");
        List<UnitView> nearby = Units.within(10.0F, 20.0F, 10.0F);
        require(nearby.size() == 2 && Units.byId(13L).orElseThrow().raw() == enemy,
                "unit snapshot queries are incorrect");
        UnitView shadowedView = Units.view(shadowed);
        require(shadowedView.x() == 300.0F && shadowedView.y() == 400.0F,
                "subclass fields must not shadow canonical world coordinates");

        view.setHealth(35.0F).setDirection(90.0F).setConstructionProgress(0.5F)
                .changeTeam(Teams.view(red));
        require(tank.hp == 35.0F && tank.direction == 90.0F
                        && tank.constructionProgress == 0.5F && tank.team == red,
                "unit operations did not call mapped game methods");

        final UnitView[] delivered = new UnitView[1];
        RustedFabricEvent.Registration registration =
                UnitEvents.subscribeAfterUnitAdded(value -> delivered[0] = value);
        UnitEvents.AFTER_REGISTER.invoker().onUnit(builder);
        require(delivered[0] != null && delivered[0].raw() == builder,
                "typed lifecycle adapter did not preserve the unit");
        registration.close();

        view.removeFromGame();
        require(tank.removed && !view.alive(), "unit removal operation failed");
        System.out.println("Stable game unit/team API contracts passed");
    }

    private static Unit unit(long id, Team team, float x, float y, float hp, float maxHp) {
        Unit unit = new Unit();
        unit.id = id;
        unit.team = team;
        unit.registeredWithTeam = true;
        unit.x = x;
        unit.y = y;
        unit.hp = hp;
        unit.maxHp = maxHp;
        return unit;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class ShadowedCoordinateUnit extends Unit {
        public float x;
        public float y;
    }
}
