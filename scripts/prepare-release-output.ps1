[CmdletBinding()]
param(
    [switch]$SkipBuild
)

$ErrorActionPreference = 'Stop'
$RepositoryRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$VersionFile = Join-Path $RepositoryRoot 'versions.properties'
$ConfiguredJavaHome = $env:JAVA_HOME
if (!$ConfiguredJavaHome -or !(Test-Path -LiteralPath (Join-Path $ConfiguredJavaHome 'bin/java.exe'))) {
    $ConfiguredJavaHome = 'C:\Program Files\Java\jdk-17'
}
if (!(Test-Path -LiteralPath (Join-Path $ConfiguredJavaHome 'bin/java.exe'))) {
    throw 'JDK 17 is required. Set JAVA_HOME to an installed JDK 17 directory.'
}
$JavaReleaseFile = Join-Path $ConfiguredJavaHome 'release'
$JavaReleaseText = if (Test-Path -LiteralPath $JavaReleaseFile -PathType Leaf) {
    [System.IO.File]::ReadAllText($JavaReleaseFile)
} else {
    ''
}
if ($JavaReleaseText -notmatch '(?m)^JAVA_VERSION="17(?:\.|"|-)') {
    $FallbackJavaHome = 'C:\Program Files\Java\jdk-17'
    if (!(Test-Path -LiteralPath (Join-Path $FallbackJavaHome 'bin/java.exe'))) {
        throw "JAVA_HOME does not point to JDK 17: $ConfiguredJavaHome"
    }
    $ConfiguredJavaHome = $FallbackJavaHome
}
$env:JAVA_HOME = $ConfiguredJavaHome
$env:PATH = (Join-Path $ConfiguredJavaHome 'bin') + [System.IO.Path]::PathSeparator + $env:PATH

function Read-Versions {
    $result = @{}
    foreach ($line in [System.IO.File]::ReadAllLines($VersionFile)) {
        $trimmed = $line.Trim()
        if (!$trimmed -or $trimmed.StartsWith('#') -or !$trimmed.Contains('=')) {
            continue
        }
        $parts = $trimmed.Split('=', 2)
        $result[$parts[0].Trim()] = $parts[1].Trim()
    }
    return $result
}

function Invoke-Checked {
    param(
        [Parameter(Mandatory = $true)][string]$Executable,
        [Parameter(Mandatory = $true)][string[]]$Arguments,
        [Parameter(Mandatory = $true)][string]$WorkingDirectory
    )
    Push-Location $WorkingDirectory
    try {
        & $Executable @Arguments
        if ($LASTEXITCODE -ne 0) {
            throw "Command failed with exit code ${LASTEXITCODE}: $Executable $Arguments"
        }
    } finally {
        Pop-Location
    }
}

function Copy-RequiredArtifact {
    param(
        [Parameter(Mandatory = $true)][string]$Source,
        [Parameter(Mandatory = $true)][string]$DestinationName
    )
    if (!(Test-Path -LiteralPath $Source -PathType Leaf)) {
        throw "Missing release artifact: $Source"
    }
    Copy-Item -LiteralPath $Source -Destination (Join-Path $OutputDirectory $DestinationName)
}

$Versions = Read-Versions
foreach ($key in @('loader', 'api', 'javaModMenu', 'iniEssentials', 'performanceProfiler')) {
    if (!$Versions.ContainsKey($key) -or !$Versions[$key]) {
        throw "Missing $key in versions.properties"
    }
}

