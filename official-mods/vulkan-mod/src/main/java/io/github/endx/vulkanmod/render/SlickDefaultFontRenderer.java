package io.github.endx.vulkanmod.render;

import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import rustedwarfare.client.render.GameImage;
import rustedwarfare.render.GraphicsEngine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Draws Slick2D's bundled AngelCode default font without creating an OpenGL context. */
public final class SlickDefaultFontRenderer {
    private static final String ROOT = "org/newdawn/slick/data/";
    private static final Pattern VALUE = Pattern.compile("([a-zA-Z]+)=(-?\\d+)");
    private final GraphicsEngine graphics;
    private final GameImage atlas;
    private final Glyph[] glyphs = new Glyph[256];
    private int lineHeight = 19;

    public SlickDefaultFontRenderer(GraphicsEngine graphics) {
        if (graphics == null) throw new NullPointerException("graphics");
        this.graphics = graphics;
        try (InputStream metrics = open(ROOT + "defaultfont.fnt");
             InputStream image = open(ROOT + "defaultfont.png")) {
            if (metrics == null || image == null) {
                throw new IOException("Slick default-font resources are missing");
            }
            readMetrics(metrics);
            atlas = graphics.loadImageFromStream(image, true);
            atlas.setName(ROOT + "defaultfont.png");
        } catch (IOException failure) {
            throw new IllegalStateException("Could not load Slick's original default font",
                    failure);
        }
    }

    public int width(String text) {
        if (text == null || text.isEmpty()) return 0;
        int cursor = 0;
        int widest = 0;
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (character == '\n') {
                widest = Math.max(widest, cursor);
                cursor = 0;
                continue;
            }
            Glyph glyph = glyph(character);
            if (glyph == null) continue;
            boolean last = index == text.length() - 1 || text.charAt(index + 1) == '\n';
            widest = Math.max(widest, cursor + (last ? glyph.width : glyph.advance));
            cursor += glyph.advance;
        }
        return widest;
    }

    public void draw(String text, float x, float y, Paint paint) {
        if (text == null || text.isEmpty()) return;
        float startX = x;
        float cursorX = x;
        float cursorY = y;
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (character == '\n') {
                cursorX = startX;
                cursorY += lineHeight;
                continue;
            }
            Glyph glyph = glyph(character);
            if (glyph == null) continue;
            if (glyph.width > 0 && glyph.height > 0) {
                Rect source = new Rect(glyph.x, glyph.y,
                        glyph.x + glyph.width, glyph.y + glyph.height);
                float left = cursorX + glyph.xOffset;
                float top = cursorY + glyph.yOffset;
                RectF destination = new RectF(left, top,
                        left + glyph.width, top + glyph.height);
                graphics.drawImage(atlas, source, destination, paint);
            }
            cursorX += glyph.advance;
        }
    }

    private Glyph glyph(char character) {
        if (character < glyphs.length && glyphs[character] != null) {
            return glyphs[character];
        }
        return '?' < glyphs.length ? glyphs['?'] : null;
    }

    private void readMetrics(InputStream input) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("common ")) {
                    int parsed = value(line, "lineHeight", lineHeight);
                    lineHeight = Math.max(1, parsed);
                } else if (line.startsWith("char id=")) {
                    int id = value(line, "id", -1);
                    if (id < 0 || id >= glyphs.length) continue;
                    glyphs[id] = new Glyph(
                            value(line, "x", 0), value(line, "y", 0),
                            value(line, "width", 0), value(line, "height", 0),
                            value(line, "xoffset", 0), value(line, "yoffset", 0),
                            value(line, "xadvance", 0));
                }
            }
        }
    }

    private static int value(String line, String name, int fallback) {
        Matcher matcher = VALUE.matcher(line);
        while (matcher.find()) {
            if (name.equals(matcher.group(1))) return Integer.parseInt(matcher.group(2));
        }
        return fallback;
    }

    private static InputStream open(String resource) {
        ClassLoader context = Thread.currentThread().getContextClassLoader();
        InputStream input = context == null ? null : context.getResourceAsStream(resource);
        if (input == null) {
            input = SlickDefaultFontRenderer.class.getClassLoader()
                    .getResourceAsStream(resource);
        }
        return input;
    }

    private static final class Glyph {
        private final int x;
        private final int y;
        private final int width;
        private final int height;
        private final int xOffset;
        private final int yOffset;
        private final int advance;

        private Glyph(int x, int y, int width, int height,
                      int xOffset, int yOffset, int advance) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.xOffset = xOffset;
            this.yOffset = yOffset;
            this.advance = advance;
        }
    }
}
