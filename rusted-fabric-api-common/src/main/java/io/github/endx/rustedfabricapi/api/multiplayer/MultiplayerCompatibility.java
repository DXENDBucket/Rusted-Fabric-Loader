package io.github.endx.rustedfabricapi.api.multiplayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Deterministic compatibility decision shared by Windows and Android. */
public final class MultiplayerCompatibility {
    private MultiplayerCompatibility() {
    }

    public enum Problem {
        LOCAL_UNSAFE,
        REMOTE_UNSAFE,
        REQUIRED_MOD_MISSING_LOCALLY,
        REQUIRED_MOD_MISSING_REMOTELY,
        MODE_MISMATCH,
        VERSION_MISMATCH,
        PROTOCOL_MISMATCH,
        SYNC_HASH_MISMATCH
    }

    public static Report evaluate(MultiplayerManifest local, MultiplayerManifest remote) {
        if (local == null || remote == null) throw new NullPointerException("manifest");
        List<Issue> issues = new ArrayList<>();
        Set<String> ids = new TreeSet<>();
        for (MultiplayerMod mod : local.mods()) ids.add(mod.id());
        for (MultiplayerMod mod : remote.mods()) ids.add(mod.id());
        for (String id : ids) compare(local.find(id), remote.find(id), issues);
        Report report = new Report(local, remote, issues);
        io.github.endx.rustedfabricapi.api.event.MultiplayerCompatibilityEvents
                .COMPATIBILITY_EVALUATED.dispatch(report);
        return report;
    }

    public static Report evaluateVanillaPeer(MultiplayerManifest local) {
        return evaluate(local, MultiplayerManifest.empty("vanilla"));
    }

    private static void compare(MultiplayerMod local, MultiplayerMod remote, List<Issue> issues) {
        String id = local != null ? local.id() : remote.id();
        if (local != null && local.mode() == MultiplayerMod.Mode.UNSAFE) {
            issues.add(new Issue(Problem.LOCAL_UNSAFE, id));
        }
        if (remote != null && remote.mode() == MultiplayerMod.Mode.UNSAFE) {
            issues.add(new Issue(Problem.REMOTE_UNSAFE, id));
        }
        if (local == null) {
            if (remote.mode() == MultiplayerMod.Mode.REQUIRED) {
                issues.add(new Issue(Problem.REQUIRED_MOD_MISSING_LOCALLY, id));
            }
            return;
        }
        if (remote == null) {
            if (local.mode() == MultiplayerMod.Mode.REQUIRED) {
                issues.add(new Issue(Problem.REQUIRED_MOD_MISSING_REMOTELY, id));
            }
            return;
        }
        if (local.mode() != remote.mode()) {
            issues.add(new Issue(Problem.MODE_MISMATCH, id));
            return;
        }
        if (local.mode() != MultiplayerMod.Mode.REQUIRED) return;
        if (!local.version().equals(remote.version())) {
            issues.add(new Issue(Problem.VERSION_MISMATCH, id));
        }
        if (!local.protocol().equals(remote.protocol())) {
            issues.add(new Issue(Problem.PROTOCOL_MISMATCH, id));
        }
        if (!local.syncHash().equals(remote.syncHash())) {
            issues.add(new Issue(Problem.SYNC_HASH_MISMATCH, id));
        }
    }

    public static final class Issue {
        private final Problem problem;
        private final String modId;

        private Issue(Problem problem, String modId) {
            this.problem = problem;
            this.modId = modId;
        }

        public Problem problem() { return problem; }
        public String modId() { return modId; }
    }

    public static final class Report {
        private final MultiplayerManifest local;
        private final MultiplayerManifest remote;
        private final List<Issue> issues;

        private Report(MultiplayerManifest local, MultiplayerManifest remote, List<Issue> issues) {
            this.local = local;
            this.remote = remote;
            this.issues = Collections.unmodifiableList(new ArrayList<>(issues));
        }

        public boolean compatible() { return issues.isEmpty(); }
        public MultiplayerManifest local() { return local; }
        public MultiplayerManifest remote() { return remote; }
        public List<Issue> issues() { return issues; }
    }
}
