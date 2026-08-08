package io.github.endx.rustedfabricapi.api.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import io.github.endx.rustedfabricapi.api.util.Identifier;

/** Directional comparison of a local registry snapshot against a remote snapshot. */
public final class RegistryComparison {
    public enum Status {
        EXACT_LAYOUT,
        SAME_ENTRIES_DIFFERENT_ORDER,
        DIFFERENT_ENTRIES,
        DIFFERENT_REGISTRY
    }

    private final RegistrySnapshot local;
    private final RegistrySnapshot remote;
    private final Status status;
    private final List<Identifier> missingLocally;
    private final List<Identifier> missingRemotely;

    private RegistryComparison(RegistrySnapshot local, RegistrySnapshot remote, Status status,
            List<Identifier> missingLocally, List<Identifier> missingRemotely) {
        this.local = local;
        this.remote = remote;
        this.status = status;
        this.missingLocally = Collections.unmodifiableList(missingLocally);
        this.missingRemotely = Collections.unmodifiableList(missingRemotely);
    }

    static RegistryComparison compare(RegistrySnapshot local, RegistrySnapshot remote) {
        RegistrySnapshot checkedLocal = Objects.requireNonNull(local, "local");
        RegistrySnapshot checkedRemote = Objects.requireNonNull(remote, "remote");
        boolean sameRegistry = checkedLocal.registryId().equals(checkedRemote.registryId())
                && checkedLocal.valueTypeName().equals(checkedRemote.valueTypeName());
        Set<Identifier> localIds = new HashSet<Identifier>(checkedLocal.ids());
        Set<Identifier> remoteIds = new HashSet<Identifier>(checkedRemote.ids());
        ArrayList<Identifier> missingLocally = difference(checkedRemote.ids(), localIds);
        ArrayList<Identifier> missingRemotely = difference(checkedLocal.ids(), remoteIds);
        Status status;
        if (!sameRegistry) {
            status = Status.DIFFERENT_REGISTRY;
        } else if (!missingLocally.isEmpty() || !missingRemotely.isEmpty()) {
            status = Status.DIFFERENT_ENTRIES;
        } else if (checkedLocal.layoutFingerprint().equals(checkedRemote.layoutFingerprint())) {
            status = Status.EXACT_LAYOUT;
        } else {
            status = Status.SAME_ENTRIES_DIFFERENT_ORDER;
        }
        return new RegistryComparison(checkedLocal, checkedRemote, status,
                missingLocally, missingRemotely);
    }

    private static ArrayList<Identifier> difference(List<Identifier> candidates,
            Set<Identifier> present) {
        ArrayList<Identifier> result = new ArrayList<Identifier>();
        for (Identifier id : candidates) if (!present.contains(id)) result.add(id);
        Collections.sort(result);
        return result;
    }

    public RegistrySnapshot local() { return local; }

    public RegistrySnapshot remote() { return remote; }

    public Status status() { return status; }

    public List<Identifier> missingLocally() { return missingLocally; }

    public List<Identifier> missingRemotely() { return missingRemotely; }

    public boolean stableIdsCompatible() {
        return status == Status.EXACT_LAYOUT || status == Status.SAME_ENTRIES_DIFFERENT_ORDER;
    }

    /** Raw IDs are safe only after both equal layouts have been frozen. */
    public boolean rawIdsCompatible() {
        return status == Status.EXACT_LAYOUT && local.frozen() && remote.frozen();
    }

    @Override public String toString() {
        return "RegistryComparison{" + local.registryId() + '=' + status
                + ", missingLocally=" + missingLocally
                + ", missingRemotely=" + missingRemotely + '}';
    }
}
