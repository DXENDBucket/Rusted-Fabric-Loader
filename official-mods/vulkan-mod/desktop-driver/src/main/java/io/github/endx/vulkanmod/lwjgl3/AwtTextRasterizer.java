package io.github.endx.vulkanmod.lwjgl3;

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
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Bidi;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/** Desktop text service kept inside the isolated driver so the shared atlas is AWT-neutral. */
final class AwtTextRasterizer implements VulkanTextRasterizer {
    private final Font regularFont;
    private final Font boldFont;
    private final Font legacyFallbackFont;
    private final Map<GlyphKey, Long> glyphKeys = new HashMap<GlyphKey, Long>();
    private final Map<Long, GlyphKey> glyphs = new HashMap<Long, GlyphKey>();
    private long nextGlyphKey = 1L;

    AwtTextRasterizer() {
        regularFont = loadGameFont("font/Roboto-Regular.ttf", Font.PLAIN);
        boldFont = loadGameFont("font/Roboto-Bold.ttf", Font.BOLD);
        legacyFallbackFont = loadGameFont("font/DroidSansFallback.ttf", Font.PLAIN);
    }

    @Override public synchronized VulkanTextLayout layout(
            String text, int requestedSize, boolean bold) {
        if (text == null) throw new NullPointerException("text");
        int size = clampSize(requestedSize);
        Font font = selectFont((bold ? boldFont : regularFont).deriveFont((float) size),
                text, size);
        BufferedImage contextImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = contextImage.createGraphics();
        configure(graphics);
        graphics.setFont(font);
        try {
            FontMetrics metrics = graphics.getFontMetrics();
            String[] lines = text.split("\\n", -1);
            int width = 1;
            for (String line : lines) width = Math.max(width, metrics.stringWidth(line));
            int lineHeight = Math.max(1, metrics.getHeight());
            int height = Math.max(1, Math.multiplyExact(lineHeight, lines.length));
            FontRenderContext context = graphics.getFontRenderContext();
            ArrayList<VulkanGlyphPlacement> placements =
                    new ArrayList<VulkanGlyphPlacement>(text.length());
            for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
                char[] characters = lines[lineIndex].toCharArray();
                int flags = Bidi.requiresBidi(characters, 0, characters.length)
                        ? Font.LAYOUT_RIGHT_TO_LEFT : Font.LAYOUT_LEFT_TO_RIGHT;
                GlyphVector run = font.layoutGlyphVector(context, characters,
                        0, characters.length, flags);
                for (int glyphIndex = 0; glyphIndex < run.getNumGlyphs(); glyphIndex++) {
                    int glyphCode = run.getGlyphCode(glyphIndex);
                    Point2D position = run.getGlyphPosition(glyphIndex);
                    placements.add(new VulkanGlyphPlacement(key(font, glyphCode),
                            Math.round((float) position.getX()), lineIndex * lineHeight));
                }
            }
            return new VulkanTextLayout(Math.min(4096, width), Math.min(4096, height),
                    lineHeight, placements.toArray(new VulkanGlyphPlacement[0]));
        } finally {
            graphics.dispose();
        }
    }

    @Override public synchronized VulkanGlyphBitmap rasterizeGlyph(long glyphKey) {
        GlyphKey glyph = glyphs.get(Long.valueOf(glyphKey));
        if (glyph == null) throw new IllegalArgumentException("unknown glyph key " + glyphKey);
        BufferedImage contextImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D contextGraphics = contextImage.createGraphics();
        configure(contextGraphics);
        FontRenderContext context = contextGraphics.getFontRenderContext();
        contextGraphics.dispose();
        GlyphVector vector = glyph.font.createGlyphVector(context,
                new int[] { glyph.glyphCode });
        Rectangle bounds = vector.getGlyphPixelBounds(0, context, 0.0f, 0.0f);
        if (bounds.width <= 0 || bounds.height <= 0) {
            return new VulkanGlyphBitmap(bounds.x, bounds.y, 0, 0, new byte[0]);
        }
        BufferedImage image = new BufferedImage(bounds.width, bounds.height,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        configure(graphics);
        graphics.setFont(glyph.font);
        graphics.setColor(Color.WHITE);
        graphics.drawGlyphVector(vector, -bounds.x, -bounds.y);
        graphics.dispose();
        return new VulkanGlyphBitmap(bounds.x, bounds.y, bounds.width, bounds.height,
                rgba(image));
    }

    private long key(Font font, int glyphCode) {
        GlyphKey candidate = new GlyphKey(font, glyphCode);
        Long known = glyphKeys.get(candidate);
        if (known != null) return known.longValue();
        if (nextGlyphKey == Long.MAX_VALUE) {
            throw new IllegalStateException("desktop glyph keys exhausted");
        }
        long created = nextGlyphKey++;
        glyphKeys.put(candidate, Long.valueOf(created));
        glyphs.put(Long.valueOf(created), candidate);
        return created;
    }

    @Override public synchronized void close() {
        glyphKeys.clear();
        glyphs.clear();
    }

    private Font selectFont(Font primary, String text, int size) {
        // Match SlickGraphicsBackend: any non-ASCII text selects DroidSansFallback for the
        // complete run. Using a different CJK font changes both its small-size strokes and
        // vertical metrics, so seemingly equivalent fallback logic visibly shifts game UI.
        for (int index = 0; index < text.length(); index++) {
            if (text.charAt(index) > 127) {
                return legacyFallbackFont.deriveFont((float) size);
            }
        }
        return primary;
    }

    private static int clampSize(int requestedSize) {
        return Math.max(4, Math.min(256, requestedSize));
    }

    private static byte[] rgba(BufferedImage image) {
        byte[] rgba = new byte[Math.multiplyExact(
                Math.multiplyExact(image.getWidth(), image.getHeight()), 4)];
        int offset = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int alpha = image.getRGB(x, y) >>> 24;
                rgba[offset++] = (byte) 255;
                rgba[offset++] = (byte) 255;
                rgba[offset++] = (byte) 255;
                rgba[offset++] = (byte) alpha;
            }
        }
        return rgba;
    }

    private static Font loadGameFont(String resource, int fallbackStyle) {
        try (InputStream stream = openGameResource(resource)) {
            if (stream == null) throw new IOException("resource not found: " + resource);
            return Font.createFont(Font.TRUETYPE_FONT, stream);
        } catch (Exception failure) {
            System.out.println("[Vulkan Mod] Could not load original game font " + resource
                    + "; using logical SansSerif: " + failure.getMessage());
            return new Font(Font.SANS_SERIF, fallbackStyle, 1);
        }
    }

    private static InputStream openGameResource(String resource) throws IOException {
        ClassLoader context = Thread.currentThread().getContextClassLoader();
        InputStream stream = context == null ? null : context.getResourceAsStream(resource);
        if (stream == null) stream = AwtTextRasterizer.class.getClassLoader()
                .getResourceAsStream(resource);
        if (stream != null) return stream;
        Path path = Paths.get(resource);
        return Files.isRegularFile(path) ? Files.newInputStream(path) : null;
    }

    private static void configure(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
    }

    private static final class GlyphKey {
        private final Font font;
        private final int glyphCode;

        private GlyphKey(Font font, int glyphCode) {
            this.font = font;
            this.glyphCode = glyphCode;
        }

        @Override public boolean equals(Object candidate) {
            if (this == candidate) return true;
            if (!(candidate instanceof GlyphKey)) return false;
            GlyphKey other = (GlyphKey) candidate;
            return glyphCode == other.glyphCode && font.equals(other.font);
        }

        @Override public int hashCode() { return 31 * font.hashCode() + glyphCode; }
    }
}
