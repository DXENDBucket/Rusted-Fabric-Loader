package io.github.endx.rustedfabricapi.impl.combat;

/** Exact primitive form of the native 1.15 shield/hull damage calculation. */
public final class NativeDamageMath {
    private NativeDamageMath() { }

    public static float projectedHp(float hp, float movementCompletion, float shieldDelay,
                                    float shield, float requestedDamage,
                                    float shieldDamageMultiplier,
                                    float shieldDeflectionMultiplier,
                                    float hullDamageMultiplier) {
        float adjustedDamage = movementCompletion < 1.0F
                ? requestedDamage * 1.75F : requestedDamage;
        float remainingDamage = adjustedDamage;
        if (shieldDelay == 0.0F && shield > 0.0F) {
            float shieldDamage = adjustedDamage * shieldDamageMultiplier;
            remainingDamage = shield < shieldDamage
                    ? adjustedDamage - shield * shieldDeflectionMultiplier
                    : adjustedDamage - adjustedDamage * shieldDeflectionMultiplier;
        }
        return remainingDamage > 0.0F
                ? hp - remainingDamage * hullDamageMultiplier : hp;
    }
}
