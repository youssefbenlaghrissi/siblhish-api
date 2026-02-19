# Libère le port de l'API (arrête le processus qui l'utilise).
# Usage: .\scripts\kill-port-8081.ps1   ou modifier $port ci-dessous
$port = 8082
$pids = (Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue).OwningProcess
if (-not $pids) {
    Write-Host "Aucun processus n'ecoute sur le port $port."
    exit 0
}
$pids = $pids | Sort-Object -Unique
foreach ($pid in $pids) {
    Write-Host "Arret du processus PID $pid (port $port)..."
    Stop-Process -Id $pid -Force -ErrorAction SilentlyContinue
}
Write-Host "Port $port libere."
