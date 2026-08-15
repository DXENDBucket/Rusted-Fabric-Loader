package io.github.endx.strategicai;

import io.github.endx.rustedfabricapi.api.ai.AiResourceControl;
import io.github.endx.rustedfabricapi.api.ai.AiResourceObjectiveKind;

/** Pure scoring and phase rules for one contested resource operation. */
final class FrontierResourcePolicy {
    private FrontierResourcePolicy() {
    }

    static float score(float strategicPriority, AiResourceObjectiveKind objective,
            float distanceFraction, float friendlyInfluence, float enemyInfluence) {
        float objectiveBonus = objective == AiResourceObjectiveKind.DENY ? 0.32F
                : objective == AiResourceObjectiveKind.LOCK_DOWN ? 0.26F
                : objective == AiResourceObjectiveKind.CAPTURE ? 0.08F : -0.2F;
        float risk = enemyInfluence / (friendlyInfluence + enemyInfluence + 1.0F);
        float distanceFitness = 1.0F - Math.min(1.0F, Math.max(0.0F, distanceFraction));
        return strategicPriority + objectiveBonus + distanceFitness * 0.18F - risk * 0.28F;
    }

    static StrategicResourceCampaign.Phase phase(AiResourceControl control,
            int escortsNear, int enemiesNear, int requiredEscorts) {
        return phase(control, escortsNear, enemiesNear, requiredEscorts,
                false, false);
    }

    static StrategicResourceCampaign.Phase phase(AiResourceControl control,
            int escortsNear, int enemiesNear, int requiredEscorts,
            boolean forwardOpening, boolean fortified) {
        if (control == AiResourceControl.OWN) {
            return enemiesNear > 0 ? StrategicResourceCampaign.Phase.SECURE
                    : StrategicResourceCampaign.Phase.HOLD;
        }
        if (control == AiResourceControl.ENEMY || control == AiResourceControl.NEUTRAL
                || enemiesNear > 0) return StrategicResourceCampaign.Phase.SECURE;
        if (forwardOpening && !fortified) {
            return StrategicResourceCampaign.Phase.FORTIFY;
        }
        if (forwardOpening && fortified) {
            return StrategicResourceCampaign.Phase.BUILD;
        }
        return escortsNear >= requiredEscorts ? StrategicResourceCampaign.Phase.BUILD
                : StrategicResourceCampaign.Phase.ASSEMBLE;
    }
}
