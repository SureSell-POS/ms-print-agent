package com.suresell.printagent.printer;

import java.math.BigDecimal;

/** Detalle de un producto en el ticket de venta. */
public record PosTicketItem(
        String name,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal total
) {}
