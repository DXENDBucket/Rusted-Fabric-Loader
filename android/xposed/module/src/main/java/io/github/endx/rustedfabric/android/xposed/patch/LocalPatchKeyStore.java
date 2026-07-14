package io.github.endx.rustedfabric.android.xposed.patch;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.math.BigInteger;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;

import javax.security.auth.x500.X500Principal;

/** Persistent, non-exportable per-install signing identity for local patched APK updates. */
public final class LocalPatchKeyStore {
    private static final String STORE = "AndroidKeyStore";
    private static final String ALIAS = "rusted-fabric-local-patch-v1";

    private LocalPatchKeyStore() {
    }

    public static synchronized SigningIdentity loadOrCreate() throws Exception {
        KeyStore store = loadStore();
        if (!store.containsAlias(ALIAS)) {
            Calendar end = Calendar.getInstance();
            end.add(Calendar.YEAR, 30);
            KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(ALIAS,
                    KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                    .setKeySize(2048)
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                    .setCertificateSubject(new X500Principal(
                            "CN=Rusted Fabric local patch,O=User-owned local install"))
                    .setCertificateSerialNumber(BigInteger.ONE)
                    .setCertificateNotBefore(new Date(System.currentTimeMillis() - 60_000L))
                    .setCertificateNotAfter(end.getTime())
                    .build();
            KeyPairGenerator generator = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_RSA, STORE);
            generator.initialize(spec);
            generator.generateKeyPair();
            store = loadStore();
        }
        PrivateKey key = (PrivateKey) store.getKey(ALIAS, null);
        X509Certificate certificate = (X509Certificate) store.getCertificate(ALIAS);
        if (key == null || certificate == null) {
            throw new IllegalStateException("Local patch signing key is unavailable");
        }
        return new SigningIdentity(key, certificate);
    }

    public static synchronized X509Certificate loadCertificate() {
        try {
            KeyStore store = loadStore();
            return store.containsAlias(ALIAS)
                    ? (X509Certificate) store.getCertificate(ALIAS) : null;
        } catch (Exception unavailable) {
            return null;
        }
    }

    private static KeyStore loadStore() throws Exception {
        KeyStore store = KeyStore.getInstance(STORE);
        store.load(null);
        return store;
    }

    public static final class SigningIdentity {
        private final PrivateKey privateKey;
        private final X509Certificate certificate;

        private SigningIdentity(PrivateKey privateKey, X509Certificate certificate) {
            this.privateKey = privateKey;
            this.certificate = certificate;
        }

        public PrivateKey getPrivateKey() { return privateKey; }
        public X509Certificate getCertificate() { return certificate; }
    }
}
