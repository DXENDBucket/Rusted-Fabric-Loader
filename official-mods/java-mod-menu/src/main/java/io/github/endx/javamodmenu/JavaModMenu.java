package io.github.endx.javamodmenu;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Path;
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
import io.github.endx.rustedfabricapi.api.asset.reload.ResourceReloadReport;
import io.github.endx.rustedfabricapi.api.development.DevelopmentWorkspaces;
import io.github.endx.rustedfabricapi.api.development.DevelopmentReloads;
import io.github.endx.rustedfabricapi.api.text.Translations;
import io.github.endx.rustedfabricapi.api.util.Identifier;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.Person;

/** Official client-side Java mod list for Rusted Fabric Loader. */
public final class JavaModMenu implements ClientModInitializer {
    public static final String MOD_ID = "java_mod_menu";
    private static String lastReloadSummary = "";

    @Override
    public void onInitializeClient() {
        Translations.registerMod(MOD_ID);
        MainMenuButtons.register(MainMenuButton.dynamic(
                Identifier.of(MOD_ID, "open"),
                () -> tr("menu.button", "Java Mods"),
                JavaModMenu::open));
    }

    private static void open() {
        List<ModContainer> mods = new ArrayList<ModContainer>(
                FabricLoader.getInstance().getAllMods());
        Collections.sort(mods, Comparator
                .comparing((ModContainer mod) -> displayName(mod.getMetadata()),
                        String.CASE_INSENSITIVE_ORDER)
                .thenComparing(mod -> mod.getMetadata().getId()));

        String summary = tr("screen.summary", "{0} loaded Java mods", mods.size());
        int workspaces = DevelopmentWorkspaces.loaded().size();
        if (workspaces > 0) {
            summary += "\n" + tr("screen.workspaces", "{0} development workspaces: {1}",
                    workspaces, DevelopmentWorkspaces.root().map(Path::toString).orElse(""));
        }
        if (!lastReloadSummary.isEmpty()) summary += "\n" + lastReloadSummary;
        ListScreenSpec.Builder page = ListScreenSpec.builder(tr("screen.title", "Java Mods"))
                .summary(summary)
                .emptyMessage(tr("screen.empty", "No Java mods are loaded"))
                .backButton(tr("screen.back", "Back"))
                .filter(tr("screen.filter", "Filter:"))
                .action(tr("screen.reload", "Reload units/resources"),
                        JavaModMenu::reloadResources);
        if (canOpenWorkspaceRoot()) {
            page.action(tr("screen.open_workspace", "Open workspace"),
                    JavaModMenu::openWorkspaceRoot);
        }
        for (ModContainer container : mods) {
            ModMetadata metadata = container.getMetadata();
            String details = tr("entry.details", "Version {0} - ID: {1}",
                    metadata.getVersion().getFriendlyString(), metadata.getId());
            if (DevelopmentWorkspaces.forMod(metadata.getId()).isPresent()) {
                details += " - " + tr("entry.workspace", "Development workspace");
            }
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

    private static void reloadResources() {
        try {
            ResourceReloadReport report = DevelopmentReloads.reloadInPlace();
            lastReloadSummary = report.successful()
                    ? tr("screen.reload_success", "Reloaded {0} resource listeners",
                            report.listenerCount())
                    : tr("screen.reload_failed", "Reload finished with {0} failures",
                            report.failureCount());
        } catch (RuntimeException failure) {
            lastReloadSummary = tr("screen.reload_error", "Reload failed: {0}",
                    clean(failure.getMessage()));
        }
        open();
    }

    private static boolean canOpenWorkspaceRoot() {
        if (System.getProperty("rustedfabric.platform", "")
                .toLowerCase(java.util.Locale.ROOT).contains("android")) return false;
        return DevelopmentWorkspaces.root().isPresent() && Desktop.isDesktopSupported();
    }

    private static void openWorkspaceRoot() {
        Path root = DevelopmentWorkspaces.root().orElse(null);
        if (root == null || !Desktop.isDesktopSupported()) return;
        try {
            Desktop.getDesktop().open(root.toFile());
        } catch (IOException | UnsupportedOperationException failure) {
            lastReloadSummary = tr("screen.open_failed", "Could not open workspace: {0}",
                    clean(failure.getMessage()));
            open();
        }
    }

    private static String displayName(ModMetadata metadata) {
        String name = clean(metadata.getName());
        return name.isEmpty() ? metadata.getId() : name;
    }

    private static String authorNames(ModMetadata metadata) {
        return metadata.getAuthors().stream().map(Person::getName).map(JavaModMenu::clean)
                .filter(value -> !value.isEmpty()).collect(Collectors.joining(", "));
    }

    private static String clean(String value) {
        return value != null ? value.trim() : "";
    }

    private static String tr(String path, String fallback, Object... arguments) {
        return Translations.translateOr(Identifier.of(MOD_ID, path), fallback, arguments);
    }
}
