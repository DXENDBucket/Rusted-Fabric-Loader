package io.github.endx.vulkanmod.android;

import io.github.endx.vulkanmod.spi.VulkanGlyphBitmap;
import io.github.endx.vulkanmod.spi.VulkanGlyphPlacement;
import io.github.endx.vulkanmod.spi.VulkanTextLayout;
import io.github.endx.vulkanmod.spi.VulkanTextRasterizer;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Cacio/AWT text bridge used by the embedded Android HotSpot VM.
 *
 * <p>The Android launcher already provides the headless AWT implementation required by the
 * desktop game. Keeping shaping here means the shared glyph atlas and Vulkan backend do not need
 * Android framework objects or a second Java/ART callback path.</p>
 */
final class AndroidAwtTextRasterizer implements VulkanTextRasterizer {
    private final Font regular = load("font/Roboto-Regular.ttf", Font.PLAIN);
    private final Font bold = load("font/Roboto-Bold.ttf", Font.BOLD);
    private final Font unicodeFallback = load("font/DroidSansFallback.ttf", Font.PLAIN);
    private final Map<Glyph, Long> keys = new HashMap<Glyph, Long>();
    private final Map<Long, Glyph> glyphs = new HashMap<Long, Glyph>();
    private long nextKey = 1L;

    @Override public synchronized VulkanTextLayout layout(
            String text, int requestedSize, boolean useBold) {
        if (text == null) throw new NullPointerException("text");
        int size = Math.max(4, Math.min(256, requestedSize));
        Font source = useBold ? bold : regular;
        if (source.canDisplayUpTo(text) >= 0 && unicodeFallback.canDisplayUpTo(text) < 0) {
            source = unicodeFallback;
        }
        Font font = source.deriveFont(useBold ? Font.BOLD : Font.PLAIN, (float) size);
        BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = probe.createGraphics();
        configure(graphics);
        graphics.setFont(font);
        try {
            FontMetrics metrics = graphics.getFontMetrics();
            int lineHeight = Math.max(1, metrics.getHeight());
            int descent = metrics.getDescent() + metrics.getLeading();
            String[] lines = text.split("\\n", -1);
            int width = 0;
            ArrayList<VulkanGlyphPlacement> placements =
                    new ArrayList<VulkanGlyphPlacement>(text.length());
            FontRenderContext context = graphics.getFontRenderContext();
            for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
                char[] characters = lines[lineIndex].toCharArray();
                GlyphVector run = font.layoutGlyphVector(context, characters,
                        0, characters.length, Font.LAYOUT_LEFT_TO_RIGHT);
                for (int glyphIndex = 0; glyphIndex < run.getNumGlyphs(); glyphIndex++) {
                    int glyphCode = run.getGlyphCode(glyphIndex);
                    Rectangle bounds = run.getGlyphPixelBounds(
                            glyphIndex, context, 0.0f, 0.0f);
                    GlyphVector isolated = font.createGlyphVector(
                            context, new int[] {glyphCode});
                    Rectangle isolatedBounds = isolated.getGlyphPixelBounds(
                            0, context, 0.0f, 0.0f);
                    placements.add(new VulkanGlyphPlacement(key(font, glyphCode),
                            bounds.x - isolatedBounds.x,
                            lineIndex * lineHeight - descent
                                    + bounds.y - isolatedBounds.y));
                }
                int lineWidth = (int) Math.ceil(
                        run.getGlyphPosition(run.getNumGlyphs()).getX());
                width = Math.max(width, lineWidth);
            }
            return new VulkanTextLayout(Math.min(4096, Math.max(1, width)),
                    Math.min(4096, Math.max(1, lineHeight * lines.length)),
                    lineHeight, placements.toArray(new VulkanGlyphPlacement[0]));
        } finally {
            graphics.dispose();
        }
    }

    @Override public synchronized VulkanGlyphBitmap rasterizeGlyph(long glyphKey) {
        Glyph glyph = glyphs.get(Long.valueOf(glyphKey));
        if (glyph == null) throw new IllegalArgumentException("unknown glyph key " + glyphKey);
        BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D probeGraphics = probe.createGraphics();
        configure(probeGraphics);
        FontRenderContext context = probeGraphics.getFontRenderContext();
        probeGraphics.dispose();
        GlyphVector vector = glyph.font.createGlyphVector(
                context, new int[] {glyph.code});
        Rectangle bounds = vector.getGlyphPixelBounds(0, context, 0.0f, 0.0f);
        if (bounds.width <= 0 || bounds.height <= 0) {
            return new VulkanGlyphBitmap(bounds.x, bounds.y, 0, 0, new byte[0]);
        }
        BufferedImage image = new BufferedImage(
                bounds.width, bounds.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        configure(graphics);
        graphics.setColor(Color.WHITE);
        graphics.drawGlyphVector(vector, -bounds.x, -bounds.y);
        graphics.dispose();
        byte[] rgba = new byte[Math.multiplyExact(
                Math.multiplyExact(bounds.width, bounds.height), 4)];
        int offset = 0;
        for (int y = 0; y < bounds.height; y++) {
            for (int x = 0; x < bounds.width; x++) {
                int alpha = image.getRGB(x, y) >>> 24;
                rgba[offset++] = (byte) 255;
                rgba[offset++] = (byte) 255;
                rgba[offset++] = (byte) 255;
                rgba[offset++] = (byte) alpha;
            }
        }
        return new VulkanGlyphBitmap(bounds.x, bounds.y,
                bounds.width, bounds.height, rgba);
    }

    @Override public synchronized void close() {
        keys.clear();
        glyphs.clear();
    }

    private long key(Font font, int code) {
        Glyph candidate = new Glyph(font, code);
        Long known = keys.get(candidate);
        if (known != null) return known.longValue();
        if (nextKey == Long.MAX_VALUE) throw new IllegalStateException(
                "Android glyph keys exhausted");
        long created = nextKey++;
        keys.put(candidate, Long.valueOf(created));
        glyphs.put(Long.valueOf(created), candidate);
        return created;
    }

    private static Font load(String resource, int fallbackStyle) {
        ClassLoader context = Thread.currentThread().getContextClassLoader();
        try (InputStream stream = context == null
                ? AndroidAwtTextRasterizer.class.getClassLoader().getResourceAsStream(resource)
                : context.getResourceAsStream(resource)) {
            if (stream == null) throw new IOException("resource not found: " + resource);
            return Font.createFont(Font.TRUETYPE_FONT, stream);
        } catch (Exception failure) {
            System.out.println("[Vulkan Mod/Android] Could not load " + resource
                    + "; using logical SansSerif: " + failure.getMessage());
            return new Font(Font.SANS_SERIF, fallbackStyle, 1);
        }
    }

    private static void configure(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    }

    private static final class Glyph {
        private final Font font;
        private final int code;

        private Glyph(Font font, int code) {
            this.font = font;
            this.code = code;
        }

        @Override public boolean equals(Object candidate) {
            if (this == candidate) return true;
            if (!(candidate instanceof Glyph)) return false;
            Glyph other = (Glyph) candidate;
            return code == other.code && font.equals(other.font);
        }

        @Override public int hashCode() {
            return 31 * font.hashCode() + code;
        }
    }
}
