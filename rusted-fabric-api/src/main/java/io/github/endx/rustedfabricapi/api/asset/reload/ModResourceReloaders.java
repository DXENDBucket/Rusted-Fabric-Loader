package io.github.endx.rustedfabricapi.api.asset.reload;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import io.github.endx.rustedfabricapi.api.asset.ModResourcePack;
import io.github.endx.rustedfabricapi.api.client.event.ClientLifecycleEvents;
import io.github.endx.rustedfabricapi.api.custom.event.CustomUnitRegistryEvents;
import io.github.endx.rustedfabricapi.api.text.LanguageEvents;
import io.github.endx.rustedfabricapi.api.util.Identifier;
import io.github.endx.rustedfabricapi.internal.development.DevelopmentReloadRuntime;

/** Registration and deterministic execution of Fabric-style mod resource reload listeners. */
public final class ModResourceReloaders {
    private static final Object LOCK = new Object();
    private static final LinkedHashMap<Identifier, Binding<?>> BINDINGS =
            new LinkedHashMap<Identifier, Binding<?>>();
    private static boolean hooksInstalled;
    private static boolean reloading;

    private ModResourceReloaders() {
    }

    public static <P> Registration register(Identifier id, ModResourcePack resources,
            ModResourceReloader<P> reloader, Identifier... dependencies) {
        Identifier checkedId = Objects.requireNonNull(id, "id");
        ModResourcePack checkedResources = Objects.requireNonNull(resources, "resources");
        ModResourceReloader<P> checkedReloader = Objects.requireNonNull(reloader, "reloader");
        LinkedHashSet<Identifier> checkedDependencies = new LinkedHashSet<Identifier>();
        if (dependencies != null) {
            for (Identifier dependency : dependencies) {
                Identifier checked = Objects.requireNonNull(dependency, "dependency");
                if (checkedId.equals(checked)) {
                    throw new IllegalArgumentException("A resource reloader cannot depend on itself: "
                            + checkedId);
                }
                checkedDependencies.add(checked);
            }
        }
        Binding<P> binding = new Binding<P>(checkedId, checkedResources, checkedReloader,
                new ArrayList<Identifier>(checkedDependencies));
        synchronized (LOCK) {
            if (BINDINGS.containsKey(checkedId)) {
                throw new IllegalArgumentException("Duplicate resource reloader ID: " + checkedId);
            }
            BINDINGS.put(checkedId, binding);
            DevelopmentWorkspaceReloadMonitor.track(checkedResources.modId());
            installHooksLocked();
        }
        return new Registration(binding);
    }

    public static <P> Registration register(String id, ModResourcePack resources,
            ModResourceReloader<P> reloader, String... dependencies) {
        Identifier[] parsed = new Identifier[dependencies != null ? dependencies.length : 0];
        for (int i = 0; i < parsed.length; i++) parsed[i] = Identifier.parse(dependencies[i]);
        return register(Identifier.parse(id), resources, reloader, parsed);
    }

    public static List<Identifier> registeredIds() {
        synchronized (LOCK) {
            return Collections.unmodifiableList(new ArrayList<Identifier>(BINDINGS.keySet()));
        }
    }

    public static ResourceReloadReport reloadAll(ResourceReloadReason reason) {
        ResourceReloadReason checkedReason = Objects.requireNonNull(reason, "reason");
        LinkedHashMap<Identifier, Binding<?>> snapshot;
        synchronized (LOCK) {
            if (reloading) throw new IllegalStateException("A mod resource reload is already running");
            reloading = true;
            snapshot = new LinkedHashMap<Identifier, Binding<?>>(BINDINGS);
        }
        long started = System.nanoTime();
        try {
            List<Identifier> ids = Collections.unmodifiableList(
                    new ArrayList<Identifier>(snapshot.keySet()));
            ModResourceReloadEvents.BEFORE_RELOAD.invoker().beforeReload(checkedReason, ids);
            ResourceReloadReport report = execute(checkedReason, snapshot, started);
            ModResourceReloadEvents.AFTER_RELOAD.invoker().afterReload(report);
            return report;
        } finally {
            synchronized (LOCK) { reloading = false; }
        }
    }

