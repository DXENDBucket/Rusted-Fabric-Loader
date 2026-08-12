package io.github.endx.vulkanmod.spi;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.RandomAccess;

/** Binding-neutral commands for one frame. Platform drivers translate these into native calls. */
public final class VulkanFrameCommands {
    public static final int COLORED_QUAD = 1;
    public static final int TEXTURED_QUAD = 2;
    public static final int COLORED_TRIANGLE = 3;
    public static final int TEXTURED_TRIANGLE = 4;
    public static final int COLORED_LINE = 5;
    public static final int COLORED_CIRCLE = 6;

    private final int width;
    private final int height;
    private final float clearRed;
    private final float clearGreen;
    private final float clearBlue;
    private final float clearAlpha;
    private final boolean clearRequested;
    private final VulkanDrawCommand[] commandArray;
    private final int commandCount;
    private final int coloredQuadCount;
    private final int texturedQuadCount;
    private final int coloredTriangleCount;
    private final int texturedTriangleCount;
    private final VulkanFrameCommandPool pool;
    private final CommandArena arena;
    private List<VulkanDrawCommand> commands;
    private List<VulkanColoredQuad> coloredQuads;
    private List<VulkanTexturedQuad> texturedQuads;
    private List<VulkanColoredTriangle> coloredTriangles;
    private List<VulkanTexturedTriangle> texturedTriangles;
    private boolean released;

    private VulkanFrameCommands(Builder builder) {
        width = builder.width;
        height = builder.height;
        clearRed = builder.clearRed;
        clearGreen = builder.clearGreen;
        clearBlue = builder.clearBlue;
        clearAlpha = builder.clearAlpha;
        clearRequested = builder.clearRequested;
        commandCount = builder.arena.size;
        coloredQuadCount = builder.coloredQuadCount;
        texturedQuadCount = builder.texturedQuadCount;
        coloredTriangleCount = builder.coloredTriangleCount;
        texturedTriangleCount = builder.texturedTriangleCount;
        pool = builder.pool;
        if (pool == null) {
            commandArray = Arrays.copyOf(builder.arena.commands, commandCount);
            arena = null;
        } else {
            commandArray = builder.arena.commands;
            arena = builder.arena;
        }
        builder.built = true;
    }

    public static Builder builder(int width, int height) {
        return new Builder(width, height, null);
    }

    /**
     * Acquires a thread-confined arena whose arrays and commands are recycled after submission.
     * Call {@link #releasePooledCommands()} after the platform driver returns.
     */
    public static Builder pooledBuilder(int width, int height) {
        VulkanFrameCommandPool pool = VulkanFrameCommandPool.current();
        return new Builder(width, height, pool);
    }

    public int width() { return width; }
    public int height() { return height; }
    public float clearRed() { return clearRed; }
    public float clearGreen() { return clearGreen; }
    public float clearBlue() { return clearBlue; }
    public float clearAlpha() { return clearAlpha; }
    public boolean clearRequested() { return clearRequested; }
    public int commandCount() { return commandCount; }
    public int coloredQuadCount() { return coloredQuadCount; }
    public int texturedQuadCount() { return texturedQuadCount; }
    public int coloredTriangleCount() { return coloredTriangleCount; }
    public int texturedTriangleCount() { return texturedTriangleCount; }

    public VulkanDrawCommand command(int index) {
        ensureReadable();
        if (index < 0 || index >= commandCount) throw new IndexOutOfBoundsException(index);
        return commandArray[index];
    }

    public int commandType(int index) {
        VulkanDrawCommand command = command(index);
        if (command instanceof VulkanColoredQuad) return COLORED_QUAD;
        if (command instanceof VulkanTexturedQuad) return TEXTURED_QUAD;
        if (command instanceof VulkanColoredTriangle) return COLORED_TRIANGLE;
        if (command instanceof VulkanTexturedTriangle) return TEXTURED_TRIANGLE;
        if (command instanceof VulkanColoredLine) return COLORED_LINE;
        if (command instanceof VulkanColoredCircle) return COLORED_CIRCLE;
        throw new IllegalArgumentException("unsupported draw command: "
                + command.getClass().getName());
    }

    public List<VulkanDrawCommand> commands() {
        ensureReadable();
        if (commands == null) commands = Collections.unmodifiableList(
                new CommandList(this));
        return commands;
    }

    public List<VulkanColoredQuad> coloredQuads() {
        ensureReadable();
        if (coloredQuads == null) coloredQuads = commandsOfType(
                VulkanColoredQuad.class, coloredQuadCount);
        return coloredQuads;
    }

