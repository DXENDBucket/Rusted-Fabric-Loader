package io.github.endx.vulkanmod.spi;

import java.util.Arrays;

/**
 * Immutable, renderer-neutral snapshot of the built-in image shader state used by one draw.
 *
 * <p>The game reuses and mutates {@code ShaderProgram} instances between units, so passing the
 * original object across the driver boundary would make queued frames observe a later unit's
 * uniforms. This value object captures only the state the native renderer needs.</p>
 */
public final class VulkanShaderState {
    public static final int PLAIN = 0;
    public static final int PURE_GREEN_TEAM_COLOR = 1;
    public static final int HUE_ADD_TEAM_COLOR = 2;
    public static final int HUE_SHIFT_TEAM_COLOR = 3;
    public static final int POST_BASE = 4;
    public static final int POST_DISPLACEMENT = 5;
    public static final int ERROR = 6;
    public static final int CUSTOM = 7;
    public static final int MAX_CUSTOM_FLOATS = 20;

    public static final VulkanShaderState DEFAULT =
            new VulkanShaderState(PLAIN, 1.0f, 1.0f, 1.0f, 1.0f, 0.15f,
                    0L, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f);

    private final int effect;
    private final float red;
    private final float green;
    private final float blue;
    private final float alpha;
    private final float teamColorAmount;
    private final long secondaryTextureHandle;
    private final float screenBaseWidth;
    private final float screenBaseHeight;
    private final float resolutionWidth;
    private final float resolutionHeight;
    private final float displacementOffset;
    private final float uiScaling;
    private final long customShaderHandle;
    private final float[] customValues;

    public VulkanShaderState(int effect, float red, float green, float blue, float alpha,
                             float teamColorAmount) {
        this(effect, red, green, blue, alpha, teamColorAmount,
                0L, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f);
    }

    public VulkanShaderState(int effect, float red, float green, float blue, float alpha,
                             float teamColorAmount, long secondaryTextureHandle,
                             float screenBaseWidth, float screenBaseHeight,
                             float resolutionWidth, float resolutionHeight,
                             float displacementOffset, float uiScaling) {
        this(effect, red, green, blue, alpha, teamColorAmount, secondaryTextureHandle,
                screenBaseWidth, screenBaseHeight, resolutionWidth, resolutionHeight,
                displacementOffset, uiScaling, 0L, null);
    }

    private VulkanShaderState(int effect, float red, float green, float blue, float alpha,
                              float teamColorAmount, long secondaryTextureHandle,
                              float screenBaseWidth, float screenBaseHeight,
                              float resolutionWidth, float resolutionHeight,
                              float displacementOffset, float uiScaling,
                              long customShaderHandle, float[] customValues) {
        if (effect < PLAIN || effect > CUSTOM) {
            throw new IllegalArgumentException("unknown built-in shader effect: " + effect);
        }
        if (secondaryTextureHandle < 0L) {
            throw new IllegalArgumentException("secondary texture handle must not be negative");
        }
        requireFinitePositive("screenBaseWidth", screenBaseWidth);
        requireFinitePositive("screenBaseHeight", screenBaseHeight);
        requireFinitePositive("resolutionWidth", resolutionWidth);
        requireFinitePositive("resolutionHeight", resolutionHeight);
        if (!Float.isFinite(displacementOffset)) {
            throw new IllegalArgumentException("displacementOffset must be finite");
        }
        requireFinitePositive("uiScaling", uiScaling);
        if (effect == POST_DISPLACEMENT && secondaryTextureHandle == 0L) {
            throw new IllegalArgumentException(
                    "post displacement requires a secondary texture");
        }
        if (effect == CUSTOM && customShaderHandle <= 0L) {
            throw new IllegalArgumentException("custom shader requires a positive handle");
        }
        if (customValues != null && customValues.length > MAX_CUSTOM_FLOATS) {
            throw new IllegalArgumentException("custom shader has more than "
                    + MAX_CUSTOM_FLOATS + " float components");
        }
        this.effect = effect;
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
        this.teamColorAmount = teamColorAmount;
        this.secondaryTextureHandle = secondaryTextureHandle;
        this.screenBaseWidth = screenBaseWidth;
        this.screenBaseHeight = screenBaseHeight;
        this.resolutionWidth = resolutionWidth;
        this.resolutionHeight = resolutionHeight;
        this.displacementOffset = displacementOffset;
        this.uiScaling = uiScaling;
        this.customShaderHandle = customShaderHandle;
        this.customValues = customValues == null ? new float[0] : customValues.clone();
    }

