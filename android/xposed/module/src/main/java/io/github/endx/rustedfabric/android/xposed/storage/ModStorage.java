package io.github.endx.rustedfabric.android.xposed.storage;

import android.content.Context;

import java.nio.file.Path;

import io.github.endx.rustedfabric.android.mod.ModRegistry;

public final class ModStorage {
    private ModStorage() {
    }

    public static ModRegistry registry(Context context) {
        Path root = context.getFilesDir().toPath().resolve("rusted-fabric").resolve("mods");
        return new ModRegistry(root);
    }
}
