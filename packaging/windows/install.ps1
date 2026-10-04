# =============================================================================
#  SureSell Print Agent - instalación COMPLETA de producción (Windows).
#  Ejecutar COMO ADMINISTRADOR. Hace, en un paso:
#    1) Política de Chrome/Edge para permitir la impresión local (sin flags).
#    2) Auto-arranque del agente al iniciar sesión (tarea programada).
#
#  Uso (PowerShell como administrador, en la carpeta con el JAR + los .ps1):
#    .\install.ps1
#    .\install.ps1 -Origin "https://pos.tudominio.com" -PrinterName "POS-58"
#    .\install.ps1 -Uninstall
# =============================================================================
param(
  [string]$Origin = "https://pos-caja.suresell.com.co",   # POS de producción; staging: https://posstaging.suresell.com.co
  [string]$PrinterName = "",
  [switch]$Uninstall
)

$ErrorActionPreference = "Stop"
$here = Split-Path -Parent $MyInvocation.MyCommand.Path

$admin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()
         ).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $admin) { Write-Error "Ejecuta COMO ADMINISTRADOR." }

if ($Uninstall) {
  & "$here\configure-chrome-policy.ps1" -Uninstall
  & "$here\install-service.ps1" -Uninstall
  Write-Host "Desinstalación completa."
  exit 0
}

Write-Host "== 1/2 Política del navegador (Local Network Access) =="
& "$here\configure-chrome-policy.ps1" -Origin $Origin

Write-Host ""
Write-Host "== 2/2 Auto-arranque del agente =="
if ($PrinterName -ne "") {
  & "$here\install-service.ps1" -PrinterName $PrinterName
} else {
  & "$here\install-service.ps1"
}

Write-Host ""
Write-Host "LISTO. Reinicia Chrome/Edge y prueba: $Origin"
Write-Host "El agente ya corre en http://localhost:8181 (verifica /api/printer/status)."
