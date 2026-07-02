package com.suresell.printagent.printer;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regresión byte-level del formato de ticket (sin hardware). Verifica que la
 * salida ESC/POS mantiene la estructura probada con la SAT Q22 y que el cajón se
 * abre tras imprimir. Congela el comportamiento antes de futuras refactorizaciones.
 */
class EscPosFormatTest {

    /** Impresora falsa que captura los bytes en vez de enviarlos al hardware. */
    static class CapturingPrinter implements PrinterPort {
        byte[] lastReceipt;
        int drawerOpens = 0;

        @Override public void printBytes(byte[] data) { this.lastReceipt = data; }
        @Override public void openDrawer() { this.drawerOpens++; }
        @Override public boolean isPrinterReady() { return true; }
    }

    @Test
    void ticketArrancaConInitTerminaEnCorteYAbreCajon() {
        CapturingPrinter printer = new CapturingPrinter();
        PrintTicketUseCase useCase = new PrintTicketUseCase(printer, new EscPosBuilder());

        PosTicketRequest ticket = new PosTicketRequest(
                "Shark Burger", "900.123-1", "Calle 26 # 53-43", "3200000",
                null, null, "INV-00042", "Angie", null, null, "2026-07-02 14:30",
                List.of(new PosTicketItem("Aletosa", 2, new BigDecimal("25900"), new BigDecimal("51800"))),
                new BigDecimal("46700"), new BigDecimal("0"), new BigDecimal("58800"),
                "Efectivo", new BigDecimal("60000"), new BigDecimal("1200"), null, "Gracias por su compra");

        useCase.execute(ticket);

        byte[] r = printer.lastReceipt;
        assertNotNull(r, "Debe haberse enviado un ticket a imprimir");

        // Arranca con INIT (ESC @ = 27,64)
        assertEquals((byte) 27, r[0]);
        assertEquals((byte) 64, r[1]);

        // Termina con corte total (GS V A 0 = 29,86,65,0)
        int n = r.length;
        assertArrayEquals(new byte[]{29, 86, 65, 0}, new byte[]{r[n - 4], r[n - 3], r[n - 2], r[n - 1]});

        // Contenido esperado (decodificado en CP850, el charset de la impresora)
        String text = new String(r, Charset.forName("CP850"));
        assertTrue(text.contains("SHARK BURGER"), "Debe incluir el negocio en mayúsculas");
        assertTrue(text.contains("TOTAL A PAGAR"), "Debe incluir el total");
        assertTrue(text.contains("INV-00042"), "Debe incluir el número de factura");

        // Abre el cajón exactamente una vez, después de imprimir.
        assertEquals(1, printer.drawerOpens);
    }

    @Test
    void comandaDeOrdenSinCajon() {
        CapturingPrinter printer = new CapturingPrinter();
        PrintOrderTicketUseCase useCase = new PrintOrderTicketUseCase(printer, new EscPosBuilder());

        OrderTicketRequest order = new OrderTicketRequest(
                101L, "AZUL", "5", "2026-07-02 14:30",
                List.of(new OrderTicketItem(2, "Aletosa", "Sin cebolla")));

        useCase.execute(order);

        byte[] r = printer.lastReceipt;
        assertNotNull(r);
        String text = new String(r, Charset.forName("CP850"));
        assertTrue(text.contains("AZUL - 5"), "Debe incluir el rastreador");
        assertTrue(text.contains("Aletosa"));
        assertTrue(text.contains("Sin cebolla"));
        // La comanda NO abre el cajón.
        assertEquals(0, printer.drawerOpens);
    }
}
