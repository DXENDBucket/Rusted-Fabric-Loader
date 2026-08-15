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
    private float economyClock = ECONOMY_INTERVAL;
    private float forceClock = FORCE_INTERVAL;
    private float strategicClock;
    private long economyCycle;
    private AiStrategicMapSnapshot cachedSituation;

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
        }
        if (cachedSituation != null && economyClock >= ECONOMY_INTERVAL) {
            economyClock %= ECONOMY_INTERVAL;
            buildPlanner.update(context, cachedSituation, economyCycle++);
        }
        if (cachedSituation != null && forceClock >= FORCE_INTERVAL) {
            forceClock %= FORCE_INTERVAL;
            forcePlanner.update(context, cachedSituation);
        }
        return AiTickDecision.REPLACE_NATIVE;
    }
}
