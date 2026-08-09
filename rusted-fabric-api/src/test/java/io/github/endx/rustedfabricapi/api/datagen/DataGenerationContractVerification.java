package io.github.endx.rustedfabricapi.api.datagen;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.endx.rustedfabricapi.api.datagen.provider.LanguageBuilder;
import io.github.endx.rustedfabricapi.api.datagen.provider.LanguageDataProvider;
import io.github.endx.rustedfabricapi.api.datagen.provider.RegistryTagDataProvider;
import io.github.endx.rustedfabricapi.api.registry.RegistryKey;
import io.github.endx.rustedfabricapi.api.util.Identifier;

/** Transaction, dependency, collision, unchanged-output, and typed-provider checks. */
public final class DataGenerationContractVerification {
    private DataGenerationContractVerification() {
    }

    public static void verify() {
        Path root;
        try {
            root = Files.createTempDirectory("rustedfabric-datagen-contract-");
        } catch (Exception failure) {
            throw new AssertionError("could not create datagen contract directory", failure);
        }
        try {
            verifyCore(root.resolve("core"));
            verifyFailureIsolation(root.resolve("failure"));
            verifyGraphFailures(root.resolve("graphs"));
            verifyTypedProviders(root.resolve("typed"));
        } catch (Exception failure) {
            throw new AssertionError("data generation contract failed", failure);
        } finally {
            deleteTempTree(root);
        }
    }

    private static void verifyCore(Path outputRoot) throws Exception {
        AtomicReference<String> text = new AtomicReference<String>("first");
        ModDataGenerator generator = new ModDataGenerator(outputRoot, "datagen_contract");
        generator.addProvider("datagen_contract:base", output -> {
            output.writeUtf8("data/datagen_contract/z.txt", "z");
            output.writeUtf8("data/datagen_contract/a.txt", text.get());
        });
        generator.addProvider("datagen_contract:dependent", output -> {
            JsonObject json = new JsonObject();
            json.addProperty("value", 7);
            output.writeJson("data/datagen_contract/result.json", json);
        }, "datagen_contract:base");

        DataGenerationReport first = generator.run().requireSuccess();
        require(first.committed() && first.generatedResourceCount() == 3
                        && first.writtenPaths().equals(Arrays.asList(
                                "data/datagen_contract/a.txt",
                                "data/datagen_contract/result.json",
                                "data/datagen_contract/z.txt")),
                "initial datagen run was not deterministically committed");
        require("first".equals(Files.readString(
                        outputRoot.resolve("data/datagen_contract/a.txt"))),
                "generated UTF-8 resource had wrong content");

        DataGenerationReport unchanged = generator.run().requireSuccess();
        require(unchanged.writtenPaths().isEmpty() && unchanged.unchangedPaths().size() == 3,
                "identical rerun rewrote unchanged resources");

        text.set("second");
        DataGenerationReport changed = generator.run().requireSuccess();
        require(changed.writtenPaths().equals(Arrays.asList("data/datagen_contract/a.txt"))
                        && changed.unchangedPaths().size() == 2,
                "changed rerun did not isolate its file update");
        require(generator.providerIds().equals(Arrays.asList(
                        Identifier.parse("datagen_contract:base"),
                        Identifier.parse("datagen_contract:dependent"))),
                "provider IDs did not preserve registration order");
    }

    private static void verifyFailureIsolation(Path outputRoot) throws Exception {
        Path target = outputRoot.resolve("data/datagen_contract/kept.txt");
        Files.createDirectories(target.getParent());
        Files.writeString(target, "old", StandardCharsets.UTF_8);

        ModDataGenerator failure = new ModDataGenerator(outputRoot, "datagen_contract");
        failure.addProvider("datagen_contract:planned",
                output -> output.writeUtf8("data/datagen_contract/kept.txt", "new"));
        failure.addProvider("datagen_contract:failing", output -> {
            output.writeUtf8("data/datagen_contract/discarded.txt", "discarded");
            throw new IllegalStateException("intentional failure");
        });
        AtomicInteger dependentCalls = new AtomicInteger();
        failure.addProvider("datagen_contract:blocked", output -> dependentCalls.incrementAndGet(),
                "datagen_contract:failing");

        DataGenerationReport report = failure.run();
        require(!report.committed() && !report.successful()
                        && report.provider(Identifier.parse("datagen_contract:failing"))
                                .orElseThrow(AssertionError::new).status()
                                == DataProviderStatus.FAILED
                        && report.provider(Identifier.parse("datagen_contract:blocked"))
                                .orElseThrow(AssertionError::new).status()
                                == DataProviderStatus.BLOCKED
                        && dependentCalls.get() == 0,
                "provider failure did not block its dependent transaction");
        require("old".equals(Files.readString(target))
                        && !Files.exists(outputRoot.resolve(
                                "data/datagen_contract/discarded.txt")),
                "failed provider transaction changed the output tree");
        expectIllegal(report::requireSuccess,
                "failed generation report was accepted as successful");

        ModDataGenerator duplicate = new ModDataGenerator(
                outputRoot.resolve("duplicate"), "datagen_contract");
        duplicate.addProvider("datagen_contract:first",
                output -> output.writeUtf8("same.txt", "one"));
        duplicate.addProvider("datagen_contract:second",
                output -> output.writeUtf8("same.txt", "two"));
        require(!duplicate.run().committed(),
                "cross-provider output collision was committed");

        ModDataGenerator traversal = new ModDataGenerator(
                outputRoot.resolve("traversal"), "datagen_contract");
        traversal.addProvider("datagen_contract:unsafe",
                output -> output.writeUtf8("../escape.txt", "bad"));
        require(!traversal.run().committed()
                        && !Files.exists(outputRoot.resolve("escape.txt")),
                "unsafe generated path escaped its transaction root");
    }

