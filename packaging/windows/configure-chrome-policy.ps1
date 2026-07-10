# =============================================================================
#  SureSell Print Agent - política de Chrome/Edge para permitir la impresión local.
#
#  Chrome 142+ (Local Network Access) bloquea que una web pública (la PWA en HTTPS)
#  llame a http://localhost:8181 (el agente), con "Permission was denied ... loopback
#  address space". El header del agente ya NO basta. La solución PERSISTENTE (sin
#  flags, sin prompts, sobrevive updates) es una POLÍTICA DE EMPRESA que allowlistea
#  el ORIGEN de la PWA:
#    - LocalNetworkAccessAllowedForUrls           (Chrome 142+, LNA)
#    - InsecurePrivateNetworkRequestsAllowedForUrls (Chrome anterior, PNA)
#  Se escribe en HKLM (requiere ADMIN) para Chrome y Edge. Ref:
#  https://chromeenterprise.google/policies/local-network-access-allowed-for-urls/
#
#  Uso (PowerShell COMO ADMINISTRADOR):
#    .\configure-chrome-policy.ps1
#    .\configure-chrome-policy.ps1 -Origin "https://pos.tudominio.com"
#    .\configure-chrome-policy.ps1 -Uninstall
# =============================================================================
param(
  [string]$Origin = "https://pos-web-production-7032.up.railway.app",
  [switch]$Uninstall
)

$ErrorActionPreference = "Stop"

# --- Requiere admin (HKLM) ---
$admin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()
         ).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $admin) {
  Write-Error "Ejecuta este script COMO ADMINISTRADOR (clic derecho en PowerShell -> Ejecutar como administrador)."
}

# Políticas de lista (una por navegador Chromium) que allowlistean el origen.
$policies = @("LocalNetworkAccessAllowedForUrls", "InsecurePrivateNetworkRequestsAllowedForUrls")
$browsers = @(
  @{ name = "Chrome"; base = "HKLM:\SOFTWARE\Policies\Google\Chrome" },
  @{ name = "Edge";   base = "HKLM:\SOFTWARE\Policies\Microsoft\Edge" }
)

foreach ($b in $browsers) {
  foreach ($p in $policies) {
    $key = Join-Path $b.base $p
    if ($Uninstall) {
      if (Test-Path $key) { Remove-Item $key -Recurse -Force; Write-Host "[$($b.name)] quitada $p" }
      continue
    }
    New-Item -Path $key -Force | Out-Null
    # Política de lista: valores REG_SZ nombrados "1","2",... Fijamos el origen en "1".
    New-ItemProperty -Path $key -Name "1" -Value $Origin -PropertyType String -Force | Out-Null
    Write-Host "[$($b.name)] $p -> $Origin"
  }
}

Write-Host ""
if ($Uninstall) {
  Write-Host "Políticas removidas. Reinicia el navegador."
} else {
  Write-Host "Listo. Reinicia Chrome/Edge (o abre chrome://policy y 'Recargar políticas')."
  Write-Host "Verifica en chrome://policy que aparezca LocalNetworkAccessAllowedForUrls = $Origin"
}
