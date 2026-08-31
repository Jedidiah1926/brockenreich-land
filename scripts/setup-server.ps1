param(
    [switch]$AcceptEula
)

$ErrorActionPreference = "Stop"
$Version = "1.21.8"
$RunDir = Join-Path $PSScriptRoot "..\run"
$PluginsDir = Join-Path $RunDir "plugins"

New-Item -ItemType Directory -Force -Path $PluginsDir | Out-Null
Set-Location $RunDir

Write-Host "Fetching latest Paper $Version build number..."
$versionInfo = Invoke-RestMethod -Uri "https://api.papermc.io/v2/projects/paper/versions/$Version"
$build = $versionInfo.builds[-1]
if (-not $build) {
    Write-Error "Could not determine latest build for Paper $Version"
    exit 1
}
Write-Host "Latest build: $build"

$jarName = "paper-$Version-$build.jar"
if (-not (Test-Path $jarName)) {
    Write-Host "Downloading $jarName..."
    Invoke-WebRequest -Uri "https://api.papermc.io/v2/projects/paper/versions/$Version/builds/$build/downloads/$jarName" -OutFile $jarName
} else {
    Write-Host "$jarName already present, skipping download."
}

Copy-Item $jarName "server.jar" -Force

if ($AcceptEula) {
    "eula=true" | Set-Content "eula.txt"
    Write-Host "Wrote eula.txt (you have accepted the Mojang EULA: https://aka.ms/MinecraftEULA)."
} else {
    "eula=false" | Set-Content "eula.txt"
    Write-Host ""
    Write-Host "NOTE: run/eula.txt was created with eula=false."
    Write-Host "Read https://aka.ms/MinecraftEULA, then either edit run/eula.txt to eula=true"
    Write-Host "or re-run this script with -AcceptEula."
}

Write-Host ""
Write-Host "Setup complete. Server jar: run/$jarName (copied as run/server.jar)"
Write-Host "Next: .\gradlew.bat deployToRunServer ; then .\scripts\run-server.ps1"
