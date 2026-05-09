param(
    [ValidateSet("Debug", "Release")]
    [string]$Variant = "Debug",

    [switch]$Lint,
    [switch]$Test
)

$ErrorActionPreference = "Stop"

$Root = Resolve-Path (Join-Path $PSScriptRoot "..")
$BundledJdk = Join-Path $Root ".jdk\jdk-17.0.19+10"
$BundledSdk = Join-Path $Root ".android-sdk"

if (Test-Path $BundledJdk) {
    $env:JAVA_HOME = $BundledJdk
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
}

if (Test-Path $BundledSdk) {
    $env:ANDROID_SDK_ROOT = $BundledSdk
    $env:ANDROID_HOME = $BundledSdk
    $env:PATH = "$env:ANDROID_SDK_ROOT\platform-tools;$env:ANDROID_SDK_ROOT\cmdline-tools\latest\bin;$env:PATH"
}

$Tasks = @("assemble$Variant")
if ($Test) {
    $Tasks += "test${Variant}UnitTest"
}
if ($Lint) {
    $Tasks += "lint$Variant"
}

Push-Location $Root
try {
    & .\gradlew.bat $Tasks
    exit $LASTEXITCODE
} finally {
    Pop-Location
}
