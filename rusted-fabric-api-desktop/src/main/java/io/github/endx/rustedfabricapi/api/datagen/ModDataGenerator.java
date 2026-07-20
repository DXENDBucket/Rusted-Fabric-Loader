package io.github.endx.rustedfabricapi.api.datagen;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import io.github.endx.rustedfabricapi.api.util.Identifier;

/** Dependency-ordered, transaction-before-commit data generator for ordinary mod Jars. */
public final class ModDataGenerator {
    private final Path outputRoot;
    private final String modId;
    private final LinkedHashMap<Identifier, Registration> providers =
            new LinkedHashMap<Identifier, Registration>();

    public ModDataGenerator(Path outputRoot, String modId) {
        this.outputRoot = Objects.requireNonNull(outputRoot, "outputRoot")
                .toAbsolutePath().normalize();
        this.modId = DataOutput.validateModId(modId);
    }

    public Path outputRoot() { return outputRoot; }

    public String modId() { return modId; }

    public ModDataGenerator addProvider(Identifier id, DataProvider provider,
            Identifier... dependencies) {
        Identifier checkedId = Objects.requireNonNull(id, "id");
        DataProvider checkedProvider = Objects.requireNonNull(provider, "provider");
        LinkedHashSet<Identifier> checkedDependencies = new LinkedHashSet<Identifier>();
        if (dependencies != null) {
            for (Identifier dependency : dependencies) {
                Identifier checked = Objects.requireNonNull(dependency, "dependency");
                if (checkedId.equals(checked)) {
                    throw new IllegalArgumentException("A data provider cannot depend on itself: "
                            + checkedId);
                }
                checkedDependencies.add(checked);
            }
        }
        if (providers.containsKey(checkedId)) {
            throw new IllegalArgumentException("Duplicate data provider ID: " + checkedId);
        }
        providers.put(checkedId, new Registration(checkedId, checkedProvider,
                new ArrayList<Identifier>(checkedDependencies)));
        return this;
    }

    public ModDataGenerator addProvider(String id, DataProvider provider,
            String... dependencies) {
        Identifier[] parsed = new Identifier[dependencies != null ? dependencies.length : 0];
        for (int i = 0; i < parsed.length; i++) parsed[i] = Identifier.parse(dependencies[i]);
        return addProvider(Identifier.parse(id), provider, parsed);
    }

    public List<Identifier> providerIds() {
        return Collections.unmodifiableList(new ArrayList<Identifier>(providers.keySet()));
    }

    /** Runs all providers; no generated file is changed when any provider fails or is blocked. */
    public DataGenerationReport run() throws IOException {
        long started = System.nanoTime();
        LinkedHashMap<Identifier, Registration> snapshot =
                new LinkedHashMap<Identifier, Registration>(providers);
        LinkedHashMap<Identifier, DataProviderResult> results =
                new LinkedHashMap<Identifier, DataProviderResult>();
        Set<Identifier> blocked = new LinkedHashSet<Identifier>();

        for (Registration registration : snapshot.values()) {
            for (Identifier dependency : registration.dependencies) {
                if (!snapshot.containsKey(dependency)) {
                    blocked.add(registration.id);
                    results.put(registration.id, result(registration.id,
                            DataProviderStatus.BLOCKED, 0,
                            "missing dependency " + dependency, null));
                    break;
                }
            }
        }
        boolean changed;
        do {
            changed = false;
            for (Registration registration : snapshot.values()) {
                if (blocked.contains(registration.id)) continue;
                for (Identifier dependency : registration.dependencies) {
                    if (blocked.contains(dependency)) {
                        blocked.add(registration.id);
                        results.put(registration.id, result(registration.id,
                                DataProviderStatus.BLOCKED, 0,
                                "blocked dependency " + dependency, null));
                        changed = true;
                        break;
                    }
                }
            }
        } while (changed);

        List<Registration> order = topologicalOrder(snapshot, blocked);
        if (order.size() != snapshot.size() - blocked.size()) {
            Set<Identifier> ordered = new LinkedHashSet<Identifier>();
            for (Registration registration : order) ordered.add(registration.id);
            for (Identifier id : snapshot.keySet()) {
                if (!blocked.contains(id) && !ordered.contains(id)) {
                    blocked.add(id);
                    results.put(id, result(id, DataProviderStatus.BLOCKED, 0,
                            "dependency cycle or dependency on a cycle", null));
                }
            }
        }

        LinkedHashMap<String, PlannedResource> planned =
                new LinkedHashMap<String, PlannedResource>();
        for (Registration registration : order) {
            if (blocked.contains(registration.id)) continue;
            Identifier failedDependency = null;
            for (Identifier dependency : registration.dependencies) {
                DataProviderResult dependencyResult = results.get(dependency);
                if (dependencyResult == null || !dependencyResult.generated()) {
                    failedDependency = dependency;
                    break;
                }
            }
            if (failedDependency != null) {
                results.put(registration.id, result(registration.id,
                        DataProviderStatus.BLOCKED, 0,
                        "dependency did not generate: " + failedDependency, null));
                continue;
            }

            DataOutput local = new DataOutput(modId, registration.id);
            try {
                registration.provider.generate(local);
                for (Map.Entry<String, byte[]> entry : local.resources().entrySet()) {
                    PlannedResource previous = planned.get(entry.getKey());
                    if (previous != null) {
                        throw new IllegalArgumentException("Generated path " + entry.getKey()
                                + " is owned by both " + previous.provider + " and "
                                + registration.id);
                    }
                }
                for (Map.Entry<String, byte[]> entry : local.resources().entrySet()) {
                    planned.put(entry.getKey(), new PlannedResource(
                            registration.id, entry.getValue()));
                }
                results.put(registration.id, result(registration.id,
                        DataProviderStatus.GENERATED, local.resources().size(), "", null));
            } catch (Exception failure) {
                results.put(registration.id, result(registration.id,
                        DataProviderStatus.FAILED, 0, failure.toString(), failure));
            }
        }

        ArrayList<DataProviderResult> orderedResults =
                new ArrayList<DataProviderResult>(snapshot.size());
        boolean allGenerated = true;
        for (Identifier id : snapshot.keySet()) {
            DataProviderResult providerResult = results.get(id);
            if (providerResult == null) {
                providerResult = result(id, DataProviderStatus.BLOCKED, 0,
                        "provider was not scheduled", null);
            }
            orderedResults.add(providerResult);
            allGenerated &= providerResult.generated();
        }
        if (!allGenerated) {
            return new DataGenerationReport(outputRoot, orderedResults, false,
                    Collections.emptyList(), Collections.emptyList(),
                    System.nanoTime() - started);
        }

        CommitResult commit = commit(planned);
        return new DataGenerationReport(outputRoot, orderedResults, true,
                commit.written, commit.unchanged, System.nanoTime() - started);
    }

