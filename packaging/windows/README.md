# SureSell Print Agent — instalación en Windows

Tres formas de correr el agente en la máquina del punto de venta, de la más simple
a la más "producto". Todas exponen la API en `http://localhost:8181` y la PWA del
POS le envía los tickets.

> El agente detecta la impresora por el fragmento de nombre `SAT` (config
> `printer.name`); si no la encuentra, usa la **impresora predeterminada** de Windows.
> Para forzar otro nombre, pásalo como argumento (ver abajo).

## Opción A — Launcher rápido (requiere Java 17)

Para probar ya. Necesita [Java 17](https://adoptium.net) instalado.

1. Copia `run-agent.bat` **y** el JAR `ms-print-agent-0.0.1-SNAPSHOT.jar`
   (de `build/libs/`) a una carpeta.
2. Doble clic en `run-agent.bat` (o `run-agent.bat "POS-58"` para forzar impresora).
3. Deja la ventana abierta. Verifica en `http://localhost:8181/api/printer/status` → `ONLINE`.

## Opción B — Auto-arranque (tarea programada = "servicio", requiere Java 17)

Para que arranque solo al iniciar sesión y corra sin ventana.

```powershell
# En PowerShell, en la carpeta con el JAR + el .ps1:
.\install-service.ps1                      # impresora "SAT"/predeterminada
.\install-service.ps1 -PrinterName "POS-58"
.\install-service.ps1 -Uninstall           # para desinstalar
```

Copia el JAR a `%LOCALAPPDATA%\SureSellPrintAgent`, crea la tarea `SureSellPrintAgent`
(inicio de sesión, oculta, con `javaw`) y la arranca. Verifica el `/status`.

## Opción C — Instalador nativo con JRE embebido (el cliente NO instala Java)

La forma "producto". Se **construye en una máquina Windows con JDK 17**:

```bat
build-installer.bat            :: carpeta portable con .exe (no requiere WiX)
build-installer.bat msi        :: instalador .msi (requiere WiX Toolset 3.x)
build-installer.bat exe        :: instalador .exe (requiere WiX Toolset 3.x)
```

Usa `jpackage` (viene con el JDK) para empacar el JAR + un runtime Java recortado.
El resultado queda en `build/installer/`. El `app-image` (default) es una carpeta
autocontenida con `SureSellPrintAgent.exe` — sin dependencias externas.

## Probar con staging

1. Arranca el agente (cualquier opción) → `http://localhost:8181/api/printer/status` = `ONLINE`.
2. En **Chrome, en la misma máquina**, abre `https://pos-web-production-7032.up.railway.app`.
3. Entra (`admin@sharkburger.co` / `shark2026`) y cobra una orden → imprime en la local.
   - El ticket sale con los datos del negocio del tenant (editables en el POS →
     "Datos del negocio"), ya no hardcodeados.
   - Chrome permite HTTPS→`http://localhost` y el agente responde el preflight PNA.

## Notas

- Puerto fijo `8181`. Si choca, edita `printer.name`/`server.port` (arg `--server.port=`).
- El agente NO maneja datos sensibles (solo imprime lo que la PWA le envía).
- Auto-update: ver `packaging/windows/update-agent.ps1` (opcional).
