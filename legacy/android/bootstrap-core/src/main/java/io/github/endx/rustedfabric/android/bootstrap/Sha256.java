package io.github.endx.rustedfabric.android.bootstrap;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Streaming SHA-256 helper that does not retain or copy the inspected APK. */
public final class Sha256 {
    private Sha256() {
    }

    public static String digest(InputStream input) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
        byte[] buffer = new byte[64 * 1024];
        for (int read = input.read(buffer); read >= 0; read = input.read(buffer)) {
            if (read > 0) {
                digest.update(buffer, 0, read);
            }
        }
        byte[] bytes = digest.digest();
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            hex.append(String.format("%02x", value & 0xff));
        }
        return hex.toString();
    }
}