    public static VulkanShaderState custom(long shaderHandle, long secondaryTextureHandle,
                                            float[] values) {
        return new VulkanShaderState(CUSTOM, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f,
                secondaryTextureHandle, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f,
                shaderHandle, values);
    }

    public int effect() { return effect; }
    public float red() { return red; }
    public float green() { return green; }
    public float blue() { return blue; }
    public float alpha() { return alpha; }
    public float teamColorAmount() { return teamColorAmount; }
    public long secondaryTextureHandle() { return secondaryTextureHandle; }
    public float screenBaseWidth() { return screenBaseWidth; }
    public float screenBaseHeight() { return screenBaseHeight; }
    public float resolutionWidth() { return resolutionWidth; }
    public float resolutionHeight() { return resolutionHeight; }
    public float displacementOffset() { return displacementOffset; }
    public float uiScaling() { return uiScaling; }
    public long customShaderHandle() { return customShaderHandle; }
    public float[] customValues() { return customValues.clone(); }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof VulkanShaderState)) return false;
        VulkanShaderState state = (VulkanShaderState) other;
        return effect == state.effect
                && Float.floatToIntBits(red) == Float.floatToIntBits(state.red)
                && Float.floatToIntBits(green) == Float.floatToIntBits(state.green)
                && Float.floatToIntBits(blue) == Float.floatToIntBits(state.blue)
                && Float.floatToIntBits(alpha) == Float.floatToIntBits(state.alpha)
                && Float.floatToIntBits(teamColorAmount)
                        == Float.floatToIntBits(state.teamColorAmount)
                && secondaryTextureHandle == state.secondaryTextureHandle
                && Float.floatToIntBits(screenBaseWidth)
                        == Float.floatToIntBits(state.screenBaseWidth)
                && Float.floatToIntBits(screenBaseHeight)
                        == Float.floatToIntBits(state.screenBaseHeight)
                && Float.floatToIntBits(resolutionWidth)
                        == Float.floatToIntBits(state.resolutionWidth)
                && Float.floatToIntBits(resolutionHeight)
                        == Float.floatToIntBits(state.resolutionHeight)
                && Float.floatToIntBits(displacementOffset)
                        == Float.floatToIntBits(state.displacementOffset)
                && Float.floatToIntBits(uiScaling) == Float.floatToIntBits(state.uiScaling)
                && customShaderHandle == state.customShaderHandle
                && Arrays.equals(customValues, state.customValues);
    }

    @Override public int hashCode() {
        int result = effect;
        result = 31 * result + Float.floatToIntBits(red);
        result = 31 * result + Float.floatToIntBits(green);
        result = 31 * result + Float.floatToIntBits(blue);
        result = 31 * result + Float.floatToIntBits(alpha);
        result = 31 * result + Float.floatToIntBits(teamColorAmount);
        result = 31 * result + Long.hashCode(secondaryTextureHandle);
        result = 31 * result + Float.floatToIntBits(screenBaseWidth);
        result = 31 * result + Float.floatToIntBits(screenBaseHeight);
        result = 31 * result + Float.floatToIntBits(resolutionWidth);
        result = 31 * result + Float.floatToIntBits(resolutionHeight);
        result = 31 * result + Float.floatToIntBits(displacementOffset);
        result = 31 * result + Float.floatToIntBits(uiScaling);
        result = 31 * result + Long.hashCode(customShaderHandle);
        result = 31 * result + Arrays.hashCode(customValues);
        return result;
    }

    private static void requireFinitePositive(String name, float value) {
        if (!Float.isFinite(value) || value <= 0.0f) {
            throw new IllegalArgumentException(name + " must be finite and positive");
        }
    }
}
