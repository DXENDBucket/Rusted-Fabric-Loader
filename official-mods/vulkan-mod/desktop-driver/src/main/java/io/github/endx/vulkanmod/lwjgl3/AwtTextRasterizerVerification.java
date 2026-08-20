package io.github.endx.vulkanmod.lwjgl3;

import io.github.endx.vulkanmod.spi.VulkanGlyphBitmap;
import io.github.endx.vulkanmod.spi.VulkanTextLayout;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/** Verifies the isolated desktop shaper retains Slick's integer-anchor glyph metrics. */
public final class AwtTextRasterizerVerification {
    private AwtTextRasterizerVerification() { }

    public static void main(String[] arguments) throws Exception {
        AwtTextRasterizer rasterizer = new AwtTextRasterizer();
        try {
            int size = 17;
            Font font = slickFont(sourceFont(rasterizer).deriveFont((float) size));
            BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            configure(graphics);
            graphics.setFont(font);
            try {
                FontMetrics metrics = graphics.getFontMetrics();
                FontRenderContext context = graphics.getFontRenderContext();
                String text = "AV";
                VulkanTextLayout layout = rasterizer.layout(text, size, false);
                GlyphVector run = font.layoutGlyphVector(context, text.toCharArray(),
                        0, text.length(), Font.LAYOUT_LEFT_TO_RIGHT);
                int expectedWidth = 1;
                int baselineTail = metrics.getDescent() + metrics.getLeading();
                for (int glyph = 0; glyph < run.getNumGlyphs(); glyph++) {
                    Rectangle bounds = run.getGlyphPixelBounds(glyph, context, 0, 0);
                    expectedWidth = Math.max(expectedWidth, bounds.x + bounds.width);
                    GlyphVector isolated = font.createGlyphVector(context,
                            new int[] {run.getGlyphCode(glyph)});
                    Rectangle isolatedBounds = isolated.getGlyphPixelBounds(0, context, 0, 0);
                    require(layout.glyph(glyph).x() == bounds.x - isolatedBounds.x,
                            "glyph x no longer matches Slick pixel bounds");
                    require(layout.glyph(glyph).y() == -baselineTail
                                    + bounds.y - isolatedBounds.y,
                            "glyph baseline no longer matches Slick drawString");
                }
                require(layout.width() == expectedWidth
                                && layout.lineHeight() == metrics.getHeight(),
                        "text measurement no longer matches Slick UnicodeFont");
                VulkanGlyphBitmap first = rasterizer.rasterizeGlyph(
                        layout.glyph(0).glyphKey());
                GlyphVector isolated = font.createGlyphVector(context,
                        new int[] {run.getGlyphCode(0)});
                Rectangle isolatedBounds = isolated.getGlyphPixelBounds(0, context, 0, 0);
                require(first.bearingX() == isolatedBounds.x
                                && first.bearingY() == isolatedBounds.y
                                && first.width() == isolatedBounds.width
                                && first.height() == isolatedBounds.height,
                        "atlas glyph raster and layout use different pixel bounds");

                VulkanTextLayout multiline = rasterizer.layout("A\nA", size, false);
                require(multiline.glyph(1).y() - multiline.glyph(0).y()
                                == multiline.lineHeight(),
                        "multiline baseline advance changed");

                verifyUnicodeFallback(rasterizer);
            } finally {
                graphics.dispose();
            }
        } finally {
            rasterizer.close();
        }
        System.out.println("Desktop Slick-compatible text metrics passed");
    }

    private static void verifyUnicodeFallback(AwtTextRasterizer rasterizer) {
        // U+1FAE0 is deliberately outside the old DroidSansFallback coverage. On current
        // Windows it resolves to Segoe UI Emoji and also verifies that a surrogate pair is
        // treated as one code point rather than two replacement characters.
        String emoji = new String(Character.toChars(0x1fae0));
        VulkanTextLayout emojiLayout = rasterizer.layout(emoji, 28, false);
        require(emojiLayout.glyphCount() == 1,
                "supplementary emoji was split into UTF-16 surrogate glyphs");
        require(!rasterizer.usesMissingGlyph(emojiLayout.glyph(0).glyphKey()),
                "installed system emoji fallback was not selected");
        VulkanGlyphBitmap emojiBitmap = rasterizer.rasterizeGlyph(
                emojiLayout.glyph(0).glyphKey());
        require(!emojiBitmap.empty(), "emoji fallback rasterized an empty glyph");

        String extensionB = new String(Character.toChars(0x20000));
        VulkanTextLayout mixed = rasterizer.layout(
                "A汉龘" + extensionB + emoji + "Z", 24, false);
        require(mixed.glyphCount() >= 6, "mixed fallback run dropped characters");
        for (int glyph = 0; glyph < mixed.glyphCount(); glyph++) {
            require(!rasterizer.usesMissingGlyph(mixed.glyph(glyph).glyphKey()),
                    "mixed Latin/CJK/emoji text retained a missing glyph");
        }

        String searchUnits = "\u641c\u7d22\u5355\u4f4d \u5efa\u9020\u8005 "
                + "\u91cd\u578b\u62e6\u622a\u673a \u5b9e\u9a8c\u6218\u6597\u8718\u86db "
                + "\u8150\u8680 \u751f\u7269\u8d28";
        VulkanTextLayout searchLayout = rasterizer.layout(searchUnits, 18, false);
        require(searchLayout.glyphCount()
                        >= searchUnits.codePointCount(0, searchUnits.length()),
                "sandbox search-unit text dropped CJK glyphs");
        for (int glyph = 0; glyph < searchLayout.glyphCount(); glyph++) {
            require(!rasterizer.usesMissingGlyph(searchLayout.glyph(glyph).glyphKey()),
                    "sandbox search-unit text retained a missing CJK glyph");
        }
    }

    private static Font sourceFont(AwtTextRasterizer rasterizer) throws Exception {
        Field field = AwtTextRasterizer.class.getDeclaredField("regularFont");
        field.setAccessible(true);
        return (Font) field.get(rasterizer);
    }

    private static Font slickFont(Font source) {
        Map<TextAttribute, Object> attributes =
                new HashMap<TextAttribute, Object>(source.getAttributes());
        attributes.put(TextAttribute.KERNING, TextAttribute.KERNING_ON);
        return source.deriveFont(attributes);
    }

    private static void configure(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
