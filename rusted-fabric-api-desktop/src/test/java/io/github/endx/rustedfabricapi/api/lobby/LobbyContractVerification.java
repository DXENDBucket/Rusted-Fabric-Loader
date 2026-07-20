package io.github.endx.rustedfabricapi.api.lobby;

import java.util.concurrent.atomic.AtomicInteger;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import rustedwarfare.network.GameSetup;

/** Dependency-free value, validation, and cancellation checks for the lobby API. */
public final class LobbyContractVerification {
    private LobbyContractVerification() {
    }

    public static void verify() {
        verifyEditorPreservesUnknownNativeValues();
        verifyEditorValidation();
        verifyCancellationAggregation();
    }

    private static void verifyEditorPreservesUnknownNativeValues() {
        GameSetup source = new GameSetup();
        source.aiDifficulty = 41;
        source.fogMode = 42;
        source.startingCredits = 43;
        GameSetupEditor editor = new GameSetupEditor(source);
        editor.noNukes(true).sharedControl(true).teamsLocked(true)
                .incomeMultiplier(2.5F).randomSeed(12345);
        GameSetup target = source.copy();
        editor.applyTo(target);
        require(target.aiDifficulty == 41 && target.fogMode == 42
                        && target.startingCredits == 43,
                "editing one lobby field destroyed unknown native values");
        require(target.noNukes && target.sharedControl && target.teamsLocked,
                "lobby boolean edits were not applied");
        require(target.incomeMultiplier == 2.5F && target.randomSeed == 12345,
                "lobby numeric edits were not applied");

        editor.aiDifficulty(LobbyAiDifficulty.IMPOSSIBLE)
                .fogMode(LobbyFogMode.LINE_OF_SIGHT)
                .startingCredits(StartingCreditsPreset.HIGH_50000);
        editor.applyTo(target);
        require(target.aiDifficulty == 3 && target.fogMode == 2
                        && target.startingCredits == 6,
                "typed lobby enum values used incorrect native IDs");
    }

    private static void verifyEditorValidation() {
        GameSetupEditor editor = new GameSetupEditor(new GameSetup());
        try {
            editor.map(LobbyMapType.CUSTOM, "bad\nmap");
            throw new AssertionError("control character in lobby map path was accepted");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
        try {
            editor.incomeMultiplier(Float.NaN);
            throw new AssertionError("non-finite lobby income multiplier was accepted");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
        try {
            editor.fogMode(LobbyFogMode.UNKNOWN);
            throw new AssertionError("read-only UNKNOWN lobby enum was accepted for writing");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }

    private static void verifyCancellationAggregation() {
        AtomicInteger calls = new AtomicInteger();
        RustedFabricEvent.Registration first = LobbyGameSetupEvents.BEFORE_UPDATE.subscribe(
                (network, current, requested) -> {
                    calls.incrementAndGet();
                    return false;
                });
        RustedFabricEvent.Registration second = LobbyGameSetupEvents.BEFORE_UPDATE.subscribe(
                (network, current, requested) -> {
                    calls.incrementAndGet();
                    return true;
                });
        require(LobbyGameSetupEvents.BEFORE_UPDATE.invoker()
                        .beforeUpdate(null, null, null),
                "lobby setup cancellation was not aggregated");
        require(calls.get() == 2, "lobby setup cancellation skipped a listener");
        first.close();
        second.close();

        calls.set(0);
        RustedFabricEvent.Registration pauseFirst = LobbyPlayerEvents.BEFORE_PAUSE_CHANGE.subscribe(
                (network, paused) -> {
                    calls.incrementAndGet();
                    return true;
                });
        RustedFabricEvent.Registration pauseSecond = LobbyPlayerEvents.BEFORE_PAUSE_CHANGE.subscribe(
                (network, paused) -> {
                    calls.incrementAndGet();
                    return false;
                });
        require(LobbyPlayerEvents.BEFORE_PAUSE_CHANGE.invoker().before(null, true),
                "lobby pause cancellation was not aggregated");
        require(calls.get() == 2, "lobby pause cancellation skipped a listener");
        pauseFirst.close();
        pauseSecond.close();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
