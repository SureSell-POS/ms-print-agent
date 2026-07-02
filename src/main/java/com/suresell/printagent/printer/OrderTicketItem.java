package com.suresell.printagent.printer;

/** Ítem de la comanda de orden/emergencia. */
public record OrderTicketItem(
        int quantity,
        String nameProduct,
        String instructions
) {}
