package io.github.endx.rustedfabricapi.api.audio;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import rustedwarfare.client.audio.SoundEngine;

/** Cancellable high-level sound playback callbacks. */
public final class SoundEvents {
    public static final RustedFabricEvent<BeforePlay> BEFORE_PLAY =
            RustedFabricEvent.create(listeners -> (engine, playback) -> {
                boolean cancelled = false;
                for (BeforePlay listener : listeners) {
                    cancelled |= listener.beforePlay(engine, playback);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterPlay> AFTER_PLAY =
            RustedFabricEvent.create(listeners -> (engine, playback) -> {
                for (AfterPlay listener : listeners) listener.afterPlay(engine, playback);
            });

    private SoundEvents() {
    }

    @FunctionalInterface
    public interface BeforePlay {
        /** Return true to suppress this sound. All registered listeners are still called. */
        boolean beforePlay(SoundEngine engine, SoundPlayback playback);
    }

    @FunctionalInterface
    public interface AfterPlay {
        void afterPlay(SoundEngine engine, SoundPlayback playback);
    }
}
