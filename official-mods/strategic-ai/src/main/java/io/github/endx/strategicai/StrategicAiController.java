package io.github.endx.strategicai;

import io.github.endx.rustedfabricapi.api.ai.AiController;
import io.github.endx.rustedfabricapi.api.ai.AiStrategicMapSnapshot;
import io.github.endx.rustedfabricapi.api.ai.AiTickContext;
import io.github.endx.rustedfabricapi.api.ai.AiTickDecision;

/** Deterministic first-pass economy and force controller for one team. */
public final class StrategicAiController implements AiController {
    private static final float ECONOMY_INTERVAL = 45.0F;
    private static final float FORCE_INTERVAL = 30.0F;
    private static final float STRATEGIC_INTERVAL = 180.0F;
    private static final float TEAM_PHASE_INTERVAL = 18.0F;

    private final StrategicBuildPlanner buildPlanner = new StrategicBuildPlanner();
    private final StrategicForcePlanner forcePlanner = new StrategicForcePlanner();
    private final StrategicResourceCampaign resourceCampaign = new StrategicResourceCampaign();
    private float economyClock = ECONOMY_INTERVAL;
    private float forceClock = FORCE_INTERVAL;
    private float strategicClock;
    private long economyCycle;
    private long forceCycle;
    private AiStrategicMapSnapshot cachedSituation;
    private StrategicTeamPlan teamPlan;
    private StrategicFrontState frontState;
    private StrategicFrontState.Mode announcedFrontMode;
    private boolean announcedSituation;

    StrategicAiController(int teamId) {
        strategicClock = -Math.floorMod(teamId, 10) * TEAM_PHASE_INTERVAL;
    }

    @Override
    public AiTickDecision tick(AiTickContext context) {
        economyClock += context.delta();
        forceClock += context.delta();
        strategicClock += context.delta();
        if (cachedSituation == null ? strategicClock >= 0.0F
                : strategicClock >= STRATEGIC_INTERVAL) {
            strategicClock = cachedSituation == null
                    ? 0.0F : strategicClock % STRATEGIC_INTERVAL;
            cachedSituation = context.strategicMap();
            if (teamPlan == null) {
                teamPlan = StrategicTeamPlan.create(cachedSituation);
                io.github.endx.rustedfabricapi.api.world.WorldPoint objective =
                        teamPlan.preferredFrontierPoint();
                System.out.println("[Strategic AI] Team " + context.team().id()
                        + " position=" + teamPlan.ownRole()
                        + ", forwardOpening=" + teamPlan.usesForwardOpening()
                        + ", landFront=" + (objective != null
                        ? (int) objective.x() + "," + (int) objective.y() : "none"));
                System.out.println("[Strategic AI] Allied assignments "
                        + teamPlan.assignmentSummary());
            }
            if (!announcedSituation) {
                announcedSituation = true;
                System.out.println("[Strategic AI] Team " + context.team().id()
                        + " ready: sandbox="
                        + io.github.endx.rustedfabricapi.api.client.RustedWarfareClient
                        .isSandboxMode()
                        + ", own=" + cachedSituation.world().own().size()
                        + ", enemies=" + cachedSituation.world().enemies().size()
                        + ", resources=" + cachedSituation.resources().size());
            }
        }
        boolean economyDue = cachedSituation != null && economyClock >= ECONOMY_INTERVAL;
        boolean forceDue = cachedSituation != null && forceClock >= FORCE_INTERVAL;
        if ((economyDue || forceDue) && teamPlan != null) {
            frontState = StrategicFrontState.assess(cachedSituation, teamPlan);
            if (frontState.mode() != announcedFrontMode) {
                announcedFrontMode = frontState.mode();
                System.out.println("[Strategic AI] Team " + context.team().id()
                        + " front=" + frontState.mode()
                        + ", ratio=" + String.format(java.util.Locale.ROOT,
                        "%.2f", frontState.exchangeRatio())
                        + ", own=" + frontState.friendlyUnits()
                        + ", allies=" + frontState.alliedUnits()
                        + ", defenses=" + frontState.enemyDefenses());
            }
            resourceCampaign.update(cachedSituation, context.world().own(),
                    context.world().enemies(), forceCycle,
                    context.team().id(), teamPlan);
        }
        if (economyDue) {
            economyClock %= ECONOMY_INTERVAL;
            buildPlanner.update(context, cachedSituation, economyCycle++,
                    resourceCampaign, teamPlan, frontState);
        }
        if (forceDue) {
            forceClock %= FORCE_INTERVAL;
            forcePlanner.update(context, cachedSituation, forceCycle++,
                    resourceCampaign, teamPlan, frontState);
        }
        return AiTickDecision.REPLACE_NATIVE;
    }
}
