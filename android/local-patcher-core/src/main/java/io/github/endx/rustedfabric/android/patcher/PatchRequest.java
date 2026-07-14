package io.github.endx.rustedfabric.android.patcher;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;

public final class PatchRequest {
    private final Path sourceApk;
    private final Path bootstrapDex;
    private final Path outputApk;
    private final PatchProfile profile;
    private final String clonePackage;

    public PatchRequest(Path sourceApk, Path bootstrapDex, Path outputApk,
                        PatchProfile profile, String clonePackage) {
        this.sourceApk = Objects.requireNonNull(sourceApk, "sourceApk");
        this.bootstrapDex = Objects.requireNonNull(bootstrapDex, "bootstrapDex");
        this.outputApk = Objects.requireNonNull(outputApk, "outputApk");
        this.profile = Objects.requireNonNull(profile, "profile");
        this.clonePackage = Objects.requireNonNull(clonePackage, "clonePackage");
        if (!clonePackage.matches("[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+")) {
            throw new IllegalArgumentException("Invalid clone package name");
        }
        if (profile.getSourcePackage().getBytes(StandardCharsets.UTF_8).length
                != clonePackage.getBytes(StandardCharsets.UTF_8).length) {
            throw new IllegalArgumentException(
                    "Local patch v1 requires an equal-width clone package name");
        }
    }

    public Path getSourceApk() { return sourceApk; }
    public Path getBootstrapDex() { return bootstrapDex; }
    public Path getOutputApk() { return outputApk; }
    public PatchProfile getProfile() { return profile; }
    public String getClonePackage() { return clonePackage; }
    public String cloneProviderAuthority() { return clonePackage + ".fileProvider"; }
}
