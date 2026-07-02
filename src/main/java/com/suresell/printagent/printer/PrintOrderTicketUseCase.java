package com.suresell.printagent.printer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Imprime la comanda de orden/emergencia (sin cajón). Formato reusado desde
 * ms-order-product, adaptado al DTO slim del agente. `createdAt` llega ya como
 * String, así que se imprime tal cual.
 */
@Slf4j
@Service
public class PrintOrderTicketUseCase {

    private final PrinterPort printerPort;
    private final EscPosBuilder pos;

    public PrintOrderTicketUseCase(PrinterPort printerPort, EscPosBuilder pos) {
        this.printerPort = printerPort;
        this.pos = pos;
    }

    public void execute(OrderTicketRequest order) {
        log.info("Inicia Impresion de Ticket de Orden/Emergencia #{}", order.idOrder());

        byte[] receiptBytes = pos.buildReceipt(bos -> {

            pos.alignCenter(bos);

            if (order.pagerColor() != null && order.pagerNumber() != null) {
                pos.inverseOn(bos);
                pos.textSize(bos, 1, 1);
                pos.boldOn(bos);

                String pagerInfo = " " + order.pagerColor().toUpperCase() + " - " + order.pagerNumber() + " ";
                pos.textLn(bos, pagerInfo);

                pos.inverseOff(bos);
                pos.textSize(bos, 0, 0);
                pos.boldOff(bos);
            } else {
                pos.inverseOn(bos);
                pos.textSize(bos, 1, 1);
                pos.textLn(bos, " SIN RASTREADOR ");
                pos.inverseOff(bos);
                pos.textSize(bos, 0, 0);
            }

            pos.feed(bos, 1);

            pos.alignLeft(bos);
            pos.textLn(bos, "Orden #: " + order.idOrder());
            if (order.createdAt() != null) {
                pos.textLn(bos, "Fecha: " + order.createdAt());
            }
            pos.textLn(bos, "------------------------------------------");

            pos.boldOn(bos);
            pos.textLn(bos, "CANT  PRODUCTO");
            pos.boldOff(bos);
            pos.textLn(bos, "------------------------------------------");

            if (order.items() != null) {
                for (OrderTicketItem item : order.items()) {
                    pos.boldOn(bos);
                    pos.textLn(bos, String.format(" %-4d %s", item.quantity(), item.nameProduct()));
                    pos.boldOff(bos);

                    if (item.instructions() != null && !item.instructions().isBlank()) {
                        pos.textLn(bos, "      * " + item.instructions());
                    }
                }
            }

            pos.textLn(bos, "------------------------------------------");
            pos.feed(bos, 3);
        });

        printerPort.printBytes(receiptBytes);
        log.info("Ticket de Orden impreso correctamente");
    }
}
