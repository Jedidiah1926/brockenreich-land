$ErrorActionPreference = "Stop"
$RunDir = Join-Path $PSScriptRoot "..\run"
Set-Location $RunDir

if (-not (Test-Path "server.jar")) {
    Write-Error "run/server.jar not found. Run scripts/setup-server.ps1 first."
    exit 1
}

$eulaContent = Get-Content "eula.txt" -ErrorAction SilentlyContinue
if (-not ($eulaContent -match "eula=true")) {
    Write-Error "EULA not accepted (run/eula.txt). See https://aka.ms/MinecraftEULA"
    exit 1
}

java -Xms1G -Xmx2G -jar server.jar --nogui
