package io.github.endx.vulkanmod.spi;

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

    public static final VulkanShaderState DEFAULT =
            new VulkanShaderState(PLAIN, 1.0f, 1.0f, 1.0f, 1.0f, 0.15f);

    private final int effect;
    private final float red;
    private final float green;
    private final float blue;
    private final float alpha;
    private final float teamColorAmount;

    public VulkanShaderState(int effect, float red, float green, float blue, float alpha,
                             float teamColorAmount) {
        if (effect < PLAIN || effect > POST_BASE) {
            throw new IllegalArgumentException("unknown built-in shader effect: " + effect);
        }
        this.effect = effect;
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
        this.teamColorAmount = teamColorAmount;
    }

    public int effect() { return effect; }
    public float red() { return red; }
    public float green() { return green; }
    public float blue() { return blue; }
    public float alpha() { return alpha; }
    public float teamColorAmount() { return teamColorAmount; }

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
                        == Float.floatToIntBits(state.teamColorAmount);
    }

    @Override public int hashCode() {
        int result = effect;
        result = 31 * result + Float.floatToIntBits(red);
        result = 31 * result + Float.floatToIntBits(green);
        result = 31 * result + Float.floatToIntBits(blue);
        result = 31 * result + Float.floatToIntBits(alpha);
        result = 31 * result + Float.floatToIntBits(teamColorAmount);
        return result;
    }
}
