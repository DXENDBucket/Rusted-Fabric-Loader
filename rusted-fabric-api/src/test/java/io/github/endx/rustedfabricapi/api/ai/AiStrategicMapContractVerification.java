package io.github.endx.rustedfabricapi.api.ai;

import io.github.endx.rustedfabricapi.api.world.WorldPoint;
import rustedwarfare.ai.AiTeam;
import rustedwarfare.framework.GameObject;
import rustedwarfare.game.Team;
import rustedwarfare.unit.Unit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;

public final class AiStrategicMapContractVerification {
    private AiStrategicMapContractVerification() {
    }

    public static void verify() {
        require(AiMovementDomain.fromName("overCliff") == AiMovementDomain.OVER_CLIFF
                        && AiMovementDomain.fromName("naval") == AiMovementDomain.LAND,
                "AI movement-domain parsing changed");
        Unit.allUnits.clear();
        AiTeam blue = new AiTeam(41);
        blue.playerName = "Blue AI";
        Team red = new Team(42);
        blue.enemy = red;
        red.enemy = blue;

        Unit blueArmy = unit(101L, blue, 45.0F, 50.0F, false, false, "land");
        Unit blueExtractor = unit(102L, blue, 55.0F, 55.0F, true, false, "building");
        Unit redArmy = unit(103L, red, 255.0F, 50.0F, false, false, "land");
        Unit.allUnits.addAll(Arrays.asList(redArmy, blueExtractor, blueArmy));

        List<AiResourceSite> resources = Arrays.asList(
                new AiResourceSite(5, 5, new WorldPoint(55.0F, 55.0F)),
                new AiResourceSite(15, 5, new WorldPoint(155.0F, 55.0F)));
        AiTerrainMapSnapshot open = terrain(false, resources);
        AiTickContext context = AiTickContext.capture(blue, 1.0F);
        AiStrategicMapSnapshot situation = AiStrategicMaps.capture(context, open);

        require(situation.teams().size() == 2,
                "strategic map did not describe both player distributions");
        AiTeamPresence own = situation.teams().stream()
                .filter(value -> value.relation() == AiTeamRelation.OWN)
                .findFirst().orElseThrow();
        require(own.team().id() == 41 && own.team().aiControlled()
                        && own.unitCount() == 2 && own.buildingCount() == 1,
                "own player identity/distribution summary was incorrect");
        require(!situation.frontline().isEmpty() && situation.primaryFront().isPresent(),
                "opposing connected armies did not produce a front line");
        require(situation.frontline().stream().anyMatch(value ->
                        value.frontlineDomain().orElse(null) == AiMovementDomain.LAND),
                "land front did not retain its movement-domain context");
        AiStrategicResource owned = situation.resources().stream()
                .filter(value -> value.site().tileX() == 5).findFirst().orElseThrow();
        require(owned.control() == AiResourceControl.OWN,
                "resource extractor ownership was not detected");
        AiStrategicResource middle = situation.resources().stream()
                .filter(value -> value.site().tileX() == 15).findFirst().orElseThrow();
        require(middle.control() == AiResourceControl.UNCLAIMED
                        && middle.objective() == AiResourceObjectiveKind.LOCK_DOWN
                        && middle.landReachable(),
                "contested open resource site was not proposed for lock-down");

        AiStrategicMapSnapshot divided = AiStrategicMaps.capture(context,
                terrain(true, resources));
        require(divided.frontline().isEmpty(),
                "mountain-separated land forces were treated as one front");
        AiStrategicResource dividedMiddle = divided.resources().stream()
                .filter(value -> value.site().tileX() == 15).findFirst().orElseThrow();
        require(!dividedMiddle.landReachable(),
                "resource site across a disconnected mountain region was marked land-reachable");
        require(dividedMiddle.reachable(AiMovementDomain.AIR),
                "air reachability was incorrectly blocked by a mountain region");
        require(divided.terrain().cell(1, 0).mountainFraction() == 1.0F,
                "mountain terrain was not retained in the strategic grid");

        Unit.allUnits.clear();
    }

    private static AiTerrainMapSnapshot terrain(boolean mountainBarrier,
            List<AiResourceSite> resources) {
        ArrayList<AiTerrainCell> cells = new ArrayList<AiTerrainCell>();
        cells.add(cell(0, 1, 1.0F, 0.0F));
        cells.add(cell(1, mountainBarrier ? -1 : 1,
                mountainBarrier ? 0.0F : 1.0F, mountainBarrier ? 1.0F : 0.0F));
        cells.add(cell(2, mountainBarrier ? 2 : 1, 1.0F, 0.0F));
        return new AiTerrainMapSnapshot(30, 10, 10, 10, 10,
                3, 1, cells, resources);
    }

    private static AiTerrainCell cell(int column, int landRegion,
            float landPassability, float mountainFraction) {
        EnumMap<AiMovementDomain, Float> passability =
                new EnumMap<AiMovementDomain, Float>(AiMovementDomain.class);
        EnumMap<AiMovementDomain, Integer> regions =
                new EnumMap<AiMovementDomain, Integer>(AiMovementDomain.class);
        EnumMap<AiMovementDomain, WorldPoint> representatives =
                new EnumMap<AiMovementDomain, WorldPoint>(AiMovementDomain.class);
        for (AiMovementDomain domain : AiMovementDomain.values()) {
            passability.put(domain, Float.valueOf(domain == AiMovementDomain.AIR
                    ? 1.0F : landPassability));
            if (landRegion > 0) regions.put(domain, Integer.valueOf(landRegion));
            if (domain == AiMovementDomain.AIR || landPassability > 0.0F) {
                representatives.put(domain, new WorldPoint(column * 10 + 5, 5));
            }
        }
        return new AiTerrainCell(column, 0, column * 10, 0, column * 10 + 10, 10,
                10, 10, 0.0F, mountainFraction, 0.0F, 0.0F, mountainFraction,
                passability, regions, representatives);
    }

    private static Unit unit(long id, Team team, float x, float y,
            boolean building, boolean flying, String movement) {
        Unit unit = new Unit();
        ((GameObject) unit).id = id;
        ((GameObject) unit).x = x;
        ((GameObject) unit).y = y;
        unit.team = team;
        unit.hp = 100.0F;
        unit.maxHp = 100.0F;
        unit.building = building;
        unit.flying = flying;
        unit.movement = movement;
        return unit;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