    public List<VulkanTexturedQuad> texturedQuads() {
        ensureReadable();
        if (texturedQuads == null) texturedQuads = commandsOfType(
                VulkanTexturedQuad.class, texturedQuadCount);
        return texturedQuads;
    }

    public List<VulkanColoredTriangle> coloredTriangles() {
        ensureReadable();
        if (coloredTriangles == null) coloredTriangles = commandsOfType(
                VulkanColoredTriangle.class, coloredTriangleCount);
        return coloredTriangles;
    }

    public List<VulkanTexturedTriangle> texturedTriangles() {
        ensureReadable();
        if (texturedTriangles == null) texturedTriangles = commandsOfType(
                VulkanTexturedTriangle.class, texturedTriangleCount);
        return texturedTriangles;
    }

    private <T extends VulkanDrawCommand> List<T> commandsOfType(Class<T> type, int count) {
        ArrayList<T> result = new ArrayList<T>(count);
        for (int index = 0; index < commandCount; index++) {
            VulkanDrawCommand command = commandArray[index];
            if (type.isInstance(command)) result.add(type.cast(command));
        }
        return Collections.unmodifiableList(result);
    }

    /** Returns a pooled frame's retained storage after synchronous driver consumption. */
    public void releasePooledCommands() {
        if (pool == null || released) return;
        for (int index = 0; index < commandCount; index++) {
            VulkanDrawCommand command = commandArray[index];
            commandArray[index] = null;
            if (command instanceof VulkanColoredQuad) {
                ((VulkanColoredQuad) command).release(pool);
            } else if (command instanceof VulkanTexturedQuad) {
                ((VulkanTexturedQuad) command).release(pool);
            } else if (command instanceof VulkanColoredTriangle) {
                ((VulkanColoredTriangle) command).release(pool);
            } else if (command instanceof VulkanTexturedTriangle) {
                ((VulkanTexturedTriangle) command).release(pool);
            } else if (command instanceof VulkanColoredLine) {
                ((VulkanColoredLine) command).release(pool);
            } else if (command instanceof VulkanColoredCircle) {
                ((VulkanColoredCircle) command).release(pool);
            }
        }
        released = true;
        commands = null;
        coloredQuads = null;
        texturedQuads = null;
        coloredTriangles = null;
        texturedTriangles = null;
        pool.recycleArena(arena);
    }

    private void ensureReadable() {
        if (released) throw new IllegalStateException("pooled frame commands were released");
    }

    static final class CommandArena {
        VulkanDrawCommand[] commands = new VulkanDrawCommand[0];
        int size;

        void add(VulkanDrawCommand command) {
            if (size == commands.length) {
                commands = Arrays.copyOf(commands, size == 0 ? 16 : size * 2);
            }
            commands[size++] = command;
        }
    }

    private static final class CommandList
            extends AbstractList<VulkanDrawCommand> implements RandomAccess {
        private final VulkanFrameCommands frame;

        private CommandList(VulkanFrameCommands frame) {
            this.frame = frame;
        }

        @Override public VulkanDrawCommand get(int requested) {
            frame.ensureReadable();
            if (requested < 0 || requested >= frame.commandCount) {
                throw new IndexOutOfBoundsException(requested);
            }
            return frame.commandArray[requested];
        }

        @Override public int size() { return frame.commandCount; }
    }

    public static final class Builder {
        private final int width;
        private final int height;
        private final VulkanFrameCommandPool pool;
        private final CommandArena arena;
        private float clearRed;
        private float clearGreen;
        private float clearBlue;
        private float clearAlpha = 1.0f;
        private boolean clearRequested;
        private int coloredQuadCount;
        private int texturedQuadCount;
        private int coloredTriangleCount;
        private int texturedTriangleCount;
        private boolean built;

        private Builder(int width, int height, VulkanFrameCommandPool pool) {
            if (width <= 0 || height <= 0) {
                throw new IllegalArgumentException("frame dimensions must be positive");
            }
            this.width = width;
            this.height = height;
            this.pool = pool;
            this.arena = pool == null ? new CommandArena() : pool.acquireArena();
        }

        public Builder clear(float red, float green, float blue, float alpha) {
            ensureOpen();
            requireFinite(red);
            requireFinite(green);
            requireFinite(blue);
            requireFinite(alpha);
            clearRed = clamp(red);
            clearGreen = clamp(green);
            clearBlue = clamp(blue);
            clearAlpha = clamp(alpha);
            clearRequested = true;
            return this;
        }

