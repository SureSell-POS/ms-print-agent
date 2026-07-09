package com.suresell.printagent.printer;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints de hardware, idénticos a los que exponía ms-order-product para que
 * el POS (PWA) funcione sin cambios de lógica: /api/printer/*.
 */
@RestController
@RequestMapping("/api/printer")
public class PrinterController {

    private final PrintTicketUseCase printTicketUseCase;
    private final PrintOrderTicketUseCase printOrderTicketUseCase;
    private final PrinterPort printerPort;

    public PrinterController(PrintTicketUseCase printTicketUseCase,
                            PrintOrderTicketUseCase printOrderTicketUseCase,
                            PrinterPort printerPort) {
        this.printTicketUseCase = printTicketUseCase;
        this.printOrderTicketUseCase = printOrderTicketUseCase;
        this.printerPort = printerPort;
    }

    @PostMapping("/ticket")
    public ResponseEntity<String> printTicket(@RequestBody PosTicketRequest request) {
        try {
            printTicketUseCase.execute(request);
            return ResponseEntity.ok("Impresión enviada correctamente");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error imprimiendo: " + e.getMessage());
        }
    }

    @PostMapping("/ticket-batch")
    public ResponseEntity<String> printTicketBatch(@RequestBody List<PosTicketRequest> requests) {
        try {
            for (PosTicketRequest request : requests) {
                printTicketUseCase.execute(request);
            }
            return ResponseEntity.ok(requests.size() + " impresiones enviadas correctamente");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error en lote de impresión: " + e.getMessage());
        }
    }

    @PostMapping("/drawer/open")
    public ResponseEntity<String> openDrawer() {
        try {
            printerPort.openDrawer();
            return ResponseEntity.ok("Comando de apertura enviado");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error abriendo cajón: " + e.getMessage());
        }
    }

    @GetMapping("/status")
    public ResponseEntity<String> getStatus() {
        boolean ready = printerPort.isPrinterReady();
        return ready ? ResponseEntity.ok("ONLINE") : ResponseEntity.status(503).body("OFFLINE");
    }

    /**
     * Versión del agente en ejecución. La lee el updater (update-agent.ps1) para
     * decidir si hay una versión más nueva publicada. Toma la Implementation-Version
     * del manifest del JAR; en dev (sin empaquetar) devuelve "dev".
     */
    @GetMapping("/version")
    public ResponseEntity<java.util.Map<String, String>> getVersion() {
        String v = getClass().getPackage().getImplementationVersion();
        return ResponseEntity.ok(java.util.Map.of("version", v == null ? "dev" : v));
    }

    @PostMapping("/order-ticket")
    public ResponseEntity<String> printOrderTicket(@RequestBody OrderTicketRequest request) {
        try {
            printOrderTicketUseCase.execute(request);
            return ResponseEntity.ok("Comanda enviada correctamente");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error imprimiendo comanda: " + e.getMessage());
        }
    }
}
