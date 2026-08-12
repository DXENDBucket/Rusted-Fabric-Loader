package io.github.endx.vulkanmod.spi;

/** Ordered ResourceStream acknowledgement and optional completion payload. */
public final class VulkanResourceStreamResult {
    private final long appliedSequence;
    private final long completionId;
    private final VulkanTextureData textureReadback;

    private VulkanResourceStreamResult(long appliedSequence, long completionId,
                                       VulkanTextureData textureReadback) {
        if (appliedSequence < 0L) throw new IllegalArgumentException("negative applied sequence");
        if (completionId < 0L) throw new IllegalArgumentException("negative completion ID");
        if (textureReadback != null && completionId == 0L) throw new IllegalArgumentException(
                "completion payload requires a positive completion ID");
        this.appliedSequence = appliedSequence;
        this.completionId = completionId;
        this.textureReadback = textureReadback;
    }

    public static VulkanResourceStreamResult applied(long sequence) {
        return new VulkanResourceStreamResult(sequence, 0L, null);
    }

    public static VulkanResourceStreamResult textureReadback(long sequence, long completionId,
                                                              VulkanTextureData texture) {
        if (completionId <= 0L) throw new IllegalArgumentException(
                "readback completion ID must be positive");
        if (texture == null) throw new NullPointerException("texture");
        return new VulkanResourceStreamResult(sequence, completionId, texture);
    }

    public static VulkanResourceStreamResult completed(long sequence, long completionId) {
        if (completionId <= 0L) throw new IllegalArgumentException(
                "completion ID must be positive");
        return new VulkanResourceStreamResult(sequence, completionId, null);
    }

    public long appliedSequence() { return appliedSequence; }
    public long completionId() { return completionId; }
    public VulkanTextureData textureReadback() { return textureReadback; }
}
