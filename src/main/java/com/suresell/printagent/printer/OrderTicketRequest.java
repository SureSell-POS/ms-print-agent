package com.suresell.printagent.printer;

import java.util.List;

/**
 * DTO slim para la comanda de orden/emergencia (endpoint /order-ticket).
 *
 * El frontend envía el objeto de orden completo; aquí solo declaramos los campos
 * que la comanda imprime. Spring Boot ignora las propiedades JSON desconocidas
 * (FAIL_ON_UNKNOWN_PROPERTIES=false por defecto), así que no hay acoplamiento con
 * el dominio de órdenes. `createdAt` se recibe como String (ya viene serializado).
 */
public record OrderTicketRequest(
        Long idOrder,
        String pagerColor,
        String pagerNumber,
        String createdAt,
        List<OrderTicketItem> items
) {}
