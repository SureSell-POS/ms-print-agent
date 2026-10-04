# =============================================================================
#  SureSell Print Agent - auto-update mínimo para Windows.
#
#  Compara la versión en ejecución (GET /api/printer/version) contra la publicada
#  en un manifiesto remoto. Si hay una más nueva: descarga el JAR, verifica el
#  hash, detiene el agente (tarea programada), reemplaza el JAR y lo reinicia.
#
#  Requiere haber instalado el auto-arranque con install-service.ps1 (usa la misma
#  tarea 'SureSellPrintAgent' y el JAR estable %LOCALAPPDATA%\SureSellPrintAgent\ms-print-agent.jar).
#
#  Uso:
#    .\update-agent.ps1                 # revisa y actualiza si hay versión nueva
#    .\update-agent.ps1 -Install        # además, programa la revisión diaria
#    .\update-agent.ps1 -Force          # reinstala aunque la versión coincida
#
#  >>> CONFIGURA $ManifestUrl <<<  (dónde publicas las versiones del agente).
#  El manifiesto es un JSON:
#    { "version": "0.0.2", "url": "https://.../ms-print-agent-0.0.2.jar", "sha256": "<hex opcional>" }
# =============================================================================
param(
  [switch]$Install,
  [switch]$Force
)

$ErrorActionPreference = "Stop"

# Releases publicados en GitHub. La URL /releases/latest/download/<asset> siempre
# apunta al asset del release MÁS NUEVO, así que no hay que tocar este script al
# publicar una versión nueva: basta con crear el release con los mismos assets.
$ManifestUrl = "https://github.com/SureSell-POS/ms-print-agent/releases/latest/download/latest.json"

$TaskName   = "SureSellPrintAgent"
$InstallDir = Join-Path $env:LOCALAPPDATA "SureSellPrintAgent"
$JarPath    = Join-Path $InstallDir "ms-print-agent.jar"
$StatusUrl  = "http://localhost:8181/api/printer/version"

function Get-RunningVersion {
  try { return (Invoke-RestMethod -Uri $StatusUrl -TimeoutSec 5).version } catch { return $null }
}

if ($Install) {
  # Programa este script para correr diario (revisa updates en segundo plano).
  $self = $MyInvocation.MyCommand.Path
  $action = "powershell.exe -NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File `"$self`""
  schtasks /Create /TN "SureSellPrintAgentUpdater" /SC DAILY /ST 04:30 /RL LIMITED /F /TR $action | Out-Null
  Write-Host "Revisión diaria programada (04:30) como 'SureSellPrintAgentUpdater'."
}

if ($ManifestUrl -like "*REEMPLAZAR*") {
  Write-Warning "ManifestUrl no configurada: edita `$ManifestUrl en este script. Nada que actualizar."
  exit 0
}

Write-Host "Consultando manifiesto: $ManifestUrl"
$manifest = Invoke-RestMethod -Uri $ManifestUrl -TimeoutSec 15
$remoteVer = $manifest.version
$localVer  = Get-RunningVersion

Write-Host "Versión en ejecución: $localVer  |  Publicada: $remoteVer"
if (-not $Force -and $localVer -eq $remoteVer) {
  Write-Host "Ya está en la última versión. Nada que hacer."
  exit 0
}

$tmp = Join-Path $env:TEMP "ms-print-agent-$remoteVer.jar"
Write-Host "Descargando $($manifest.url) ..."
Invoke-WebRequest -Uri $manifest.url -OutFile $tmp -TimeoutSec 120

if ($manifest.sha256) {
  $hash = (Get-FileHash -Path $tmp -Algorithm SHA256).Hash.ToLower()
  if ($hash -ne $manifest.sha256.ToLower()) {
    Remove-Item $tmp -Force
    Write-Error "Hash no coincide (esperado $($manifest.sha256), obtenido $hash). Aborta."
  }
}

Write-Host "Deteniendo el agente ..."
schtasks /End /TN $TaskName 2>$null | Out-Null
Start-Sleep -Seconds 2

New-Item -ItemType Directory -Force -Path $InstallDir | Out-Null
Copy-Item $tmp $JarPath -Force
Remove-Item $tmp -Force

Write-Host "Reiniciando el agente ..."
schtasks /Run /TN $TaskName | Out-Null
Start-Sleep -Seconds 3

$new = Get-RunningVersion
Write-Host "Actualizado. Versión en ejecución ahora: $new"
