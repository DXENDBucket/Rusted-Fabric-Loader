package io.github.endx.strategicai;

import io.github.endx.rustedfabricapi.api.ai.AiController;
import io.github.endx.rustedfabricapi.api.ai.AiStrategicMapSnapshot;
import io.github.endx.rustedfabricapi.api.ai.AiTickContext;
import io.github.endx.rustedfabricapi.api.ai.AiTickDecision;

/** Deterministic first-pass economy and force controller for one team. */
public final class StrategicAiController implements AiController {
    private static final float ECONOMY_INTERVAL = 45.0F;
    private static final float FORCE_INTERVAL = 120.0F;

    private final int teamId;
    private final StrategicBuildPlanner buildPlanner = new StrategicBuildPlanner();
    private final StrategicForcePlanner forcePlanner = new StrategicForcePlanner();
    private float economyClock = ECONOMY_INTERVAL;
    private float forceClock = FORCE_INTERVAL;
    private long economyCycle;

    StrategicAiController(int teamId) {
        this.teamId = teamId;
    }

    @Override
    public AiTickDecision tick(AiTickContext context) {
        economyClock += context.delta();
        forceClock += context.delta();
        AiStrategicMapSnapshot situation = null;
        if (economyClock >= ECONOMY_INTERVAL) {
            economyClock %= ECONOMY_INTERVAL;
            situation = context.strategicMap();
            buildPlanner.update(context, situation, economyCycle++);
        }
        if (forceClock >= FORCE_INTERVAL) {
            forceClock %= FORCE_INTERVAL;
            if (situation == null) situation = context.strategicMap();
            forcePlanner.update(context, situation);
        }
        return AiTickDecision.REPLACE_NATIVE;
    }

    int teamId() { return teamId; }
}