    private static void verifyGraphFailures(Path outputRoot) throws Exception {
        AtomicInteger calls = new AtomicInteger();
        ModDataGenerator missing = new ModDataGenerator(outputRoot.resolve("missing"),
                "datagen_contract");
        missing.addProvider("datagen_contract:waiting", output -> calls.incrementAndGet(),
                "datagen_contract:not_registered");
        DataGenerationReport missingReport = missing.run();
        require(!missingReport.committed() && calls.get() == 0
                        && missingReport.providers().get(0).status()
                                == DataProviderStatus.BLOCKED,
                "missing provider dependency was executed");

        ModDataGenerator cycle = new ModDataGenerator(outputRoot.resolve("cycle"),
                "datagen_contract");
        cycle.addProvider("datagen_contract:a", output -> calls.incrementAndGet(),
                "datagen_contract:b");
        cycle.addProvider("datagen_contract:b", output -> calls.incrementAndGet(),
                "datagen_contract:a");
        DataGenerationReport cycleReport = cycle.run();
        require(!cycleReport.committed()
                        && cycleReport.providers().stream().allMatch(result ->
                                result.status() == DataProviderStatus.BLOCKED)
                        && calls.get() == 0,
                "cyclic providers were scheduled");
    }

    private static void verifyTypedProviders(Path outputRoot) throws Exception {
        RegistryKey<String> registry = RegistryKey.of(
                "datagen_contract:modes", String.class);
        ModDataGenerator generator = new ModDataGenerator(outputRoot, "datagen_contract");
        generator.addProvider("datagen_contract:language",
                new LanguageDataProvider("datagen_contract", "zh-CN") {
                    @Override protected void generateTranslations(LanguageBuilder translations) {
                        translations.add("z_key", "最后")
                                .add("datagen_contract:a_key", "值=一\n二");
                    }
                });
        generator.addProvider("datagen_contract:tags",
                new RegistryTagDataProvider<String>(registry, "datagen_contract") {
                    @Override protected void generateTags(TagLookup<String> tags) {
                        tags.tag("datagen_contract:interactive")
                                .condition(ResourceConditionJson.allModsLoaded(
                                        "datagen_contract"))
                                .add(Identifier.parse("datagen_contract:primary"))
                                .addTag(Identifier.parse("datagen_contract:base"))
                                .addOptional(Identifier.parse("addon:optional"));
                    }
                }, "datagen_contract:language");

        DataGenerationReport report = generator.run().requireSuccess();
        require(report.generatedResourceCount() == 2,
                "typed providers generated the wrong resource count");
        String language = Files.readString(outputRoot.resolve(
                "assets/datagen_contract/lang/zh_cn.properties"));
        require(language.equals("a_key=值\\=一\\n二\nz_key=最后\n"),
                "language provider output was not sorted/escaped UTF-8 properties: " + language);

        Path tagPath = outputRoot.resolve(
                "data/datagen_contract/tags/datagen_contract/modes/interactive.json");
        JsonObject tag = JsonParser.parseString(Files.readString(tagPath)).getAsJsonObject();
        require(tag.get("replace").isJsonPrimitive()
                        && !tag.get("replace").getAsBoolean()
                        && tag.getAsJsonArray("values").size() == 3
                        && tag.getAsJsonArray("rusted_fabric:load_conditions").size() == 1
                        && tag.getAsJsonArray("values").get(2).getAsJsonObject()
                                .get("required").getAsBoolean() == false,
                "registry tag provider emitted an incompatible JSON contract");
    }

    private static void expectIllegal(Runnable action, String message) {
        try {
            action.run();
            throw new AssertionError(message);
        } catch (IllegalStateException expected) {
            // Expected.
        }
    }

    private static void deleteTempTree(Path root) {
        try {
            Path normalized = root.toAbsolutePath().normalize();
            Path temp = Path.of(System.getProperty("java.io.tmpdir")).toAbsolutePath().normalize();
            if (!normalized.startsWith(temp)
                    || !normalized.getFileName().toString()
                            .startsWith("rustedfabric-datagen-contract-")) {
                throw new AssertionError("refusing to delete unexpected datagen path " + normalized);
            }
            try (Stream<Path> paths = Files.walk(normalized)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception failure) {
                        throw new RuntimeException(failure);
                    }
                });
            }
        } catch (Exception failure) {
            throw new AssertionError("could not clean datagen contract directory", failure);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
