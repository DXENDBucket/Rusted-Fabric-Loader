package io.github.endx.rustedfabricapi.api.ini.action;

/** Selects visible custom actions, hidden custom actions, or both. */
public enum IniActionSectionScope {
    ACTION,
    HIDDEN_ACTION,
    ACTION_AND_HIDDEN;

    public boolean accepts(boolean hiddenAction) {
        return this == ACTION_AND_HIDDEN
                || (hiddenAction && this == HIDDEN_ACTION)
                || (!hiddenAction && this == ACTION);
    }

    public boolean overlaps(IniActionSectionScope other) {
        if (other == null) return false;
        return accepts(false) && other.accepts(false)
                || accepts(true) && other.accepts(true);
    }
}
