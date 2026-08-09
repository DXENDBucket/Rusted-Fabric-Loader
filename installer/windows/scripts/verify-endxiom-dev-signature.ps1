param(
    [Parameter(Mandatory = $true)]
    [string]$LauncherFile,

    [Parameter(Mandatory = $true)]
    [string]$InstallerFile,

    [Parameter(Mandatory = $true)]
    [string]$PayloadZip
)

$ErrorActionPreference = 'Stop'
$temporaryDirectory = Join-Path ([IO.Path]::GetTempPath()) ('rusted-fabric-signature-' + [Guid]::NewGuid().ToString('N'))
$embeddedLauncher = Join-Path $temporaryDirectory 'RustedFabricLauncher.exe'

try {
    New-Item -ItemType Directory -Path $temporaryDirectory | Out-Null
    Add-Type -AssemblyName System.IO.Compression
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [IO.Compression.ZipFile]::OpenRead($PayloadZip)
    try {
        $entry = $archive.GetEntry('core/RustedFabricLauncher.exe')
        if ($null -eq $entry) {
            throw 'Signed installer payload is missing core/RustedFabricLauncher.exe'
        }
        $input = $entry.Open()
        try {
            $output = [IO.File]::Create($embeddedLauncher)
            try { $input.CopyTo($output) } finally { $output.Dispose() }
        } finally { $input.Dispose() }
    } finally { $archive.Dispose() }

    $expectedThumbprint = $null
    foreach ($file in @($LauncherFile, $embeddedLauncher, $InstallerFile)) {
        $signature = Get-AuthenticodeSignature -LiteralPath $file
        if ($null -eq $signature.SignerCertificate) {
            throw "File is not Authenticode signed: $file"
        }
        if ($signature.SignerCertificate.Subject -ne 'CN=EndXiom') {
            throw "Unexpected signer for $file`: $($signature.SignerCertificate.Subject)"
        }
        if ($null -eq $expectedThumbprint) {
            $expectedThumbprint = $signature.SignerCertificate.Thumbprint
        } elseif ($signature.SignerCertificate.Thumbprint -ne $expectedThumbprint) {
            throw "Signer thumbprint differs for $file`: $($signature.SignerCertificate.Thumbprint)"
        }
    }

    Write-Output $expectedThumbprint
} finally {
    if (Test-Path -LiteralPath $temporaryDirectory) {
        Remove-Item -LiteralPath $temporaryDirectory -Recurse -Force
    }
}
