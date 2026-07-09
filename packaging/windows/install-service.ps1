# =============================================================================
#  SureSell Print Agent - instalacion de auto-arranque en Windows.
#
#  Registra el agente como una Tarea Programada que arranca al INICIAR SESION el
#  usuario y se mantiene corriendo en segundo plano (con javaw, sin ventana). Es
#  la forma mas simple de "servicio" sin herramientas extra ni admin.
#
#  Uso (PowerShell):
#    .\install-service.ps1                 # instala (impresora "SAT"/default)
#    .\install-service.ps1 -PrinterName "POS-58"
#    .\install-service.ps1 -Uninstall      # desinstala
#
#  Copia el JAR + este script a %LOCALAPPDATA%\SureSellPrintAgent y crea la tarea.
# =============================================================================
param(
  [string]$PrinterName = "",
  [switch]$Uninstall
)

$ErrorActionPreference = "Stop"
$TaskName = "SureSellPrintAgent"
$InstallDir = Join-Path $env:LOCALAPPDATA "SureSellPrintAgent"

if ($Uninstall) {
  Write-Host "Desinstalando $TaskName ..."
  schtasks /End /TN $TaskName 2>$null | Out-Null
  schtasks /Delete /TN $TaskName /F 2>$null | Out-Null
  Write-Host "Tarea eliminada. (La carpeta $InstallDir se conserva; borrala a mano si quieres.)"
  exit 0
}

# --- Verifica Java ---
$java = (Get-Command javaw -ErrorAction SilentlyContinue)
if (-not $java) { $java = (Get-Command java -ErrorAction SilentlyContinue) }
if (-not $java) {
  Write-Error "No se encontro Java en el PATH. Instala Java 17 (https://adoptium.net) y reintenta."
}
$javaw = Join-Path (Split-Path $java.Source) "javaw.exe"
if (-not (Test-Path $javaw)) { $javaw = $java.Source }

# --- Localiza el JAR (junto a este script o en ..\..\build\libs) ---
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$jar = Get-ChildItem -Path $scriptDir -Filter "ms-print-agent-*.jar" -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $jar) {
  $jar = Get-ChildItem -Path (Join-Path $scriptDir "..\..\build\libs") -Filter "ms-print-agent-*.jar" -ErrorAction SilentlyContinue | Select-Object -First 1
}
if (-not $jar) {
  Write-Error "No se encontro ms-print-agent-*.jar. Copialo junto a este script o corre 'gradlew.bat clean bootJar'."
}

# --- Instala: copia el JAR a InstallDir con NOMBRE ESTABLE ---
# El nombre fijo 'ms-print-agent.jar' permite que update-agent.ps1 lo sobrescriba
# sin tener que reconfigurar la tarea programada.
New-Item -ItemType Directory -Force -Path $InstallDir | Out-Null
$destJar = Join-Path $InstallDir "ms-print-agent.jar"
Copy-Item $jar.FullName $destJar -Force
Write-Host "JAR instalado en $destJar"

$printerArg = ""
if ($PrinterName -ne "") { $printerArg = " --printer.name=`"$PrinterName`"" }

# --- Crea/actualiza la Tarea Programada (al iniciar sesion, oculta, auto-reinicio) ---
$action    = "`"$javaw`" -jar `"$destJar`"$printerArg"
schtasks /Create /TN $TaskName /SC ONLOGON /RL LIMITED /F `
  /TR $action | Out-Null

# Arranca ya mismo esta sesion.
schtasks /Run /TN $TaskName | Out-Null

Write-Host ""
Write-Host "Listo. El agente arranca al iniciar sesion y ya quedo corriendo."
Write-Host "Verifica: abre http://localhost:8181/api/printer/status (debe decir ONLINE)."
Write-Host "Para desinstalar: .\install-service.ps1 -Uninstall"
