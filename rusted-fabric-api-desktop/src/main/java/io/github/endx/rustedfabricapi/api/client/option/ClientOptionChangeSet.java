package io.github.endx.rustedfabricapi.api.client.option;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** Immutable, ordered set of validated option changes. */
public final class ClientOptionChangeSet {
    private static final ClientOptionChangeSet EMPTY =
            new ClientOptionChangeSet(Collections.emptyList());

    private final List<ClientOptionChange<?>> changes;
    private final boolean restartRequired;

    ClientOptionChangeSet(List<ClientOptionChange<?>> changes) {
        this.changes = Collections.unmodifiableList(
                new ArrayList<ClientOptionChange<?>>(changes));
        boolean restart = false;
        for (ClientOptionChange<?> change : changes) restart |= change.restartRequired();
        this.restartRequired = restart;
    }

    public static ClientOptionChangeSet empty() { return EMPTY; }
    public List<ClientOptionChange<?>> changes() { return changes; }
    public int size() { return changes.size(); }
    public boolean isEmpty() { return changes.isEmpty(); }
    public boolean restartRequired() { return restartRequired; }

    public boolean contains(ClientOption<?> option) {
        return change(option).isPresent();
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<ClientOptionChange<T>> change(ClientOption<T> option) {
        for (ClientOptionChange<?> change : changes) {
            if (change.option() == option) {
                return Optional.of((ClientOptionChange<T>) change);
            }
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return changes.toString();
    }
}
