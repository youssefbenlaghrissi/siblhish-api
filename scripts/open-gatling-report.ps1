# Ouvre le dernier rapport Gatling dans le navigateur par defaut.
$reportsDir = Join-Path $PSScriptRoot "..\build\reports\gatling"
if (-not (Test-Path $reportsDir)) {
    Write-Host "Aucun rapport trouve. Lancez d'abord: .\gradlew gatlingRun"
    exit 1
}
$latest = Get-ChildItem $reportsDir -Directory | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $latest) {
    Write-Host "Aucun rapport trouve dans $reportsDir"
    exit 1
}
$indexPath = Join-Path $latest.FullName "index.html"
if (-not (Test-Path $indexPath)) {
    Write-Host "Fichier index.html introuvable dans $($latest.Name)"
    exit 1
}
Write-Host "Ouverture du rapport: $indexPath"
Start-Process $indexPath
exit 0
