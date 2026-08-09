param(
    [Parameter(Mandatory = $true)]
    [string]$PublicCertificatePath
)

$ErrorActionPreference = 'Stop'
$subject = 'CN=EndXiom'
$friendlyName = 'EndXiom Rusted Fabric Development Code Signing'
$now = Get-Date

$certificate = Get-ChildItem Cert:\CurrentUser\My |
    Where-Object {
        $_.Subject -eq $subject -and
        $_.FriendlyName -eq $friendlyName -and
        $_.HasPrivateKey -and
        $_.NotAfter -gt $now.AddDays(30) -and
        ($_.EnhancedKeyUsageList | Where-Object {
            $_.ObjectId -eq '1.3.6.1.5.5.7.3.3' -or $_.ObjectId.Value -eq '1.3.6.1.5.5.7.3.3'
        })
    } |
    Sort-Object NotAfter -Descending |
    Select-Object -First 1

if ($null -eq $certificate) {
    $certificate = New-SelfSignedCertificate `
        -Type CodeSigningCert `
        -Subject $subject `
        -FriendlyName $friendlyName `
        -CertStoreLocation Cert:\CurrentUser\My `
        -KeyAlgorithm RSA `
        -KeyLength 3072 `
        -HashAlgorithm SHA256 `
        -KeyExportPolicy NonExportable `
        -NotAfter $now.AddYears(5)
}

$parent = Split-Path -Parent $PublicCertificatePath
if ($parent) {
    New-Item -ItemType Directory -Force -Path $parent | Out-Null
}
Export-Certificate -Cert $certificate -FilePath $PublicCertificatePath -Force | Out-Null
Write-Output $certificate.Thumbprint
