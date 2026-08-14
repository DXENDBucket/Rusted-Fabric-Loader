package io.github.endx.rustedfabricapi.api.ai;

/** A game-thread controller for one explicitly assigned AI team. */
@FunctionalInterface
public interface AiController {
    /**
     * Performs one AI update.
     *
     * <p>Return {@link AiTickDecision#REPLACE_NATIVE} only when this update has intentionally
     * replaced the original AI. Returning {@link AiTickDecision#PASS} permits incremental
     * controllers that observe or augment the native implementation.</p>
     */
    AiTickDecision tick(AiTickContext context);
}
