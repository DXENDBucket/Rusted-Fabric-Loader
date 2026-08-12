package io.github.endx.vulkanmod.spi;

/** Platform text shaping/rasterization boundary; implementations may use AWT, FreeType, or Skia. */
public interface VulkanTextRasterizer extends AutoCloseable {
    /** Shapes a string and returns stable glyph keys plus baseline-relative placements. */
    VulkanTextLayout layout(String text, int pixelSize, boolean bold);

    /** Rasterizes one stable key previously returned by {@link #layout}. */
    VulkanGlyphBitmap rasterizeGlyph(long glyphKey);

    /** Compatibility path used while legacy takeover still uploads whole text runs. */
    VulkanTextureData rasterizeText(String text, int pixelSize, boolean bold);

    @Override default void close() { }
}