    private static ResourceReloadReport execute(ResourceReloadReason reason,
            LinkedHashMap<Identifier, Binding<?>> snapshot, long started) {
        LinkedHashMap<Identifier, ResourceReloadReport.Result> results =
                new LinkedHashMap<Identifier, ResourceReloadReport.Result>();
        Set<Identifier> blocked = new LinkedHashSet<Identifier>();

        for (Binding<?> binding : snapshot.values()) {
            for (Identifier dependency : binding.dependencies) {
                if (!snapshot.containsKey(dependency)) {
                    blocked.add(binding.id);
                    results.put(binding.id, result(binding.id, ResourceReloadStatus.BLOCKED,
                            "missing dependency " + dependency, null));
                    break;
                }
            }
        }
        boolean changed;
        do {
            changed = false;
            for (Binding<?> binding : snapshot.values()) {
                if (blocked.contains(binding.id)) continue;
                for (Identifier dependency : binding.dependencies) {
                    if (blocked.contains(dependency)) {
                        blocked.add(binding.id);
                        results.put(binding.id, result(binding.id, ResourceReloadStatus.BLOCKED,
                                "blocked dependency " + dependency, null));
                        changed = true;
                        break;
                    }
                }
            }
        } while (changed);

        LinkedHashMap<Identifier, Integer> indegree = new LinkedHashMap<Identifier, Integer>();
        LinkedHashMap<Identifier, List<Identifier>> dependents =
                new LinkedHashMap<Identifier, List<Identifier>>();
        for (Binding<?> binding : snapshot.values()) {
            if (blocked.contains(binding.id)) continue;
            int count = 0;
            for (Identifier dependency : binding.dependencies) {
                if (blocked.contains(dependency)) continue;
                count++;
                dependents.computeIfAbsent(dependency,
                        ignored -> new ArrayList<Identifier>()).add(binding.id);
            }
            indegree.put(binding.id, Integer.valueOf(count));
        }
        ArrayDeque<Identifier> ready = new ArrayDeque<Identifier>();
        for (Map.Entry<Identifier, Integer> entry : indegree.entrySet()) {
            if (entry.getValue().intValue() == 0) ready.add(entry.getKey());
        }
        ArrayList<Binding<?>> order = new ArrayList<Binding<?>>();
        while (!ready.isEmpty()) {
            Identifier id = ready.removeFirst();
            order.add(snapshot.get(id));
            List<Identifier> next = dependents.get(id);
            if (next == null) continue;
            for (Identifier dependent : next) {
                int remaining = indegree.get(dependent).intValue() - 1;
                indegree.put(dependent, Integer.valueOf(remaining));
                if (remaining == 0) ready.addLast(dependent);
            }
        }
        if (order.size() != indegree.size()) {
            Set<Identifier> ordered = new LinkedHashSet<Identifier>();
            for (Binding<?> binding : order) ordered.add(binding.id);
            for (Identifier id : indegree.keySet()) {
                if (!ordered.contains(id)) {
                    blocked.add(id);
                    results.put(id, result(id, ResourceReloadStatus.BLOCKED,
                            "dependency cycle or dependency on a cycle", null));
                }
            }
        }

        LinkedHashMap<Identifier, Object> prepared = new LinkedHashMap<Identifier, Object>();
        for (Binding<?> binding : order) {
            if (blocked.contains(binding.id)) continue;
            try {
                prepared.put(binding.id, binding.prepare());
            } catch (Exception failure) {
                results.put(binding.id, result(binding.id, ResourceReloadStatus.PREPARE_FAILED,
                        failure.toString(), failure));
            }
        }
        for (Binding<?> binding : order) {
            if (blocked.contains(binding.id) || results.containsKey(binding.id)) continue;
            Identifier failedDependency = null;
            for (Identifier dependency : binding.dependencies) {
                ResourceReloadReport.Result dependencyResult = results.get(dependency);
                if (dependencyResult == null
                        || dependencyResult.status() != ResourceReloadStatus.APPLIED) {
                    failedDependency = dependency;
                    break;
                }
            }
            if (failedDependency != null) {
                results.put(binding.id, result(binding.id, ResourceReloadStatus.BLOCKED,
                        "dependency did not apply: " + failedDependency, null));
                continue;
            }
            try {
                binding.apply(prepared.get(binding.id));
                results.put(binding.id, result(binding.id, ResourceReloadStatus.APPLIED, "", null));
            } catch (Exception failure) {
                results.put(binding.id, result(binding.id, ResourceReloadStatus.APPLY_FAILED,
                        failure.toString(), failure));
            }
        }

        ArrayList<ResourceReloadReport.Result> orderedResults =
                new ArrayList<ResourceReloadReport.Result>(snapshot.size());
        for (Identifier id : snapshot.keySet()) {
            ResourceReloadReport.Result result = results.get(id);
            if (result == null) {
                result = result(id, ResourceReloadStatus.BLOCKED,
                        "reloader was not scheduled", null);
            }
            orderedResults.add(result);
        }
        return new ResourceReloadReport(reason, orderedResults, System.nanoTime() - started);
    }

