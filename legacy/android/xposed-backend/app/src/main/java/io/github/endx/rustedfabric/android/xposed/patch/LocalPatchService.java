package io.github.endx.rustedfabric.android.xposed.patch;

import android.content.Context;
import android.net.Uri;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import io.github.endx.rustedfabric.android.patcher.LocalApkPatcher;
import io.github.endx.rustedfabric.android.patcher.LocalApkSigner;
import io.github.endx.rustedfabric.android.patcher.PatchProfile;
import io.github.endx.rustedfabric.android.patcher.PatchReport;
import io.github.endx.rustedfabric.android.patcher.PatchRequest;

/** Complete user-local pipeline: copy, verify, patch, sign, verify, and enqueue install. */
public final class LocalPatchService {
    private static final String BOOTSTRAP_ASSET = "rusted-fabric/patched-bootstrap.dex";

    private LocalPatchService() {
    }

    public static Result patchAndEnqueue(Context context, Uri source) throws Exception {
        return patchAndEnqueue(context, source, stage -> { });
    }

    public static Result patchAndEnqueue(Context context, Uri source,
                                         ProgressListener progress) throws Exception {
        if (progress == null) throw new NullPointerException("progress");
        progress.onStage(Stage.PREPARING);
        Path work = context.getCacheDir().toPath().resolve("local-patcher");
        Files.createDirectories(work);
        Path input = work.resolve("user-input.apk");
        Path bootstrap = work.resolve("patched-bootstrap.dex");
        Path unsigned = work.resolve("patched-unsigned.apk");
        Path signed = work.resolve("rusted-fabric-patched.apk");
        try {
            progress.onStage(Stage.COPYING_SOURCE);
            copyUri(context, source, input, LocalApkPatcher.MAX_SOURCE_APK_BYTES);
            copyAsset(context, bootstrap);
            progress.onStage(Stage.VERIFYING_AND_WEAVING);
            PatchReport report = new LocalApkPatcher().patchUnsigned(new PatchRequest(
                    input, bootstrap, unsigned, PatchProfile.officialAndroid115(),
                    PatchProfile.DEFAULT_CLONE_PACKAGE));
            progress.onStage(Stage.SIGNING);
            LocalPatchKeyStore.SigningIdentity identity = LocalPatchKeyStore.loadOrCreate();
            LocalApkSigner.SigningResult signature = new LocalApkSigner().sign(
                    unsigned, signed, "Rusted Fabric local patch",
                    identity.getPrivateKey(), identity.getCertificate());
            report = report.withSignedOutput(signature.getApkSha256());
            Files.write(context.getFilesDir().toPath().resolve("last-local-patch-report.json"),
                    report.toJson().getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            progress.onStage(Stage.REQUESTING_INSTALL);
            int sessionId = LocalPatchInstaller.enqueue(context, signed);
            progress.onStage(Stage.COMPLETE);
            return new Result(sessionId, signature.getCertificateSha256());
        } finally {
            Files.deleteIfExists(input);
            Files.deleteIfExists(bootstrap);
            Files.deleteIfExists(unsigned);
            Files.deleteIfExists(signed);
        }
    }

    public enum Stage {
        PREPARING,
        COPYING_SOURCE,
        VERIFYING_AND_WEAVING,
        SIGNING,
        REQUESTING_INSTALL,
        COMPLETE
    }

    @FunctionalInterface
    public interface ProgressListener {
        void onStage(Stage stage);
    }

    private static void copyUri(Context context, Uri source, Path target, long limit)
            throws Exception {
        try (InputStream input = context.getContentResolver().openInputStream(source)) {
            if (input == null) throw new IllegalArgumentException("Selected APK cannot be opened");
            copyBounded(input, target, limit);
        }
    }

    private static void copyAsset(Context context, Path target) throws Exception {
        try (InputStream input = context.getAssets().open(BOOTSTRAP_ASSET)) {
            copyBounded(input, target, LocalApkPatcher.MAX_BOOTSTRAP_DEX_BYTES);
        }
    }

    private static void copyBounded(InputStream input, Path target, long limit) throws Exception {
        Files.deleteIfExists(target);
        try (OutputStream output = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW)) {
            byte[] buffer = new byte[64 * 1024];
            long total = 0;
            for (int count = input.read(buffer); count >= 0; count = input.read(buffer)) {
                if (count == 0) continue;
                total += count;
                if (total > limit) throw new IllegalArgumentException("Selected file is too large");
                output.write(buffer, 0, count);
            }
            if (total == 0) throw new IllegalArgumentException("Selected file is empty");
        } catch (Exception failure) {
            Files.deleteIfExists(target);
            throw failure;
        }
    }

    public static final class Result {
        private final int sessionId;
        private final String certificateSha256;

        private Result(int sessionId, String certificateSha256) {
            this.sessionId = sessionId;
            this.certificateSha256 = certificateSha256;
        }

        public int getSessionId() { return sessionId; }
        public String getCertificateSha256() { return certificateSha256; }
    }
}
