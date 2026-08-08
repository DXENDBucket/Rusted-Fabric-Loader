package io.github.endx.rustedfabricapi.api.asset;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import io.github.endx.rustedfabricapi.api.text.LanguageEvents;
import io.github.endx.rustedfabricapi.api.text.Translations;
import rustedwarfare.core.LanguageSettings;

/** Classpath resource, extraction, translation fallback, and event checks. */
public final class AssetTextContractVerification {
    private AssetTextContractVerification() {
    }

    public static void verify() {
        ModResourcePack pack = ModResources.forClass("contract_mod",
                AssetTextContractVerification.class);
        verifyResourceSafety(pack);
        verifyResourceDiscovery();
        verifyExtraction(pack);
        verifyTranslations(pack);
        verifyEvents(pack);
    }

    private static void verifyResourceSafety(ModResourcePack pack) {
        try {
            require(pack.resource("assets/contract/lang/en.properties").exists(),
                    "classpath mod resource was not found");
            require(pack.resource("assets/contract/lang/en.properties").readUtf8()
                            .contains("Hello {0}"),
                    "UTF-8 mod resource read changed content");
        } catch (IOException exception) {
            throw new AssertionError("could not read classpath mod resource", exception);
        }
        try {
            pack.resource("assets/../escape.txt");
            throw new AssertionError("resource traversal path was accepted");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }

    private static void verifyExtraction(ModResourcePack pack) {
        try {
            ModResource resource = pack.resource("assets/contract/lang/zh.properties");
            Path first = resource.extractToCache().orElseThrow(AssertionError::new);
            Path second = resource.extractToCache().orElseThrow(AssertionError::new);
            require(first.equals(second), "content-addressed extraction path was unstable");
            require(first.startsWith(ModResources.cacheRoot()) && Files.isRegularFile(first),
                    "extracted mod resource escaped or was not created");
            require(Files.readString(first).contains("你好"),
                    "extracted UTF-8 resource changed content");
        } catch (IOException exception) {
            throw new AssertionError("could not extract classpath mod resource", exception);
        }
    }

    private static void verifyResourceDiscovery() {
        ModResourcePack pack = new ModResourcePack("discovery_mod", path -> {
            String value = path.toString().replace('\\', '/');
            if (!value.endsWith(".properties")) return Optional.empty();
            InputStream input = new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
            return Optional.of(input);
        }, prefix -> Arrays.asList(
                Path.of("assets/discovery/lang/zh.properties"),
                Path.of("assets/discovery/lang/en.properties"),
                Path.of("assets/discovery/lang/readme.txt")));
        require(pack.supportsDiscovery(), "enumerable resource pack reported no discovery support");
        try {
            List<ModResource> found = pack.find("assets/discovery/lang",
                    path -> path.endsWith(".properties"));
            require(found.size() == 2
                            && found.get(0).relativePath().toString().replace('\\', '/')
                                    .endsWith("en.properties")
                            && found.get(1).relativePath().toString().replace('\\', '/')
                                    .endsWith("zh.properties"),
                    "resource discovery was not filtered and path-sorted");
        } catch (IOException exception) {
            throw new AssertionError("resource discovery failed", exception);
        }

        ModResourcePack directOnly = ModResources.forClass("direct_only",
                AssetTextContractVerification.class);
        require(!directOnly.supportsDiscovery(),
                "class-loader resource pack incorrectly promised discovery support");
        try {
            directOnly.find("assets/contract");
            throw new AssertionError("direct-only resource pack accepted discovery");
        } catch (UnsupportedOperationException expected) {
            // Expected.
        } catch (IOException exception) {
            throw new AssertionError("direct-only discovery reported the wrong failure", exception);
        }
    }

    private static void verifyTranslations(ModResourcePack pack) {
        String previous = LanguageSettings.forcedLanguage;
        Translations.Registration registration = Translations.register("contract", pack);
        try {
            LanguageSettings.forcedLanguage = "zh";
            Translations.invalidateCaches();
            require("你好 水桶".equals(Translations.translate("contract:greeting", "水桶")),
                    "locale-specific translation or formatting failed");
            require("English fallback".equals(Translations.translate("contract:fallback")),
                    "English translation fallback failed");
            require("missing value".equals(Translations.translateOr(
                            "contract:missing", "missing {0}", "value")),
                    "explicit translation fallback failed");
            require(Translations.contains(io.github.endx.rustedfabricapi.api.util.Identifier.of(
                            "contract", "greeting")),
                    "registered translation key was not discoverable");
        } finally {
            LanguageSettings.forcedLanguage = previous;
            Translations.invalidateCaches();
            registration.close();
        }
        require(!Translations.registeredNamespaces().contains("contract"),
                "translation registration did not unregister");
    }

    private static void verifyEvents(ModResourcePack pack) {
        AtomicInteger calls = new AtomicInteger();
        RustedFabricEvent.Registration first = ModResourceEvents.BEFORE_EXTRACT.subscribe(resource -> {
            calls.incrementAndGet();
            return false;
        });
        RustedFabricEvent.Registration second = ModResourceEvents.BEFORE_EXTRACT.subscribe(resource -> {
            calls.incrementAndGet();
            return true;
        });
        try {
            require(!pack.resource("assets/contract/lang/en.properties")
                            .extractToCache().isPresent(),
                    "cancelled mod resource extraction returned a path");
        } catch (IOException exception) {
            throw new AssertionError("cancelled extraction performed I/O", exception);
        }
        require(calls.get() == 2, "resource extraction cancellation skipped a listener");
        first.close();
        second.close();

        StringBuilder languages = new StringBuilder();
        RustedFabricEvent.Registration languageFirst = LanguageEvents.AFTER_RELOAD.subscribe(
                language -> languages.append('1').append(language));
        RustedFabricEvent.Registration languageSecond = LanguageEvents.AFTER_RELOAD.subscribe(
                language -> languages.append('2').append(language));
        LanguageEvents.AFTER_RELOAD.invoker().onReload("zh_cn");
        languageFirst.close();
        languageSecond.close();
        require("1zh_cn2zh_cn".contentEquals(languages),
                "language reload listeners did not preserve registration order");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
