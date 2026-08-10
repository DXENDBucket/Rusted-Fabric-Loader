package io.github.endx.rustedfabricapi.api.custom;

import io.github.endx.rustedfabricapi.impl.custom.PerActionAutoTriggerCooldownRuntime;

/**
 * Opt-in native auto-trigger cooldown isolation for custom-unit actions.
 *
 * <p>Once any action config belonging to a custom-unit metadata object is configured, the
 * metadata's native core cooldown becomes the per-action fallback. Configured actions use their
 * own duration, and firing one action no longer blocks the other auto-trigger actions on the same
 * unit instance.</p>
 */
public final class PerActionAutoTriggerCooldowns {
    private PerActionAutoTriggerCooldowns() { }

    /**
     * Configures one native custom-action config with a cooldown measured in seconds.
     * Metadata and action config are typed as Object so this API remains usable without exposing
     * mapped game classes in a mod's public ABI.
     */
    public static void configureSeconds(Object metadata, Object actionConfig, float seconds) {
        if (metadata == null) throw new NullPointerException("metadata");
        if (actionConfig == null) throw new NullPointerException("actionConfig");
        if (!Float.isFinite(seconds) || seconds < 0.0F) {
            throw new IllegalArgumentException("auto-trigger cooldown seconds must be finite and non-negative");
        }
        PerActionAutoTriggerCooldownRuntime.configureFrames(metadata, actionConfig, seconds * 60.0F);
    }

    /** Returns whether this metadata has opted into independent per-action cooldowns. */
    public static boolean isEnabled(Object metadata) {
        return metadata != null && PerActionAutoTriggerCooldownRuntime.isEnabled(metadata);
    }
}
