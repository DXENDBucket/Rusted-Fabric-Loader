package io.github.endx.vulkanmod.lwjgl3;

import io.github.endx.vulkanmod.spi.VulkanGlyphBitmap;
import io.github.endx.vulkanmod.spi.VulkanGlyphPlacement;
import io.github.endx.vulkanmod.spi.VulkanTextLayout;
import io.github.endx.vulkanmod.spi.VulkanTextRasterizer;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Desktop text service kept inside the isolated driver so the shared atlas is AWT-neutral. */
final class AwtTextRasterizer implements VulkanTextRasterizer {
    private final Font regularFont;
    private final Font boldFont;
    private final Font legacyFallbackFont;
    private final List<Font> systemFallbackFonts;
    private final Map<String, Font> systemFallbackCache = new HashMap<String, Font>();
    private final Set<String> missingFallbackCache = new HashSet<String>();
    private final Map<GlyphKey, Long> glyphKeys = new HashMap<GlyphKey, Long>();
    private final Map<Long, GlyphKey> glyphs = new HashMap<Long, GlyphKey>();
    private long nextGlyphKey = 1L;

    AwtTextRasterizer() {
        regularFont = loadGameFont("font/Roboto-Regular.ttf", Font.PLAIN);
        boldFont = loadGameFont("font/Roboto-Bold.ttf", Font.BOLD);
        legacyFallbackFont = loadGameFont("font/DroidSansFallback.ttf", Font.PLAIN);
        systemFallbackFonts = discoverSystemFallbackFonts();
    }

