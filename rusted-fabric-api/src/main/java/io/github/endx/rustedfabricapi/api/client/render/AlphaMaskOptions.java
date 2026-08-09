package io.github.endx.rustedfabricapi.api.client.render;

import java.util.Objects;

/** Immutable alpha threshold and composition policy for image masks. */
public final class AlphaMaskOptions {
    public static final AlphaMaskOptions DEFAULT = new AlphaMaskOptions(
            0.0F, false, MaskThresholdMode.KEEP, MaskAlphaMode.MULTIPLY);

    private final float threshold;
    private final boolean inverted;
    private final MaskThresholdMode thresholdMode;
    private final MaskAlphaMode alphaMode;

    public AlphaMaskOptions(float threshold, boolean inverted,
            MaskThresholdMode thresholdMode, MaskAlphaMode alphaMode) {
        if (!Float.isFinite(threshold) || threshold < 0.0F || threshold > 1.0F) {
            throw new IllegalArgumentException("threshold must be between zero and one");
        }
        this.threshold = threshold;
        this.inverted = inverted;
        this.thresholdMode = Objects.requireNonNull(thresholdMode, "thresholdMode");
        this.alphaMode = Objects.requireNonNull(alphaMode, "alphaMode");
    }

    public float threshold() { return threshold; }
    public boolean inverted() { return inverted; }
    public MaskThresholdMode thresholdMode() { return thresholdMode; }
    public MaskAlphaMode alphaMode() { return alphaMode; }

    /** Applies inversion, threshold treatment, and the selected content-alpha formula. */
    public int combineAlpha(int contentAlpha, float sampledMaskAlpha) {
        if (contentAlpha < 0 || contentAlpha > 255) {
            throw new IllegalArgumentException("contentAlpha must be between zero and 255");
        }
        if (!Float.isFinite(sampledMaskAlpha)) {
            throw new IllegalArgumentException("sampled mask alpha must be finite");
        }
        float mask = clamp(sampledMaskAlpha);
        if (inverted) mask = 1.0F - mask;
        if (mask < threshold) {
            mask = 0.0F;
        } else {
            switch (thresholdMode) {
                case BINARY:
                    mask = 1.0F;
                    break;
                case NORMALIZE:
                    mask = threshold >= 1.0F ? 1.0F
                            : (mask - threshold) / (1.0F - threshold);
                    break;
                case KEEP:
                default:
                    break;
            }
        }
        float content = contentAlpha / 255.0F;
        float combined;
        switch (alphaMode) {
            case MIN: combined = Math.min(content, mask); break;
            case REPLACE: combined = mask; break;
            case MULTIPLY:
            default: combined = content * mask; break;
        }
        return Math.round(clamp(combined) * 255.0F);
    }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
