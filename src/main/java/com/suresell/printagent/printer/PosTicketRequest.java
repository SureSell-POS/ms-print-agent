package com.suresell.printagent.printer;

import java.math.BigDecimal;
import java.util.List;

/** Solicitud de impresión de ticket POS (factura de venta). */
public record PosTicketRequest(
        String businessName,
        String nit,
        String address,
        String phone,
        String resolutionDian,
        String resolutionRange,
        String ticketNumber,
        String cashierName,
        String customerName,
        String customerId,
        String dateTime,
        List<PosTicketItem> items,
        BigDecimal subtotal,
        BigDecimal tax,
        BigDecimal total,
        String paymentMethod,
        BigDecimal cashGiven,
        BigDecimal change,
        String qrContent,
        String footerMessage
) {}
