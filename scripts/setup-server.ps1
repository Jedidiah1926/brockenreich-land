param(
    [switch]$AcceptEula
)

$ErrorActionPreference = "Stop"
$Version = "1.21.8"
$RunDir = Join-Path $PSScriptRoot "..\run"
$PluginsDir = Join-Path $RunDir "plugins"
$UserAgent = "brockenreich-land-setup-script (+https://github.com/Jedidiah1926/brockenreich-land)"

New-Item -ItemType Directory -Force -Path $PluginsDir | Out-Null
Set-Location $RunDir

Write-Host "Fetching Paper $Version builds..."
$builds = Invoke-RestMethod -Uri "https://fill.papermc.io/v3/projects/paper/versions/$Version/builds" -UserAgent $UserAgent
if (-not $builds -or $builds.Count -eq 0) {
    Write-Error "No builds found for Paper $Version"
    exit 1
}

$stable = $builds | Where-Object { $_.channel -eq "STABLE" }
$candidates = if ($stable) { $stable } else { $builds }
$latest = $candidates | Sort-Object -Property id -Descending | Select-Object -First 1

$downloadUrl = $latest.downloads.'server:default'.url
$jarName = $latest.downloads.'server:default'.name
if (-not $jarName) { $jarName = "paper-$Version-$($latest.id).jar" }

Write-Host "Latest build: $($latest.id) [$($latest.channel)]"

if (-not (Test-Path $jarName)) {
    Write-Host "Downloading $jarName..."
    Invoke-WebRequest -Uri $downloadUrl -OutFile $jarName -UserAgent $UserAgent
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
