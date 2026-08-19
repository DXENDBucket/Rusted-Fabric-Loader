package io.github.endx.rustedfabric.android.launcher.jvm;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import io.github.endx.rustedfabric.android.jvm.ManagedContentLibrary;

/** Installs APK-owned official mods without bundling or modifying the game itself. */
public final class OfficialModProvisioner {
    private static final OfficialMod[] MODS = {
            new OfficialMod("rusted-fabric-api.jar", "rusted_fabric_api", true),
            new OfficialMod("java-mod-menu.jar", "java_mod_menu", true),
            new OfficialMod("vulkan-mod.jar", "vulkan_mod", true),
            new OfficialMod("ini-essentials.jar", "ini_essentials", false),
            new OfficialMod("performance-profiler.jar", "performance_profiler", false)
    };

    private OfficialModProvisioner() {
    }

    public static void provision(Context context) throws IOException {
        File gameRoot = DesktopGameImportService.importedRoot(context);
        if (!gameRoot.isDirectory()) throw new IOException("Desktop game is not imported");
        File cache = new File(context.getCacheDir(), "official-mods");
        if (!cache.isDirectory() && !cache.mkdirs()) {
            throw new IOException("Cannot prepare official mod assets");
        }
        for (OfficialMod mod : MODS) {
            File source = new File(cache, mod.asset);
            installAsset(context, "rusted-fabric/official-mods/" + mod.asset, source);
            ManagedContentLibrary.provisionOfficialJavaMod(gameRoot.toPath(), source.toPath(),
                    mod.id, mod.defaultEnabled);
        }
    }

    private static void installAsset(Context context, String asset, File target) throws IOException {
        File staging = new File(target.getParentFile(), target.getName() + ".importing");
        Files.deleteIfExists(staging.toPath());
        try (InputStream input = context.getAssets().open(asset);
             FileOutputStream output = new FileOutputStream(staging)) {
            byte[] buffer = new byte[32 * 1024];
            int read;
            while ((read = input.read(buffer)) >= 0) output.write(buffer, 0, read);
        }
        Files.move(staging.toPath(), target.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    private static final class OfficialMod {
        final String asset;
        final String id;
        final boolean defaultEnabled;

        OfficialMod(String asset, String id, boolean defaultEnabled) {
            this.asset = asset;
            this.id = id;
            this.defaultEnabled = defaultEnabled;
        }
    }
}
