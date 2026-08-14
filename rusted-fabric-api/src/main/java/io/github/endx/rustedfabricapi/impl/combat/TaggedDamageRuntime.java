package io.github.endx.rustedfabricapi.impl.combat;

import rustedwarfare.custom.CustomTagList;
import rustedwarfare.unit.Unit;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.function.Supplier;

/** Synchronous metadata for direct damage that must not manufacture a game projectile. */
public final class TaggedDamageRuntime {
    private static final ThreadLocal<Deque<Frame>> ACTIVE = new ThreadLocal<>();

    private TaggedDamageRuntime() { }

    public static float apply(Unit target, CustomTagList tags, Supplier<Float> operation) {
        Frame frame = new Frame(Objects.requireNonNull(target, "target"), tags);
        Deque<Frame> frames = ACTIVE.get();
        if (frames == null) {
            frames = new ArrayDeque<>();
            ACTIVE.set(frames);
        }
        frames.push(frame);
        try {
            Float result = Objects.requireNonNull(operation, "operation").get();
            return result != null ? result.floatValue() : 0.0F;
        } finally {
            frames.remove(frame);
            if (frames.isEmpty()) ACTIVE.remove();
        }
    }

    public static CustomTagList tags(Unit target) {
        Deque<Frame> frames = ACTIVE.get();
        if (frames == null) return null;
        for (Frame frame : frames) {
            if (frame.target == target) return frame.tags;
        }
        return null;
    }

    private static final class Frame {
        private final Unit target;
        private final CustomTagList tags;

        private Frame(Unit target, CustomTagList tags) {
            this.target = target;
            this.tags = tags;
        }
    }
}
