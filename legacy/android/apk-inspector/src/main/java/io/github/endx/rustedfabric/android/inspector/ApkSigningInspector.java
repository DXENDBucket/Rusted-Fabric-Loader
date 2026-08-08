package io.github.endx.rustedfabric.android.inspector;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

final class ApkSigningInspector {
    private static final byte[] APK_SIGNING_MAGIC = "APK Sig Block 42".getBytes(StandardCharsets.US_ASCII);

    SigningInfo inspect(java.nio.file.Path apk) throws IOException {
        SigningInfo result = new SigningInfo();
        Set<String> certificates = new LinkedHashSet<>();
        try (JarFile jar = new JarFile(apk.toFile(), true)) {
            byte[] buffer = new byte[32 * 1024];
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory() || entry.getName().regionMatches(true, 0, "META-INF/", 0, 9)) {
                    continue;
                }
                try (InputStream input = jar.getInputStream(entry)) {
                    while (input.read(buffer) != -1) {
                        // Reading to EOF makes JarFile verify v1 signatures and attach certificates.
                    }
                }
                Certificate[] signers = entry.getCertificates();
                if (signers != null) {
                    for (Certificate certificate : signers) {
                        try {
                            certificates.add(Hashing.sha256(certificate.getEncoded()));
                        } catch (CertificateEncodingException error) {
                            throw new IOException("Unable to encode APK signer certificate", error);
                        }
                    }
                }
            }
            // Some APKs expose the PKCS#7 certificate chain but do not attach certificates to
            // individual JarEntry instances on every JDK. Parse only the certificate container;
            // JarFile above remains responsible for v1 integrity verification.
            Enumeration<JarEntry> signatureEntries = jar.entries();
            while (signatureEntries.hasMoreElements()) {
                JarEntry entry = signatureEntries.nextElement();
                String upper = entry.getName().toUpperCase(java.util.Locale.ROOT);
                if (!upper.startsWith("META-INF/")
                        || !(upper.endsWith(".RSA") || upper.endsWith(".DSA") || upper.endsWith(".EC"))) {
                    continue;
                }
                try (InputStream input = jar.getInputStream(entry)) {
                    for (Certificate certificate : CertificateFactory.getInstance("X.509")
                            .generateCertificates(input)) {
                        certificates.add(Hashing.sha256(certificate.getEncoded()));
                    }
                } catch (CertificateException error) {
                    throw new IOException("Unable to parse APK signer certificate", error);
                }
            }
        }
        result.v1CertificateSha256.addAll(certificates);
        Collections.sort(result.v1CertificateSha256);
        result.apkSigningBlockPresent = hasApkSigningBlock(apk);
        return result;
    }

    private static boolean hasApkSigningBlock(java.nio.file.Path apk) throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(apk.toFile(), "r")) {
            long length = file.length();
            int tailLength = (int) Math.min(length, 65_557L);
            if (tailLength < 22) {
                return false;
            }
            byte[] tail = new byte[tailLength];
            file.seek(length - tailLength);
            file.readFully(tail);
            int eocd = -1;
            for (int i = tail.length - 22; i >= 0; i--) {
                if ((tail[i] & 0xff) == 0x50 && (tail[i + 1] & 0xff) == 0x4b
                        && (tail[i + 2] & 0xff) == 0x05 && (tail[i + 3] & 0xff) == 0x06) {
                    eocd = i;
                    break;
                }
            }
            if (eocd < 0) {
                return false;
            }
            long centralDirectory = littleU32(tail, eocd + 16);
            if (centralDirectory < APK_SIGNING_MAGIC.length) {
                return false;
            }
            byte[] magic = new byte[APK_SIGNING_MAGIC.length];
            file.seek(centralDirectory - APK_SIGNING_MAGIC.length);
            file.readFully(magic);
            return java.util.Arrays.equals(magic, APK_SIGNING_MAGIC);
        }
    }

    private static long littleU32(byte[] bytes, int offset) {
        return (bytes[offset] & 0xffL) | ((bytes[offset + 1] & 0xffL) << 8)
                | ((bytes[offset + 2] & 0xffL) << 16) | ((bytes[offset + 3] & 0xffL) << 24);
    }
}
