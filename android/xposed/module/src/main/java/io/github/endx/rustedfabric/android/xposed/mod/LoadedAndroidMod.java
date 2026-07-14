package io.github.endx.rustedfabric.android.xposed.mod;

import io.github.endx.rustedfabric.android.mod.RustedFabricModMetadata;
import io.github.endx.rustedfabricapi.api.RustedFabricModEntrypoint;

public final class LoadedAndroidMod {
    private final RustedFabricModMetadata metadata;
    private final String archiveSha256;
    private final String dexSha256;
    private final RustedFabricModEntrypoint entrypoint;
    private final ClassLoader classLoader;

    LoadedAndroidMod(RustedFabricModMetadata metadata, String archiveSha256, String dexSha256,
                     RustedFabricModEntrypoint entrypoint, ClassLoader classLoader) {
        this.metadata = metadata;
        this.archiveSha256 = archiveSha256;
        this.dexSha256 = dexSha256;
        this.entrypoint = entrypoint;
        this.classLoader = classLoader;
    }

    public RustedFabricModMetadata getMetadata() {
        return metadata;
    }

    public String getArchiveSha256() {
        return archiveSha256;
    }

    public String getDexSha256() {
        return dexSha256;
    }

    public RustedFabricModEntrypoint getEntrypoint() {
        return entrypoint;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }
}