        public Builder coloredQuad(VulkanColoredQuad quad) {
            ensureOpen();
            if (quad == null) throw new NullPointerException("quad");
            arena.add(quad);
            coloredQuadCount++;
            return this;
        }

        public Builder coloredQuad(float x, float y, float width, float height,
                                   float red, float green, float blue, float alpha,
                                   VulkanDrawState state) {
            return coloredQuad(pool == null
                    ? new VulkanColoredQuad(x, y, width, height,
                            red, green, blue, alpha, state)
                    : VulkanColoredQuad.acquire(pool, x, y, width, height,
                            red, green, blue, alpha, state));
        }

        public Builder texturedQuad(VulkanTexturedQuad quad) {
            ensureOpen();
            if (quad == null) throw new NullPointerException("quad");
            arena.add(quad);
            texturedQuadCount++;
            return this;
        }

        public Builder texturedQuad(long textureHandle,
                                    float x, float y, float width, float height,
                                    float u0, float v0, float u1, float v1,
                                    float red, float green, float blue, float alpha,
                                    VulkanDrawState state) {
            return texturedQuad(pool == null
                    ? new VulkanTexturedQuad(textureHandle, x, y, width, height,
                            u0, v0, u1, v1, red, green, blue, alpha, state)
                    : VulkanTexturedQuad.acquire(pool, textureHandle, x, y, width, height,
                            u0, v0, u1, v1, red, green, blue, alpha, state));
        }

        public Builder coloredTriangle(VulkanColoredTriangle triangle) {
            ensureOpen();
            if (triangle == null) throw new NullPointerException("triangle");
            arena.add(triangle);
            coloredTriangleCount++;
            return this;
        }

        public Builder coloredTriangle(float[] positions, float[] colors,
                                       VulkanDrawState state) {
            return coloredTriangle(pool == null
                    ? new VulkanColoredTriangle(positions, colors, state)
                    : VulkanColoredTriangle.acquire(pool, positions, colors, state));
        }

        public Builder coloredLine(float x1, float y1, float x2, float y2, float thickness,
                                   float red, float green, float blue, float alpha,
                                   VulkanDrawState state) {
            ensureOpen();
            arena.add(pool == null
                    ? new VulkanColoredLine(x1, y1, x2, y2, thickness,
                            red, green, blue, alpha, state)
                    : VulkanColoredLine.acquire(pool, x1, y1, x2, y2, thickness,
                            red, green, blue, alpha, state));
            return this;
        }

        public Builder coloredCircle(float x, float y, float radius, float thickness,
                                     float red, float green, float blue, float alpha,
                                     int segments, boolean filled, VulkanDrawState state) {
            ensureOpen();
            arena.add(pool == null
                    ? new VulkanColoredCircle(x, y, radius, thickness,
                            red, green, blue, alpha, segments, filled, state)
                    : VulkanColoredCircle.acquire(pool, x, y, radius, thickness,
                            red, green, blue, alpha, segments, filled, state));
            return this;
        }

        public Builder texturedTriangle(VulkanTexturedTriangle triangle) {
            ensureOpen();
            if (triangle == null) throw new NullPointerException("triangle");
            arena.add(triangle);
            texturedTriangleCount++;
            return this;
        }

        public Builder texturedTriangle(long textureHandle, float[] positions, float[] uvs,
                                        float[] colors, VulkanDrawState state) {
            return texturedTriangle(pool == null
                    ? new VulkanTexturedTriangle(textureHandle, positions, uvs, colors, state)
                    : VulkanTexturedTriangle.acquire(pool, textureHandle,
                            positions, uvs, colors, state));
        }

        public VulkanFrameCommands build() {
            ensureOpen();
            return new VulkanFrameCommands(this);
        }

        /** Releases an unfinished pooled builder after frame construction failed. */
        public void discard() {
            if (built) return;
            if (pool == null) {
                built = true;
                return;
            }
            VulkanFrameCommands temporary = new VulkanFrameCommands(this);
            temporary.releasePooledCommands();
        }

        private void ensureOpen() {
            if (built) throw new IllegalStateException("frame builder is already closed");
        }

        private static float clamp(float value) {
            return Math.max(0.0f, Math.min(1.0f, value));
        }

        private static void requireFinite(float value) {
            if (!Float.isFinite(value)) throw new IllegalArgumentException("clear must be finite");
        }
    }
}
