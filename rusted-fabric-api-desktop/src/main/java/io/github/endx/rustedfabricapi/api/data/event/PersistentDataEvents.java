package io.github.endx.rustedfabricapi.api.data.event;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import io.github.endx.rustedfabricapi.api.util.Identifier;
import rustedwarfare.unit.Unit;

/** Persistent extension-block lifecycle and recoverable codec failures. */
public final class PersistentDataEvents {
    public static final RustedFabricEvent<BeforeWrite> BEFORE_WRITE =
            RustedFabricEvent.create(listeners -> () -> {
                for (BeforeWrite listener : listeners) listener.beforeWrite();
            });
    public static final RustedFabricEvent<AfterRead> AFTER_READ =
            RustedFabricEvent.create(listeners -> (formatVersion, entries) -> {
                for (AfterRead listener : listeners) listener.afterRead(formatVersion, entries);
            });
    public static final RustedFabricEvent<CodecFailure> CODEC_FAILURE =
            RustedFabricEvent.create(listeners -> (id, unit, writing, failure) -> {
                for (CodecFailure listener : listeners) {
                    listener.onCodecFailure(id, unit, writing, failure);
                }
            });
    public static final RustedFabricEvent<MalformedBlock> MALFORMED_BLOCK =
            RustedFabricEvent.create(listeners -> failure -> {
                for (MalformedBlock listener : listeners) listener.onMalformedBlock(failure);
            });

    private PersistentDataEvents() {
    }

    @FunctionalInterface
    public interface BeforeWrite {
        void beforeWrite();
    }

    @FunctionalInterface
    public interface AfterRead {
        void afterRead(int formatVersion, int entryCount);
    }

    @FunctionalInterface
    public interface CodecFailure {
        void onCodecFailure(Identifier id, Unit unit, boolean writing, RuntimeException failure);
    }

    @FunctionalInterface
    public interface MalformedBlock {
        void onMalformedBlock(RuntimeException failure);
    }
}