    private static ResourceReloadReport.Result result(Identifier id,
            ResourceReloadStatus status, String detail, Exception failure) {
        return new ResourceReloadReport.Result(id, status, detail, failure);
    }

    private static void installHooksLocked() {
        if (hooksInstalled) return;
        hooksInstalled = true;
        ClientLifecycleEvents.AFTER_ENGINE_INITIALIZATION.register(engine ->
                reloadAll(ResourceReloadReason.INITIAL_ENGINE_READY));
        CustomUnitRegistryEvents.AFTER_ACTION_LINKS_BUILT.register(types -> {
            if (!DevelopmentReloadRuntime.isIntegratedUnitReloadRunning()) {
                reloadAll(ResourceReloadReason.NATIVE_CUSTOM_UNITS);
            }
        });
        LanguageEvents.AFTER_RELOAD.register(language ->
                reloadAll(ResourceReloadReason.LANGUAGE));
        io.github.endx.rustedfabricapi.api.client.event.ClientTickEvents.END_CLIENT_TICK
                .register(engine -> {
                    if (DevelopmentWorkspaceReloadMonitor.poll(System.nanoTime())) {
                        try {
                            reloadAll(ResourceReloadReason.DEVELOPMENT_WORKSPACE_CHANGED);
                        } catch (RuntimeException failure) {
                            System.err.println("[Rusted Fabric API] Development resource reload failed: "
                                    + failure);
                        }
                    }
                });
    }

    private static final class Binding<P> {
        final Identifier id;
        final ModResourcePack resources;
        final ModResourceReloader<P> reloader;
        final List<Identifier> dependencies;

        Binding(Identifier id, ModResourcePack resources, ModResourceReloader<P> reloader,
                List<Identifier> dependencies) {
            this.id = id;
            this.resources = resources;
            this.reloader = reloader;
            this.dependencies = Collections.unmodifiableList(dependencies);
        }

        Object prepare() throws Exception { return reloader.prepare(resources); }

        @SuppressWarnings("unchecked")
        void apply(Object prepared) throws Exception { reloader.apply((P) prepared); }
    }

    public static final class Registration implements AutoCloseable {
        private final Binding<?> binding;
        private boolean active = true;

        Registration(Binding<?> binding) { this.binding = binding; }

        public Identifier id() { return binding.id; }

        public synchronized boolean unregister() {
            if (!active) return false;
            active = false;
            synchronized (LOCK) {
                boolean removed = BINDINGS.remove(binding.id, binding);
                if (removed) DevelopmentWorkspaceReloadMonitor.untrack(binding.resources.modId());
                return removed;
            }
        }

        @Override public void close() { unregister(); }
    }
}
