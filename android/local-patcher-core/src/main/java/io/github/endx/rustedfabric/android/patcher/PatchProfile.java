package io.github.endx.rustedfabric.android.patcher;

import java.util.Objects;
import java.util.Locale;

public final class PatchProfile {
    public static final String DEFAULT_CLONE_PACKAGE = "io.github.endx.rwpatch";
    public static final String PATCHED_APPLICATION =
            "io.github.endx.rustedfabric.android.patched.PatchedApplication";

    private final String id;
    private final String sourceSha256;
    private final String sourcePackage;
    private final String sourceApplication;
    private final String sourceProviderAuthority;

    public PatchProfile(String id, String sourceSha256, String sourcePackage,
                        String sourceApplication, String sourceProviderAuthority) {
        this.id = Objects.requireNonNull(id, "id");
        this.sourceSha256 = Objects.requireNonNull(sourceSha256, "sourceSha256")
                .toLowerCase(Locale.ROOT);
        this.sourcePackage = Objects.requireNonNull(sourcePackage, "sourcePackage");
        this.sourceApplication = Objects.requireNonNull(sourceApplication, "sourceApplication");
        this.sourceProviderAuthority = Objects.requireNonNull(
                sourceProviderAuthority, "sourceProviderAuthority");
    }

    public static PatchProfile officialAndroid115() {
        return new PatchProfile("rw-android-1.15-code176-v1.0",
                "328f37106985a2ba424efec9ac312ede0395f3bac56e3d5db5d642dd6aecc04c",
                "com.corrodinggames.rts",
                "com.corrodinggames.rts.appFramework.RWApplication",
                "com.corrodinggames.rts.fileProvider");
    }

    public String getId() { return id; }
    public String getSourceSha256() { return sourceSha256; }
    public String getSourcePackage() { return sourcePackage; }
    public String getSourceApplication() { return sourceApplication; }
    public String getSourceProviderAuthority() { return sourceProviderAuthority; }
}
