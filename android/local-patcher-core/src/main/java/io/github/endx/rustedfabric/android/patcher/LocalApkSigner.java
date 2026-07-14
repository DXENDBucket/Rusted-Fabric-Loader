package io.github.endx.rustedfabric.android.patcher;

import com.android.apksig.ApkSigner;
import com.android.apksig.ApkVerifier;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;

/** Signs an already aligned local patch with a persistent user-owned key. */
public final class LocalApkSigner {
    public SigningResult sign(Path unsignedApk, Path outputApk, String signerName,
                              PrivateKey privateKey, X509Certificate certificate)
            throws PatchException {
        Objects.requireNonNull(unsignedApk, "unsignedApk");
        Objects.requireNonNull(outputApk, "outputApk");
        Objects.requireNonNull(privateKey, "privateKey");
        Objects.requireNonNull(certificate, "certificate");
        if (!Files.isRegularFile(unsignedApk)) {
            throw new PatchException(PatchException.Reason.INPUT_MISSING,
                    "Unsigned patched APK is missing");
        }
        Path output = outputApk.toAbsolutePath().normalize();
        Path parent = output.getParent();
        if (parent == null) {
            throw new PatchException(PatchException.Reason.OUTPUT_FAILED,
                    "Signed output APK must have a parent directory");
        }
        Path temporary = parent.resolve("." + output.getFileName() + "."
                + UUID.randomUUID() + ".signed.tmp");
        try {
            Files.createDirectories(parent);
            ApkSigner.SignerConfig config = new ApkSigner.SignerConfig.Builder(
                    signerName == null || signerName.trim().isEmpty()
                            ? "Rusted Fabric local patch" : signerName,
                    privateKey, Collections.singletonList(certificate)).build();
            new ApkSigner.Builder(Collections.singletonList(config))
                    .setInputApk(unsignedApk.toFile())
                    .setOutputApk(temporary.toFile())
                    .setV1SigningEnabled(true)
                    .setV2SigningEnabled(true)
                    .setV3SigningEnabled(true)
                    .setOtherSignersSignaturesPreserved(false)
                    .build()
                    .sign();
            ApkVerifier.Result verification = new ApkVerifier.Builder(temporary.toFile())
                    .build().verify();
            if (!verification.isVerified()) {
                throw new PatchException(PatchException.Reason.SIGNATURE_INVALID,
                        "Signed APK did not pass apksig verification");
            }
            move(temporary, output);
            return new SigningResult(PatcherSha256.digest(output),
                    PatcherSha256.digest(certificate.getEncoded()),
                    verification.isVerifiedUsingV1Scheme(),
                    verification.isVerifiedUsingV2Scheme(),
                    verification.isVerifiedUsingV3Scheme());
        } catch (PatchException expected) {
            throw expected;
        } catch (Exception failure) {
            throw new PatchException(PatchException.Reason.SIGNING_FAILED,
                    "Patched APK signing failed", failure);
        } finally {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException ignored) {
                // Keep the primary result; stale temporary files are harmless and app-private.
            }
        }
    }

    private static void move(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException unsupported) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static final class SigningResult {
        private final String apkSha256;
        private final String certificateSha256;
        private final boolean v1;
        private final boolean v2;
        private final boolean v3;

        private SigningResult(String apkSha256, String certificateSha256,
                              boolean v1, boolean v2, boolean v3) {
            this.apkSha256 = apkSha256;
            this.certificateSha256 = certificateSha256;
            this.v1 = v1;
            this.v2 = v2;
            this.v3 = v3;
        }

        public String getApkSha256() { return apkSha256; }
        public String getCertificateSha256() { return certificateSha256; }
        public boolean isV1() { return v1; }
        public boolean isV2() { return v2; }
        public boolean isV3() { return v3; }
    }
}
