package io.github.endx.vulkanmod.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Binding-neutral commands for one frame. Platform drivers translate these into native calls. */
public final class VulkanFrameCommands {
    private final int width;
    private final int height;
    private final float clearRed;
    private final float clearGreen;
    private final float clearBlue;
    private final float clearAlpha;
    private final List<VulkanColoredQuad> coloredQuads;
    private final List<VulkanTexturedQuad> texturedQuads;
    private final List<VulkanColoredTriangle> coloredTriangles;
    private final List<VulkanTexturedTriangle> texturedTriangles;
    private final List<VulkanDrawCommand> commands;

    private VulkanFrameCommands(Builder builder) {
        width = builder.width;
        height = builder.height;
        clearRed = builder.clearRed;
        clearGreen = builder.clearGreen;
        clearBlue = builder.clearBlue;
        clearAlpha = builder.clearAlpha;
        coloredQuads = Collections.unmodifiableList(
                new ArrayList<VulkanColoredQuad>(builder.coloredQuads));
        texturedQuads = Collections.unmodifiableList(
                new ArrayList<VulkanTexturedQuad>(builder.texturedQuads));
        coloredTriangles = Collections.unmodifiableList(
                new ArrayList<VulkanColoredTriangle>(builder.coloredTriangles));
        texturedTriangles = Collections.unmodifiableList(
                new ArrayList<VulkanTexturedTriangle>(builder.texturedTriangles));
        commands = Collections.unmodifiableList(
                new ArrayList<VulkanDrawCommand>(builder.commands));
    }

    public static Builder builder(int width, int height) {
        return new Builder(width, height);
    }

    public int width() { return width; }
    public int height() { return height; }
    public float clearRed() { return clearRed; }
    public float clearGreen() { return clearGreen; }
    public float clearBlue() { return clearBlue; }
    public float clearAlpha() { return clearAlpha; }
    public List<VulkanColoredQuad> coloredQuads() { return coloredQuads; }
    public List<VulkanTexturedQuad> texturedQuads() { return texturedQuads; }
    public List<VulkanColoredTriangle> coloredTriangles() { return coloredTriangles; }
    public List<VulkanTexturedTriangle> texturedTriangles() { return texturedTriangles; }
    public List<VulkanDrawCommand> commands() { return commands; }

    public static final class Builder {
        private final int width;
        private final int height;
        private float clearRed;
        private float clearGreen;
        private float clearBlue;
        private float clearAlpha = 1.0f;
        private final List<VulkanColoredQuad> coloredQuads =
                new ArrayList<VulkanColoredQuad>();
        private final List<VulkanTexturedQuad> texturedQuads =
                new ArrayList<VulkanTexturedQuad>();
        private final List<VulkanColoredTriangle> coloredTriangles =
                new ArrayList<VulkanColoredTriangle>();
        private final List<VulkanTexturedTriangle> texturedTriangles =
                new ArrayList<VulkanTexturedTriangle>();
        private final List<VulkanDrawCommand> commands =
                new ArrayList<VulkanDrawCommand>();

        private Builder(int width, int height) {
            if (width <= 0 || height <= 0) {
                throw new IllegalArgumentException("frame dimensions must be positive");
            }
            this.width = width;
            this.height = height;
        }

        public Builder clear(float red, float green, float blue, float alpha) {
            VulkanColoredQuad color = new VulkanColoredQuad(
                    0.0f, 0.0f, 0.0f, 0.0f, red, green, blue, alpha);
            clearRed = color.red();
            clearGreen = color.green();
            clearBlue = color.blue();
            clearAlpha = color.alpha();
            return this;
        }

        public Builder coloredQuad(VulkanColoredQuad quad) {
            if (quad == null) throw new NullPointerException("quad");
            coloredQuads.add(quad);
            commands.add(quad);
            return this;
        }

        public Builder texturedQuad(VulkanTexturedQuad quad) {
            if (quad == null) throw new NullPointerException("quad");
            texturedQuads.add(quad);
            commands.add(quad);
            return this;
        }

        public Builder coloredTriangle(VulkanColoredTriangle triangle) {
            if (triangle == null) throw new NullPointerException("triangle");
            coloredTriangles.add(triangle);
            commands.add(triangle);
            return this;
        }

        public Builder texturedTriangle(VulkanTexturedTriangle triangle) {
            if (triangle == null) throw new NullPointerException("triangle");
            texturedTriangles.add(triangle);
            commands.add(triangle);
            return this;
        }

        public VulkanFrameCommands build() {
            return new VulkanFrameCommands(this);
        }
    }
}
