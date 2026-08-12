package io.github.endx.vulkanmod.spi;

/** Ordered ResourceStream acknowledgement and optional completion payload. */
public final class VulkanResourceStreamResult {
    private final long appliedSequence;
    private final long completionId;
    private final VulkanTextureData textureReadback;
    private final boolean completionPending;

    private VulkanResourceStreamResult(long appliedSequence, long completionId,
                                       VulkanTextureData textureReadback,
                                       boolean completionPending) {
        if (appliedSequence < 0L) throw new IllegalArgumentException("negative applied sequence");
        if (completionId < 0L) throw new IllegalArgumentException("negative completion ID");
        if (textureReadback != null && completionId == 0L) throw new IllegalArgumentException(
                "completion payload requires a positive completion ID");
        if (completionPending && completionId == 0L) throw new IllegalArgumentException(
                "pending completion requires a positive completion ID");
        if (completionPending && textureReadback != null) throw new IllegalArgumentException(
                "pending completion cannot already contain a payload");
        this.appliedSequence = appliedSequence;
        this.completionId = completionId;
        this.textureReadback = textureReadback;
        this.completionPending = completionPending;
    }

    public static VulkanResourceStreamResult applied(long sequence) {
        return new VulkanResourceStreamResult(sequence, 0L, null, false);
    }

    /** The stream was accepted, but its requested completion is not ready yet. */
    public static VulkanResourceStreamResult pending(long sequence, long completionId) {
        if (completionId <= 0L) throw new IllegalArgumentException(
                "completion ID must be positive");
        return new VulkanResourceStreamResult(sequence, completionId, null, true);
    }

    public static VulkanResourceStreamResult textureReadback(long sequence, long completionId,
                                                              VulkanTextureData texture) {
        if (completionId <= 0L) throw new IllegalArgumentException(
                "readback completion ID must be positive");
        if (texture == null) throw new NullPointerException("texture");
        return new VulkanResourceStreamResult(sequence, completionId, texture, false);
    }

    public static VulkanResourceStreamResult completed(long sequence, long completionId) {
        if (completionId <= 0L) throw new IllegalArgumentException(
                "completion ID must be positive");
        return new VulkanResourceStreamResult(sequence, completionId, null, false);
    }

    public long appliedSequence() { return appliedSequence; }
    public long completionId() { return completionId; }
    public VulkanTextureData textureReadback() { return textureReadback; }
    public boolean hasCompletion() { return completionId != 0L; }
    public boolean completionPending() { return completionPending; }
    public boolean completionReady() { return completionId != 0L && !completionPending; }
}