    @Override public synchronized VulkanTextLayout layout(
            String text, int requestedSize, boolean bold) {
        if (text == null) throw new NullPointerException("text");
        int size = clampSize(requestedSize);
        Font primary = slickFont((bold ? boldFont : regularFont).deriveFont((float) size));
        Font legacy = slickFont(legacyFallbackFont.deriveFont((float) size));
        boolean preferLegacy = containsNonAscii(text);
        BufferedImage contextImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = contextImage.createGraphics();
        configure(graphics);
        try {
            String[] lines = text.split("\\n", -1);
            ArrayList<List<FontRun>> lineRuns = new ArrayList<List<FontRun>>(lines.length);
            int lineHeight = 1;
            int baselineTail = 0;
            for (String line : lines) {
                List<FontRun> runs = fontRuns(line, primary, legacy, size,
                        bold, preferLegacy);
                lineRuns.add(runs);
                if (runs.isEmpty()) {
                    graphics.setFont(preferLegacy ? legacy : primary);
                    FontMetrics metrics = graphics.getFontMetrics();
                    lineHeight = Math.max(lineHeight, metrics.getHeight());
                    baselineTail = Math.max(baselineTail,
                            metrics.getDescent() + metrics.getLeading());
                } else {
                    for (FontRun run : runs) {
                        graphics.setFont(run.font);
                        FontMetrics metrics = graphics.getFontMetrics();
                        lineHeight = Math.max(lineHeight, metrics.getHeight());
                        baselineTail = Math.max(baselineTail,
                                metrics.getDescent() + metrics.getLeading());
                    }
                }
            }
            int width = 1;
            int height = Math.max(1, Math.multiplyExact(lineHeight, lines.length));
            FontRenderContext context = graphics.getFontRenderContext();
            ArrayList<VulkanGlyphPlacement> placements =
                    new ArrayList<VulkanGlyphPlacement>(text.length());
            for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
                float penX = 0.0f;
                for (FontRun fontRun : lineRuns.get(lineIndex)) {
                    char[] characters = fontRun.text.toCharArray();
                    GlyphVector run = fontRun.font.layoutGlyphVector(context, characters,
                            0, characters.length, Font.LAYOUT_LEFT_TO_RIGHT);
                    int spaceWidth = spaceWidth(fontRun.font, context);
                    for (int glyphIndex = 0; glyphIndex < run.getNumGlyphs(); glyphIndex++) {
                        int glyphCode = run.getGlyphCode(glyphIndex);
                        int characterIndex = run.getGlyphCharIndex(glyphIndex);
                        int codePoint = fontRun.text.codePointAt(characterIndex);
                        Rectangle bounds = run.getGlyphPixelBounds(
                                glyphIndex, context, penX, 0.0f);
                        if (codePoint == ' ') bounds.width = spaceWidth;
                        width = Math.max(width, bounds.x + bounds.width);
                        GlyphVector isolated = fontRun.font.createGlyphVector(context,
                                new int[] { glyphCode });
                        Rectangle isolatedBounds = isolated.getGlyphPixelBounds(
                                0, context, 0.0f, 0.0f);
                        placements.add(new VulkanGlyphPlacement(
                                key(fontRun.font, glyphCode),
                                bounds.x - isolatedBounds.x,
                                lineIndex * lineHeight - baselineTail
                                        + bounds.y - isolatedBounds.y));
                    }
                    penX += (float) run.getGlyphPosition(run.getNumGlyphs()).getX();
                    width = Math.max(width, (int) Math.ceil(penX));
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

    synchronized boolean usesMissingGlyph(long glyphKey) {
        GlyphKey glyph = glyphs.get(Long.valueOf(glyphKey));
        if (glyph == null) throw new IllegalArgumentException("unknown glyph key " + glyphKey);
        return glyph.glyphCode == glyph.font.getMissingGlyphCode();
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
        systemFallbackCache.clear();
        missingFallbackCache.clear();
    }

    private List<FontRun> fontRuns(String text, Font primary, Font legacy,
                                   int size, boolean bold, boolean preferLegacy) {
        ArrayList<FontRun> runs = new ArrayList<FontRun>();
        int offset = 0;
        while (offset < text.length()) {
            int end = nextClusterEnd(text, offset);
            String cluster = text.substring(offset, end);
            Font font = selectFont(cluster, primary, legacy, size, bold, preferLegacy);
            if (!runs.isEmpty() && runs.get(runs.size() - 1).font.equals(font)) {
                runs.get(runs.size() - 1).text += cluster;
            } else {
                runs.add(new FontRun(font, cluster));
            }
            offset = end;
        }
        return runs;
    }

    private Font selectFont(String cluster, Font primary, Font legacy,
                            int size, boolean bold, boolean preferLegacy) {
        Font first = preferLegacy ? legacy : primary;
        Font second = preferLegacy ? primary : legacy;
        if (canDisplay(first, cluster)) return first;
        if (canDisplay(second, cluster)) return second;
        Font system = systemFallback(cluster);
        if (system == null) return first;
        int style = bold && system.getFamily().indexOf("Emoji") < 0
                ? Font.BOLD : Font.PLAIN;
        return slickFont(system.deriveFont(style, (float) size));
    }

    private Font systemFallback(String cluster) {
        Font cached = systemFallbackCache.get(cluster);
        if (cached != null) return cached;
        if (missingFallbackCache.contains(cluster)) return null;
        for (Font font : systemFallbackFonts) {
            if (canDisplay(font, cluster)) {
                systemFallbackCache.put(cluster, font);
                return font;
            }
        }
        missingFallbackCache.add(cluster);
        return null;
    }

    private static boolean canDisplay(Font font, String text) {
        return font.canDisplayUpTo(text) < 0;
    }

    private static boolean containsNonAscii(String text) {
        for (int index = 0; index < text.length(); index++) {
            if (text.charAt(index) > 127) return true;
        }
        return false;
    }

    private static int nextClusterEnd(String text, int start) {
        int first = text.codePointAt(start);
        int offset = start + Character.charCount(first);
        if (isRegionalIndicator(first) && offset < text.length()) {
            int second = text.codePointAt(offset);
            if (isRegionalIndicator(second)) offset += Character.charCount(second);
        }
        while (offset < text.length()) {
            int codePoint = text.codePointAt(offset);
            if (isClusterExtension(codePoint)) {
                offset += Character.charCount(codePoint);
                continue;
            }
            if (codePoint == 0x200d) {
                offset += Character.charCount(codePoint);
                if (offset < text.length()) {
                    int joined = text.codePointAt(offset);
                    offset += Character.charCount(joined);
                }
                continue;
            }
            break;
        }
        return offset;
    }

    private static boolean isClusterExtension(int codePoint) {
        int type = Character.getType(codePoint);
        return type == Character.NON_SPACING_MARK
                || type == Character.COMBINING_SPACING_MARK
                || type == Character.ENCLOSING_MARK
                || codePoint == 0xfe0e || codePoint == 0xfe0f
                || (codePoint >= 0x1f3fb && codePoint <= 0x1f3ff)
                || (codePoint >= 0xe0020 && codePoint <= 0xe007f);
    }

    private static boolean isRegionalIndicator(int codePoint) {
        return codePoint >= 0x1f1e6 && codePoint <= 0x1f1ff;
    }

    private static int spaceWidth(Font font, FontRenderContext context) {
        GlyphVector space = font.layoutGlyphVector(context, new char[] {' '}, 0, 1,
                Font.LAYOUT_LEFT_TO_RIGHT);
        return space.getGlyphLogicalBounds(0).getBounds().width;
    }

    private static List<Font> discoverSystemFallbackFonts() {
        LinkedHashMap<String, Font> fonts = new LinkedHashMap<String, Font>();
        Set<String> families = new HashSet<String>();
        GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        for (String family : environment.getAvailableFontFamilyNames()) {
            families.add(family.toLowerCase(java.util.Locale.ROOT));
        }
        String[] preferred = {
                "Segoe UI Emoji", "Segoe UI Symbol", "Microsoft YaHei UI",
                "Microsoft YaHei", "Noto Color Emoji", "Noto Sans CJK SC",
                "Noto Sans", "Apple Color Emoji", "Arial Unicode MS",
                "Nirmala UI", "Malgun Gothic", "SimSun"
        };
        for (String name : preferred) {
            if (families.contains(name.toLowerCase(java.util.Locale.ROOT))) {
                addFallback(fonts, new Font(name, Font.PLAIN, 1));
            }
        }
        for (Font font : environment.getAllFonts()) addFallback(fonts, font);
        addFallback(fonts, new Font(Font.SANS_SERIF, Font.PLAIN, 1));
        return new ArrayList<Font>(fonts.values());
    }

    private static void addFallback(Map<String, Font> fonts, Font font) {
        String key = font.getFontName(java.util.Locale.ROOT).toLowerCase(java.util.Locale.ROOT);
        if (!fonts.containsKey(key)) fonts.put(key, font);
    }

    private static Font slickFont(Font source) {
        Map<TextAttribute, Object> attributes =
                new HashMap<TextAttribute, Object>(source.getAttributes());
        attributes.put(TextAttribute.KERNING, TextAttribute.KERNING_ON);
        return source.deriveFont(attributes);
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
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_ON);
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

    private static final class FontRun {
        private final Font font;
        private String text;

        private FontRun(Font font, String text) {
            this.font = font;
            this.text = text;
        }
    }
}
