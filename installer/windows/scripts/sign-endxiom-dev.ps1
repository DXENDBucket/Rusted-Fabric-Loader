param(
    [Parameter(Mandatory = $true)]
    [string]$InputFile,

    [Parameter(Mandatory = $true)]
    [string]$OutputFile
)

$ErrorActionPreference = 'Stop'
$friendlyName = 'EndXiom Rusted Fabric Development Code Signing'
$certificate = Get-ChildItem Cert:\CurrentUser\My |
    Where-Object {
        $_.FriendlyName -eq $friendlyName -and
        $_.HasPrivateKey -and
        $_.NotAfter -gt (Get-Date) -and
        ($_.EnhancedKeyUsageList | Where-Object {
            $_.ObjectId -eq '1.3.6.1.5.5.7.3.3' -or $_.ObjectId.Value -eq '1.3.6.1.5.5.7.3.3'
        })
    } |
    Sort-Object NotAfter -Descending |
    Select-Object -First 1

if ($null -eq $certificate) {
    throw 'EndXiom development code-signing certificate was not found in Cert:\CurrentUser\My'
}

$parent = Split-Path -Parent $OutputFile
if ($parent) {
    New-Item -ItemType Directory -Force -Path $parent | Out-Null
}
Copy-Item -LiteralPath $InputFile -Destination $OutputFile -Force
$signature = Set-AuthenticodeSignature `
    -LiteralPath $OutputFile `
    -Certificate $certificate `
    -HashAlgorithm SHA256

if ($null -eq $signature.SignerCertificate) {
    throw "Authenticode signing failed: $($signature.StatusMessage)"
}
Write-Output $signature.SignerCertificate.Thumbprint
