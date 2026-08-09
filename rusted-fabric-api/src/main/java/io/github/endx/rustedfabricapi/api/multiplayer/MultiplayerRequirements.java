package io.github.endx.rustedfabricapi.api.multiplayer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/** Adds runtime-activated synchronized requirements to the otherwise static mod manifest. */
public final class MultiplayerRequirements {
    private static final Object LOCK = new Object();
    private static final Map<String, ActiveRequirement> ACTIVE =
            new LinkedHashMap<String, ActiveRequirement>();

    private MultiplayerRequirements() { }

    /**
     * Conservatively activates a requirement for the rest of this process.
     * Repeating the same requirement is harmless; conflicting declarations fail immediately.
     */
    public static Activation activate(MultiplayerMod requiredMod) {
        MultiplayerMod checked = Objects.requireNonNull(requiredMod, "requiredMod");
        if (checked.mode() != MultiplayerMod.Mode.REQUIRED) {
            throw new IllegalArgumentException("Dynamic multiplayer requirement must use REQUIRED mode");
        }
        synchronized (LOCK) {
            ActiveRequirement active = ACTIVE.get(checked.id());
            if (active == null) {
                active = new ActiveRequirement(checked);
                ACTIVE.put(checked.id(), active);
            } else {
                MultiplayerMod declaration = active.mod;
                if (!declaration.version().equals(checked.version())
                        || !declaration.protocol().equals(checked.protocol())
                        || !declaration.syncHash().equals(checked.syncHash())) {
                    throw new IllegalStateException("Conflicting dynamic multiplayer requirement for "
                            + checked.id());
                }
                active.references++;
            }
            return new Activation(active);
        }
    }

    public static boolean isActive(String modId) {
        synchronized (LOCK) { return ACTIVE.containsKey(modId); }
    }

    /** Returns a canonical manifest with active requirements replacing static optional rows. */
    public static MultiplayerManifest effective(MultiplayerManifest base) {
        Objects.requireNonNull(base, "base");
        synchronized (LOCK) {
            if (ACTIVE.isEmpty()) return base;
            Map<String, MultiplayerMod> merged = new LinkedHashMap<String, MultiplayerMod>();
            for (MultiplayerMod mod : base.mods()) merged.put(mod.id(), mod);
            for (ActiveRequirement required : ACTIVE.values()) {
                merged.put(required.mod.id(), required.mod);
            }
            return new MultiplayerManifest(base.platform(), new ArrayList<MultiplayerMod>(merged.values()));
        }
    }

    public static final class Activation implements AutoCloseable {
        private final ActiveRequirement requirement;
        private final AtomicBoolean closed = new AtomicBoolean();

        private Activation(ActiveRequirement requirement) { this.requirement = requirement; }

        public boolean deactivate() {
            if (!closed.compareAndSet(false, true)) return false;
            synchronized (LOCK) {
                requirement.references--;
                if (requirement.references == 0) {
                    ACTIVE.remove(requirement.mod.id(), requirement);
                }
            }
            return true;
        }

        @Override public void close() { deactivate(); }
    }

    private static final class ActiveRequirement {
        private final MultiplayerMod mod;
        private int references = 1;
        private ActiveRequirement(MultiplayerMod mod) { this.mod = mod; }
    }
}
