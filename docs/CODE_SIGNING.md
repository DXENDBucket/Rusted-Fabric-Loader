# Windows Code Signing

## EndXiom development signature

The repository can create a local self-signed code-signing certificate with subject `CN=EndXiom`
and sign both the installed launcher and the outer installer:

```bat
gradlew.bat windowsInstallerEndXiomDevSigned
```

The first build creates a 3072-bit RSA/SHA-256 certificate in `Cert:\CurrentUser\My`. Its friendly
name is `EndXiom Rusted Fabric Development Code Signing`, its private key is non-exportable, and its
initial validity is five years. Later builds reuse the same valid certificate, keeping the signer
identity stable on this Windows account.

The build exports only the public certificate to
`local-signing/EndXiom-dev-code-signing.cer`. `local-signing/` is ignored by Git. The final artifact
is:

```text
installer/windows/build/dist/Rusted-Fabric-Installer-<version>-EndXiom-dev-signed.exe
```

The verification task checks that the launcher and installer both have subject `CN=EndXiom` and
the same certificate thumbprint. Because the certificate is self-signed, Windows normally reports
that its chain ends in an untrusted root. That is expected: this signature detects post-signing
changes and identifies repeated builds made with this local key, but it does not create public
publisher trust or bypass SmartScreen.

Do not instruct ordinary players to place this certificate in Trusted Root Certification
Authorities. That grants broad trust to the certificate and is unsuitable as a public distribution
model.

## Public releases

A publicly trusted release needs a code-signing certificate issued after identity verification by a
recognized certificate authority, or a managed cloud-signing service. Current providers commonly
keep private keys in a hardware token, HSM, or managed signing service instead of an ordinary PFX
file.

With a locally accessible production certificate and the Windows SDK installed, the usual command
shape is:

```bat
signtool sign /fd SHA256 /sha1 CERTIFICATE_THUMBPRINT ^
  /tr RFC3161_TIMESTAMP_URL /td SHA256 Rusted-Fabric-Installer.exe
signtool verify /pa /v Rusted-Fabric-Installer.exe
```

Use the timestamp service supplied or recommended by the certificate authority. A valid RFC 3161
timestamp lets Windows validate that the file was signed while the certificate was valid, even
after that certificate expires. Sign the installed launcher before embedding it, then sign the
finished outer installer.

Certificate-chain trust and Microsoft Defender SmartScreen reputation are separate. A CA-issued
signature establishes a verifiable publisher, while download reputation can still take time to
develop unless the selected signing product provides immediate reputation handling.
