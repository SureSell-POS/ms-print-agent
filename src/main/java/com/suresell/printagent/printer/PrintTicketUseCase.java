package com.suresell.printagent.printer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.text.DecimalFormat;

/**
 * Genera e imprime el ticket de venta (factura POS). Lógica de formato reusada
 * verbatim desde ms-order-product para preservar el layout probado en la SAT Q22.
 */
@Slf4j
@Service
public class PrintTicketUseCase {
    private final PrinterPort printerPort;
    private final EscPosBuilder pos;
    private static final DecimalFormat MONEY_FMT = new DecimalFormat("$ #,##0");

    /** Formatea dinero de forma segura: un valor null no debe romper la impresión. */
    private String money(BigDecimal value) {
        return MONEY_FMT.format(value != null ? value : BigDecimal.ZERO);
    }
    public PrintTicketUseCase(PrinterPort printerPort, EscPosBuilder pos) {
        this.printerPort = printerPort;
        this.pos = pos;
    }
    public void execute(PosTicketRequest ticket) {
        log.info("Inicia Impresion Ticket");
        byte[] receiptBytes = pos.buildReceipt(bos -> {
            pos.alignCenter(bos);
            pos.boldOn(bos);
            pos.textLn(bos, ticket.businessName().toUpperCase());
            pos.boldOff(bos);
            pos.textLn(bos, "NIT: " + ticket.nit());
            pos.textLn(bos, ticket.address());
            pos.textLn(bos, "Tel: " + ticket.phone());
            pos.textLn(bos, "Regimen Fiscal: IVA Responsable");
            pos.feed(bos, 1);
            if (ticket.resolutionDian() != null) {
                pos.textLn(bos, "Autorización Numeración de Facturación");
                pos.textLn(bos, ticket.resolutionDian());
                pos.textLn(bos, ticket.resolutionRange());
            }
            pos.feed(bos, 1);
            pos.textLn(bos, "------------------------------------------");
            pos.alignLeft(bos);
            pos.textLn(bos, "Factura de Venta No: " + ticket.ticketNumber());
            pos.textLn(bos, "Fecha: " + ticket.dateTime());
            pos.textLn(bos, "Cajero: " + ticket.cashierName());
            if (ticket.customerName() != null) {
                pos.textLn(bos, "Cliente: " + ticket.customerName());
                pos.textLn(bos, "CC/NIT: " + ticket.customerId());
            }
            pos.textLn(bos, "------------------------------------------");
            pos.boldOn(bos);
            pos.textLn(bos, "ITEM / DESCRIPCION           TOTAL");
            pos.boldOff(bos);
            for (PosTicketItem item : ticket.items()) {
                pos.alignLeft(bos);
                pos.textLn(bos, item.name());
                String quantityPrice = item.quantity() + " x " + money(item.unitPrice());
                String totalStr = money(item.total());
                String line = String.format("%-25s %15s", quantityPrice, totalStr);
                pos.textLn(bos, line);
            }
            pos.textLn(bos, "------------------------------------------");
            pos.alignRight(bos);
            // Si la orden no trae subtotal (p.ej. órdenes de la app móvil), usar el total.
            BigDecimal subtotalToPrint = ticket.subtotal() != null ? ticket.subtotal() : ticket.total();
            pos.textLn(bos, "SUBTOTAL: " + money(subtotalToPrint));
            pos.textLn(bos, "IMPUESTOS (INC): " + money(ticket.tax()));
            pos.boldOn(bos);
            pos.feed(bos, 1);
            pos.textLn(bos, "TOTAL A PAGAR: " + money(ticket.total()));
            pos.boldOff(bos);
            pos.feed(bos, 1);
            pos.textLn(bos, "Efectivo: " + money(ticket.cashGiven()));
            pos.textLn(bos, "Cambio: " + money(ticket.change()));
            pos.feed(bos, 1);
            pos.alignCenter(bos);
            pos.textLn(bos, "Forma de Pago: " + ticket.paymentMethod());
            pos.feed(bos, 1);
            pos.feed(bos, 1);
            pos.textLn(bos, ticket.footerMessage());
            pos.textLn(bos, "Sistema POS desarrollado por");
            pos.textLn(bos, "--------- SureSell ---------");
            pos.textLn(bos, "­-- www.suresell.com.co --­");
            pos.feed(bos, 2);
        });
        log.info("Ticket Procesado");
        printerPort.printBytes(receiptBytes);
        log.info("Ticket Impreso");
        printerPort.openDrawer();
        log.info("Caja Abierta");
    }
}
