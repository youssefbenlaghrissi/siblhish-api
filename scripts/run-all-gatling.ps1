# Lance les 4 simulations Gatling (Load, Defaut, Smoke, Stress) puis ouvre le dernier rapport.
# Usage : .\scripts\run-all-gatling.ps1
# Prealable : API demarree (.\gradlew bootRun) sur le port 8082.

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$simulations = @("0", "1", "2", "3")
foreach ($num in $simulations) {
    Write-Host "=== Simulation $num ===" -ForegroundColor Cyan
    $num | .\gradlew gatlingRun --no-daemon 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Simulation $num a echoue (code $LASTEXITCODE). Continuation..." -ForegroundColor Yellow
    }
}

$reportsDir = Join-Path $root "build\reports\gatling"
$latest = Get-ChildItem $reportsDir -Directory -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if ($latest) {
    $indexPath = Join-Path $latest.FullName "index.html"
    if (Test-Path $indexPath) {
        Write-Host "Ouverture du rapport : $indexPath" -ForegroundColor Green
        Start-Process $indexPath
    }
} else {
    Write-Host "Aucun rapport trouve dans $reportsDir" -ForegroundColor Red
}
