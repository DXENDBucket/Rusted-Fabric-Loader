package io.github.endx.rustedfabricmodmenu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import io.github.endx.rustedfabricapi.api.client.screen.ClientScreens;
import io.github.endx.rustedfabricapi.api.client.screen.ListScreenEntry;
import io.github.endx.rustedfabricapi.api.client.screen.ListScreenSpec;
import io.github.endx.rustedfabricapi.api.client.screen.MainMenuButton;
import io.github.endx.rustedfabricapi.api.client.screen.MainMenuButtons;
import io.github.endx.rustedfabricapi.api.text.Translations;
import io.github.endx.rustedfabricapi.api.util.Identifier;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.Person;

/** Official client-side Java mod list for Rusted Fabric Loader. */
public final class RustedFabricModMenu implements ClientModInitializer {
    public static final String MOD_ID = "rustedfabricmodmenu";

    @Override
    public void onInitializeClient() {
        Translations.registerMod(MOD_ID);
        MainMenuButtons.register(MainMenuButton.dynamic(
                Identifier.of(MOD_ID, "open"),
                () -> tr("menu.button", "Java Mods"),
                RustedFabricModMenu::open));
    }

    private static void open() {
        List<ModContainer> mods = new ArrayList<ModContainer>(
                FabricLoader.getInstance().getAllMods());
        Collections.sort(mods, Comparator
                .comparing((ModContainer mod) -> displayName(mod.getMetadata()),
                        String.CASE_INSENSITIVE_ORDER)
                .thenComparing(mod -> mod.getMetadata().getId()));

        ListScreenSpec.Builder page = ListScreenSpec.builder(tr("screen.title", "Java Mods"))
                .summary(tr("screen.summary", "{0} loaded Java mods", mods.size()))
                .emptyMessage(tr("screen.empty", "No Java mods are loaded"))
                .backButton(tr("screen.back", "Back"))
                .filter(tr("screen.filter", "Filter:"));
        for (ModContainer container : mods) {
            ModMetadata metadata = container.getMetadata();
            String details = tr("entry.details", "Version {0} - ID: {1}",
                    metadata.getVersion().getFriendlyString(), metadata.getId());
            String authors = authorNames(metadata);
            String description = clean(metadata.getDescription());
            if (!authors.isEmpty()) {
                String by = tr("entry.authors", "Authors: {0}", authors);
                description = description.isEmpty() ? by : by + "\n" + description;
            }
            page.add(ListScreenEntry.of(displayName(metadata), details, description));
        }
        ClientScreens.openList(page.build());
    }

    private static String displayName(ModMetadata metadata) {
        String name = clean(metadata.getName());
        return name.isEmpty() ? metadata.getId() : name;
    }

    private static String authorNames(ModMetadata metadata) {
        return metadata.getAuthors().stream().map(Person::getName).map(RustedFabricModMenu::clean)
                .filter(value -> !value.isEmpty()).collect(Collectors.joining(", "));
    }

    private static String clean(String value) {
        return value != null ? value.trim() : "";
    }

    private static String tr(String path, String fallback, Object... arguments) {
        return Translations.translateOr(Identifier.of(MOD_ID, path), fallback, arguments);
    }
}
