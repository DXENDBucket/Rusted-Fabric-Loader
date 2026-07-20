package io.github.endx.rustedfabricapi.api.client.warlog;

import io.github.endx.rustedfabricapi.api.client.RustedWarfareClient;
import rustedwarfare.core.GameEngine;
import rustedwarfare.ui.WarLogInterface;
import rustedwarfare.unit.Unit;

import java.util.Objects;

/** Convenience access to the local on-screen war/event log. */
public final class WarLog {
    private WarLog() {
    }

    public static WarLogInterface manager() {
        GameEngine engine = RustedWarfareClient.requireEngine();
        if (engine.gameUI == null || engine.gameUI.warLogInterface == null) {
            throw new IllegalStateException("War log is not initialized");
        }
        return engine.gameUI.warLogInterface;
    }

    public static void post(String text) {
        manager().addTextEntry(requireText(text));
    }

    /** Posts an entry forced visible for the requested duration. */
    public static void post(String text, int durationMillis) {
        if (durationMillis <= 0) throw new IllegalArgumentException("durationMillis must be positive");
        manager().addTimedTextEntry(requireText(text), durationMillis);
    }

    public static void unitCreated(Unit unit) {
        manager().addUnitCreated(Objects.requireNonNull(unit, "unit"));
    }

    public static void upgradeCompleted(Unit unit) {
        manager().addUpgradeCompleted(Objects.requireNonNull(unit, "unit"));
    }

    public static void unitDamaged(Unit unit) {
        manager().addUnitDamaged(Objects.requireNonNull(unit, "unit"));
    }

    public static void clear() {
        manager().clearEntries();
    }

    /** Centers the camera on the first unread positioned entry, matching the native hotkey. */
    public static void jumpToFirstUnread() {
        manager().jumpToFirstUnreadEntry();
    }

    private static String requireText(String text) {
        if (text == null) throw new NullPointerException("text");
        if (text.trim().isEmpty()) throw new IllegalArgumentException("War-log text must not be blank");
        return text;
    }
}
