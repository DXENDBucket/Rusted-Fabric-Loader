package io.github.endx.rustedfabricapi.api.datagen;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.github.endx.rustedfabricapi.api.util.Identifier;

/** Immutable report for provider execution and the final output commit. */
public final class DataGenerationReport {
    private final Path outputRoot;
    private final List<DataProviderResult> providers;
    private final boolean committed;
    private final List<String> writtenPaths;
    private final List<String> unchangedPaths;
    private final long elapsedNanos;

    DataGenerationReport(Path outputRoot, List<DataProviderResult> providers,
            boolean committed, List<String> writtenPaths, List<String> unchangedPaths,
            long elapsedNanos) {
        this.outputRoot = Objects.requireNonNull(outputRoot, "outputRoot");
        this.providers = immutable(providers);
        this.committed = committed;
        this.writtenPaths = immutableStrings(writtenPaths);
        this.unchangedPaths = immutableStrings(unchangedPaths);
        this.elapsedNanos = elapsedNanos;
    }

    public Path outputRoot() { return outputRoot; }

    public List<DataProviderResult> providers() { return providers; }

    public Optional<DataProviderResult> provider(Identifier id) {
        Identifier checked = Objects.requireNonNull(id, "id");
        for (DataProviderResult result : providers) {
            if (result.id().equals(checked)) return Optional.of(result);
        }
        return Optional.empty();
    }

    public boolean committed() { return committed; }

    public boolean successful() {
        if (!committed) return false;
        for (DataProviderResult provider : providers) {
            if (!provider.generated()) return false;
        }
        return true;
    }

    public List<String> writtenPaths() { return writtenPaths; }

    public List<String> unchangedPaths() { return unchangedPaths; }

    public int generatedResourceCount() {
        int count = 0;
        for (DataProviderResult provider : providers) count += provider.resourceCount();
        return count;
    }

    public long elapsedNanos() { return elapsedNanos; }

    /** Returns this report or throws with every failed/blocked provider in the message. */
    public DataGenerationReport requireSuccess() {
        if (successful()) return this;
        StringBuilder message = new StringBuilder("Mod data generation did not commit");
        for (DataProviderResult provider : providers) {
            if (provider.generated()) continue;
            message.append("; ").append(provider.id()).append('=').append(provider.status());
            if (!provider.detail().isEmpty()) message.append(" (").append(provider.detail()).append(')');
        }
        throw new IllegalStateException(message.toString());
    }

    private static List<DataProviderResult> immutable(List<DataProviderResult> values) {
        ArrayList<DataProviderResult> copy = new ArrayList<DataProviderResult>(values);
        for (DataProviderResult value : copy) Objects.requireNonNull(value, "provider result");
        return Collections.unmodifiableList(copy);
    }

    private static List<String> immutableStrings(List<String> values) {
        ArrayList<String> copy = new ArrayList<String>(values);
        for (String value : copy) Objects.requireNonNull(value, "path");
        return Collections.unmodifiableList(copy);
    }

    @Override public String toString() {
        return "DataGenerationReport{committed=" + committed + ", providers="
                + providers.size() + ", written=" + writtenPaths.size()
                + ", unchanged=" + unchangedPaths.size() + '}';
    }
}
