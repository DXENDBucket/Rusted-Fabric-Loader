package io.github.endx.rustedfabric.android.patcher.cli;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import io.github.endx.rustedfabric.android.patcher.LocalApkPatcher;
import io.github.endx.rustedfabric.android.patcher.LocalApkSigner;
import io.github.endx.rustedfabric.android.patcher.PatchException;
import io.github.endx.rustedfabric.android.patcher.PatchProfile;
import io.github.endx.rustedfabric.android.patcher.PatchReport;
import io.github.endx.rustedfabric.android.patcher.PatchRequest;

/** Developer CLI. Consumer Android UI will call the same patcher core directly. */
public final class LocalPatcherMain {
    private LocalPatcherMain() {
    }

    public static void main(String[] args) throws Exception {
        Map<String, String> options = parse(args);
        if (options.containsKey("--help")) {
            usage();
            return;
        }
        Path input = requiredPath(options, "--input");
        Path bootstrapDex = requiredPath(options, "--bootstrap-dex");
        Path output = requiredPath(options, "--output");
        String clonePackage = options.getOrDefault("--clone-package",
                PatchProfile.DEFAULT_CLONE_PACKAGE);
        boolean signing = options.containsKey("--keystore");
        Path unsignedOutput = signing
                ? output.resolveSibling("." + output.getFileName() + ".unsigned.tmp") : output;
        PatchRequest request = new PatchRequest(input, bootstrapDex, unsignedOutput,
                PatchProfile.officialAndroid115(), clonePackage);
        try {
            PatchReport report = new LocalApkPatcher().patchUnsigned(request);
            LocalApkSigner.SigningResult signingResult = null;
            if (signing) {
                signingResult = sign(options, unsignedOutput, output);
                report = report.withSignedOutput(signingResult.getApkSha256());
            }
            Path reportPath = options.containsKey("--report")
                    ? Paths.get(options.get("--report"))
                    : output.resolveSibling(output.getFileName() + ".patch-report.json");
            Files.write(reportPath, report.toJson().getBytes(StandardCharsets.UTF_8));
            System.out.println(signing ? "Signed local patch completed"
                    : "Unsigned local patch completed");
            System.out.println("Output: " + output.toAbsolutePath().normalize());
            System.out.println("Report: " + reportPath.toAbsolutePath().normalize());
            System.out.println("SHA-256: " + report.getOutputSha256());
            if (signingResult != null) {
                System.out.println("Certificate SHA-256: "
                        + signingResult.getCertificateSha256());
                System.out.println("Schemes: v1=" + signingResult.isV1()
                        + " v2=" + signingResult.isV2() + " v3=" + signingResult.isV3());
            } else {
                System.out.println("Unsigned output; pass --keystore to create an installable APK.");
            }
        } catch (PatchException failure) {
            System.err.println("Patch failed [" + failure.getReason() + "]: "
                    + failure.getMessage());
            throw failure;
        } finally {
            if (signing) Files.deleteIfExists(unsignedOutput);
        }
    }

    private static LocalApkSigner.SigningResult sign(Map<String, String> options,
                                                     Path unsigned, Path output) throws Exception {
        Path keyStorePath = requiredPath(options, "--keystore");
        String alias = required(options, "--key-alias");
        char[] storePassword = required(options, "--store-pass").toCharArray();
        char[] keyPassword = options.getOrDefault("--key-pass",
                new String(storePassword)).toCharArray();
        try {
            KeyStore store = KeyStore.getInstance("JKS");
            try (java.io.InputStream input = Files.newInputStream(keyStorePath)) {
                store.load(input, storePassword);
            }
            Key key = store.getKey(alias, keyPassword);
            Certificate certificate = store.getCertificate(alias);
            if (!(key instanceof PrivateKey) || !(certificate instanceof X509Certificate)) {
                throw new IllegalArgumentException("Keystore alias is not an X.509 private key");
            }
            return new LocalApkSigner().sign(unsigned, output, alias,
                    (PrivateKey) key, (X509Certificate) certificate);
        } finally {
            Arrays.fill(storePassword, '\0');
            Arrays.fill(keyPassword, '\0');
        }
    }

    private static Map<String, String> parse(String[] args) {
        Map<String, String> options = new LinkedHashMap<>();
        for (int index = 0; index < args.length; index++) {
            String key = args[index];
            if ("--help".equals(key)) {
                options.put(key, "true");
                continue;
            }
            if (!key.startsWith("--") || index + 1 >= args.length) {
                throw new IllegalArgumentException("Malformed option: " + key);
            }
            if (options.put(key, args[++index]) != null) {
                throw new IllegalArgumentException("Duplicate option: " + key);
            }
        }
        return options;
    }

    private static Path requiredPath(Map<String, String> options, String key) {
        return Paths.get(required(options, key));
    }

    private static String required(Map<String, String> options, String key) {
        String value = options.get(key);
        if (value == null || value.trim().isEmpty()) {
            usage();
            throw new IllegalArgumentException("Missing " + key);
        }
        return value;
    }

    private static void usage() {
        System.out.println("Usage: local-patcher --input game.apk --bootstrap-dex classes.dex "
                + "--output patched.apk [--clone-package io.github.endx.rwpatch] "
                + "[--report report.json] [--keystore local.jks --store-pass password "
                + "--key-alias alias [--key-pass password]]");
    }
}
