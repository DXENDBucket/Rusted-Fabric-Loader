package io.github.endx.rustedfabric.android.mod;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class VerifiedModArchive {
    private final Path archivePath;
    private final RustedFabricModMetadata metadata;
    private final String archiveSha256;
    private final String dexSha256;
    private final Set<String> definedClasses;

    VerifiedModArchive(Path archivePath, RustedFabricModMetadata metadata, String archiveSha256,
                       String dexSha256, Set<String> definedClasses) {
        this.archivePath = archivePath;
        this.metadata = metadata;
        this.archiveSha256 = archiveSha256;
        this.dexSha256 = dexSha256;
        this.definedClasses = Collections.unmodifiableSet(new LinkedHashSet<>(definedClasses));
    }

    public Path getArchivePath() {
        return archivePath;
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

    public Set<String> getDefinedClasses() {
        return definedClasses;
    }
}