    private CommitResult commit(Map<String, PlannedResource> planned) throws IOException {
        if (Files.exists(outputRoot) && !Files.isDirectory(outputRoot)) {
            throw new IOException("Data-generation output root is not a directory: " + outputRoot);
        }
        Files.createDirectories(outputRoot);
        Path realRoot = outputRoot.toRealPath();
        ArrayList<String> paths = new ArrayList<String>(planned.keySet());
        Collections.sort(paths);
        ArrayList<String> written = new ArrayList<String>();
        ArrayList<String> unchanged = new ArrayList<String>();
        for (String relative : paths) {
            Path requested = realRoot.resolve(relative).normalize();
            if (!requested.startsWith(realRoot)) {
                throw new IOException("Generated path escaped output root: " + relative);
            }
            Path parent = requested.getParent();
            Files.createDirectories(parent);
            Path realParent = parent.toRealPath();
            if (!realParent.startsWith(realRoot)) {
                throw new IOException("Generated path traversed a linked directory: " + relative);
            }
            Path target = realParent.resolve(requested.getFileName());
            if (Files.isSymbolicLink(target) || Files.exists(target) && !Files.isRegularFile(target)) {
                throw new IOException("Generated target is not a regular file: " + relative);
            }
            Path temporary = Files.createTempFile(realParent,
                    "." + requested.getFileName().toString() + '.', ".tmp");
            try {
                Files.write(temporary, planned.get(relative).bytes);
                if (Files.isRegularFile(target) && Files.mismatch(temporary, target) == -1L) {
                    Files.delete(temporary);
                    unchanged.add(relative);
                    continue;
                }
                try {
                    Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE,
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException unsupported) {
                    Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
                }
                written.add(relative);
            } finally {
                Files.deleteIfExists(temporary);
            }
        }
        return new CommitResult(written, unchanged);
    }

    private static List<Registration> topologicalOrder(
            LinkedHashMap<Identifier, Registration> providers, Set<Identifier> blocked) {
        LinkedHashMap<Identifier, Integer> indegree = new LinkedHashMap<Identifier, Integer>();
        LinkedHashMap<Identifier, List<Identifier>> dependents =
                new LinkedHashMap<Identifier, List<Identifier>>();
        for (Registration registration : providers.values()) {
            if (blocked.contains(registration.id)) continue;
            int count = 0;
            for (Identifier dependency : registration.dependencies) {
                if (blocked.contains(dependency)) continue;
                count++;
                dependents.computeIfAbsent(dependency,
                        ignored -> new ArrayList<Identifier>()).add(registration.id);
            }
            indegree.put(registration.id, Integer.valueOf(count));
        }
        ArrayDeque<Identifier> ready = new ArrayDeque<Identifier>();
        for (Map.Entry<Identifier, Integer> entry : indegree.entrySet()) {
            if (entry.getValue().intValue() == 0) ready.addLast(entry.getKey());
        }
        ArrayList<Registration> result = new ArrayList<Registration>();
        while (!ready.isEmpty()) {
            Identifier id = ready.removeFirst();
            result.add(providers.get(id));
            List<Identifier> next = dependents.get(id);
            if (next == null) continue;
            for (Identifier dependent : next) {
                int value = indegree.get(dependent).intValue() - 1;
                indegree.put(dependent, Integer.valueOf(value));
                if (value == 0) ready.addLast(dependent);
            }
        }
        return result;
    }

    private static DataProviderResult result(Identifier id, DataProviderStatus status,
            int count, String detail, Exception failure) {
        return new DataProviderResult(id, status, count, detail, failure);
    }

    private static final class Registration {
        final Identifier id;
        final DataProvider provider;
        final List<Identifier> dependencies;

        Registration(Identifier id, DataProvider provider, List<Identifier> dependencies) {
            this.id = id;
            this.provider = provider;
            this.dependencies = Collections.unmodifiableList(dependencies);
        }
    }

    private static final class PlannedResource {
        final Identifier provider;
        final byte[] bytes;

        PlannedResource(Identifier provider, byte[] bytes) {
            this.provider = provider;
            this.bytes = bytes;
        }
    }

    private static final class CommitResult {
        final List<String> written;
        final List<String> unchanged;

        CommitResult(List<String> written, List<String> unchanged) {
            this.written = written;
            this.unchanged = unchanged;
        }
    }
}
