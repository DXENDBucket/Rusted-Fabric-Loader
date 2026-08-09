package io.github.endx.rustedfabricexample;

import java.nio.file.Path;

import io.github.endx.rustedfabricapi.api.datagen.DataGenerationReport;
import io.github.endx.rustedfabricapi.api.datagen.ModDataGenerator;
import io.github.endx.rustedfabricapi.api.datagen.ResourceConditionJson;
import io.github.endx.rustedfabricapi.api.datagen.provider.LanguageBuilder;
import io.github.endx.rustedfabricapi.api.datagen.provider.LanguageDataProvider;
import io.github.endx.rustedfabricapi.api.datagen.provider.RegistryTagDataProvider;
import io.github.endx.rustedfabricapi.api.registry.RegistryKey;
import io.github.endx.rustedfabricapi.api.util.Identifier;

/** Build-time example; this source set is not packaged into the runtime mod Jar. */
public final class ExampleDataGeneration {
    private static final RegistryKey<String> MODES = RegistryKey.of(
            "rusted_fabric_example:modes", String.class);

    private ExampleDataGeneration() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Expected the generated-resource output directory");
        }
        ModDataGenerator generator = new ModDataGenerator(
                Path.of(args[0]), "rusted_fabric_example");
        generator.addProvider("rusted_fabric_example:zh_cn_language",
                new LanguageDataProvider("rusted_fabric_example", "zh_cn") {
                    @Override protected void generateTranslations(LanguageBuilder translations) {
                        translations.add("loader_ready", "类型化 API 资源已为 {0} 加载")
                                .add("language_reloaded", "语言资源已重新加载：{0}")
                                .add("datagen_notice", "此文本由普通 Java datagen 生成");
                    }
                });
        generator.addProvider("rusted_fabric_example:generated_mode_tags",
                new RegistryTagDataProvider<String>(MODES, "rusted_fabric_example") {
                    @Override protected void generateTags(TagLookup<String> tags) {
                        tags.tag("rusted_fabric_example:generated_tools")
                                .condition(ResourceConditionJson.allModsLoaded(
                                        "rusted_fabric_example"))
                                .add(Identifier.parse("rusted_fabric_example:inspection"))
                                .addOptional(Identifier.parse("optionaladdon:advanced_inspection"));
                    }
                }, "rusted_fabric_example:zh_cn_language");

        DataGenerationReport report = generator.run().requireSuccess();
        System.out.println("Generated " + report.generatedResourceCount()
                + " resources: " + report.writtenPaths().size() + " written, "
                + report.unchangedPaths().size() + " unchanged");
    }
}
