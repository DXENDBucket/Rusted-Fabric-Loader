package io.github.endx.strategicai;

import io.github.endx.rustedfabricapi.api.ai.AiResourceControl;
import io.github.endx.rustedfabricapi.api.ai.AiResourceObjectiveKind;

public final class FrontierResourcePolicyVerification {
    private FrontierResourcePolicyVerification() {
    }

    public static void main(String[] args) {
        float contested = FrontierResourcePolicy.score(0.8F,
                AiResourceObjectiveKind.LOCK_DOWN, 0.45F, 4.0F, 3.0F);
        float remoteSafe = FrontierResourcePolicy.score(0.6F,
                AiResourceObjectiveKind.CAPTURE, 0.85F, 0.0F, 0.0F);
        require(contested > remoteSafe,
                "a valuable frontline resource lost to a remote passive expansion");
        require(FrontierResourcePolicy.phase(AiResourceControl.UNCLAIMED,
                        1, 0, 2) == StrategicResourceCampaign.Phase.ASSEMBLE,
                "an unescorted builder was allowed to claim the front");
        require(FrontierResourcePolicy.phase(AiResourceControl.UNCLAIMED,
                        2, 0, 2) == StrategicResourceCampaign.Phase.BUILD,
                "a secured resource was not released for construction");
        require(FrontierResourcePolicy.phase(AiResourceControl.ENEMY,
                        3, 0, 2) == StrategicResourceCampaign.Phase.SECURE,
                "an enemy extractor was treated as buildable");
        require(FrontierResourcePolicy.phase(AiResourceControl.OWN,
                        2, 0, 2) == StrategicResourceCampaign.Phase.HOLD,
                "a newly captured extractor was not held");
        require(FrontierResourcePolicy.phase(AiResourceControl.UNCLAIMED,
                        0, 0, 2, true, false)
                        == StrategicResourceCampaign.Phase.FORTIFY,
                "a suitable forward opening waited for a conventional escort");
        require(FrontierResourcePolicy.phase(AiResourceControl.UNCLAIMED,
                        0, 0, 2, true, true)
                        == StrategicResourceCampaign.Phase.BUILD,
                "a fortified forward resource was not released for mining");
        System.out.println("Strategic AI frontier resource policy contracts passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
