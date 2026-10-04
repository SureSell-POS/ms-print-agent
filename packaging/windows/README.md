# SureSell Print Agent — instalación en Windows

> 🔴 **Cambiar el dominio del POS rompe la impresión en todas las máquinas a la vez.** Las políticas
> que escriben `configure-chrome-policy.ps1` / `install.ps1` llevan la URL literal del POS
> (`LocalNetworkAccessAllowedForUrls`); con otra URL, Chrome 142+ cuelga la llamada a
> `localhost:8181` y el POS dice «Impresora Offline» con el agente sano. Ver
> `docs/operacion/DNS-Y-DOMINIOS.md` y `docs/operacion/INSTALADOR-AGENTE-PLAN.md` §3.2 (la política
> la escribirá el agente con el origen real).


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

## Opción D — Instalación COMPLETA de producción (recomendada para clientes)

Un solo comando (PowerShell **como administrador**) que deja todo listo: política del
navegador + auto-arranque del agente.

```powershell
.\install.ps1                                   # origen por defecto: https://pos-caja.suresell.com.co (producción)
.\install.ps1 -Origin "https://pos.tudominio.com" -PrinterName "POS-58"
.\install.ps1 -Uninstall
```

### ¿Por qué hace falta la política del navegador? (Local Network Access)
Chrome 142+ (oct-2025) **bloquea** que una web pública (la PWA en HTTPS) llame a
`http://localhost:8181` (el agente): *"Permission was denied … loopback address space"*.
El header del agente ya NO basta y **no se puede pedir al cliente que toque `chrome://flags`**
(eso es solo para pruebas rápidas). La solución persistente es una **política de empresa**
que allowlistea el ORIGEN de la PWA — la aplica `install.ps1` (o `configure-chrome-policy.ps1`)
en el registro (HKLM), para Chrome y Edge. Sobrevive updates y no muestra prompts.
Ref: https://chromeenterprise.google/policies/local-network-access-allowed-for-urls/

> Verifícala en `chrome://policy` → debe aparecer `LocalNetworkAccessAllowedForUrls` con tu origen.
> Solo para una prueba puntual sin instalar la política: `chrome://flags/#local-network-access-checks` = Disabled.

## Probar con staging

1. Arranca el agente (cualquier opción) → `http://localhost:8181/api/printer/status` = `ONLINE`.
2. Aplica la política del navegador (`install.ps1` o `configure-chrome-policy.ps1`, como admin)
   y **reinicia Chrome** — si no, Chrome 142+ bloquea la llamada a localhost.
   Para staging: `.\install.ps1 -Origin "https://posstaging.suresell.com.co"` y, una vez, en PowerShell normal:
   `setx AGENTE_ORIGENES "https://posstaging.suresell.com.co"` (sin eso el agente solo atiende al POS de producción).
   Cierra sesión de Windows y vuelve a entrar para que la tarea del agente la lea.
3. En **Chrome, en la misma máquina**, abre `https://posstaging.suresell.com.co`.
4. Entra con un usuario de QA de staging y cobra una orden → imprime en la local.
   - El ticket sale con los datos del negocio del tenant (editables en el POS →
     "Datos del negocio"), ya no hardcodeados.

## Actualizar un local a 0.0.2 (A10: solo 127.0.0.1 y solo el POS)

Desde 0.0.2 el agente **solo escucha en 127.0.0.1** (otra máquina de la red no llega) y **solo atiende al POS**:
`https://pos-caja.suresell.com.co`. Cualquier otra web (incluidos staging y un programa local en el 4200)
recibe 403 sin cabeceras CORS. Un lote lleva como mucho 20 tickets. Un error devuelve un texto genérico; el detalle
queda en el log del agente.

**Una vez, para publicar la versión** (en el equipo de desarrollo):

1. `./gradlew clean bootJar` → `build/libs/ms-print-agent-0.0.2.jar`.
2. `shasum -a 256 build/libs/ms-print-agent-0.0.2.jar` → copia el hash.
3. Crea un `latest.json`:
   `{ "version": "0.0.2", "url": "https://github.com/SureSell-POS/ms-print-agent/releases/download/v0.0.2/ms-print-agent-0.0.2.jar", "sha256": "<hash>" }`
4. En GitHub, crea el release `v0.0.2` en `SureSell-POS/ms-print-agent` con dos assets: el JAR y `latest.json`.

**En cada local** (PowerShell **como administrador**, en la carpeta con los `.ps1`):

1. Política del navegador con el dominio actual del POS (las máquinas antiguas tienen la URL de Railway, que ya no
   sirve):
   `.\configure-chrome-policy.ps1` (escribe `https://pos-caja.suresell.com.co`).
2. Agente nuevo: `.\update-agent.ps1` (o espera a la revisión diaria de las 04:30 si se instaló con `-Install`).
3. **Cierra Chrome del todo y ábrelo otra vez.**
4. Comprueba:
   - `http://127.0.0.1:8181/api/printer/version` → `{"version":"0.0.2"}`.
   - `chrome://policy` → `LocalNetworkAccessAllowedForUrls` = `https://pos-caja.suresell.com.co`.
   - En el POS: cobra una venta y abre el cajón.

**Si el POS dice «Impresora Offline» tras actualizar**, casi siempre el origen no está en la lista. Mira la URL de la
barra del navegador. Si no es la de arriba, arranca el agente con la variable `AGENTE_ORIGENES` (lista
separada por comas, sin `/` final) o corrige la URL del POS.

## Notas

- Puerto fijo `8181`, solo en `127.0.0.1`. Si choca, edita `printer.name`/`server.port` (arg `--server.port=`).
- El agente NO maneja datos sensibles (solo imprime lo que la PWA le envía).
- Auto-update: ver `packaging/windows/update-agent.ps1` (opcional).
