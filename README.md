# ms-print-agent

> 🔴 **Cambiar el dominio del POS rompe la impresión en todas las máquinas a la vez.** Las políticas
> que escriben `configure-chrome-policy.ps1` / `install.ps1` llevan la URL literal del POS
> (`LocalNetworkAccessAllowedForUrls`); con otra URL, Chrome 142+ cuelga la llamada a
> `localhost:8181` y el POS dice «Impresora Offline» con el agente sano. Ver
> `docs/operacion/DNS-Y-DOMINIOS.md` y `docs/operacion/INSTALADOR-AGENTE-PLAN.md` §3.2 (la política
> la escribirá el agente con el origen real).


Agente local de hardware para el POS de **SureSell**. Es un microservicio Spring Boot
mínimo (solo `web` + `lombok`, sin base de datos) que corre **en la máquina del punto de
venta** y expone la impresora térmica ESC/POS y el cajón monedero como una API HTTP local.

La PWA del POS se sirve por HTTPS desde un dominio público, pero la impresión requiere
acceso al hardware local. Este agente resuelve ese puente: escucha en `localhost` y la
PWA le envía los tickets a imprimir.

## Arquitectura

Puertos y adaptadores (hexagonal), en un solo paquete `com.suresell.printagent.printer`:

- `PrinterController` — endpoints REST `/api/printer/*`.
- `PrintTicketUseCase` — arma e imprime la **factura de venta** (y abre el cajón).
- `PrintOrderTicketUseCase` — imprime la **comanda** de orden/emergencia (sin cajón).
- `EscPosBuilder` — construye los bytes ESC/POS (formato probado en la SAT Q22).
- `PrinterPort` / `JavaXPrinterAdapter` — salida a la impresora vía `javax.print`.
- `config/PrinterCorsFilter` — CORS + **Private Network Access** para que Chrome permita
  a la PWA pública llamar a `http://localhost`.

El formato de los tickets se reutiliza verbatim desde `ms-order-product` para preservar el
layout ya probado en producción.

## Requisitos

- Java 17
- Una impresora térmica ESC/POS instalada en el SO (por defecto, clase **SAT Q22**).

## Configuración

`src/main/resources/application.yml`:

| Propiedad | Default | Descripción |
|-----------|---------|-------------|
| `server.port` | `8181` | Puerto local del agente (distinto del 8081 de datos, para correr ambos). |
| `printer.name` | `SAT` | Nombre (o fragmento) de la impresora en el SO. |
| `server.address` | `127.0.0.1` | Solo la propia máquina; otra de la red no llega al agente. |
| `agente.origenes` (env `AGENTE_ORIGENES`) | solo `https://pos-caja.suresell.com.co` | Únicas webs que pueden llamar al agente; otra recibe 403 sin cabeceras CORS. Staging (`posstaging`) y desarrollo (`localhost:4200`) se añaden con `AGENTE_ORIGENES`, solo en esas máquinas. |

Actualizar los locales: `packaging/windows/README.md` → «Actualizar un local a 0.0.2».

## Ejecutar

```bash
./gradlew bootRun          # arranca el agente en http://localhost:8181
./gradlew clean build      # build completo
./gradlew clean bootJar    # empaqueta el JAR ejecutable
./gradlew test             # pruebas (formato ESC/POS)
```

## API — `/api/printer`

| Método | Ruta | Body | Descripción |
|--------|------|------|-------------|
| `POST` | `/ticket` | `PosTicketRequest` | Imprime factura de venta y abre el cajón. |
| `POST` | `/ticket-batch` | `PosTicketRequest[]` | Imprime varias facturas en lote. |
| `POST` | `/order-ticket` | `OrderTicketRequest` | Imprime comanda de orden/emergencia. |
| `POST` | `/drawer/open` | — | Abre el cajón monedero. |
| `GET`  | `/status` | — | `ONLINE` (200) u `OFFLINE` (503). |

Los DTOs `PosTicketRequest` y `OrderTicketRequest` toleran propiedades JSON desconocidas,
por lo que el frontend puede enviar el objeto de orden completo sin acoplamiento.

## Contexto

Forma parte del monorepo **SureSell**. Se despliega junto al punto de venta; el resto de
microservicios corren en la nube. Ver `ms-order-product` para el origen del formato de
tickets.
