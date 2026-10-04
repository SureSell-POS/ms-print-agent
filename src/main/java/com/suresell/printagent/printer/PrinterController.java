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

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PrinterController.class);

    /** A10: una petición no vacía el rollo de papel. */
    static final int MAXIMO_POR_LOTE = 20;

    /** A10: el detalle (impresora, Java) va al log; al cliente, un mensaje genérico. */
    private static ResponseEntity<String> error(String queFallo, Exception e) {
        log.error("{}", queFallo, e);
        return ResponseEntity.internalServerError().body(queFallo + ". Revisa que la impresora esté encendida y conectada.");
    }

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
            return error("No se pudo imprimir", e);
        }
    }

    @PostMapping("/ticket-batch")
    public ResponseEntity<String> printTicketBatch(@RequestBody List<PosTicketRequest> requests) {
        if (requests.size() > MAXIMO_POR_LOTE) {
            return ResponseEntity.badRequest().body("Un lote lleva como mucho " + MAXIMO_POR_LOTE + " tickets.");
        }
        try {
            for (PosTicketRequest request : requests) {
                printTicketUseCase.execute(request);
            }
            return ResponseEntity.ok(requests.size() + " impresiones enviadas correctamente");
        } catch (Exception e) {
            return error("No se pudo imprimir el lote", e);
        }
    }

    @PostMapping("/drawer/open")
    public ResponseEntity<String> openDrawer() {
        try {
            printerPort.openDrawer();
            return ResponseEntity.ok("Comando de apertura enviado");
        } catch (Exception e) {
            return error("No se pudo abrir el cajón", e);
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
            return error("No se pudo imprimir la comanda", e);
        }
    }
}
