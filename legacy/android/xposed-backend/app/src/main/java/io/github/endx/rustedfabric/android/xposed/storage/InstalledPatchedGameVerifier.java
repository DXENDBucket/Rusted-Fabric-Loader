package io.github.endx.rustedfabric.android.xposed.storage;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;

import java.security.MessageDigest;
import java.security.cert.X509Certificate;

import io.github.endx.rustedfabric.android.patcher.PatchProfile;
import io.github.endx.rustedfabric.android.xposed.patch.LocalPatchKeyStore;

/** Verifies the side-by-side package by exact package, entrypoint, and the on-device signer. */
public final class InstalledPatchedGameVerifier {
    private InstalledPatchedGameVerifier() {
    }

    @SuppressWarnings("deprecation")
    public static Result verify(Context context) {
        X509Certificate localCertificate = LocalPatchKeyStore.loadCertificate();
        if (localCertificate == null) return new Result(false, "LOCAL_SIGNER_UNAVAILABLE");
        try {
            int flags = Build.VERSION.SDK_INT >= 28
                    ? PackageManager.GET_SIGNING_CERTIFICATES : PackageManager.GET_SIGNATURES;
            PackageInfo info = context.getPackageManager().getPackageInfo(
                    PatchProfile.DEFAULT_CLONE_PACKAGE, flags);
            ApplicationInfo application = info.applicationInfo;
            if (application == null || !PatchProfile.PATCHED_APPLICATION.equals(
                    application.className)) {
                return new Result(false, "PATCH_ENTRYPOINT_MISMATCH");
            }
            byte[] expected = sha256(localCertificate.getEncoded());
            Signature[] signatures;
            if (Build.VERSION.SDK_INT >= 28) {
                signatures = info.signingInfo == null ? null
                        : info.signingInfo.getApkContentsSigners();
            } else {
                signatures = info.signatures;
            }
            if (signatures == null || signatures.length != 1
                    || !MessageDigest.isEqual(expected, sha256(signatures[0].toByteArray()))) {
                return new Result(false, "PATCH_SIGNER_MISMATCH");
            }
            return new Result(true, "VERIFIED_LOCAL_PATCH");
        } catch (PackageManager.NameNotFoundException missing) {
            return new Result(false, "PATCH_NOT_INSTALLED");
        } catch (Exception failure) {
            return new Result(false, "PATCH_VERIFICATION_FAILED");
        }
    }

    private static byte[] sha256(byte[] bytes) throws Exception {
        return MessageDigest.getInstance("SHA-256").digest(bytes);
    }

    public static final class Result {
        private final boolean verified;
        private final String status;

        private Result(boolean verified, String status) {
            this.verified = verified;
            this.status = status;
        }

        public boolean isVerified() { return verified; }
        public String getStatus() { return status; }
    }
}
