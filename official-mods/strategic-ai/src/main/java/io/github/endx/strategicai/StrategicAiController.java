package io.github.endx.strategicai;

import io.github.endx.rustedfabricapi.api.ai.AiController;
import io.github.endx.rustedfabricapi.api.ai.AiStrategicMapSnapshot;
import io.github.endx.rustedfabricapi.api.ai.AiTickContext;
import io.github.endx.rustedfabricapi.api.ai.AiTickDecision;

/** Deterministic first-pass economy and force controller for one team. */
public final class StrategicAiController implements AiController {
    private static final float ECONOMY_INTERVAL = 45.0F;
    private static final float FORCE_INTERVAL = 30.0F;
    private static final float MICRO_INTERVAL = 12.0F;
    private static final float SITUATION_INTERVAL = 60.0F;
    private static final float REPLAN_INTERVAL = 300.0F;
    private static final float TEAM_PHASE_INTERVAL = 12.0F;

    private final StrategicBuildPlanner buildPlanner = new StrategicBuildPlanner();
    private final StrategicForcePlanner forcePlanner = new StrategicForcePlanner();
    private final StrategicResourceCampaign resourceCampaign = new StrategicResourceCampaign();
    private float economyClock = ECONOMY_INTERVAL;
    private float forceClock = FORCE_INTERVAL;
    private float microClock = MICRO_INTERVAL;
    private float situationClock;
    private float replanClock = REPLAN_INTERVAL;
    private long economyCycle;
    private long forceCycle;
    private long microCycle;
    private AiStrategicMapSnapshot cachedSituation;
    private StrategicTeamPlan teamPlan;
    private StrategicFrontState frontState;
    private StrategicFrontState.Mode announcedFrontMode;
    private boolean announcedSituation;

    StrategicAiController(int teamId) {
        situationClock = -Math.floorMod(teamId, 10) * TEAM_PHASE_INTERVAL;
        // Deterministically spread teams across the 12-tick micro window to avoid a single
        // frame rebuilding every AI battlefield index at once.
        microClock = MICRO_INTERVAL - Math.floorMod(teamId, (int) MICRO_INTERVAL);
    }

    @Override
    public AiTickDecision tick(AiTickContext context) {
        economyClock += context.delta();
        forceClock += context.delta();
        microClock += context.delta();
        situationClock += context.delta();
        replanClock += context.delta();
        if (cachedSituation == null ? situationClock >= 0.0F
                : situationClock >= SITUATION_INTERVAL) {
            situationClock = cachedSituation == null
                    ? 0.0F : situationClock % SITUATION_INTERVAL;
            cachedSituation = context.strategicMap();
            boolean replan = teamPlan == null || replanClock >= REPLAN_INTERVAL
                    || !teamPlan.frontlineOperational(cachedSituation);
            if (replan) {
                StrategicTeamPlan previous = teamPlan;
                teamPlan = StrategicTeamPlan.replan(cachedSituation, previous);
                replanClock %= REPLAN_INTERVAL;
                boolean changed = teamPlan.doctrineChangedFrom(previous);
                if (changed && previous != null) {
                    buildPlanner.onStrategicReplan();
                    forcePlanner.onStrategicReplan();
                    resourceCampaign.onStrategicReplan();
                }
                io.github.endx.rustedfabricapi.api.world.WorldPoint objective =
                        teamPlan.preferredFrontierPoint();
                System.out.println("[Strategic AI] Team " + context.team().id()
                        + (previous == null ? " position=" : " replanned position=")
                        + teamPlan.ownRole()
                        + ", forwardOpening=" + teamPlan.usesForwardOpening()
                        + ", landFront=" + (objective != null
                        ? (int) objective.x() + "," + (int) objective.y() : "none"));
                if (changed || previous == null) {
                    System.out.println("[Strategic AI] Allied assignments "
                            + teamPlan.assignmentSummary());
                }
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
        boolean microDue = cachedSituation != null && microClock >= MICRO_INTERVAL;
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
        if (microDue) {
            microClock %= MICRO_INTERVAL;
            forcePlanner.updateMicro(context, cachedSituation, microCycle++, teamPlan);
        }
        if (forceDue) {
            forceClock %= FORCE_INTERVAL;
            forcePlanner.update(context, cachedSituation, forceCycle++,
                    resourceCampaign, teamPlan, frontState);
        }
        return AiTickDecision.REPLACE_NATIVE;
    }
}
