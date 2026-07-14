package io.github.endx.rustedfabric.android.patcher;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

final class PatcherSha256 {
    private PatcherSha256() {
    }

    static String digest(Path file) throws IOException {
        try (InputStream input = Files.newInputStream(file)) {
            MessageDigest digest;
            try {
                digest = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException impossible) {
                throw new IllegalStateException(impossible);
            }
            byte[] buffer = new byte[64 * 1024];
            for (int count = input.read(buffer); count >= 0; count = input.read(buffer)) {
                if (count > 0) digest.update(buffer, 0, count);
            }
            StringBuilder hex = new StringBuilder(64);
            for (byte value : digest.digest()) {
                hex.append(Character.forDigit((value >>> 4) & 15, 16));
                hex.append(Character.forDigit(value & 15, 16));
            }
            return hex.toString();
        }
    }

    static String digest(byte[] bytes) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
        return hex(digest.digest(bytes));
    }

    private static String hex(byte[] hash) {
        StringBuilder hex = new StringBuilder(hash.length * 2);
        for (byte value : hash) {
            hex.append(Character.forDigit((value >>> 4) & 15, 16));
            hex.append(Character.forDigit(value & 15, 16));
        }
        return hex.toString();
    }
}
