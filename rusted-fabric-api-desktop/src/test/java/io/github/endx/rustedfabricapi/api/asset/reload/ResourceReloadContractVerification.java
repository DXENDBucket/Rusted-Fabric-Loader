package io.github.endx.rustedfabricapi.api.asset.reload;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.endx.rustedfabricapi.api.asset.ModResourcePack;
import io.github.endx.rustedfabricapi.api.asset.ModResources;
import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import io.github.endx.rustedfabricapi.api.util.Identifier;

/** Two-phase ordering, isolation, dependency, cycle, report, and cleanup checks. */
public final class ResourceReloadContractVerification {
    private ResourceReloadContractVerification() {
    }

    public static void verify() {
        ModResourcePack resources = ModResources.forClass("reload_contract",
                ResourceReloadContractVerification.class);
        List<String> phases = new ArrayList<String>();
        AtomicInteger beforeCalls = new AtomicInteger();
        AtomicInteger afterCalls = new AtomicInteger();
        RustedFabricEvent.Registration before = ModResourceReloadEvents.BEFORE_RELOAD.subscribe(
                (reason, ids) -> {
                    if (reason == ResourceReloadReason.MANUAL && ids.size() == 5) {
                        beforeCalls.incrementAndGet();
                    }
                });
        RustedFabricEvent.Registration after = ModResourceReloadEvents.AFTER_RELOAD.subscribe(
                report -> {
                    if (report.reason() == ResourceReloadReason.MANUAL) afterCalls.incrementAndGet();
                });

        ModResourceReloaders.Registration base = ModResourceReloaders.register(
                "reload_contract:base", resources, new ModResourceReloader<Properties>() {
                    @Override public Properties prepare(ModResourcePack pack) throws Exception {
                        phases.add("prepare-base");
                        return pack.resource("assets/contract/lang/en.properties")
                                .readPropertiesUtf8();
                    }

                    @Override public void apply(Properties prepared) {
                        require("Hello {0}".equals(prepared.getProperty("greeting")),
                                "prepared resource data changed before apply");
                        phases.add("apply-base");
                    }
                });
        ModResourceReloaders.Registration dependent = ModResourceReloaders.register(
                "reload_contract:dependent", resources, new StringReloader(
                        phases, "prepare-dependent", "apply-dependent"),
                "reload_contract:base");
        ModResourceReloaders.Registration failed = ModResourceReloaders.register(
                "reload_contract:failed", resources, new ModResourceReloader<String>() {
                    @Override public String prepare(ModResourcePack pack) throws Exception {
                        phases.add("prepare-failed");
                        throw new Exception("expected prepare failure");
                    }

                    @Override public void apply(String prepared) {
                        throw new AssertionError("failed preparation reached apply");
                    }
                });
        ModResourceReloaders.Registration blocked = ModResourceReloaders.register(
                "reload_contract:blocked", resources, new StringReloader(
                        phases, "prepare-blocked", "apply-blocked"),
                "reload_contract:failed");
        ModResourceReloaders.Registration independent = ModResourceReloaders.register(
                "reload_contract:independent", resources, new StringReloader(
                        phases, "prepare-independent", "apply-independent"));

        ResourceReloadReport report = ModResourceReloaders.reloadAll(ResourceReloadReason.MANUAL);
        require(report.listenerCount() == 5 && report.failureCount() == 2 && !report.successful(),
                "reload report summary lost success or failure counts");
        require(status(report, "reload_contract:base") == ResourceReloadStatus.APPLIED
                        && status(report, "reload_contract:dependent") == ResourceReloadStatus.APPLIED
                        && status(report, "reload_contract:failed") == ResourceReloadStatus.PREPARE_FAILED
                        && status(report, "reload_contract:blocked") == ResourceReloadStatus.BLOCKED
                        && status(report, "reload_contract:independent") == ResourceReloadStatus.APPLIED,
                "reload report assigned an incorrect listener status: " + report.results());
        require(phases.indexOf("prepare-base") < phases.indexOf("apply-base")
                        && phases.indexOf("apply-base") < phases.indexOf("apply-dependent")
                        && !phases.contains("apply-blocked")
                        && phases.contains("apply-independent"),
                "prepare/apply dependency ordering or failure isolation drifted: " + phases);
        require(beforeCalls.get() == 1 && afterCalls.get() == 1,
                "resource reload lifecycle events were not dispatched once");
        require(report.result(Identifier.parse("reload_contract:failed"))
                        .orElseThrow(AssertionError::new).failure().isPresent(),
                "resource reload report discarded the preparation failure");

        closeAll(base, dependent, failed, blocked, independent);

        AtomicInteger invalidPrepares = new AtomicInteger();
        ModResourceReloaders.Registration missing = ModResourceReloaders.register(
                "reload_contract:missing", resources,
                countingReloader(invalidPrepares), "reload_contract:not_registered");
        ModResourceReloaders.Registration cycleA = ModResourceReloaders.register(
                "reload_contract:cycle_a", resources,
                countingReloader(invalidPrepares), "reload_contract:cycle_b");
        ModResourceReloaders.Registration cycleB = ModResourceReloaders.register(
                "reload_contract:cycle_b", resources,
                countingReloader(invalidPrepares), "reload_contract:cycle_a");
        ResourceReloadReport invalid = ModResourceReloaders.reloadAll(ResourceReloadReason.MANUAL);
        require(invalid.failureCount() == 3
                        && status(invalid, "reload_contract:missing") == ResourceReloadStatus.BLOCKED
                        && status(invalid, "reload_contract:cycle_a") == ResourceReloadStatus.BLOCKED
                        && status(invalid, "reload_contract:cycle_b") == ResourceReloadStatus.BLOCKED
                        && invalidPrepares.get() == 0,
                "missing/cyclic reload dependencies were executed or misreported");
        closeAll(missing, cycleA, cycleB);
        before.close();
        after.close();
        require(ModResourceReloaders.registeredIds().isEmpty(),
                "resource reloader registrations leaked after close");
    }

    private static ModResourceReloader<String> countingReloader(AtomicInteger prepares) {
        return new ModResourceReloader<String>() {
            @Override public String prepare(ModResourcePack resources) {
                prepares.incrementAndGet();
                return "prepared";
            }

            @Override public void apply(String prepared) {
                throw new AssertionError("blocked reloader reached apply");
            }
        };
    }

    private static ResourceReloadStatus status(ResourceReloadReport report, String id) {
        return report.result(Identifier.parse(id)).orElseThrow(AssertionError::new).status();
    }

    private static void closeAll(ModResourceReloaders.Registration... registrations) {
        for (ModResourceReloaders.Registration registration : registrations) registration.close();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class StringReloader implements ModResourceReloader<String> {
        private final List<String> phases;
        private final String prepareName;
        private final String applyName;

        StringReloader(List<String> phases, String prepareName, String applyName) {
            this.phases = phases;
            this.prepareName = prepareName;
            this.applyName = applyName;
        }

        @Override public String prepare(ModResourcePack resources) {
            phases.add(prepareName);
            return prepareName;
        }

        @Override public void apply(String prepared) { phases.add(applyName); }
    }
}