$AndroidBuildFile = Join-Path $RepositoryRoot 'android/launcher/app/build.gradle'
$AndroidBuildText = [System.IO.File]::ReadAllText($AndroidBuildFile)
$AndroidVersionMatch = [regex]::Match($AndroidBuildText, "versionName\s+'([^']+)'")
if (!$AndroidVersionMatch.Success) {
    throw 'Unable to read Android versionName'
}
$AndroidVersion = $AndroidVersionMatch.Groups[1].Value
$ReleaseName = 'v' + $Versions.loader
$OutputRoot = [System.IO.Path]::GetFullPath((Join-Path $RepositoryRoot 'release-output'))
$OutputDirectory = [System.IO.Path]::GetFullPath((Join-Path $OutputRoot $ReleaseName))
if (!$OutputDirectory.StartsWith($OutputRoot + [System.IO.Path]::DirectorySeparatorChar,
        [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "Unsafe release output path: $OutputDirectory"
}

if (!$SkipBuild) {
    Invoke-Checked -Executable (Join-Path $RepositoryRoot 'gradlew.bat') `
        -Arguments @('windowsInstallerEndXiomDevSigned', 'verifyDistribution', '--console=plain') `
        -WorkingDirectory $RepositoryRoot
    Invoke-Checked -Executable (Join-Path $RepositoryRoot 'gradlew.bat') `
        -Arguments @('-p', 'android/launcher', ':app:assembleRelease',
            ':app:verifyNoGamePayload', '--console=plain') `
        -WorkingDirectory $RepositoryRoot
    $ReferenceRoot = Join-Path $RepositoryRoot 'official-mods/ini-essentials'
    Invoke-Checked -Executable 'python' `
        -Arguments @('docs/generate_reference.py', '--check') `
        -WorkingDirectory $ReferenceRoot
}

if (Test-Path -LiteralPath $OutputDirectory) {
    Remove-Item -LiteralPath $OutputDirectory -Recurse -Force
}
New-Item -ItemType Directory -Path $OutputDirectory | Out-Null

Copy-RequiredArtifact `
    -Source (Join-Path $RepositoryRoot "installer/windows/build/dist/Rusted-Fabric-Installer-$($Versions.loader)-EndXiom-dev-signed.exe") `
    -DestinationName "Rusted-Fabric-Installer-$($Versions.loader)-Windows-x64-EndXiom-self-signed.exe"
Copy-RequiredArtifact `
    -Source (Join-Path $RepositoryRoot 'android/launcher/app/build/outputs/apk/release/app-release.apk') `
    -DestinationName "Rusted-Fabric-Android-Launcher-$AndroidVersion-arm64-v8a.APK"
Copy-RequiredArtifact `
    -Source (Join-Path $RepositoryRoot "rusted-fabric-api/build/libs/official/rusted-fabric-api-$($Versions.api).jar") `
    -DestinationName "rusted-fabric-api-$($Versions.api).jar"
Copy-RequiredArtifact `
    -Source (Join-Path $RepositoryRoot "official-mods/java-mod-menu/build/libs/java-mod-menu-$($Versions.javaModMenu)-official.jar") `
    -DestinationName "java-mod-menu-$($Versions.javaModMenu).jar"
Copy-RequiredArtifact `
    -Source (Join-Path $RepositoryRoot "official-mods/ini-essentials/build/libs/ini-essentials-$($Versions.iniEssentials)-official.jar") `
    -DestinationName "ini-essentials-$($Versions.iniEssentials).jar"
Copy-RequiredArtifact `
    -Source (Join-Path $RepositoryRoot "official-mods/performance-profiler/build/libs/performance-profiler-$($Versions.performanceProfiler)-official.jar") `
    -DestinationName "performance-profiler-$($Versions.performanceProfiler).jar"
Copy-RequiredArtifact `
    -Source (Join-Path $RepositoryRoot 'official-mods/ini-essentials/docs/INI Essentials Unit Modding Reference.xlsx') `
    -DestinationName "INI-Essentials-Unit-Modding-Reference-$($Versions.iniEssentials).xlsx"

$ChecksumLines = Get-ChildItem -LiteralPath $OutputDirectory -File |
    Sort-Object Name |
    ForEach-Object {
        $hash = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
        "$hash  $($_.Name)"
    }
[System.IO.File]::WriteAllLines(
    (Join-Path $OutputDirectory 'SHA256SUMS.txt'),
    $ChecksumLines,
    [System.Text.UTF8Encoding]::new($false))

Write-Host "Release output ready: $OutputDirectory"
Get-ChildItem -LiteralPath $OutputDirectory -File |
    Sort-Object Name |
    Select-Object Name, Length
